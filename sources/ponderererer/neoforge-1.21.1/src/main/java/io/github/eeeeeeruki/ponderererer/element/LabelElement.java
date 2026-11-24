package io.github.eeeeeeruki.ponderererer.element;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** A small name tag floating above a point in the scene ("input", "red", "higher"...), with a pointer down to it. */
public class LabelElement extends FxOverlay {
    private final Component text;
    private final int color;

    public LabelElement(Vec3 anchor, Component text, int color) {
        super(anchor);
        this.text = text;
        this.color = color;
        setOffset(0, 0);
    }

    @Override
    protected void draw(PonderScene scene, PonderUI screen, GuiGraphics g, float pt, float a, int x, int y) {
        Font font = screen.getFontRenderer();
        int w = font.width(text) + 8, h = 13;
        int x0 = x - w / 2, y0 = y - h - 7;
        int border = withAlpha(color, a), bg = withAlpha(0xE0101018, a);
        g.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, border);
        g.fill(x0, y0, x0 + w, y0 + h, bg);
        for (int i = 0; i < 5; i++) g.fill(x - 4 + i, y0 + h + 1 + i, x + 5 - i, y0 + h + 2 + i, border); // pointer
        g.drawString(font, text, x0 + 4, y0 + 3, withAlpha(0xFFFFFFFF, a), false);
    }
}
