package com.eeeeeeruki.callai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

/**
 * OpenAI互換API（/chat/completions）を呼びます。
 * 中ではGemini形式の body を OpenAI形式に直して送り、返事をGemini形式の candidate に直して返すので、
 * 会話やコマンド実行の処理はGeminiのときと同じものを使えます。
 */
public final class OpenAIClient {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private static final Pattern THINK = Pattern.compile("(?s)<think>.*?</think>");

    private OpenAIClient() {}

    public static CompletableFuture<JsonObject> call(String baseUrl, String apiKey, String model, JsonObject body,
                                                     int timeoutSeconds) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return CompletableFuture.failedFuture(new GeminiClient.GeminiException(
                    "OpenAI互換APIのURLが設定されていません（/ai openai url <URL>）"));
        }
        if (model == null || model.isBlank()) {
            return CompletableFuture.failedFuture(new GeminiClient.GeminiException(
                    "OpenAI互換APIのモデルが設定されていません（/ai model <名前>）"));
        }
        JsonObject req = convert(model, body, true);
        return send(baseUrl, apiKey, req, timeoutSeconds).exceptionallyCompose(err -> {
            Throwable t = GeminiClient.unwrap(err);
            // ツール（関数呼び出し）に対応していないモデルなら、ツール無しで送り直す
            if (t instanceof GeminiClient.GeminiException ge && ge.getMessage().startsWith("HTTP 4")
                    && ge.getMessage().toLowerCase(Locale.ROOT).contains("tool") && req.has("tools")) {
                return send(baseUrl, apiKey, convert(model, body, false), timeoutSeconds);
            }
            return CompletableFuture.failedFuture(err);
        });
    }

    // ------------------------------------------------------------------
    // Gemini形式 → OpenAI形式
    // ------------------------------------------------------------------

    static JsonObject convert(String model, JsonObject body, boolean withTools) {
        JsonObject req = new JsonObject();
        req.addProperty("model", model);
        req.addProperty("stream", false);
        JsonArray messages = new JsonArray();

        if (body.has("systemInstruction")) {
            String sys = joinText(body.getAsJsonObject("systemInstruction").getAsJsonArray("parts"));
            if (!sys.isEmpty()) messages.add(msg("system", sys));
        }

        int callSeq = 0;
        JsonArray contents = body.getAsJsonArray("contents");
        for (JsonElement ce : contents) {
            JsonObject c = ce.getAsJsonObject();
            boolean model_ = "model".equals(c.has("role") ? c.get("role").getAsString() : "user");
            JsonArray parts = c.has("parts") ? c.getAsJsonArray("parts") : new JsonArray();

            StringBuilder text = new StringBuilder();
            JsonArray images = new JsonArray();
            JsonArray toolCalls = new JsonArray();
            JsonArray toolResults = new JsonArray();
            for (JsonElement pe : parts) {
                if (!pe.isJsonObject()) continue;
                JsonObject p = pe.getAsJsonObject();
                if (p.has("thought") && p.get("thought").getAsBoolean()) continue;
                if (p.has("text")) {
                    text.append(p.get("text").getAsString());
                } else if (p.has("inlineData")) {
                    JsonObject in = p.getAsJsonObject("inlineData");
                    JsonObject url = new JsonObject();
                    url.addProperty("url", "data:" + in.get("mimeType").getAsString() + ";base64," + in.get("data").getAsString());
                    JsonObject img = new JsonObject();
                    img.addProperty("type", "image_url");
                    img.add("image_url", url);
                    images.add(img);
                } else if (p.has("functionCall")) {
                    JsonObject fc = p.getAsJsonObject("functionCall");
                    String id = fc.has("id") ? fc.get("id").getAsString() : "call_" + (callSeq++);
                    JsonObject fn = new JsonObject();
                    fn.addProperty("name", fc.get("name").getAsString());
                    fn.addProperty("arguments", fc.has("args") ? fc.get("args").toString() : "{}");
                    JsonObject tc = new JsonObject();
                    tc.addProperty("id", id);
                    tc.addProperty("type", "function");
                    tc.add("function", fn);
                    toolCalls.add(tc);
                } else if (p.has("functionResponse")) {
                    JsonObject fr = p.getAsJsonObject("functionResponse");
                    JsonObject m = new JsonObject();
                    m.addProperty("role", "tool");
                    m.addProperty("tool_call_id", fr.has("id") ? fr.get("id").getAsString() : "call_0");
                    m.addProperty("name", fr.get("name").getAsString());
                    m.addProperty("content", fr.has("response") ? fr.get("response").toString() : "{}");
                    toolResults.add(m);
                }
            }

            if (!toolResults.isEmpty()) {
                messages.addAll(toolResults);
                continue;
            }
            if (model_) {
                JsonObject m = new JsonObject();
                m.addProperty("role", "assistant");
                if (text.length() > 0 || toolCalls.isEmpty()) m.addProperty("content", text.toString());
                else m.add("content", com.google.gson.JsonNull.INSTANCE);
                if (!toolCalls.isEmpty()) m.add("tool_calls", toolCalls);
                messages.add(m);
            } else if (images.isEmpty()) {
                messages.add(msg("user", text.toString()));
            } else {
                JsonArray content = new JsonArray();
                JsonObject t = new JsonObject();
                t.addProperty("type", "text");
                t.addProperty("text", text.toString());
                content.add(t);
                content.addAll(images);
                JsonObject m = new JsonObject();
                m.addProperty("role", "user");
                m.add("content", content);
                messages.add(m);
            }
        }
        req.add("messages", messages);

        if (withTools && body.has("tools")) {
            JsonArray tools = new JsonArray();
            for (JsonElement te : body.getAsJsonArray("tools")) {
                JsonObject t = te.getAsJsonObject();
                if (!t.has("functionDeclarations")) continue;
                for (JsonElement de : t.getAsJsonArray("functionDeclarations")) {
                    JsonObject wrap = new JsonObject();
                    wrap.addProperty("type", "function");
                    wrap.add("function", de.deepCopy());
                    tools.add(wrap);
                }
            }
            if (!tools.isEmpty()) req.add("tools", tools);
        }
        return req;
    }

    private static JsonObject msg(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    private static String joinText(JsonArray parts) {
        StringBuilder sb = new StringBuilder();
        if (parts != null) {
            for (JsonElement pe : parts) {
                if (pe.isJsonObject() && pe.getAsJsonObject().has("text")) sb.append(pe.getAsJsonObject().get("text").getAsString());
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 送信と、OpenAI形式 → Gemini形式
    // ------------------------------------------------------------------

    private static CompletableFuture<JsonObject> send(String baseUrl, String apiKey, JsonObject req, int timeoutSeconds) {
        String url = baseUrl.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (!url.endsWith("/chat/completions")) url = url + "/chat/completions";
        HttpRequest.Builder rb;
        try {
            rb = HttpRequest.newBuilder(URI.create(url));
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(new GeminiClient.GeminiException("URLの形が正しくありません: " + url));
        }
        rb.timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(req.toString(), StandardCharsets.UTF_8));
        if (apiKey != null && !apiKey.isBlank()) rb.header("Authorization", "Bearer " + apiKey.trim());
        return HTTP.sendAsync(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(OpenAIClient::parse);
    }

    private static JsonObject parse(HttpResponse<String> res) {
        JsonObject json;
        try {
            json = JsonParser.parseString(res.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new CompletionException(new GeminiClient.GeminiException("HTTP " + res.statusCode() + "（返事を読めませんでした）"));
        }
        if (res.statusCode() != 200 || json.has("error")) {
            String msg = "HTTP " + res.statusCode();
            if (json.has("error")) {
                JsonElement e = json.get("error");
                if (e.isJsonObject() && e.getAsJsonObject().has("message")) msg += ": " + e.getAsJsonObject().get("message").getAsString();
                else if (e.isJsonPrimitive()) msg += ": " + e.getAsString();
            }
            throw new CompletionException(new GeminiClient.GeminiException(msg));
        }
        JsonArray choices = json.has("choices") && json.get("choices").isJsonArray() ? json.getAsJsonArray("choices") : null;
        if (choices == null || choices.isEmpty()) {
            throw new CompletionException(new GeminiClient.GeminiException("返事が空でした"));
        }
        JsonObject choice = choices.get(0).getAsJsonObject();
        JsonObject message = choice.has("message") ? choice.getAsJsonObject("message") : new JsonObject();

        JsonArray parts = new JsonArray();
        if (message.has("content") && message.get("content").isJsonPrimitive()) {
            String text = THINK.matcher(message.get("content").getAsString()).replaceAll("").trim();
            if (!text.isEmpty()) {
                JsonObject p = new JsonObject();
                p.addProperty("text", text);
                parts.add(p);
            }
        }
        if (message.has("tool_calls") && message.get("tool_calls").isJsonArray()) {
            int i = 0;
            for (JsonElement te : message.getAsJsonArray("tool_calls")) {
                JsonObject tc = te.getAsJsonObject();
                if (!tc.has("function")) continue;
                JsonObject fn = tc.getAsJsonObject("function");
                JsonObject args;
                try {
                    JsonElement a = fn.get("arguments");
                    args = a == null ? new JsonObject()
                            : a.isJsonObject() ? a.getAsJsonObject()
                            : JsonParser.parseString(a.getAsString()).getAsJsonObject();
                } catch (Exception e) {
                    args = new JsonObject();
                }
                JsonObject fc = new JsonObject();
                fc.addProperty("name", fn.has("name") ? fn.get("name").getAsString() : "");
                fc.add("args", args);
                fc.addProperty("id", tc.has("id") && !tc.get("id").isJsonNull() ? tc.get("id").getAsString()
                        : "call_" + System.nanoTime() + "_" + (i));
                i++;
                JsonObject p = new JsonObject();
                p.add("functionCall", fc);
                parts.add(p);
            }
        }

        JsonObject content = new JsonObject();
        content.addProperty("role", "model");
        content.add("parts", parts);
        JsonObject candidate = new JsonObject();
        candidate.add("content", content);
        if (choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()) {
            candidate.addProperty("finishReason", choice.get("finish_reason").getAsString());
        }
        return candidate;
    }
}
