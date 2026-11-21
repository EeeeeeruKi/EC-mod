package com.eeeeeeruki.mentionmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/**
 * 「@」キーで、最初から「@」が入った状態のチャットを開く（「/」でコマンドを開くのと同じ感じ）。
 * 初期値は日本語キーボードの「@」キー（英語配列の「[」の位置）。操作設定で変えられる。
 */
public final class MentionKey {
    public static final KeyMapping OPEN = new KeyMapping("key.mentionmod.open_mention",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, "key.categories.mentionmod");

    private MentionKey() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(MentionKey::onRegister);
        MinecraftForge.EVENT_BUS.addListener(MentionKey::onTick);
    }

    private static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    private static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new ChatScreen("@"));
            }
        }
    }
}
