package com.eeeeeeruki.compacttoasts;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ToastsConfig {
    public static final int DEFAULT_MAX = 20;
    public static final int DEFAULT_THRESHOLD = 4;
    public static final int MAX_LIMIT = 50;

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAX_TOASTS;
    public static final ForgeConfigSpec.IntValue COMPACT_THRESHOLD;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("トーストの表示に関する設定です。").translation("compacttoasts.config.toasts").push("toasts");
        MAX_TOASTS = b
                .comment("同時に表示できるトーストの数です（バニラは5です）。")
                .translation("compacttoasts.config.maxToasts")
                .defineInRange("maxToasts", DEFAULT_MAX, 1, MAX_LIMIT);
        COMPACT_THRESHOLD = b
                .comment("表示中のトーストがこの数以上になると、1件ずつ1行の表示にまとめます。",
                        "maxToasts より大きくすると、コンパクト表示は行われません。")
                .translation("compacttoasts.config.compactThreshold")
                .defineInRange("compactThreshold", DEFAULT_THRESHOLD, 1, MAX_LIMIT + 1);
        b.pop();
        SPEC = b.build();
    }

    private ToastsConfig() {}

    public static int maxToasts() {
        try {
            return MAX_TOASTS.get();
        } catch (Throwable t) {
            return DEFAULT_MAX;
        }
    }

    public static int compactThreshold() {
        try {
            return COMPACT_THRESHOLD.get();
        } catch (Throwable t) {
            return DEFAULT_THRESHOLD;
        }
    }

    public static void save(int max, int threshold) {
        try {
            MAX_TOASTS.set(max);
            COMPACT_THRESHOLD.set(threshold);
            SPEC.save();
        } catch (Throwable t) {
            CompactToasts.LOGGER.warn("Could not save Compact Toasts config", t);
        }
    }
}
