package com.eeeeeeruki.compacttoasts;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ToastsConfig {
    public static final int DEFAULT_MAX = 20;
    public static final int DEFAULT_THRESHOLD = 4;
    public static final int MAX_LIMIT = 50;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_TOASTS;
    public static final ModConfigSpec.IntValue COMPACT_THRESHOLD;
    public static final ModConfigSpec.EnumValue<HeaderMode> HEADER_MODE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
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
        HEADER_MODE = b
                .comment("コンパクト表示で「実績達成！」「クエスト完了！」などの見出しを2秒出してから名前に切り替えるかどうかです。",
                        "ALL＝有効、EXCEPT_ADVANCEMENTS＝進捗以外、NONE＝無効")
                .translation("compacttoasts.config.header.name")
                .defineEnum("headerMode", HeaderMode.EXCEPT_ADVANCEMENTS);
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

    public static HeaderMode headerMode() {
        try {
            return HEADER_MODE.get();
        } catch (Throwable t) {
            return HeaderMode.EXCEPT_ADVANCEMENTS;
        }
    }

    public static void save(int max, int threshold, HeaderMode mode) {
        try {
            HEADER_MODE.set(mode);
            MAX_TOASTS.set(max);
            COMPACT_THRESHOLD.set(threshold);
            SPEC.save();
        } catch (Throwable t) {
            CompactToasts.LOGGER.warn("Could not save Compact Toasts config", t);
        }
    }
}
