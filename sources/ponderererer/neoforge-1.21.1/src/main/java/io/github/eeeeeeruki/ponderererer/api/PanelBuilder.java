package io.github.eeeeeeruki.ponderererer.api;

import io.github.eeeeeeruki.ponderererer.element.PanelElement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lays out a fake machine window. Slot and bar indexes are returned in the order they were added.
 * <pre>
 * PanelBuilder p = PanelBuilder.machine(Component.literal("Enrichment Chamber"));
 * int in = p.slot(56, 17, ItemStack.EMPTY);
 * int out = p.output(116, 35, ItemStack.EMPTY);
 * p.arrow(79, 35);
 * int energy = p.bar(160, 15, 6, 52, 0xFF3CFE9A, 1f, null);
 * </pre>
 */
public class PanelBuilder {
    final Component title;
    final int width, height;
    final List<Consumer<PanelElement>> steps = new ArrayList<>();
    int slots, bars;
    Vec3 anchor;
    Integer offsetX, offsetY;

    public PanelBuilder(Component title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    /** A 176x80 window, the size of the top part of a vanilla machine screen. */
    public static PanelBuilder machine(Component title) {
        return new PanelBuilder(title, 176, 80);
    }

    /** Puts the window next to a point in the scene instead of the middle of the screen. */
    public PanelBuilder at(Vec3 anchor) {
        this.anchor = anchor;
        return this;
    }

    public PanelBuilder offset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        return this;
    }

    public int slot(int x, int y, ItemStack stack) {
        steps.add(p -> p.addSlot(x, y, stack.copy(), false));
        return slots++;
    }

    /** A large output slot. */
    public int output(int x, int y, ItemStack stack) {
        steps.add(p -> p.addSlot(x, y, stack.copy(), true));
        return slots++;
    }

    /** A progress arrow pointing right. Animate it with FX#panelProgress. */
    public PanelBuilder arrow(int x, int y) {
        steps.add(p -> p.setArrow(x, y));
        return this;
    }

    /** A vertical bar (energy, fluid, chemical). value is 0..1. */
    public int bar(int x, int y, int w, int h, int color, float value, Component label) {
        steps.add(p -> p.addBar(x, y, w, h, color, value, label));
        return bars++;
    }

    PanelElement build() {
        PanelElement e = new PanelElement(anchor, title, width, height);
        if (offsetX != null) e.setOffset(offsetX, offsetY);
        else if (anchor == null) e.setOffset(0, 0);
        steps.forEach(s -> s.accept(e));
        return e;
    }
}
