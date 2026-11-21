package com.eeeeeeruki.minecrafttweak.server;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.feature.client.ChatImageFeature;
import com.eeeeeeruki.minecrafttweak.network.ChatImageNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * チャットの画像のサーバー側。送られてきた画像ファイルを受け取って覚えておき、「[画像]」付きのメッセージを全員に流す。
 * 見る人から「この画像をください」と言われたら、覚えている画像を送る。
 * どれもサーバーのメインスレッドで動く。
 */
public final class ChatImageServer {
    /** 覚えておく画像の合計の大きさ（古いものから忘れる）。 */
    private static final long MAX_STORE = 64L * 1024 * 1024;

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final LinkedHashMap<String, byte[]> STORE = new LinkedHashMap<>(16, 0.75f, true);
    private static long storeBytes;
    /** 画像がチャットに貼られたときに呼ばれる（Call AI など、ほかのmodとの連携用）。 */
    private static final java.util.List<java.util.function.BiConsumer<ServerPlayer, String>> LISTENERS =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private static final class Pending {
        final int total;
        final String caption;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int next;

        Pending(int total, String caption) {
            this.total = total;
            this.caption = caption;
        }
    }

    private ChatImageServer() {
    }

    private static boolean enabled() {
        return ChatImageFeature.INSTANCE != null && ChatImageFeature.INSTANCE.isEnabled();
    }

    private static int maxBytes() {
        int kb = 1024;
        try {
            kb = ChatImageFeature.INSTANCE.maxUploadKb.get();
        } catch (Exception ignored) {
        }
        return kb * 1024;
    }

    public static void onUpload(ServerPlayer player, int index, int total, String caption, byte[] data) {
        UUID uuid = player.getUUID();
        if (!enabled()) {
            if (index == 0) {
                player.sendSystemMessage(Component.translatable("minecrafttweak.chat_images.server_off").withStyle(ChatFormatting.RED));
            }
            return;
        }
        int max = maxBytes();
        if (index == 0) {
            PENDING.remove(uuid);
            if (total <= 0 || (long) (total - 1) * ChatImageNetwork.UPLOAD_CHUNK > max) {
                tooBig(player, max);
                return;
            }
            PENDING.put(uuid, new Pending(total, caption == null ? "" : caption));
        }
        Pending p = PENDING.get(uuid);
        if (p == null || index != p.next || total != p.total) {
            PENDING.remove(uuid);
            return;
        }
        p.out.write(data, 0, data.length);
        p.next++;
        if (p.out.size() > max) {
            PENDING.remove(uuid);
            tooBig(player, max);
            return;
        }
        if (p.next == p.total) {
            PENDING.remove(uuid);
            finish(player, p.caption, p.out.toByteArray());
        }
    }

    private static void tooBig(ServerPlayer player, int max) {
        player.sendSystemMessage(Component.translatable("minecrafttweak.chat_images.server_too_big", max / 1024).withStyle(ChatFormatting.RED));
    }

    private static void finish(ServerPlayer player, String caption, byte[] bytes) {
        if (!ChatImageFormat.supported(bytes)) {
            player.sendSystemMessage(Component.translatable("minecrafttweak.chat_images.error.not_image").withStyle(ChatFormatting.RED));
            return;
        }
        String id = ChatImageFormat.id(bytes);
        store(id, bytes);
        String text = caption.strip();
        if (text.length() > 256) {
            text = text.substring(0, 256);
        }
        MutableComponent body = text.isEmpty() ? Component.empty() : Component.literal(text + " ");
        body.append(imageTag(id));
        Component message = Component.translatable("chat.type.text", player.getDisplayName(), body);
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(message, false);
        }
        for (java.util.function.BiConsumer<ServerPlayer, String> l : LISTENERS) {
            try {
                l.accept(player, id);
            } catch (Throwable t) {
                MinecraftTweak.LOGGER.warn("minEcraft twEEk: chat image listener failed: {}", t.toString());
            }
        }
        MinecraftTweak.LOGGER.info("minEcraft twEEk: {} sent a chat image {} ({} bytes)", player.getName().getString(), id, bytes.length);
    }

    /** チャットに入れる「[画像]」。このmodが入っていない人には、ただの文字に見える。 */
    public static Component imageTag(String id) {
        return Component.literal("[画像]").withStyle(s -> s.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ChatImageFormat.TAG_PREFIX + id))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("minEcraft twEEk image"))));
    }

    private static void store(String id, byte[] bytes) {
        byte[] old = STORE.put(id, bytes);
        if (old != null) {
            storeBytes -= old.length;
        }
        storeBytes += bytes.length;
        Iterator<Map.Entry<String, byte[]>> it = STORE.entrySet().iterator();
        while (storeBytes > MAX_STORE && it.hasNext()) {
            Map.Entry<String, byte[]> e = it.next();
            if (e.getKey().equals(id)) {
                continue;
            }
            storeBytes -= e.getValue().length;
            it.remove();
        }
    }

    /**
     * ほかのmodとの連携用。画像がチャットに貼られるたびに (送った人, 画像の番号) で呼ばれる。サーバーのメインスレッドで呼ばれる。
     */
    public static void addListener(java.util.function.BiConsumer<ServerPlayer, String> listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    /** ほかのmodとの連携用。覚えている画像ファイルの中身（もう忘れていたら null）。サーバーのメインスレッドで呼ぶ。 */
    public static byte[] getImage(String id) {
        return id == null ? null : STORE.get(id);
    }

    public static void onRequest(ServerPlayer player, String id) {
        if (!ChatImageFormat.validId(id)) {
            return;
        }
        byte[] bytes = STORE.get(id);
        if (bytes == null) {
            ChatImageNetwork.sendData(player, id, 0, 0, new byte[0]);
            return;
        }
        int chunk = ChatImageNetwork.DATA_CHUNK;
        int total = Math.max(1, (bytes.length + chunk - 1) / chunk);
        for (int i = 0; i < total; i++) {
            int from = i * chunk;
            int to = Math.min(bytes.length, from + chunk);
            ChatImageNetwork.sendData(player, id, i, total, Arrays.copyOfRange(bytes, from, to));
        }
    }

    public static void onLogout(UUID uuid) {
        PENDING.remove(uuid);
    }
}
