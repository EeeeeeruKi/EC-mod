package com.eeeeeeruki.minecrafttweak.core;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 機能の追加設定1つ分（数値やオン/オフ）。 */
public final class FeatureOption {
    public enum Type { BOOLEAN, INT, DOUBLE, STRING }

    private final Feature feature;
    private final String id;
    private final Type type;
    private final ModConfigSpec.ConfigValue<?> value;
    private final double min;
    private final double max;
    private String[] filePatterns;

    FeatureOption(Feature feature, String id, Type type, ModConfigSpec.ConfigValue<?> value, double min, double max) {
        this.feature = feature;
        this.id = id;
        this.type = type;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    /** ファイルを選ぶ「参照」ボタンを出すときのファイルの種類（例: *.ogg）。null なら出さない。 */
    public String[] filePatterns() {
        return filePatterns;
    }

    void setFilePatterns(String[] patterns) {
        this.filePatterns = patterns;
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
            case STRING -> "";
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

    /** 設定コードに書くときの値（t / f / 数値 / 文字）。文字は「,」などを %XX にして入れる。 */
    public String textValue() {
        if (!loaded()) {
            return "?";
        }
        Object v = value.get();
        if (v instanceof Boolean b) {
            return b ? "t" : "f";
        }
        if (type == Type.STRING) {
            return CodeValues.encodeText(String.valueOf(v));
        }
        return String.valueOf(v);
    }

    /** 詳細設定の入力欄に出す文字（文字の設定は %XX にしない）。 */
    public String editText() {
        if (type == Type.STRING) {
            return loaded() ? String.valueOf(value.get()) : "";
        }
        return textValue();
    }

    /** 詳細設定の入力欄から値を入れる。 */
    @SuppressWarnings("unchecked")
    public Component applyEdit(String text) {
        if (type == Type.STRING) {
            if (!loaded()) {
                return Component.translatable("minecrafttweak.text.error.not_loaded");
            }
            ((ModConfigSpec.ConfigValue<String>) value).set(text.trim());
            value.save();
            return null;
        }
        return apply(text);
    }

    /** 初期値（設定コードの形）。 */
    public String defaultTextValue() {
        Object d = value.getDefault();
        if (d instanceof Boolean b) {
            return b ? "t" : "f";
        }
        if (type == Type.STRING) {
            return CodeValues.encodeText(String.valueOf(d));
        }
        return String.valueOf(d);
    }

    /** 画面やチャットに出すときの値。 */
    public Component displayValue() {
        if (loaded() && value.get() instanceof Boolean b) {
            return TextSettings.onOff(b);
        }
        if (type == Type.STRING) {
            String t = editText();
            if (t.isEmpty()) {
                return Component.translatable("minecrafttweak.config.empty_text");
            }
            return Component.literal(t.length() > 40 ? t.substring(0, 37) + "..." : t);
        }
        return Component.literal(textValue());
    }

    /** 設定コードの文字から値を入れる。うまくいけば null、だめならエラーの文を返す。 */
    @SuppressWarnings("unchecked")
    public Component apply(String raw) {
        if (!loaded()) {
            return Component.translatable("minecrafttweak.text.error.not_loaded");
        }
        switch (type) {
            case STRING -> {
                ((ModConfigSpec.ConfigValue<String>) value).set(CodeValues.decodeText(raw));
            }
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
