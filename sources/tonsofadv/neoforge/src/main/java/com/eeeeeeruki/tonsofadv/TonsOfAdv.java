package com.eeeeeeruki.tonsofadv;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

/** 進捗は data/tonsofadv/advancement にあります。 */
@Mod(TonsOfAdv.MOD_ID)
public class TonsOfAdv {
    public static final String MOD_ID = "tonsofadv";

    public TonsOfAdv(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        if (FMLEnvironment.dist.isClient()) ClientSetup.init(container);
        ActionStats.register(modBus);
        if (ModList.get().isLoaded("aether")) {
            AetherCompat.register(modBus);
        }
    }
}
