package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mekanism：メカスーツのHUD表示 */
@Pseudo
@Mixin(targets = "mekanism.client.render.HUDRenderer", remap = false)
public abstract class MekanismHUDRendererMixin {

    @Redirect(method = {
            "renderEnergyIcon(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/Predicate;)I",
            "renderMekaSuitModuleIcons(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphics;IIZI)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private ItemStack layeredarmor$slot1(net.minecraft.world.entity.player.Player entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "mekanism");
    }
}
