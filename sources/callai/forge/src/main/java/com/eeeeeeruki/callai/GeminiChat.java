package com.eeeeeeruki.callai;

import com.eeeeeeruki.callai.network.CallAINetwork;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * プレイヤーごとの会話（履歴・クールダウン）を管理して、Geminiとやり取りします。
 */
public final class GeminiChat {
    static final class Session {
        final Deque<GeminiClient.Turn> history = new ArrayDeque<>();
        boolean busy;
        long lastSentMs;
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final long REPLY_DELAY_MIN_MS = 800L;
    private static final long REPLY_DELAY_MAX_MS = 4000L;
    /** 関数を呼んで結果を返す、の往復の上限 */
    private static final int MAX_TOOL_ROUNDS = 8;

    private GeminiChat() {}

    /** /tell @gemini */
    public static void talk(ServerPlayer player, String message) {
        whisper(player, message, message, false);
    }

    /** /gemini do <指示> */
    public static void doTask(ServerPlayer player, String instruction) {
        if (!CommandRunner.allowed(true)) {
            player.sendSystemMessage(Component.literal("このサーバーでは、Geminiがコマンドを実行できないように設定されています。")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        whisper(player, instruction,
                "次のことを、必要なコマンドを実行してやってください。終わったら何をしたかを短く教えてください。\n" + instruction,
                true);
    }

    private static void whisper(ServerPlayer player, String shown, String message, boolean viaDo) {
        Session s = begin(player);
        if (s == null) return;
        String botName = GeminiConfig.BOT_NAME.get();
        MinecraftServer server = player.getServer();
        UUID uuid = player.getUUID();

        // 自分の画面に「Geminiにささやきました」を出す（バニラの/tellと同じ見た目）
        player.sendSystemMessage(Component.translatable("commands.message.display.outgoing",
                        Component.literal(botName), Component.literal(shown))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        MentionBridge.setStatus(player, botName, MentionBridge.THINKING);

        request(player, s, message, viaDo, (reply, err) -> server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (err != null) {
                synchronized (s) {
                    s.busy = false;
                }
                MentionBridge.setStatus(p, botName, MentionBridge.IDLE);
                if (p != null) {
                    p.sendSystemMessage(Component.literal("Geminiから返事をもらえませんでした: " + err)
                            .withStyle(ChatFormatting.RED));
                }
                return;
            }
            if (p != null && MentionBridge.available()) {
                MentionBridge.setStatus(p, botName, MentionBridge.REPLYING);
                CompletableFuture.delayedExecutor(replyDelayMs(reply), TimeUnit.MILLISECONDS)
                        .execute(() -> server.execute(() -> deliver(server, uuid, s, botName, reply)));
            } else {
                deliver(server, uuid, s, botName, reply);
            }
        }));
    }

    /** Mention Mod から、チャットの @gemini で話しかけられた時 */
    public static boolean askPublic(ServerPlayer player, String message, BiConsumer<String, String> callback) {
        Session s = begin(player);
        if (s == null) return false;
        request(player, s, message, false, (reply, err) -> {
            synchronized (s) {
                s.busy = false;
            }
            callback.accept(reply, err);
        });
        return true;
    }

    public static long replyDelayMs(String reply) {
        return Math.max(REPLY_DELAY_MIN_MS, Math.min(REPLY_DELAY_MAX_MS, 400L + reply.length() * 35L));
    }

    private static Session begin(ServerPlayer player) {
        String apiKey = KeyStore.keyFor(player);
        if (apiKey.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Geminiはまだ設定されていません。OPの方に /gemini default_key set <APIキー> で設定してもらうか、/gemini key set <APIキー> で自分のキーを設定するか、/gemini key use <名前> で他の人のキーを借りてください。")
                    .withStyle(ChatFormatting.RED));
            return null;
        }
        Session s = SESSIONS.computeIfAbsent(player.getUUID(), k -> new Session());
        synchronized (s) {
            if (s.busy) {
                player.sendSystemMessage(Component.literal("Geminiが前のメッセージに返事を考えています。少し待ってください。")
                        .withStyle(ChatFormatting.YELLOW));
                return null;
            }
            long now = System.currentTimeMillis();
            long waitMs = GeminiConfig.COOLDOWN_SECONDS.get() * 1000L - (now - s.lastSentMs);
            if (waitMs > 0) {
                player.sendSystemMessage(Component.literal("あと" + ((waitMs + 999) / 1000) + "秒待ってから話しかけてください。")
                        .withStyle(ChatFormatting.YELLOW));
                return null;
            }
            s.busy = true;
            s.lastSentMs = now;
        }
        return s;
    }

