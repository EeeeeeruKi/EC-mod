package com.eeeeeeruki.minecrafttweak.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** クライアントだけで動かす初期化。 */
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void init() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new TweakConfigScreen(parent)));
    }
}
