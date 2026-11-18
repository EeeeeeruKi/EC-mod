package dev.eeeeeeruki.layeredarmor.mixin;

import dev.eeeeeeruki.layeredarmor.compat.CapabilityGuard;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/** curios:item の二重登録でクラッシュしないようにする */
@Mixin(value = AttachCapabilitiesEvent.class, remap = false)
public abstract class AttachCapabilitiesEventMixin {
    @Shadow @Final private Map<ResourceLocation, ICapabilityProvider> caps;

    @Shadow public abstract Object getObject();

    @Inject(method = "addCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private void layeredarmor$guard(ResourceLocation key, ICapabilityProvider cap, CallbackInfo ci) {
        if (CapabilityGuard.onAdd(this, getObject(), key, cap, caps)) ci.cancel();
    }
}
