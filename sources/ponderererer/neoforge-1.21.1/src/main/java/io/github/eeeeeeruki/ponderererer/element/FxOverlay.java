package io.github.eeeeeeruki.ponderererer.element;

import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.PonderElementBase;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Base for 2D overlay elements drawn on the Ponder screen, optionally anchored to a point in the scene.
 */
public abstract class FxOverlay extends PonderElementBase implements PonderOverlayElement {
    protected float alpha, prevAlpha, alphaTarget = 1f, alphaStep = 0.25f;
    protected int age;
    protected Vec3 anchor;
    protected int offsetX, offsetY;
    private boolean removeWhenHidden;

    protected FxOverlay(Vec3 anchor) {
        this.anchor = anchor;
        setVisible(true);
    }

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    public void fadeOut(int ticks) {
        alphaTarget = 0f;
        alphaStep = ticks <= 0 ? 1f : 1f / ticks;
        removeWhenHidden = true;
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

    @Override
    public void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float pt) {
        float a = Mth.clamp(Mth.lerp(pt, prevAlpha, alpha), 0f, 1f);
        if (a <= 0.01f) return;
        int x, y;
        if (anchor != null) {
            Vec2 p = scene.getTransform().sceneToScreen(anchor, pt);
            x = (int) p.x + offsetX;
            y = (int) p.y + offsetY;
        } else {
            x = offsetX;
            y = offsetY;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        draw(scene, screen, graphics, pt, a, x, y);
        graphics.pose().popPose();
    }

    protected abstract void draw(PonderScene scene, PonderUI screen, GuiGraphics graphics, float pt, float alpha, int x, int y);

    protected static int withAlpha(int rgb, float a) {
        int base = (rgb >>> 24) & 0xFF;
        if (base == 0) base = 0xFF;
        int out = Mth.clamp((int) (base * a), 0, 255);
        return (out << 24) | (rgb & 0xFFFFFF);
    }
}
