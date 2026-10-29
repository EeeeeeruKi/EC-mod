package com.eeeeeeruki.callai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Mention Mod が入っていれば、「Geminiが考え中」「Geminiが返信中」を出してもらいます。
 */
final class MentionBridge {
    static final int IDLE = 0;
    static final int THINKING = 1;
    static final int REPLYING = 2;

    private static boolean checked;
    private static Method setStatus;

    private MentionBridge() {}

    static boolean available() {
        if (!checked) {
            checked = true;
            if (ModList.get().isLoaded("mentionmod")) {
                try {
                    setStatus = Class.forName("com.eeeeeeruki.mentionmod.api.AiStatusApi")
                            .getMethod("setStatus", ServerPlayer.class, String.class, int.class);
                } catch (Throwable t) {
                    setStatus = null;
                }
            }
        }
        return setStatus != null;
    }

    static void setStatus(ServerPlayer player, String botName, int status) {
        if (player == null || !available()) return;
        try {
            setStatus.invoke(null, player, botName, status);
        } catch (Throwable ignored) {
        }
    }
}
