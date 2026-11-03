package com.eeeeeeruki.compacttoasts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastComponent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Minecraft が持っている ToastComponent を CompactToastComponent に差し替える。 */
final class ToastInstaller {
    private static boolean done;

    private ToastInstaller() {}

    static void install() {
        if (done) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        done = true;
        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() != ToastComponent.class) continue;
                f.setAccessible(true);
                Object old = f.get(mc);
                if (old instanceof CompactToastComponent) return;
                CompactToastComponent replacement = new CompactToastComponent(mc);
                if (old instanceof ToastComponent oldToasts) replacement.adopt(oldToasts);
                f.set(mc, replacement);
                if (mc.getToasts() == replacement) {
                    CompactToasts.LOGGER.info("Compact Toasts: toast display replaced");
                } else {
                    CompactToasts.LOGGER.warn("Compact Toasts: could not replace the toast display");
                }
                return;
            }
            CompactToasts.LOGGER.warn("Compact Toasts: toast field not found");
        } catch (Throwable t) {
            CompactToasts.LOGGER.error("Compact Toasts: failed to replace the toast display", t);
        }
    }
}
