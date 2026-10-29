package com.eeeeeeruki.callai.network;

import com.eeeeeeruki.callai.client.ScreenshotCapture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** サーバー → クライアント：「今の画面を撮って送ってください」 */
public record ScreenshotRequestPacket(int id) {
    public static void encode(ScreenshotRequestPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.id());
    }

    public static ScreenshotRequestPacket decode(FriendlyByteBuf buf) {
        return new ScreenshotRequestPacket(buf.readVarInt());
    }

    public static void handle(ScreenshotRequestPacket p, Supplier<NetworkEvent.Context> ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ScreenshotCapture.captureAndSend(p.id());
        }
    }
}
