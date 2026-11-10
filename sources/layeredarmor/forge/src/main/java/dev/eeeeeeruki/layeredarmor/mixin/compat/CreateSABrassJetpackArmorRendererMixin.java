package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Create Stuff 'N Additions：ジェットパック・外骨格の見た目 */
@Pseudo
@Mixin(targets = "net.mcreator.createstuffadditions.client.renderer.jetpack.BrassJetpackArmorRenderer", remap = false)
public abstract class CreateSABrassJetpackArmorRendererMixin {

    @Redirect(method = {
            "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private ItemStack layeredarmor$slot1(net.minecraft.world.entity.LivingEntity entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.client.ClientLayers.displayBySlot(entity, slot);
    }
}
