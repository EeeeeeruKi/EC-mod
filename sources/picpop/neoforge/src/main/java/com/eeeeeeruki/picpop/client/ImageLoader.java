package com.eeeeeeruki.picpop.client;

import com.eeeeeeruki.picpop.PicPopConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.Component;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** URLから画像をダウンロードして、NativeImage（GIFならコマごと）にします。別スレッドで動きます。 */
public final class ImageLoader {
    private static final int MAX_STILL = 2048;
    private static final int MAX_GIF = 512;
    private static final int MAX_FRAMES = 600;
    private static final long FRAME_BUDGET = 256L * 1024 * 1024;

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "PicPop Loader");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private ImageLoader() {}

    /** 読み込み失敗。理由をゲーム内に表示します。 */
    public static final class LoadException extends RuntimeException {
        public final Component reason;

        LoadException(Component reason) {
            super(reason.getString());
            this.reason = reason;
        }
    }

    /** frames は1枚以上。delays はミリ秒。 */
    public record Decoded(int width, int height, List<NativeImage> frames, int[] delays) {
        void close() {
            for (NativeImage f : frames) f.close();
        }
    }

    public static CompletableFuture<Decoded> load(String url) {
        final int maxMb = PicPopConfig.MAX_FILE_MB.get();
        return CompletableFuture.supplyAsync(() -> decode(download(url, maxMb)), EXEC);
    }

    private static byte[] download(String url, int maxMb) {
        long limit = maxMb * 1024L * 1024L;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0 (PicPop Minecraft mod)")
                    .header("Accept", "image/png,image/jpeg,image/gif,image/bmp,image/*;q=0.8,*/*;q=0.5")
                    .GET().build();
            HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = resp.body()) {
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    throw new LoadException(Component.translatable("picpop.error.http", resp.statusCode()));
                }
                long declared = resp.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (declared > limit) throw new LoadException(Component.translatable("picpop.error.too_large", maxMb));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                long total = 0;
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > limit) throw new LoadException(Component.translatable("picpop.error.too_large", maxMb));
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        } catch (LoadException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LoadException(Component.translatable("picpop.error.network", "interrupted"));
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new LoadException(Component.translatable("picpop.error.network", msg));
        }
    }

    static Decoded decode(byte[] bytes) {
        try {
            if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
                Decoded gif = decodeGif(bytes);
                if (gif != null) return gif;
            }
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) throw new LoadException(Component.translatable("picpop.error.format"));
            int[] size = fit(img.getWidth(), img.getHeight(), MAX_STILL);
            NativeImage ni = toNative(scale(img, size[0], size[1]));
            List<NativeImage> frames = new ArrayList<>();
            frames.add(ni);
            return new Decoded(size[0], size[1], frames, new int[]{0});
        } catch (LoadException e) {
            throw e;
        } catch (Exception | OutOfMemoryError e) {
            throw new LoadException(Component.translatable("picpop.error.format"));
        }
    }

    private static Decoded decodeGif(byte[] bytes) throws IOException {
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
        if (!it.hasNext()) return null;
        ImageReader reader = it.next();
        List<NativeImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(iis, false);
            int sw = 0, sh = 0;
            try {
                IIOMetadata sm = reader.getStreamMetadata();
                if (sm != null) {
                    Node lsd = child(sm.getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
                    if (lsd != null) {
                        sw = intAttr(lsd, "logicalScreenWidth", 0);
                        sh = intAttr(lsd, "logicalScreenHeight", 0);
                    }
                }
            } catch (Exception ignored) {
            }
            BufferedImage canvas = null;
            int[] out = null;
            for (int i = 0; i < MAX_FRAMES; i++) {
                BufferedImage frame;
                IIOMetadata meta;
                try {
                    frame = reader.read(i);
                    meta = reader.getImageMetadata(i);
                } catch (IndexOutOfBoundsException e) {
                    break;
                } catch (Exception e) {
                    if (i == 0) throw new IOException(e);
                    break; // 壊れたコマ以降は捨てる
                }
                int left = 0, top = 0, delay = 10;
                String disposal = "none";
                Node root = meta.getAsTree("javax_imageio_gif_image_1.0");
                Node desc = child(root, "ImageDescriptor");
                if (desc != null) {
                    left = intAttr(desc, "imageLeftPosition", 0);
                    top = intAttr(desc, "imageTopPosition", 0);
                }
                Node gce = child(root, "GraphicControlExtension");
                if (gce != null) {
                    delay = intAttr(gce, "delayTime", 10);
                    Node d = gce.getAttributes().getNamedItem("disposalMethod");
                    if (d != null) disposal = d.getNodeValue();
                }
                if (canvas == null) {
                    if (sw <= 0 || sh <= 0) {
                        sw = left + frame.getWidth();
                        sh = top + frame.getHeight();
                    }
                    canvas = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                    out = fit(sw, sh, MAX_GIF);
                }
                if ((long) out[0] * out[1] * 4L * (frames.size() + 1) > FRAME_BUDGET) break;

                BufferedImage backup = null;
                if ("restoreToPrevious".equals(disposal)) {
                    backup = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D bg = backup.createGraphics();
                    bg.drawImage(canvas, 0, 0, null);
                    bg.dispose();
                }
                Graphics2D g = canvas.createGraphics();
                g.drawImage(frame, left, top, null);
                g.dispose();

                frames.add(toNative(scale(canvas, out[0], out[1])));
                delays.add(delay <= 1 ? 100 : delay * 10); // ブラウザと同じく 0〜1 は 0.1秒にする

                if ("restoreToBackgroundColor".equals(disposal)) {
                    Graphics2D c = canvas.createGraphics();
                    c.setComposite(AlphaComposite.Clear);
                    c.fillRect(left, top, frame.getWidth(), frame.getHeight());
                    c.dispose();
                } else if (backup != null) {
                    canvas = backup;
                }
            }
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            for (NativeImage f : frames) f.close();
            return null;
        } finally {
            reader.dispose();
        }
        if (frames.isEmpty()) return null;
        int[] d = new int[delays.size()];
        for (int i = 0; i < d.length; i++) d[i] = delays.get(i);
        return new Decoded(frames.get(0).getWidth(), frames.get(0).getHeight(), frames, d);
    }

    private static Node child(Node parent, String name) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (name.equals(n.getNodeName())) return n;
        }
        return null;
    }

    private static int intAttr(Node node, String name, int def) {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null) return def;
        Node a = attrs.getNamedItem(name);
        if (a == null) return def;
        try {
            return Integer.parseInt(a.getNodeValue());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int[] fit(int w, int h, int max) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        if (w <= max && h <= max) return new int[]{w, h};
        double s = Math.min((double) max / w, (double) max / h);
        return new int[]{Math.max(1, (int) Math.round(w * s)), Math.max(1, (int) Math.round(h * s))};
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        if (src.getWidth() == w && src.getHeight() == h) return src;
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    private static NativeImage toNative(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        NativeImage ni = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = px[y * w + x];
                int a = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                ni.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r); // NativeImage は ABGR
            }
        }
        return ni;
    }
}
