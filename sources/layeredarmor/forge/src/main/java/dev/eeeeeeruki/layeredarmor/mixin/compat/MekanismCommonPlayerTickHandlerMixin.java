package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mekanism：メカスーツの機能（落下ダメージ吸収・ステップ補助・重力制御など）を重ね着でも動かす */
@Pseudo
@Mixin(targets = "mekanism.common.CommonPlayerTickHandler", remap = false)
public abstract class MekanismCommonPlayerTickHandlerMixin {

    @Redirect(method = {
            "onEntityAttacked(Lnet/minecraftforge/event/entity/living/LivingAttackEvent;)V",
            "getFallAbsorptionEnergyInfo(Lnet/minecraft/world/entity/LivingEntity;)Lmekanism/common/CommonPlayerTickHandler$FallEnergyInfo;"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private ItemStack layeredarmor$slot1(net.minecraft.world.entity.LivingEntity entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "mekanism");
    }

    @Redirect(method = {
            "tickEnd(Lnet/minecraft/world/entity/player/Player;)V",
            "onLivingJump(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingJumpEvent;)V",
            "getBreakSpeed(Lnet/minecraftforge/event/entity/player/PlayerEvent$BreakSpeed;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private ItemStack layeredarmor$slot2(net.minecraft.world.entity.player.Player entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "mekanism");
    }

    @Redirect(method = {
            "isScubaMaskOn(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Z",
            "getStepBoost(Lnet/minecraft/world/entity/player/Player;)F",
            "getSwimBoost(Lnet/minecraft/world/entity/player/Player;)F",
            "isGravitationalModulationReady(Lnet/minecraft/world/entity/player/Player;)Z"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private static ItemStack layeredarmor$slot3(net.minecraft.world.entity.player.Player entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "mekanism");
    }
}