    /** Geminiに問い合わせて、callback(返事, null) か callback(null, エラーの説明) をサーバーのスレッド以外から呼びます */
    private static void request(ServerPlayer player, Session s, String message, boolean viaDo,
                                BiConsumer<String, String> callback) {
        List<GeminiClient.Turn> turns;
        synchronized (s) {
            turns = new ArrayList<>(s.history);
        }
        turns.add(new GeminiClient.Turn(true, message));

        String apiKey = KeyStore.keyFor(player);
        boolean tools = CommandRunner.allowed(viaDo);
        String prompt = GeminiConfig.SYSTEM_PROMPT.get().replace("{player}", player.getGameProfile().getName());
        if (tools) prompt = prompt + CommandRunner.promptAddendum(player);
        MinecraftServer server = player.getServer();
        UUID uuid = player.getUUID();
        String botName = GeminiConfig.BOT_NAME.get();
        final String systemPrompt = prompt;
        final List<GeminiClient.Turn> history = turns;

        CompletableFuture<byte[]> shot = CompletableFuture.completedFuture(null);
        if (ReadSettings.isEnabled(player)) {
            if (CallAINetwork.clientHasMod(player)) {
                shot = ScreenshotService.request(player);
            } else {
                player.sendSystemMessage(Component.literal("画面を読み取るには、クライアントにもCall AIを入れてください（今回は文字だけで送ります）。")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }

        shot.thenCompose(image -> {
            List<GeminiClient.Turn> t = history;
            if (image != null) {
                t = new ArrayList<>(history);
                t.set(t.size() - 1, new GeminiClient.Turn(true,
                        message + "\n\n（添付の画像は、このプレイヤーがいま見ているMinecraftの画面です）"));
            }
            JsonObject body = GeminiClient.buildBody(systemPrompt, t, image, GeminiConfig.THINKING_LEVEL.get(),
                    tools ? CommandRunner.declarations() : null);
            return converse(apiKey, body, server, uuid, botName, new int[]{0}, 0);
        }).whenComplete((reply, err) -> server.execute(() -> {
            if (err != null) {
                CallAI.LOGGER.warn("Gemini request failed: {}", describe(err));
                callback.accept(null, describe(err));
                return;
            }
            String text = cleanup(reply);
            synchronized (s) {
                s.history.addLast(new GeminiClient.Turn(true, message));
                s.history.addLast(new GeminiClient.Turn(false, text));
                int max = GeminiConfig.HISTORY_TURNS.get() * 2;
                while (s.history.size() > max) s.history.removeFirst();
            }
            callback.accept(text, null);
        }));
    }

    /**
     * Geminiに送って、関数を呼びたいと言われたらコマンドを実行して結果を返す、を繰り返します。
     * 最後に文章の返事を返します。
     */
    private static CompletableFuture<String> converse(String apiKey, JsonObject body, MinecraftServer server, UUID uuid,
                                                      String botName, int[] commandsRun, int round) {
        String model = GeminiConfig.MODEL.get();
        int timeout = GeminiConfig.TIMEOUT_SECONDS.get();
        return GeminiClient.call(apiKey, model, body, timeout).thenCompose(candidate -> {
            List<GeminiClient.FunctionCall> calls = GeminiClient.functionCalls(candidate);
            if (calls.isEmpty()) {
                return CompletableFuture.completedFuture(GeminiClient.requireText(candidate));
            }
            if (round >= MAX_TOOL_ROUNDS) {
                String t = GeminiClient.text(candidate);
                return CompletableFuture.completedFuture(t.isEmpty() ? "（コマンドを実行できる回数の上限に達したので、ここで止めました）" : t);
            }

            // Geminiの発言（関数呼び出し）はそのまま会話に足します（thoughtSignature も残すため）
            JsonArray contents = body.getAsJsonArray("contents");
            if (candidate.has("content")) contents.add(candidate.get("content"));

            return CompletableFuture.supplyAsync(() -> {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                int max = GeminiConfig.MAX_COMMANDS.get();
                List<JsonObject> results = new ArrayList<>();
                for (GeminiClient.FunctionCall call : calls) {
                    JsonObject r;
                    if (!CommandRunner.TOOL_NAME.equals(call.name())) {
                        r = new JsonObject();
                        r.addProperty("success", false);
                        r.addProperty("output", "そのような関数はありません。");
                    } else if (p == null) {
                        r = new JsonObject();
                        r.addProperty("success", false);
                        r.addProperty("output", "プレイヤーがサーバーにいないので実行できませんでした。");
                    } else if (commandsRun[0] >= max) {
                        r = new JsonObject();
                        r.addProperty("success", false);
                        r.addProperty("output", "1回の話しかけで実行できるコマンドの数（" + max + "個）を超えたので、実行しませんでした。");
                    } else {
                        commandsRun[0]++;
                        String cmd = call.args().has("command") ? call.args().get("command").getAsString() : "";
                        r = CommandRunner.run(p, cmd, botName);
                    }
                    results.add(r);
                }
                return results;
            }, server).thenCompose(results -> {
                contents.add(GeminiClient.functionResponses(calls, results));
                return converse(apiKey, body, server, uuid, botName, commandsRun, round + 1);
            });
        });
    }

    private static void deliver(MinecraftServer server, UUID uuid, Session s, String botName, String reply) {
        synchronized (s) {
            s.busy = false;
        }
        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
        if (p == null) return;
        MentionBridge.setStatus(p, botName, MentionBridge.IDLE);
        p.sendSystemMessage(Component.translatable("commands.message.display.incoming",
                        Component.literal(botName), Component.literal(reply))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    public static void reset(UUID uuid) {
        Session s = SESSIONS.get(uuid);
        if (s != null) {
            synchronized (s) {
                s.history.clear();
            }
        }
    }

    public static void forget(UUID uuid) {
        SESSIONS.remove(uuid);
    }

    public static void clearAll() {
        SESSIONS.clear();
    }

    private static String cleanup(String text) {
        String t = text.replace("\r", "")
                .replace("**", "")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        int max = GeminiConfig.MAX_REPLY_CHARS.get();
        if (t.length() > max) t = t.substring(0, max) + "…";
        return t;
    }

    private static String describe(Throwable err) {
        Throwable t = GeminiClient.unwrap(err);
        if (t instanceof GeminiClient.GeminiException) return t.getMessage();
        if (t instanceof java.net.http.HttpTimeoutException || t instanceof TimeoutException) {
            return "時間切れになりました（" + GeminiConfig.TIMEOUT_SECONDS.get()
                    + "秒）。考える時間が長いモデルは、OPの方に /gemini timeout で待ち時間を延ばすか、/gemini thinking low で考える量を減らしてもらってください";
        }
        if (t instanceof java.net.ConnectException) return "Geminiにつながりませんでした";
        return t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
    }
}
