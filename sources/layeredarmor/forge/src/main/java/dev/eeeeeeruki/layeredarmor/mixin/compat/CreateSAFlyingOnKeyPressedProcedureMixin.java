package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Create Stuff 'N Additions：ジェットパック・外骨格など */
@Pseudo
@Mixin(targets = "net.mcreator.createstuffadditions.procedures.FlyingOnKeyPressedProcedure", remap = false)
public abstract class CreateSAFlyingOnKeyPressedProcedureMixin {

    @Redirect(method = {
            "execute(Lnet/minecraft/world/entity/Entity;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private static ItemStack layeredarmor$slot1(net.minecraft.world.entity.LivingEntity entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "create_sa");
    }
}
