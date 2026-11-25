package com.eeeeeeruki.minecrafttweak.server;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.feature.client.DiceBotFeature;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * BCDice（ダイスボット）。サーバーで動く。
 *
 * - チャットの最初の言葉が、設定したゲームシステムのダイスのコマンドなら、BCDice-API に振ってもらい、結果をそのまま全員に流す。
 *   どの言葉がコマンドかは、BCDice-API が教えてくれる正規表現（command_pattern）で決める。
 * - 「rccb」はこのmodだけのコマンド。1D100 で成功値を決めてから、その成功値で CCB（5%ルール）の判定をする。
 * - 「S」から始まるシークレットダイスは、結果を振った人にだけ見せる。
 */
public final class DiceBot {
    /** command_pattern をもらえるまで使う、BCDice の共通コマンドの頭。 */
    private static final Pattern FALLBACK = Pattern.compile(
            "^S?([+\\-(]*\\d+|\\d+B\\d+|\\d+R\\d+|\\d+U\\d+|C[+\\-(]*\\d+|CHOICE|D66|(REPEAT|REP|X)\\d+|CCB|CC|RESB|RES|CBRB|CBR)",
            Pattern.CASE_INSENSITIVE);
    private static final Random RANDOM = new Random();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "minEcraft twEEk BCDice");
        t.setDaemon(true);
        return t;
    });
    private static HttpClient http;
    private static volatile Pattern pattern;
    private static volatile String patternKey = "";
    private static volatile boolean loadingPattern;

    private DiceBot() {
    }

    private static boolean enabled() {
        return DiceBotFeature.INSTANCE != null && DiceBotFeature.INSTANCE.isEnabled();
    }

    private static String system() {
        try {
            String s = DiceBotFeature.INSTANCE.gameSystem.get().trim();
            return s.isEmpty() ? "DiceBot" : s;
        } catch (Exception e) {
            return "Cthulhu";
        }
    }

    private static String api() {
        try {
            String s = DiceBotFeature.INSTANCE.apiUrl.get().trim();
            while (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }
            return s;
        } catch (Exception e) {
            return "https://bcdice.onlinesession.app";
        }
    }

    /** チャットが送られたとき（サーバーのメインスレッド）。 */
    public static void onChat(ServerPlayer player, String message) {
        if (!enabled() || message == null) {
            return;
        }
        String text = message.strip();
        if (text.isEmpty()) {
            return;
        }
        String command = text.split("\\s+", 2)[0];
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (command.equalsIgnoreCase("rccb")) {
            String result = rccb();
            long sentAt = System.currentTimeMillis();
            POOL.submit(() -> afterChat(server, sentAt, () -> broadcast(server, player, Component.literal(result))));
            return;
        }
        if (!currentPattern().matcher(command).find()) {
            return;
        }
        String system = system();
        String api = api();
        long sentAt = System.currentTimeMillis();
        POOL.submit(() -> {
            try {
                JsonObject result = roll(api, system, command);
                if (result == null || !result.has("ok") || !result.get("ok").getAsBoolean() || !result.has("text")) {
                    return; // コマンドではなかった
                }
                String out = result.get("text").getAsString();
                boolean secret = result.has("secret") && result.get("secret").getAsBoolean();
                afterChat(server, sentAt, () -> {
                    if (secret) {
                        player.sendSystemMessage(prefix().append(Component.translatableWithFallback(
                                "minecrafttweak.dicebot.secret_self", "(シークレット) %s", out)));
                        broadcast(server, null, Component.translatableWithFallback("minecrafttweak.dicebot.secret_others",
                                "%s がシークレットダイスを振りました", player.getDisplayName()));
                    } else {
                        broadcast(server, player, Component.literal(out));
                    }
                });
            } catch (Exception e) {
                MinecraftTweak.LOGGER.debug("minEcraft twEEk: BCDice request failed", e);
                server.execute(() -> player.sendSystemMessage(prefix().append(Component.translatableWithFallback(
                        "minecrafttweak.dicebot.error", "BCDice につながりませんでした（%s）", String.valueOf(e.getMessage())))
                        .withStyle(ChatFormatting.RED)));
            }
        });
    }

    /** 結果を出すまでに空ける時間（チャットの文がみんなの画面に出てから結果を出すため）。 */
    private static final long DELAY_MS = 300;

    /**
     * チャットの文が流れたあとに結果を出す。チャットを受け取ってから DELAY_MS たつまで待ってから、
     * サーバーのメインスレッドの次の処理の順番に回す（別のスレッドから呼ぶ）。
     */
    private static void afterChat(MinecraftServer server, long sentAt, Runnable task) {
        long wait = sentAt + DELAY_MS - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        server.execute(() -> server.tell(new TickTask(server.getTickCount(), task)));
    }

    /** rccb: 成功値を 1D100 で決めて、その値で CCB（クリティカル 5 以下・ファンブル 96 以上）の判定をする。 */
    static String rccb() {
        int target = RANDOM.nextInt(100) + 1;
        int roll = RANDOM.nextInt(100) + 1;
        String judge;
        if (roll <= target) {
            judge = roll <= 5 ? "クリティカル！" : "成功";
        } else {
            judge = roll >= 96 ? "ファンブル！" : "失敗";
        }
        return "成功値決定ロール: " + target + ", 判定ロール: " + roll + " (" + judge + ")";
    }

    private static MutableComponent prefix() {
        return Component.literal("[BCDice] ").withStyle(ChatFormatting.GOLD);
    }

    private static void broadcast(MinecraftServer server, ServerPlayer player, Component body) {
        MutableComponent line = prefix();
        if (player != null) {
            line.append(player.getDisplayName()).append(": ");
        }
        line.append(body);
        server.getPlayerList().broadcastSystemMessage(line, false);
    }

    private static HttpClient client() {
        if (http == null) {
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
        }
        return http;
    }

    private static JsonObject get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "minEcraft-twEEk (Minecraft mod; https://github.com/EeeeeeruKi/EC-mod)")
                .GET().build();
        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 500) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        try {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            if (response.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            throw e;
        }
    }

    private static JsonObject roll(String api, String system, String command) throws Exception {
        String url = api + "/v2/game_system/" + URLEncoder.encode(system, StandardCharsets.UTF_8)
                + "/roll?command=" + URLEncoder.encode(command, StandardCharsets.UTF_8);
        return get(url);
    }

    /** そのゲームシステムのコマンドの正規表現。まだなら共通のものを使いながら、裏でもらいに行く。 */
    private static Pattern currentPattern() {
        String key = api() + "|" + system();
        if (!key.equals(patternKey)) {
            pattern = null;
            patternKey = key;
        }
        Pattern p = pattern;
        if (p != null) {
            return p;
        }
        if (!loadingPattern) {
            loadingPattern = true;
            String api = api();
            String system = system();
            POOL.submit(() -> {
                try {
                    JsonObject info = get(api + "/v2/game_system/" + URLEncoder.encode(system, StandardCharsets.UTF_8));
                    if (info != null && info.has("command_pattern")) {
                        pattern = Pattern.compile(info.get("command_pattern").getAsString().replace("\\A", "^").replace("\\z", "$"),
                                Pattern.CASE_INSENSITIVE);
                    }
                } catch (PatternSyntaxException e) {
                    MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not read the BCDice command pattern for {}", system);
                } catch (Exception e) {
                    MinecraftTweak.LOGGER.debug("minEcraft twEEk: could not load the BCDice command pattern", e);
                } finally {
                    loadingPattern = false;
                }
            });
        }
        return FALLBACK;
    }

    /** 設定コードなどでゲームシステムを変えたとき用（次のチャットで取り直す）。 */
    public static void reset() {
        pattern = null;
        patternKey = "";
    }
}
