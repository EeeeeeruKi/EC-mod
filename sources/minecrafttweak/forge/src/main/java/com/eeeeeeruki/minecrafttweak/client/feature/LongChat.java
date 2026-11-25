package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.feature.client.LongChatFeature;
import com.eeeeeeruki.minecrafttweak.network.LongChatNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * チャットの文字数を増やす（クライアント側）。
 * チャット欄の上限を設定の文字数にして、256文字を超えたものだけ、ここで送り方を変える（ChatScreenMixin から呼ぶ）。
 */
public final class LongChat {
    public static final int VANILLA = 256;
    /** サーバーにこのmodがないときに、分けて送る残り。 */
    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static int cooldown;
    private static boolean registered;

    private LongChat() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                onTick();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> QUEUE.clear());
    }

    public static boolean enabled() {
        return LongChatFeature.INSTANCE != null && LongChatFeature.INSTANCE.isEnabled();
    }

    /** チャット欄に入れられる文字数。 */
    public static int maxLength() {
        if (!enabled()) {
            return VANILLA;
        }
        try {
            return Math.max(VANILLA, Math.min(LongChatNetwork.MAX, LongChatFeature.INSTANCE.maxLength.get()));
        } catch (Exception e) {
            return VANILLA;
        }
    }

    public static boolean serverSupports() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && LongChatNetwork.CHANNEL.isRemotePresent(connection.getConnection());
    }

    /** 送るとき。256文字を超えていて、こちらで送ったなら true（バニラでは送らない）。 */
    public static boolean onSend(String text, boolean addToRecentChat) {
        if (!enabled() || text == null || text.length() <= VANILLA) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return false;
        }
        if (addToRecentChat) {
            mc.gui.getChat().addRecentChat(text);
        }
        boolean command = text.startsWith("/");
        if (serverSupports()) {
            LongChatNetwork.send(command, command ? text.substring(1) : text);
            return true;
        }
        if (command) {
            mc.gui.getChat().addMessage(Component.translatable("minecrafttweak.longer_chat.command_too_long", text.length())
                    .withStyle(ChatFormatting.RED));
            return true;
        }
        List<String> parts = split(text);
        mc.gui.getChat().addMessage(Component.translatable("minecrafttweak.longer_chat.splitting", parts.size())
                .withStyle(ChatFormatting.GRAY));
        QUEUE.addAll(parts);
        return true;
    }

    /**
     * ほかの機能からチャットに文を送る（長さはなんでもいい）。
     * 256文字以下ならそのまま、長いときは「チャットの文字数を増やす」がオンでサーバーにこのmodがあればまるごと、
     * そうでなければ256文字ずつに分けて1秒ごとに送る。
     */
    public static void sendText(String text) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || text == null || text.isBlank()) {
            return;
        }
        if (text.length() <= VANILLA) {
            connection.sendChat(text);
        } else if (enabled() && serverSupports()) {
            LongChatNetwork.send(false, text);
        } else {
            QUEUE.addAll(split(text));
        }
    }

    /** 256文字ずつに分ける（できれば空白のところで）。 */
    static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        String rest = text;
        while (rest.length() > VANILLA) {
            int cut = rest.lastIndexOf(' ', VANILLA);
            if (cut < VANILLA / 2) {
                cut = VANILLA;
                // サロゲートペアの途中で切らない
                if (Character.isHighSurrogate(rest.charAt(cut - 1))) {
                    cut--;
                }
            }
            String part = rest.substring(0, cut).strip();
            if (!part.isEmpty()) {
                parts.add(part);
            }
            rest = rest.substring(cut).strip();
        }
        if (!rest.isEmpty()) {
            parts.add(rest);
        }
        return parts;
    }

    /** 分けたものを1秒に1つずつ送る（速く送りすぎるとサーバーにキックされる）。 */
    private static void onTick() {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (QUEUE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            QUEUE.clear();
            return;
        }
        connection.sendChat(QUEUE.poll());
        cooldown = 20;
    }
}
