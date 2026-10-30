package com.eeeeeeruki.mentionmod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * チャット本文から @プレイヤー / @OP / @everyone を探して、
 * 色付きのメッセージと通知先のプレイヤーを作る。
 */
public final class MentionParser {
    public static final int KIND_PLAYER = 0;
    public static final int KIND_OP = 1;
    public static final int KIND_EVERYONE = 2;
    public static final int KIND_REPLY = 3;

    /** 直前が英数字・_ のときはメンション扱いしない（メールアドレス対策） */
    private static final Pattern MENTION = Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9_]{1,16})");

    private MentionParser() {}

    /**
     * @param message        色付けしたメッセージ（changed が false なら null）
     * @param targets        通知するプレイヤーと種類（送信者は含まない）
     * @param everyoneDenied OPでない人が @everyone を使った
     * @param changed        メンションが1つ以上あった
     * @param ai             @ai（Call AI。@gemini も可）へのメンションがあった
     */
    public record Result(MutableComponent message, Map<ServerPlayer, Integer> targets,
                         boolean everyoneDenied, boolean changed, boolean ai) {}

    public static boolean isOp(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    public static Result parse(String raw, ServerPlayer sender, MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        Map<String, ServerPlayer> byName = new HashMap<>();
        for (ServerPlayer p : players) {
            byName.put(p.getGameProfile().getName().toLowerCase(Locale.ROOT), p);
        }

        boolean senderIsOp = isOp(sender);
        Map<ServerPlayer, Integer> targets = new LinkedHashMap<>();
        MutableComponent out = Component.empty();
        Matcher m = MENTION.matcher(raw);
        int last = 0;
        boolean changed = false;
        boolean denied = false;
        boolean ai = false;

        while (m.find()) {
            String lower = m.group(1).toLowerCase(Locale.ROOT);
            ChatFormatting color = null;
            String display = m.group();

            if (lower.equals("everyone")) {
                if (senderIsOp) {
                    color = ChatFormatting.GOLD;
                    display = "@everyone";
                    for (ServerPlayer p : players) addTarget(targets, p, KIND_EVERYONE);
                } else {
                    denied = true;
                }
            } else if ((lower.equals("ai") || lower.equals("gemini")) && CallAiBridge.available()) {
                color = ChatFormatting.LIGHT_PURPLE;
                display = "@ai";
                ai = true;
            } else if (lower.equals("op")) {
                color = ChatFormatting.GOLD;
                display = "@OP";
                for (ServerPlayer p : players) {
                    if (isOp(p)) addTarget(targets, p, KIND_OP);
                }
            } else {
                ServerPlayer p = byName.get(lower);
                if (p != null) {
                    color = ChatFormatting.AQUA;
                    display = "@" + p.getGameProfile().getName();
                    addTarget(targets, p, KIND_PLAYER);
                }
            }

            if (color != null) {
                if (m.start() > last) out.append(Component.literal(raw.substring(last, m.start())));
                out.append(Component.literal(display).withStyle(color));
                last = m.end();
                changed = true;
            }
        }

        targets.remove(sender);
        if (!changed) return new Result(null, targets, denied, false, false);
        if (last < raw.length()) out.append(Component.literal(raw.substring(last)));
        return new Result(out, targets, denied, true, ai);
    }

    /** 同じ人に複数の種類が当たったら、より個人向けの種類（数字が小さい方）を使う */
    private static void addTarget(Map<ServerPlayer, Integer> targets, ServerPlayer p, int kind) {
        targets.merge(p, kind, Math::min);
    }
}
