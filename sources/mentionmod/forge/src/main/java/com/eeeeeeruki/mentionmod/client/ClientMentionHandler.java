package com.eeeeeeruki.mentionmod.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public final class ClientMentionHandler {
    private static final long SOUND_INTERVAL_MS = 300;
    private static long lastSound = 0;

    private ClientMentionHandler() {}

    public static void onMention(String sender, String message, int kind) {
        Minecraft mc = Minecraft.getInstance();

        // ゲーム内でも「ポーン」と鳴らす（まとめて届いた時に重ならないように少し間をあける）
        long now = System.currentTimeMillis();
        if (now - lastSound > SOUND_INTERVAL_MS) {
            lastSound = now;
            try {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.6F));
            } catch (Throwable ignored) {
            }
        }

        // ゲームを見ている時は、音と色付きのチャットだけにする
        if (mc.isWindowActive()) return;

        try {
            // タスクバーのアイコンを点滅させる
            GLFW.glfwRequestWindowAttention(mc.getWindow().getWindow());
        } catch (Throwable ignored) {
        }

        String key = switch (kind) {
            case 1 -> "mentionmod.notify.op";
            case 2 -> "mentionmod.notify.everyone";
            case 3 -> "mentionmod.notify.reply";
            default -> "mentionmod.notify.player";
        };
        String title = I18n.get(key, sender);
        String body = ChatFormatting.stripFormatting(message);
        WindowsNotifier.show(title, body == null ? "" : body);
    }
}
