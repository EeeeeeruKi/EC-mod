package com.eeeeeeruki.callai;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.Set;

/**
 * 画面の読み取り（/ai read）をオンにしているプレイヤー（serverconfig/callai-read.json）。
 */
public final class ReadSettings {
    private static final Gson GSON = new Gson();
    private static Set<String> enabled;
    private static Path loadedFrom;

    private ReadSettings() {}

    public static synchronized boolean isEnabled(ServerPlayer player) {
        return load(player.getServer()).contains(player.getUUID().toString());
    }

    public static synchronized void set(ServerPlayer player, boolean on) {
        Set<String> set = load(player.getServer());
        if (on) set.add(player.getUUID().toString());
        else set.remove(player.getUUID().toString());
        save();
    }

    public static synchronized void unload() {
        enabled = null;
        loadedFrom = null;
    }

    private static Set<String> load(MinecraftServer server) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("callai-read.json");
        if (enabled != null && file.equals(loadedFrom)) return enabled;
        enabled = new HashSet<>();
        loadedFrom = file;
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                List<String> list = GSON.fromJson(r, new TypeToken<List<String>>() {}.getType());
                if (list != null) enabled.addAll(list);
            } catch (Exception e) {
                CallAI.LOGGER.warn("Failed to read callai-read.json: {}", e.toString());
            }
        }
        return enabled;
    }

    private static void save() {
        if (enabled == null || loadedFrom == null) return;
        try {
            Files.createDirectories(loadedFrom.getParent());
            try (Writer w = Files.newBufferedWriter(loadedFrom, StandardCharsets.UTF_8)) {
                GSON.toJson(enabled.stream().sorted().toList(), w);
            }
        } catch (Exception e) {
            CallAI.LOGGER.warn("Failed to save callai-read.json: {}", e.toString());
        }
    }
}
