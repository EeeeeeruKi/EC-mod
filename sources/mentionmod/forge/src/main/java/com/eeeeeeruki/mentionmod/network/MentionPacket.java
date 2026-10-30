package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.client.ClientMentionHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** サーバー → クライアント：「あなたがメンションされました」 */
public record MentionPacket(String sender, String message, int kind) {

    public static void encode(MentionPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.sender());
        buf.writeUtf(msg.message());
        buf.writeVarInt(msg.kind());
    }

    public static MentionPacket decode(FriendlyByteBuf buf) {
        return new MentionPacket(buf.readUtf(), buf.readUtf(), buf.readVarInt());
    }

    public static void handle(MentionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // consumerMainThread なのでメインスレッドで呼ばれる
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientMentionHandler.onMention(msg.sender(), msg.message(), msg.kind());
        }
    }
}
