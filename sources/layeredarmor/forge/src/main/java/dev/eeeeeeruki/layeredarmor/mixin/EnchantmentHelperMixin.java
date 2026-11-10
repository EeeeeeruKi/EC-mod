package dev.eeeeeeruki.layeredarmor.mixin;

import dev.eeeeeeruki.layeredarmor.Layers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Predicate;

/**
 * 「体に着けている防具のエンチャント」を見る処理に重ね着の防具も含める。
 * （水中呼吸・水中歩行・氷渡り・ソウルスピード・スニーク速度上昇・修繕 など）
 * 防護系とトゲは getArmorSlots の拡張で効くようになる。
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I",
            at = @At("RETURN"), cancellable = true)
    private static void layeredarmor$level(Enchantment enchantment, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (enchantment == null || entity == null) return;
        int current = cir.getReturnValueI();
        EquipmentSlot[] slots = ((EnchantmentAccessor) enchantment).layeredarmor$getSlots();
        int level = Layers.enchantLevel(enchantment, entity, slots, current);
        if (level != current) cir.setReturnValue(level);
    }

    @Inject(method = "getRandomItemWith(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;",
            at = @At("RETURN"), cancellable = true)
    private static void layeredarmor$random(Enchantment enchantment, LivingEntity entity, Predicate<ItemStack> filter,
                                            CallbackInfoReturnable<Map.Entry<EquipmentSlot, ItemStack>> cir) {
        if (enchantment == null || entity == null) return;
        EquipmentSlot[] slots = ((EnchantmentAccessor) enchantment).layeredarmor$getSlots();
        Map.Entry<EquipmentSlot, ItemStack> current = cir.getReturnValue();
        Map.Entry<EquipmentSlot, ItemStack> picked = Layers.randomWith(enchantment, entity, slots, filter, current);
        if (picked != current) cir.setReturnValue(picked);
    }
}
