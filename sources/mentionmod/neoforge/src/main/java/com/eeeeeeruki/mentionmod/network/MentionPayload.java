package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.MentionMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** サーバー → クライアント：「あなたがメンションされました」 */
public record MentionPayload(String sender, String message, int kind) implements CustomPacketPayload {
    public static final Type<MentionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MentionMod.MOD_ID, "mention"));

    public static final StreamCodec<ByteBuf, MentionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MentionPayload::sender,
            ByteBufCodecs.STRING_UTF8, MentionPayload::message,
            ByteBufCodecs.VAR_INT, MentionPayload::kind,
            MentionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
