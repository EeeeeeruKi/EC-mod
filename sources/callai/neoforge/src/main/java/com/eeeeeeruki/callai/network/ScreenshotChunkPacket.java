package com.eeeeeeruki.callai.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** クライアント → サーバー：スクリーンショット（JPEG）を分けて送ります。total が 0 なら撮れませんでした */
public record ScreenshotChunkPacket(int id, int index, int total, byte[] data) implements CustomPacketPayload {
    public static final int MAX_CHUNK = 30000;
    public static final Type<ScreenshotChunkPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("callai", "screenshot_chunk"));
    public static final StreamCodec<ByteBuf, ScreenshotChunkPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScreenshotChunkPacket::id,
            ByteBufCodecs.VAR_INT, ScreenshotChunkPacket::index,
            ByteBufCodecs.VAR_INT, ScreenshotChunkPacket::total,
            ByteBufCodecs.byteArray(MAX_CHUNK), ScreenshotChunkPacket::data,
            ScreenshotChunkPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
