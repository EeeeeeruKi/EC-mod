package net.ramune1256.hijikimod.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ramune1256.hijikimod.HijikiMod;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HijikiMod.MOD_ID);

    public static final RegistryObject<SoundEvent> HIJIKI_VOICE =
            SOUND_EVENTS.register("hijiki_voice", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(HijikiMod.MOD_ID, "hijiki_voice")));

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
