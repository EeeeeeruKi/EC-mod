package com.eeeeeeruki.picpop;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 見る人ごとの設定（config/picpop-client.toml） */
public final class PicPopConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DEFAULT_SECONDS;
    public static final ModConfigSpec.IntValue CORNER_SIZE;
    public static final ModConfigSpec.IntValue CENTER_SIZE;
    public static final ModConfigSpec.IntValue MAX_FILE_MB;
    public static final ModConfigSpec.BooleanValue ALLOW_SERVER_IMAGES;
    public static final ModConfigSpec.BooleanValue CHAT_BUTTON;
    public static final ModConfigSpec.BooleanValue CHAT_BUTTON_ALL_LINKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("display");
        DEFAULT_SECONDS = b.comment("秒数を指定しなかったときに表示する時間（秒）です。0 にすると消すまで表示します。")
                .defineInRange("defaultSeconds", 10, 0, 86400);
        CORNER_SIZE = b.comment("中央以外に出すときの大きさ（画面に対する%）です。")
                .defineInRange("cornerSizePercent", 35, 1, 100);
        CENTER_SIZE = b.comment("中央に出すときの大きさ（画面に対する%）です。")
                .defineInRange("centerSizePercent", 60, 1, 100);
        b.pop();
        b.push("loading");
        MAX_FILE_MB = b.comment("読み込める画像ファイルの大きさの上限（MB）です。")
                .defineInRange("maxFileMB", 16, 1, 128);
        ALLOW_SERVER_IMAGES = b.comment("サーバー（/image コマンド）から送られた画像を表示するかどうかです。")
                .define("allowServerImages", true);
        b.pop();
        b.push("chat");
        CHAT_BUTTON = b.comment("チャットの画像リンクに［画像を表示］ボタンを付けるかどうかです。")
                .define("chatButton", true);
        CHAT_BUTTON_ALL_LINKS = b.comment("true にすると、画像らしくないリンクにもボタンを付けます。")
                .define("chatButtonForAllLinks", false);
        b.pop();
        SPEC = b.build();
    }

    private PicPopConfig() {}
}
