package io.github.eeeeeeruki.ponderererer.element;

import net.createmod.ponder.api.element.PonderSceneElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.PonderElementBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * Base for Ponderererer scene elements: handles a simple fade in / fade out.
 * Subclasses draw in {@link #draw} with the pose stack already in scene space.
 */
public abstract class FxElement extends PonderElementBase implements PonderSceneElement {
    protected float alpha, prevAlpha;
    protected float alphaTarget = 1f;
    protected float alphaStep = 0.2f;
    protected int age;
    private boolean removeWhenHidden;

    protected FxElement() {
        setVisible(true);
    }

    /** Fades the element out over the given ticks, then hides it. */
    public void fadeOut(int ticks) {
        alphaTarget = 0f;
        alphaStep = ticks <= 0 ? 1f : 1f / ticks;
        removeWhenHidden = true;
    }

    public void fadeIn(int ticks) {
        alphaTarget = 1f;
        alphaStep = ticks <= 0 ? 1f : 1f / ticks;
        removeWhenHidden = false;
        setVisible(true);
    }

    @Override
    public void tick(PonderScene scene) {
        age++;
        prevAlpha = alpha;
        if (alpha < alphaTarget) alpha = Math.min(alphaTarget, alpha + alphaStep);
        else if (alpha > alphaTarget) alpha = Math.max(alphaTarget, alpha - alphaStep);
        if (removeWhenHidden && alpha <= 0f && prevAlpha <= 0f) setVisible(false);
        update(scene);
    }

    @Override
    public void whileSkipping(PonderScene scene) {
        tick(scene);
    }

    protected void update(PonderScene scene) {}

    protected float alpha(float pt) {
        return Mth.clamp(Mth.lerp(pt, prevAlpha, alpha), 0f, 1f);
    }

    @Override
    public void renderFirst(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt) {}

    @Override
    public void renderLayer(PonderLevel world, MultiBufferSource buffer, RenderType type, GuiGraphics graphics, float pt) {}

    @Override
    public void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt) {
        float a = alpha(pt);
        if (a <= 0.001f) return;
        draw(world, buffer, graphics, pt, a);
    }

    protected abstract void draw(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt, float alpha);
}
