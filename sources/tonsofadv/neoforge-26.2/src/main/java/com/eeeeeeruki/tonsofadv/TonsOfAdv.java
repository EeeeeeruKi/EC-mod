package com.eeeeeeruki.tonsofadv;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** 進捗は data/tonsofadv/advancement にあります（26.2 版）。 */
@Mod(TonsOfAdv.MOD_ID)
public class TonsOfAdv {
    public static final String MOD_ID = "tonsofadv";

    public TonsOfAdv(IEventBus modBus) {
        ActionStats.register(modBus);
    }
}
