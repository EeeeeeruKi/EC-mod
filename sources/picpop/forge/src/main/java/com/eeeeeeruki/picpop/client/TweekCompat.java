package com.eeeeeeruki.picpop.client;

import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;

/**
 * minEcraft twEEk の「チャットの画像」との連携。twEEk が無くても動くように、すべてリフレクションで呼びます。
 * - twEEk がチャットの中に表示するリンクには、PicPop の［画像を表示］ボタンを付けません（画像をクリックすれば開けるため）。
 * - twEEk でファイルとして送られた画像（tweek:番号）は、twEEk が読み込んだテクスチャをそのまま大きく表示します。
 */
final class TweekCompat {
    private static final String CLASS = "com.eeeeeeruki.minecrafttweak.client.feature.ChatImages";
    private static boolean tried;
    private static Method showsLinkInChat;
    private static Method picpopFrames;

    private TweekCompat() {}

    private static void init() {
        if (tried) return;
        tried = true;
        try {
            Class<?> c = Class.forName(CLASS, false, TweekCompat.class.getClassLoader());
            try {
                showsLinkInChat = c.getMethod("showsLinkInChat", String.class);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                picpopFrames = c.getMethod("picpopFrames", String.class);
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Throwable ignored) {
            // twEEk が入っていない
        }
    }

    static boolean showsLinkInChat(String url) {
        init();
        if (showsLinkInChat == null) return false;
        try {
            return (Boolean) showsLinkInChat.invoke(null, url);
        } catch (Throwable e) {
            return false;
        }
    }

    static boolean available() {
        init();
        return picpopFrames != null;
    }

    /** twEEk の画像（ファイルで送られたもの）。まだ読み込まれていなければ null */
    static ExternalImage frames(String id) {
        init();
        if (picpopFrames == null) return null;
        try {
            Object[] r = (Object[]) picpopFrames.invoke(null, id);
            if (r == null || r.length < 4) return null;
            ResourceLocation[] frames = (ResourceLocation[]) r[0];
            int[] delays = (int[]) r[1];
            int w = (Integer) r[2], h = (Integer) r[3];
            if (frames.length == 0 || w <= 0 || h <= 0) return null;
            return new ExternalImage(frames, delays, w, h);
        } catch (Throwable e) {
            return null;
        }
    }
}
