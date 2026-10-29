package com.eeeeeeruki.callai.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 画面の読み取り用の通信です。クライアントに Call AI が無くても入れるように、相手に無くてもOKにしています。
 */
public final class CallAINetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("callai", "main"), () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));

    private CallAINetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ScreenshotRequestPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ScreenshotRequestPacket::encode)
                .decoder(ScreenshotRequestPacket::decode)
                .consumerMainThread(ScreenshotRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(ScreenshotChunkPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ScreenshotChunkPacket::encode)
                .decoder(ScreenshotChunkPacket::decode)
                .consumerMainThread(ScreenshotChunkPacket::handle)
                .add();
    }

    public static boolean clientHasMod(ServerPlayer player) {
        try {
            return CHANNEL.isRemotePresent(player.connection.connection);
        } catch (Exception e) {
            return false;
        }
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
