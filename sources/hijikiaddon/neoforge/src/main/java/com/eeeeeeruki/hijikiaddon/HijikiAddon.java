package com.eeeeeeruki.hijikiaddon;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(HijikiAddon.MOD_ID)
public class HijikiAddon {
    public static final String MOD_ID = "hijikiaddon";

    public HijikiAddon(IEventBus modEventBus) {
        ModItems.register(modEventBus);
    }
}
