package com.eeeeeeruki.skyriders.entity;

import net.minecraft.text.Text;

/**
 * パッチ9種。表示の段階は HP以外 -14〜+18、HPは -14〜+16。
 * 内部値は HP以外「-2 始まり」（表示0 = 内部-2）なので、内部はどれも -16〜+16 になる。
 */
public enum PatchKind {
    TOP_SPEED("top_speed", 0xFF4040, 18),
    ACCEL("accel", 0xB050FF, 18),      // 紫の矢印パネル
    CHARGE("charge", 0xFFE040, 18),
    TURN("turn", 0x40D040, 18),
    FLY("fly", 0x40C0FF, 18),
    ATTACK("attack", 0xFF40A0, 18),
    DEFENSE("defense", 0x4060FF, 18),
    WEIGHT("weight", 0x9A9AAA, 18),
    HP("hp", 0x60FF90, 16);

    public static final int MIN = -14;
    public static final PatchKind[] ALL = values();

    public final String id;
    public final int color;
    public final int max;

    PatchKind(String id, int color, int max) {
        this.id = id;
        this.color = color;
        this.max = max;
    }

    /** 表示の段階 → 内部値 */
    public int internal(int display) {
        return this == HP ? display : display - 2;
    }

    public Text displayName() {
        return Text.translatable("patch.skyriders." + this.id);
    }
}
