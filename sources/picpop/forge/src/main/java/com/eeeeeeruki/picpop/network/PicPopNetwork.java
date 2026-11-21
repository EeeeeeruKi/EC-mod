package com.eeeeeeruki.picpop.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** サーバー → クライアントの「画像を出して／消して」の通信。相手に PicPop が無くても接続できます。 */
public final class PicPopNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("picpop", "main"), () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));

    private PicPopNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ShowImagePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ShowImagePacket::encode)
                .decoder(ShowImagePacket::decode)
                .consumerMainThread(ShowImagePacket::handle)
                .add();
    }

    public static boolean clientHasMod(ServerPlayer player) {
        try {
            return CHANNEL.isRemotePresent(player.connection.connection);
        } catch (Exception e) {
            return false;
        }
    }

    public static void sendTo(ServerPlayer player, ShowImagePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
