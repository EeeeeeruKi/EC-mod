package com.eeeeeeruki.claudeupdater.client;

import com.eeeeeeruki.claudeupdater.core.UpdaterCore;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/** 20x20 icon-only button on the title screen that opens the update check. */
public class UpdateIconButton extends Button {
    /** Set by the loader entry point (ResourceLocation is built differently on each version). */
    public static ResourceLocation ICON;

    public UpdateIconButton(int x, int y, Screen parent) {
        super(x, y, 20, 20, Component.literal("MODの更新"),
                b -> Minecraft.getInstance().setScreen(ClientHooks.openMenu(parent)), DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.literal("MODの更新を確認します")));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Component label = getMessage();
        setMessage(Component.empty()); // draw the plain button frame without text
        super.renderWidget(g, mouseX, mouseY, partialTick);
        setMessage(label);

        RenderSystem.enableBlend();
        g.blit(ICON, getX() + 2, getY() + 2, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();

        // green dot when the startup check found updates
        if (!UpdaterCore.found().isEmpty()) {
            g.fill(getX() + 14, getY() + 2, getX() + 18, getY() + 6, 0xFF1E7A3A);
            g.fill(getX() + 15, getY() + 3, getX() + 17, getY() + 5, 0xFF55FF55);
        }
    }

    /**
     * Adds the button to the title screen: just left of the language button when it can be found,
     * otherwise in the top-left corner.
     */
    public static void addTo(Screen screen, List<GuiEventListener> widgets, Consumer<GuiEventListener> add) {
        int x = 4;
        int y = 4;
        AbstractWidget lang = null;
        for (GuiEventListener l : widgets) {
            if (l instanceof AbstractWidget w && w.getWidth() == 20 && w.getHeight() == 20
                    && w.getX() + 20 <= screen.width / 2 - 100
                    && (lang == null || w.getY() > lang.getY())) {
                lang = w; // the small square button left of the main column (language)
            }
        }
        if (lang != null && lang.getX() - 24 >= 0) {
            x = lang.getX() - 24;
            y = lang.getY();
        }
        add.accept(new UpdateIconButton(x, y, screen));
    }
}
