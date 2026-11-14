package dev.eeeeeeruki.layeredarmor.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eeeeeeruki.layeredarmor.client.ClientLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraLayer.class)
public abstract class ElytraLayerMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"))
    private void layeredarmor$capture(PoseStack pose, MultiBufferSource buffer, int light, LivingEntity entity,
                                      float a, float b, float c, float d, float e, float f, CallbackInfo ci) {
        ClientLayers.renderEntity = entity;
        ClientLayers.renderSlot = EquipmentSlot.CHEST;
    }

    @ModifyVariable(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("STORE"), ordinal = 0, require = 0)
    private ItemStack layeredarmor$swap(ItemStack stack) {
        return ClientLayers.displayElytra(ClientLayers.renderEntity, stack);
    }
}
