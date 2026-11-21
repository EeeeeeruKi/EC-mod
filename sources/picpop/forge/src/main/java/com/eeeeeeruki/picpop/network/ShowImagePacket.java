package com.eeeeeeruki.picpop.network;

import com.eeeeeeruki.picpop.ImageArgs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** clear が true なら全部消す。それ以外は url の画像を h / v の位置に出す。 */
public record ShowImagePacket(boolean clear, String url, int h, int v, int seconds, int size) {
    public static ShowImagePacket show(ImageArgs a) {
        return new ShowImagePacket(false, a.url(), a.h().ordinal(), a.v().ordinal(), a.seconds(), a.size());
    }

    public static ShowImagePacket clearAll() {
        return new ShowImagePacket(true, "", 0, 0, 0, 0);
    }

    public static void encode(ShowImagePacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.clear);
        buf.writeUtf(p.url, 2048);
        buf.writeByte(p.h);
        buf.writeByte(p.v);
        buf.writeInt(p.seconds);
        buf.writeInt(p.size);
    }

    public static ShowImagePacket decode(FriendlyByteBuf buf) {
        return new ShowImagePacket(buf.readBoolean(), buf.readUtf(2048), buf.readByte(), buf.readByte(), buf.readInt(), buf.readInt());
    }

    public static void handle(ShowImagePacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.eeeeeeruki.picpop.client.ClientPacketHandler.handle(p));
        ctx.get().setPacketHandled(true);
    }
}
