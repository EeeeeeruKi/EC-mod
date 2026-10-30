package com.eeeeeeruki.mentionmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** クライアント → サーバー：「いま返信モードかどうか」 */
public record ReplyStatePacket(boolean replying) {

    public static void encode(ReplyStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.replying());
    }

    public static ReplyStatePacket decode(FriendlyByteBuf buf) {
        return new ReplyStatePacket(buf.readBoolean());
    }

    public static void handle(ReplyStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            MentionNetwork.broadcastReplyStatus(player.getServer(), player.getUUID(), msg.replying());
        }
    }
}
