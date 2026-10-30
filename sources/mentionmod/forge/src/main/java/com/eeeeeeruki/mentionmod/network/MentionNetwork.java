package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.MentionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;

public final class MentionNetwork {
    private static final String PROTOCOL = "4";

    // acceptMissingOr: このmodが入っていないクライアント・サーバーとも接続できる
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MentionMod.MOD_ID, "main"),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL));

    private MentionNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(MentionPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MentionPacket::encode)
                .decoder(MentionPacket::decode)
                .consumerMainThread(MentionPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReplyPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReplyPacket::encode)
                .decoder(ReplyPacket::decode)
                .consumerMainThread(ReplyPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReplyStatePacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReplyStatePacket::encode)
                .decoder(ReplyStatePacket::decode)
                .consumerMainThread(ReplyStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(ReplyStatusPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ReplyStatusPacket::encode)
                .decoder(ReplyStatusPacket::decode)
                .consumerMainThread(ReplyStatusPacket::handle)
                .add();
        CHANNEL.messageBuilder(AiStatusPacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AiStatusPacket::encode)
                .decoder(AiStatusPacket::decode)
                .consumerMainThread(AiStatusPacket::handle)
                .add();
    }

    public static void sendAiStatus(ServerPlayer target, String name, int state) {
        try {
            if (!CHANNEL.isRemotePresent(target.connection.connection)) return;
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new AiStatusPacket(name, state));
        } catch (Exception ignored) {
        }
    }

    public static void sendMention(ServerPlayer target, String sender, String message, int kind) {
        try {
            if (!CHANNEL.isRemotePresent(target.connection.connection)) return;
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new MentionPacket(sender, message, kind));
        } catch (Exception ignored) {
            // クライアントにmodが無いなどで送れなくても、チャット自体は止めない
        }
    }

    /** AIの状態を全員に伝える（チャットの @gemini 用） */
    public static void broadcastAiStatus(MinecraftServer server, String name, int state) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendAiStatus(p, name, state);
    }

    /** 返信モードかどうかを全員に伝える（Watut の「返信中」表示に使う） */
    public static void broadcastReplyStatus(MinecraftServer server, UUID player, boolean replying) {
        if (server == null) return;
        ReplyStatusPacket packet = new ReplyStatusPacket(player, replying);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            try {
                if (CHANNEL.isRemotePresent(p.connection.connection)) {
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
