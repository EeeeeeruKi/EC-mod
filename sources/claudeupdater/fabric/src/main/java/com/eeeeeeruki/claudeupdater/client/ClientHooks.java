package com.eeeeeeruki.claudeupdater.client;

import com.eeeeeeruki.claudeupdater.core.UpdateCandidate;
import com.eeeeeeruki.claudeupdater.core.UpdaterCore;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.List;
import java.util.function.BooleanSupplier;

/** Opens the update screen over the title screen once the startup check has finished. */
public final class ClientHooks {
    private ClientHooks() {
    }

    private static boolean shown;

    /** True while the ??? key (Ctrl+Shift by default) is held. Set by the loader entry point. */
    public static BooleanSupplier secretHeld = () -> false;

    /**
     * Shortcut key on menus (Ctrl+U by default): opens the update check, or the install screen when the ???
     * key is held too. Works even when another mod hides or covers the title-screen button.
     *
     * @return true when the key was handled (the event should be cancelled)
     */
    public static boolean onMenuKey(Screen screen, int keyCode, int scanCode, KeyMapping updateKey) {
        Minecraft mc = Minecraft.getInstance();
        if (screen == null || mc.level != null) return false; // menus only, never in-game
        if (screen instanceof UpdateScreen || screen instanceof InstallScreen) return false;
        if (screen.getFocused() instanceof EditBox) return false; // typing in a text field
        // Fabric has no key modifiers: the key itself is U by default and Ctrl must be held with it
        if (!Screen.hasControlDown() || !updateKey.matches(keyCode, scanCode)) return false;
        mc.setScreen(openMenu(screen));
        return true;
    }

    /** The update menu, or the hidden install screen when the ??? key is held. */
    public static Screen openMenu(Screen parent) {
        boolean secret;
        try {
            secret = secretHeld.getAsBoolean();
        } catch (Throwable t) {
            secret = false;
        }
        return secret ? new InstallScreen(parent) : new UpdateScreen(parent);
    }

    public static void onClientTick() {
        if (shown || !UpdaterCore.checked()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof TitleScreen)) return;
        shown = true;
        List<UpdateCandidate> found = UpdaterCore.found();
        if (!found.isEmpty()) mc.setScreen(new UpdateScreen(mc.screen, found));
    }
}
