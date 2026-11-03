package com.eeeeeeruki.tonsofadv.mixin;

import com.eeeeeeruki.tonsofadv.TonsOfAdv;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** かまどから取り出したとき（製錬した数） */
@Mixin(FurnaceResultSlot.class)
public class FurnaceResultSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"), require = 0)
    private void tonsofadv$smelted(Player player, ItemStack carried, CallbackInfo ci) {
        TonsOfAdv.add(player, "smelted_total", Math.max(1, carried.getCount()));
    }
}
