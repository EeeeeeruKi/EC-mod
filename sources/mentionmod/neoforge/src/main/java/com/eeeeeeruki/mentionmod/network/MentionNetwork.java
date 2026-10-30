package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.ReplyTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

public final class MentionNetwork {
    private MentionNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // optional: このmodが入っていないクライアントでもサーバーに入れる
        PayloadRegistrar registrar = event.registrar("4").optional();
        registrar.playToClient(MentionPayload.TYPE, MentionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBridge.handle(payload)));
        registrar.playToClient(ReplyStatusPayload.TYPE, ReplyStatusPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBridge.handleStatus(payload)));
        registrar.playToClient(AiStatusPayload.TYPE, AiStatusPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBridge.handleAi(payload)));
        registrar.playToServer(ReplyPayload.TYPE, ReplyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ReplyTracker.put(sp, payload.target(), payload.snippet(), payload.text());
                    }
                }));
        registrar.playToServer(ReplyStatePayload.TYPE, ReplyStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        broadcastReplyStatus(sp.getServer(), sp.getUUID(), payload.replying());
                    }
                }));
    }

    public static void sendMention(ServerPlayer target, String sender, String message, int kind) {
        try {
            if (!target.connection.hasChannel(MentionPayload.TYPE)) return;
            PacketDistributor.sendToPlayer(target, new MentionPayload(sender, message, kind));
        } catch (Exception ignored) {
            // クライアントにmodが無いなどで送れなくても、チャット自体は止めない
        }
    }

    public static void sendAiStatus(ServerPlayer target, String name, int state) {
        try {
            if (!target.connection.hasChannel(AiStatusPayload.TYPE)) return;
            PacketDistributor.sendToPlayer(target, new AiStatusPayload(name, state));
        } catch (Exception ignored) {
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
        ReplyStatusPayload payload = new ReplyStatusPayload(player, replying);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            try {
                if (p.connection.hasChannel(ReplyStatusPayload.TYPE)) {
                    PacketDistributor.sendToPlayer(p, payload);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
