package com.eeeeeeruki.callai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * /ai model の Tab 候補。
 * いつもの候補（よく使うGeminiのモデル）に、APIから取ってきた「いま使えるモデル」の一覧を足して出す。
 * 一覧はバックグラウンドで取りに行き、10分のあいだ覚えておく（取れなければいつもの候補だけ）。
 */
public final class ModelList {
    /** よく使うモデル（一覧が取れない時も出す） */
    private static final List<String> GEMINI_DEFAULTS = List.of(
            "gemini-flash-latest",
            "gemini-flash-lite-latest",
            "gemini-3.8-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-4-argon");

    private static final long CACHE_MS = 10 * 60 * 1000;
    private static final long RETRY_MS = 60 * 1000;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private static volatile List<String> fetched = List.of();
    private static volatile String fetchedFor = "";
    private static volatile long fetchedAt = 0;
    private static volatile boolean fetching = false;

    private ModelList() {}

    /** 候補の一覧（打った人のキーで取りに行く。足りなければデフォルトのキー） */
    public static List<String> suggestions(CommandSourceStack src) {
        boolean openai = GeminiChat.useOpenAI();
        String key;
        String target;
        if (openai) {
            key = GeminiConfig.OPENAI_API_KEY.get().trim();
            target = "openai|" + GeminiConfig.OPENAI_BASE_URL.get().trim();
        } else {
            key = src.getEntity() instanceof ServerPlayer p ? KeyStore.keyFor(p) : KeyStore.defaultKey();
            if (key.isEmpty()) key = KeyStore.defaultKey();
            target = "gemini";
        }
        refreshIfNeeded(openai, key, target);

        Set<String> out = new LinkedHashSet<>();
        if (!openai) out.addAll(GEMINI_DEFAULTS);
        if (target.equals(fetchedFor)) out.addAll(fetched);
        return new ArrayList<>(out);
    }

    /** Geminiのモデル名は小文字だけなので、大文字で打たれても直す */
    public static String normalize(String name, boolean openai) {
        String n = name.trim();
        if (!openai) {
            n = n.toLowerCase(Locale.ROOT);
            if (n.startsWith("models/")) n = n.substring("models/".length());
        }
        return n;
    }

    private static void refreshIfNeeded(boolean openai, String key, String target) {
        long now = System.currentTimeMillis();
        boolean same = target.equals(fetchedFor);
        long age = now - fetchedAt;
        if (fetching) return;
        if (same && age < (fetched.isEmpty() ? RETRY_MS : CACHE_MS)) return;
        if (!openai && key.isEmpty()) return;
        if (openai && GeminiConfig.OPENAI_BASE_URL.get().isBlank()) return;

        fetching = true;
        fetchedAt = now;
        HttpRequest req;
        try {
            req = openai ? openaiRequest(key) : geminiRequest(key);
        } catch (Exception e) {
            fetching = false;
            return;
        }
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((res, err) -> {
                    try {
                        List<String> list = List.of();
                        if (err == null && res.statusCode() == 200) {
                            list = openai ? parseOpenAI(res.body()) : parseGemini(res.body());
                        }
                        fetched = list;
                        fetchedFor = target;
                        fetchedAt = System.currentTimeMillis();
                    } catch (Exception e) {
                        fetched = List.of();
                        fetchedFor = target;
                    } finally {
                        fetching = false;
                    }
                });
    }

    private static HttpRequest geminiRequest(String key) {
        return HttpRequest.newBuilder(URI.create(
                        "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000"))
                .timeout(Duration.ofSeconds(15))
                .header("x-goog-api-key", key)
                .GET().build();
    }

    private static HttpRequest openaiRequest(String key) {
        String base = GeminiConfig.OPENAI_BASE_URL.get().trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + "/models"))
                .timeout(Duration.ofSeconds(15)).GET();
        if (!key.isEmpty()) b.header("Authorization", "Bearer " + key);
        return b.build();
    }

    /** generateContent に対応しているモデルだけ */
    private static List<String> parseGemini(String body) {
        List<String> out = new ArrayList<>();
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        if (!json.has("models")) return out;
        for (JsonElement e : json.getAsJsonArray("models")) {
            JsonObject m = e.getAsJsonObject();
            if (!m.has("name")) continue;
            boolean ok = false;
            if (m.has("supportedGenerationMethods")) {
                for (JsonElement g : m.getAsJsonArray("supportedGenerationMethods")) {
                    if ("generateContent".equals(g.getAsString())) ok = true;
                }
            }
            if (!ok) continue;
            String name = m.get("name").getAsString();
            if (name.startsWith("models/")) name = name.substring("models/".length());
            // 画像・音声・埋め込み専用のモデルはチャットに使えないので外す
            String low = name.toLowerCase(Locale.ROOT);
            if (low.contains("embedding") || low.contains("image") || low.contains("tts")
                    || low.contains("live") || low.contains("transcribe") || low.contains("omni")) continue;
            out.add(name);
        }
        out.sort(null);
        return out;
    }

    private static List<String> parseOpenAI(String body) {
        List<String> out = new ArrayList<>();
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        if (!json.has("data")) return out;
        for (JsonElement e : json.getAsJsonArray("data")) {
            JsonObject m = e.getAsJsonObject();
            if (m.has("id")) out.add(m.get("id").getAsString());
        }
        out.sort(null);
        return out;
    }
}
