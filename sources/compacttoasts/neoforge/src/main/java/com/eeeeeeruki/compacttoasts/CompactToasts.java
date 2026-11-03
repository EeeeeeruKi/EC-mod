package com.eeeeeeruki.compacttoasts;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = CompactToasts.MODID, dist = Dist.CLIENT)
public class CompactToasts {
    public static final String MODID = "compacttoasts";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CompactToasts(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ToastsConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (mod, parent) -> new ToastsConfigScreen(parent));
        NeoForge.EVENT_BUS.addListener(CompactToasts::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Pre event) {
        ToastInstaller.install();
    }
}
