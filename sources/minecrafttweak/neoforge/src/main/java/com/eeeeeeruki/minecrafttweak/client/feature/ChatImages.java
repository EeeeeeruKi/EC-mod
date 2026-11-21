package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.client.ChatAttachScreen;
import com.eeeeeeruki.minecrafttweak.feature.client.ChatImageFeature;
import com.eeeeeeruki.minecrafttweak.network.ChatImageNetwork;
import com.eeeeeeruki.minecrafttweak.server.ChatImageFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * チャットの画像（クライアント側）。
 *
 * - チャット画面のいちばん左に「＋」ボタンを足す（入力欄は少し右にずらす）。
 * - チャットに画像のリンクか「[画像]」があったら、そのメッセージの下に画像の行を足す（ChatComponentMixin）。
 *   画像の行は、チャットの1行ぶんの高さに切った画像を順に描く。GIF は時間で絵を切り替える。
 * - 読み込みは別のスレッドでやり、終わったらチャットを並べ直して正しい高さにする。
 */
public final class ChatImages {
    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final String[] IMAGE_EXT = {".png", ".jpg", ".jpeg", ".gif", ".bmp"};
    private static final int MAX_DOWNLOAD = 8 * 1024 * 1024;
    private static final int MAX_CACHE = 320;
    private static final int MAX_PER_MESSAGE = 3;
    public static final String[] FILE_PATTERNS = {"*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"};

