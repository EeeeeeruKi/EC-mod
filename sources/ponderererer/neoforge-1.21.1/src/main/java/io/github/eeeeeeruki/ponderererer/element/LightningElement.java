package io.github.eeeeeeruki.ponderererer.element;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** A crackling electric arc between two points. The shape changes every few ticks. */
public class LightningElement extends FxElement {
    private final Vec3 from, to;
    private final int color;
    private final int segments;
    private final float jitter;
    private final int branches;
    private final RandomSource random = RandomSource.create();
    private final List<Vec3[]> lines = new ArrayList<>();
    private int rebuildEvery = 2;

    public LightningElement(Vec3 from, Vec3 to, int color, int segments, float jitter, int branches) {
        this.from = from;
        this.to = to;
        this.color = color;
        this.segments = Math.max(2, segments);
        this.jitter = jitter;
        this.branches = Math.max(0, branches);
        this.alpha = this.prevAlpha = 1f;
        rebuild();
    }

    public void setRebuildEvery(int ticks) { this.rebuildEvery = Math.max(1, ticks); }

    @Override
    protected void update(PonderScene scene) {
        if (age % rebuildEvery == 0) rebuild();
    }

    private void rebuild() {
        lines.clear();
        List<Vec3> main = path(from, to, segments, jitter);
        for (int i = 0; i < main.size() - 1; i++) lines.add(new Vec3[]{main.get(i), main.get(i + 1)});
        for (int k = 0; k < branches; k++) {
            int idx = 1 + random.nextInt(Math.max(1, main.size() - 2));
            Vec3 s = main.get(idx);
            Vec3 dir = to.subtract(from).scale(0.25 + random.nextDouble() * 0.2);
            Vec3 e = s.add(dir).add(rand(jitter * 2));
            List<Vec3> br = path(s, e, Math.max(2, segments / 3), jitter * 0.7f);
            for (int i = 0; i < br.size() - 1; i++) lines.add(new Vec3[]{br.get(i), br.get(i + 1)});
        }
    }

    private List<Vec3> path(Vec3 a, Vec3 b, int n, float jit) {
        List<Vec3> pts = new ArrayList<>();
        pts.add(a);
        for (int i = 1; i < n; i++) pts.add(a.lerp(b, (double) i / n).add(rand(jit)));
        pts.add(b);
        return pts;
    }

    private Vec3 rand(float s) {
        return new Vec3((random.nextDouble() - 0.5) * 2 * s, (random.nextDouble() - 0.5) * 2 * s, (random.nextDouble() - 0.5) * 2 * s);
    }

    @Override
    protected void draw(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt, float a) {
        Matrix4f m = graphics.pose().last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        float r = FxRender.red(color), g = FxRender.green(color), b = FxRender.blue(color);
        float baseA = FxRender.alpha(color) * a;
        for (Vec3[] l : lines) {
            FxRender.prism(vc, m, l[0], l[1], 0.06f, r, g, b, 0.35f * baseA);
            FxRender.prism(vc, m, l[0], l[1], 0.02f, 1f, 1f, 1f, 0.9f * baseA);
        }
    }
}
