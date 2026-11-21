package com.eeeeeeruki.minecrafttweak.network;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.server.LongChatServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** 長いチャット・コマンドを送る通信（サーバーにこのmodがなくても入れるよう、どの版でも受け入れる）。 */
public final class LongChatNetwork {
    public static final int MAX = 8000;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MinecraftTweak.MODID, "long_chat"), () -> "1", v -> true, v -> true);
    private static boolean registered;

    private LongChatNetwork() {
    }

    /** 長いチャットかコマンド（プレイヤー → サーバー）。 */
    public record Message(boolean command, String text) {
        void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(command);
            buf.writeUtf(text, MAX);
        }

        static Message decode(FriendlyByteBuf buf) {
            return new Message(buf.readBoolean(), buf.readUtf(MAX));
        }
    }

    public static void register(IEventBus modBus) {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.messageBuilder(Message.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Message::encode).decoder(Message::decode)
                .consumerMainThread((Message p, Supplier<NetworkEvent.Context> ctx) -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null) {
                        LongChatServer.onMessage(player, p.command(), p.text());
                    }
                }).add();
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> LongChatServer.onLogout(e.getEntity().getUUID()));
    }

    public static void send(boolean command, String text) {
        CHANNEL.sendToServer(new Message(command, text));
    }
}
