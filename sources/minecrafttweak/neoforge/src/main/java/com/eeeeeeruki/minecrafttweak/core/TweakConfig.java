package com.eeeeeeruki.minecrafttweak.core;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * config/minecrafttweak.toml の中身。
 * [分類.機能] ごとに enabled と、その機能の追加設定が並ぶ。
 */
public final class TweakConfig {
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("minEcraft twEEk - turn each feature on/off here or from the in-game config screen.");
        for (FeatureCategory category : FeatureCategory.values()) {
            List<Feature> features = Features.inCategory(category);
            if (features.isEmpty()) {
                continue;
            }
            builder.push(category.id());
            for (Feature feature : features) {
                builder.push(feature.id());
                feature.buildConfig(builder);
                builder.pop();
            }
            builder.pop();
        }
        SPEC = builder.build();
    }

    private TweakConfig() {
    }
}
