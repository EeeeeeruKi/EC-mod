package com.eeeeeeruki.minecrafttweak.core;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 機能の追加設定1つ分（数値やオン/オフ）。 */
public final class FeatureOption {
    public enum Type { BOOLEAN, INT, DOUBLE }

    private final Feature feature;
    private final String id;
    private final Type type;
    private final ModConfigSpec.ConfigValue<?> value;
    private final double min;
    private final double max;

    FeatureOption(Feature feature, String id, Type type, ModConfigSpec.ConfigValue<?> value, double min, double max) {
        this.feature = feature;
        this.id = id;
        this.type = type;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public Feature feature() {
        return feature;
    }

    public String id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public String nameKey() {
        return feature.nameKey() + "." + id;
    }

    public String descriptionKey() {
        return nameKey() + ".desc";
    }

    /** 「機能番号＋英字」の形の番号（1a, 1b, …, 1z, 1aa, …）。 */
    public String number() {
        return feature.number() + letters(feature.options().indexOf(this));
    }

    static String letters(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        do {
            sb.insert(0, (char) ('a' + n % 26));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    static int indexOfLetters(String letters) {
        int n = 0;
        for (char c : letters.toCharArray()) {
            if (c < 'a' || c > 'z') {
                return -1;
            }
            n = n * 26 + (c - 'a' + 1);
        }
        return n - 1;
    }

    /** 入れられる範囲（数値の設定のときだけ）。 */
    public String rangeText() {
        return switch (type) {
            case INT -> (int) min + " ~ " + (int) max;
            case DOUBLE -> min + " ~ " + max;
            case BOOLEAN -> "t / f";
        };
    }

    public boolean booleanValue() {
        return loaded() && value.get() instanceof Boolean b && b;
    }

    /** 初期値に戻す。 */
    @SuppressWarnings("unchecked")
    public void reset() {
        if (!loaded()) {
            return;
        }
        ModConfigSpec.ConfigValue<Object> v = (ModConfigSpec.ConfigValue<Object>) value;
        v.set(v.getDefault());
        v.save();
    }

    private boolean loaded() {
        return TweakConfig.SPEC.isLoaded();
    }

    /** テキスト設定に書くときの値（t / f / 数値）。 */
    public String textValue() {
        if (!loaded()) {
            return "?";
        }
        Object v = value.get();
        if (v instanceof Boolean b) {
            return b ? "t" : "f";
        }
        return String.valueOf(v);
    }

    /** 画面やチャットに出すときの値。 */
    public Component displayValue() {
        if (loaded() && value.get() instanceof Boolean b) {
            return TextSettings.onOff(b);
        }
        return Component.literal(textValue());
    }

    /** 文字列から値を入れる。うまくいけば null、だめならエラーの文を返す。 */
    public Component apply(String raw) {
        if (!loaded()) {
            return Component.translatable("minecrafttweak.text.error.not_loaded");
        }
        switch (type) {
            case BOOLEAN -> {
                Boolean b = TextSettings.parseBool(raw);
                if (b == null) {
                    return Component.translatable("minecrafttweak.text.error.bool", number(), raw);
                }
                ((ModConfigSpec.BooleanValue) value).set(b);
            }
            case INT -> {
                int v;
                try {
                    v = Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    return Component.translatable("minecrafttweak.text.error.number", number(), raw);
                }
                if (v < min || v > max) {
                    return Component.translatable("minecrafttweak.text.error.range", number(), raw,
                            String.valueOf((int) min), String.valueOf((int) max));
                }
                ((ModConfigSpec.IntValue) value).set(v);
            }
            case DOUBLE -> {
                double v;
                try {
                    v = Double.parseDouble(raw);
                } catch (NumberFormatException e) {
                    return Component.translatable("minecrafttweak.text.error.number", number(), raw);
                }
                if (v < min || v > max) {
                    return Component.translatable("minecrafttweak.text.error.range", number(), raw,
                            String.valueOf(min), String.valueOf(max));
                }
                ((ModConfigSpec.DoubleValue) value).set(v);
            }
        }
        value.save();
        return null;
    }
}
