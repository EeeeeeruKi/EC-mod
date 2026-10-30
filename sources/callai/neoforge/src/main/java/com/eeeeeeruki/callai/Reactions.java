package com.eeeeeeruki.callai;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * プレイヤーが進捗（挑戦など）を達成した時や、死んだ時に、AIにひとこと喋らせます。
 */
public final class Reactions {
    private static final Gson GSON = new Gson();
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();
    private static Set<String> optOut;
    private static Path loadedFrom;

    private Reactions() {}

    // ------------------------------------------------------------------
    // イベントから呼ばれる（サーバーのスレッド）
    // ------------------------------------------------------------------

    /** frame は task / goal / challenge */
    public static void onAdvancement(ServerPlayer player, String frame, String title, String description) {
        String mode = norm(GeminiConfig.REACT_ADV_MODE.get());
        if (mode.equals("off") || !frameAllowed(frame)) return;
        if (!mode.equals("praise") && !mode.equals("tease")) mode = ThreadLocalRandom.current().nextBoolean() ? "praise" : "tease";
        String kind = switch (frame) {
            case "challenge" -> "挑戦";
            case "goal" -> "目標";
            default -> "進捗";
        };
        String style = mode.equals("praise")
                ? "思いきり褒めてください。すごさが伝わるように。"
                : "軽く煽ってください（「やっとか」「遅くない？」のような冗談っぽいからかい）。最後はちょっとだけ認めてあげてください。";
        String msg = "プレイヤー「" + player.getGameProfile().getName() + "」が、" + kind + "「" + title + "」を達成しました。"
                + (description.isBlank() ? "" : "（説明: " + description + "）")
                + "\nこのことについて、" + style;
        react(player, msg);
    }

    public static void onDeath(ServerPlayer player, String deathMessage) {
        String mode = norm(GeminiConfig.REACT_DEATH_MODE.get());
        if (mode.equals("off")) return;
        if (!mode.equals("tease") && !mode.equals("comfort")) mode = ThreadLocalRandom.current().nextBoolean() ? "tease" : "comfort";
        String style = mode.equals("comfort")
                ? "やさしく慰めて、次はがんばろうと励ましてください。"
                : "死に方をネタに、軽く煽ってください（冗談っぽく、本気で傷つけない程度に）。";
        String msg = "プレイヤー「" + player.getGameProfile().getName() + "」が死にました。死亡メッセージ: 「" + deathMessage + "」"
                + "\nこのことについて、" + style;
        react(player, msg);
    }

    private static void react(ServerPlayer player, String message) {
        if (isOptedOut(player)) return;
        int chance = GeminiConfig.REACT_CHANCE.get();
        if (chance <= 0 || ThreadLocalRandom.current().nextInt(100) >= chance) return;
        long now = System.currentTimeMillis();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < GeminiConfig.REACT_COOLDOWN.get() * 1000L) return;
        LAST.put(player.getUUID(), now);

        String bot = GeminiConfig.BOT_NAME.get();
        String system = "あなたはMinecraftのサーバーにいるAI「" + bot + "」です。"
                + "サーバーで起きたできごとに、ひとことだけ反応します。"
                + "Minecraftのチャットで読むので、1〜2文の短い返事にしてください。"
                + "マークダウンは使わず、名前を呼びかけてもかまいません。"
                + "煽るときも、差別的なこと・ひどい悪口・本気で傷つけることは言わないでください。"
                + "日本語で返事してください。";


        system = system + Memory.promptFor(player);
        MinecraftServer server = player.getServer();
        UUID uuid = player.getUUID();
        GeminiChat.oneShot(player, system, message).whenComplete((text, err) -> server.execute(() -> {
            if (err != null || text == null || text.isBlank()) {
                if (err != null) CallAI.LOGGER.warn("Call AI reaction failed: {}", err.toString());
                return;
            }
            if (norm(GeminiConfig.REACT_OUTPUT.get()).equals("whisper")) {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p != null) {
                    p.sendSystemMessage(Component.translatable("commands.message.display.incoming",
                                    Component.literal(bot), Component.literal(text))
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            } else {
                server.getPlayerList().broadcastSystemMessage(Component.literal("<" + bot + "> " + text), false);
            }
        }));
    }

