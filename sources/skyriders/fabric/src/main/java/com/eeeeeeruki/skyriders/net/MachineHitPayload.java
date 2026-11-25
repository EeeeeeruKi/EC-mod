package com.eeeeeeruki.skyriders.net;

import com.eeeeeeruki.skyriders.SkyRiders;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** サーバー → 乗っている人：攻撃を受けた時の吹っ飛び（動きは乗っている人の画面で計算しているため） */
public record MachineHitPayload(int entityId, float vx, float vy, float vz, int stun) implements CustomPayload {
    public static final CustomPayload.Id<MachineHitPayload> ID = new CustomPayload.Id<>(SkyRiders.id("hit"));
    public static final PacketCodec<RegistryByteBuf, MachineHitPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, MachineHitPayload::entityId,
            PacketCodecs.FLOAT, MachineHitPayload::vx,
            PacketCodecs.FLOAT, MachineHitPayload::vy,
            PacketCodecs.FLOAT, MachineHitPayload::vz,
            PacketCodecs.VAR_INT, MachineHitPayload::stun,
            MachineHitPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
