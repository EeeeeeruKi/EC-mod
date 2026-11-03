package com.eeeeeeruki.tonsofadv;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/** 進捗は data/tonsofadv/advancement にあります。エーテルのタブの進捗では、エーテルの進捗の音を鳴らします。 */
@Mod(TonsOfAdv.MOD_ID)
public class TonsOfAdv {
    public static final String MOD_ID = "tonsofadv";

    public TonsOfAdv(IEventBus modBus) {
        if (ModList.get().isLoaded("aether")) {
            NeoForge.EVENT_BUS.addListener(this::onAdvancementEarned);
        }
    }

    private void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = event.getAdvancement().id();
        String sound = AetherSounds.pick(id.getNamespace(), id.getPath());
        if (sound == null) return;
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("aether", sound));
        if (soundEvent != null) {
            player.playNotifySound(soundEvent, SoundSource.MUSIC, 1.0F, 1.0F);
        }
    }
}
