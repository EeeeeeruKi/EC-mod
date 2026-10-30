package com.eeeeeeruki.callai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AIが覚えていること（serverconfig/callai-memory.json）。
 * プレイヤーごとの「その人について覚えていること」と、サーバーのみんなで共有の「サーバーについて覚えていること」があります。
 * 覚えたことは毎回AIへの指示に入るので、ログアウトやサーバーの再起動のあとも覚えています。
 */
public final class Memory {
    public static final String TOOL_NAME = "remember";
    private static final int MAX_PLAYER = 30;
    private static final int MAX_SERVER = 50;
    private static final int MAX_CHARS = 200;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 保存する中身 */
    private static final class Data {
        Map<String, List<String>> players = new LinkedHashMap<>();
        List<String> server = new ArrayList<>();
    }

    private static Data data;
    private static Path loadedFrom;

    private Memory() {}

    // ------------------------------------------------------------------
    // 読み書き
    // ------------------------------------------------------------------

    public static synchronized List<String> of(ServerPlayer player) {
        return new ArrayList<>(load(player.getServer()).players.getOrDefault(player.getUUID().toString(), List.of()));
    }

    public static synchronized List<String> server(MinecraftServer server) {
        return new ArrayList<>(load(server).server);
    }

    /** 覚えます。いっぱいなら一番古いものを忘れます。追加した文を返します */
    public static synchronized String add(ServerPlayer player, boolean shared, String text) {
        String t = text == null ? "" : text.replace("\n", " ").trim();
        if (t.isEmpty()) return "";
        if (t.length() > MAX_CHARS) t = t.substring(0, MAX_CHARS);
        Data d = load(player.getServer());
        List<String> list = shared ? d.server : d.players.computeIfAbsent(player.getUUID().toString(), k -> new ArrayList<>());
        if (!list.contains(t)) list.add(t);
        int max = shared ? MAX_SERVER : MAX_PLAYER;
        while (list.size() > max) list.remove(0);
        save();
        return t;
    }

    /** 番号（1から）で忘れます。忘れた文を返します（無ければ null） */
    public static synchronized String remove(MinecraftServer server, String uuidOrNull, int number) {
        Data d = load(server);
        List<String> list = uuidOrNull == null ? d.server : d.players.get(uuidOrNull);
        if (list == null || number < 1 || number > list.size()) return null;
        String removed = list.remove(number - 1);
        save();
        return removed;
    }

    public static synchronized void clear(MinecraftServer server, String uuidOrNull) {
        Data d = load(server);
        if (uuidOrNull == null) d.server.clear();
        else d.players.remove(uuidOrNull);
        save();
    }

    public static synchronized void unload() {
        data = null;
        loadedFrom = null;
    }

    // ------------------------------------------------------------------
    // AIに渡すもの
    // ------------------------------------------------------------------

    /** systemPrompt の後ろに付ける、覚えていることの一覧 */
    public static String promptFor(ServerPlayer player) {
        List<String> mine = of(player);
        List<String> shared = server(player.getServer());
        StringBuilder sb = new StringBuilder();
        if (!shared.isEmpty() || !mine.isEmpty()) {
            sb.append("\n\n【あなたが覚えていること】");
            if (!shared.isEmpty()) sb.append("\nサーバーについて: ").append(String.join(" / ", shared));
            if (!mine.isEmpty()) {
                sb.append("\n").append(player.getGameProfile().getName()).append("について: ").append(String.join(" / ", mine));
            }
        }
        return sb.toString();
    }

    /** ツールが使えるときの説明 */
    public static String toolAddendum() {
        return "\n\n【覚える】プレイヤーに「覚えて」と頼まれた時や、名前・好きなもの・拠点の座標・約束など、あとで役に立ちそうなことを教えてもらった時は、"
                + "remember ツールで覚えてください。覚えたことは次に話すときも使えます。";
    }

    /** ツールを使わないとき（OpenAI互換）の、返事に目印を書いてもらう方法の説明 */
    public static String tagAddendum() {
        return "\n\n【覚える】プレイヤーに「覚えて」と頼まれた時や、名前・好きなもの・拠点の座標・約束など、あとで役に立ちそうなことを教えてもらった時は、"
                + "返事の最後に [覚える: 覚えること] と書いてください（サーバーのみんなに関係することは [覚える(サーバー): 覚えること]）。"
                + "この目印はプレイヤーには見えず、次に話すときも思い出せるようになります。";
    }

    /** 返事の中の [覚える: …] を取り出して覚え、目印を消した返事を返します（サーバーのスレッドで） */
    public static final java.util.regex.Pattern TAG =
            java.util.regex.Pattern.compile("[\\[［【]\\s*覚える\\s*(?:[(（]\\s*(サーバー|server)\\s*[)）])?\\s*[:：]\\s*([^\\]］】]+)[\\]］】]");

