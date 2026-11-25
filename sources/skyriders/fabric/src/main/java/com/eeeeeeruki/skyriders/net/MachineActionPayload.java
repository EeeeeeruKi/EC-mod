package com.eeeeeeruki.skyriders.net;

import com.eeeeeeruki.skyriders.SkyRiders;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** クライアント → サーバー：マシンの操作（スピン・チャージ・使用・スペシャル・降車） */
public record MachineActionPayload(int action) implements CustomPayload {
    public static final int SPIN = 0;
    public static final int CHARGE_START = 1;
    public static final int CHARGE_STOP = 2;
    public static final int USE = 3;
    public static final int SPECIAL = 4;
    public static final int EXIT_OR_SWAP = 5;

    public static final CustomPayload.Id<MachineActionPayload> ID = new CustomPayload.Id<>(SkyRiders.id("action"));
    public static final PacketCodec<RegistryByteBuf, MachineActionPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, MachineActionPayload::action, MachineActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
