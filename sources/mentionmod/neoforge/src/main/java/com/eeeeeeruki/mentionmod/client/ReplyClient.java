package com.eeeeeeruki.mentionmod.client;

import com.eeeeeeruki.mentionmod.network.ReplyPayload;
import com.eeeeeeruki.mentionmod.network.ReplyStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * チャット画面でメッセージを右クリックすると返信モードになる。
 * 受け取ったプレイヤーのチャットに見えない目印（insertion）を付けておき、
 * 右クリックした場所の目印から、どのメッセージかを探す。
 * Watut が入っている時は、「入力中」の表示も日本語にして「返信中」を出す。
 */
public final class ReplyClient {
    private static final String TOKEN = "mentionmod:reply#";
    private static final int MAX_KEEP = 300;
    private static final int TYPING_CHAR_LIMIT = 50;

    private record Entry(int id, String sender, String text) {}

    private static final Map<Integer, Entry> MESSAGES = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Entry> eldest) {
            return size() > MAX_KEEP;
        }
    };
    /** 返信モード中のプレイヤー（サーバーから届く） */
    private static final Set<UUID> REMOTE_REPLYING = new HashSet<>();
    private static int nextId = 0;
    private static Entry target;

    private ReplyClient() {}

    public static void init() {
        WatutCompat.init();
        NeoForge.EVENT_BUS.addListener(ReplyClient::onReceive);
        NeoForge.EVENT_BUS.addListener(ReplyClient::onMouse);
        NeoForge.EVENT_BUS.addListener(ReplyClient::onRender);
        NeoForge.EVENT_BUS.addListener(ReplyClient::onClose);
        NeoForge.EVENT_BUS.addListener(ReplyClient::onSend);
        NeoForge.EVENT_BUS.addListener(ReplyClient::onGuiRenderPre);
        NeoForge.EVENT_BUS.addListener(ReplyClient::onTick);
        NeoForge.EVENT_BUS.addListener(ReplyClient::onLogout);
    }

    /** AI（Call AI など）の状態。名前 → [状態, 更新した時刻] */
    private static final Map<String, long[]> AI_STATUS = new LinkedHashMap<>();
    private static final long AI_STATUS_TIMEOUT_MS = 660_000; // 長く考えるモデルでも消えないように（終わったら IDLE が届く）

    public static void setAiStatus(String name, int state) {
        if (state == 0) AI_STATUS.remove(name);
        else AI_STATUS.put(name, new long[]{state, System.currentTimeMillis()});
    }

    public static void setRemoteReplying(UUID player, boolean replying) {
        if (replying) REMOTE_REPLYING.add(player);
        else REMOTE_REPLYING.remove(player);
    }

    private static void setTarget(Entry entry) {
        boolean was = target != null;
        target = entry;
        boolean now = target != null;
        if (was != now && serverHasMod()) {
            try {
                PacketDistributor.sendToServer(new ReplyStatePayload(now));
            } catch (Exception ignored) {
            }
        }
    }

    private static void onReceive(ClientChatReceivedEvent.Player event) {
        String text = event.getPlayerChatMessage().signedContent();
        String name = nameOf(event.getSender());
        if (name == null) name = event.getBoundChatType().name().getString();
        if (text == null || name.isEmpty()) return;

        int id = nextId++;
        MESSAGES.put(id, new Entry(id, name, text));
        Component original = event.getMessage();
        event.setMessage(Component.empty().withStyle(s -> s.withInsertion(TOKEN + id)).append(original));
    }

    private static void onMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;
        Style style = Minecraft.getInstance().gui.getChat()
                .getClickedComponentStyleAt(event.getMouseX(), event.getMouseY());
        Entry entry = entryOf(style);
        if (entry == null) return;

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // 同じメッセージをもう一度右クリックすると解除
            setTarget((target != null && target.id() == entry.id()) ? null : entry);
            event.setCanceled(true);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && Screen.hasShiftDown()) {
            // 目印の文字が入力欄に入らないようにする
            event.setCanceled(true);
        }
    }

    /** Watut は画面の描画より先に「〇〇 is typing」を描くので、その前に止める */
    private static void onGuiRenderPre(RenderGuiEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof ChatScreen) {
            WatutCompat.suppressOriginalText();
        }
    }

    private static void onTick(ClientTickEvent.Post event) {
        WatutCompat.restore();
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        REMOTE_REPLYING.clear();
        AI_STATUS.clear();
        target = null;
    }

    private static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ChatScreen screen)) return;
        WatutCompat.restore();
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        boolean bar = target != null;
        if (bar) {
            int y = screen.height - 27;
            g.fill(2, y, screen.width - 2, y + 12, 0xC0000000);
            Component line = Component.translatable("mentionmod.reply.bar", target.sender(), shorten(target.text(), 40));
            g.drawString(font, line, 4, y + 2, 0xFFFFFF);
        }

        boolean watut = WatutCompat.isReady() && WatutCompat.shouldShow();
        if (watut || !AI_STATUS.isEmpty()) {
            String typing = typingLine(watut);
            if (!typing.isEmpty()) {
                int y = (bar ? screen.height - 38 : screen.height - 24) + WatutCompat.offsetY();
                g.drawString(font, typing, 2 + WatutCompat.offsetX(), y, 0xFFFFFF);
            }
        }
    }

    /** 「Aが入力中　Bが返信中　Geminiが考え中...」 */
    private static String typingLine(boolean watut) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener c = mc.getConnection();
        if (c == null || mc.level == null) return "";

        List<String> typers = new ArrayList<>();
        List<String> repliers = new ArrayList<>();
        if (watut) {
            for (UUID uuid : WatutCompat.typingPlayers()) {
                PlayerInfo info = c.getPlayerInfo(uuid);
                if (info == null) continue;
                String name = info.getProfile().getName();
                if (REMOTE_REPLYING.contains(uuid)) repliers.add(name);
                else typers.add(name);
            }
        }

        List<String> parts = new ArrayList<>();
        if (!typers.isEmpty()) parts.add(I18n.get("mentionmod.watut.typing", String.join("、", typers)));
        if (!repliers.isEmpty()) parts.add(I18n.get("mentionmod.watut.replying", String.join("、", repliers)));
        String people = String.join("　", parts);
        if (people.length() > TYPING_CHAR_LIMIT) people = I18n.get("mentionmod.watut.many");

        // AIの状態は人数が多くても省略せずに出す
        List<String> ai = new ArrayList<>();
        long now = System.currentTimeMillis();
        AI_STATUS.entrySet().removeIf(e -> now - e.getValue()[1] > AI_STATUS_TIMEOUT_MS);
        for (Map.Entry<String, long[]> e : AI_STATUS.entrySet()) {
            String key = e.getValue()[0] == 2 ? "mentionmod.ai.replying" : "mentionmod.ai.thinking";
            ai.add(I18n.get(key, e.getKey()));
        }

        List<String> all = new ArrayList<>();
        if (!people.isEmpty()) all.add(people);
        all.addAll(ai);
        if (all.isEmpty()) return "";
        String line = String.join("　", all);

        // Watut と同じ「...」のアニメーション
        int rate = 6;
        long time = mc.level.getGameTime() % (rate * 4);
        StringBuilder anim = new StringBuilder();
        while (time > rate) {
            time -= rate;
            anim.append('.');
        }
        return line + anim;
    }

    private static void onClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof ChatScreen) setTarget(null);
    }

    private static void onSend(ClientChatEvent event) {
        Entry t = target;
        if (t == null) return;
        String msg = event.getMessage();
        if (msg.startsWith("/")) return;
        setTarget(null);

        if (serverHasMod()) {
            try {
                PacketDistributor.sendToServer(new ReplyPayload(t.sender(), shorten(t.text(), 100), msg));
            } catch (Exception ignored) {
            }
        } else if (!msg.toLowerCase(Locale.ROOT).contains("@" + t.sender().toLowerCase(Locale.ROOT))) {
            // サーバーにこのmodが無い時は、せめて @名前 を付けて送る
            event.setMessage("@" + t.sender() + " " + msg);
        }
    }

    private static boolean serverHasMod() {
        ClientPacketListener c = Minecraft.getInstance().getConnection();
        return c != null && c.hasChannel(ReplyPayload.TYPE);
    }

    private static Entry entryOf(Style style) {
        if (style == null || style.getInsertion() == null) return null;
        String ins = style.getInsertion();
        if (!ins.startsWith(TOKEN)) return null;
        try {
            return MESSAGES.get(Integer.parseInt(ins.substring(TOKEN.length())));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String nameOf(UUID uuid) {
        ClientPacketListener c = Minecraft.getInstance().getConnection();
        if (c == null) return null;
        PlayerInfo info = c.getPlayerInfo(uuid);
        return info == null ? null : info.getProfile().getName();
    }

    private static String shorten(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
