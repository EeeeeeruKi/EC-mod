package com.eeeeeeruki.minecrafttweak.network;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.server.LongChatServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 長いチャット・コマンドを送る通信（サーバーにこのmodがなくても入れるよう、なくてもいい通信）。 */
public final class LongChatNetwork {
    public static final int MAX = 8000;
    private static boolean registered;

    private LongChatNetwork() {
    }

    /** 長いチャットかコマンド（プレイヤー → サーバー）。 */
    public record Message(boolean command, String text) implements CustomPacketPayload {
        public static final Type<Message> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MinecraftTweak.MODID, "long_chat"));
        public static final StreamCodec<FriendlyByteBuf, Message> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBoolean(p.command);
                    buf.writeUtf(p.text, MAX);
                },
                buf -> new Message(buf.readBoolean(), buf.readUtf(MAX)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(IEventBus modBus) {
        if (registered) {
            return;
        }
        registered = true;
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").optional()
                .playToServer(Message.TYPE, Message.CODEC, (p, ctx) -> {
                    if (ctx.player() instanceof ServerPlayer player) {
                        LongChatServer.onMessage(player, p.command(), p.text());
                    }
                }));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> LongChatServer.onLogout(e.getEntity().getUUID()));
    }

    public static void send(boolean command, String text) {
        PacketDistributor.sendToServer(new Message(command, text));
    }
}
