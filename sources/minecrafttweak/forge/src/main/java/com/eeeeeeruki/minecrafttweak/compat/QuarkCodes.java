package com.eeeeeeruki.minecrafttweak.compat;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.core.CodeValues;
import com.eeeeeeruki.minecrafttweak.core.CodeEntry;
import com.eeeeeeruki.minecrafttweak.core.CodeProvider;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Quark の設定（頭文字 q）。Quark が使っている Zeta の設定の仕組みを、そのまま読み書きする。
 *
 * 項目番号は、設定の場所（例: tweaks.automatic_recipe_unlock.enabled）から計算した6けたの数字。
 * Quark のバージョンが変わって設定が増えても、同じ設定の番号はほぼ変わらない。
 * 数値・オン/オフの設定だけを扱い、文字や一覧の設定は飛ばす。
 */
public final class QuarkCodes implements CodeProvider {
    private List<CodeEntry> entries;
    private Object zeta;
    private Object internals;
    private Method getMethod;
    private Method setMethod;
    private Method flushMethod;
    private boolean failed;

    @Override
    public String prefix() {
        return "q";
    }

    @Override
    public Component displayName() {
        return Component.translatable("minecrafttweak.code.provider.q");
    }

    @Override
    public boolean available() {
        return load();
    }

    @Override
    public boolean clientOnly() {
        return false;
    }

    @Override
    public List<CodeEntry> entries() {
        return load() ? Collections.unmodifiableList(entries) : List.of();
    }

    @Override
    public void afterApply() {
        if (load()) {
            try {
                flushMethod.invoke(internals);
            } catch (Exception e) {
                throw new RuntimeException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
        }
    }

    /** Quark の設定を読みにいく。設定ができあがる前（起動の途中）なら false。 */
    private synchronized boolean load() {
        if (entries != null) {
            return true;
        }
        if (failed) {
            return false;
        }
        try {
            Class<?> quark = Class.forName("org.violetmoon.quark.base.Quark", true, QuarkCodes.class.getClassLoader());
            zeta = quark.getField("ZETA").get(null);
            Object manager = zeta.getClass().getField("configManager").get(zeta);
            internals = zeta.getClass().getField("configInternals").get(zeta);
            if (manager == null || internals == null) {
                return false; // まだ用意されていない
            }
            Class<?> valueDef = Class.forName("org.violetmoon.zeta.config.ValueDefinition", true, QuarkCodes.class.getClassLoader());
            Class<?> internalsType = Class.forName("org.violetmoon.zeta.config.IZetaConfigInternals", true, QuarkCodes.class.getClassLoader());
            getMethod = internalsType.getMethod("get", valueDef);
            setMethod = internalsType.getMethod("set", valueDef, Object.class);
            flushMethod = internalsType.getMethod("flush");

            Object root = manager.getClass().getMethod("getRootConfig").invoke(manager);
            List<Object> values = new ArrayList<>();
            collect(root, values);

            Field pathField = Class.forName("org.violetmoon.zeta.config.Definition", true, QuarkCodes.class.getClassLoader()).getField("path");
            Field defaultField = valueDef.getField("defaultValue");
            List<Entry> list = new ArrayList<>();
            for (Object def : values) {
                @SuppressWarnings("unchecked")
                List<String> path = (List<String>) pathField.get(def);
                Object defaultValue = defaultField.get(def);
                if (defaultValue instanceof Boolean || defaultValue instanceof Integer || defaultValue instanceof Double) {
                    list.add(new Entry(String.join(".", path), def, defaultValue));
                }
            }
            list.sort(Comparator.comparing(e -> e.path));
            Set<Integer> used = new HashSet<>();
            for (Entry e : list) {
                int id = hashId(e.path);
                while (!used.add(id)) {
                    id = id >= 999999 ? 100000 : id + 1;
                }
                e.key = "q" + id;
            }
            list.sort(Comparator.comparing(e -> e.key));
            entries = new ArrayList<>(list);
            return true;
        } catch (ClassNotFoundException e) {
            failed = true; // Quark が入っていない
            return false;
        } catch (Throwable t) {
            MinecraftTweak.LOGGER.debug("Quark config is not ready yet", t);
            return false;
        }
    }

    private static void collect(Object section, List<Object> out) throws Exception {
        Collection<?> values = (Collection<?>) section.getClass().getMethod("getValues").invoke(section);
        out.addAll(values);
        Collection<?> subs = (Collection<?>) section.getClass().getMethod("getSubsections").invoke(section);
        for (Object sub : subs) {
            collect(sub, out);
        }
    }

    private static int hashId(String path) {
        CRC32 crc = new CRC32();
        crc.update(path.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (int) (crc.getValue() % 900000L) + 100000;
    }

    private final class Entry implements CodeEntry {
        private final String path;
        private final Object def;
        private final Object defaultValue;
        private String key;

        Entry(String path, Object def, Object defaultValue) {
            this.path = path;
            this.def = def;
            this.defaultValue = defaultValue;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public Component name() {
            return Component.literal(path.replace(".", " > "));
        }

        @Override
        public boolean available() {
            return true;
        }

        private Object get() throws Exception {
            return getMethod.invoke(internals, def);
        }

        @Override
        public String textValue() {
            try {
                return CodeValues.format(get());
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public String defaultTextValue() {
            return CodeValues.format(defaultValue);
        }

        @Override
        public Component apply(String raw) {
            Object parsed = CodeValues.parseLike(defaultValue, defaultValue.getClass(), raw);
            if (parsed == null) {
                return Component.translatable("minecrafttweak.code.error.invalid", key, raw);
            }
            try {
                boolean ok = (boolean) def.getClass().getMethod("validate", Object.class).invoke(def, parsed);
                if (!ok) {
                    return Component.translatable("minecrafttweak.code.error.invalid", key, raw);
                }
                setMethod.invoke(internals, def, parsed);
                return null;
            } catch (Exception e) {
                return Component.translatable("minecrafttweak.code.error.failed", key,
                        String.valueOf(e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
        }
    }
}
