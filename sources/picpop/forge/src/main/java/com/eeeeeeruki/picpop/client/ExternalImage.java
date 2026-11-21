package com.eeeeeeruki.picpop.client;

import net.minecraft.resources.ResourceLocation;

/** ほかのmod（minEcraft twEEk）が持っているテクスチャをそのまま使う画像。テクスチャは閉じません。 */
record ExternalImage(ResourceLocation[] frames, int[] delays, int width, int height) implements ShownImage {
    @Override
    public ResourceLocation frame(long now) {
        if (frames.length == 1 || delays.length != frames.length) return frames[0];
        long total = 0;
        for (int d : delays) total += Math.max(1, d);
        long t = now % total;
        for (int i = 0; i < frames.length; i++) {
            t -= Math.max(1, delays[i]);
            if (t < 0) return frames[i];
        }
        return frames[frames.length - 1];
    }
}
