package com.eeeeeeruki.callai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Gemini API（generateContent）を非同期で呼びます。
 */
public final class GeminiClient {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** 会話の1発言 */
    public record Turn(boolean fromUser, String text) {}

    /** Geminiが呼びたいツール（関数）1回分 */
    public record FunctionCall(String name, JsonObject args, String id) {}

    private GeminiClient() {}

    // ------------------------------------------------------------------
    // 文字だけのやり取り（v1.8.0 までと同じ）
    // ------------------------------------------------------------------

    public static CompletableFuture<String> generate(String apiKey, String model, String systemPrompt,
                                                     List<Turn> turns, int timeoutSeconds) {
        return generate(apiKey, model, systemPrompt, turns, timeoutSeconds, null);
    }

    public static CompletableFuture<String> generate(String apiKey, String model, String systemPrompt,
                                                     List<Turn> turns, int timeoutSeconds, byte[] image) {
        return generate(apiKey, model, systemPrompt, turns, timeoutSeconds, image, "default");
    }

    public static CompletableFuture<String> generate(String apiKey, String model, String systemPrompt,
                                                     List<Turn> turns, int timeoutSeconds, byte[] image,
                                                     String thinkingLevel) {
        JsonObject body = buildBody(systemPrompt, turns, image, thinkingLevel, null);
        return call(apiKey, model, body, timeoutSeconds).thenApply(GeminiClient::requireText);
    }

    // ------------------------------------------------------------------
    // ツール（関数呼び出し）付きのやり取り
    // ------------------------------------------------------------------

