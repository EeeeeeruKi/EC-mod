package com.eeeeeeruki.callai.api;

import com.eeeeeeruki.callai.GeminiChat;
import com.eeeeeeruki.callai.GeminiConfig;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

/**
 * ほかのMOD（Mention Mod など）から Call AI を使うための入口です。
 */
public final class CallAIApi {
    private CallAIApi() {}

    public static String botName() {
        return GeminiConfig.BOT_NAME.get();
    }

    /**
     * Geminiに問い合わせます。callback には (返事, null) か (null, エラーの説明) が渡されます。
     * 問い合わせを始められなかった（キーが無い・クールダウン中など）ときは false を返します。
     */
    public static boolean ask(ServerPlayer player, String message, BiConsumer<String, String> callback) {
        return GeminiChat.askPublic(player, message, callback);
    }

    public static long replyDelayMs(String reply) {
        return GeminiChat.replyDelayMs(reply);
    }
}
