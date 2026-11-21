package com.eeeeeeruki.picpop.client;

import com.eeeeeeruki.picpop.Align;
import com.eeeeeeruki.picpop.ImageArgs;
import com.eeeeeeruki.picpop.PicPopConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

/** 表示中の画像（位置ごとに1枚）と、読み込んだ画像のキャッシュ。描画スレッドで動きます。 */
public final class ImageManager {
    private static final int CACHE_SIZE = 8;
    private static final int MARGIN = 6;
    private static final long FADE_IN = 150, FADE_OUT = 400;

    private record Slot(Align.H h, Align.V v) {}

    private record Display(String url, ShownImage image, int sizePercent, long start, long end) {}

    private static final Map<Slot, Display> DISPLAYS = new HashMap<>();
    private static final Map<Slot, Integer> GENERATION = new HashMap<>();
    private static final LinkedHashMap<String, LoadedImage> CACHE = new LinkedHashMap<>(16, 0.75f, true);

    private ImageManager() {}

    public static void show(ImageArgs a) {
        Minecraft mc = Minecraft.getInstance();
        Slot slot = new Slot(a.h(), a.v());
        int gen = GENERATION.merge(slot, 1, Integer::sum);
        int seconds = a.seconds() < 0 ? PicPopConfig.DEFAULT_SECONDS.get() : a.seconds();
        int size = a.size() > 0 ? a.size()
                : (a.h() == Align.H.CENTER && a.v() == Align.V.CENTER ? PicPopConfig.CENTER_SIZE.get() : PicPopConfig.CORNER_SIZE.get());

        if (ImageArgs.isTweekRef(a.url())) {
            ExternalImage ext = TweekCompat.frames(a.url().substring(6));
            if (ext != null) {
                put(slot, a.url(), ext, size, seconds);
            } else if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("picpop.error.load",
                        Component.translatable("picpop.error.tweek")).withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        LoadedImage cached = CACHE.get(a.url());
        if (cached != null) {
            put(slot, a.url(), cached, size, seconds);
            return;
        }
        if (mc.player != null) mc.player.displayClientMessage(Component.translatable("picpop.loading"), true);
        ImageLoader.load(a.url()).whenComplete((decoded, err) -> mc.execute(() -> {
            if (err != null) {
                Throwable e = err instanceof CompletionException && err.getCause() != null ? err.getCause() : err;
                Component reason = e instanceof ImageLoader.LoadException le ? le.reason : Component.literal(String.valueOf(e.getMessage()));
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.translatable("picpop.error.load", reason).withStyle(ChatFormatting.RED), false);
                    mc.player.displayClientMessage(Component.empty(), true);
                }
                return;
            }
            LoadedImage img;
            LoadedImage existing = CACHE.get(a.url());
            if (existing != null) {
                decoded.close();
                img = existing;
            } else {
                try {
                    img = LoadedImage.create(decoded);
                } catch (Exception ex) {
                    decoded.close();
                    if (mc.player != null) mc.player.displayClientMessage(
                            Component.translatable("picpop.error.load", Component.translatable("picpop.error.format")).withStyle(ChatFormatting.RED), false);
                    return;
                }
                CACHE.put(a.url(), img);
                trimCache();
            }
            if (mc.player != null) mc.player.displayClientMessage(Component.empty(), true);
            if (GENERATION.getOrDefault(slot, 0) == gen) put(slot, a.url(), img, size, seconds);
        }));
    }

    private static void put(Slot slot, String url, ShownImage img, int size, int seconds) {
        long now = System.currentTimeMillis();
        long end = seconds == 0 ? Long.MAX_VALUE : now + seconds * 1000L;
        DISPLAYS.put(slot, new Display(url, img, size, now, end));
        trimCache();
    }

    private static void trimCache() {
        if (CACHE.size() <= CACHE_SIZE) return;
        Iterator<Map.Entry<String, LoadedImage>> it = CACHE.entrySet().iterator();
        while (CACHE.size() > CACHE_SIZE && it.hasNext()) {
            Map.Entry<String, LoadedImage> e = it.next();
            boolean inUse = DISPLAYS.values().stream().anyMatch(d -> d.image == e.getValue());
            if (!inUse) {
                e.getValue().close();
                it.remove();
            }
        }
    }

    public static void clearAll() {
        DISPLAYS.clear();
        for (Slot s : new ArrayList<>(GENERATION.keySet())) GENERATION.merge(s, 1, Integer::sum);
    }

    /** ワールドから抜けたときにキャッシュも捨てます */
    public static void reset() {
        clearAll();
        for (LoadedImage img : CACHE.values()) img.close();
        CACHE.clear();
    }

    public static void render(GuiGraphics g) {
        if (DISPLAYS.isEmpty()) return;
        long now = System.currentTimeMillis();
        DISPLAYS.values().removeIf(d -> now >= d.end);
        if (DISPLAYS.isEmpty()) return;
        int sw = g.guiWidth(), sh = g.guiHeight();
        Minecraft mc = Minecraft.getInstance();
        boolean hotbar = mc.gameMode != null && mc.screen == null;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (Map.Entry<Slot, Display> e : DISPLAYS.entrySet()) {
            Slot s = e.getKey();
            Display d = e.getValue();
            ShownImage img = d.image;
            int iw = img.width(), ih = img.height();

            double maxW = sw * d.sizePercent / 100.0, maxH = sh * d.sizePercent / 100.0;
            double scale = Math.min(maxW / iw, maxH / ih);
            int w = Math.max(1, (int) Math.round(iw * scale));
            int h = Math.max(1, (int) Math.round(ih * scale));
            int bottomMargin = (s.h == Align.H.CENTER && hotbar) ? 52 : MARGIN; // ホットバーと重ならないように
            int x = switch (s.h) {
                case LEFT -> MARGIN;
                case CENTER -> (sw - w) / 2;
                case RIGHT -> sw - w - MARGIN;
            };
            int y = switch (s.v) {
                case UP -> MARGIN;
                case CENTER -> (sh - h) / 2;
                case DOWN -> sh - h - bottomMargin;
            };

            float alpha = 1f;
            long age = now - d.start;
            if (age < FADE_IN) alpha = age / (float) FADE_IN;
            if (d.end != Long.MAX_VALUE && d.end - now < FADE_OUT) alpha = Math.min(alpha, (d.end - now) / (float) FADE_OUT);
            RenderSystem.setShaderColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, alpha)));
            g.blit(img.frame(now), x, y, w, h, 0f, 0f, iw, ih, iw, ih);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
