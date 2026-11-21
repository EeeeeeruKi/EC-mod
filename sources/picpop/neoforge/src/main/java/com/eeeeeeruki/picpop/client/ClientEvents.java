package com.eeeeeeruki.picpop.client;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;

public final class ClientEvents {
    private ClientEvents() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRenderGui);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientCommands);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, ClientEvents::onChat);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLogout);
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
