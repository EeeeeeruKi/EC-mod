package com.eeeeeeruki.skyriders.net;

import com.eeeeeeruki.skyriders.SkyRiders;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** サーバー → 乗っている人：マシンをスタート地点／TA地点へ動かす。freeze はカウントダウンで動けないtick */
public record RaceControlPayload(int entityId, double x, double y, double z, float yaw, int freeze) implements CustomPayload {
    public static final CustomPayload.Id<RaceControlPayload> ID = new CustomPayload.Id<>(SkyRiders.id("race_control"));
    public static final PacketCodec<RegistryByteBuf, RaceControlPayload> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeVarInt(p.entityId());
                buf.writeDouble(p.x());
                buf.writeDouble(p.y());
                buf.writeDouble(p.z());
                buf.writeFloat(p.yaw());
                buf.writeVarInt(p.freeze());
            },
            buf -> new RaceControlPayload(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
