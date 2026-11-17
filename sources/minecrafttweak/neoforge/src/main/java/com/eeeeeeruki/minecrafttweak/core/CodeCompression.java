package com.eeeeeeruki.minecrafttweak.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** 圧縮コード（「MT1.」で始まる英数字の文字列）。中身は普通の設定コードを縮めたもの。 */
public final class CodeCompression {
    public static final String PREFIX = "MT1.";

    private CodeCompression() {
    }

    public static boolean isCompressed(String token) {
        return token.regionMatches(true, 0, PREFIX, 0, PREFIX.length());
    }

    public static String encode(String code) {
        byte[] input = code.getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (!deflater.finished()) {
            int n = deflater.deflate(buf);
            out.write(buf, 0, n);
        }
        deflater.end();
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
    }

    /** 戻せなければ null。 */
    public static String decode(String token) {
        try {
            byte[] data = Base64.getUrlDecoder().decode(token.substring(PREFIX.length()));
            Inflater inflater = new Inflater(true);
            inflater.setInput(data);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int guard = 0;
            while (!inflater.finished() && guard++ < 10000) {
                int n = inflater.inflate(buf);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                out.write(buf, 0, n);
                if (out.size() > 1_000_000) {
                    break;
                }
            }
            inflater.end();
            return out.toString(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | DataFormatException e) {
            return null;
        }
    }
}
