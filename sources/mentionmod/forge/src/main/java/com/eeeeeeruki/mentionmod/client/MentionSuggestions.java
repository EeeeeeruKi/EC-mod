package com.eeeeeeruki.mentionmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * チャットで「@」を打つと、入力欄の上にメンションの候補を出す。
 * 「@」だけの時は @OP・@everyone（OPのみ）・@ai（使えるサーバーのみ）を出し、
 * 1文字以上打ったら、その文字で始まるプレイヤー名も出す。
 * Tab かクリックで決定、↑↓で選ぶ、Esc で閉じる。
 */
public final class MentionSuggestions {
    private static final int MAX_ROWS = 8;
    private static final int ROW_H = 12;
    private static final int MAX_CHAT_LENGTH = 256;

    private record Candidate(String text, String desc) {}

    /** start: 「@」の位置、end: 置き換える範囲の終わり */
    private record Token(int start, int end, String key, List<Candidate> list) {}

    private static boolean serverAi;
    private static int selected;
    private static String lastKey;
    private static String dismissedKey;

    // 前回描いた候補の場所（クリックの判定用）
    private static int boxX, boxTop, boxW, boxRows;

    private MentionSuggestions() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(MentionSuggestions::onRender);
        MinecraftForge.EVENT_BUS.addListener(MentionSuggestions::onKey);
        MinecraftForge.EVENT_BUS.addListener(MentionSuggestions::onClick);
        MinecraftForge.EVENT_BUS.addListener(MentionSuggestions::onLogout);
    }

    public static void setServerAi(boolean ai) {
        serverAi = ai;
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        serverAi = false;
        lastKey = null;
        dismissedKey = null;
    }

    // ---- 候補を探す ----

    private static EditBox input(Screen screen) {
        for (GuiEventListener l : screen.children()) {
            if (l instanceof EditBox box) return box;
        }
        return null;
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    private static Token current(Screen screen) {
        if (!(screen instanceof ChatScreen)) return null;
        if (!ReplyClient.serverHasModPublic()) return null;
        EditBox box = input(screen);
        if (box == null) return null;

        String v = box.getValue();
        int cur = Math.min(box.getCursorPosition(), v.length());
        if (v.startsWith("/")) return null;

        int i = cur;
        while (i > 0 && isNameChar(v.charAt(i - 1))) i--;
        if (i == 0 || v.charAt(i - 1) != '@') return null;
        int at = i - 1;
        // メールアドレスのように、直前が英数字なら候補を出さない
        if (at > 0 && isNameChar(v.charAt(at - 1))) return null;
        int end = cur;
        while (end < v.length() && isNameChar(v.charAt(end))) end++;

        String key = v + "|" + cur;
        if (key.equals(dismissedKey)) return null;

        String prefix = v.substring(i, cur).toLowerCase(Locale.ROOT);
        List<Candidate> list = candidates(prefix);
        if (list.isEmpty()) return null;
        // もう打ち終わっている時（候補が1つだけで、打った文字と同じ）は出さない
        if (list.size() == 1 && list.get(0).text().substring(1).equalsIgnoreCase(v.substring(i, end))) return null;

        if (!key.equals(lastKey)) {
            lastKey = key;
            selected = 0;
        }
        if (selected >= list.size()) selected = list.size() - 1;
        return new Token(at, end, key, list);
    }

    private static List<Candidate> candidates(String prefix) {
        Minecraft mc = Minecraft.getInstance();
        List<Candidate> out = new ArrayList<>();
        addSpecial(out, prefix, "OP", "mentionmod.suggest.op");
        if (mc.player != null && mc.player.hasPermissions(2)) {
            addSpecial(out, prefix, "everyone", "mentionmod.suggest.everyone");
        }
        if (serverAi) addSpecial(out, prefix, "ai", "mentionmod.suggest.ai");

        // プレイヤー名は1文字以上打ってから
        ClientPacketListener c = mc.getConnection();
        if (!prefix.isEmpty() && c != null) {
            String self = mc.player != null ? mc.player.getGameProfile().getName() : "";
            List<String> names = new ArrayList<>();
            for (PlayerInfo info : c.getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (name.equalsIgnoreCase(self)) continue;
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) names.add(name);
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            for (String n : names) out.add(new Candidate("@" + n, ""));
        }
        return out.size() > MAX_ROWS ? new ArrayList<>(out.subList(0, MAX_ROWS)) : out;
    }

    private static void addSpecial(List<Candidate> out, String prefix, String word, String descKey) {
        if (word.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(new Candidate("@" + word, I18n.get(descKey)));
    }

    // ---- 決定 ----

    private static void accept(Screen screen, Token t) {
        EditBox box = input(screen);
        if (box == null) return;
        String v = box.getValue();
        String word = t.list().get(selected).text();
        String after = v.substring(Math.min(t.end(), v.length()));
        String insert = after.startsWith(" ") ? word : word + " ";
        String nv = v.substring(0, t.start()) + insert + after;
        if (nv.length() > MAX_CHAT_LENGTH) return;
        int pos = t.start() + word.length() + 1;
        box.setValue(nv);
        box.setCursorPosition(Math.min(pos, nv.length()));
        box.setHighlightPos(box.getCursorPosition());
    }

    // ---- 画面 ----

    private static void onRender(ScreenEvent.Render.Post event) {
        boxRows = 0;
        Screen screen = event.getScreen();
        Token t = current(screen);
        if (t == null) return;
        EditBox box = input(screen);
        Font font = Minecraft.getInstance().font;
        GuiGraphics g = event.getGuiGraphics();

        int w = 0;
        for (Candidate c : t.list()) {
            int cw = font.width(c.text()) + (c.desc().isEmpty() ? 0 : 8 + font.width(c.desc()));
            w = Math.max(w, cw);
        }
        w += 6;

        String before = box.getValue().substring(0, Math.min(t.start(), box.getValue().length()));
        int x = Math.max(2, Math.min(4 + font.width(before), screen.width - w - 2));
        int bottom = ReplyClient.isReplying() ? screen.height - 28 : screen.height - 15;
        int rows = t.list().size();
        int top = bottom - rows * ROW_H;

        // マウスが乗っている候補を選ぶ
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        if (mx >= x && mx < x + w && my >= top && my < bottom) selected = (my - top) / ROW_H;

        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(x, top, x + w, bottom, 0xE0000000);
        for (int i = 0; i < rows; i++) {
            Candidate c = t.list().get(i);
            int y = top + i * ROW_H + 2;
            boolean sel = i == selected;
            g.drawString(font, c.text(), x + 3, y, sel ? 0xFFFF55 : 0xAAAAAA);
            if (!c.desc().isEmpty()) {
                g.drawString(font, c.desc(), x + 3 + font.width(c.text()) + 8, y, 0x777777);
            }
        }
        g.pose().popPose();

        boxX = x;
        boxTop = top;
        boxW = w;
        boxRows = rows;
    }

    private static void onKey(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        Token t = current(screen);
        if (t == null) return;
        int n = t.list().size();
        switch (event.getKeyCode()) {
            case GLFW.GLFW_KEY_TAB -> {
                accept(screen, t);
                event.setCanceled(true);
            }
            case GLFW.GLFW_KEY_UP -> {
                selected = (selected - 1 + n) % n;
                event.setCanceled(true);
            }
            case GLFW.GLFW_KEY_DOWN -> {
                selected = (selected + 1) % n;
                event.setCanceled(true);
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                // 候補だけ閉じる（チャットは閉じない）
                dismissedKey = t.key();
                event.setCanceled(true);
            }
            default -> {
            }
        }
    }

    private static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (boxRows == 0 || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        double mx = event.getMouseX();
        double my = event.getMouseY();
        if (mx < boxX || mx >= boxX + boxW || my < boxTop || my >= boxTop + boxRows * ROW_H) return;
        Screen screen = event.getScreen();
        Token t = current(screen);
        if (t == null) return;
        selected = Math.min((int) ((my - boxTop) / ROW_H), t.list().size() - 1);
        accept(screen, t);
        event.setCanceled(true);
    }
}
