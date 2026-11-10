package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mekanism：メカスーツの見た目 */
@Pseudo
@Mixin(targets = "mekanism.client.render.layer.MekanismArmorLayer", remap = false)
public abstract class MekanismArmorLayerMixin {

    @Redirect(method = {
            "renderArmorPart(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;IF)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private ItemStack layeredarmor$slot1(net.minecraft.world.entity.LivingEntity entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.client.ClientLayers.displayBySlot(entity, slot);
    }
}
