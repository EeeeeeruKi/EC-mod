package io.github.eeeeeeruki.ponderererer.api;

import io.github.eeeeeeruki.ponderererer.element.CardElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Lays out an information card (see {@link CardElement}). Slots, gauges and texts get indexes in the order they
 * are added; use them with the rows and with FX#cardSlot / cardBar / cardText to change the card later.
 * <pre>
 * CardBuilder card = CardBuilder.of(machineName).icon(machine).target(topOfMachine);
 * int energy = card.bar(0xFF3CFE9A, 1f, Component.literal("Energy"));
 * int in = card.slot(ItemStack.EMPTY), out = card.bigSlot(ItemStack.EMPTY);
 * card.process(new int[]{energy}, new int[]{in}, true, new int[]{out}, new int[0]).legend();
 * </pre>
 */
public class CardBuilder {
    private final Component title;
    private ItemStack icon = ItemStack.EMPTY;
    private Vec3 target;
    private final List<Consumer<CardElement>> steps = new ArrayList<>();
    private int slots, bars, texts;

    private CardBuilder(Component title) {
        this.title = title;
    }

    public static CardBuilder of(Component title) {
        return new CardBuilder(title);
    }

    public CardBuilder icon(ItemStack icon) {
        this.icon = icon.copy();
        return this;
    }

    /** Draws a dotted line from the card to this point in the scene. */
    public CardBuilder target(Vec3 target) {
        this.target = target;
        return this;
    }

    public int slot(ItemStack stack) {
        ItemStack copy = stack.copy();
        steps.add(c -> c.addSlot(copy.copy(), false));
        return slots++;
    }

    /** A large slot, like a machine's output slot. */
    public int bigSlot(ItemStack stack) {
        ItemStack copy = stack.copy();
        steps.add(c -> c.addSlot(copy.copy(), true));
        return slots++;
    }

    /** A gauge. color is ARGB, value 0..1; the label shows in the legend row. */
    public int bar(int color, float value, Component label) {
        steps.add(c -> c.addBar(color, value, label));
        return bars++;
    }

    public CardBuilder process(int[] leftBars, int[] in, boolean arrow, int[] out, int[] rightBars) {
        steps.add(c -> c.addRow(new CardElement.ProcessRow(leftBars, in, arrow, out, rightBars)));
        return this;
    }

    /** A crafting grid: nine slot indexes (-1 for an empty cell) and the result slot. */
    public CardBuilder grid(int[] cells, int out) {
        steps.add(c -> c.addRow(new CardElement.GridRow(cells, out)));
        return this;
    }

    /** A text line; returns its index for FX#cardText. color is ARGB (0xFF404040 is the normal dark grey). */
    public int text(Component text, ItemStack icon, int color) {
        ItemStack copy = icon == null ? ItemStack.EMPTY : icon.copy();
        steps.add(c -> {
            int i = c.addText(text);
            c.addRow(new CardElement.TextRow(i, copy, color));
        });
        return texts++;
    }

    public int text(Component text) {
        return text(text, ItemStack.EMPTY, 0xFF404040);
    }

    /** Colour keys for all labelled gauges. */
    public CardBuilder legend() {
        steps.add(c -> c.addRow(new CardElement.LegendRow()));
        return this;
    }

    public CardBuilder icons(int... slotIndexes) {
        steps.add(c -> c.addRow(new CardElement.IconsRow(slotIndexes)));
        return this;
    }

    /** A label with a horizontal gauge, for comparing amounts. Returns the label's text index. */
    public int hbar(int bar, Component label) {
        steps.add(c -> {
            int i = c.addText(label);
            c.addRow(new CardElement.HBarRow(bar, i));
        });
        return texts++;
    }

    // ------------------------------------------------------------------ machine screens (the real GUI layout)

    /**
     * Draws the card as a machine screen of this size in GUI pixels, with the game's own GUI textures; parts are
     * placed with the *At methods at the same spots as in the real screen. Rows added afterwards go under it.
     * titleX: where the title starts, or -1 to centre it.
     */
    public CardBuilder screen(int width, int height, int titleX) {
        steps.add(c -> c.useScreen(width, height, titleX));
        return this;
    }

    /** A slot whose frame's top-left corner is at x, y. kind: normal, input, output, power, extra, input_2, output_2,
     *  output_large, output_wide, none (item only) or vanilla (item only, for a picture background). */
    public int slotAt(int x, int y, String kind, ItemStack stack) {
        int i = slot(stack);
        steps.add(c -> c.addScreenSlot(x, y, kind, i));
        return i;
    }

    /** A gauge. type: standard, small, small_med, medium, wide; fill: chemical, fluid, energy, heat;
     *  frame: normal, red, blue, orange, aqua, yellow. Returns the bar index (its value is the fill level). */
    public int gaugeAt(int x, int y, String type, String fill, String frame, int color, float value, Component label) {
        int b = bar(color, value, label);
        steps.add(c -> c.addScreenGauge(x, y, type, fill, frame, b));
        return b;
    }

    /** A Mekanism power bar (vertical 6 x 54 or horizontal 54 x 6). Returns the bar index. */
    public int powerAt(int x, int y, boolean vertical, float value, Component label) {
        int b = bar(0xFF3CFE9A, value, label);
        steps.add(c -> c.addScreenPower(x, y, vertical, b));
        return b;
    }

    /** A progress arrow: bar, right, large_right, large_left, small_right, small_left, tall_right, bi. */
    public CardBuilder progressAt(int x, int y, String type) {
        steps.add(c -> c.addScreenProgress(x, y, type));
        return this;
    }

    /** The dark screen some machines have (text on it is usually green, 0xFF3CFE9A). */
    public CardBuilder innerScreenAt(int x, int y, int w, int h) {
        steps.add(c -> c.addScreenInner(x, y, w, h));
        return this;
    }

    /** Text at x, y (wrapped at maxWidth). Returns its index for FX#cardText. */
    public int textAt(int x, int y, Component text, int color, int maxWidth, boolean centered) {
        int i = texts++;
        steps.add(c -> {
            c.addText(text);
            c.addScreenText(x, y, i, color, maxWidth, centered);
        });
        return i;
    }

    /** Part of a texture drawn at x, y (e.g. a vanilla screen picture). */
    public CardBuilder imageAt(net.minecraft.resources.ResourceLocation tex, int x, int y, int u, int v, int w, int h, int texW, int texH) {
        steps.add(c -> c.addScreenImage(tex, x, y, u, v, w, h, texW, texH));
        return this;
    }

    CardElement build() {
        CardElement e = new CardElement(title, icon, target);
        steps.forEach(s -> s.accept(e));
        return e;
    }
}
