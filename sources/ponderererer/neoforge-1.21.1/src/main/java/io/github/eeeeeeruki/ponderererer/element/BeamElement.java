package io.github.eeeeeeruki.ponderererer.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** A glowing straight beam (laser) between two points. Both ends can be animated. */
public class BeamElement extends FxElement {
    private final AnimatedVec start, end;
    private int color;
    private float width;
    private boolean pulse = true;
    private float grow = 1f, prevGrow = 1f;
    private int growTicks;

    public BeamElement(Vec3 from, Vec3 to, int color, float width) {
        this.start = new AnimatedVec(from);
        this.end = new AnimatedVec(to);
        this.color = color;
        this.width = width;
        this.alpha = this.prevAlpha = 0f;
    }

    /** Makes the beam shoot out from the start point over the given ticks. */
    public BeamElement growIn(int ticks) {
        growTicks = Math.max(1, ticks);
        grow = prevGrow = 0f;
        return this;
    }

    public void setPulse(boolean pulse) { this.pulse = pulse; }
    public void setColor(int color) { this.color = color; }
    public void setWidth(float width) { this.width = width; }
    public void moveStart(Vec3 to, int ticks) { start.moveTo(to, ticks); }
    public void moveEnd(Vec3 to, int ticks) { end.moveTo(to, ticks); }

    @Override
    protected void update(net.createmod.ponder.foundation.PonderScene scene) {
        start.tick();
        end.tick();
        prevGrow = grow;
        if (grow < 1f) grow = Math.min(1f, grow + 1f / growTicks);
    }

    @Override
    protected void draw(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt, float a) {
        Vec3 from = start.get(pt);
        Vec3 to = from.lerp(end.get(pt), Mth.lerp(pt, prevGrow, grow));
        float time = age + pt;
        float w = width * (pulse ? 1f + 0.12f * Mth.sin(time * 0.6f) : 1f);
        float r = FxRender.red(color), g = FxRender.green(color), b = FxRender.blue(color);
        float baseA = FxRender.alpha(color) * a;

        PoseStack ps = graphics.pose();
        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        // outer glow, then a bright core
        FxRender.prism(vc, m, from, to, w * 1.8f, r, g, b, 0.18f * baseA);
        FxRender.prism(vc, m, from, to, w * 1.0f, r, g, b, 0.45f * baseA);
        FxRender.prism(vc, m, from, to, w * 0.4f, Math.min(1, r + 0.6f), Math.min(1, g + 0.6f), Math.min(1, b + 0.6f), 0.9f * baseA);
    }
}
