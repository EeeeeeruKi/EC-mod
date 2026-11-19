package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.client.code.ModCodes;
import com.eeeeeeruki.minecrafttweak.client.code.VanillaCodes;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.Features;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** クライアントだけで動かす初期化。 */
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void init(ModContainer container, IEventBus modBus) {
        for (Feature feature : Features.all()) {
            feature.registerClient(modBus);
        }
        TextSettings.registerProvider(new VanillaCodes());
        TextSettings.registerProvider(ModCodes.sodium());
        TextSettings.registerProvider(ModCodes.sodiumExtra());
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (mod, parent) -> new TweakConfigScreen(parent));
    }
}
