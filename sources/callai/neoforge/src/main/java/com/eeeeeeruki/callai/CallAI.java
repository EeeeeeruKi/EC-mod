package com.eeeeeeruki.callai;

import com.eeeeeeruki.callai.network.CallAINetwork;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(CallAI.MOD_ID)
public class CallAI {
    public static final String MOD_ID = "callai";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** /tell @gemini ～ / /msg @gemini ～ / /w @gemini ～ を拾う */
    private static final Pattern TELL_GEMINI = Pattern.compile(
            "^(?:minecraft:)?(?:tell|msg|w)\\s+@gemini(?:\\s+(.*))?$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public CallAI(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, GeminiConfig.SPEC);
        modBus.addListener(CallAINetwork::register);

        NeoForge.EVENT_BUS.addListener(this::onCommand);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onLogout);
        NeoForge.EVENT_BUS.addListener(this::onLogin);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onCommand(CommandEvent event) {
        String input = event.getParseResults().getReader().getString().trim();
        if (input.startsWith("/")) input = input.substring(1);
        Matcher m = TELL_GEMINI.matcher(input);
        if (!m.matches()) return;

        // バニラの/tellには渡さない
        event.setCanceled(true);

        CommandSourceStack src = event.getParseResults().getContext().getSource();
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Geminiにささやけるのはプレイヤーだけです。"));
            return;
        }

