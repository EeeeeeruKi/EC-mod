package com.eeeeeeruki.claudeupdater.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** config/claudeupdater.json */
public class UpdaterConfig {
    public static final String DEFAULT_INDEX =
            "https://raw.githubusercontent.com/EeeeeeruKi/EC-mod/main/index.json";

    public String _readme = "indexUrl: 更新情報の場所 / checkOnStartup: 起動時に確認する / "
            + "serverAutoUpdate: サーバーでは確認なしでダウンロードする / "
            + "hidden: 起動時の更新画面に二度と表示しないMODのID（消すとまた表示されます）";
    public String indexUrl = DEFAULT_INDEX;
    public boolean checkOnStartup = true;
    public boolean serverAutoUpdate = true;
    public int timeoutSeconds = 20;
    public Set<String> hidden = new LinkedHashSet<>();
    /** 1.0.0 only: hid one version per mod. Moved into {@link #hidden} on load. */
    public Map<String, String> ignored;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static UpdaterConfig load(Path file) {
        UpdaterConfig cfg = null;
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                cfg = GSON.fromJson(r, UpdaterConfig.class);
            } catch (Exception e) {
                Log.warn("設定ファイルを読めなかったため初期値を使います: " + e);
            }
        }
        if (cfg == null) cfg = new UpdaterConfig();
        if (cfg.indexUrl == null || cfg.indexUrl.isBlank()) cfg.indexUrl = DEFAULT_INDEX;
        if (cfg.hidden == null) cfg.hidden = new LinkedHashSet<>();
        if (cfg.ignored != null) {
            cfg.hidden.addAll(cfg.ignored.keySet());
            cfg.ignored = null;
        }
        if (cfg.timeoutSeconds < 3) cfg.timeoutSeconds = 3;
        cfg.save(file); // writes new keys back so the file stays complete
        return cfg;
    }

    public synchronized void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            Log.warn("設定ファイルを保存できませんでした: " + e);
        }
    }

    public static ModIndex parseIndex(String json) {
        return GSON.fromJson(json, ModIndex.class);
    }
}
