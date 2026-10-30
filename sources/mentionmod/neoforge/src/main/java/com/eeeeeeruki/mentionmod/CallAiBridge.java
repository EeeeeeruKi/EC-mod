package com.eeeeeeruki.mentionmod;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

/**
 * Call AI との連携。Call AI（v1.4.0 以降）が入っていると、チャットの @ai（@gemini）に AI が返事をする。
 * Call AI が無くても動くように、リフレクションで呼ぶ。
 */
public final class CallAiBridge {
    private static boolean checked;
    private static Method botName;
    private static Method ask;
    private static Method replyDelayMs;

    private CallAiBridge() {}

    public static boolean available() {
        if (!checked) {
            checked = true;
            if (ModList.get().isLoaded("callai")) {
                try {
                    Class<?> api = Class.forName("com.eeeeeeruki.callai.api.CallAIApi");
                    botName = api.getMethod("botName");
                    ask = api.getMethod("ask", ServerPlayer.class, String.class, BiConsumer.class);
                    replyDelayMs = api.getMethod("replyDelayMs", String.class);
                } catch (Throwable t) {
                    ask = null;
                }
            }
        }
        return ask != null;
    }

    public static String botName() {
        try {
            Object name = botName.invoke(null);
            if (name instanceof String s && !s.isBlank()) return s;
        } catch (Throwable ignored) {
        }
        return "AI";
    }

    /** 返事は done に (返事, null) か (null, 失敗の理由) で届く。話しかけられなかったら false */
    public static boolean ask(ServerPlayer player, String message, BiConsumer<String, String> done) {
        try {
            return Boolean.TRUE.equals(ask.invoke(null, player, message, done));
        } catch (Throwable t) {
            return false;
        }
    }

    public static long replyDelayMs(String text) {
        try {
            return ((Number) replyDelayMs.invoke(null, text)).longValue();
        } catch (Throwable t) {
            return 1500;
        }
    }
}
