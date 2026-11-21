package com.eeeeeeruki.picpop;

import com.eeeeeeruki.picpop.network.PicPopNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(PicPop.MODID)
public class PicPop {
    public static final String MODID = "picpop";

    public PicPop() {
        PicPopNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PicPopConfig.SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.eeeeeeruki.picpop.client.ClientEvents::init);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ImageCommand.register(event.getDispatcher());
    }
}
