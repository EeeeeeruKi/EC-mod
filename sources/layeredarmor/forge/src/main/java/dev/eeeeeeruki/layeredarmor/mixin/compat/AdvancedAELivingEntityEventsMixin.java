package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** AdvancedAE：量子アーマーの機能（無敵・水中呼吸・ジャンプ・落下など） */
@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.events.AAELivingEntityEvents", remap = false)
public abstract class AdvancedAELivingEntityEventsMixin {

    @Redirect(method = {
            "livingFallDamage(Lnet/minecraftforge/event/entity/living/LivingFallEvent;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private static ItemStack layeredarmor$slot1(net.minecraft.server.level.ServerPlayer entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "advanced_ae");
    }

    @Redirect(method = {
            "checkInvulnerability(Lnet/minecraftforge/event/entity/living/LivingAttackEvent;)V",
            "breath(Lnet/minecraftforge/event/entity/living/LivingBreatheEvent;)V",
            "jumpEvent(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingJumpEvent;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private static ItemStack layeredarmor$slot2(net.minecraft.world.entity.player.Player entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "advanced_ae");
    }
}
