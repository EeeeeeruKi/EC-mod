package com.eeeeeeruki.skyriders.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

/** レースのBGM（ループ再生、音量は「音楽」の設定に従う） */
public final class RaceBgm {
    private RaceBgm() {
    }

    private static SoundInstance current;
    private static String currentId = "";

    public static void play(String id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (id.equals(currentId) && current != null && mc.getSoundManager().isPlaying(current)) return;
        stop();
        Identifier sid = Identifier.tryParse(id);
        if (sid == null || id.isEmpty()) return;
        mc.getMusicTracker().stop();
        current = new PositionedSoundInstance(sid, SoundCategory.MUSIC, 1f, 1f, SoundInstance.createRandom(),
                true, 0, SoundInstance.AttenuationType.NONE, 0, 0, 0, true);
        currentId = id;
        mc.getSoundManager().play(current);
    }

    public static void stop() {
        if (current != null) MinecraftClient.getInstance().getSoundManager().stop(current);
        current = null;
        currentId = "";
    }
}
