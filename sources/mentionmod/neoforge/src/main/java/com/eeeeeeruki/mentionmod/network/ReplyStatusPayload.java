package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.MentionMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** サーバー → クライアント：「player が返信モードになった／やめた」 */
public record ReplyStatusPayload(UUID player, boolean replying) implements CustomPacketPayload {
    public static final Type<ReplyStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MentionMod.MOD_ID, "reply_status"));

    public static final StreamCodec<ByteBuf, ReplyStatusPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ReplyStatusPayload::player,
            ByteBufCodecs.BOOL, ReplyStatusPayload::replying,
            ReplyStatusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
