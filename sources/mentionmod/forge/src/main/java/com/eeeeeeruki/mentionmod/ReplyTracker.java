package com.eeeeeeruki.mentionmod;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアントから「次のチャットは〇〇への返信です」と届いた情報を、
 * そのチャットがサーバーに届くまで一時的に持っておく。
 */
public final class ReplyTracker {
    public record Pending(String target, String snippet, String text, long time) {}

    private static final long EXPIRE_MS = 10_000;
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private ReplyTracker() {}

    public static void put(ServerPlayer player, String target, String snippet, String text) {
        PENDING.put(player.getUUID(), new Pending(cut(target, 16), cut(snippet, 100), normalize(text),
                System.currentTimeMillis()));
    }

    /** このチャットが返信なら返信先を返す（1回きり） */
    public static Pending take(ServerPlayer player, String raw) {
        Pending p = PENDING.remove(player.getUUID());
        if (p == null) return null;
        if (System.currentTimeMillis() - p.time() > EXPIRE_MS) return null;
        if (!p.text().equals(normalize(raw))) return null;
        return p;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
