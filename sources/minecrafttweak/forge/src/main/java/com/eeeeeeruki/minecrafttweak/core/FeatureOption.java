package com.eeeeeeruki.minecrafttweak.core;

import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/** 機能の追加設定1つ分（数値やオン/オフ）。 */
public final class FeatureOption {
    public enum Type { BOOLEAN, INT, DOUBLE }

    private final Feature feature;
    private final String id;
    private final Type type;
    private final ForgeConfigSpec.ConfigValue<?> value;
    private final double min;
    private final double max;

    FeatureOption(Feature feature, String id, Type type, ForgeConfigSpec.ConfigValue<?> value, double min, double max) {
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

    /** 「機能番号.設定番号」の形の番号。 */
    public String number() {
        return feature.number() + "." + (feature.options().indexOf(this) + 1);
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
                ((ForgeConfigSpec.BooleanValue) value).set(b);
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
                ((ForgeConfigSpec.IntValue) value).set(v);
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
                ((ForgeConfigSpec.DoubleValue) value).set(v);
            }
        }
        value.save();
        return null;
    }
}
