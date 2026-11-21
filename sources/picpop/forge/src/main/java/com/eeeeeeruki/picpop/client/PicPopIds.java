package com.eeeeeeruki.picpop.client;

import net.minecraft.resources.ResourceLocation;

final class PicPopIds {
    private PicPopIds() {}

    static ResourceLocation texture(int n) {
        return new ResourceLocation("picpop", "dynamic/image_" + n);
    }
}