    private static final LinkedHashMap<String, Img> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<String, ByteArrayOutputStream> INCOMING = new HashMap<>();
    private static final Set<String> REQUESTED = new java.util.HashSet<>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "minEcraft twEEk chat images");
        t.setDaemon(true);
        return t;
    });
    private static HttpClient http;
    private static int textureCounter;
    private static boolean refreshQueued;
    private static boolean registered;

    /** 送る前の添付ファイル。 */
    private static Path pendingFile;
    /** 添付の画面で入れた画像のリンク（送るときに読み込んで、ファイルと同じようにサーバーへ送る）。 */
    private static String pendingUrl;
    /** 添付の画面に行くときだけ、チャットを閉じても添付を消さない。 */
    private static boolean keepPending;

    public enum LoadState { LOADING, READY, FAILED }

    /** 1つの画像（読み込み中・読み込み済み・失敗）。 */
    public static final class Img {
        final String key;
        volatile LoadState state = LoadState.LOADING;
        String reasonKey = "";
        Object[] reasonArgs = new Object[0];
        int width;
        int height;
        ResourceLocation[] frames = new ResourceLocation[0];
        int[] delays = new int[0];
        int totalDelay;

        Img(String key) {
            this.key = key;
        }

        ResourceLocation frameAt(long millis) {
            if (frames.length == 1 || totalDelay <= 0) {
                return frames[0];
            }
            long t = millis % totalDelay;
            for (int i = 0; i < frames.length; i++) {
                t -= delays[i];
                if (t < 0) {
                    return frames[i];
                }
            }
            return frames[frames.length - 1];
        }
    }

    /** チャットの1行ぶんの画像。文字としては何も出さない（描くのは draw）。 */
    public static final class ImageLine implements FormattedCharSequence {
        final Img img;
        final int row;
        final int dispWidth;
        final int dispHeight;
        final int lineHeight;

        ImageLine(Img img, int row, int dispWidth, int dispHeight, int lineHeight) {
            this.img = img;
            this.row = row;
            this.dispWidth = dispWidth;
            this.dispHeight = dispHeight;
            this.lineHeight = lineHeight;
        }

        @Override
        public boolean accept(FormattedCharSink sink) {
            return true;
        }
    }

    /** 読み込んだ画像（まだテクスチャにしていないもの）。 */
    private record Decoded(int width, int height, List<NativeImage> frames, int[] delays) {
    }

    private ChatImages() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(ChatImages::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ChatImages::onScreenRender);
        NeoForge.EVENT_BUS.addListener(ChatImages::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(ChatImages::onLogout);
    }

    public static boolean enabled() {
        return ChatImageFeature.INSTANCE != null && ChatImageFeature.INSTANCE.isEnabled();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(MinecraftTweak.MODID, path);
    }

    /** サーバーにこのmodがあって、ファイルを送れるか。 */
    public static boolean serverSupportsFiles() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(ChatImageNetwork.Upload.TYPE);
    }

    // ---------------------------------------------------------------- チャットの行

    /** メッセージの下に足す画像の行（ChatComponentMixin から呼ぶ）。 */
    public static List<FormattedCharSequence> linesFor(Component content, ChatComponent chat) {
        if (!enabled() || content == null) {
            return List.of();
        }
        List<String> keys = findKeys(content);
        if (keys.isEmpty()) {
            return List.of();
        }
        Minecraft mc = Minecraft.getInstance();
        int lineHeight = Math.max(1, (int) (9.0 * (mc.options.chatLineSpacing().get() + 1.0)));
        double scale = chat.getScale();
        int maxWidth = Math.max(16, (int) Math.floor(chat.getWidth() / (scale <= 0 ? 1.0 : scale)) - 4);
        int maxHeight = 60;
        try {
            maxHeight = ChatImageFeature.INSTANCE.maxHeight.get();
        } catch (Exception ignored) {
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String key : keys) {
            Img img = get(key);
            if (img.state == LoadState.READY && img.width > 0 && img.height > 0) {
                double s = Math.min(1.0, Math.min(maxWidth / (double) img.width, maxHeight / (double) img.height));
                int w = Math.max(1, (int) Math.round(img.width * s));
                int h = Math.max(1, (int) Math.round(img.height * s));
                int rows = (h + lineHeight - 1) / lineHeight;
                for (int r = 0; r < rows; r++) {
                    lines.add(new ImageLine(img, r, w, h, lineHeight));
                }
            } else {
                lines.add(new ImageLine(img, 0, 0, 0, lineHeight));
            }
        }
        return lines;
    }

    private static List<String> findKeys(Component content) {
        Set<String> keys = new LinkedHashSet<>();
        content.<Object>visit((Style style, String text) -> {
            ClickEvent click = style.getClickEvent();
            if (click != null && click.getAction() == ClickEvent.Action.COPY_TO_CLIPBOARD
                    && click.getValue().startsWith(ChatImageFormat.TAG_PREFIX)) {
                String id = click.getValue().substring(ChatImageFormat.TAG_PREFIX.length());
                if (ChatImageFormat.validId(id)) {
                    keys.add("id:" + id);
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        boolean links = true;
        try {
            links = ChatImageFeature.INSTANCE.loadLinks.get();
        } catch (Exception ignored) {
        }
        if (links) {
            Matcher m = URL.matcher(content.getString());
            while (m.find()) {
                String url = trimUrl(m.group());
                if (isImageUrl(url)) {
                    keys.add("url:" + url);
                }
            }
        }
        List<String> list = new ArrayList<>(keys);
        return list.size() > MAX_PER_MESSAGE ? list.subList(0, MAX_PER_MESSAGE) : list;
    }

    private static String trimUrl(String url) {
        while (!url.isEmpty() && ".,!?)]}>".indexOf(url.charAt(url.length() - 1)) >= 0) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    public static boolean isImageUrl(String url) {
        String path = url;
        int cut = path.indexOf('?');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        cut = path.indexOf('#');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        path = path.toLowerCase(Locale.ROOT);
        for (String ext : IMAGE_EXT) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /** 画像の行を描く（ChatComponentMixin から、文字を描くかわりに呼ぶ）。 */
    public static void draw(GuiGraphics graphics, ImageLine line, int x, int y, int color) {
        int alpha = (color >>> 24) & 0xFF;
        if (alpha <= 3) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Img img = line.img;
        if (img.state != LoadState.READY || line.dispWidth <= 0 || img.frames.length == 0) {
            String text;
            int rgb;
            if (img.state == LoadState.FAILED) {
                text = I18n.get("minecrafttweak.chat_images.failed", I18n.get(img.reasonKey, img.reasonArgs));
                rgb = 0xFF7070;
            } else {
                text = I18n.get("minecrafttweak.chat_images.loading");
                rgb = 0xAAAAAA;
            }
            graphics.drawString(mc.font, text, x, y, rgb | (alpha << 24));
            return;
        }
        int top = line.row * line.lineHeight;
        int h = Math.min(line.lineHeight, line.dispHeight - top);
        if (h <= 0) {
            return;
        }
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha / 255f);
        graphics.blit(img.frameAt(System.currentTimeMillis()), x, y, line.dispWidth, h,
                0f, (float) top, line.dispWidth, h, line.dispWidth, line.dispHeight);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    // ---------------------------------------------------------------- 読み込み

    private static Img get(String key) {
        Img img = CACHE.get(key);
        if (img == null) {
            img = new Img(key);
            CACHE.put(key, img);
            trimCache();
            start(img);
        }
        return img;
    }

    private static void trimCache() {
        Iterator<Map.Entry<String, Img>> it = CACHE.entrySet().iterator();
        while (CACHE.size() > MAX_CACHE && it.hasNext()) {
            Img old = it.next().getValue();
            it.remove();
            release(old);
        }
    }

    private static void release(Img img) {
        for (ResourceLocation rl : img.frames) {
            Minecraft.getInstance().getTextureManager().release(rl);
        }
        img.frames = new ResourceLocation[0];
    }

    private static void start(Img img) {
        if (img.key.startsWith("url:")) {
            String url = img.key.substring(4);
            POOL.submit(() -> {
                try {
                    byte[] bytes = download(url);
                    decodeAndUpload(img, bytes);
                } catch (LoadException e) {
                    Minecraft.getInstance().execute(() -> fail(img, e.key, e.args));
                } catch (Exception e) {
                    Minecraft.getInstance().execute(() -> fail(img, "minecrafttweak.chat_images.reason.network"));
                }
            });
        } else {
            String id = img.key.substring(3);
            if (!serverSupportsFiles()) {
                fail(img, "minecrafttweak.chat_images.reason.no_server_mod");
                return;
            }
            if (REQUESTED.add(id)) {
                ChatImageNetwork.sendRequest(id);
            }
        }
    }

    private static final class LoadException extends Exception {
        final String key;
        final Object[] args;

        LoadException(String key, Object... args) {
            super(key);
            this.key = key;
            this.args = args;
        }
    }

    private static byte[] download(String url) throws Exception {
        if (http == null) {
            http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10)).build();
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "minEcraft-twEEk (Minecraft mod; https://github.com/EeeeeeruKi/EC-mod)")
                .header("Accept", "image/png,image/jpeg,image/gif,image/bmp,image/*;q=0.5")
                .GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = response.body()) {
            if (response.statusCode() / 100 != 2) {
                throw new LoadException("minecrafttweak.chat_images.reason.http", response.statusCode());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_DOWNLOAD) {
                    throw new LoadException("minecrafttweak.chat_images.reason.too_big");
                }
            }
            return out.toByteArray();
        }
    }

    /** 別のスレッドで画像を読み、メインスレッドでテクスチャにする。 */
    private static void decodeAndUpload(Img img, byte[] bytes) {
        Decoded decoded;
        try {
            decoded = decode(bytes);
        } catch (LoadException e) {
            Minecraft.getInstance().execute(() -> fail(img, e.key, e.args));
            return;
        } catch (Throwable t) {
            MinecraftTweak.LOGGER.debug("minEcraft twEEk: could not decode a chat image", t);
            Minecraft.getInstance().execute(() -> fail(img, "minecrafttweak.chat_images.reason.decode"));
            return;
        }
        Minecraft.getInstance().execute(() -> upload(img, decoded));
    }

    private static void upload(Img img, Decoded d) {
        if (CACHE.get(img.key) != img) {
            d.frames.forEach(NativeImage::close);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation[] frames = new ResourceLocation[d.frames.size()];
        for (int i = 0; i < frames.length; i++) {
            ResourceLocation rl = texture("chat_image/" + (textureCounter++));
            mc.getTextureManager().register(rl, new DynamicTexture(d.frames.get(i)));
            frames[i] = rl;
        }
        int total = 0;
        for (int delay : d.delays) {
            total += delay;
        }
        img.frames = frames;
        img.delays = d.delays;
        img.totalDelay = total;
        img.width = d.width;
        img.height = d.height;
        img.state = LoadState.READY;
        queueRefresh();
    }

    private static void fail(Img img, String reasonKey, Object... args) {
        img.reasonKey = reasonKey;
        img.reasonArgs = args;
        img.state = LoadState.FAILED;
        queueRefresh();
    }

    /** 画像の高さが決まったので、チャットを並べ直す（同じフレームにまとめる）。 */
    private static void queueRefresh() {
        if (refreshQueued) {
            return;
        }
        refreshQueued = true;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            refreshQueued = false;
            if (mc.gui != null) {
                mc.gui.getChat().rescaleChat();
            }
        });
    }

    /** サーバーから届いた画像の一部。 */
    public static void onServerData(String id, int index, int total, byte[] data) {
        Img img = CACHE.get("id:" + id);
        if (img == null) {
            return;
        }
        if (total <= 0) {
            INCOMING.remove(id);
            fail(img, "minecrafttweak.chat_images.reason.missing");
            return;
        }
        ByteArrayOutputStream out = INCOMING.computeIfAbsent(id, k -> new ByteArrayOutputStream());
        out.write(data, 0, data.length);
        if (out.size() > MAX_DOWNLOAD) {
            INCOMING.remove(id);
            fail(img, "minecrafttweak.chat_images.reason.too_big");
            return;
        }
        if (index == total - 1) {
            INCOMING.remove(id);
            byte[] bytes = out.toByteArray();
            POOL.submit(() -> decodeAndUpload(img, bytes));
        }
    }

    // ---------------------------------------------------------------- 画像の読み込み（デコード）

    private static Decoded decode(byte[] bytes) throws Exception {
        String format = ChatImageFormat.detect(bytes);
        if (format == null || format.equals("webp")) {
            throw new LoadException("minecrafttweak.chat_images.reason.format");
        }
        if (format.equals("gif")) {
            Decoded gif = decodeGif(bytes);
            if (gif != null) {
                return gif;
            }
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new LoadException("minecrafttweak.chat_images.reason.format");
        }
        BufferedImage scaled = scale(image, 512);
        return new Decoded(scaled.getWidth(), scaled.getHeight(), List.of(toNative(scaled)), new int[]{1000});
    }

    private static Decoded decodeGif(byte[] bytes) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            return null;
        }
        ImageReader reader = readers.next();
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(in, false);
            int count = Math.min(reader.getNumImages(true), 150);
            if (count <= 1) {
                return null;
            }
            int width = 0;
            int height = 0;
            IIOMetadata stream = reader.getStreamMetadata();
            if (stream != null) {
                IIOMetadataNode root = (IIOMetadataNode) stream.getAsTree("javax_imageio_gif_stream_1.0");
                IIOMetadataNode screen = child(root, "LogicalScreenDescriptor");
                if (screen != null) {
                    width = parse(screen.getAttribute("logicalScreenWidth"));
                    height = parse(screen.getAttribute("logicalScreenHeight"));
                }
            }
            if (width <= 0 || height <= 0) {
                BufferedImage first = reader.read(0);
                width = first.getWidth();
                height = first.getHeight();
            }
            // 1枚の大きさは、枚数が多いほど小さくする（メモリを使いすぎないように）
            int side = 256;
            while (side > 64 && (long) count * Math.min(side, width) * Math.min(side, height) > 3_000_000L) {
                side -= 32;
            }
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            List<NativeImage> frames = new ArrayList<>();
            int[] delays = new int[count];
            try {
                for (int i = 0; i < count; i++) {
                    BufferedImage frame = reader.read(i);
                    IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                    IIOMetadataNode desc = child(root, "ImageDescriptor");
                    IIOMetadataNode control = child(root, "GraphicControlExtension");
                    int left = desc == null ? 0 : parse(desc.getAttribute("imageLeftPosition"));
                    int top = desc == null ? 0 : parse(desc.getAttribute("imageTopPosition"));
                    int delay = control == null ? 10 : parse(control.getAttribute("delayTime"));
                    String disposal = control == null ? "none" : control.getAttribute("disposalMethod");
                    BufferedImage previous = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;
                    g.drawImage(frame, left, top, null);
                    frames.add(toNative(scale(copy(canvas), side)));
                    delays[i] = delay <= 1 ? 100 : delay * 10;
                    if ("restoreToBackgroundColor".equals(disposal)) {
                        g.setComposite(AlphaComposite.Clear);
                        g.fillRect(left, top, frame.getWidth(), frame.getHeight());
                        g.setComposite(AlphaComposite.SrcOver);
                    } else if (previous != null) {
                        g.setComposite(AlphaComposite.Src);
                        g.drawImage(previous, 0, 0, null);
                        g.setComposite(AlphaComposite.SrcOver);
                    }
                }
            } catch (Throwable t) {
                frames.forEach(NativeImage::close);
                throw t;
            } finally {
                g.dispose();
            }
            NativeImage first = frames.get(0);
            return new Decoded(first.getWidth(), first.getHeight(), frames, delays);
        } finally {
            reader.dispose();
        }
    }

    private static IIOMetadataNode child(IIOMetadataNode node, String name) {
        for (int i = 0; i < node.getLength(); i++) {
            if (node.item(i) instanceof IIOMetadataNode n && n.getNodeName().equals(name)) {
                return n;
            }
        }
        return null;
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage scale(BufferedImage src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        double s = Math.min(1.0, maxSide / (double) Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * s));
        int nh = Math.max(1, (int) Math.round(h * s));
        if (s >= 1.0 && src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }
        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    private static NativeImage toNative(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        NativeImage out = new NativeImage(w, h, false);
        int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = argb[y * w + x];
                int abgr = (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
                out.setPixelRGBA(x, y, abgr);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 添付と送信

    public static Path pendingFile() {
        return pendingFile;
    }

    public static void setPendingFile(Path file) {
        pendingFile = file;
        pendingUrl = null;
    }

    public static String pendingUrl() {
        return pendingUrl;
    }

    public static void setPendingUrl(String url) {
        pendingUrl = url;
        pendingFile = null;
    }

    public static boolean hasPending() {
        return pendingFile != null || pendingUrl != null;
    }

    public static void clearPending() {
        pendingFile = null;
        pendingUrl = null;
    }

    /** 添付する前に確かめる。だめなら理由の翻訳キー、よければ null。 */
    public static String checkFile(Path file) {
        try {
            long size = Files.size(file);
            if (size > maxUploadBytes()) {
                return "minecrafttweak.chat_images.error.too_big";
            }
            byte[] head = new byte[12];
            try (InputStream in = Files.newInputStream(file)) {
                int n = in.readNBytes(head, 0, head.length);
                if (n < head.length || !ChatImageFormat.supported(head)) {
                    return "minecrafttweak.chat_images.error.not_image";
                }
            }
            return null;
        } catch (Exception e) {
            return "minecrafttweak.chat_images.error.read";
        }
    }

    public static int maxUploadBytes() {
        try {
            return ChatImageFeature.INSTANCE.maxUploadKb.get() * 1024;
        } catch (Exception e) {
            return 1024 * 1024;
        }
    }

    /** チャットを送るとき（ChatScreenMixin から）。添付があれば画像として送り、ふつうのチャットは送らない。 */
    public static boolean onSend(String message) {
        if (!hasPending() || !enabled()) {
            return false;
        }
        String text = message == null ? "" : message.strip();
        if (text.startsWith("/")) {
            return false;
        }
        Path file = pendingFile;
        String url = pendingUrl;
        clearPending();
        Minecraft mc = Minecraft.getInstance();
        if (!serverSupportsFiles()) {
            mc.gui.getChat().addMessage(Component.translatable("minecrafttweak.chat_images.attach.file_unavailable"));
            return true;
        }
        if (text.length() > 256) {
            text = text.substring(0, 256);
        }
        if (url != null) {
            // リンクの画像は、自分が読み込んでから、ファイルと同じようにサーバーへ送る（見る人はリンク先につながらない）
            String caption = text;
            mc.gui.getChat().addMessage(Component.translatable("minecrafttweak.chat_images.sending_link"));
            POOL.submit(() -> {
                try {
                    byte[] data = download(url);
                    mc.execute(() -> sendBytes(caption, data));
                } catch (LoadException e) {
                    mc.execute(() -> mc.gui.getChat().addMessage(Component.translatable("minecrafttweak.chat_images.error.link_failed",
                            Component.translatable(e.key, e.args))));
                } catch (Exception e) {
                    mc.execute(() -> mc.gui.getChat().addMessage(Component.translatable("minecrafttweak.chat_images.error.link_failed",
                            Component.translatable("minecrafttweak.chat_images.reason.network"))));
                }
            });
            if (!text.isEmpty()) {
                mc.gui.getChat().addRecentChat(text);
            }
            return true;
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            mc.gui.getChat().addMessage(Component.translatable("minecrafttweak.chat_images.error.read"));
            return true;
        }
        sendBytes(text, bytes);
        if (!text.isEmpty()) {
            mc.gui.getChat().addRecentChat(text);
        }
        return true;
    }

    /** 画像の中身をサーバーへ送る（メインスレッドで呼ぶ）。 */
    private static void sendBytes(String text, byte[] bytes) {
        Minecraft mc = Minecraft.getInstance();
        if (bytes.length > maxUploadBytes() || !ChatImageFormat.supported(bytes)) {
            mc.gui.getChat().addMessage(Component.translatable(bytes.length > maxUploadBytes()
                    ? "minecrafttweak.chat_images.error.too_big" : "minecrafttweak.chat_images.error.not_image", maxUploadBytes() / 1024));
            return;
        }
        if (!serverSupportsFiles()) {
            mc.gui.getChat().addMessage(Component.translatable("minecrafttweak.chat_images.attach.file_unavailable"));
            return;
        }
        // 自分の画面では、サーバーからもらわなくても出せるように先に読んでおく
        String id = ChatImageFormat.id(bytes);
        String key = "id:" + id;
        if (!CACHE.containsKey(key)) {
            Img img = new Img(key);
            CACHE.put(key, img);
            trimCache();
            REQUESTED.add(id);
            POOL.submit(() -> decodeAndUpload(img, bytes));
        }
        int chunk = ChatImageNetwork.UPLOAD_CHUNK;
        int total = Math.max(1, (bytes.length + chunk - 1) / chunk);
        for (int i = 0; i < total; i++) {
            int from = i * chunk;
            int to = Math.min(bytes.length, from + chunk);
            ChatImageNetwork.sendUpload(i, total, text, java.util.Arrays.copyOfRange(bytes, from, to));
        }
    }

    // ---------------------------------------------------------------- 画面

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ChatScreen screen) || !enabled()) {
            return;
        }
        EditBox input = null;
        for (GuiEventListener l : event.getListenersList()) {
            if (l instanceof EditBox box) {
                input = box;
                break;
            }
        }
        if (input == null) {
            return;
        }
        input.setX(input.getX() + 14);
        input.setWidth(input.getWidth() - 14);
        EditBox box = input;
        Button plus = Button.builder(Component.literal("+"), b -> {
                    keepPending = true;
                    Minecraft.getInstance().setScreen(new ChatAttachScreen(box.getValue()));
                })
                .bounds(2, screen.height - 14, 12, 12)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.chat_images.plus")))
                .build();
        event.addListener(plus);
    }

    private static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ChatScreen screen) || !hasPending() || !enabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String name = pendingFile != null ? pendingFile.getFileName().toString() : shorten(pendingUrl);
        String text = I18n.get("minecrafttweak.chat_images.attached", name);
        int w = mc.font.width(text);
        GuiGraphics g = event.getGuiGraphics();
        g.fill(2, screen.height - 27, 2 + w + 4, screen.height - 15, 0xC0000000);
        g.drawString(mc.font, text, 4, screen.height - 25, 0x55FF55);
    }

    private static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof ChatScreen) {
            if (keepPending) {
                keepPending = false;
            } else {
                clearPending();
            }
        }
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        for (Img img : CACHE.values()) {
            release(img);
        }
        CACHE.clear();
        INCOMING.clear();
        REQUESTED.clear();
        clearPending();
    }

    // ---------------------------------------------------------------- PicPop 連携

    private static Boolean picpopLoaded;

    /** PicPop（画像を画面に大きく出すmod）が入っているか。 */
    public static boolean picpopLoaded() {
        if (picpopLoaded == null) {
            try {
                picpopLoaded = ModList.get().isLoaded("picpop");
            } catch (Exception e) {
                picpopLoaded = false;
            }
        }
        return picpopLoaded;
    }

    /**
     * PicPop 連携: チャットの画像の上にマウスがあるときの文字の形（ChatComponentMixin から呼ぶ）。
     * クリックすると PicPop の /imageview で画面の中央に大きく表示する。PicPop が無いときは null。
     */
    public static Style picpopStyle(FormattedCharSequence text, double chatX) {
        if (!enabled() || !picpopLoaded() || !(text instanceof ImageLine line)) {
            return null;
        }
        Img img = line.img;
        if (img.state != LoadState.READY || line.dispWidth <= 0 || chatX < 0 || chatX > line.dispWidth) {
            return null;
        }
        String arg;
        if (img.key.startsWith("url:")) {
            arg = img.key.substring(4);
        } else if (img.key.startsWith("id:")) {
            arg = "tweek:" + img.key.substring(3);
        } else {
            return null;
        }
        return Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/imageview " + arg))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("minecrafttweak.chat_images.picpop")));
    }

    /** PicPop 連携: このリンクの画像をチャットの中に表示するか（表示するなら PicPop はボタンを付けない）。 */
    public static boolean showsLinkInChat(String url) {
        if (!enabled() || url == null || !isImageUrl(url)) {
            return false;
        }
        try {
            return ChatImageFeature.INSTANCE.loadLinks.get();
        } catch (Exception e) {
            return true;
        }
    }

    /** PicPop 連携: ファイルで送られた画像のコマ {ResourceLocation[] コマ, int[] 待ち時間(ms), 幅, 高さ}。まだ無ければ null。 */
    public static Object[] picpopFrames(String id) {
        Img img = CACHE.get("id:" + id);
        if (img == null || img.state != LoadState.READY || img.frames.length == 0) {
            return null;
        }
        return new Object[]{img.frames.clone(), img.delays.clone(), img.width, img.height};
    }

    private static String shorten(String url) {
        if (url == null) {
            return "";
        }
        return url.length() <= 48 ? url : url.substring(0, 45) + "...";
    }
}