    public static String takeTags(ServerPlayer player, String reply, String botName) {
        java.util.regex.Matcher m = TAG.matcher(reply);
        if (player != null) {
            while (m.find()) {
                String saved = add(player, m.group(1) != null, m.group(2));
                if (!saved.isEmpty()) {
                    player.sendSystemMessage(Component.literal("📝 " + botName + "が覚えました" + (m.group(1) != null ? "（サーバー）" : "") + ": " + saved)
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
        }
        return TAG.matcher(reply).replaceAll("").trim();
    }

    public static JsonObject declaration() {
        JsonObject text = new JsonObject();
        text.addProperty("type", "string");
        text.addProperty("description", "覚えること。1文で短く（例: 拠点の座標は 120 64 -300 / 好きな食べ物はステーキ）");
        JsonObject scope = new JsonObject();
        scope.addProperty("type", "string");
        JsonArray values = new JsonArray();
        values.add("player");
        values.add("server");
        scope.add("enum", values);
        scope.addProperty("description", "player = 話しかけてきたプレイヤーについてのこと、server = サーバーのみんなに関係すること");
        JsonObject props = new JsonObject();
        props.add("text", text);
        props.add("scope", scope);
        JsonArray required = new JsonArray();
        required.add("text");
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        params.add("properties", props);
        params.add("required", required);

        JsonObject fn = new JsonObject();
        fn.addProperty("name", TOOL_NAME);
        fn.addProperty("description", "あとで思い出せるように、ことがらを覚える（サーバーを再起動しても忘れない）。");
        fn.add("parameters", params);
        return fn;
    }

    /** remember ツールを実行します（サーバーのスレッドで） */
    public static JsonObject run(ServerPlayer player, JsonObject args, String botName) {
        JsonObject r = new JsonObject();
        String text = args.has("text") ? args.get("text").getAsString() : "";
        boolean shared = args.has("scope") && "server".equalsIgnoreCase(args.get("scope").getAsString());
        String saved = add(player, shared, text);
        if (saved.isEmpty()) {
            r.addProperty("success", false);
            r.addProperty("output", "覚えることが空です。");
            return r;
        }
        player.sendSystemMessage(Component.literal("📝 " + botName + "が覚えました" + (shared ? "（サーバー）" : "") + ": " + saved)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        r.addProperty("success", true);
        r.addProperty("output", "覚えました。");
        return r;
    }

    // ------------------------------------------------------------------
    // /ai memory
    // ------------------------------------------------------------------

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("memory")
                .executes(ctx -> list(ctx, false))
                .then(Commands.literal("add")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String t = add(p, false, StringArgumentType.getString(ctx, "text"));
                                    ctx.getSource().sendSuccess(() -> Component.literal("AIに覚えさせました: " + t), false);
                                    return 1;
                                })))
                .then(Commands.literal("remove")
                        .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    return removed(ctx, remove(p.getServer(), p.getUUID().toString(),
                                            IntegerArgumentType.getInteger(ctx, "number")));
                                })))
                .then(Commands.literal("clear")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            clear(p.getServer(), p.getUUID().toString());
                            ctx.getSource().sendSuccess(() -> Component.literal("AIが覚えていたあなたのことを、全部忘れさせました。"), false);
                            return 1;
                        }))
                // サーバーについて覚えていること（見るのは誰でも、変えるのはOP）
                .then(Commands.literal("server")
                        .executes(ctx -> list(ctx, true))
                        .then(Commands.literal("add")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            String t = add(p, true, StringArgumentType.getString(ctx, "text"));
                                            ctx.getSource().sendSuccess(() -> Component.literal("AIにサーバーのこととして覚えさせました: " + t), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("remove")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                        .executes(ctx -> removed(ctx, remove(ctx.getSource().getServer(), null,
                                                IntegerArgumentType.getInteger(ctx, "number"))))))
                        .then(Commands.literal("clear")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    clear(ctx.getSource().getServer(), null);
                                    ctx.getSource().sendSuccess(() -> Component.literal("AIが覚えていたサーバーのことを、全部忘れさせました。"), true);
                                    return 1;
                                })));
    }

    private static int removed(CommandContext<CommandSourceStack> ctx, String text) {
        if (text == null) {
            ctx.getSource().sendFailure(Component.literal("その番号のものはありません。/ai memory で番号を確認してください。"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("AIに忘れさせました: " + text), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx, boolean shared) throws CommandSyntaxException {
        List<String> list = shared ? server(ctx.getSource().getServer()) : of(ctx.getSource().getPlayerOrException());
        if (list.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(shared
                    ? "AIが覚えているサーバーのことは、まだありません。"
                    : "AIが覚えているあなたのことは、まだありません。/ai memory add <覚えてほしいこと> で覚えさせられます。"), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder(shared ? "AIが覚えているサーバーのこと:" : "AIが覚えているあなたのこと:");
        for (int i = 0; i < list.size(); i++) sb.append("\n").append(i + 1).append(". ").append(list.get(i));
        sb.append(shared ? "\n（OPは /ai memory server remove <番号> で忘れさせられます）" : "\n（/ai memory remove <番号> で忘れさせられます）");
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    // ------------------------------------------------------------------

    private static Data load(MinecraftServer server) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("callai-memory.json");
        if (data != null && file.equals(loadedFrom)) return data;
        data = new Data();
        loadedFrom = file;
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d != null) {
                    if (d.players != null) data.players = new LinkedHashMap<>(d.players);
                    if (d.server != null) data.server = new ArrayList<>(d.server);
                }
            } catch (Exception e) {
                CallAI.LOGGER.warn("Failed to read callai-memory.json: {}", e.toString());
            }
        }
        // 読み込んだリストは変更できるものにしておく
        data.players.replaceAll((k, v) -> new ArrayList<>(v));
        return data;
    }

    private static void save() {
        if (data == null || loadedFrom == null) return;
        try {
            Files.createDirectories(loadedFrom.getParent());
            try (Writer w = Files.newBufferedWriter(loadedFrom, StandardCharsets.UTF_8)) {
                GSON.toJson(data, w);
            }
        } catch (Exception e) {
            CallAI.LOGGER.warn("Failed to save callai-memory.json: {}", e.toString());
        }
    }
}
