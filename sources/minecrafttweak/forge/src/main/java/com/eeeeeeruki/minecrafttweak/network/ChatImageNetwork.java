package com.eeeeeeruki.minecrafttweak.network;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.server.ChatImageServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * チャットの画像の通信。サーバーにこのmodがなくても入れるように、どの版でも（なくても）受け入れる。
 * サーバーへの1回の通信は 32KB までなので、画像は 30000 バイトずつに分けて送る。
 */
public final class ChatImageNetwork {
    public static final int UPLOAD_CHUNK = 30000;
    public static final int DATA_CHUNK = 512 * 1024;
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MinecraftTweak.MODID, "chat_image"), () -> VERSION, v -> true, v -> true);
    private static boolean registered;

    private ChatImageNetwork() {
    }

    /** 画像ファイルの一部（プレイヤー → サーバー）。 */
    public record Upload(int index, int total, String caption, byte[] data) {
        void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(index);
            buf.writeVarInt(total);
            buf.writeUtf(caption, 256);
            buf.writeByteArray(data);
        }

        static Upload decode(FriendlyByteBuf buf) {
            return new Upload(buf.readVarInt(), buf.readVarInt(), buf.readUtf(256), buf.readByteArray(UPLOAD_CHUNK));
        }
    }

    /** 「この画像をください」（プレイヤー → サーバー）。 */
    public record Request(String imageId) {
        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(imageId, 40);
        }

        static Request decode(FriendlyByteBuf buf) {
            return new Request(buf.readUtf(40));
        }
    }

    /** 画像の一部（サーバー → プレイヤー）。total が 0 なら「その画像はもうない」。 */
    public record Data(String imageId, int index, int total, byte[] data) {
        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(imageId, 40);
            buf.writeVarInt(index);
            buf.writeVarInt(total);
            buf.writeByteArray(data);
        }

        static Data decode(FriendlyByteBuf buf) {
            return new Data(buf.readUtf(40), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(DATA_CHUNK));
        }
    }

    public static void register(IEventBus modBus) {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.messageBuilder(Upload.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Upload::encode).decoder(Upload::decode)
                .consumerMainThread((Upload p, Supplier<NetworkEvent.Context> ctx) -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null) {
                        ChatImageServer.onUpload(player, p.index(), p.total(), p.caption(), p.data());
                    }
                }).add();
        CHANNEL.messageBuilder(Request.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Request::encode).decoder(Request::decode)
                .consumerMainThread((Request p, Supplier<NetworkEvent.Context> ctx) -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null) {
                        ChatImageServer.onRequest(player, p.imageId());
                    }
                }).add();
        CHANNEL.messageBuilder(Data.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Data::encode).decoder(Data::decode)
                .consumerMainThread((Data p, Supplier<NetworkEvent.Context> ctx) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHook.onData(p)))
                .add();
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ChatImageServer.onLogout(e.getEntity().getUUID()));
    }

    private static final class ClientHook {
        static void onData(Data p) {
            com.eeeeeeruki.minecrafttweak.client.feature.ChatImages.onServerData(p.imageId(), p.index(), p.total(), p.data());
        }
    }

    public static void sendData(ServerPlayer player, String imageId, int index, int total, byte[] data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Data(imageId, index, total, data));
    }

    public static void sendUpload(int index, int total, String caption, byte[] data) {
        CHANNEL.sendToServer(new Upload(index, total, caption, data));
    }

    public static void sendRequest(String imageId) {
        CHANNEL.sendToServer(new Request(imageId));
    }
}
