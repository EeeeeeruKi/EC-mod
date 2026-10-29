package com.eeeeeeruki.callai.network;

import com.eeeeeeruki.callai.ScreenshotService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** クライアント → サーバー：スクリーンショット（JPEG）を分けて送ります。total が 0 なら撮れませんでした */
public record ScreenshotChunkPacket(int id, int index, int total, byte[] data) {
    public static final int MAX_CHUNK = 30000;

    public static void encode(ScreenshotChunkPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.id());
        buf.writeVarInt(p.index());
        buf.writeVarInt(p.total());
        buf.writeByteArray(p.data());
    }

    public static ScreenshotChunkPacket decode(FriendlyByteBuf buf) {
        return new ScreenshotChunkPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(MAX_CHUNK));
    }

    public static void handle(ScreenshotChunkPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            ScreenshotService.onChunk(sender, p.id(), p.index(), p.total(), p.data());
        }
    }
}
