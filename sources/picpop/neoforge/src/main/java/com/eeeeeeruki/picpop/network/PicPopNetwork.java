package com.eeeeeeruki.picpop.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** サーバー → クライアントの「画像を出して／消して」の通信。相手に PicPop が無くても接続できます。 */
public final class PicPopNetwork {
    private PicPopNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(ShowImagePacket.TYPE, ShowImagePacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> ClientBridge.handle(packet)));
    }

    public static boolean clientHasMod(ServerPlayer player) {
        try {
            return player.connection.hasChannel(ShowImagePacket.TYPE);
        } catch (Exception e) {
            return false;
        }
    }

    public static void sendTo(ServerPlayer player, ShowImagePacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    /** クライアントのクラスをサーバーで読み込まないように分けています */
    static final class ClientBridge {
        private ClientBridge() {}

        static void handle(ShowImagePacket p) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.eeeeeeruki.picpop.client.ClientPacketHandler.handle(p);
            }
        }
    }
}
