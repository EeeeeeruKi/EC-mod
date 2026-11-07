package com.eeeeeeruki.compacttoasts;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** コンパクト表示の1行に出す内容。バニラのトーストから取り出す。 */
record ToastInfo(@Nullable ItemStack icon, @Nullable IconPainter painter, Component text, @Nullable Component extra, int accent) {
    /** ItemStack 以外のアイコン（FTB Library の Icon など）を描く。 */
    interface IconPainter {
        void paint(net.minecraft.client.gui.GuiGraphics g, int x, int y);
    }

    private static final Map<String, Optional<Field>> FIELDS = new ConcurrentHashMap<>();

    @Nullable
    static ToastInfo of(Toast toast) {
        try {
            if (toast instanceof AdvancementToast) return advancement(toast);
            if (toast instanceof RecipeToast) return recipe(toast);
            if (toast instanceof SystemToast) return system(toast);
            if (toast instanceof TutorialToast) return tutorial(toast);
            return ModToastInfo.of(toast);
        } catch (Throwable t) {
            // 取り出せないときは縮小表示に任せる
        }
        return null;
    }

    @Nullable
    private static ToastInfo advancement(Toast toast) throws IllegalAccessException {
        Advancement adv = get(toast, Advancement.class, 0);
        if (adv == null) return null;
        DisplayInfo display = adv.getDisplay();
        if (display == null) return null;
        ChatFormatting fmt = display.getFrame().getChatColor();
        Integer color = fmt.getColor();
        int accent = 0xFF000000 | (color != null ? color : 0xFFFF55);
        return new ToastInfo(display.getIcon(), null, display.getTitle(), null, accent);
    }

    @Nullable
    private static ToastInfo recipe(Toast toast) throws IllegalAccessException {
        List<?> recipes = get(toast, List.class, 0);
        if (recipes == null || recipes.isEmpty()) return null;
        Object first = recipes.get(0);
        if (!(first instanceof Recipe<?> recipe)) return null;
        Minecraft mc = Minecraft.getInstance();
        ItemStack result = mc.level != null ? recipe.getResultItem(mc.level.registryAccess()) : ItemStack.EMPTY;
        ItemStack icon = result.isEmpty() ? recipe.getToastSymbol() : result;
        Component name = result.isEmpty() ? Component.translatable("recipe.toast.title") : result.getHoverName();
        Component extra = recipes.size() > 1 ? Component.translatable("compacttoasts.more", recipes.size() - 1) : null;
        return new ToastInfo(icon, null, Component.translatable("compacttoasts.recipe", name), extra, 0xFF55FF55);
    }

    @Nullable
    private static ToastInfo system(Toast toast) throws IllegalAccessException {
        Component title = get(toast, Component.class, 0);
        if (title == null) return null;
        Component extra = null;
        List<?> lines = get(toast, List.class, 0);
        if (lines != null && !lines.isEmpty() && lines.get(0) instanceof FormattedCharSequence seq) {
            String s = plain(seq);
            if (!s.isBlank()) extra = Component.literal(s);
        }
        return new ToastInfo(null, null, title, extra, 0xFF5599FF);
    }

    @Nullable
    private static ToastInfo tutorial(Toast toast) throws IllegalAccessException {
        Component title = get(toast, Component.class, 0);
        if (title == null) return null;
        Component message = get(toast, Component.class, 1);
        return new ToastInfo(null, null, title, message, 0xFFAAAAAA);
    }

    private static String plain(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    /** 型で探したフィールドの値を返す（難読化された名前に頼らないため）。 */
    @Nullable
    @SuppressWarnings("unchecked")
    private static <T> T get(Object obj, Class<T> type, int nth) throws IllegalAccessException {
        String key = obj.getClass().getName() + "|" + type.getName() + "|" + nth;
        Optional<Field> field = FIELDS.computeIfAbsent(key, k -> find(obj.getClass(), type, nth));
        if (field.isEmpty()) return null;
        Object value = field.get().get(obj);
        return type.isInstance(value) ? (T) value : null;
    }

    private static Optional<Field> find(Class<?> cls, Class<?> type, int nth) {
        List<Field> matches = new ArrayList<>();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (type.isAssignableFrom(f.getType())) matches.add(f);
            }
        }
        if (matches.size() <= nth) return Optional.empty();
        Field f = matches.get(nth);
        try {
            f.setAccessible(true);
        } catch (Throwable t) {
            return Optional.empty();
        }
        return Optional.of(f);
    }
}
