package com.eeeeeeruki.mentionmod;

import com.eeeeeeruki.mentionmod.client.ReplyClient;
import com.eeeeeeruki.mentionmod.network.MentionNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MentionMod.MOD_ID)
public class MentionMod {
    public static final String MOD_ID = "mentionmod";

    public MentionMod() {
        MentionNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(ServerChatHandler::onChat);
        MinecraftForge.EVENT_BUS.addListener(MentionMod::onLogout);
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
