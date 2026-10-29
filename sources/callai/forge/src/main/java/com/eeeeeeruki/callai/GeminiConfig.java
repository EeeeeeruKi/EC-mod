package com.eeeeeeruki.callai;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * サーバー設定（ワールドの serverconfig/callai-server.toml に保存されます）
 */
public final class GeminiConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> BOT_NAME;
    public static final ForgeConfigSpec.ConfigValue<String> SYSTEM_PROMPT;
    public static final ForgeConfigSpec.IntValue HISTORY_TURNS;
    public static final ForgeConfigSpec.IntValue COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_REPLY_CHARS;
    public static final ForgeConfigSpec.IntValue TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<String> THINKING_LEVEL;

    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_MODE;
    public static final ForgeConfigSpec.IntValue COMMAND_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue MAX_COMMANDS;
    public static final ForgeConfigSpec.ConfigValue<String> BLOCKED_COMMANDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("gemini");

        API_KEY = b.comment("Gemini APIのキーです。自分でキーを設定していない人が使うデフォルトのキーです。ゲーム内で /gemini default_key set <キー> でも設定できます。")
                .define("apiKey", "");
        MODEL = b.comment("使うモデル名です（例: gemini-flash-latest）。/gemini model <名前> でも変えられます。")
                .define("model", "gemini-flash-latest");
        BOT_NAME = b.comment("ささやきに表示される名前です。")
                .define("botName", "Gemini");
        SYSTEM_PROMPT = b.comment("Geminiへの指示です。{player} は話しかけたプレイヤーの名前に置き換わります。")
                .define("systemPrompt",
                        "あなたはMinecraftのサーバーにいるAIアシスタント「Gemini」です。"
                                + "今あなたはプレイヤー「{player}」から /tell でささやかれています。"
                                + "Minecraftのチャットで読むので、返事は短く（3文くらいまで）してください。"
                                + "マークダウン（**や#や箇条書き）は使わず、普通の文章で書いてください。"
                                + "相手と同じ言語で返事してください。");
        HISTORY_TURNS = b.comment("プレイヤーごとに覚えておく会話の往復数です。0にすると毎回1回きりの返事になります。")
                .defineInRange("historyTurns", 10, 0, 50);
        COOLDOWN_SECONDS = b.comment("同じプレイヤーが次に話しかけられるまでの秒数です。")
                .defineInRange("cooldownSeconds", 3, 0, 3600);
        MAX_REPLY_CHARS = b.comment("返事の最大文字数です。これより長いと切り詰めます。")
                .defineInRange("maxReplyChars", 600, 50, 4000);
        TIMEOUT_SECONDS = b.comment("Geminiの返事を待つ最大秒数です。考える時間が長いモデルは長めにしてください。/gemini timeout <秒> でも変えられます。")
                .defineInRange("replyTimeoutSeconds", 120, 5, 600);
        THINKING_LEVEL = b.comment("Geminiが返事の前に考える量です（default / minimal / low / medium / high）。少ないほど速く返事が来ます。default はモデルにおまかせです。対応していないモデルでは自動で default になります。/gemini thinking <量> でも変えられます。")
                .define("thinkingLevel", "low");

        b.pop();
        b.push("commands");

        COMMAND_MODE = b.comment("Geminiがコマンドを実行できる場面です。all = /tell @gemini・チャットの @gemini・/gemini do のどれでも、do = /gemini do のときだけ、off = 実行しません。/gemini commands mode <all|do|off> でも変えられます。")
                .define("mode", "all");
        COMMAND_MAX_LEVEL = b.comment("Geminiがコマンドを実行するときの権限レベルの上限です（0〜4）。実際の権限は「話しかけた人の権限」とこの上限の低い方になるので、OPでない人がGeminiを使ってOPのコマンドを実行することはできません。/gemini commands level <0〜4> でも変えられます。")
                .defineInRange("maxPermissionLevel", 2, 0, 4);
        MAX_COMMANDS = b.comment("1回の話しかけでGeminiが実行できるコマンドの最大数です。")
                .defineInRange("maxCommandsPerMessage", 8, 1, 50);
        BLOCKED_COMMANDS = b.comment("Geminiに実行させないコマンドの名前です（カンマ区切り）。execute ... run の中に書かれていても止めます。/gemini commands block <名前> と unblock <名前> でも変えられます。")
                .define("blockedCommands", CommandRunner.DEFAULT_BLOCKED);

        b.pop();
        SPEC = b.build();
    }

    private GeminiConfig() {}
}
