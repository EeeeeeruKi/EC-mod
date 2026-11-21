package com.eeeeeeruki.picpop.network;

import com.eeeeeeruki.picpop.ImageArgs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** clear が true なら全部消す。それ以外は url の画像を h / v の位置に出す。 */
public record ShowImagePacket(boolean clear, String url, int h, int v, int seconds, int size) implements CustomPacketPayload {
    public static final Type<ShowImagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("picpop", "show_image"));
    public static final StreamCodec<FriendlyByteBuf, ShowImagePacket> STREAM_CODEC =
            StreamCodec.ofMember(ShowImagePacket::write, ShowImagePacket::read);

    public static ShowImagePacket show(ImageArgs a) {
        return new ShowImagePacket(false, a.url(), a.h().ordinal(), a.v().ordinal(), a.seconds(), a.size());
    }

    public static ShowImagePacket clearAll() {
        return new ShowImagePacket(true, "", 0, 0, 0, 0);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(clear);
        buf.writeUtf(url, 2048);
        buf.writeByte(h);
        buf.writeByte(v);
        buf.writeInt(seconds);
        buf.writeInt(size);
    }

    private static ShowImagePacket read(FriendlyByteBuf buf) {
        return new ShowImagePacket(buf.readBoolean(), buf.readUtf(2048), buf.readByte(), buf.readByte(), buf.readInt(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
