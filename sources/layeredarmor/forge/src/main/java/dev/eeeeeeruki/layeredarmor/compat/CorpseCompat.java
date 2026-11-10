package dev.eeeeeeruki.layeredarmor.compat;

import dev.eeeeeeruki.layeredarmor.LayeredArmor;
import dev.eeeeeeruki.layeredarmor.Layers;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Corpse（死体MOD）の見た目に重ね着の防具を反映する。
 *
 * Corpseは死んだ瞬間の装備を「見た目用のコピー」として死体に保存している（アイテム本体は別に保存される）。
 * そのコピーに対して、プレイヤーと同じ表示ルールを当てる：
 *  ・バニラ欄が空 → 重ね着の防具のコピーを入れる（印を付ける）
 *  ・バニラ欄に防具がある → そのまま。ただしCosmetic Armorで透明にされたときのために、下に着ている重ね着防具を印として持たせる
 */
public final class CorpseCompat {
    private CorpseCompat() {}

    public static final String TAG_LAYERED = "LayeredArmorCopy";
    public static final String TAG_UNDER = "LayeredArmorUnder";

    private static Method getEquipment;
    private static boolean failed;

    @SuppressWarnings("unchecked")
    public static void applyToDeath(Player player, Object death) {
        if (failed || player == null || death == null) return;
        try {
            if (getEquipment == null) {
                getEquipment = death.getClass().getMethod("getEquipment");
            }
            Object list = getEquipment.invoke(death);
            if (!(list instanceof NonNullList<?> raw)) return;
            NonNullList<ItemStack> equipment = (NonNullList<ItemStack>) raw;
            for (EquipmentSlot slot : Layers.ARMOR_SLOTS) {
                int idx = slot.ordinal(); // Corpseは EquipmentSlot.values() の順で保存している
                if (idx >= equipment.size()) continue;
                ItemStack layered = Layers.visible(player, slot);
                if (layered.isEmpty()) continue;
                ItemStack current = equipment.get(idx);
                if (current.isEmpty()) {
                    ItemStack copy = layered.copy();
                    copy.getOrCreateTag().putBoolean(TAG_LAYERED, true);
                    equipment.set(idx, copy);
                } else {
                    ItemStack copy = current.copy();
                    copy.getOrCreateTag().put(TAG_UNDER, layered.copy().save(new CompoundTag()));
                    equipment.set(idx, copy);
                }
            }
        } catch (Throwable t) {
            failed = true;
            LayeredArmor.LOGGER.warn("[LayeredArmor] Corpseとの連携に失敗したため、死体の見た目への反映を止めます", t);
        }
    }

    public static boolean hasDisplayTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && (tag.contains(TAG_LAYERED) || tag.contains(TAG_UNDER, Tag.TAG_COMPOUND));
    }

    public static boolean isLayeredCopy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_LAYERED);
    }

    public static CompoundTag underTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_UNDER, Tag.TAG_COMPOUND) ? tag.getCompound(TAG_UNDER) : null;
    }
}
