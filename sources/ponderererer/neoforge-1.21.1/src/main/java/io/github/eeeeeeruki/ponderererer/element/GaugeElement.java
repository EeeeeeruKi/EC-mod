package io.github.eeeeeeruki.ponderererer.element;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A Mekanism-style gauge (energy, fluid, chemical...) floating next to a point in the scene.
 * The value animates smoothly with {@link #setValue(float, int)}.
 */
public class GaugeElement extends FxOverlay {
    private final Component label;
    private final int color;
    private final float max;
    private final String unit;
    private final boolean horizontal;
    private float value, prevValue, from, target;
    private int duration, elapsed;

    public GaugeElement(Vec3 anchor, Component label, int color, float value, float max, String unit, boolean horizontal) {
        super(anchor);
        this.label = label;
        this.color = color;
        this.max = Math.max(0.0001f, max);
        this.unit = unit == null ? "" : unit;
        this.horizontal = horizontal;
        this.value = this.prevValue = this.from = this.target = value;
        setOffset(14, -24);
    }

    public void setValue(float newValue, int ticks) {
        from = value;
        target = newValue;
        duration = Math.max(0, ticks);
        elapsed = 0;
        if (duration == 0) value = prevValue = newValue;
    }

    @Override
    protected void update(PonderScene scene) {
        prevValue = value;
        if (elapsed < duration) {
            elapsed++;
            value = Mth.lerp((float) elapsed / duration, from, target);
        } else {
            value = target;
        }
    }

    @Override
    protected void draw(PonderScene scene, PonderUI screen, GuiGraphics g, float pt, float a, int x, int y) {
        Font font = screen.getFontRenderer();
        float v = Mth.lerp(pt, prevValue, value);
        float frac = Mth.clamp(v / max, 0f, 1f);
        int w = horizontal ? 52 : 10, h = horizontal ? 10 : 44;

        int border = withAlpha(0xFF373737, a), bg = withAlpha(0xFF1B1B1B, a), hi = withAlpha(0xFF8B8B8B, a);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fill(x, y, x + w, y + h, bg);
        int c = withAlpha(color, a);
        if (horizontal) {
            int fw = Math.round(w * frac);
            g.fill(x, y, x + fw, y + h, c);
        } else {
            int fh = Math.round(h * frac);
            g.fill(x, y + h - fh, x + w, y + h, c);
            for (int i = 1; i < 4; i++) g.fill(x, y + h * i / 4, x + 3, y + h * i / 4 + 1, hi); // scale marks
        }

        int tx = horizontal ? x : x + w + 4;
        int ty = horizontal ? y - 11 : y + h / 2 - 9;
        int textColor = withAlpha(0xFFFFFFFF, a);
        if (label != null) g.drawString(font, label, tx, ty, textColor, true);
        String amount = format(v) + (max > 0 ? " / " + format(max) : "") + (unit.isEmpty() ? "" : " " + unit);
        g.drawString(font, amount, tx, horizontal ? y + h + 3 : ty + 10, withAlpha(0xFFBDBDBD, a), true);
    }

    private static String format(float f) {
        if (f >= 1_000_000) return String.format("%.1fM", f / 1_000_000f);
        if (f >= 10_000) return String.format("%.1fk", f / 1000f);
        return String.valueOf(Math.round(f));
    }
}
