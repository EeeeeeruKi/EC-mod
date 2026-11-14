package dev.eeeeeeruki.layeredarmor.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eeeeeeruki.layeredarmor.client.ClientLayers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0)
    private void layeredarmor$capture(PoseStack pose, MultiBufferSource buffer, LivingEntity entity, EquipmentSlot slot,
                                      int light, HumanoidModel<?> model, CallbackInfo ci) {
        ClientLayers.renderEntity = entity;
        ClientLayers.renderSlot = slot;
    }

    @ModifyVariable(method = "renderArmorPiece", at = @At("STORE"), ordinal = 0, require = 0)
    private ItemStack layeredarmor$swap(ItemStack stack) {
        return ClientLayers.displayArmor(ClientLayers.renderEntity, ClientLayers.renderSlot, stack);
    }
}