    /**
     * 送る中身を作ります。thinkingLevel が default か空なら考える量は指定しません。
     * tools が null でなければ、その関数宣言（functionDeclarations）を付けます。
     */
    public static JsonObject buildBody(String systemPrompt, List<Turn> turns, byte[] image,
                                       String thinkingLevel, JsonArray functionDeclarations) {
        JsonObject body = new JsonObject();

        String level = thinkingLevel == null ? "default" : thinkingLevel.trim().toLowerCase(Locale.ROOT);
        if (!level.isEmpty() && !level.equals("default")) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("thinkingLevel", level);
            JsonObject gen = new JsonObject();
            gen.add("thinkingConfig", thinking);
            body.add("generationConfig", gen);
        }

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonObject sys = new JsonObject();
            sys.add("parts", parts(systemPrompt));
            body.add("systemInstruction", sys);
        }

        if (functionDeclarations != null && !functionDeclarations.isEmpty()) {
            JsonObject tool = new JsonObject();
            tool.add("functionDeclarations", functionDeclarations);
            JsonArray tools = new JsonArray();
            tools.add(tool);
            body.add("tools", tools);
        }

        JsonArray contents = new JsonArray();
        for (int i = 0; i < turns.size(); i++) {
            Turn t = turns.get(i);
            JsonObject c = new JsonObject();
            c.addProperty("role", t.fromUser() ? "user" : "model");
            JsonArray p = parts(t.text());
            if (image != null && i == turns.size() - 1) {
                JsonObject inline = new JsonObject();
                inline.addProperty("mimeType", "image/jpeg");
                inline.addProperty("data", Base64.getEncoder().encodeToString(image));
                JsonObject part = new JsonObject();
                part.add("inlineData", inline);
                p.add(part);
            }
            c.add("parts", p);
            contents.add(c);
        }
        body.add("contents", contents);
        return body;
    }

    /**
     * 1回送って、最初の候補（candidate）を返します。
     * 考える量の指定でエラーになったら、指定を外して送り直します（body からも外すので、続きの送信でも外れたままです）。
     */
    public static CompletableFuture<JsonObject> call(String apiKey, String model, JsonObject body, int timeoutSeconds) {
        return send(apiKey, model, body, timeoutSeconds).exceptionallyCompose(err -> {
            Throwable t = unwrap(err);
            if (t instanceof GeminiException ge && ge.getMessage().startsWith("HTTP 400")
                    && ge.getMessage().toLowerCase(Locale.ROOT).contains("think")
                    && body.has("generationConfig")) {
                body.remove("generationConfig");
                return send(apiKey, model, body, timeoutSeconds);
            }
            return CompletableFuture.failedFuture(err);
        });
    }

    /** 候補の中の関数呼び出しを順番に取り出します */
    public static List<FunctionCall> functionCalls(JsonObject candidate) {
        List<FunctionCall> list = new ArrayList<>();
        JsonArray ps = candidateParts(candidate);
        if (ps == null) return list;
        for (JsonElement pe : ps) {
            if (!pe.isJsonObject()) continue;
            JsonObject p = pe.getAsJsonObject();
            if (!p.has("functionCall") || !p.get("functionCall").isJsonObject()) continue;
            JsonObject fc = p.getAsJsonObject("functionCall");
            String name = fc.has("name") ? fc.get("name").getAsString() : "";
            JsonObject args = fc.has("args") && fc.get("args").isJsonObject() ? fc.getAsJsonObject("args") : new JsonObject();
            String id = fc.has("id") ? fc.get("id").getAsString() : null;
            list.add(new FunctionCall(name, args, id));
        }
        return list;
    }

    /** 候補の文章（考えている途中の部分は除く）。無ければ空文字 */
    public static String text(JsonObject candidate) {
        StringBuilder sb = new StringBuilder();
        JsonArray ps = candidateParts(candidate);
        if (ps != null) {
            for (JsonElement pe : ps) {
                if (!pe.isJsonObject()) continue;
                JsonObject p = pe.getAsJsonObject();
                if (p.has("thought") && p.get("thought").getAsBoolean()) continue;
                if (p.has("text")) sb.append(p.get("text").getAsString());
            }
        }
        return sb.toString().trim();
    }

    /** 文章が無ければ「返事が空でした」のエラーにします */
    public static String requireText(JsonObject candidate) {
        String t = text(candidate);
        if (t.isEmpty()) {
            String reason = candidate.has("finishReason") ? candidate.get("finishReason").getAsString() : "不明";
            throw new CompletionException(new GeminiException("返事が空でした（" + reason + "）"));
        }
        return t;
    }

    /** 関数の結果を返すための user の発言を作ります */
    public static JsonObject functionResponses(List<FunctionCall> calls, List<JsonObject> results) {
        JsonArray ps = new JsonArray();
        for (int i = 0; i < calls.size(); i++) {
            FunctionCall c = calls.get(i);
            JsonObject fr = new JsonObject();
            fr.addProperty("name", c.name());
            if (c.id() != null) fr.addProperty("id", c.id());
            fr.add("response", results.get(i));
            JsonObject part = new JsonObject();
            part.add("functionResponse", fr);
            ps.add(part);
        }
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", ps);
        return content;
    }

    public static Throwable unwrap(Throwable err) {
        Throwable t = err;
        while (t instanceof CompletionException && t.getCause() != null) t = t.getCause();
        return t;
    }

    // ------------------------------------------------------------------

    private static JsonArray candidateParts(JsonObject candidate) {
        if (candidate == null || !candidate.has("content") || !candidate.get("content").isJsonObject()) return null;
        JsonObject content = candidate.getAsJsonObject("content");
        if (!content.has("parts") || !content.get("parts").isJsonArray()) return null;
        return content.getAsJsonArray("parts");
    }

    private static CompletableFuture<JsonObject> send(String apiKey, String model, JsonObject body, int timeoutSeconds) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + URLEncoder.encode(model, StandardCharsets.UTF_8) + ":generateContent";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(GeminiClient::parse);
    }

    private static JsonArray parts(String text) {
        JsonArray arr = new JsonArray();
        JsonObject p = new JsonObject();
        p.addProperty("text", text);
        arr.add(p);
        return arr;
    }

    private static JsonObject parse(HttpResponse<String> res) {
        JsonObject json;
        try {
            json = JsonParser.parseString(res.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new CompletionException(new GeminiException("HTTP " + res.statusCode() + "（返事を読めませんでした）"));
        }

        if (res.statusCode() != 200) {
            String msg = "HTTP " + res.statusCode();
            if (json.has("error") && json.get("error").isJsonObject()) {
                JsonObject err = json.getAsJsonObject("error");
                if (err.has("message")) msg = msg + ": " + err.get("message").getAsString();
            }
            throw new CompletionException(new GeminiException(msg));
        }

        JsonArray candidates = json.has("candidates") ? json.getAsJsonArray("candidates") : null;
        if (candidates == null || candidates.isEmpty()) {
            String reason = "返事が空でした";
            if (json.has("promptFeedback")) {
                JsonObject fb = json.getAsJsonObject("promptFeedback");
                if (fb.has("blockReason")) reason = "安全フィルターでブロックされました（" + fb.get("blockReason").getAsString() + "）";
            }
            throw new CompletionException(new GeminiException(reason));
        }
        return candidates.get(0).getAsJsonObject();
    }

    public static final class GeminiException extends RuntimeException {
        public GeminiException(String msg) { super(msg); }
    }
}
