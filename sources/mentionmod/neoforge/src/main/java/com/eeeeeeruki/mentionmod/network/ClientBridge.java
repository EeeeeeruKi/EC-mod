package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.client.ClientMentionHandler;
import com.eeeeeeruki.mentionmod.client.ReplyClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** 専用サーバーでクライアント用クラスを読み込まないための中継 */
final class ClientBridge {
    private ClientBridge() {}

    static void handle(MentionPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientMentionHandler.onMention(payload.sender(), payload.message(), payload.kind());
        }
    }

    static void handleAi(AiStatusPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ReplyClient.setAiStatus(payload.name(), payload.state());
        }
    }

    static void handleStatus(ReplyStatusPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ReplyClient.setRemoteReplying(payload.player(), payload.replying());
        }
    }
}
