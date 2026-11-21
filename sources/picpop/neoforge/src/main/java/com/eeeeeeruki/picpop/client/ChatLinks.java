package com.eeeeeeruki.picpop.client;

import com.eeeeeeruki.picpop.ImageArgs;
import com.eeeeeeruki.picpop.PicPopConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** チャットの画像リンクのうしろに［画像を表示］ボタンを付けます */
final class ChatLinks {
    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"]+", Pattern.CASE_INSENSITIVE);
    private static final Set<String> IMAGE_HOSTS = Set.of("i.imgur.com", "i.gyazo.com", "pbs.twimg.com",
            "media.tenor.com", "media.giphy.com", "i.giphy.com");

    private ChatLinks() {}

    static Component process(Component message) {
        if (message == null || !PicPopConfig.CHAT_BUTTON.get()) return null;
        String text = message.getString();
        if (!text.contains("http")) return null;
        Matcher m = URL.matcher(text);
        Set<String> urls = new LinkedHashSet<>();
        boolean all = PicPopConfig.CHAT_BUTTON_ALL_LINKS.get();
        while (m.find() && urls.size() < 3) {
            String u = trim(m.group());
            if (ImageArgs.isHttpUrl(u) && (all || looksLikeImage(u))) urls.add(u);
        }
        if (urls.isEmpty()) return null;
        MutableComponent out = message.copy();
        for (String u : urls) {
            out.append(Component.literal(" "));
            out.append(Component.translatable("picpop.chat.button").withStyle(s -> s
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(false)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/imageview " + u))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("picpop.chat.hover", u)))));
        }
        return out;
    }

    private static String trim(String u) {
        while (!u.isEmpty() && ".,!?)]}>、。」』）".indexOf(u.charAt(u.length() - 1)) >= 0) u = u.substring(0, u.length() - 1);
        return u;
    }

    static boolean looksLikeImage(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".gif") || path.endsWith(".bmp")) return true;
            if (IMAGE_HOSTS.contains(host)) return true;
            String q = uri.getQuery() == null ? "" : uri.getQuery().toLowerCase(Locale.ROOT);
            return q.contains("format=png") || q.contains("format=jpg") || q.contains("format=gif");
        } catch (Exception e) {
            return false;
        }
    }
}
