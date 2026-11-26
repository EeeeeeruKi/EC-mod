package com.eeeeeeruki.skyriders.net;

import com.eeeeeeruki.skyriders.SkyRiders;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** サーバー → クライアント：レースの状態（HUD用）。elapsed は今までのtick */
public record RaceStatePayload(boolean active, String course, int lap, int laps, int next, int total, int elapsed)
        implements CustomPayload {
    public static final CustomPayload.Id<RaceStatePayload> ID = new CustomPayload.Id<>(SkyRiders.id("race_state"));
    public static final PacketCodec<RegistryByteBuf, RaceStatePayload> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeBoolean(p.active());
                buf.writeString(p.course());
                buf.writeVarInt(p.lap());
                buf.writeVarInt(p.laps());
                buf.writeVarInt(p.next());
                buf.writeVarInt(p.total());
                buf.writeVarInt(p.elapsed());
            },
            buf -> new RaceStatePayload(buf.readBoolean(), buf.readString(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
