package com.eeeeeeruki.minecrafttweak.network;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.server.ChatImageServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * チャットの画像の通信。サーバーにこのmodがなくても入れるように、なくてもいい通信（optional）にしている。
 * サーバーへの1回の通信は 32KB までなので、画像は 30000 バイトずつに分けて送る。
 */
public final class ChatImageNetwork {
    public static final int UPLOAD_CHUNK = 30000;
    public static final int DATA_CHUNK = 512 * 1024;
    private static boolean registered;

    private ChatImageNetwork() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MinecraftTweak.MODID, path);
    }

    /** 画像ファイルの一部（プレイヤー → サーバー）。 */
    public record Upload(int index, int total, String caption, byte[] data) implements CustomPacketPayload {
        public static final Type<Upload> TYPE = new Type<>(id("chat_image_upload"));
        public static final StreamCodec<FriendlyByteBuf, Upload> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.index);
                    buf.writeVarInt(p.total);
                    buf.writeUtf(p.caption, 256);
                    buf.writeByteArray(p.data);
                },
                buf -> new Upload(buf.readVarInt(), buf.readVarInt(), buf.readUtf(256), buf.readByteArray(UPLOAD_CHUNK)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 「この画像をください」（プレイヤー → サーバー）。 */
    public record Request(String imageId) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(id("chat_image_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.imageId, 40),
                buf -> new Request(buf.readUtf(40)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 画像の一部（サーバー → プレイヤー）。total が 0 なら「その画像はもうない」。 */
    public record Data(String imageId, int index, int total, byte[] data) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(id("chat_image_data"));
        public static final StreamCodec<FriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.imageId, 40);
                    buf.writeVarInt(p.index);
                    buf.writeVarInt(p.total);
                    buf.writeByteArray(p.data);
                },
                buf -> new Data(buf.readUtf(40), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(DATA_CHUNK)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(IEventBus modBus) {
        if (registered) {
            return;
        }
        registered = true;
        modBus.addListener(ChatImageNetwork::onRegister);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ChatImageServer.onLogout(e.getEntity().getUUID()));
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(Upload.TYPE, Upload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) {
                ChatImageServer.onUpload(player, p.index(), p.total(), p.caption(), p.data());
            }
        });
        registrar.playToServer(Request.TYPE, Request.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) {
                ChatImageServer.onRequest(player, p.imageId());
            }
        });
        registrar.playToClient(Data.TYPE, Data.CODEC, (p, ctx) -> ClientHook.onData(p));
    }

    private static final class ClientHook {
        static void onData(Data p) {
            com.eeeeeeruki.minecrafttweak.client.feature.ChatImages.onServerData(p.imageId(), p.index(), p.total(), p.data());
        }
    }

    public static void sendData(ServerPlayer player, String imageId, int index, int total, byte[] data) {
        PacketDistributor.sendToPlayer(player, new Data(imageId, index, total, data));
    }

    public static void sendUpload(int index, int total, String caption, byte[] data) {
        PacketDistributor.sendToServer(new Upload(index, total, caption, data));
    }

    public static void sendRequest(String imageId) {
        PacketDistributor.sendToServer(new Request(imageId));
    }
}