        String message = m.group(1) == null ? "" : m.group(1).trim();
        if (message.isEmpty()) {
            player.sendSystemMessage(Component.literal("使い方: /tell @gemini <メッセージ>").withStyle(ChatFormatting.YELLOW));
            return;
        }
        GeminiChat.talk(player, message);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("gemini")
                // 誰でも: 自分の会話をリセット
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            GeminiChat.reset(p.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("Geminiとの会話をリセットしました。"), false);
                            return 1;
                        }))
                // 誰でも: 状態を見る
                .then(Commands.literal("status")
                        .executes(CallAI::status))
                // 誰でも: 画面の読み取り
                .then(Commands.literal("read")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            boolean on = ReadSettings.isEnabled(p);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "画面の読み取り: " + (on ? "オン" : "オフ") + "（/gemini read true か false で切り替えます）"), false);
                            return 1;
                        })
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    boolean on = BoolArgumentType.getBool(ctx, "enabled");
                                    ReadSettings.set(p, on);
                                    String msg = on
                                            ? "画面の読み取りをオンにしました。Geminiに話しかけると、そのときのあなたの画面もGeminiに送られます。"
                                            : "画面の読み取りをオフにしました。";
                                    ctx.getSource().sendSuccess(() -> Component.literal(msg)
                                            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.WHITE), false);
                                    if (on && !CallAINetwork.clientHasMod(p)) {
                                        p.sendSystemMessage(Component.literal("※画面を読み取るには、あなたのMinecraftにもCall AIを入れる必要があります。")
                                                .withStyle(ChatFormatting.YELLOW));
                                    }
                                    return 1;
                                })))
                // 誰でも: Geminiにコマンドでやってもらう
                .then(Commands.literal("do")
                        .then(Commands.argument("instruction", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String text = StringArgumentType.getString(ctx, "instruction").trim();
                                    if (text.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("使い方: /gemini do <やってほしいこと>"));
                                        return 0;
                                    }
                                    GeminiChat.doTask(p, text);
                                    return 1;
                                })))
                // コマンドの実行の設定（見るのは誰でも、変えるのはOP）
                .then(Commands.literal("commands")
                        .executes(CallAI::commandsStatus)
                        .then(Commands.literal("mode")
                                .requires(s -> s.hasPermission(2))
                                .then(commandMode("all"))
                                .then(commandMode("do"))
                                .then(commandMode("off")))
                        .then(Commands.literal("level")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 4))
                                        .executes(ctx -> {
                                            int level = IntegerArgumentType.getInteger(ctx, "level");
                                            GeminiConfig.COMMAND_MAX_LEVEL.set(level);
                                            GeminiConfig.COMMAND_MAX_LEVEL.save();
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Geminiがコマンドを実行するときの権限レベルの上限を " + level
                                                            + " にしました。話しかけた人の権限がこれより低い時は、その人の権限で実行します。"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("block")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                ctx.getSource().getServer().getCommands().getDispatcher().getRoot().getChildren()
                                                        .stream().map(n -> n.getName()).toList(), b))
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            boolean added = CommandRunner.block(name);
                                            ctx.getSource().sendSuccess(() -> Component.literal(added
                                                    ? "/" + name + " をGeminiに実行させないようにしました。"
                                                    : "/" + name + " はもう禁止されています。"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("unblock")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(CommandRunner.blocked(), b))
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            boolean removed = CommandRunner.unblock(name);
                                            if (!removed) {
                                                ctx.getSource().sendFailure(Component.literal("/" + name + " は禁止リストに入っていません。"));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "/" + name + " をGeminiが実行できるようにしました。"), true);
                                            return 1;
                                        }))))
                // OP: デフォルトのキー（自分のキーが無い人が使う）
                .then(Commands.literal("default_key")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("apiKey", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String key = StringArgumentType.getString(ctx, "apiKey").trim();
                                            KeyStore.setDefaultKey(key);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "デフォルトのキーを設定しました（" + mask(key) + "）。自分でキーを設定していない人は、このキーでGeminiと話します。")
                                                    .withStyle(ChatFormatting.GREEN), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    KeyStore.setDefaultKey("");
                                    ctx.getSource().sendSuccess(() -> Component.literal("デフォルトのキーを削除しました。"), false);
                                    return 1;
                                })))
                // 誰でも: 自分のキー・キーを借りる
                .then(Commands.literal("key")
                        .then(Commands.literal("set")
                                .then(Commands.argument("apiKey", StringArgumentType.word())
                                        .executes(ctx -> setKey(ctx, true))
                                        .then(Commands.literal("public").executes(ctx -> setKey(ctx, true)))
                                        .then(Commands.literal("private").executes(ctx -> setKey(ctx, false)))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    KeyStore.clearOwnKey(p);
                                    ctx.getSource().sendSuccess(() -> Component.literal("あなたのキーを削除しました。"), false);
                                    return 1;
                                }))
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    List<String> names = KeyStore.lenders(ctx.getSource().getServer());
                                    String msg = names.isEmpty()
                                            ? "借りられるキーはまだありません。"
                                            : "借りられるキー（/gemini key use <名前>）: " + String.join("、", names);
                                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                    return 1;
                                }))
                        .then(Commands.literal("use")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    KeyStore.stopUsing(p);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "キーを借りるのをやめました。自分のキー（無ければデフォルトのキー）を使います。"), false);
                                    return 1;
                                })
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(KeyStore.lenders(ctx.getSource().getServer()), b))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            String lender = KeyStore.use(p, name);
                                            if (lender == null) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        name + " さんの借りられる（public の）キーが見つかりません。/gemini key list で確認できます。"));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    lender + "さんのキーを借りました。これからは" + lender + "さんのキーでGeminiと話します。やめる時は /gemini key use です。")
                                                    .withStyle(ChatFormatting.GREEN), false);
                                            return 1;
                                        }))))
                // OP: 返事を待つ最大秒数
                .then(Commands.literal("timeout")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "返事を待つ最大秒数: " + GeminiConfig.TIMEOUT_SECONDS.get() + "秒（/gemini timeout <5〜600> で変えられます）"), false);
                            return 1;
                        })
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 600))
                                .executes(ctx -> {
                                    int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                    GeminiConfig.TIMEOUT_SECONDS.set(sec);
                                    GeminiConfig.TIMEOUT_SECONDS.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("返事を待つ最大秒数を " + sec + "秒にしました。"), false);
                                    return 1;
                                })))
                // OP: 考える量
                .then(Commands.literal("thinking")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "考える量: " + GeminiConfig.THINKING_LEVEL.get() + "（/gemini thinking <default|minimal|low|medium|high> で変えられます）"), false);
                            return 1;
                        })
                        .then(thinkingLevel("default"))
                        .then(thinkingLevel("minimal"))
                        .then(thinkingLevel("low"))
                        .then(thinkingLevel("medium"))
                        .then(thinkingLevel("high")))
                // OP: モデル変更
                .then(Commands.literal("model")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    GeminiConfig.MODEL.set(name);
                                    GeminiConfig.MODEL.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("モデルを " + name + " に変更しました。"), false);
                                    return 1;
                                })))
        );
    }

    private void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) KeyStore.updateName(sp);
    }

    private void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GeminiChat.forget(event.getEntity().getUUID());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        GeminiChat.clearAll();
        KeyStore.unload();
        ReadSettings.unload();
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String def = KeyStore.defaultKey();
        String defShown = def.isEmpty() ? "未設定" : (src.hasPermission(2) ? mask(def) : "設定済み");
        StringBuilder sb = new StringBuilder("デフォルトのキー: " + defShown);
        if (src.getEntity() instanceof ServerPlayer p) {
            KeyStore.Entry e = KeyStore.entry(p);
            String own = (e == null || e.key.isBlank()) ? "未設定" : mask(e.key) + "（" + (e.isPublic ? "public" : "private") + "）";
            sb.append(" / あなたのキー: ").append(own);
            KeyStore.Resolved r = KeyStore.resolve(p);
            sb.append(" / 使われるキー: ").append(r.key().isEmpty() ? "なし" : r.source());
        }
        sb.append(" / モデル: ").append(GeminiConfig.MODEL.get());
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int commandsStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String mode = switch (CommandRunner.mode()) {
            case ALL -> "all（/tell @gemini・チャットの @gemini・/gemini do で実行できます）";
            case DO -> "do（/gemini do のときだけ実行できます）";
            case OFF -> "off（実行しません）";
        };
        StringBuilder sb = new StringBuilder("Geminiのコマンドの実行: ").append(mode)
                .append(" / 権限レベルの上限: ").append(GeminiConfig.COMMAND_MAX_LEVEL.get());
        if (src.getEntity() instanceof ServerPlayer p) {
            sb.append("（あなたが頼むと ").append(CommandRunner.levelFor(p)).append(" で実行）");
        }
        sb.append(" / 1回で実行できる数: ").append(GeminiConfig.MAX_COMMANDS.get())
                .append(" / 禁止: ").append(String.join(", ", CommandRunner.blocked()));
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandMode(String mode) {
        return Commands.literal(mode).executes(ctx -> {
            GeminiConfig.COMMAND_MODE.set(mode);
            GeminiConfig.COMMAND_MODE.save();
            String detail = switch (mode) {
                case "all" -> "/tell @gemini・チャットの @gemini・/gemini do で、Geminiがコマンドを実行できます。";
                case "do" -> "/gemini do のときだけ、Geminiがコマンドを実行できます。";
                default -> "Geminiはコマンドを実行しません。";
            };
            ctx.getSource().sendSuccess(() -> Component.literal("コマンドの実行を " + mode + " にしました。" + detail), true);
            return 1;
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> thinkingLevel(String level) {
        return Commands.literal(level).executes(ctx -> {
            GeminiConfig.THINKING_LEVEL.set(level);
            GeminiConfig.THINKING_LEVEL.save();
            String note = switch (level) {
                case "default" -> "（モデルにおまかせ）";
                case "minimal", "low" -> "（速く返事が来ます）";
                default -> "（じっくり考えますが、返事まで時間がかかります）";
            };
            ctx.getSource().sendSuccess(() -> Component.literal("考える量を " + level + " にしました" + note), false);
            return 1;
        });
    }

    private static int setKey(CommandContext<CommandSourceStack> ctx, boolean isPublic) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(ctx, "apiKey").trim();
        KeyStore.setOwnKey(p, key, isPublic);
        String note = isPublic
                ? "（public：他の人も /gemini key use " + p.getGameProfile().getName() + " で借りられます）"
                : "（private：あなただけが使います）";
        ctx.getSource().sendSuccess(() -> Component.literal("あなたのキーを設定しました（" + mask(key) + "）" + note)
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static String mask(String key) {
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "…" + key.substring(key.length() - 4);
    }
}
