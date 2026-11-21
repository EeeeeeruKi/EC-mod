package net.ramune1256.hijikimod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.ramune1256.hijikimod.block.ModBlocks;
import net.ramune1256.hijikimod.item.ModCreativeModeTab;
import net.ramune1256.hijikimod.item.ModItems;
import net.ramune1256.hijikimod.item.ModSoundEvents;

@Mod(HijikiMod.MOD_ID)
public class HijikiMod {
    public static final String MOD_ID = "hijiki";

    public HijikiMod(IEventBus modEventBus) {
        ModCreativeModeTab.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModSoundEvents.register(modEventBus);
        net.ramune1256.hijikimod.entity.ModEntities.register(modEventBus);
    }
}
