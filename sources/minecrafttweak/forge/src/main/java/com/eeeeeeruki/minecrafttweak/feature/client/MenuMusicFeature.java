package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.ForgeConfigSpec;

/** タイトル画面などのメニューで、URL（またはファイル）の曲を流す。 */
public class MenuMusicFeature extends Feature {
    public static MenuMusicFeature INSTANCE;
    public ForgeConfigSpec.ConfigValue<String> url;
    public ForgeConfigSpec.IntValue volume;
    public ForgeConfigSpec.BooleanValue loop;
    public ForgeConfigSpec.BooleanValue keepPlaying;

    public MenuMusicFeature() {
        super("custom_menu_music", FeatureCategory.CLIENT,
                "Plays your own music (an .ogg, .mp3 or .wav URL or file path) on the menu screens.");
        INSTANCE = this;
    }

    @Override
    protected void addOptions(ForgeConfigSpec.Builder builder) {
        url = fileOption(builder, "url", "URL or file path of an .ogg / .mp3 / .wav file.", "", "*.ogg", "*.mp3", "*.wav");
        volume = intOption(builder, "volume", "Volume in percent (multiplied by the music volume).", 100, 0, 100);
        loop = boolOption(builder, "loop", "Play the music again when it ends.", true);
        keepPlaying = boolOption(builder, "keep_playing", "Keep the music playing everywhere: in worlds, after unpausing and in setting screens.", false);
    }

    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.MenuMusic.register();
    }
}
