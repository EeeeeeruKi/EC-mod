package com.eeeeeeruki.tonsofadv;

import net.neoforged.neoforge.common.ModConfigSpec;

/** クライアントの設定（config/tonsofadv-client.toml） */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SPECIAL_SOUNDS;
    public static final ModConfigSpec.BooleanValue SOUNDS_AS_RECORDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SPECIAL_SOUNDS = b.comment("エーテルなどのタブの進捗で、そのMODの特別な音を鳴らす（false にすると普通の進捗の音になる）")
                .translation("tonsofadv.configuration.specialSounds")
                .define("specialSounds", true);
        SOUNDS_AS_RECORDS = b.comment("エーテルの進捗の音の音量を「ジュークボックス/音ブロック」で調節する（false にすると「音楽」の音量になる）")
                .translation("tonsofadv.configuration.soundsAsRecords")
                .define("soundsAsRecords", true);
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

    static boolean soundsAsRecords() {
        try {
            return SOUNDS_AS_RECORDS.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
