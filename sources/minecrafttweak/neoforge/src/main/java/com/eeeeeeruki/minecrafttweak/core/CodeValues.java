package com.eeeeeeruki.minecrafttweak.core;

import net.minecraft.util.OptionEnum;

import java.math.BigDecimal;
import java.util.Locale;

/** 設定コードの値の読み書き（t / f / 数値 / 選択肢の番号）。 */
public final class CodeValues {
    private CodeValues() {
    }

    /** 値を設定コードの文字にする。 */
    public static String format(Object value) {
        if (value instanceof Boolean b) {
            return b ? "t" : "f";
        }
        if (value instanceof Double d) {
            return trim(d);
        }
        if (value instanceof Float f) {
            return trim(f.doubleValue());
        }
        if (value instanceof Enum<?> e) {
            if (e instanceof OptionEnum oe) {
                return String.valueOf(oe.getId());
            }
            return String.valueOf(e.ordinal());
        }
        return String.valueOf(value);
    }

    private static String trim(double d) {
        BigDecimal bd = BigDecimal.valueOf(d).setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.scale() < 0 ? bd.setScale(0).toPlainString() : bd.toPlainString();
    }

    /**
     * 今の値と同じ種類で文字を読む。読めなければ null。
     * 選択肢は番号（バニラは設定の番号、MODは並び順で0から）か名前で書ける。
     */
    public static Object parseLike(Object current, Class<?> type, String raw) {
        try {
            if (type == Boolean.class || type == boolean.class) {
                return TextSettings.parseBool(raw);
            }
            if (type == Integer.class || type == int.class) {
                return Integer.parseInt(raw);
            }
            if (type == Double.class || type == double.class) {
                return Double.parseDouble(raw);
            }
            if (type == Float.class || type == float.class) {
                return Float.parseFloat(raw);
            }
            Class<?> enumType = type.isEnum() ? type
                    : current instanceof Enum<?> e ? e.getDeclaringClass() : null;
            if (enumType != null) {
                Object[] constants = enumType.getEnumConstants();
                for (Object c : constants) {
                    if (c instanceof OptionEnum oe && String.valueOf(oe.getId()).equals(raw)) {
                        return c;
                    }
                }
                for (Object c : constants) {
                    if (!(c instanceof OptionEnum) && String.valueOf(((Enum<?>) c).ordinal()).equals(raw)) {
                        return c;
                    }
                }
                for (Object c : constants) {
                    if (((Enum<?>) c).name().toLowerCase(Locale.ROOT).equals(raw.toLowerCase(Locale.ROOT))) {
                        return c;
                    }
                }
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    /** 選択肢の一覧（「0=fast 1=fancy」のような説明用）。 */
    public static String choices(Class<?> enumType) {
        StringBuilder sb = new StringBuilder();
        for (Object c : enumType.getEnumConstants()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(format(c)).append('=').append(((Enum<?>) c).name().toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }
}
