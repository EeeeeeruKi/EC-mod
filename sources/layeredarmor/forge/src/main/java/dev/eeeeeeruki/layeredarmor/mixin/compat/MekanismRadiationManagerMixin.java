package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mekanism：放射線防護（防護服・メカスーツ） */
@Pseudo
@Mixin(targets = "mekanism.common.lib.radiation.RadiationManager", remap = false)
public abstract class MekanismRadiationManagerMixin {

    @Redirect(method = {
            "getRadiationResistance(Lnet/minecraft/world/entity/LivingEntity;)D"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private ItemStack layeredarmor$slot1(net.minecraft.world.entity.LivingEntity entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "mekanism");
    }
}
