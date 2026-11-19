package com.eeeeeeruki.minecrafttweak.client.feature;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 曲の読み込み。URL ならダウンロードして（同じURLは2回目からファイルを使う）、ファイルならそのまま読み、
 * PCM（16ビット）にする。対応は .ogg（Ogg Vorbis）と .wav。
 */
public final class AudioLoader {
    /** これより大きいファイルは読まない（50MB）。 */
    private static final long MAX_BYTES = 50L * 1024 * 1024;

    private AudioLoader() {
    }

    /** 読み込んだ音。pcm は使い終わったら free() で返す。 */
    public static final class Audio {
        public final ShortBuffer pcm;
        public final int channels;
        public final int sampleRate;
        private final boolean fromStb;

        Audio(ShortBuffer pcm, int channels, int sampleRate, boolean fromStb) {
            this.pcm = pcm;
            this.channels = channels;
            this.sampleRate = sampleRate;
            this.fromStb = fromStb;
        }

        public void free() {
            if (fromStb) {
                LibCStdlib.free(pcm);
            } else {
                MemoryUtil.memFree(pcm);
            }
        }
    }

    public static class LoadException extends Exception {
        public final String key;
        public final String detail;

        LoadException(String key, String detail) {
            super(key + ": " + detail);
            this.key = key;
            this.detail = detail;
        }
    }

    public static Audio load(String source, Path cacheDir) throws LoadException {
        byte[] data = readBytes(source.trim(), cacheDir);
        String lower = source.toLowerCase(Locale.ROOT);
        boolean ogg = data.length >= 4 && data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S';
        boolean wav = data.length >= 4 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F';
        if (ogg || (!wav && lower.contains(".ogg"))) {
            return decodeOgg(data);
        }
        if (wav) {
            return decodeWav(data);
        }
        throw new LoadException("minecrafttweak.menu_music.error.format", "");
    }

    private static byte[] readBytes(String source, Path cacheDir) throws LoadException {
        try {
            if (source.startsWith("http://") || source.startsWith("https://")) {
                Files.createDirectories(cacheDir);
                Path cached = cacheDir.resolve(hash(source) + ".bin");
                if (Files.isRegularFile(cached) && Files.size(cached) > 0) {
                    return Files.readAllBytes(cached);
                }
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(15))
                        .build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(source))
                        .timeout(Duration.ofSeconds(60))
                        .header("User-Agent", "minEcraft-twEEk")
                        .GET().build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() / 100 != 2) {
                    throw new LoadException("minecrafttweak.menu_music.error.http", String.valueOf(response.statusCode()));
                }
                byte[] bytes;
                try (InputStream in = new BufferedInputStream(response.body())) {
                    bytes = readLimited(in);
                }
                Path tmp = cacheDir.resolve(hash(source) + ".tmp");
                Files.write(tmp, bytes);
                Files.move(tmp, cached, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return bytes;
            }
            Path file = Path.of(source.startsWith("file:") ? URI.create(source).getPath() : source);
            if (!Files.isRegularFile(file)) {
                throw new LoadException("minecrafttweak.menu_music.error.not_found", source);
            }
            if (Files.size(file) > MAX_BYTES) {
                throw new LoadException("minecrafttweak.menu_music.error.too_big", "");
            }
            return Files.readAllBytes(file);
        } catch (LoadException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new LoadException("minecrafttweak.menu_music.error.url", String.valueOf(e.getMessage()));
        } catch (IOException e) {
            throw new LoadException("minecrafttweak.menu_music.error.io", String.valueOf(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LoadException("minecrafttweak.menu_music.error.io", "interrupted");
        }
    }

    private static byte[] readLimited(InputStream in) throws IOException, LoadException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            if (out.size() > MAX_BYTES) {
                throw new LoadException("minecrafttweak.menu_music.error.too_big", "");
            }
        }
        return out.toByteArray();
    }

    private static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }

    private static Audio decodeOgg(byte[] data) throws LoadException {
        ByteBuffer mem = MemoryUtil.memAlloc(data.length);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            mem.put(data).flip();
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer rate = stack.mallocInt(1);
            ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(mem, channels, rate);
            if (pcm == null) {
                throw new LoadException("minecrafttweak.menu_music.error.decode", "ogg");
            }
            int ch = channels.get(0);
            if (ch < 1 || ch > 2) {
                LibCStdlib.free(pcm);
                throw new LoadException("minecrafttweak.menu_music.error.channels", String.valueOf(ch));
            }
            return new Audio(pcm, ch, rate.get(0), true);
        } finally {
            MemoryUtil.memFree(mem);
        }
    }

    private static Audio decodeWav(byte[] data) throws LoadException {
        try (AudioInputStream original = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
            AudioFormat src = original.getFormat();
            int ch = src.getChannels();
            if (ch < 1 || ch > 2) {
                throw new LoadException("minecrafttweak.menu_music.error.channels", String.valueOf(ch));
            }
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16, ch, ch * 2,
                    src.getSampleRate(), false);
            try (AudioInputStream converted = AudioSystem.getAudioInputStream(target, original)) {
                byte[] bytes = converted.readAllBytes();
                ShortBuffer pcm = MemoryUtil.memAllocShort(bytes.length / 2);
                ShortBuffer view = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                pcm.put(view).flip();
                return new Audio(pcm, ch, (int) src.getSampleRate(), false);
            }
        } catch (LoadException e) {
            throw e;
        } catch (Exception e) {
            throw new LoadException("minecrafttweak.menu_music.error.decode", String.valueOf(e.getMessage()));
        }
    }
}
