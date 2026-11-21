package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * パソコンの表示言語を調べる。
 * Java の Locale は起動のしかた（ランチャーの -Duser.language など）やほかのmodで変わることがあるので、
 * Windows では OS に直接たずねる（GetUserDefaultUILanguage＝今の表示言語）。
 * mac は AppleLanguages、Linux は LANG などの環境変数を見る。だめなときだけ Java の Locale を使う。
 */
public final class OsLanguage {
    private static String[] cached;

    private OsLanguage() {
    }

    interface Kernel32 extends Library {
        short GetUserDefaultUILanguage();

        int LCIDToLocaleName(int lcid, char[] name, int size, int flags);
    }

    /** {言語, 国}（小文字。国はわからなければ空）。 */
    public static synchronized String[] get() {
        if (cached == null) {
            String tag = detect();
            cached = parse(tag);
            MinecraftTweak.LOGGER.info("minEcraft twEEk: system language is {} ({})", tag, cached[0] + "_" + cached[1]);
        }
        return cached;
    }

    private static String detect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                String tag = windows();
                if (tag != null) return tag;
            } else if (os.contains("mac")) {
                String tag = mac();
                if (tag != null) return tag;
            } else {
                for (String env : new String[]{"LC_ALL", "LC_MESSAGES", "LANG"}) {
                    String v = System.getenv(env);
                    if (v != null && !v.isBlank() && !v.equals("C") && !v.startsWith("C.") && !v.equals("POSIX")) {
                        return v;
                    }
                }
            }
        } catch (Throwable t) {
            MinecraftTweak.LOGGER.debug("minEcraft twEEk: could not ask the OS for its language", t);
        }
        return Locale.getDefault(Locale.Category.DISPLAY).toLanguageTag();
    }

    private static String windows() {
        Kernel32 k = Native.load("kernel32", Kernel32.class);
        int langId = k.GetUserDefaultUILanguage() & 0xFFFF;
        char[] buf = new char[85];
        int n = k.LCIDToLocaleName(langId, buf, buf.length, 0);
        if (n <= 1) return null;
        return new String(buf, 0, n - 1);
    }

    private static String mac() throws Exception {
        Process p = new ProcessBuilder("defaults", "read", "-g", "AppleLanguages").redirectErrorStream(true).start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        p.waitFor(3, TimeUnit.SECONDS);
        // 例: ( "ja-JP", "en-US" )
        String s = sb.toString();
        int a = s.indexOf('"');
        int b = a < 0 ? -1 : s.indexOf('"', a + 1);
        if (a >= 0 && b > a) return s.substring(a + 1, b);
        String t = s.replace("(", "").replace(")", "").trim();
        int comma = t.indexOf(',');
        if (comma > 0) t = t.substring(0, comma).trim();
        return t.isEmpty() ? null : t;
    }

    /** "ja-JP" / "ja_JP.UTF-8" / "zh-Hant-TW" などを {言語, 国} にする。 */
    static String[] parse(String tag) {
        if (tag == null) return new String[]{"en", "us"};
        String t = tag.trim();
        int dot = t.indexOf('.');
        if (dot >= 0) t = t.substring(0, dot);
        int at = t.indexOf('@');
        if (at >= 0) t = t.substring(0, at);
        String[] parts = t.replace('_', '-').split("-");
        String lang = parts.length > 0 ? parts[0].toLowerCase(Locale.ROOT) : "en";
        String country = "";
        String script = "";
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() == 4 && script.isEmpty()) script = part.toLowerCase(Locale.ROOT);
            else if ((part.length() == 2 || part.length() == 3) && country.isEmpty()) country = part.toLowerCase(Locale.ROOT);
        }
        if (lang.equals("zh") && country.isEmpty()) {
            country = script.equals("hant") ? "tw" : "cn";
        }
        if (lang.isEmpty()) lang = "en";
        return new String[]{lang, country};
    }
}
