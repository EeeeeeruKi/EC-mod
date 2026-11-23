package com.eeeeeeruki.minecrafttweak.core;

/**
 * 機能の分類。設定ファイルの節と、設定画面のタブになる。
 * 分類を増やすときはここに足して、lang に minecrafttweak.category.&lt;id&gt; を追加する。
 */
public enum FeatureCategory {
    TWEAKS("tweaks"),
    BUILDING("building"),
    TOOLS("tools"),
    MANAGEMENT("management"),
    WORLD("world"),
    CLIENT("client"),
    /** ほかのmodやバニラの不具合をよける修正（最初からオン） */
    FIXES("fixes");

    private final String id;

    FeatureCategory(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "minecrafttweak.category." + id;
    }
}
