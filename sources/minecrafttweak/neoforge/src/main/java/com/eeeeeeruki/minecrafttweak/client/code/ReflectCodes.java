package com.eeeeeeruki.minecrafttweak.client.code;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.core.CodeEntry;
import com.eeeeeeruki.minecrafttweak.core.CodeProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 他MODの設定を、そのMODの設定クラスの項目（フィールド）を直接読み書きして変える。
 * MODやバージョンごとに名前が違うので、候補をいくつも並べて最初に見つかったものを使う。
 */
public class ReflectCodes implements CodeProvider {
    private final String prefix;
    private final String nameKey;
    private final String[][] holders; // {クラス名, 設定を返す static メソッド名}
    private final List<CodeEntry> entries = new ArrayList<>();
    private final boolean reloadChunks;
    private Method getter;
    private boolean searched;

    public ReflectCodes(String prefix, String nameKey, boolean reloadChunks, String[][] holders) {
        this.prefix = prefix;
        this.nameKey = nameKey;
        this.reloadChunks = reloadChunks;
        this.holders = holders;
    }

    /** 項目を足す。paths は "quality.leavesQuality" のような形で、見つかった最初のものを使う。 */
    public ReflectCodes add(int number, int min, int max, String... paths) {
        entries.add(new Entry(prefix + number, "minecrafttweak.code." + prefix + number, min, max, paths));
        return this;
    }

    public ReflectCodes add(int number, String... paths) {
        return add(number, Integer.MIN_VALUE, Integer.MAX_VALUE, paths);
    }

    private Object options() {
        if (!searched) {
            searched = true;
            for (String[] h : holders) {
                try {
                    Class<?> c = Class.forName(h[0], true, ReflectCodes.class.getClassLoader());
                    Method m = c.getMethod(h[1]);
                    if (Modifier.isStatic(m.getModifiers())) {
                        getter = m;
                        break;
                    }
                } catch (Throwable ignored) {
                    // このMODは入っていない
                }
            }
        }
        if (getter == null) {
            return null;
        }
        try {
            return getter.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String prefix() {
        return prefix;
    }

    @Override
    public Component displayName() {
        return Component.translatable(nameKey);
    }

    @Override
    public boolean available() {
        return options() != null;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public List<CodeEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public void afterApply() throws RuntimeException {
        Object options = options();
        if (options == null) {
            return;
        }
        try {
            try {
                options.getClass().getMethod("writeChanges").invoke(options);
            } catch (NoSuchMethodException e) {
                options.getClass().getMethod("writeToDisk", options.getClass()).invoke(null, options);
            }
        } catch (Exception e) {
            MinecraftTweak.LOGGER.warn("Could not save {} options", prefix, e);
            throw new RuntimeException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
        Minecraft mc = Minecraft.getInstance();
        if (reloadChunks && mc.level != null && mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    /** 見つかったフィールドと、それを持っているオブジェクト。 */
    private record Target(Object owner, Field field) {
        Object get() throws IllegalAccessException {
            return field.get(owner);
        }
    }

    private final class Entry implements CodeEntry {
        private final String key;
        private final String nameKey;
        private final int min;
        private final int max;
        private final String[] paths;

        Entry(String key, String nameKey, int min, int max, String[] paths) {
            this.key = key;
            this.nameKey = nameKey;
            this.min = min;
            this.max = max;
            this.paths = paths;
        }

        private Target target() {
            Object root = options();
            if (root == null) {
                return null;
            }
            for (String path : paths) {
                try {
                    Object owner = root;
                    String[] parts = path.split("\\.");
                    for (int i = 0; i < parts.length - 1 && owner != null; i++) {
                        owner = owner.getClass().getField(parts[i]).get(owner);
                    }
                    if (owner == null) {
                        continue;
                    }
                    Field f = owner.getClass().getField(parts[parts.length - 1]);
                    if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                        continue;
                    }
                    return new Target(owner, f);
                } catch (Throwable ignored) {
                    // このバージョンには無い
                }
            }
            return null;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public Component name() {
            return Component.translatable(nameKey);
        }

        @Override
        public boolean available() {
            return target() != null;
        }

        @Override
        public String textValue() {
            Target t = target();
            if (t == null) {
                return null;
            }
            try {
                return CodeValues.format(t.get());
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        @Override
        public Component apply(String raw) {
            Target t = target();
            if (t == null) {
                return Component.translatable("minecrafttweak.code.error.unavailable", key);
            }
            try {
                Object current = t.get();
                Object parsed = CodeValues.parseLike(current, t.field().getType(), raw);
                if (parsed == null) {
                    return Component.translatable("minecrafttweak.code.error.invalid", key, raw);
                }
                if (parsed instanceof Integer i && (i < min || i > max)) {
                    return Component.translatable("minecrafttweak.text.error.range", key, raw,
                            String.valueOf(min), String.valueOf(max));
                }
                t.field().set(t.owner(), parsed);
                return null;
            } catch (IllegalAccessException e) {
                return Component.translatable("minecrafttweak.code.error.failed", key, e.getMessage());
            }
        }
    }
}
