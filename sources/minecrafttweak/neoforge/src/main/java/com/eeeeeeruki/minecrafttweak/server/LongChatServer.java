package com.eeeeeeruki.minecrafttweak.server;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.feature.client.LongChatFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 長いチャット・コマンドのサーバー側。
 * チャットは「<名前> 文」の形で全員に流し、コマンドはそのプレイヤーが打ったのと同じように実行する（権限もそのまま）。
 */
public final class LongChatServer {
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private LongChatServer() {
    }

    public static int maxLength() {
        try {
            return LongChatFeature.INSTANCE.maxLength.get();
        } catch (Exception e) {
            return 2048;
        }
    }

    public static void onMessage(ServerPlayer player, boolean command, String text) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (LongChatFeature.INSTANCE == null || !LongChatFeature.INSTANCE.isEnabled()) {
            player.sendSystemMessage(Component.translatable("minecrafttweak.longer_chat.server_off").withStyle(ChatFormatting.RED));
            return;
        }
        if (text.length() > maxLength()) {
            player.sendSystemMessage(Component.translatable("minecrafttweak.longer_chat.too_long", maxLength()).withStyle(ChatFormatting.RED));
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < 500) {
            player.sendSystemMessage(Component.translatable("minecrafttweak.longer_chat.wait").withStyle(ChatFormatting.RED));
            return;
        }
        LAST.put(player.getUUID(), now);
        if (command) {
            String cmd = text.startsWith("/") ? text.substring(1) : text;
            MinecraftTweak.LOGGER.info("minEcraft twEEk: {} issued a long command ({} characters)", player.getName().getString(), cmd.length());
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), cmd);
            return;
        }
        String clean = clean(text);
        if (clean.isBlank()) {
            return;
        }
        Component message = Component.translatable("chat.type.text", player.getDisplayName(), Component.literal(clean));
        server.getPlayerList().broadcastSystemMessage(message, false);
        DiceBot.onChat(player, clean);
    }

    /** バニラのチャットで使えない文字（制御文字と「§」）を取る。 */
    private static String clean(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= ' ' && c != 127 && c != '§') {
                sb.append(c);
            }
        }
        return sb.toString().strip();
    }

    public static void onLogout(UUID uuid) {
        LAST.remove(uuid);
    }
}
