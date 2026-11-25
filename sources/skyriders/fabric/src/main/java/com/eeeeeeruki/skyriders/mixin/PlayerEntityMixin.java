package com.eeeeeeruki.skyriders.mixin;

import com.eeeeeeruki.skyriders.entity.MachineEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** マシンに乗っている間は、Shift（スニーク）で降りないようにする。Shiftはチャージに使う。 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "shouldDismount", at = @At("HEAD"), cancellable = true)
    private void skyriders$keepRiding(CallbackInfoReturnable<Boolean> cir) {
        if (((Entity) (Object) this).getVehicle() instanceof MachineEntity) {
            cir.setReturnValue(false);
        }
    }
}
