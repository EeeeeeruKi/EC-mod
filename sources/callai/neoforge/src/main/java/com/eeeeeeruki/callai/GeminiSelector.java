package com.eeeeeeruki.callai;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * 「@ai」セレクター。誰にも当たらないセレクターとして読み込まれ、
 * /tell @ai はコマンドイベントでGemini宛てに振り分けられます。
 */
public final class GeminiSelector {
    public static final String TOKEN = "@ai";
    private static final Component TOOLTIP = Component.literal("AIにささやきます");

    private GeminiSelector() {}

    /** 読み取り位置が「@ai」なら読み進めて true を返します */
    public static boolean tryConsume(StringReader reader) {
        String rest = reader.getRemaining();
        if (!rest.startsWith(TOKEN)) return false;
        if (rest.length() > TOKEN.length() && !Character.isWhitespace(rest.charAt(TOKEN.length()))) return false;
        reader.setCursor(reader.getCursor() + TOKEN.length());
        return true;
    }

    /** 誰にも当たらないセレクターを作ります */
    public static EntitySelector build(EntitySelectorParser parser) {
        parser.setMaxResults(1);
        parser.setIncludesEntities(false);
        parser.addPredicate(entity -> false);
        return parser.getSelector();
    }

    /** /tell・/msg・/w の宛先で、入力中の文字に合えば「@ai」を候補に足します */
    public static void suggest(SuggestionsBuilder builder) {
        if (!isWhisperTarget(builder)) return;
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        if (TOKEN.startsWith(typed)) {
            builder.suggest(TOKEN, TOOLTIP);
        }
    }

    private static boolean isWhisperTarget(SuggestionsBuilder builder) {
        String before = builder.getInput().substring(0, Math.min(builder.getStart(), builder.getInput().length())).trim();
        if (before.startsWith("/")) before = before.substring(1);
        String cmd = before.toLowerCase(Locale.ROOT);
        if (cmd.startsWith("minecraft:")) cmd = cmd.substring("minecraft:".length());
        return cmd.equals("tell") || cmd.equals("msg") || cmd.equals("w");
    }
}
