package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.client.ReplyClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** サーバー → クライアント：「player が返信モードになった／やめた」 */
public record ReplyStatusPacket(UUID player, boolean replying) {

    public static void encode(ReplyStatusPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.player());
        buf.writeBoolean(msg.replying());
    }

    public static ReplyStatusPacket decode(FriendlyByteBuf buf) {
        return new ReplyStatusPacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(ReplyStatusPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ReplyClient.setRemoteReplying(msg.player(), msg.replying());
        }
    }
}
