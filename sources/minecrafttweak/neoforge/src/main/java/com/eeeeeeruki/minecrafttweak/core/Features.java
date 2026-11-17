package com.eeeeeeruki.minecrafttweak.core;

import com.eeeeeeruki.minecrafttweak.feature.tweaks.ArmorStandArmsFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** すべての機能の一覧。新しい機能はここの static ブロックに1行足す。 */
public final class Features {
    private static final List<Feature> ALL = new ArrayList<>();

    static {
        // ===== 調整 =====
        add(new ArmorStandArmsFeature());

        // ===== 建築 =====

        // ===== 道具 =====

        // ===== 整理 =====

        // ===== ワールド =====

        // ===== 見た目・操作 =====
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
