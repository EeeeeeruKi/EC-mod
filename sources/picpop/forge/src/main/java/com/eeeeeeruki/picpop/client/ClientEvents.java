package com.eeeeeeruki.picpop.client;

import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

public final class ClientEvents {
    private ClientEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onRenderGui);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onClientCommands);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, ClientEvents::onChat);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onLogout);
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        ImageManager.render(event.getGuiGraphics());
    }

    private static void onClientCommands(RegisterClientCommandsEvent event) {
        ClientImageCommand.register(event.getDispatcher());
    }

    private static void onChat(ClientChatReceivedEvent event) {
        if (event instanceof ClientChatReceivedEvent.System sys && sys.isOverlay()) return;
        Component out = ChatLinks.process(event.getMessage());
        if (out != null) event.setMessage(out);
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ImageManager.reset();
    }
}
