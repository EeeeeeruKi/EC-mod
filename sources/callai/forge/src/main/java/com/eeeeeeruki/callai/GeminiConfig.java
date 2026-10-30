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

    public static final ForgeConfigSpec.ConfigValue<String> PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_BASE_URL;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;

    public static final ForgeConfigSpec.ConfigValue<String> REACT_ADV_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> REACT_ADV_SCOPE;
    public static final ForgeConfigSpec.ConfigValue<String> REACT_DEATH_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> REACT_OUTPUT;
    public static final ForgeConfigSpec.IntValue REACT_CHANCE;
    public static final ForgeConfigSpec.IntValue REACT_COOLDOWN;

    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_MODE;
    public static final ForgeConfigSpec.IntValue COMMAND_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue MAX_COMMANDS;
    public static final ForgeConfigSpec.ConfigValue<String> BLOCKED_COMMANDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
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
        b.push("reactions");

        REACT_ADV_MODE = b.comment("プレイヤーが進捗を達成した時のAIの反応です。off = 反応しない、praise = 褒める、tease = 煽る、mixed = 褒めたり煽ったり。/ai react advancement <off|praise|tease|mixed> でも変えられます。")
                .define("advancement", "mixed");
        REACT_ADV_SCOPE = b.comment("反応する進捗の種類です。task = すべての進捗、goal = 目標と挑戦、challenge = 挑戦だけ。/ai react advancement scope <challenge|goal|task> でも変えられます。")
                .define("advancementTypes", "task");
        REACT_DEATH_MODE = b.comment("プレイヤーが死んだ時のAIの反応です。off = 反応しない、tease = 煽る、comfort = 慰める、mixed = 煽ったり慰めたり。/ai react death <off|tease|comfort|mixed> でも変えられます。")
                .define("death", "mixed");
        REACT_OUTPUT = b.comment("反応の出し方です。chat = チャットで全員に見える、whisper = その人にだけささやく。/ai react output <chat|whisper> でも変えられます。")
                .define("output", "chat");
        REACT_CHANCE = b.comment("反応する確率（%）です。/ai react chance <0〜100> でも変えられます。")
                .defineInRange("chance", 100, 0, 100);
        REACT_COOLDOWN = b.comment("同じプレイヤーに次に反応するまでの秒数です（APIの使いすぎを防ぎます）。/ai react cooldown <秒> でも変えられます。")
                .defineInRange("cooldownSeconds", 10, 0, 3600);

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
