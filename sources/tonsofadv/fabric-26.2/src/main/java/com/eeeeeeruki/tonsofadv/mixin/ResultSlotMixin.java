package com.eeeeeeruki.tonsofadv.mixin;

import com.eeeeeeruki.tonsofadv.TonsOfAdv;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 作業台などで完成品を取り出したとき（クラフトした回数） */
@Mixin(ResultSlot.class)
public class ResultSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"), require = 0)
    private void tonsofadv$crafted(Player player, ItemStack carried, CallbackInfo ci) {
        TonsOfAdv.add(player, "crafted_total", 1);
    }
}
