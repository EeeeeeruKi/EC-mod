package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.MentionMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** サーバー → クライアント：ログインした時に「このサーバーで @ai が使えるか」を伝える（候補の表示に使う） */
public record FeaturesPayload(boolean ai) implements CustomPacketPayload {
    public static final Type<FeaturesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MentionMod.MOD_ID, "features"));

    public static final StreamCodec<ByteBuf, FeaturesPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(FeaturesPayload::new, FeaturesPayload::ai);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
