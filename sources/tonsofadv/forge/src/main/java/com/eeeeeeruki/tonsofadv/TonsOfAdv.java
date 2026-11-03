package com.eeeeeeruki.tonsofadv;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** 進捗は data/tonsofadv/advancements にあります。エーテルが入っていると、エーテルのタブの進捗のトーストでエーテルの音が鳴ります。 */
@Mod(TonsOfAdv.MOD_ID)
public class TonsOfAdv {
    public static final String MOD_ID = "tonsofadv";

    public TonsOfAdv() {
        if (ModList.get().isLoaded("aether")) {
            AetherCompat.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
