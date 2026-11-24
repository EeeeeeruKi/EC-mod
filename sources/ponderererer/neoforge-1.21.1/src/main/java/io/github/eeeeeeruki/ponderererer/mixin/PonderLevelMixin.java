package io.github.eeeeeeruki.ponderererer.mixin;

import io.github.eeeeeeruki.ponderererer.world.SceneLight;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scene-controlled light (darkness, light sources) for Ponder worlds; see {@link SceneLight}. */
@Mixin(value = PonderLevel.class, remap = false)
public abstract class PonderLevelMixin {
    @Shadow
    int overrideLight;

    @Inject(method = "getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ponderererer$sceneLight(LightLayer layer, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (overrideLight != -1) return; // Ponder is drawing something with its own fake light
        int light = SceneLight.get((PonderLevel) (Object) this, layer, pos);
        if (light >= 0) cir.setReturnValue(light);
    }

    @Inject(method = "restore", at = @At("HEAD"), require = 0)
    private void ponderererer$resetLight(CallbackInfo ci) {
        SceneLight.clear((PonderLevel) (Object) this);
    }
}
