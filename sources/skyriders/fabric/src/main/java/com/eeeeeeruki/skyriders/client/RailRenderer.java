package com.eeeeeeruki.skyriders.client;

import com.eeeeeeruki.skyriders.SkyRiders;
import com.eeeeeeruki.skyriders.block.RailBlockEntity;
import com.eeeeeeruki.skyriders.block.RailGeometry;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** レールを、となりとつながったなめらかな細いパイプとして描く */
public class RailRenderer implements BlockEntityRenderer<RailBlockEntity> {
    private static final Identifier TEXTURE = SkyRiders.id("textures/entity/rail.png");
    private static final float RADIUS = 0.0625f; // 太さ 2/16
    private static final int SEGMENTS = 10;

    public RailRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(RailBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {
        if (entity.getWorld() == null) return;
        BlockPos pos = entity.getPos();
        RailGeometry.Curve c = RailGeometry.curve(entity.getWorld(), pos);
        if (c == null) return; // 壊した直後
        Vec3d origin = Vec3d.of(pos);
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f m = entry.getPositionMatrix();

        Vec3d[] ring0 = null;
        Vec3d prevP = null;
        float v0 = 0;
        for (int i = 0; i <= SEGMENTS; i++) {
            double t = i / (double) SEGMENTS;
            Vec3d p = c.point(t).subtract(origin);
            Vec3d tan = c.tangent(t).normalize();
            Vec3d up = Math.abs(tan.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
            Vec3d side = tan.crossProduct(up).normalize().multiply(RADIUS);
            Vec3d top = side.crossProduct(tan).normalize().multiply(RADIUS);
            Vec3d[] ring = {p.add(top).add(side), p.add(top).subtract(side), p.subtract(top).subtract(side), p.subtract(top).add(side)};
            float v1 = prevP == null ? 0 : v0 + (float) p.distanceTo(prevP) * 2f;
            if (ring0 != null) {
                for (int k = 0; k < 4; k++) {
                    Vec3d a0 = ring0[k], b0 = ring0[(k + 1) % 4], a1 = ring[k], b1 = ring[(k + 1) % 4];
                    Vec3d n = a0.add(b0).multiply(0.5).subtract(prevP).normalize();
                    float u0 = k * 0.25f, u1 = u0 + 0.25f;
                    quad(vc, m, entry, a0, b0, b1, a1, u0, u1, v0, v1, n, light, overlay);
                }
            }
            ring0 = ring;
            prevP = p;
            v0 = v1;
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, MatrixStack.Entry entry, Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                             float u0, float u1, float v0, float v1, Vec3d n, int light, int overlay) {
        vertex(vc, m, entry, a, u0, v0, n, light);
        vertex(vc, m, entry, b, u1, v0, n, light);
        vertex(vc, m, entry, c, u1, v1, n, light);
        vertex(vc, m, entry, d, u0, v1, n, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, MatrixStack.Entry entry, Vec3d p, float u, float v, Vec3d n, int light) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, (float) n.x, (float) n.y, (float) n.z);
    }

    @Override
    public boolean rendersOutsideBoundingBox(RailBlockEntity blockEntity) {
        return true;
    }
}
