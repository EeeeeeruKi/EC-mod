package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.client.MentionSuggestions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** サーバー → クライアント：ログインした時に「このサーバーで @ai が使えるか」を伝える（候補の表示に使う） */
public record FeaturesPacket(boolean ai) {

    public static void encode(FeaturesPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.ai());
    }

    public static FeaturesPacket decode(FriendlyByteBuf buf) {
        return new FeaturesPacket(buf.readBoolean());
    }

    public static void handle(FeaturesPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MentionSuggestions.setServerAi(msg.ai());
        }
    }
}
