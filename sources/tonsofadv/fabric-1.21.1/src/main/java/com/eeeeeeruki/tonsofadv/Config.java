package com.eeeeeeruki.tonsofadv;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** クライアントの設定（config/tonsofadv-client.properties）。ファイルを書き換えてゲームを再起動すると反映されます。 */
public final class Config {
    private static Boolean specialSounds, soundsAsRecords;

    private Config() {
    }

    static boolean specialSounds() {
        if (specialSounds == null) load();
        return specialSounds;
    }

    public static boolean soundsAsRecords() {
        if (soundsAsRecords == null) load();
        return soundsAsRecords;
    }

    private static synchronized void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("tonsofadv-client.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    p.load(r);
                }
            }
        } catch (Exception ignored) {
        }
        specialSounds = !"false".equalsIgnoreCase(p.getProperty("specialSounds", "true").trim());
        soundsAsRecords = !"false".equalsIgnoreCase(p.getProperty("soundsAsRecords", "true").trim());
        if (!Files.exists(file)) {
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write("# エーテルのタブの進捗で、エーテルの特別な音を鳴らす（false にすると普通の進捗の音になる）\n");
                w.write("specialSounds=true\n");
                w.write("# エーテルの進捗の音の音量を「ジュークボックス/音ブロック」で調節する（false にすると「音楽」の音量になる）\n");
                w.write("soundsAsRecords=true\n");
            } catch (Exception ignored) {
            }
        }
    }
}
