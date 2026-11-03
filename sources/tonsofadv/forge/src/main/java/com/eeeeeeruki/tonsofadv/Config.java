package com.eeeeeeruki.tonsofadv;

import net.minecraftforge.common.ForgeConfigSpec;

/** クライアントの設定（config/tonsofadv-client.toml） */
public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SPECIAL_SOUNDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        SPECIAL_SOUNDS = b.comment("エーテルなどのタブの進捗で、そのMODの特別な音を鳴らす（false にすると普通の進捗の音になる）")
                .translation("tonsofadv.configuration.specialSounds")
                .define("specialSounds", true);
        SPEC = b.build();
    }

    private Config() {
    }

    static boolean specialSounds() {
        try {
            return SPECIAL_SOUNDS.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
