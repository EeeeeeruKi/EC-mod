package com.eeeeeeruki.picpop.client;

import net.minecraft.resources.ResourceLocation;

/** 画面に出せる画像（自分で読み込んだもの、または minEcraft twEEk が読み込んだもの） */
interface ShownImage {
    /** 今の時間に描くテクスチャ */
    ResourceLocation frame(long now);

    int width();

    int height();
}
