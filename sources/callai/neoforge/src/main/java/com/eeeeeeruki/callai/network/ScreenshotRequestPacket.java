package com.eeeeeeruki.callai.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** サーバー → クライアント：「今の画面を撮って送ってください」 */
public record ScreenshotRequestPacket(int id) implements CustomPacketPayload {
    public static final Type<ScreenshotRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("callai", "screenshot_request"));
    public static final StreamCodec<ByteBuf, ScreenshotRequestPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ScreenshotRequestPacket::new, ScreenshotRequestPacket::id);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
