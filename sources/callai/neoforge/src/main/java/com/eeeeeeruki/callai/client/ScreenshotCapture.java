package com.eeeeeeruki.callai.client;

import com.eeeeeeruki.callai.network.CallAINetwork;
import com.eeeeeeruki.callai.network.ScreenshotChunkPacket;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 今の画面を撮って、縮小したJPEGをサーバーに送ります（ファイルには保存しません）。
 */
public final class ScreenshotCapture {
    private static final int MAX_SIDE = 1024;
    private static final float QUALITY = 0.75f;

    private ScreenshotCapture() {}

    public static void captureAndSend(int id) {
        int[] rgb;
        int w;
        int h;
        try {
            Minecraft mc = Minecraft.getInstance();
            try (NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                int srcW = img.getWidth();
                int srcH = img.getHeight();
                double scale = Math.min(1.0, MAX_SIDE / (double) Math.max(srcW, srcH));
                w = Math.max(1, (int) Math.round(srcW * scale));
                h = Math.max(1, (int) Math.round(srcH * scale));
                rgb = new int[w * h];
                for (int y = 0; y < h; y++) {
                    int sy = Math.min(srcH - 1, (int) (y / scale));
                    for (int x = 0; x < w; x++) {
                        int sx = Math.min(srcW - 1, (int) (x / scale));
                        int abgr = img.getPixelRGBA(sx, sy);
                        int r = abgr & 0xFF;
                        int g = (abgr >> 8) & 0xFF;
                        int b = (abgr >> 16) & 0xFF;
                        rgb[y * w + x] = (r << 16) | (g << 8) | b;
                    }
                }
            }
        } catch (Throwable t) {
            sendFailed(id);
            return;
        }

        final int fw = w;
        final int fh = h;
        final int[] pixels = rgb;
        Thread t = new Thread(() -> {
            try {
                BufferedImage bi = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_RGB);
                bi.setRGB(0, 0, fw, fh, pixels, 0, fw);
                byte[] jpeg = encodeJpeg(bi);
                int total = (jpeg.length + ScreenshotChunkPacket.MAX_CHUNK - 1) / ScreenshotChunkPacket.MAX_CHUNK;
                for (int i = 0; i < total; i++) {
                    int from = i * ScreenshotChunkPacket.MAX_CHUNK;
                    int to = Math.min(jpeg.length, from + ScreenshotChunkPacket.MAX_CHUNK);
                    CallAINetwork.sendToServer(new ScreenshotChunkPacket(id, i, total, Arrays.copyOfRange(jpeg, from, to)));
                }
            } catch (Throwable e) {
                sendFailed(id);
            }
        }, "CallAI-Screenshot");
        t.setDaemon(true);
        t.start();
    }

    private static byte[] encodeJpeg(BufferedImage img) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static void sendFailed(int id) {
        try {
            CallAINetwork.sendToServer(new ScreenshotChunkPacket(id, 0, 0, new byte[0]));
        } catch (Throwable ignored) {
        }
    }
}
