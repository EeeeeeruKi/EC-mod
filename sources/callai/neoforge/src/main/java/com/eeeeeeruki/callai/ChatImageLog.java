package com.eeeeeeruki.callai;

import net.minecraft.server.level.ServerPlayer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * チャットに貼られた画像を覚えておき、AIに話しかけられたときに一緒に見せる。
 * ・minEcraft twEEk の「チャットの画像」でファイルとして貼られた画像（twEEk がサーバーに入っているとき）
 * ・チャットに書かれた画像のリンク（http / https）
 * 話しかけたメッセージに入っている画像のリンクと、直近に貼られた画像（その人がまだAIに見せていないもの）を最大3枚まで送る。
 */
public final class ChatImageLog {
    /** 覚えておく画像の数 */
    private static final int KEEP = 20;
    /** 1回に送る画像の数 */
    private static final int MAX_SEND = 3;
    /** リンクから取ってくる画像の大きさの上限 */
    private static final int MAX_DOWNLOAD = 8 * 1024 * 1024;
    /** Geminiに送る前に縮める大きさ（長い辺） */
    private static final int MAX_SIDE = 1024;

    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"]+", Pattern.CASE_INSENSITIVE);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** tweakId か url のどちらかが入る */
    public record Item(String tweakId, String url, String sender, long time) {
        String key() {
            return tweakId != null ? "tweak:" + tweakId : "url:" + url;
        }
    }

    /** 送る画像（JPEG）と、その説明 */
    public record Picture(byte[] jpeg, String note) {}

    private static final Deque<Item> RECENT = new ArrayDeque<>();
    private static boolean hooked;
    private static Method tweakGet;

    private ChatImageLog() {}

    /** minEcraft twEEk が入っていれば、貼られた画像を教えてもらうようにする（何回呼んでも1回だけ） */
    public static synchronized void hookTweak() {
        if (hooked) return;
        hooked = true;
        try {
            Class<?> c = Class.forName("com.eeeeeeruki.minecrafttweak.server.ChatImageServer");
            Method add = c.getMethod("addListener", BiConsumer.class);
            tweakGet = c.getMethod("getImage", String.class);
            BiConsumer<ServerPlayer, String> listener = (player, id) ->
                    add(new Item(id, null, player.getGameProfile().getName(), System.currentTimeMillis()));
            add.invoke(null, listener);
            CallAI.LOGGER.info("Call AI: minEcraft twEEk のチャットの画像と連携しました");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            tweakGet = null; // twEEk が無いか、古い（1.16.0 以前）
        } catch (Throwable t) {
            tweakGet = null;
            CallAI.LOGGER.warn("Call AI: minEcraft twEEk との連携に失敗しました: {}", t.toString());
        }
    }

    public static boolean tweakLinked() {
        return tweakGet != null;
    }

    /** チャットのメッセージから画像のリンクを拾って覚える（サーバーのスレッドで呼ぶ） */
    public static void onChat(ServerPlayer sender, String raw) {
        if (GeminiConfig.CHAT_IMAGE_MINUTES.get() <= 0 || raw == null) return;
        for (String url : urls(raw)) {
            add(new Item(null, url, sender.getGameProfile().getName(), System.currentTimeMillis()));
        }
    }

    public static synchronized void clear() {
        RECENT.clear();
    }

    private static synchronized void add(Item item) {
        RECENT.removeIf(i -> i.key().equals(item.key()));
        RECENT.addLast(item);
        while (RECENT.size() > KEEP) RECENT.removeFirst();
    }

    private static List<String> urls(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = URL.matcher(text);
        while (m.find()) {
            String u = m.group();
            // 文の終わりの記号は外す
            while (!u.isEmpty() && ".,)]}!?、。」』）".indexOf(u.charAt(u.length() - 1)) >= 0) u = u.substring(0, u.length() - 1);
            if (u.length() > 8 && !out.contains(u)) out.add(u);
        }
        return out;
    }

    /**
     * AIに見せる画像を選んで、JPEGにして返す（サーバーのスレッドで呼ぶ）。
     * message に入っているリンク → 直近に貼られた画像（seen に入っていないもの）の順に、最大3枚。
     * 選んだ画像は seen に入れるので、同じ人には同じ画像を2回送らない。
     */
    public static CompletableFuture<List<Picture>> pick(String message, Set<String> seen) {
        int minutes = GeminiConfig.CHAT_IMAGE_MINUTES.get();
        if (minutes <= 0) return CompletableFuture.completedFuture(List.of());

        List<Item> chosen = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (String url : urls(message == null ? "" : message)) {
            if (chosen.size() >= MAX_SEND) break;
            chosen.add(new Item(null, url, null, now));
        }
        synchronized (ChatImageLog.class) {
            Iterator<Item> it = RECENT.descendingIterator();
            while (it.hasNext() && chosen.size() < MAX_SEND) {
                Item i = it.next();
                if (now - i.time() > minutes * 60_000L) break;
                if (seen.contains(i.key())) continue;
                if (chosen.stream().anyMatch(c -> c.key().equals(i.key()))) continue;
                chosen.add(i);
            }
        }
        for (Item i : chosen) seen.add(i.key());
        if (chosen.isEmpty()) return CompletableFuture.completedFuture(List.of());

        List<CompletableFuture<Picture>> parts = new ArrayList<>();
        for (Item i : chosen) {
            String who = i.sender() == null ? "このメッセージに書かれたリンクの画像" : i.sender() + "さんがチャットに貼った画像";
            if (i.tweakId() != null) {
                byte[] raw = tweakBytes(i.tweakId());
                parts.add(raw == null ? CompletableFuture.completedFuture(null)
                        : CompletableFuture.supplyAsync(() -> toPicture(raw, who)));
            } else {
                parts.add(download(i.url()).thenApply(raw -> raw == null ? null : toPicture(raw, who)));
            }
        }
        return CompletableFuture.allOf(parts.toArray(new CompletableFuture[0])).handle((v, e) -> {
            List<Picture> out = new ArrayList<>();
            for (CompletableFuture<Picture> f : parts) {
                if (f.isCompletedExceptionally()) continue;
                Picture p = f.getNow(null);
                if (p != null) out.add(p);
            }
            return out;
        });
    }

    private static byte[] tweakBytes(String id) {
        if (tweakGet == null) return null;
        try {
            Object o = tweakGet.invoke(null, id);
            return o instanceof byte[] b ? b : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** リンクから画像を取ってくる。画像でない・大きすぎる・家の中のネットワークを指している、のどれかなら null */
    private static CompletableFuture<byte[]> download(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = URI.create(url);
                if (!safeHost(uri.getHost())) return null;
                HttpRequest req = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "Mozilla/5.0 (Call AI Minecraft mod)")
                        .GET().build();
                HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream in = res.body()) {
                    if (res.statusCode() != 200) return null;
                    if (!safeHost(res.uri().getHost())) return null; // 転送された先も確かめる
                    String type = res.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
                    if (!type.isEmpty() && !type.startsWith("image/") && !type.startsWith("application/octet-stream")) return null;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        if (out.size() > MAX_DOWNLOAD) return null;
                    }
                    return out.toByteArray();
                }
            } catch (Exception e) {
                return null;
            }
        });
    }

    /** 自分のPCや家のネットワーク（ルーターなど）には取りに行かない */
    private static boolean safeHost(String host) {
        if (host == null || host.isBlank()) return false;
        try {
            for (InetAddress a : InetAddress.getAllByName(host)) {
                if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()
                        || a.isAnyLocalAddress() || a.isMulticastAddress()) return false;
                byte[] b = a.getAddress();
                // IPv6 のユニークローカル（fc00::/7）と、100.64.0.0/10
                if (b.length == 16 && (b[0] & 0xFE) == 0xFC) return false;
                if (b.length == 4 && (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** PNG・JPEG・GIF（最初のコマ）・BMP を、縮めたJPEGにする。読めなければ null */
    private static Picture toPicture(byte[] raw, String note) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
            if (src == null) return null;
            int w = src.getWidth();
            int h = src.getHeight();
            double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(w, h));
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            BufferedImage img = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, nw, nh);
            g.drawImage(src, 0, 0, nw, nh, null);
            g.dispose();

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) return null;
            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.8F);
                writer.write(null, new IIOImage(img, null, null), param);
            } finally {
                writer.dispose();
            }
            return new Picture(bytes.toByteArray(), note);
        } catch (Throwable t) {
            return null;
        }
    }
}