    private static boolean frameAllowed(String frame) {
        String scope = norm(GeminiConfig.REACT_ADV_SCOPE.get());
        return switch (scope) {
            case "task" -> true;
            case "goal" -> frame.equals("goal") || frame.equals("challenge");
            default -> frame.equals("challenge");
        };
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------
    // /ai react
    // ------------------------------------------------------------------

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("react")
                .executes(Reactions::status)
                // 誰でも: 自分に反応してほしいか
                .then(Commands.literal("me")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    boolean on = BoolArgumentType.getBool(ctx, "enabled");
                                    setOptOut(p, !on);
                                    ctx.getSource().sendSuccess(() -> Component.literal(on
                                            ? "あなたが進捗を達成したり死んだりした時に、AIが反応するようにしました。"
                                            : "あなたにはAIが反応しないようにしました。"), false);
                                    return 1;
                                })))
                // OP: 設定
                .then(Commands.literal("advancement")
                        .requires(s -> s.hasPermission(2))
                        .then(option("off", GeminiConfig.REACT_ADV_MODE::set, GeminiConfig.REACT_ADV_MODE::save, () -> "進捗を達成した時は反応しないようにしました。"))
                        .then(option("praise", GeminiConfig.REACT_ADV_MODE::set, GeminiConfig.REACT_ADV_MODE::save, () -> "進捗を達成した時に、AIが褒めるようにしました。"))
                        .then(option("tease", GeminiConfig.REACT_ADV_MODE::set, GeminiConfig.REACT_ADV_MODE::save, () -> "進捗を達成した時に、AIが煽るようにしました。"))
                        .then(option("mixed", GeminiConfig.REACT_ADV_MODE::set, GeminiConfig.REACT_ADV_MODE::save, () -> "進捗を達成した時に、AIが褒めたり煽ったりするようにしました。"))
                        .then(Commands.literal("scope")
                                .then(option("challenge", GeminiConfig.REACT_ADV_SCOPE::set, GeminiConfig.REACT_ADV_SCOPE::save, () -> "反応する進捗を「挑戦」だけにしました。"))
                                .then(option("goal", GeminiConfig.REACT_ADV_SCOPE::set, GeminiConfig.REACT_ADV_SCOPE::save, () -> "反応する進捗を「目標」と「挑戦」にしました。"))
                                .then(option("task", GeminiConfig.REACT_ADV_SCOPE::set, GeminiConfig.REACT_ADV_SCOPE::save, () -> "反応する進捗を、すべての進捗にしました。"))))
                .then(Commands.literal("death")
                        .requires(s -> s.hasPermission(2))
                        .then(option("off", GeminiConfig.REACT_DEATH_MODE::set, GeminiConfig.REACT_DEATH_MODE::save, () -> "死んだ時は反応しないようにしました。"))
                        .then(option("tease", GeminiConfig.REACT_DEATH_MODE::set, GeminiConfig.REACT_DEATH_MODE::save, () -> "死んだ時に、AIが煽るようにしました。"))
                        .then(option("comfort", GeminiConfig.REACT_DEATH_MODE::set, GeminiConfig.REACT_DEATH_MODE::save, () -> "死んだ時に、AIが慰めるようにしました。"))
                        .then(option("mixed", GeminiConfig.REACT_DEATH_MODE::set, GeminiConfig.REACT_DEATH_MODE::save, () -> "死んだ時に、AIが煽ったり慰めたりするようにしました。")))
                .then(Commands.literal("output")
                        .requires(s -> s.hasPermission(2))
                        .then(option("chat", GeminiConfig.REACT_OUTPUT::set, GeminiConfig.REACT_OUTPUT::save, () -> "AIの反応を、チャットで全員に見えるようにしました。"))
                        .then(option("whisper", GeminiConfig.REACT_OUTPUT::set, GeminiConfig.REACT_OUTPUT::save, () -> "AIの反応を、その人にだけささやくようにしました。")))
                .then(Commands.literal("chance")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    int v = IntegerArgumentType.getInteger(ctx, "percent");
                                    GeminiConfig.REACT_CHANCE.set(v);
                                    GeminiConfig.REACT_CHANCE.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("AIが反応する確率を " + v + "% にしました。"), true);
                                    return 1;
                                })))
                .then(Commands.literal("cooldown")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 3600))
                                .executes(ctx -> {
                                    int v = IntegerArgumentType.getInteger(ctx, "seconds");
                                    GeminiConfig.REACT_COOLDOWN.set(v);
                                    GeminiConfig.REACT_COOLDOWN.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "同じ人に次に反応するまでの秒数を " + v + "秒にしました。"), true);
                                    return 1;
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> option(String value,
                                                                    java.util.function.Consumer<String> set,
                                                                    Runnable save, Supplier<String> message) {
        return Commands.literal(value).executes(ctx -> {
            set.accept(value);
            save.run();
            ctx.getSource().sendSuccess(() -> Component.literal(message.get()), true);
            return 1;
        });
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        StringBuilder sb = new StringBuilder("AIの反応 / 進捗: ").append(GeminiConfig.REACT_ADV_MODE.get())
                .append("（対象: ").append(GeminiConfig.REACT_ADV_SCOPE.get()).append("）")
                .append(" / 死んだ時: ").append(GeminiConfig.REACT_DEATH_MODE.get())
                .append(" / 出し方: ").append(GeminiConfig.REACT_OUTPUT.get())
                .append(" / 確率: ").append(GeminiConfig.REACT_CHANCE.get()).append("%")
                .append(" / 間隔: ").append(GeminiConfig.REACT_COOLDOWN.get()).append("秒");
        if (src.getEntity() instanceof ServerPlayer p) {
            sb.append(" / あなたへの反応: ").append(isOptedOut(p) ? "オフ" : "オン").append("（/ai react me true|false）");
        }
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    // ------------------------------------------------------------------
    // 反応してほしくない人（serverconfig/callai-react-optout.json）
    // ------------------------------------------------------------------

    private static synchronized boolean isOptedOut(ServerPlayer player) {
        return load(player.getServer()).contains(player.getUUID().toString());
    }

    private static synchronized void setOptOut(ServerPlayer player, boolean out) {
        Set<String> set = load(player.getServer());
        if (out) set.add(player.getUUID().toString());
        else set.remove(player.getUUID().toString());
        save();
    }

    public static synchronized void unload() {
        optOut = null;
        loadedFrom = null;
        LAST.clear();
    }

    private static Set<String> load(MinecraftServer server) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("callai-react-optout.json");
        if (optOut != null && file.equals(loadedFrom)) return optOut;
        optOut = new HashSet<>();
        loadedFrom = file;
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                List<String> list = GSON.fromJson(r, new TypeToken<List<String>>() {}.getType());
                if (list != null) optOut.addAll(list);
            } catch (Exception e) {
                CallAI.LOGGER.warn("Failed to read callai-react-optout.json: {}", e.toString());
            }
        }
        return optOut;
    }

    private static void save() {
        if (optOut == null || loadedFrom == null) return;
        try {
            Files.createDirectories(loadedFrom.getParent());
            try (Writer w = Files.newBufferedWriter(loadedFrom, StandardCharsets.UTF_8)) {
                GSON.toJson(optOut.stream().sorted().toList(), w);
            }
        } catch (Exception e) {
            CallAI.LOGGER.warn("Failed to save callai-react-optout.json: {}", e.toString());
        }
    }
}
