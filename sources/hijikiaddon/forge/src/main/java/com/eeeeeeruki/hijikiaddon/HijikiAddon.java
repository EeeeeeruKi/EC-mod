package com.eeeeeeruki.hijikiaddon;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HijikiAddon.MOD_ID)
public class HijikiAddon {
    public static final String MOD_ID = "hijikiaddon";

    public HijikiAddon() {
        ModItems.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
