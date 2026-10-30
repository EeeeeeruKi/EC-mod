package com.eeeeeeruki.callai;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * サーバー設定（ワールドの serverconfig/callai-server.toml に保存されます）
 */
public final class GeminiConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> API_KEY;
    public static final ModConfigSpec.ConfigValue<String> MODEL;
    public static final ModConfigSpec.ConfigValue<String> BOT_NAME;
    public static final ModConfigSpec.ConfigValue<String> SYSTEM_PROMPT;
    public static final ModConfigSpec.IntValue HISTORY_TURNS;
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue MAX_REPLY_CHARS;
    public static final ModConfigSpec.IntValue TIMEOUT_SECONDS;
    public static final ModConfigSpec.ConfigValue<String> THINKING_LEVEL;

    public static final ModConfigSpec.ConfigValue<String> PROVIDER;
    public static final ModConfigSpec.ConfigValue<String> OPENAI_BASE_URL;
    public static final ModConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ModConfigSpec.ConfigValue<String> OPENAI_API_KEY;

    public static final ModConfigSpec.ConfigValue<String> COMMAND_MODE;
    public static final ModConfigSpec.IntValue COMMAND_MAX_LEVEL;
    public static final ModConfigSpec.IntValue MAX_COMMANDS;
    public static final ModConfigSpec.ConfigValue<String> BLOCKED_COMMANDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("gemini");

        API_KEY = b.comment("Gemini APIのキーです。自分でキーを設定していない人が使うデフォルトのキーです。ゲーム内で /ai default_key set <キー> でも設定できます。")
                .define("apiKey", "");
        MODEL = b.comment("使うモデル名です（例: gemini-flash-latest）。provider が gemini のときに使います。/ai model <名前> でも変えられます。")
                .define("model", "gemini-flash-latest");
        BOT_NAME = b.comment("ささやきに表示される名前です。")
                .define("botName", "AI");
        SYSTEM_PROMPT = b.comment("AIへの指示です。{player} は話しかけたプレイヤーの名前、{bot} は botName に置き換わります。")
                .define("systemPrompt",
                        "あなたはMinecraftのサーバーにいるAIアシスタント「{bot}」です。"
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
        TIMEOUT_SECONDS = b.comment("AIの返事を待つ最大秒数です。考える時間が長いモデルは長めにしてください。/ai timeout <秒> でも変えられます。")
                .defineInRange("replyTimeoutSeconds", 120, 5, 600);
        THINKING_LEVEL = b.comment("AIが返事の前に考える量です（default / minimal / low / medium / high）。少ないほど速く返事が来ます。default はモデルにおまかせです。対応していないモデルでは自動で default になります。/ai thinking <量> でも変えられます。")
                .define("thinkingLevel", "low");

        b.pop();
        b.push("provider");

        PROVIDER = b.comment("つなぐ先です。gemini = Google の Gemini API、openai = OpenAI互換のAPI（OpenAI・Ollama・LM Studio・llama.cpp など）。/ai provider <gemini|openai> でも変えられます。")
                .define("type", "gemini");
        OPENAI_BASE_URL = b.comment("OpenAI互換APIのURLです。/chat/completions の手前まで書きます（例: http://localhost:11434/v1 、https://api.openai.com/v1）。/ai openai url <URL> でも変えられます。")
                .define("openaiBaseUrl", "http://localhost:11434/v1");
        OPENAI_MODEL = b.comment("OpenAI互換APIで使うモデル名です（例: qwen3:4b 、gpt-4o-mini）。/ai model <名前> でも変えられます。")
                .define("openaiModel", "");
        OPENAI_API_KEY = b.comment("OpenAI互換APIのキーです。いらないサーバー（Ollama など）なら空のままで大丈夫です。プレイヤーが /ai key set で設定したGeminiのキーは、OpenAI互換のサーバーには送りません。/ai openai key <キー> でも設定できます。")
                .define("openaiApiKey", "");

        b.pop();
        b.push("commands");

        COMMAND_MODE = b.comment("AIがコマンドを実行できる場面です。all = /tell @ai・チャットの @ai・/ai do のどれでも、do = /ai do のときだけ、off = 実行しません。/ai commands mode <all|do|off> でも変えられます。")
                .define("mode", "all");
        COMMAND_MAX_LEVEL = b.comment("AIがコマンドを実行するときの権限レベルの上限です（0〜4）。実際の権限は「話しかけた人の権限」とこの上限の低い方になるので、OPでない人がAIを使ってOPのコマンドを実行することはできません。/ai commands level <0〜4> でも変えられます。")
                .defineInRange("maxPermissionLevel", 2, 0, 4);
        MAX_COMMANDS = b.comment("1回の話しかけでAIが実行できるコマンドの最大数です。")
                .defineInRange("maxCommandsPerMessage", 8, 1, 50);
        BLOCKED_COMMANDS = b.comment("AIに実行させないコマンドの名前です（カンマ区切り）。execute ... run の中に書かれていても止めます。/ai commands block <名前> と unblock <名前> でも変えられます。")
                .define("blockedCommands", CommandRunner.DEFAULT_BLOCKED);

        b.pop();
        SPEC = b.build();
    }

    private GeminiConfig() {}
}
