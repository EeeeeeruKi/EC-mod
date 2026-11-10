package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** AdvancedAE：量子アーマーの見た目 */
@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.events.AAEClientPlayerEvents", remap = false)
public abstract class AdvancedAEClientPlayerEventsMixin {

    @Redirect(method = {
            "playerRender(Lnet/minecraftforge/client/event/RenderPlayerEvent$Pre;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private static ItemStack layeredarmor$slot1(net.minecraft.world.entity.player.Player entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.client.ClientLayers.displayBySlot(entity, slot);
    }
}
