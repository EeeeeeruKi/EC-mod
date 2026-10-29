package com.eeeeeeruki.callai;

import com.eeeeeeruki.callai.network.CallAINetwork;
import com.eeeeeeruki.callai.network.ScreenshotRequestPacket;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * クライアントにスクリーンショットを頼んで、分割して届いたJPEGを組み立てます。
 */
public final class ScreenshotService {
    private static final long TIMEOUT_SECONDS = 8L;
    private static final int MAX_CHUNKS = 40;
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    private static final Map<Integer, Pending> PENDING = new ConcurrentHashMap<>();

    static final class Pending {
        final UUID player;
        final CompletableFuture<byte[]> future = new CompletableFuture<>();
        byte[][] chunks;
        int received;

        Pending(UUID player) {
            this.player = player;
        }
    }

    private ScreenshotService() {}

    /** 撮れなかった時や、クライアントにこのMODが無い時は null で完了します */
    public static CompletableFuture<byte[]> request(ServerPlayer player) {
        if (!CallAINetwork.clientHasMod(player)) return CompletableFuture.completedFuture(null);
        int id = NEXT_ID.incrementAndGet();
        Pending p = new Pending(player.getUUID());
        PENDING.put(id, p);
        try {
            CallAINetwork.sendTo(player, new ScreenshotRequestPacket(id));
        } catch (Exception e) {
            PENDING.remove(id);
            return CompletableFuture.completedFuture(null);
        }
        return p.future.completeOnTimeout(null, TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((r, e) -> PENDING.remove(id));
    }

    public static void onChunk(ServerPlayer player, int id, int index, int total, byte[] data) {
        Pending p = PENDING.get(id);
        if (p == null || !p.player.equals(player.getUUID())) return;
        if (total <= 0 || total > MAX_CHUNKS || index < 0 || index >= total) {
            p.future.complete(null);
            return;
        }
        synchronized (p) {
            if (p.chunks == null) p.chunks = new byte[total][];
            if (p.chunks.length != total || p.chunks[index] != null) return;
            p.chunks[index] = data;
            p.received++;
            if (p.received < total) return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] c : p.chunks) out.writeBytes(c);
        p.future.complete(out.toByteArray());
    }
}
