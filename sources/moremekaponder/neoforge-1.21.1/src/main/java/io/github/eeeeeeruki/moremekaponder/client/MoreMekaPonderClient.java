package io.github.eeeeeeruki.moremekaponder.client;

import io.github.eeeeeeruki.moremekaponder.MoreMekaPonder;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = MoreMekaPonder.MOD_ID, dist = Dist.CLIENT)
public final class MoreMekaPonderClient {
    public MoreMekaPonderClient(IEventBus modBus) {
        PonderIndex.addPlugin(new MmpPlugin());
    }
}
