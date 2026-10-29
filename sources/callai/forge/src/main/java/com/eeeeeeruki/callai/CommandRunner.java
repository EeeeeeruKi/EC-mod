package com.eeeeeeruki.callai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Geminiが頼んだコマンドを、話しかけたプレイヤーとして実行します。
 * 権限は「そのプレイヤーの権限」と設定の上限の低い方です。
 */
public final class CommandRunner {
    public static final String TOOL_NAME = "run_command";

    public static final String DEFAULT_BLOCKED =
            "stop,op,deop,ban,ban-ip,pardon,pardon-ip,whitelist,save-off,save-on,save-all,debug,perf,jfr,publish,transfer,gemini";

    /** 実行の結果をプレイヤーの画面に出す最大行数 */
    private static final int MAX_SHOWN_LINES = 5;
    /** Geminiに返す結果の最大文字数 */
    private static final int MAX_OUTPUT_CHARS = 2000;

    public enum Mode { ALL, DO, OFF }

    private CommandRunner() {}

    public static Mode mode() {
        String m = GeminiConfig.COMMAND_MODE.get().trim().toLowerCase(Locale.ROOT);
        return switch (m) {
            case "do" -> Mode.DO;
            case "off" -> Mode.OFF;
            default -> Mode.ALL;
        };
    }

    /** /gemini do のとき true。それ以外（/tell・チャット）は false */
    public static boolean allowed(boolean viaDo) {
        Mode m = mode();
        return m == Mode.ALL || (m == Mode.DO && viaDo);
    }

    /** このプレイヤーのために実行するときの権限レベル */
    public static int levelFor(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        int own = server == null ? 0 : server.getProfilePermissions(player.getGameProfile());
        return Math.max(0, Math.min(own, GeminiConfig.COMMAND_MAX_LEVEL.get()));
    }

    // ------------------------------------------------------------------
    // 禁止リスト
    // ------------------------------------------------------------------

    public static Set<String> blocked() {
        Set<String> set = new LinkedHashSet<>();
        for (String s : GeminiConfig.BLOCKED_COMMANDS.get().split(",")) {
            String n = normalizeName(s);
            if (!n.isEmpty()) set.add(n);
        }
        return set;
    }

    public static boolean block(String name) {
        Set<String> set = blocked();
        boolean added = set.add(normalizeName(name));
        saveBlocked(set);
        return added;
    }

    public static boolean unblock(String name) {
        Set<String> set = blocked();
        boolean removed = set.remove(normalizeName(name));
        saveBlocked(set);
        return removed;
    }

    private static void saveBlocked(Set<String> set) {
        GeminiConfig.BLOCKED_COMMANDS.set(String.join(",", set));
        GeminiConfig.BLOCKED_COMMANDS.save();
    }

    private static String normalizeName(String s) {
        String n = s.trim().toLowerCase(Locale.ROOT);
        if (n.startsWith("/")) n = n.substring(1);
        if (n.startsWith("minecraft:")) n = n.substring("minecraft:".length());
        return n;
    }

