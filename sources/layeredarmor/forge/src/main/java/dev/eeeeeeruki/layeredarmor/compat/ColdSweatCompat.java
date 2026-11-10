package dev.eeeeeeruki.layeredarmor.compat;

import dev.eeeeeeruki.layeredarmor.LayeredArmor;
import dev.eeeeeeruki.layeredarmor.Layers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Cold Sweat の「装備しているアイテムを部位ごとに調べる」処理に重ね着の防具を混ぜる。
 *
 * やり方：元の処理が終わったあと、防具欄だけを「重ね着の防具」に差し替えた状態でもう一度同じ処理を走らせ、
 * その結果を足し合わせる。（防寒の値そのものは getArmorSlots の拡張で入る）
 */
public final class ColdSweatCompat {
    private ColdSweatCompat() {}

    private static final ThreadLocal<ItemStack[]> OVERRIDE = new ThreadLocal<>();
    private static boolean failed;

    /** Cold Sweat の中の getItemBySlot をこれに差し替えている */
    public static ItemStack getItemBySlot(LivingEntity entity, EquipmentSlot slot) {
        ItemStack[] override = OVERRIDE.get();
        if (override == null) return entity.getItemBySlot(slot);
        if (slot.getType() != EquipmentSlot.Type.ARMOR) return ItemStack.EMPTY;
        ItemStack s = override[slot.getIndex()];
        return s == null ? ItemStack.EMPTY : s;
    }

    private static final String MANAGER = "com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager";
    private static final Map<String, java.lang.reflect.Method> METHODS = new java.util.concurrent.ConcurrentHashMap<>();

    /** EntityTempManager の static メソッドを名前で呼ぶ（バージョン違いで無くても落ちないように） */
    public static Object call(String name, Object... args) {
        try {
            java.lang.reflect.Method m = METHODS.computeIfAbsent(name, n -> {
                try {
                    for (java.lang.reflect.Method cand : Class.forName(MANAGER).getMethods()) {
                        if (cand.getName().equals(n) && cand.getParameterCount() == args.length) return cand;
                    }
                } catch (Throwable ignored) {
                }
                return null;
            });
            return m == null ? null : m.invoke(null, args);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isRerunning() {
        return OVERRIDE.get() != null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void mergeMap(LivingEntity entity, Map result, Supplier<Map> rerun) {
        if (result == null) return;
        forEachLayer(entity, () -> {
            Map extra = rerun.get();
            if (extra != null) result.putAll(extra);
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void mergeCollection(LivingEntity entity, Collection result, Supplier<Collection> rerun) {
        if (result == null) return;
        forEachLayer(entity, () -> {
            Collection extra = rerun.get();
            if (extra != null) result.addAll(extra);
        });
    }

    private static void forEachLayer(LivingEntity entity, Runnable action) {
        if (failed || isRerunning() || !(entity instanceof Player)) return;
        try {
            ItemStack[][] layers = new ItemStack[4][];
            int depth = 0;
            for (EquipmentSlot slot : Layers.ARMOR_SLOTS) {
                List<Layers.Entry> entries = Layers.entries(entity, slot);
                depth = Math.max(depth, entries.size());
            }
            if (depth == 0) return;
            for (int layer = 0; layer < depth; layer++) {
                ItemStack[] override = new ItemStack[4];
                for (EquipmentSlot slot : Layers.ARMOR_SLOTS) {
                    List<Layers.Entry> entries = Layers.entries(entity, slot);
                    override[slot.getIndex()] = layer < entries.size() ? entries.get(layer).stack() : ItemStack.EMPTY;
                }
                OVERRIDE.set(override);
                try {
                    action.run();
                } finally {
                    OVERRIDE.remove();
                }
            }
        } catch (Throwable t) {
            failed = true;
            OVERRIDE.remove();
            LayeredArmor.LOGGER.warn("[LayeredArmor] Cold Sweatとの連携の一部に失敗したため、その部分を止めます", t);
        }
    }
}
