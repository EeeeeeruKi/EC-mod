package dev.eeeeeeruki.layeredarmor.mixin.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mekanism：画面のHUD表示 */
@Pseudo
@Mixin(targets = "mekanism.client.render.hud.MekanismHUD", remap = false)
public abstract class MekanismHUDMixin {

    @Redirect(method = {
            "render(Lnet/minecraftforge/client/gui/overlay/ForgeGui;Lnet/minecraft/client/gui/GuiGraphics;FII)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 0, remap = false)
    private ItemStack layeredarmor$slot1(net.minecraft.world.entity.player.Player entity, EquipmentSlot slot) {
        return dev.eeeeeeruki.layeredarmor.Layers.prefer(entity, slot, "mekanism");
    }
}
