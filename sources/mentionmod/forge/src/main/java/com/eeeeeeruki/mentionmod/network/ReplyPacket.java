package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.ReplyTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** クライアント → サーバー：「次に送るチャット(text)は target の snippet への返信です」 */
public record ReplyPacket(String target, String snippet, String text) {

    public static void encode(ReplyPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.target());
        buf.writeUtf(msg.snippet());
        buf.writeUtf(msg.text());
    }

    public static ReplyPacket decode(FriendlyByteBuf buf) {
        return new ReplyPacket(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(ReplyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ReplyTracker.put(player, msg.target(), msg.snippet(), msg.text());
        }
    }
}
