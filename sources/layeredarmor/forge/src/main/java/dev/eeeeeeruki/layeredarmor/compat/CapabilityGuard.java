package dev.eeeeeeruki.layeredarmor.compat;

import dev.eeeeeeruki.layeredarmor.LayeredArmor;
import dev.eeeeeeruki.layeredarmor.Layers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 「Duplicate Capability Key: curios:item」でゲームが落ちるのを防ぐ。
 *
 * 他のMODが同じ防具に自分で Curios の機能を付けている場合、Layered Armor の登録とぶつかる。
 * そのときは他のMODの方を残し、Layered Armor の方を捨てる。
 * 他のMODどうしがぶつかった場合も、落とさずに先に付いた方を残す。
 */
public final class CapabilityGuard {
    private CapabilityGuard() {}

    /** Curios 本体のアイテム機能の取り付け処理を実行中か */
    public static final ThreadLocal<Boolean> IN_CURIOS = ThreadLocal.withInitial(() -> false);
    /** Layered Armor の登録から付いた curios:item を持っているイベント */
    private static final Set<Object> OURS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static boolean warned;

    private static ResourceLocation key() {
        return top.theillusivec4.curios.api.CuriosCapability.ID_ITEM;
    }

    /**
     * @return true なら元の addCapability を止める
     */
    public static <V> boolean onAdd(Object event, Object object, ResourceLocation id, V provider, Map<ResourceLocation, V> caps) {
        try {
            if (!key().equals(id) || !(object instanceof ItemStack stack)) return false;
            boolean ours = IN_CURIOS.get() && Layers.LAYERABLE.contains(stack.getItem());
            if (!caps.containsKey(id)) {
                if (ours) OURS.add(event);
                return false;
            }
            if (ours) {
                // 他のMODが先に付けている → Layered Armor の分は付けない
                return true;
            }
            if (OURS.remove(event)) {
                // Layered Armor の分が先に付いている → 他のMODの分に置き換える
                caps.put(id, provider);
                return true;
            }
            // 他のMODどうしの重複。落とさずに先の方を残す
            if (!warned) {
                warned = true;
                LayeredArmor.LOGGER.warn("[LayeredArmor] {} に curios:item が二重に付けられようとしたため、後の方を無視しました（クラッシュ防止）", stack.getItem());
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
