package com.eeeeeeruki.tonsofadv.mixin;

import com.eeeeeeruki.tonsofadv.Config;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** エーテルの進捗トーストの音（「音楽」で鳴る）を「ジュークボックス/音ブロック」の音量で鳴らし直します */
@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @ModifyVariable(method = "play", at = @At("HEAD"), argsOnly = true, require = 0)
    private SoundInstance tonsofadv$records(SoundInstance s) {
        if (s == null || s.getSource() != SoundSource.MUSIC || !Config.soundsAsRecords()) return s;
        ResourceLocation id = s.getLocation();
        if ("aether".equals(id.getNamespace()) && id.getPath().startsWith("ui.toast.")) {
            return new SimpleSoundInstance(id, SoundSource.RECORDS, 1.0F, 1.0F,
                    SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
        }
        return s;
    }
}
