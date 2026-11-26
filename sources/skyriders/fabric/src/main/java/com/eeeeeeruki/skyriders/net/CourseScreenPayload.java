package com.eeeeeeruki.skyriders.net;

import com.eeeeeeruki.skyriders.SkyRiders;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** サーバー → クライアント：管理画面を開く／中身を更新する（中身はJSON） */
public record CourseScreenPayload(String json) implements CustomPayload {
    public static final CustomPayload.Id<CourseScreenPayload> ID = new CustomPayload.Id<>(SkyRiders.id("course_screen"));
    public static final PacketCodec<RegistryByteBuf, CourseScreenPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, CourseScreenPayload::json, CourseScreenPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
