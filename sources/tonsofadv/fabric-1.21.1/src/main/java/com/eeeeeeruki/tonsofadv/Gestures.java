package com.eeeeeeruki.tonsofadv;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * ちょっとした動きを数えます：右や左を向いた角度、上・下・真上・真下を見た回数、
 * 後ろ・左・右に歩いた距離、しゃがんだ回数。毎ティック、前のティックとの向きと位置の差から出します。
 */
final class Gestures {
    private static final class State {
        boolean init, up, down, straightUp, straightDown, sneak;
        float yaw, pitch, turnRight, turnLeft;
        double x, z, back, left, right;
    }

    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();

    private Gestures() {
    }

    static void tick(ServerPlayer p) {
        State s = STATES.computeIfAbsent(p, k -> new State());
        float yaw = p.getYRot(), pitch = p.getXRot();
        double x = p.getX(), z = p.getZ();
        if (!s.init) {
            s.init = true;
            s.yaw = yaw; s.pitch = pitch; s.x = x; s.z = z; s.sneak = p.isShiftKeyDown();
            return;
        }
        // turning (yaw goes up when turning right); big jumps are teleports, not turns
        float dy = Mth.wrapDegrees(yaw - s.yaw);
        if (Math.abs(dy) < 120) {
            if (dy > 0) s.turnRight += dy; else s.turnLeft -= dy;
        }
        int r = (int) s.turnRight, l = (int) s.turnLeft;
        if (r > 0) { TonsOfAdv.add(p, "turned_right", r); s.turnRight -= r; }
        if (l > 0) { TonsOfAdv.add(p, "turned_left", l); s.turnLeft -= l; }
        // looking up / down (pitch is negative when looking up)
        if (pitch <= -60 && !s.up) { s.up = true; TonsOfAdv.add(p, "looked_up", 1); } else if (pitch > -30) s.up = false;
        if (pitch >= 60 && !s.down) { s.down = true; TonsOfAdv.add(p, "looked_down", 1); } else if (pitch < 30) s.down = false;
        if (pitch <= -89 && !s.straightUp) { s.straightUp = true; TonsOfAdv.add(p, "looked_straight_up", 1); } else if (pitch > -80) s.straightUp = false;
        if (pitch >= 89 && !s.straightDown) { s.straightDown = true; TonsOfAdv.add(p, "looked_straight_down", 1); } else if (pitch < 80) s.straightDown = false;
        // sneak key presses
        boolean sneak = p.isShiftKeyDown();
        if (sneak && !s.sneak) TonsOfAdv.add(p, "sneak_presses", 1);
        s.sneak = sneak;
        // stepping backward / sideways compared with where the player is looking
        double dx = x - s.x, dz = z - s.z, d = Math.sqrt(dx * dx + dz * dz);
        if (!p.isPassenger() && d > 0.02 && d < 1.0) {
            float moveYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float rel = Mth.wrapDegrees(moveYaw - yaw);
            if (Math.abs(rel) > 135) s.back += d * 100; else if (rel > 45) s.right += d * 100; else if (rel < -45) s.left += d * 100;
            int b = (int) s.back, lf = (int) s.left, rt = (int) s.right;
            if (b > 0) { TonsOfAdv.add(p, "moved_backward", b); s.back -= b; }
            if (lf > 0) { TonsOfAdv.add(p, "moved_left", lf); s.left -= lf; }
            if (rt > 0) { TonsOfAdv.add(p, "moved_right", rt); s.right -= rt; }
        }
        s.yaw = yaw; s.pitch = pitch; s.x = x; s.z = z;
    }
}
