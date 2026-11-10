package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mekanism：視覚強化・スキューバマスクなど（クライアント） */
@Pseudo
@Mixin(targets = "mekanism.client.ClientTickHandler", remap = false)
public abstract class MekanismClientTickHandlerMixin {

    @Redirect(method = {
            "setModelVisibility(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/model/HumanoidModel;Z)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private static ItemStack layeredarmor$slot1(net.minecraft.world.entity.LivingEntity entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.client.ClientLayers.displayBySlot(entity, slot);
    }

    @Redirect(method = {
            "updateSelectorRenderer(Lnet/minecraft/world/entity/EquipmentSlot;)Z"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private ItemStack layeredarmor$slot2(net.minecraft.client.player.LocalPlayer entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "mekanism");
    }

    @Redirect(method = {
            "onFog(Lnet/minecraftforge/client/event/ViewportEvent$RenderFog;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private ItemStack layeredarmor$slot3(net.minecraft.world.entity.player.Player entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "mekanism");
    }

    @Redirect(method = {
            "isScubaMaskOn(Lnet/minecraft/world/entity/player/Player;)Z",
            "isVisionEnhancementOn(Lnet/minecraft/world/entity/player/Player;)Z"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private static ItemStack layeredarmor$slot4(net.minecraft.world.entity.player.Player entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "mekanism");
    }
}
