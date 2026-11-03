package com.eeeeeeruki.tonsofadv;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** 進捗は data/tonsofadv/advancements にあります。 */
@Mod(TonsOfAdv.MOD_ID)
public class TonsOfAdv {
    public static final String MOD_ID = "tonsofadv";

    public TonsOfAdv() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        if (FMLEnvironment.dist.isClient()) ClientSetup.init();
        ActionStats.register(FMLJavaModLoadingContext.get().getModEventBus());
        if (ModList.get().isLoaded("aether")) {
            AetherCompat.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
