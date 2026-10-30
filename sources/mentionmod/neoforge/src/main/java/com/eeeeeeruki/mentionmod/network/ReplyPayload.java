package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.MentionMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** クライアント → サーバー：「次に送るチャット(text)は target の snippet への返信です」 */
public record ReplyPayload(String target, String snippet, String text) implements CustomPacketPayload {
    public static final Type<ReplyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MentionMod.MOD_ID, "reply"));

    public static final StreamCodec<ByteBuf, ReplyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ReplyPayload::target,
            ByteBufCodecs.STRING_UTF8, ReplyPayload::snippet,
            ByteBufCodecs.STRING_UTF8, ReplyPayload::text,
            ReplyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
