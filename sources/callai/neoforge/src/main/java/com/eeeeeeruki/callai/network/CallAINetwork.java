package com.eeeeeeruki.callai.network;

import com.eeeeeeruki.callai.ScreenshotService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 画面の読み取り用の通信です。クライアントに Call AI が無くても入れるように optional にしています。
 */
public final class CallAINetwork {
    private CallAINetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(ScreenshotRequestPacket.TYPE, ScreenshotRequestPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> ClientBridge.capture(packet.id())));
        registrar.playToServer(ScreenshotChunkPacket.TYPE, ScreenshotChunkPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        ScreenshotService.onChunk(sp, packet.id(), packet.index(), packet.total(), packet.data());
                    }
                }));
    }

    public static boolean clientHasMod(ServerPlayer player) {
        try {
            return player.connection.hasChannel(ScreenshotRequestPacket.TYPE);
        } catch (Exception e) {
            return false;
        }
    }

    public static void sendTo(ServerPlayer player, ScreenshotRequestPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(ScreenshotChunkPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    /** クライアントのクラスをサーバーで読み込まないように分けています */
    static final class ClientBridge {
        private ClientBridge() {}

        static void capture(int id) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.eeeeeeruki.callai.client.ScreenshotCapture.captureAndSend(id);
            }
        }
    }
}
