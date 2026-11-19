package io.github.eeeeeeruki.ponderererer.element;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Tints the whole scene view: use it for a white flash (explosions, lightning) or for night / darkness.
 */
public class ScreenTintElement extends FxOverlay {
    private final int color;
    private final int holdTicks;

    public ScreenTintElement(int color, int fadeInTicks, int holdTicks) {
        super(null);
        this.color = color;
        this.holdTicks = holdTicks;
        this.alphaStep = fadeInTicks <= 0 ? 1f : 1f / fadeInTicks;
        if (fadeInTicks <= 0) this.alpha = this.prevAlpha = 1f;
    }

    @Override
    protected void update(PonderScene scene) {
        if (holdTicks >= 0 && age == holdTicks) fadeOut(Math.max(4, holdTicks / 2 + 4));
    }

    @Override
    protected void draw(PonderScene scene, PonderUI screen, GuiGraphics g, float pt, float a, int x, int y) {
        // Draw without writing depth: the tint darkens the scene but the Ponder buttons
        // and text drawn after it must stay on top.
        g.flush();
        RenderSystem.depthMask(false);
        g.fill(0, 0, screen.width, screen.height, withAlpha(color, a));
        g.flush();
        RenderSystem.depthMask(true);
    }
}
