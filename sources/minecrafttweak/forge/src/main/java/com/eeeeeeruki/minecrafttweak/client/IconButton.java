package com.eeeeeeruki.minecrafttweak.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 左にアイテムのアイコンが付くボタン（Quark の Q メニューの分類ボタンと同じ形）。 */
public class IconButton extends Button {
    private final ItemStack icon;

    public IconButton(int x, int y, int width, int height, Component message, ItemStack icon, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, getX() + 2, getY() + (getHeight() - 16) / 2);
        }
    }
}
