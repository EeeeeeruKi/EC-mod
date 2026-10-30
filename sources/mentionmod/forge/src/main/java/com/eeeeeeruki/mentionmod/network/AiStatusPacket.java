package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.client.ReplyClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** サーバー → クライアント：「AI（name）がいま考え中／返信中／なにもしていない」 */
public record AiStatusPacket(String name, int state) {

    public static void encode(AiStatusPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name());
        buf.writeVarInt(msg.state());
    }

    public static AiStatusPacket decode(FriendlyByteBuf buf) {
        return new AiStatusPacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(AiStatusPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ReplyClient.setAiStatus(msg.name(), msg.state());
        }
    }
}
