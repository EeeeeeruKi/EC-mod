package io.github.eeeeeeruki.hyperponder;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientInit {
    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientInit::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new HyperPonderPlugin()));
    }
}
