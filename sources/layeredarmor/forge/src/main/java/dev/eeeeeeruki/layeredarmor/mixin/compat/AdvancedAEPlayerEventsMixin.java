package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** AdvancedAE：量子アーマーの機能（飛行・採掘速度など） */
@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.events.AAEPlayerEvents", remap = false)
public abstract class AdvancedAEPlayerEventsMixin {

    @Redirect(method = {
            "breakSpeed(Lnet/minecraftforge/event/entity/player/PlayerEvent$BreakSpeed;)V",
            "playerTickStart(Lnet/minecraftforge/event/TickEvent$PlayerTickEvent;)V",
            "playerTickEnd(Lnet/minecraftforge/event/TickEvent$PlayerTickEvent;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private static ItemStack layeredarmor$slot1(net.minecraft.world.entity.player.Player entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "advanced_ae");
    }
}
