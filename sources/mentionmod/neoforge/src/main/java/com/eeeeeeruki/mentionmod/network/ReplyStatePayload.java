package com.eeeeeeruki.mentionmod.network;

import com.eeeeeeruki.mentionmod.MentionMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** クライアント → サーバー：「いま返信モードかどうか」 */
public record ReplyStatePayload(boolean replying) implements CustomPacketPayload {
    public static final Type<ReplyStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MentionMod.MOD_ID, "reply_state"));

    public static final StreamCodec<ByteBuf, ReplyStatePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ReplyStatePayload::new, ReplyStatePayload::replying);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
