package com.eeeeeeruki.claudeupdater;

import com.eeeeeeruki.claudeupdater.client.ClientHooks;
import com.eeeeeeruki.claudeupdater.client.UpdateIconButton;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Client entry point: keys, the title-screen button and the update screen. */
public class ClaudeUpdaterClient implements ClientModInitializer {
    /**
     * Hold this together with Ctrl while opening the update menu to get the install screen. Shown as ??? in
     * Controls. Right Shift by default so it does not take Left Shift away from sneaking; either Shift works.
     */
    static final KeyMapping SECRET = new KeyMapping("???", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.misc");
    /** Ctrl + this key (U by default) opens the update check from any menu. */
    static final KeyMapping UPDATE = new KeyMapping("MODの更新を確認", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U, "key.categories.misc");

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(UPDATE);
        KeyBindingHelper.registerKeyBinding(SECRET);
        ClientHooks.secretHeld = ClaudeUpdaterClient::secretHeld;
        UpdateIconButton.ICON = ResourceLocation.fromNamespaceAndPath(ClaudeUpdater.MODID, "textures/gui/update.png");

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) {
                List<AbstractWidget> buttons = Screens.getButtons(screen);
                List<GuiEventListener> widgets = new ArrayList<>(buttons);
                UpdateIconButton.addTo(screen, widgets, l -> buttons.add((AbstractWidget) l));
            }
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) ->
                    !ClientHooks.onMenuKey(s, key, scancode, UPDATE));
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> ClientHooks.onClientTick());
    }

    /** Screens swallow key events, so the key's own state is not updated on the title screen: read GLFW. */
    static boolean secretHeld() {
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(SECRET);
        if (key.equals(InputConstants.UNKNOWN)) return false;
        long w = Minecraft.getInstance().getWindow().getWindow();
        int code = key.getValue();
        boolean down;
        if (key.getType() == InputConstants.Type.KEYSYM) {
            down = InputConstants.isKeyDown(w, code);
            if (code == GLFW.GLFW_KEY_LEFT_SHIFT || code == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                down |= InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_SHIFT)
                        || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_SHIFT);
            }
            if (code == GLFW.GLFW_KEY_LEFT_CONTROL || code == GLFW.GLFW_KEY_RIGHT_CONTROL) {
                down |= InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_CONTROL);
            }
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            down = GLFW.glfwGetMouseButton(w, code) == GLFW.GLFW_PRESS;
        } else {
            down = false;
        }
        return down && Screen.hasControlDown();
    }
}
