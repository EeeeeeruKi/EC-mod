package com.eeeeeeruki.skyriders.net;

import com.eeeeeeruki.skyriders.SkyRiders;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** サーバー → クライアント：BGMを流す（空なら止める） */
public record BgmPayload(String sound) implements CustomPayload {
    public static final CustomPayload.Id<BgmPayload> ID = new CustomPayload.Id<>(SkyRiders.id("bgm"));
    public static final PacketCodec<RegistryByteBuf, BgmPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, BgmPayload::sound, BgmPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
