package com.eeeeeeruki.mentionmod.api;

import com.eeeeeeruki.mentionmod.network.MentionNetwork;
import net.minecraft.server.level.ServerPlayer;

/**
 * ほかのmod（Call AI など）から、AIの状態をプレイヤーの画面に出すための入口。
 * 依存関係を作らずに使えるよう、呼ぶ側はリフレクションで
 * {@code AiStatusApi.setStatus(ServerPlayer, String, int)} を呼ぶ。
 */
public final class AiStatusApi {
    public static final int IDLE = 0;
    public static final int THINKING = 1;
    public static final int REPLYING = 2;

    private AiStatusApi() {}

    /** viewer の画面で「name が考え中／返信中」を出す。IDLE で消える */
    public static void setStatus(ServerPlayer viewer, String name, int state) {
        if (viewer == null || name == null) return;
        MentionNetwork.sendAiStatus(viewer, name, state);
    }
}
