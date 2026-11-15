package io.github.eeeeeeruki.ponderererer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Ponderererer - extra rendering effects for Ponder scenes.
 * The common entry point only registers the config; everything else is client-only.
 */
@Mod(Ponderererer.MOD_ID)
public final class Ponderererer {
    public static final String MOD_ID = "ponderererer";

    public Ponderererer(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, FxConfig.SPEC);
    }
}
