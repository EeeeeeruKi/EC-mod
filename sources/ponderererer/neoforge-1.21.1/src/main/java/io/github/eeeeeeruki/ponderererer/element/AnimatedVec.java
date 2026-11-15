package io.github.eeeeeeruki.ponderererer.element;

import net.minecraft.world.phys.Vec3;

/** A position that moves smoothly to a target over a number of ticks. */
public final class AnimatedVec {
    private Vec3 from, to, current, prev;
    private int duration, elapsed;

    public AnimatedVec(Vec3 start) {
        from = to = current = prev = start;
    }

    public void moveTo(Vec3 target, int ticks) {
        from = current;
        to = target;
        duration = Math.max(0, ticks);
        elapsed = 0;
        if (duration == 0) current = prev = target;
    }

    public void tick() {
        prev = current;
        if (elapsed < duration) {
            elapsed++;
            float t = (float) elapsed / duration;
            t = t * t * (3 - 2 * t); // smoothstep
            current = from.lerp(to, t);
        } else {
            current = to;
        }
    }

    public Vec3 get(float pt) {
        return prev.lerp(current, pt);
    }

    public Vec3 target() {
        return to;
    }
}
