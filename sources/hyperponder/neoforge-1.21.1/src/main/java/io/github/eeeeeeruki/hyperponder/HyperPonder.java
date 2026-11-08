package io.github.eeeeeeruki.hyperponder;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.createmod.ponder.foundation.PonderIndex;

@Mod(value = "hyperponder", dist = Dist.CLIENT)
public class HyperPonder {
    public HyperPonder(IEventBus modBus) {
        modBus.addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new HyperPonderPlugin()));
    }
}
