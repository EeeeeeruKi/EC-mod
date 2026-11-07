package com.eeeeeeruki.compacttoasts;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ほかのMODのトーストから、1行表示に使う中身を取り出す。
 * どのMODにも依存しないように、すべてリフレクションで読む。
 *  - FTB Quests / FTB Library（SimpleToast: getTitle / getSubtitle / getIcon / isImportant）
 *  - Patchouli（ボタニアの辞典など: LexiconToast）
 *  - そのほか、タイトルやアイコンを持っていそうなトースト
 */
final class ModToastInfo {
    static final int ACCENT_FTB = 0xFFFFD84A;
    static final int ACCENT_FTB_IMPORTANT = 0xFFFF88FF;
    static final int ACCENT_BOOK = 0xFF4FD08A;
    static final int ACCENT_OTHER = 0xFFFF9944;

    private static final Map<String, Optional<Method>> METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> FIELDS = new ConcurrentHashMap<>();

    private ModToastInfo() {}

    static ToastInfo of(Toast toast) {
        try {
            String name = toast.getClass().getName();
            if (name.startsWith("vazkii.patchouli.") || toast.getClass().getSimpleName().equals("LexiconToast")) {
                ToastInfo info = patchouli(toast);
                if (info != null) return info;
            }
            ToastInfo info = byMethods(toast);
            if (info != null) return info;
            return byFields(toast);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Patchouli の「本に新しい項目が増えました」トースト（ボタニアの辞典など）。 */
    private static ToastInfo patchouli(Toast toast) throws Exception {
        Object book = toast.getToken();
        if (book == null) return null;
        ItemStack icon = null;
        Object item = call(book, "getBookItem");
        if (item instanceof ItemStack stack) icon = stack;
        Component title = null;
        Field nameField = findField(book.getClass(), "name");
        if (nameField != null) {
            Object v = nameField.get(book);
            if (v instanceof Component c) title = c;
            else if (v instanceof String s) title = Component.translatable(s);
        }
        if (title == null && icon != null && !icon.isEmpty()) title = icon.getHoverName();
        if (title == null) return null;
        return new ToastInfo(icon, null, title, null, ACCENT_BOOK, Component.translatable("patchouli.gui.lexicon.toast.info"), false);
    }

    /** getTitle() などのメソッドを持つトースト（FTB Quests など）。 */
    private static ToastInfo byMethods(Toast toast) throws Exception {
        Component title = asComponent(call(toast, "getTitle"));
        if (title == null) return null;
        Component extra = null;
        for (String m : new String[]{"getSubtitle", "getDescription", "getMessage", "getText"}) {
            extra = asComponent(call(toast, m));
            if (extra != null && !extra.getString().isBlank()) break;
            extra = null;
        }
        Object iconObj = call(toast, "getIcon");
        ItemStack icon = iconObj instanceof ItemStack s ? s : null;
        ToastInfo.IconPainter painter = icon == null ? painterFor(iconObj) : null;
        boolean ftb = toast.getClass().getName().startsWith("dev.ftb.") || hasSuper(toast.getClass(), "dev.ftb.");
        int accent = ACCENT_OTHER;
        if (ftb) {
            Object important = call(toast, "isImportant");
            accent = Boolean.TRUE.equals(important) ? ACCENT_FTB_IMPORTANT : ACCENT_FTB;
        }
        // FTB のクエスト完了トーストは「クエスト完了！」がタイトルで、クエスト名がサブタイトル
        if (ftb && extra != null) {
            return new ToastInfo(icon, painter, extra, null, accent, title, false);
        }
        return new ToastInfo(icon, painter, title, extra, accent);
    }

    /** フィールドにタイトル（Component）とアイコン（ItemStack）を持っていそうなトースト。 */
    private static ToastInfo byFields(Toast toast) throws Exception {
        Component title = null;
        Component extra = null;
        ItemStack icon = null;
        ToastInfo.IconPainter painter = null;
        for (Field f : fields(toast.getClass())) {
            Object v = f.get(toast);
            if (v == null) continue;
            if (v instanceof Component c) {
                if (title == null) title = c;
                else if (extra == null) extra = c;
            } else if (v instanceof ItemStack s) {
                if (icon == null && !s.isEmpty()) icon = s;
            } else if (painter == null && icon == null) {
                painter = painterFor(v);
            }
        }
        if (title == null) return null;
        return new ToastInfo(icon, icon == null ? painter : null, title, extra, ACCENT_OTHER);
    }

    /** draw(GuiGraphics, int, int, int, int) を持つアイコン（FTB Library の Icon など）を描く。 */
    private static ToastInfo.IconPainter painterFor(Object icon) {
        if (icon == null) return null;
        Method draw = method(icon.getClass(), "draw", GuiGraphics.class, int.class, int.class, int.class, int.class);
        if (draw == null) return null;
        return (g, x, y) -> {
            try {
                draw.invoke(icon, g, x, y, 16, 16);
            } catch (Throwable ignored) {
            }
        };
    }

    private static Component asComponent(Object o) {
        if (o instanceof Component c) return c;
        if (o instanceof String s && !s.isBlank()) return Component.literal(s);
        return null;
    }

    private static boolean hasSuper(Class<?> c, String prefix) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            if (k.getName().startsWith(prefix)) return true;
        }
        return false;
    }

    private static Object call(Object obj, String name) throws Exception {
        Method m = method(obj.getClass(), name);
        return m == null ? null : m.invoke(obj);
    }

    private static Method method(Class<?> cls, String name, Class<?>... params) {
        StringBuilder key = new StringBuilder(cls.getName()).append('#').append(name);
        for (Class<?> p : params) key.append(',').append(p.getName());
        return METHODS.computeIfAbsent(key.toString(), k -> {
            for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    Method m = c.getDeclaredMethod(name, params);
                    if (Modifier.isStatic(m.getModifiers())) continue;
                    m.setAccessible(true);
                    return Optional.of(m);
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable t) {
                    return Optional.empty();
                }
            }
            try {
                Method m = cls.getMethod(name, params);
                m.setAccessible(true);
                return Optional.of(m);
            } catch (Throwable t) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    private static Field findField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static List<Field> fields(Class<?> cls) {
        return FIELDS.computeIfAbsent(cls, k -> {
            List<Field> out = new ArrayList<>();
            for (Class<?> c = k; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                    try {
                        f.setAccessible(true);
                        out.add(f);
                    } catch (Throwable ignored) {
                    }
                }
            }
            return out;
        });
    }
}
