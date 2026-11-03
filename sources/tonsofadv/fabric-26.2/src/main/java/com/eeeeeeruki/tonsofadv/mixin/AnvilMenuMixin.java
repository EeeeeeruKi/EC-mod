package com.eeeeeeruki.tonsofadv.mixin;

import com.eeeeeeruki.tonsofadv.TonsOfAdv;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 金床で仕上げたとき */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @Inject(method = "onTake", at = @At("HEAD"), require = 0)
    private void tonsofadv$anvil(Player player, ItemStack carried, CallbackInfo ci) {
        TonsOfAdv.add(player, "anvil_repairs", 1);
    }
}
