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

    CardElement build() {
        CardElement e = new CardElement(title, icon, target);
        steps.forEach(s -> s.accept(e));
        return e;
    }
}
