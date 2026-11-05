package com.eeeeeeruki.compacttoasts;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public class CompactToasts implements ClientModInitializer {
    public static final String MODID = "compacttoasts";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean openScreen;

    @Override
    public void onInitializeClient() {
        ToastsConfig.load();
        ClientTickEvents.START_CLIENT_TICK.register(mc -> ToastInstaller.install());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (openScreen) {
                openScreen = false;
                mc.setScreen(new ToastsConfigScreen(null));
            }
        });
        // Mod Menu が無くても /compacttoasts で設定画面を開ける
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("compacttoasts").executes(ctx -> {
                    openScreen = true;
                    return 1;
                })));
    }
}
