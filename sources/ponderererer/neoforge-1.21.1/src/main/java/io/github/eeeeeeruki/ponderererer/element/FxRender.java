package io.github.eeeeeeruki.ponderererer.element;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Small drawing helpers shared by the beam and lightning elements. */
public final class FxRender {
    private FxRender() {}

    /**
     * Draws a square prism (4 side faces + caps) from a to b.
     * Expects a POSITION_COLOR quad buffer such as RenderType.lightning().
     */
    public static void prism(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float halfWidth,
                             float r, float g, float bl, float alpha) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0e-8) return;
        Vec3 d = dir.normalize();
        Vec3 ref = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = d.cross(ref).normalize().scale(halfWidth);
        Vec3 v = d.cross(u).normalize().scale(halfWidth);

        Vec3[] ca = {a.add(u).add(v), a.subtract(u).add(v), a.subtract(u).subtract(v), a.add(u).subtract(v)};
        Vec3[] cb = {b.add(u).add(v), b.subtract(u).add(v), b.subtract(u).subtract(v), b.add(u).subtract(v)};
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            quad(vc, m, ca[i], ca[j], cb[j], cb[i], r, g, bl, alpha);
            quad(vc, m, cb[i], cb[j], ca[j], ca[i], r, g, bl, alpha); // back face so it shows from all angles
        }
        quad(vc, m, ca[0], ca[1], ca[2], ca[3], r, g, bl, alpha);
        quad(vc, m, cb[3], cb[2], cb[1], cb[0], r, g, bl, alpha);
    }

    public static void quad(VertexConsumer vc, Matrix4f m, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                            float r, float g, float b, float a) {
        vertex(vc, m, p1, r, g, b, a);
        vertex(vc, m, p2, r, g, b, a);
        vertex(vc, m, p3, r, g, b, a);
        vertex(vc, m, p4, r, g, b, a);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
    }

    public static float red(int argb) { return ((argb >> 16) & 0xFF) / 255f; }
    public static float green(int argb) { return ((argb >> 8) & 0xFF) / 255f; }
    public static float blue(int argb) { return (argb & 0xFF) / 255f; }
    public static float alpha(int argb) {
        int a = (argb >>> 24) & 0xFF;
        return a == 0 ? 1f : a / 255f;
    }
}
