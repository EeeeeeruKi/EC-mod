package com.eeeeeeruki.tonsofadv.mixin;

import com.eeeeeeruki.tonsofadv.TonsOfAdv;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 経験値をもらったとき */
@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "giveExperiencePoints", at = @At("HEAD"), require = 0)
    private void tonsofadv$xp(int amount, CallbackInfo ci) {
        TonsOfAdv.add((Player) (Object) this, "xp_gained", amount);
    }
}
