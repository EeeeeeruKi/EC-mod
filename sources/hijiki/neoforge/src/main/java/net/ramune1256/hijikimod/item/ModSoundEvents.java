package net.ramune1256.hijikimod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ramune1256.hijikimod.HijikiMod;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, HijikiMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HIJIKI_VOICE =
            SOUND_EVENTS.register("hijiki_voice", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(HijikiMod.MOD_ID, "hijiki_voice")));

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
