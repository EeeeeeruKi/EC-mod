package com.eeeeeeruki.callai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import java.util.Locale;
import java.util.Map;

/**
 * プレイヤーごとのAPIキー（serverconfig/callai-keys.json）。
 * 使う順番は「借りているキー → 自分のキー → デフォルトのキー」です。
 */
public final class KeyStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Entry> entries;
    private static Path loadedFrom;

    public static final class Entry {
        public String name = "";
        public String key = "";
        public boolean isPublic = true;
        public String use = "";
    }

    public record Resolved(String key, String source) {}

    private KeyStore() {}

    public static String defaultKey() {
        return GeminiConfig.API_KEY.get().trim();
    }

    public static void setDefaultKey(String key) {
        GeminiConfig.API_KEY.set(key);
        GeminiConfig.API_KEY.save();
    }

    public static synchronized Resolved resolve(ServerPlayer player) {
        Map<String, Entry> map = load(player.getServer());
        Entry own = map.get(id(player));
        if (own != null && !own.use.isEmpty()) {
            Entry lender = map.get(own.use);
            if (lender != null && lender.isPublic && !lender.key.isBlank()) {
                return new Resolved(lender.key.trim(), lender.name + "さんのキー");
            }
        }
        if (own != null && !own.key.isBlank()) {
            return new Resolved(own.key.trim(), "あなたのキー");
        }
        return new Resolved(defaultKey(), "デフォルトのキー");
    }

    public static String keyFor(ServerPlayer player) {
        return resolve(player).key();
    }

    public static synchronized Entry entry(ServerPlayer player) {
        return load(player.getServer()).get(id(player));
    }

    public static synchronized void setOwnKey(ServerPlayer player, String key, boolean isPublic) {
        Entry e = touch(player);
        e.key = key.trim();
        e.isPublic = isPublic;
        save();
    }

    public static synchronized void clearOwnKey(ServerPlayer player) {
        Entry e = touch(player);
        e.key = "";
        save();
    }

    /** name のキーを借ります。借りられたらその人の名前、ダメなら null を返します */
    public static synchronized String use(ServerPlayer player, String name) {
        Map<String, Entry> map = load(player.getServer());
        String lower = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Entry> me : map.entrySet()) {
            Entry e = me.getValue();
            if (e.name.toLowerCase(Locale.ROOT).equals(lower) && e.isPublic && !e.key.isBlank()) {
                Entry own = touch(player);
                own.use = me.getKey();
                save();
                return e.name;
            }
        }
        return null;
    }

    public static synchronized void stopUsing(ServerPlayer player) {
        Entry e = entry(player);
        if (e == null || e.use.isEmpty()) return;
        e.use = "";
        save();
    }

    /** キーを貸している（public の）人の名前一覧 */
    public static synchronized List<String> lenders(MinecraftServer server) {
        List<String> list = new ArrayList<>();
        for (Entry e : load(server).values()) {
            if (e.isPublic && !e.key.isBlank() && !e.name.isEmpty()) list.add(e.name);
        }
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    public static synchronized void updateName(ServerPlayer player) {
        Entry e = load(player.getServer()).get(id(player));
        String name = player.getGameProfile().getName();
        if (e != null && !name.equals(e.name)) {
            e.name = name;
            save();
        }
    }

    public static synchronized void unload() {
        entries = null;
        loadedFrom = null;
    }

    private static String id(ServerPlayer player) {
        return player.getUUID().toString();
    }

    private static Entry touch(ServerPlayer player) {
        Map<String, Entry> map = load(player.getServer());
        Entry e = map.computeIfAbsent(id(player), k -> new Entry());
        e.name = player.getGameProfile().getName();
        return e;
    }

    private static Map<String, Entry> load(MinecraftServer server) {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        Path file = dir.resolve("callai-keys.json");
        if (entries != null && file.equals(loadedFrom)) return entries;

        entries = new LinkedHashMap<>();
        loadedFrom = file;
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    Map<String, Entry> m = GSON.fromJson(r, new TypeToken<Map<String, Entry>>() {}.getType());
                    if (m != null) entries.putAll(m);
                }
            } else {
                // v1.5.0 の自分だけのキーを引き継ぐ
                Path old = dir.resolve("callai-private-keys.json");
                if (Files.exists(old)) {
                    try (Reader r = Files.newBufferedReader(old, StandardCharsets.UTF_8)) {
                        Map<String, String> m = GSON.fromJson(r, new TypeToken<Map<String, String>>() {}.getType());
                        if (m != null) {
                            for (Map.Entry<String, String> me : m.entrySet()) {
                                Entry e = new Entry();
                                e.key = me.getValue() == null ? "" : me.getValue();
                                e.isPublic = false;
                                entries.put(me.getKey(), e);
                            }
                        }
                    }
                    save();
                }
            }
        } catch (Exception e) {
            CallAI.LOGGER.warn("Failed to read API keys: {}", e.toString());
        }
        for (Entry e : entries.values()) {
            if (e.name == null) e.name = "";
            if (e.key == null) e.key = "";
            if (e.use == null) e.use = "";
        }
        return entries;
    }

    private static void save() {
        if (entries == null || loadedFrom == null) return;
        try {
            Files.createDirectories(loadedFrom.getParent());
            try (Writer w = Files.newBufferedWriter(loadedFrom, StandardCharsets.UTF_8)) {
                GSON.toJson(entries, w);
            }
        } catch (Exception e) {
            CallAI.LOGGER.warn("Failed to save API keys: {}", e.toString());
        }
    }
}
