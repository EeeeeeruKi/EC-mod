package io.github.eeeeeeruki.ponderererer.element;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A small fake GUI window (slots, a progress arrow and bars) to show how a machine screen is used.
 * Use {@link io.github.eeeeeeruki.ponderererer.api.PanelBuilder} to lay it out.
 */
public class PanelElement extends FxOverlay {
    public static final class Slot {
        public final int x, y;
        public ItemStack stack;
        public final boolean output;
        public Slot(int x, int y, ItemStack stack, boolean output) {
            this.x = x; this.y = y; this.stack = stack; this.output = output;
        }
    }

    public static final class Bar {
        public final int x, y, w, h, color;
        public final Component label;
        float value, prev, from, target;
        int duration, elapsed;
        public Bar(int x, int y, int w, int h, int color, float value, Component label) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.color = color; this.label = label;
            this.value = this.prev = this.from = this.target = value;
        }
        void set(float v, int ticks) {
            from = value; target = v; duration = Math.max(0, ticks); elapsed = 0;
            if (duration == 0) value = prev = v;
        }
        void tick() {
            prev = value;
            if (elapsed < duration) { elapsed++; value = Mth.lerp((float) elapsed / duration, from, target); }
            else value = target;
        }
    }

    private final Component title;
    private final int width, height;
    private final List<Slot> slots = new ArrayList<>();
    private final List<Bar> bars = new ArrayList<>();
    private int arrowX = -1, arrowY;
    private final Bar arrow = new Bar(0, 0, 22, 15, 0xFFFFFFFF, 0, null);
    private int highlight = -1;
    private int clickFlash;

    public PanelElement(Vec3 anchor, Component title, int width, int height) {
        super(anchor);
        this.title = title;
        this.width = width;
        this.height = height;
        setOffset(16, -height / 2);
    }

    public int addSlot(int x, int y, ItemStack stack, boolean output) {
        slots.add(new Slot(x, y, stack, output));
        return slots.size() - 1;
    }

    public int addBar(int x, int y, int w, int h, int color, float value, Component label) {
        bars.add(new Bar(x, y, w, h, color, value, label));
        return bars.size() - 1;
    }

    public void setArrow(int x, int y) {
        this.arrowX = x;
        this.arrowY = y;
    }

    public void setSlot(int index, ItemStack stack) {
        if (index >= 0 && index < slots.size()) slots.get(index).stack = stack;
    }

    public void setBar(int index, float value, int ticks) {
        if (index >= 0 && index < bars.size()) bars.get(index).set(value, ticks);
    }

    public void setProgress(float value, int ticks) {
        arrow.set(value, ticks);
    }

    /** Draws a pulsing frame and a cursor on the slot; -1 clears it. */
    public void highlightSlot(int index, boolean click) {
        highlight = index;
        if (click) clickFlash = 6;
    }

    @Override
    protected void update(PonderScene scene) {
        for (Bar b : bars) b.tick();
        arrow.tick();
        if (clickFlash > 0) clickFlash--;
    }

    @Override
    protected void draw(PonderScene scene, PonderUI screen, GuiGraphics g, float pt, float a, int x, int y) {
        if (anchor == null) {
            x = screen.width / 2 - width / 2 + offsetX;
            y = screen.height / 2 - height / 2 + offsetY;
        }
        Font font = screen.getFontRenderer();
        // vanilla-like window
        g.fill(x, y, x + width, y + height, withAlpha(0xFFC6C6C6, a));
        g.fill(x, y, x + width - 1, y + 1, withAlpha(0xFFFFFFFF, a));
        g.fill(x, y, x + 1, y + height - 1, withAlpha(0xFFFFFFFF, a));
        g.fill(x + 1, y + height - 1, x + width, y + height, withAlpha(0xFF555555, a));
        g.fill(x + width - 1, y + 1, x + width, y + height, withAlpha(0xFF555555, a));
        if (title != null) g.drawString(font, title, x + 6, y + 5, withAlpha(0xFF404040, a), false);

        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            int sx = x + s.x, sy = y + s.y;
            int size = s.output ? 22 : 18;
            int off = s.output ? -2 : 0;
            g.fill(sx + off, sy + off, sx + off + size, sy + off + size, withAlpha(0xFF8B8B8B, a));
            g.fill(sx + off, sy + off, sx + off + size - 1, sy + off + 1, withAlpha(0xFF373737, a));
            g.fill(sx + off, sy + off, sx + off + 1, sy + off + size - 1, withAlpha(0xFF373737, a));
            g.fill(sx + off + 1, sy + off + size - 1, sx + off + size, sy + off + size, withAlpha(0xFFFFFFFF, a));
            g.fill(sx + off + size - 1, sy + off + 1, sx + off + size, sy + off + size, withAlpha(0xFFFFFFFF, a));
            if (s.stack != null && !s.stack.isEmpty() && a > 0.5f) {
                g.renderItem(s.stack, sx + 1, sy + 1);
                g.renderItemDecorations(font, s.stack, sx + 1, sy + 1);
            }
            if (i == highlight) {
                float pulse = 0.5f + 0.5f * Mth.sin((age + pt) * 0.4f);
                int c = withAlpha(0xFFFFFFFF, a * (0.35f + 0.4f * pulse));
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, c);
                drawCursor(g, sx + 12, sy + 12 + (clickFlash > 0 ? 1 : 0), a);
            }
        }

        if (arrowX >= 0) {
            int ax = x + arrowX, ay = y + arrowY;
            float p = Mth.clamp(Mth.lerp(pt, arrow.prev, arrow.value), 0, 1);
            drawArrow(g, ax, ay, withAlpha(0xFF8B8B8B, a), 1f);
            drawArrow(g, ax, ay, withAlpha(0xFFFFFFFF, a), p);
        }

        for (Bar b : bars) {
            int bx = x + b.x, by = y + b.y;
            float v = Mth.clamp(Mth.lerp(pt, b.prev, b.value), 0, 1);
            g.fill(bx - 1, by - 1, bx + b.w + 1, by + b.h + 1, withAlpha(0xFF373737, a));
            g.fill(bx, by, bx + b.w, by + b.h, withAlpha(0xFF1B1B1B, a));
            int fh = Math.round(b.h * v);
            g.fill(bx, by + b.h - fh, bx + b.w, by + b.h, withAlpha(b.color, a));
            if (b.label != null) g.drawString(font, b.label, bx - 1, by + b.h + 3, withAlpha(0xFF404040, a), false);
        }
    }

    private static void drawArrow(GuiGraphics g, int x, int y, int color, float progress) {
        int len = Math.round(22 * progress);
        if (len <= 0) return;
        int shaft = Math.min(len, 15);
        g.fill(x, y + 6, x + shaft, y + 10, color);
        for (int i = 15; i < len; i++) {
            int half = 22 - i;
            g.fill(x + i, y + 8 - half, x + i + 1, y + 8 + half, color);
        }
    }

    private static void drawCursor(GuiGraphics g, int x, int y, float a) {
        int black = withAlpha(0xFF000000, a), white = withAlpha(0xFFFFFFFF, a);
        for (int i = 0; i < 9; i++) {
            g.fill(x - 1, y + i - 1, x + i / 2 + 2, y + i, black);
            g.fill(x, y + i, x + i / 2 + 1, y + i + 1, white);
        }
    }
}
