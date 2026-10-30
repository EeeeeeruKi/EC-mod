package com.eeeeeeruki.mentionmod;

import com.eeeeeeruki.mentionmod.api.AiStatusApi;
import com.eeeeeeruki.mentionmod.network.MentionNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class ServerChatHandler {
    private static final int SNIPPET_SHOWN = 20;
    private static final Pattern GEMINI = Pattern.compile("(?i)(?<![A-Za-z0-9_])@(?:ai|gemini)(?![A-Za-z0-9_])");

    private ServerChatHandler() {}

    public static void onChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        MinecraftServer server = sender.getServer();
        if (server == null) return;

        String raw = event.getRawText();
        ReplyTracker.Pending reply = ReplyTracker.take(sender, raw);
        MentionParser.Result result = MentionParser.parse(raw, sender, server);

        if (result.everyoneDenied()) {
            sender.sendSystemMessage(Component.translatableWithFallback(
                    "mentionmod.everyone_denied", "@everyone はOPのみ使用できます。")
                    .withStyle(ChatFormatting.RED));
        }
        if (!result.changed() && reply == null) return;

        MutableComponent body = result.changed() ? result.message() : Component.literal(raw);
        Map<ServerPlayer, Integer> targets = result.targets();

        if (reply != null) {
            body = replyHeader(reply.target(), reply.snippet()).append(body);
            ServerPlayer replied = server.getPlayerList().getPlayerByName(reply.target());
            if (replied != null && replied != sender) {
                // 名前でメンションもされている時は、そっちの通知を優先する
                targets.merge(replied, MentionParser.KIND_REPLY,
                        (old, now) -> old == MentionParser.KIND_PLAYER ? old : now);
            }
        }

        event.setMessage(body);

        String senderName = sender.getGameProfile().getName();
        for (Map.Entry<ServerPlayer, Integer> e : targets.entrySet()) {
            MentionNetwork.sendMention(e.getKey(), senderName, raw, e.getValue());
        }

        if (result.ai()) askAi(server, sender, raw);
    }

    /** チャットで @ai（@gemini）された時：AI に聞いて、返事をチャットに出す */
    private static void askAi(MinecraftServer server, ServerPlayer sender, String raw) {
        String bot = CallAiBridge.botName();
        String question = GEMINI.matcher(raw).replaceAll("").trim();
        if (question.isEmpty()) question = raw;
        UUID uuid = sender.getUUID();
        String askerName = sender.getGameProfile().getName();

        boolean started = CallAiBridge.ask(sender, question, (text, error) -> {
            if (error != null) {
                MentionNetwork.broadcastAiStatus(server, bot, AiStatusApi.IDLE);
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p != null) {
                    p.sendSystemMessage(Component.literal(bot + "から返事をもらえませんでした: " + error)
                            .withStyle(ChatFormatting.RED));
                }
                return;
            }
            // 「AIが返信中」を少し見せてから、チャットに返事を出す
            MentionNetwork.broadcastAiStatus(server, bot, AiStatusApi.REPLYING);
            CompletableFuture.delayedExecutor(CallAiBridge.replyDelayMs(text), TimeUnit.MILLISECONDS)
                    .execute(() -> server.execute(() -> {
                        MentionNetwork.broadcastAiStatus(server, bot, AiStatusApi.IDLE);
                        MutableComponent line = Component.literal("<").append(
                                        Component.literal(bot).withStyle(ChatFormatting.LIGHT_PURPLE))
                                .append(Component.literal("> "))
                                .append(replyHeader(askerName, raw))
                                .append(Component.literal(text));
                        server.getPlayerList().broadcastSystemMessage(line, false);
                        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                        if (p != null) MentionNetwork.sendMention(p, bot, text, MentionParser.KIND_REPLY);
                    }));
        });
        if (started) MentionNetwork.broadcastAiStatus(server, bot, AiStatusApi.THINKING);
    }

    /** 「↪名前「元のメッセージ」 」の部分。カーソルを合わせると元のメッセージが全部見える */
    static MutableComponent replyHeader(String target, String snippet) {
        String shown = snippet.length() > SNIPPET_SHOWN ? snippet.substring(0, SNIPPET_SHOWN) + "…" : snippet;
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal(target + ": " + snippet));
        return Component.literal("↪").withStyle(s -> s.withColor(ChatFormatting.GRAY).withHoverEvent(hover))
                .append(Component.literal(target).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("「" + shown + "」 ").withStyle(ChatFormatting.GRAY));
    }
}
