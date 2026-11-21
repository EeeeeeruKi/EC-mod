package com.eeeeeeruki.picpop;

import com.eeeeeeruki.picpop.network.PicPopNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(PicPop.MODID)
public class PicPop {
    public static final String MODID = "picpop";

    public PicPop(IEventBus modBus, ModContainer container) {
        modBus.addListener(PicPopNetwork::register);
        container.registerConfig(ModConfig.Type.CLIENT, PicPopConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.eeeeeeruki.picpop.client.ClientEvents.init();
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ImageCommand.register(event.getDispatcher());
    }
}
