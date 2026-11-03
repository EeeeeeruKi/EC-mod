package com.eeeeeeruki.tonsofadv;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

/** 進捗は data/tonsofadv/advancement にあります。エーテルが入っていると、エーテルのタブの進捗のトーストでエーテルの音が鳴ります。 */
@Mod(TonsOfAdv.MOD_ID)
public class TonsOfAdv {
    public static final String MOD_ID = "tonsofadv";

    public TonsOfAdv(IEventBus modBus) {
        if (ModList.get().isLoaded("aether")) {
            AetherCompat.register(modBus);
        }
    }
}
