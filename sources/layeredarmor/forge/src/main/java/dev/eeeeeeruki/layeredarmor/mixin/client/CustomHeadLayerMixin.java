package dev.eeeeeeruki.layeredarmor.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eeeeeeruki.layeredarmor.client.ClientLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 頭の重ね着スロットのカボチャ・頭ブロックも表示する */
@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"))
    private void layeredarmor$capture(PoseStack pose, MultiBufferSource buffer, int light, LivingEntity entity,
                                      float a, float b, float c, float d, float e, float f, CallbackInfo ci) {
        ClientLayers.renderEntity = entity;
    }

    @ModifyVariable(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("STORE"), ordinal = 0, require = 0)
    private ItemStack layeredarmor$swap(ItemStack stack) {
        return ClientLayers.display(ClientLayers.renderEntity, EquipmentSlot.HEAD, stack);
    }
}
