package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.client.code.ModCodes;
import com.eeeeeeruki.minecrafttweak.client.code.VanillaCodes;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.Features;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;

/** クライアントだけで動かす初期化。 */
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void init(IEventBus modBus) {
        for (Feature feature : Features.all()) {
            feature.registerClient(modBus);
        }
        TextSettings.registerProvider(new VanillaCodes());
        TextSettings.registerProvider(ModCodes.sodium());
        TextSettings.registerProvider(ModCodes.sodiumExtra());
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new TweakConfigScreen(parent)));
    }
}
