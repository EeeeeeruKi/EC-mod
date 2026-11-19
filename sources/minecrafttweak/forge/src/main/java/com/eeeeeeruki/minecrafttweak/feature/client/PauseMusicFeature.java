package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * ポーズ画面で、タイトル画面と同じ曲を流す。
 * タイトル画面で流れていた曲（Aether などのMODの曲も）を覚えておいて、それを流す。
 * 「カスタムメニューBGM」がオンなら、その曲を流す。
 */
public class PauseMusicFeature extends Feature {
    public static PauseMusicFeature INSTANCE;

    public PauseMusicFeature() {
        super("pause_music", FeatureCategory.CLIENT,
                "Plays the title screen music on the pause screen (also the music of mods like the Aether).");
        INSTANCE = this;
    }

    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.MenuMusic.register();
    }
}
