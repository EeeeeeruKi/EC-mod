package com.eeeeeeruki.skyriders.net;

import com.eeeeeeruki.skyriders.SkyRiders;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** クライアント → サーバー：管理画面での操作 */
public record CourseEditPayload(int action, String text, int value) implements CustomPayload {
    public static final int SELECT = 1;        // text: コース名
    public static final int LAPS = 2;          // value: 増やす数（-1/+1）
    public static final int NUMBER = 3;        // value: 増やす数（-1/+1）
    public static final int RESET_RECORDS = 4;
    public static final int CANCEL_RACE = 5;
    public static final int REFRESH = 6;
    public static final int START_RACE = 7;   // スタート地点からカウントダウン
    public static final int START_TA = 8;     // TA地点から助走

    public static final CustomPayload.Id<CourseEditPayload> ID = new CustomPayload.Id<>(SkyRiders.id("course_edit"));
    public static final PacketCodec<RegistryByteBuf, CourseEditPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, CourseEditPayload::action,
            PacketCodecs.STRING, CourseEditPayload::text,
            PacketCodecs.VAR_INT, CourseEditPayload::value,
            CourseEditPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