    /** 禁止されているコマンドが入っていれば、その名前を返します（execute ... run の後ろも見ます） */
    static String findBlocked(String command) {
        Set<String> set = blocked();
        List<String> tokens = Arrays.asList(command.trim().split("\\s+"));
        for (int i = 0; i < tokens.size(); i++) {
            if (i == 0 || tokens.get(i - 1).equalsIgnoreCase("run")) {
                String name = normalizeName(tokens.get(i));
                if (set.contains(name)) return name;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Geminiに渡す説明
    // ------------------------------------------------------------------

    /** Geminiに渡す関数の宣言 */
    public static JsonArray declarations() {
        JsonObject command = new JsonObject();
        command.addProperty("type", "string");
        command.addProperty("description",
                "実行するMinecraft（Java版）のコマンド。先頭の / は付けない。例: time set day / give @s minecraft:bread 16 / tp @s 0 80 0");
        JsonObject props = new JsonObject();
        props.add("command", command);
        JsonArray required = new JsonArray();
        required.add("command");
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        params.add("properties", props);
        params.add("required", required);

        JsonObject fn = new JsonObject();
        fn.addProperty("name", TOOL_NAME);
        fn.addProperty("description",
                "話しかけてきたプレイヤーとして、Minecraftのサーバーでコマンドを1つ実行し、実行結果のメッセージを返す。");
        fn.add("parameters", params);

        JsonArray arr = new JsonArray();
        arr.add(fn);
        return arr;
    }

    /** systemPrompt の後ろに付ける、コマンドについての説明 */
    public static String promptAddendum(ServerPlayer player) {
        int level = levelFor(player);
        BlockPos pos = player.blockPosition();
        String levelText = switch (level) {
            case 0 -> "0（一般のプレイヤーと同じ。/time や /give などのOP用のコマンドは使えません）";
            case 1 -> "1";
            case 2 -> "2（OP相当。/time /weather /give /tp /gamemode /effect /summon などが使えます）";
            default -> level + "（管理者）";
        };
        return "\n\n【コマンドの実行】あなたは run_command ツールで、Minecraftのコマンドを実行できます。"
                + "コマンドはプレイヤー「" + player.getGameProfile().getName() + "」として実行され、権限レベルは " + levelText + " です。"
                + "権限が足りないコマンドは失敗します。"
                + "プレイヤーに頼まれた時や、頼まれたことをするのに必要な時だけ使ってください。"
                + "コマンドは先頭の / を付けずに書いてください。@s はこのプレイヤー、~ ~ ~ はこのプレイヤーの今の位置です。"
                + "1回の話しかけで実行できるのは " + GeminiConfig.MAX_COMMANDS.get() + " 個までです。"
                + "実行結果を確かめてから、何をしたかを短く伝えてください。失敗したら、その理由を伝えてください。"
                + "サーバーを止める、ほかのプレイヤーの物を壊す・奪うなど、迷惑になることは頼まれてもしないでください。"
                + "\nこのプレイヤーの今の状態: 座標 " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + "、ディメンション " + player.level().dimension().location()
                + "、ゲームモード " + player.gameMode.getGameModeForPlayer().getName() + "。";
    }

    // ------------------------------------------------------------------
    // 実行（サーバーのスレッドで呼んでください）
    // ------------------------------------------------------------------

    public static JsonObject run(ServerPlayer player, String rawCommand, String botName) {
        JsonObject result = new JsonObject();
        String command = rawCommand == null ? "" : rawCommand.trim();
        while (command.startsWith("/")) command = command.substring(1).trim();

        if (command.isEmpty()) {
            result.addProperty("success", false);
            result.addProperty("output", "コマンドが空です。");
            return result;
        }
        if (command.toLowerCase(Locale.ROOT).contains("@gemini")) {
            result.addProperty("success", false);
            result.addProperty("output", "@gemini を使うコマンドは実行できません。");
            return result;
        }
        String blockedName = findBlocked(command);
        if (blockedName != null) {
            player.sendSystemMessage(Component.literal(botName + "が /" + command + " を実行しようとしましたが、/" + blockedName
                            + " は禁止されているので止めました。")
                    .withStyle(ChatFormatting.YELLOW));
            result.addProperty("success", false);
            result.addProperty("output", "/" + blockedName + " はこのサーバーの設定で禁止されているので、実行しませんでした。");
            return result;
        }

        player.sendSystemMessage(Component.literal("⚙ " + botName + "がコマンドを実行しました: /" + command)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        CallAI.LOGGER.info("[Call AI] {} が Gemini 経由で実行: /{}", player.getGameProfile().getName(), command);

        Capture capture = new Capture(player);
        CommandSourceStack source = player.createCommandSourceStack()
                .withSource(capture)
                .withPermission(levelFor(player));
        try {
            player.getServer().getCommands().performPrefixedCommand(source, command);
        } catch (Exception e) {
            capture.lines.add("エラー: " + e.getMessage());
            capture.failed = true;
        }

        String output = capture.lines.isEmpty() ? "（メッセージは出ませんでした）" : String.join("\n", capture.lines);
        if (output.length() > MAX_OUTPUT_CHARS) output = output.substring(0, MAX_OUTPUT_CHARS) + "…";
        result.addProperty("success", !capture.failed);
        result.addProperty("output", output);
        return result;
    }

    /** コマンドの結果のメッセージを集めつつ、プレイヤーにも見せます */
    private static final class Capture implements CommandSource {
        final ServerPlayer player;
        final List<String> lines = new ArrayList<>();
        boolean failed;
        int shown;

        Capture(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public void sendSystemMessage(Component message) {
            lines.add(message.getString());
            if (message.getStyle().getColor() != null
                    && message.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.RED))) {
                failed = true;
            }
            if (shown < MAX_SHOWN_LINES) {
                shown++;
                player.sendSystemMessage(message);
            }
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return player.shouldInformAdmins();
        }
    }
}
