package com.eeeeeeruki.tonsofadv;

import net.neoforged.neoforge.common.ModConfigSpec;

/** クライアントの設定（config/tonsofadv-client.toml） */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SPECIAL_SOUNDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
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
