package com.eeeeeeruki.mentionmod;

import com.eeeeeeruki.mentionmod.client.ReplyClient;
import com.eeeeeeruki.mentionmod.network.MentionNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(MentionMod.MOD_ID)
public class MentionMod {
    public static final String MOD_ID = "mentionmod";

    public MentionMod(IEventBus modBus) {
        modBus.addListener(MentionNetwork::register);
        NeoForge.EVENT_BUS.addListener(ServerChatHandler::onChat);
        NeoForge.EVENT_BUS.addListener(MentionMod::onLogout);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ReplyClient.init();
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MentionNetwork.broadcastReplyStatus(sp.getServer(), sp.getUUID(), false);
        }
    }
}
