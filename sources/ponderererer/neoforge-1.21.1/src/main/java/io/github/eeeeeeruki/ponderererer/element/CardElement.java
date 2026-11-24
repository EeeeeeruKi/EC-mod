package io.github.eeeeeeruki.ponderererer.element;

import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * An information card in the style of a machine screen: a title with an icon, then rows of slots, gauges,
 * progress arrows, crafting grids and text. Cards stack in a column on the left side of the Ponder screen
 * (the text boxes are on the right), shrink when there is little room, and can draw a dotted line to the block
 * they describe. Build one with {@link io.github.eeeeeeruki.ponderererer.api.CardBuilder}.
 */
public class CardElement extends FxOverlay {
    static final int PAD = 5, ROW_GAP = 4, TOP = 44, LEFT = 6, SPACING = 6, BAR_W = 9, BAR_H = 38, TEXT_W = 180;

    public static final class Slot {
        ItemStack stack;
        final boolean big;
        int flash;

        Slot(ItemStack stack, boolean big) {
            this.stack = stack;
            this.big = big;
        }

        int size() {
            return big ? 26 : 18;
        }
    }

    public static final class Bar {
        final int color;
        final Component label;
        float value, prev, from, target;
        int duration, elapsed;

        Bar(int color, float value, Component label) {
            this.color = color;
            this.label = label;
            this.value = this.prev = this.from = this.target = value;
        }

        void set(float v, int ticks) {
            from = value;
            target = v;
            duration = Math.max(0, ticks);
            elapsed = 0;
            if (duration == 0) value = prev = v;
        }

        void tick() {
            prev = value;
            if (elapsed < duration) {
                elapsed++;
                value = Mth.lerp((float) elapsed / duration, from, target);
            } else {
                value = target;
            }
        }

        float get(float pt) {
            return Mth.clamp(Mth.lerp(pt, prev, value), 0f, 1f);
        }
    }

    /** One horizontal band of the card. */
    public interface Row {
        int width(CardElement card, Font font);

        int height(CardElement card, Font font);

        void draw(CardElement card, GuiGraphics g, Font font, int x, int y, float pt, float a);
    }

    final Component title;
    final ItemStack icon;
    final Vec3 target;
    final List<Slot> slots = new ArrayList<>();
    final List<Bar> bars = new ArrayList<>();
    final List<Row> rows = new ArrayList<>();
    final List<Component> texts = new ArrayList<>();
    final Bar progress = new Bar(0xFF3CFE9A, 0f, null);
    private int lastW, lastH;

    public CardElement(Component title, ItemStack icon, Vec3 target) {
        super(null);
        this.title = title;
        this.icon = icon == null ? ItemStack.EMPTY : icon;
        this.target = target;
    }

    // ------------------------------------------------------------------ building

    public int addSlot(ItemStack stack, boolean big) {
        slots.add(new Slot(stack, big));
        return slots.size() - 1;
    }

    public int addBar(int color, float value, Component label) {
        bars.add(new Bar(color, value, label));
        return bars.size() - 1;
    }

    public int addText(Component text) {
        texts.add(text);
        return texts.size() - 1;
    }

    public void addRow(Row row) {
        rows.add(row);
    }

    // ------------------------------------------------------------------ changing

    public void setSlot(int index, ItemStack stack) {
        if (index < 0 || index >= slots.size()) return;
        Slot s = slots.get(index);
        s.stack = stack;
        s.flash = 10;
    }

    public void setBar(int index, float value, int ticks) {
        if (index >= 0 && index < bars.size()) bars.get(index).set(value, ticks);
    }

    public void setProgress(float value, int ticks) {
        progress.set(value, ticks);
    }

    public void setText(int index, Component text) {
        if (index >= 0 && index < texts.size()) texts.set(index, text);
    }

