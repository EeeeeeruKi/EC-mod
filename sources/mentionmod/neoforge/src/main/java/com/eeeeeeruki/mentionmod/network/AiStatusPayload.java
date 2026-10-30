package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.MentionMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** サーバー → クライアント：「AI（name）がいま考え中／返信中／なにもしていない」 */
public record AiStatusPayload(String name, int state) implements CustomPacketPayload {
    public static final Type<AiStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MentionMod.MOD_ID, "ai_status"));

    public static final StreamCodec<ByteBuf, AiStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AiStatusPayload::name,
            ByteBufCodecs.VAR_INT, AiStatusPayload::state,
            AiStatusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
