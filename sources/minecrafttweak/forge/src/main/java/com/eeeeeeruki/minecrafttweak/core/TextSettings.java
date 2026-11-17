package com.eeeeeeruki.minecrafttweak.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 設定コード（インポート/エクスポート）。
 * 「項目番号-値」を「,」でつなぐ。例: "1-t,1.1-0,2-f"
 * - 項目番号 N … N番の機能のオン/オフ（t / f）
 * - 項目番号 N.M … N番の機能の M番目の追加設定（数値、または t / f）
 */
public final class TextSettings {
    private TextSettings() {
    }

    public static Component onOff(boolean value) {
        return value
                ? Component.translatable("minecrafttweak.config.on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("minecrafttweak.config.off").withStyle(ChatFormatting.RED);
    }

    public static Boolean parseBool(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "t", "true", "on", "y", "yes", "1", "o" -> Boolean.TRUE;
            case "f", "false", "off", "n", "no", "0", "x" -> Boolean.FALSE;
            default -> null;
        };
    }

    /** 今の設定を設定コードにする（エクスポート）。 */
    public static String currentText() {
        StringBuilder sb = new StringBuilder();
        for (Feature feature : Features.all()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(feature.number()).append('-').append(feature.isEnabled() ? 't' : 'f');
            for (FeatureOption option : feature.options()) {
                sb.append(',').append(option.number()).append('-').append(option.textValue());
            }
        }
        return sb.toString();
    }

    public record Result(int changed, List<Component> messages, List<Component> errors) {
    }

    public static Result apply(String input) {
        List<Component> messages = new ArrayList<>();
        List<Component> errors = new ArrayList<>();
        String text = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .replace('、', ',').replace(';', ',').replaceAll("\\s+", "");
        if (text.isEmpty()) {
            errors.add(Component.translatable("minecrafttweak.text.error.empty"));
            return new Result(0, messages, errors);
        }
        int changed = 0;
        for (String entry : text.split(",")) {
            if (entry.isEmpty()) {
                continue;
            }
            int dash = entry.indexOf('-');
            if (dash <= 0 || dash == entry.length() - 1) {
                errors.add(Component.translatable("minecrafttweak.text.error.format", entry));
                continue;
            }
            Component error = applyOne(entry.substring(0, dash), entry.substring(dash + 1), messages);
            if (error != null) {
                errors.add(error);
            } else {
                changed++;
            }
        }
        return new Result(changed, messages, errors);
    }

    private static Component applyOne(String key, String raw, List<Component> messages) {
        String[] parts = key.split("\\.");
        int featureNo;
        int optionNo = 0;
        try {
            featureNo = Integer.parseInt(parts[0]);
            if (parts.length == 2) {
                optionNo = Integer.parseInt(parts[1]);
            } else if (parts.length != 1) {
                return Component.translatable("minecrafttweak.text.error.key", key);
            }
        } catch (NumberFormatException e) {
            return Component.translatable("minecrafttweak.text.error.key", key);
        }

        List<Feature> all = Features.all();
        if (featureNo < 1 || featureNo > all.size()) {
            return Component.translatable("minecrafttweak.text.error.no_feature", key);
        }
        Feature feature = all.get(featureNo - 1);
        Component name = Component.translatable(feature.nameKey());

        if (optionNo == 0) {
            Boolean b = parseBool(raw);
            if (b == null) {
                return Component.translatable("minecrafttweak.text.error.bool", key, raw);
            }
            if (!TweakConfig.SPEC.isLoaded()) {
                return Component.translatable("minecrafttweak.text.error.not_loaded");
            }
            feature.setEnabled(b);
            messages.add(Component.translatable("minecrafttweak.text.set", key, name, onOff(b)));
            return null;
        }

        if (optionNo < 1 || optionNo > feature.options().size()) {
            return Component.translatable("minecrafttweak.text.error.no_option", key);
        }
        FeatureOption option = feature.options().get(optionNo - 1);
        Component error = option.apply(raw);
        if (error != null) {
            return error;
        }
        messages.add(Component.translatable("minecrafttweak.text.set", key,
                Component.translatable(option.nameKey()), option.displayValue()));
        return null;
    }

    /** 番号つきの一覧（チャットの /minecrafttweak list 用）。 */
    public static List<Component> listLines() {
        List<Component> lines = new ArrayList<>();
        for (Feature feature : Features.all()) {
            lines.add(Component.literal(feature.number() + ". ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable(feature.nameKey()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" : ").withStyle(ChatFormatting.GRAY))
                    .append(onOff(feature.isEnabled())));
            for (FeatureOption option : feature.options()) {
                lines.add(Component.literal("   " + option.number() + " ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable(option.nameKey()).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" : ").withStyle(ChatFormatting.GRAY))
                        .append(option.displayValue()));
            }
        }
        return lines;
    }
}
