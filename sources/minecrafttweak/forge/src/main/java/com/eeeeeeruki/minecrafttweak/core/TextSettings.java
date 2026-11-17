package com.eeeeeeruki.minecrafttweak.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 設定コード（インポート/エクスポート）。
 * 「項目番号-値」を「,」でつなぐ。例: "1-t,1a-2,v1-12,s3-f"
 * - N … このMODの N番の機能のオン/オフ（t / f）
 * - N＋英字 … N番の機能の詳細設定（a が1つ目、b が2つ目…）
 * - 頭文字＋N … バニラ（v）や Sodium（s）、Sodium Extra（se）などの設定
 */
public final class TextSettings {
    private static final List<CodeProvider> PROVIDERS = new ArrayList<>();
    private static final Pattern KEY = Pattern.compile("([a-z]*)(\\d+)(?:([a-z]+)|\\.(\\d+))?");

    private TextSettings() {
    }

    public static void registerProvider(CodeProvider provider) {
        PROVIDERS.add(provider);
    }

    public static List<CodeProvider> providers() {
        return Collections.unmodifiableList(PROVIDERS);
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

    /** このMODの機能だけの設定コード。 */
    public static String currentText() {
        return currentText(false);
    }

    /** 今の設定を設定コードにする（エクスポート）。includeClient ならバニラや他MODの設定も入れる。 */
    public static String currentText(boolean includeClient) {
        StringBuilder sb = new StringBuilder();
        for (Feature feature : Features.all()) {
            append(sb, String.valueOf(feature.number()), feature.isEnabled() ? "t" : "f");
            for (FeatureOption option : feature.options()) {
                append(sb, option.number(), option.textValue());
            }
        }
        if (includeClient) {
            for (CodeProvider provider : PROVIDERS) {
                if (!provider.available()) {
                    continue;
                }
                for (CodeEntry entry : provider.entries()) {
                    try {
                        if (entry.available()) {
                            String v = entry.textValue();
                            if (v != null) {
                                append(sb, entry.key(), v);
                            }
                        }
                    } catch (Throwable ignored) {
                        // 読めない項目は飛ばす
                    }
                }
            }
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String key, String value) {
        if (sb.length() > 0) {
            sb.append(',');
        }
        sb.append(key).append('-').append(value);
    }

    public record Result(int changed, List<Component> messages, List<Component> errors) {
    }

    /** サーバーのコマンド用（クライアントの設定は変えない）。 */
    public static Result apply(String input) {
        return apply(input, false);
    }

    public static Result apply(String input, boolean allowClient) {
        List<Component> messages = new ArrayList<>();
        List<Component> errors = new ArrayList<>();
        String text = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .replace('、', ',').replace(';', ',').replaceAll("\\s+", "");
        if (text.isEmpty()) {
            errors.add(Component.translatable("minecrafttweak.text.error.empty"));
            return new Result(0, messages, errors);
        }
        Set<CodeProvider> touched = new LinkedHashSet<>();
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
            Component error = applyOne(entry.substring(0, dash), entry.substring(dash + 1), messages, allowClient, touched);
            if (error != null) {
                errors.add(error);
            } else {
                changed++;
            }
        }
        for (CodeProvider provider : touched) {
            try {
                provider.afterApply();
            } catch (Throwable t) {
                errors.add(Component.translatable("minecrafttweak.code.error.save", provider.displayName(),
                        String.valueOf(t.getMessage())));
            }
        }
        return new Result(changed, messages, errors);
    }

    private static Component applyOne(String key, String raw, List<Component> messages,
                                      boolean allowClient, Set<CodeProvider> touched) {
        key = key.toLowerCase(Locale.ROOT);
        Matcher m = KEY.matcher(key);
        if (!m.matches()) {
            return Component.translatable("minecrafttweak.text.error.key", key);
        }
        String prefix = m.group(1);
        if (!prefix.isEmpty()) {
            if (m.group(3) != null || m.group(4) != null) {
                return Component.translatable("minecrafttweak.text.error.key", key);
            }
            return applyProvider(prefix, key, raw, messages, allowClient, touched);
        }

        int featureNo;
        int optionNo = 0;
        try {
            featureNo = Integer.parseInt(m.group(2));
            if (m.group(3) != null) {
                optionNo = FeatureOption.indexOfLetters(m.group(3)) + 1;
            } else if (m.group(4) != null) {
                optionNo = Integer.parseInt(m.group(4)); // 古い「1.1」の書き方も読める
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

    private static Component applyProvider(String prefix, String key, String raw, List<Component> messages,
                                           boolean allowClient, Set<CodeProvider> touched) {
        for (CodeProvider provider : PROVIDERS) {
            if (!provider.prefix().equals(prefix)) {
                continue;
            }
            if (provider.clientOnly() && !allowClient) {
                return Component.translatable("minecrafttweak.code.error.client_only", key);
            }
            if (!provider.available()) {
                return Component.translatable("minecrafttweak.code.error.not_installed", key, provider.displayName());
            }
            for (CodeEntry entry : provider.entries()) {
                if (!entry.key().equals(key)) {
                    continue;
                }
                Component error;
                try {
                    if (!entry.available()) {
                        return Component.translatable("minecrafttweak.code.error.unavailable", key);
                    }
                    error = entry.apply(raw);
                } catch (Throwable t) {
                    error = Component.translatable("minecrafttweak.code.error.failed", key, String.valueOf(t.getMessage()));
                }
                if (error != null) {
                    return error;
                }
                touched.add(provider);
                messages.add(Component.translatable("minecrafttweak.text.set", key, entry.name(),
                        Component.literal(String.valueOf(entry.textValue()))));
                return null;
            }
            return Component.translatable("minecrafttweak.code.error.no_item", key);
        }
        if (!allowClient && (prefix.equals("v") || prefix.equals("s") || prefix.equals("se"))) {
            return Component.translatable("minecrafttweak.code.error.client_only", key);
        }
        return Component.translatable("minecrafttweak.code.error.no_prefix", key);
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
