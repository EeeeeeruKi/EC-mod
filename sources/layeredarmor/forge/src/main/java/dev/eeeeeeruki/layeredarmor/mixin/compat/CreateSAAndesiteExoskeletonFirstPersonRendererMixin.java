package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Create Stuff 'N Additions：ジェットパック・外骨格の見た目 */
@Pseudo
@Mixin(targets = "net.mcreator.createstuffadditions.client.renderer.exoskeleton.AndesiteExoskeletonFirstPersonRenderer", remap = false)
public abstract class CreateSAAndesiteExoskeletonFirstPersonRendererMixin {

    @Redirect(method = {
            "clientTick()V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private static ItemStack layeredarmor$slot1(net.minecraft.client.player.LocalPlayer entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.client.ClientLayers.displayBySlot(entity, slot);
    }
}
