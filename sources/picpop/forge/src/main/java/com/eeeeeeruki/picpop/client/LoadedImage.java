package com.eeeeeeruki.picpop.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** GPUに乗せた画像。GIFはコマが変わるたびにテクスチャを書き換えます。描画スレッド専用です。 */
final class LoadedImage {
    private static int counter = 0;

    final ResourceLocation id;
    final int width, height;
    private final DynamicTexture texture;
    private final List<NativeImage> frames; // GIFのときだけ（静止画は texture が持つ）
    private final int[] delays;
    private final int totalMs;
    private final long startMs = System.currentTimeMillis();
    private final boolean blur;
    private int current = 0;
    private boolean closed = false;

    private LoadedImage(ImageLoader.Decoded d) {
        this.width = d.width();
        this.height = d.height();
        this.blur = Math.max(width, height) > 128;
        this.id = PicPopIds.texture(counter++);
        if (d.frames().size() == 1) {
            this.texture = new DynamicTexture(d.frames().get(0));
            this.frames = List.of();
            this.delays = new int[0];
            this.totalMs = 0;
        } else {
            NativeImage first = new NativeImage(width, height, false);
            first.copyFrom(d.frames().get(0));
            this.texture = new DynamicTexture(first);
            this.frames = d.frames();
            this.delays = d.delays();
            int t = 0;
            for (int x : delays) t += x;
            this.totalMs = Math.max(1, t);
        }
        reupload();
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    static LoadedImage create(ImageLoader.Decoded d) {
        return new LoadedImage(d);
    }

    private void reupload() {
        NativeImage px = texture.getPixels();
        if (px == null) return;
        texture.bind();
        px.upload(0, 0, 0, 0, 0, px.getWidth(), px.getHeight(), blur, true, false, false);
    }

    /** 今の時間に合うコマにします */
    void tick(long now) {
        if (closed || frames.isEmpty()) return;
        long t = (now - startMs) % totalMs;
        int i = 0;
        long acc = 0;
        while (i < delays.length - 1) {
            acc += delays[i];
            if (t < acc) break;
            i++;
        }
        if (i != current) {
            NativeImage px = texture.getPixels();
            if (px != null) {
                px.copyFrom(frames.get(i));
                reupload();
            }
            current = i;
        }
    }

    void close() {
        if (closed) return;
        closed = true;
        Minecraft.getInstance().getTextureManager().release(id);
        for (NativeImage f : frames) f.close();
    }
}
