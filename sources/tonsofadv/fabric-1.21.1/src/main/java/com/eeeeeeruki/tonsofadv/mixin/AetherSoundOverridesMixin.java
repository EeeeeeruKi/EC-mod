package com.eeeeeeruki.tonsofadv.mixin;

import com.eeeeeeruki.tonsofadv.TonsOfAdv;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * エーテルがトーストの音を選ぶところで、このMODのエーテルのタブの進捗にエーテルの音を返します。
 * エーテルが入っていないときは何もしません。
 */
@Pseudo
@Mixin(targets = "com.aetherteam.aether.api.AetherAdvancementSoundOverrides")
public class AetherSoundOverridesMixin {
    @Inject(method = "retrieveOverride", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void tonsofadv$sound(AdvancementHolder holder, CallbackInfoReturnable<SoundEvent> cir) {
        String sound = TonsOfAdv.aetherSound(holder.id());
        if (sound != null) {
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("aether", sound));
            if (event != null) cir.setReturnValue(event);
        }
    }
}