    @Override
    protected void update(PonderScene scene) {
        for (Bar b : bars) b.tick();
        progress.tick();
        for (Slot s : slots) if (s.flash > 0) s.flash--;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void draw(PonderScene scene, PonderUI screen, GuiGraphics graphics, float pt, float a, int x, int y) {
        // not used: render() places the card itself
    }

    @Override
    public void render(PonderScene scene, PonderUI screen, GuiGraphics g, float pt) {
        float a = Mth.clamp(Mth.lerp(pt, prevAlpha, alpha), 0f, 1f);
        Font font = screen.getFontRenderer();
        measure(font);
        if (a <= 0.01f) return;

        // place the card in the column of cards on the left
        List<CardElement> cards = new ArrayList<>();
        scene.forEach(CardElement.class, c -> {
            if (c.isVisible() && c.lastH > 0) cards.add(c);
        });
        int total = 0, widest = 0;
        for (CardElement c : cards) {
            total += c.lastH + SPACING;
            widest = Math.max(widest, c.lastW);
        }
        float avail = screen.height - TOP - 40;
        float scale = Math.min(1f, Math.min(avail / Math.max(1, total), screen.width * 0.34f / Math.max(1, widest)));
        scale = Math.max(0.45f, scale);
        float top = TOP;
        for (CardElement c : cards) {
            if (c == this) break;
            top += (c.lastH + SPACING) * scale;
        }

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        if (target != null) {
            Vec2 to = scene.getTransform().sceneToScreen(target, pt);
            int fx = Math.round(LEFT + lastW * scale), fy = Math.round(top + 11 * scale);
            dottedLine(g, fx, fy, Math.round(to.x), Math.round(to.y), withAlpha(0xFFFFFFFF, a * 0.85f));
            g.fill(Math.round(to.x) - 2, Math.round(to.y) - 2, Math.round(to.x) + 2, Math.round(to.y) + 2, withAlpha(0xFFFFFFFF, a));
            g.fill(Math.round(to.x) - 1, Math.round(to.y) - 1, Math.round(to.x) + 1, Math.round(to.y) + 1, withAlpha(0xFF3CFE9A, a));
        }
        g.pose().translate(LEFT, top, 0);
        g.pose().scale(scale, scale, 1f);
        drawBody(g, font, pt, a);
        g.pose().popPose();
    }

    private void measure(Font font) {
        int w = PAD * 2 + (icon.isEmpty() ? 0 : 19) + font.width(title);
        int h = PAD + 16;
        for (Row r : rows) {
            w = Math.max(w, r.width(this, font) + PAD * 2);
            h += ROW_GAP + r.height(this, font);
        }
        lastW = Math.max(w, 96);
        lastH = h + PAD;
    }

    private void drawBody(GuiGraphics g, Font font, float pt, float a) {
        int w = lastW, h = lastH;
        g.fill(0, 0, w, h, withAlpha(0xFFC6C6C6, a));
        g.fill(0, 0, w - 1, 1, withAlpha(0xFFFFFFFF, a));
        g.fill(0, 0, 1, h - 1, withAlpha(0xFFFFFFFF, a));
        g.fill(1, h - 1, w, h, withAlpha(0xFF555555, a));
        g.fill(w - 1, 1, w, h, withAlpha(0xFF555555, a));
        int tx = PAD;
        if (!icon.isEmpty()) {
            if (a > 0.5f) g.renderItem(icon, PAD, PAD - 1);
            tx += 19;
        }
        g.drawString(font, title, tx, PAD + 4, withAlpha(0xFF404040, a), false);
        g.fill(PAD, PAD + 16, w - PAD, PAD + 17, withAlpha(0xFF8B8B8B, a));
        int y = PAD + 16;
        for (Row r : rows) {
            y += ROW_GAP;
            r.draw(this, g, font, PAD, y, pt, a);
            y += r.height(this, font);
        }
    }

    // ------------------------------------------------------------------ pieces

    void drawSlot(GuiGraphics g, Font font, int index, int x, int y, float a) {
        if (index < 0 || index >= slots.size()) return;
        Slot s = slots.get(index);
        int size = s.size();
        g.fill(x, y, x + size, y + size, withAlpha(0xFF8B8B8B, a));
        g.fill(x, y, x + size - 1, y + 1, withAlpha(0xFF373737, a));
        g.fill(x, y, x + 1, y + size - 1, withAlpha(0xFF373737, a));
        g.fill(x + 1, y + size - 1, x + size, y + size, withAlpha(0xFFFFFFFF, a));
        g.fill(x + size - 1, y + 1, x + size, y + size, withAlpha(0xFFFFFFFF, a));
        int ix = x + (size - 16) / 2, iy = y + (size - 16) / 2;
        if (s.stack != null && !s.stack.isEmpty() && a > 0.5f) {
            g.renderItem(s.stack, ix, iy);
            g.renderItemDecorations(font, s.stack, ix, iy);
        }
        if (s.flash > 0) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1, withAlpha(0xFFFFFFFF, a * s.flash / 14f));
            g.pose().popPose();
        }
    }

    void drawBar(GuiGraphics g, int index, int x, int y, int w, int h, float pt, float a, boolean horizontal) {
        if (index < 0 || index >= bars.size()) return;
        Bar b = bars.get(index);
        float v = b.get(pt);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, withAlpha(0xFF373737, a));
        g.fill(x, y, x + w, y + h, withAlpha(0xFF1B1B1B, a));
        int c = withAlpha(b.color, a);
        if (horizontal) {
            g.fill(x, y, x + Math.round(w * v), y + h, c);
        } else {
            int fh = Math.round(h * v);
            g.fill(x, y + h - fh, x + w, y + h, c);
            for (int i = 1; i < 4; i++) g.fill(x, y + h * i / 4, x + 3, y + h * i / 4 + 1, withAlpha(0xFF8B8B8B, a));
        }
    }

    void drawArrow(GuiGraphics g, int x, int y, float pt, float a) {
        float p = progress.get(pt);
        arrowShape(g, x, y, withAlpha(0xFF8B8B8B, a), 1f);
        arrowShape(g, x, y, withAlpha(0xFF3CFE9A, a), p);
    }

    private static void arrowShape(GuiGraphics g, int x, int y, int color, float progress) {
        int len = Math.round(24 * progress);
        if (len <= 0) return;
        int shaft = Math.min(len, 16);
        g.fill(x, y + 6, x + shaft, y + 10, color);
        for (int i = 16; i < len; i++) {
            int half = 24 - i;
            g.fill(x + i, y + 8 - half, x + i + 1, y + 8 + half, color);
        }
    }

    private static void dottedLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = x1 - x0, dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i = 0; i <= steps; i += 3) {
            int px = x0 + dx * i / Math.max(1, steps), py = y0 + dy * i / Math.max(1, steps);
            g.fill(px, py, px + 2, py + 2, color);
        }
    }

    // ------------------------------------------------------------------ rows

    /** [gauges] [input slots] -> [output slots] [gauges], everything centred on one line. */
    public static final class ProcessRow implements Row {
        final int[] left, in, out, right;
        final boolean arrow;

        public ProcessRow(int[] left, int[] in, boolean arrow, int[] out, int[] right) {
            this.left = left;
            this.in = in;
            this.arrow = arrow;
            this.out = out;
            this.right = right;
        }

        @Override
        public int width(CardElement c, Font f) {
            int w = (left.length + right.length) * (BAR_W + 4);
            for (int i : in) w += c.slots.get(i).size() + 3;
            for (int i : out) w += c.slots.get(i).size() + 3;
            if (arrow) w += 32;
            if (left.length > 0) w += 4;
            if (right.length > 0) w += 4;
            return w;
        }

        @Override
        public int height(CardElement c, Font f) {
            return left.length + right.length > 0 ? BAR_H : 26;
        }

        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, int x, int y, float pt, float a) {
            int h = height(c, f);
            for (int b : left) {
                c.drawBar(g, b, x, y, BAR_W, h, pt, a, false);
                x += BAR_W + 4;
            }
            if (left.length > 0) x += 4;
            for (int i : in) {
                int s = c.slots.get(i).size();
                c.drawSlot(g, f, i, x, y + (h - s) / 2, a);
                x += s + 3;
            }
            if (arrow) {
                c.drawArrow(g, x + 4, y + h / 2 - 8, pt, a);
                x += 32;
            }
            for (int i : out) {
                int s = c.slots.get(i).size();
                c.drawSlot(g, f, i, x, y + (h - s) / 2, a);
                x += s + 3;
            }
            if (right.length > 0) x += 4;
            for (int b : right) {
                c.drawBar(g, b, x, y, BAR_W, h, pt, a, false);
                x += BAR_W + 4;
            }
        }
    }

    /** A 3 x 3 crafting grid, an arrow and the result. */
    public static final class GridRow implements Row {
        final int[] cells;
        final int out;

        public GridRow(int[] cells, int out) {
            this.cells = cells;
            this.out = out;
        }

        @Override
        public int width(CardElement c, Font f) {
            return 54 + 34 + 26;
        }

        @Override
        public int height(CardElement c, Font f) {
            return 54;
        }

        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, int x, int y, float pt, float a) {
            for (int i = 0; i < 9; i++) {
                int sx = x + (i % 3) * 18, sy = y + (i / 3) * 18;
                if (i < cells.length && cells[i] >= 0) {
                    c.drawSlot(g, f, cells[i], sx, sy, a);
                } else {
                    g.fill(sx, sy, sx + 18, sy + 18, withAlpha(0xFF8B8B8B, a));
                    g.fill(sx, sy, sx + 17, sy + 1, withAlpha(0xFF373737, a));
                    g.fill(sx, sy, sx + 1, sy + 17, withAlpha(0xFF373737, a));
                    g.fill(sx + 1, sy + 17, sx + 18, sy + 18, withAlpha(0xFFFFFFFF, a));
                    g.fill(sx + 17, sy + 1, sx + 18, sy + 18, withAlpha(0xFFFFFFFF, a));
                }
            }
            c.drawArrow(g, x + 58, y + 19, pt, a);
            c.drawSlot(g, f, out, x + 88, y + 14, a);
        }
    }

    /** A line of text (wrapped), optionally with an item icon in front. */
    public static final class TextRow implements Row {
        final int text;
        final ItemStack icon;
        final int color;

        public TextRow(int text, ItemStack icon, int color) {
            this.text = text;
            this.icon = icon == null ? ItemStack.EMPTY : icon;
            this.color = color;
        }

        private int indent() {
            return icon.isEmpty() ? 0 : 19;
        }

        @Override
        public int width(CardElement c, Font f) {
            return indent() + Math.min(TEXT_W, f.width(c.texts.get(text)));
        }

        @Override
        public int height(CardElement c, Font f) {
            int lines = f.split(c.texts.get(text), TEXT_W).size();
            return Math.max(icon.isEmpty() ? 0 : 16, lines * 10);
        }

        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, int x, int y, float pt, float a) {
            if (!icon.isEmpty() && a > 0.5f) g.renderItem(icon, x, y);
            List<FormattedCharSequence> lines = f.split(c.texts.get(text), TEXT_W);
            int ty = y + (icon.isEmpty() ? 0 : Math.max(0, (16 - lines.size() * 10) / 2 + 1));
            for (FormattedCharSequence line : lines) {
                g.drawString(f, line, x + indent(), ty, withAlpha(color, a), false);
                ty += 10;
            }
        }
    }

    /** Colour keys for the labelled gauges: what each coloured bar holds. */
    public static final class LegendRow implements Row {
        private List<int[]> layout(CardElement c, Font f) {
            List<int[]> out = new ArrayList<>(); // {bar, x, line}
            int x = 0, line = 0;
            for (int i = 0; i < c.bars.size(); i++) {
                Component label = c.bars.get(i).label;
                if (label == null) continue;
                int w = 10 + f.width(label) + 8;
                if (x > 0 && x + w > TEXT_W) {
                    x = 0;
                    line++;
                }
                out.add(new int[]{i, x, line});
                x += w;
            }
            return out;
        }

        @Override
        public int width(CardElement c, Font f) {
            int w = 0;
            for (int[] e : layout(c, f)) w = Math.max(w, e[1] + 10 + f.width(c.bars.get(e[0]).label));
            return w;
        }

        @Override
        public int height(CardElement c, Font f) {
            List<int[]> l = layout(c, f);
            return l.isEmpty() ? 0 : (l.get(l.size() - 1)[2] + 1) * 11;
        }

        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, int x, int y, float pt, float a) {
            for (int[] e : layout(c, f)) {
                Bar b = c.bars.get(e[0]);
                int bx = x + e[1], by = y + e[2] * 11;
                g.fill(bx, by + 1, bx + 7, by + 8, withAlpha(0xFF373737, a));
                g.fill(bx + 1, by + 2, bx + 6, by + 7, withAlpha(b.color, a));
                g.drawString(f, b.label, bx + 10, by, withAlpha(0xFF404040, a), false);
            }
        }
    }

    /** A row of slots (wraps after eight), e.g. a filter list. */
    public static final class IconsRow implements Row {
        final int[] items;

        public IconsRow(int[] items) {
            this.items = items;
        }

        @Override
        public int width(CardElement c, Font f) {
            return Math.min(8, items.length) * 19;
        }

        @Override
        public int height(CardElement c, Font f) {
            return ((items.length + 7) / 8) * 19;
        }

        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, int x, int y, float pt, float a) {
            for (int i = 0; i < items.length; i++) c.drawSlot(g, f, items[i], x + (i % 8) * 19, y + (i / 8) * 19, a);
        }
    }

    /** A label and a horizontal gauge on one line, for comparing amounts. */
    public static final class HBarRow implements Row {
        final int bar;
        final int text;

        public HBarRow(int bar, int text) {
            this.bar = bar;
            this.text = text;
        }

        @Override
        public int width(CardElement c, Font f) {
            return Math.max(70, f.width(c.texts.get(text)) + 6) + 90;
        }

        @Override
        public int height(CardElement c, Font f) {
            return 10;
        }

        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, int x, int y, float pt, float a) {
            int lw = Math.max(70, f.width(c.texts.get(text)) + 6);
            g.drawString(f, c.texts.get(text), x, y + 1, withAlpha(0xFF404040, a), false);
            c.drawBar(g, bar, x + lw, y + 1, 86, 7, pt, a, true);
        }
    }
}
