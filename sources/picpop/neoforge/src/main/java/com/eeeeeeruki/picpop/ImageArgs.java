package com.eeeeeeruki.picpop;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * 「URL [横] [縦] [秒数] [大きさ%]」を読み取ります。
 * seconds / size が -1 のときは、見る人の設定（デフォルト）を使います。seconds が 0 のときは消すまで表示します。
 */
public record ImageArgs(String url, Align.H h, Align.V v, int seconds, int size) {
    private static final SimpleCommandExceptionType BAD_URL =
            new SimpleCommandExceptionType(Component.translatable("picpop.error.url"));
    private static final DynamicCommandExceptionType BAD_TOKEN =
            new DynamicCommandExceptionType(t -> Component.translatable("picpop.error.token", t));

    public static ImageArgs center(String url) {
        return new ImageArgs(url, Align.H.CENTER, Align.V.CENTER, -1, -1);
    }

    public static ImageArgs parse(String input) throws CommandSyntaxException {
        String[] t = input.trim().split("\\s+");
        if (t.length == 0 || t[0].isEmpty() || !isHttpUrl(t[0])) throw BAD_URL.create();
        Align.H h = null;
        Align.V v = null;
        int seconds = -1, size = -1;
        for (int i = 1; i < t.length; i++) {
            String s = t[i];
            Align.H ph = Align.parseH(s);
            Align.V pv = Align.parseV(s);
            if (ph != null && pv != null) {
                if (h == null) h = ph;
                else if (v == null) v = pv;
                else throw BAD_TOKEN.create(s);
            } else if (ph != null) {
                if (h != null) throw BAD_TOKEN.create(s);
                h = ph;
            } else if (pv != null) {
                if (v != null) throw BAD_TOKEN.create(s);
                v = pv;
            } else if (s.endsWith("%")) {
                Integer n = number(s.substring(0, s.length() - 1));
                if (n == null || n < 1 || n > 100) throw BAD_TOKEN.create(s);
                size = n;
            } else {
                String num = s.toLowerCase(Locale.ROOT);
                if (num.endsWith("秒")) num = num.substring(0, num.length() - 1);
                else if (num.endsWith("s")) num = num.substring(0, num.length() - 1);
                Integer n = number(num);
                if (n == null || n < 0 || n > 86400) throw BAD_TOKEN.create(s);
                seconds = n;
            }
        }
        return new ImageArgs(t[0], h == null ? Align.H.CENTER : h, v == null ? Align.V.CENTER : v, seconds, size);
    }

    private static Integer number(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isHttpUrl(String s) {
        try {
            URI u = new URI(s);
            String scheme = u.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && u.getHost() != null && s.length() <= 2000;
        } catch (Exception e) {
            return false;
        }
    }

    /** 入力中の候補（URLのあとに位置・秒数を出します） */
    public static <S> CompletableFuture<Suggestions> suggest(CommandContext<S> ctx, SuggestionsBuilder builder) {
        String rem = builder.getRemaining();
        int sp = rem.lastIndexOf(' ');
        if (sp < 0) {
            return SharedSuggestionProvider.suggest(List.of("https://"), builder);
        }
        boolean hSet = false, vSet = false, secSet = false, sizeSet = false;
        String[] typed = rem.substring(0, sp).trim().split("\\s+");
        for (int i = 1; i < typed.length; i++) {
            String s = typed[i];
            Align.H ph = Align.parseH(s);
            Align.V pv = Align.parseV(s);
            if (ph != null && pv != null) {
                if (!hSet) hSet = true; else vSet = true;
            } else if (ph != null) hSet = true;
            else if (pv != null) vSet = true;
            else if (s.endsWith("%")) sizeSet = true;
            else secSet = true;
        }
        List<String> list = new ArrayList<>();
        if (!hSet) {
            list.add("left");
            list.add("center");
            list.add("right");
        }
        if (!vSet) {
            if (!list.contains("center")) list.add("center");
            list.add("down");
            list.add("up");
        }
        if (hSet && vSet) {
            if (!secSet) {
                list.add("5");
                list.add("10");
                list.add("30");
                list.add("0");
            }
            if (!sizeSet) {
                list.add("25%");
                list.add("50%");
            }
        }
        return SharedSuggestionProvider.suggest(list, builder.createOffset(builder.getStart() + sp + 1));
    }
}
