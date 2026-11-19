package com.eeeeeeruki.minecrafttweak.core;

import com.eeeeeeruki.minecrafttweak.feature.client.MenuMusicFeature;
import com.eeeeeeruki.minecrafttweak.feature.client.EnglishSearchFeature;
import com.eeeeeeruki.minecrafttweak.feature.client.PauseMusicFeature;
import com.eeeeeeruki.minecrafttweak.feature.client.SystemLanguageFeature;
import com.eeeeeeruki.minecrafttweak.feature.tweaks.XpPickupFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * すべての機能の一覧。新しい機能はここの static ブロックに1行足す。
 * 足した順に番号（1, 2, 3…）が付き、テキスト設定で使う。番号がずれないよう、新しい機能は必ず一番下に足す。
 */
public final class Features {
    private static final List<Feature> ALL = new ArrayList<>();

    static {
        // 1
        add(new XpPickupFeature());
        // 2
        add(new SystemLanguageFeature());
        // 3
        add(new MenuMusicFeature());
        // 4
        add(new PauseMusicFeature());
        // 5
        add(new EnglishSearchFeature());
    }

    private Features() {
    }

    private static void add(Feature feature) {
        for (Feature f : ALL) {
            if (f.id().equals(feature.id())) {
                throw new IllegalStateException("Duplicate feature id: " + feature.id());
            }
        }
        ALL.add(feature);
    }

    public static List<Feature> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static int numberOf(Feature feature) {
        return ALL.indexOf(feature) + 1;
    }

    public static List<Feature> inCategory(FeatureCategory category) {
        List<Feature> list = new ArrayList<>();
        for (Feature f : ALL) {
            if (f.category() == category) {
                list.add(f);
            }
        }
        return list;
    }
}
