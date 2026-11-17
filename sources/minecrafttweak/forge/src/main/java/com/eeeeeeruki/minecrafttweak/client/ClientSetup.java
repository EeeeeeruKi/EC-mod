package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.client.code.ModCodes;
import com.eeeeeeruki.minecrafttweak.client.code.VanillaCodes;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** クライアントだけで動かす初期化。 */
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void init() {
        TextSettings.registerProvider(new VanillaCodes());
        TextSettings.registerProvider(ModCodes.sodium());
        TextSettings.registerProvider(ModCodes.sodiumExtra());
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new TweakConfigScreen(parent)));
    }
}
