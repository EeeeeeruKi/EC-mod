package com.eeeeeeruki.picpop;

import java.util.Locale;

/** 画像を出す位置（横と縦） */
public final class Align {
    private Align() {}

    public enum H { LEFT, CENTER, RIGHT }

    public enum V { UP, CENTER, DOWN }

    public static H parseH(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "left", "l", "左", "ひだり" -> H.LEFT;
            case "center", "centre", "middle", "c", "中", "中央", "まんなか", "真ん中" -> H.CENTER;
            case "right", "r", "右", "みぎ" -> H.RIGHT;
            default -> null;
        };
    }

    public static V parseV(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "up", "top", "u", "上", "うえ" -> V.UP;
            case "center", "centre", "middle", "c", "中", "中央", "まんなか", "真ん中" -> V.CENTER;
            case "down", "bottom", "d", "下", "した" -> V.DOWN;
            default -> null;
        };
    }

    public static H h(int i) {
        H[] v = H.values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }

    public static V v(int i) {
        V[] v = V.values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }
}
