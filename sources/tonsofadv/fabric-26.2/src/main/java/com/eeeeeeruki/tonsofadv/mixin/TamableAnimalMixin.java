package com.eeeeeeruki.tonsofadv.mixin;

import com.eeeeeeruki.tonsofadv.TonsOfAdv;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** オオカミ・ネコなどを手なずけたとき */
@Mixin(TamableAnimal.class)
public class TamableAnimalMixin {
    @Inject(method = "tame", at = @At("HEAD"), require = 0)
    private void tonsofadv$tame(Player player, CallbackInfo ci) {
        TonsOfAdv.add(player, "animals_tamed", 1);
    }
}
