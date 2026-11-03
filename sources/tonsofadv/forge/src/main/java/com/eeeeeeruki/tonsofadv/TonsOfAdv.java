package com.eeeeeeruki.tonsofadv;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** 進捗は data/tonsofadv/advancements にあります。エーテルのタブの進捗では、エーテルの進捗の音を鳴らします。 */
@Mod(TonsOfAdv.MOD_ID)
public class TonsOfAdv {
    public static final String MOD_ID = "tonsofadv";

    public TonsOfAdv() {
        if (ModList.get().isLoaded("aether")) {
            MinecraftForge.EVENT_BUS.addListener(this::onAdvancementEarned);
        }
    }

    private void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = event.getAdvancement().getId();
        String sound = AetherSounds.pick(id.getNamespace(), id.getPath());
        if (sound == null) return;
        SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("aether", sound));
        if (soundEvent != null) {
            player.playNotifySound(soundEvent, SoundSource.MUSIC, 1.0F, 1.0F);
        }
    }
}
