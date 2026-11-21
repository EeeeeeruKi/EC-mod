package com.eeeeeeruki.minecrafttweak.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * マイクラの音の仕組み（OpenAL）が使える状態かを調べる。
 * 起動時の読み込み中や、言語・リソースパックの読み込み直しの途中は OpenAL がまだ無い（作り直し中）。
 * そのときに OpenAL を呼ぶと LWJGL の中が壊れて、音の読み込みも、リソースの読み込み全体も失敗してしまう。
 */
public final class SoundReady {
    private static Field engineField;
    private static Field loadedField;
    private static boolean lookedUp;

    private SoundReady() {
    }

    public static boolean ready() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) {
            return false;
        }
        try {
            if (!lookedUp) {
                lookedUp = true;
                for (Field f : SoundManager.class.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers()) && f.getType() == SoundEngine.class) {
                        f.setAccessible(true);
                        engineField = f;
                        break;
                    }
                }
                for (Field f : SoundEngine.class.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers()) && f.getType() == boolean.class) {
                        f.setAccessible(true);
                        loadedField = f;
                        break;
                    }
                }
            }
            if (engineField == null || loadedField == null) {
                return false;
            }
            Object engine = engineField.get(mc.getSoundManager());
            return engine != null && loadedField.getBoolean(engine);
        } catch (Throwable t) {
            return false;
        }
    }
}
