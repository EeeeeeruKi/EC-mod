package com.eeeeeeruki.compacttoasts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/compacttoasts.json に保存する設定。 */
public final class ToastsConfig {
    public static final int DEFAULT_MAX = 20;
    public static final int DEFAULT_THRESHOLD = 4;
    public static final int MAX_LIMIT = 50;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static int maxToasts = DEFAULT_MAX;
    private static int compactThreshold = DEFAULT_THRESHOLD;

    private ToastsConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("compacttoasts.json");
    }

    public static void load() {
        Path f = file();
        if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                JsonObject o = GSON.fromJson(r, JsonObject.class);
                if (o != null) {
                    if (o.has("maxToasts")) maxToasts = clamp(o.get("maxToasts").getAsInt(), 1, MAX_LIMIT);
                    if (o.has("compactThreshold")) compactThreshold = clamp(o.get("compactThreshold").getAsInt(), 1, MAX_LIMIT + 1);
                }
            } catch (Exception e) {
                CompactToasts.LOGGER.warn("Could not read {}", f, e);
            }
        }
        write();
    }

    public static int maxToasts() {
        return maxToasts;
    }

    public static int compactThreshold() {
        return compactThreshold;
    }

    public static void save(int max, int threshold) {
        maxToasts = clamp(max, 1, MAX_LIMIT);
        compactThreshold = clamp(threshold, 1, MAX_LIMIT + 1);
        write();
    }

    private static void write() {
        JsonObject o = new JsonObject();
        o.addProperty("_comment_maxToasts", "同時に表示できるトーストの数です（1〜50、バニラは5です）。");
        o.addProperty("maxToasts", maxToasts);
        o.addProperty("_comment_compactThreshold", "表示中のトーストがこの数以上になると、1件ずつ1行の表示にまとめます（maxToasts より大きくするとオフ）。");
        o.addProperty("compactThreshold", compactThreshold);
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                GSON.toJson(o, w);
            }
        } catch (Exception e) {
            CompactToasts.LOGGER.warn("Could not save Compact Toasts config", e);
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
