package dev.eeeeeeruki.layeredarmor.mixin;

import dev.eeeeeeruki.layeredarmor.compat.CapabilityGuard;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Curios 本体がアイテムに機能を付けている最中かどうかを記録する */
@Pseudo
@Mixin(targets = "top.theillusivec4.curios.common.event.CuriosEventHandler", remap = false)
public abstract class CuriosEventHandlerMixin {
    @Inject(method = "attachStackCapabilities", at = @At("HEAD"), remap = false)
    private void layeredarmor$enter(AttachCapabilitiesEvent<ItemStack> event, CallbackInfo ci) {
        CapabilityGuard.IN_CURIOS.set(true);
    }

    @Inject(method = "attachStackCapabilities", at = @At("RETURN"), remap = false)
    private void layeredarmor$exit(AttachCapabilitiesEvent<ItemStack> event, CallbackInfo ci) {
        CapabilityGuard.IN_CURIOS.set(false);
    }
}
