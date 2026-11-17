package com.eeeeeeruki.minecrafttweak.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** クライアントだけで動かす初期化。 */
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void init(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (mod, parent) -> new TweakConfigScreen(parent));
    }
}
