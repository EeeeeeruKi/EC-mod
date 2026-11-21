package com.eeeeeeruki.minecrafttweak.core;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * 圧縮コード。中身は普通の設定コードを縮めたもの。
 *
 * - 「MT2.」… 今の形。項目を1つずつビットに詰めて（オン/オフは1ビット、Quark の番号は20ビット…）、
 *   さらに縮むときだけ Deflate をかけ、英数字と「-」「.」の64文字で書く。
 *   Discord などで斜体や太字になってしまう「_」「*」は使わない。
 * - 「MT1.」… 前の形（テキストを Deflate して base64url）。読み込みだけできる。
 */
public final class CodeCompression {
    public static final String PREFIX = "MT2.";
    public static final String PREFIX_V1 = "MT1.";

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.";
    private static final Pattern KEY = Pattern.compile("([a-z]*)(\\d+)(?:([a-z]+)|\\.(\\d+))?");
    private static final String[] PREFIXES = {"", "v", "s", "se", "q"};
    private static final int OTHER_PREFIX = 5;

    // 値の種類（オン/オフ以外。3ビット）
    private static final int V_INT = 2;
    private static final int V_NEG_INT = 3;
    private static final int V_DECIMAL = 4;
    private static final int V_TEXT = 5;      // 「%」で書いた文字を戻して、そのまま詰める
    private static final int V_RAW = 6;       // そのままの文字

    private CodeCompression() {
    }

    public static boolean isCompressed(String token) {
        return token.regionMatches(true, 0, PREFIX, 0, PREFIX.length())
                || token.regionMatches(true, 0, PREFIX_V1, 0, PREFIX_V1.length());
    }

    // =====================================================================
    // 縮める
    // =====================================================================

    /** 1つの項目を分けたもの。 */
    private record Item(int prefixIndex, String prefix, String digits, long number, String suffix, String value) {
    }

    public static String encode(String code) {
        List<Item> items = new ArrayList<>();
        List<String> odd = new ArrayList<>();
        for (String entry : code.split(",")) {
            if (entry.isEmpty()) {
                continue;
            }
            Item item = parse(entry);
            if (item == null) {
                odd.add(entry);
            } else {
                items.add(item);
            }
        }
        // 並べ替えると、同じ頭文字が続き、番号の差が小さくなって短く書ける（読み込むときの順番は関係ない）
        items.sort((a, b) -> {
            if (a.prefixIndex != b.prefixIndex) return Integer.compare(a.prefixIndex, b.prefixIndex);
            int c = a.prefix.compareTo(b.prefix);
            if (c != 0) return c;
            if (a.number != b.number) return Long.compare(a.number, b.number);
            return a.suffix.compareTo(b.suffix);
        });
        Bits bits = new Bits();
        bits.gamma(odd.size() + 1);
        for (String o : odd) {
            writeString(bits, o.getBytes(StandardCharsets.UTF_8));
        }
        bits.gamma(items.size() + 1);
        int i = 0;
        while (i < items.size()) {
            // 同じ頭文字のまとまり
            int j = i;
            while (j < items.size() && items.get(j).prefixIndex == items.get(i).prefixIndex
                    && items.get(j).prefix.equals(items.get(i).prefix)) {
                j++;
            }
            List<Item> group = items.subList(i, j);
            bits.gamma(group.size());
            bits.write(group.get(0).prefixIndex, 3);
            if (group.get(0).prefixIndex == OTHER_PREFIX) {
                writeString(bits, group.get(0).prefix.getBytes(StandardCharsets.UTF_8));
            }
            long[] deltas = new long[group.size()];
            long prev = 0;
            for (int k = 0; k < group.size(); k++) {
                deltas[k] = group.get(k).number - prev;
                prev = group.get(k).number;
            }
            int k = bestRice(deltas);
            bits.write(k, 6);
            for (int n = 0; n < group.size(); n++) {
                Item it = group.get(n);
                rice(bits, deltas[n], k);
                boolean zeros = it.digits.length() > 1 && it.digits.charAt(0) == '0';
                bits.write(zeros ? 1 : 0, 1);
                if (zeros) {
                    int z = 0;
                    while (z < it.digits.length() - 1 && it.digits.charAt(z) == '0') {
                        z++;
                    }
                    bits.gamma(z);
                }
                if (it.suffix.isEmpty()) {
                    bits.write(0, 1);
                } else if (it.suffix.charAt(0) == '.') {
                    bits.write(1, 1);
                    bits.write(1, 1);
                    writeNumber(bits, it.suffix.substring(1));
                } else {
                    bits.write(1, 1);
                    bits.write(0, 1);
                    bits.gamma(it.suffix.length());
                    for (char c : it.suffix.toCharArray()) {
                        bits.write(c - 'a', 5);
                    }
                }
                writeValue(bits, it.value);
            }
            i = j;
        }
        byte[] packed = bits.toBytes();
        byte[] deflated = deflate(packed);
        byte[] body;
        int flags;
        if (deflated.length < packed.length) {
            body = deflated;
            flags = 1;
        } else {
            body = packed;
            flags = 0;
        }
        byte[] all = new byte[body.length + 1];
        all[0] = (byte) flags;
        System.arraycopy(body, 0, all, 1, body.length);
        return PREFIX + toText(all);
    }

    private static Item parse(String entry) {
        int dash = entry.indexOf('-');
        if (dash <= 0) {
            return null;
        }
        Matcher m = KEY.matcher(entry.substring(0, dash));
        if (!m.matches() || m.group(2).length() > 17) {
            return null;
        }
        String prefix = m.group(1);
        int p = OTHER_PREFIX;
        for (int i = 0; i < PREFIXES.length; i++) {
            if (PREFIXES[i].equals(prefix)) {
                p = i;
                break;
            }
        }
        String suffix = m.group(3) != null ? m.group(3) : m.group(4) != null ? "." + m.group(4) : "";
        if (suffix.startsWith(".") && suffix.length() > 18) {
            return null;
        }
        return new Item(p, prefix, m.group(2), Long.parseLong(m.group(2)), suffix, entry.substring(dash + 1));
    }

    /** ライス符号の k（いちばん短くなるもの）。 */
    private static int bestRice(long[] values) {
        int best = 0;
        long bestBits = Long.MAX_VALUE;
        for (int k = 0; k < 60; k++) {
            long total = 0;
            for (long v : values) {
                total += riceLength(v, k);
            }
            if (total < bestBits) {
                bestBits = total;
                best = k;
            }
        }
        return best;
    }

    private static long riceLength(long v, int k) {
        long q = v >>> k;
        if (q >= 16) {
            return 16 + 2L * (64 - Long.numberOfLeadingZeros(v + 1)) - 1;
        }
        return q + 1 + k;
    }

    private static void rice(Bits bits, long v, int k) {
        long q = v >>> k;
        if (q >= 16) {
            bits.write(0xFFFF, 16); // 大きすぎるときはガンマ符号で
            bits.gamma(v + 1);
            return;
        }
        for (long i = 0; i < q; i++) {
            bits.write(1, 1);
        }
        bits.write(0, 1);
        bits.write(v & ((1L << k) - 1), k);
    }

    private static long readRice(Bits bits, int k) {
        long q = 0;
        while (bits.read(1) == 1) {
            if (++q == 16) {
                return bits.readGamma() - 1;
            }
        }
        return (q << k) | bits.read(k);
    }

    /** 数字の並び（先頭の0もそのまま戻せるように）。 */
    private static void writeNumber(Bits bits, String digits) {
        int zeros = 0;
        while (zeros < digits.length() - 1 && digits.charAt(zeros) == '0') {
            zeros++;
        }
        bits.gamma(zeros + 1);
        bits.gamma(new BigInteger(digits.substring(zeros)).add(BigInteger.ONE));
    }

    private static String readNumber(Bits bits) {
        int zeros = (int) bits.readGamma() - 1;
        String rest = bits.readGammaBig().subtract(BigInteger.ONE).toString();
        return "0".repeat(Math.max(0, zeros)) + rest;
    }

    private static void writeValue(Bits bits, String v) {
        if (v.equals("t") || v.equals("f")) {
            bits.write(1, 1);
            bits.write(v.equals("t") ? 1 : 0, 1);
            return;
        }
        bits.write(0, 1);
        if (v.matches("0|[1-9]\\d{0,17}")) {
            bits.write(V_INT, 3);
            bits.gamma(new BigInteger(v).add(BigInteger.ONE));
            return;
        }
        if (v.matches("-[1-9]\\d{0,17}")) {
            bits.write(V_NEG_INT, 3);
            bits.gamma(new BigInteger(v.substring(1)));
            return;
        }
        if (v.matches("-?(0|[1-9]\\d{0,15})\\.\\d{1,7}") && !v.endsWith("0")) {
            BigDecimal d = new BigDecimal(v);
            if (d.toPlainString().equals(v) || ("-" + d.abs().toPlainString()).equals(v)) {
                bits.write(V_DECIMAL, 3);
                bits.write(v.startsWith("-") ? 1 : 0, 1);
                bits.write(d.scale(), 3);
                bits.gamma(d.unscaledValue().abs().add(BigInteger.ONE));
                return;
            }
        }
        String plain = decodeText(v);
        if (plain != null && encodeText(plain).equals(v)) {
            bits.write(V_TEXT, 3);
            writeString(bits, plain.getBytes(StandardCharsets.UTF_8));
            return;
        }
        bits.write(V_RAW, 3);
        writeString(bits, v.getBytes(StandardCharsets.UTF_8));
    }

    private static String readValue(Bits bits) {
        if (bits.read(1) == 1) {
            return bits.read(1) == 1 ? "t" : "f";
        }
        int type = (int) bits.read(3);
        return switch (type) {
            case V_INT -> bits.readGammaBig().subtract(BigInteger.ONE).toString();
            case V_NEG_INT -> "-" + bits.readGammaBig();
            case V_DECIMAL -> {
                boolean negative = bits.read(1) == 1;
                int scale = (int) bits.read(3);
                BigInteger unscaled = bits.readGammaBig().subtract(BigInteger.ONE);
                String s = new BigDecimal(unscaled, scale).toPlainString();
                yield negative ? "-" + s : s;
            }
            case V_TEXT -> encodeText(new String(readString(bits), StandardCharsets.UTF_8));
            case V_RAW -> new String(readString(bits), StandardCharsets.UTF_8);
            default -> throw new IllegalStateException("bad value type");
        };
    }

    private static void writeString(Bits bits, byte[] bytes) {
        bits.gamma(bytes.length + 1);
        for (byte b : bytes) {
            bits.write(b & 0xFF, 8);
        }
    }

    private static byte[] readString(Bits bits) {
        int n = (int) bits.readGamma() - 1;
        if (n < 0 || n > 1_000_000) {
            throw new IllegalStateException("bad length");
        }
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            out[i] = (byte) bits.read(8);
        }
        return out;
    }

    // =====================================================================
    // 戻す
    // =====================================================================

    /** 戻せなければ null。 */
    public static String decode(String token) {
        try {
            if (token.regionMatches(true, 0, PREFIX_V1, 0, PREFIX_V1.length())) {
                return decodeV1(token);
            }
            byte[] all = fromText(token.substring(PREFIX.length()));
            if (all == null || all.length < 1) {
                return null;
            }
            byte[] body = new byte[all.length - 1];
            System.arraycopy(all, 1, body, 0, body.length);
            if ((all[0] & 1) != 0) {
                body = inflate(body);
                if (body == null) {
                    return null;
                }
            }
            Bits bits = new Bits(body);
            List<String> out = new ArrayList<>();
            int odd = (int) bits.readGamma() - 1;
            if (odd < 0 || odd > 100_000) {
                return null;
            }
            for (int i = 0; i < odd; i++) {
                out.add(new String(readString(bits), StandardCharsets.UTF_8));
            }
            int count = (int) bits.readGamma() - 1;
            if (count < 0 || count > 100_000) {
                return null;
            }
            int done = 0;
            while (done < count) {
                int size = (int) bits.readGamma();
                int p = (int) bits.read(3);
                String prefix;
                if (p == OTHER_PREFIX) {
                    prefix = new String(readString(bits), StandardCharsets.UTF_8);
                } else if (p < PREFIXES.length) {
                    prefix = PREFIXES[p];
                } else {
                    return null;
                }
                int k = (int) bits.read(6);
                long number = 0;
                for (int n = 0; n < size; n++) {
                    number += readRice(bits, k);
                    String digits = Long.toString(number);
                    if (bits.read(1) == 1) {
                        int z = (int) bits.readGamma();
                        digits = "0".repeat(z) + digits;
                    }
                    String suffix = "";
                    if (bits.read(1) == 1) {
                        if (bits.read(1) == 1) {
                            suffix = "." + readNumber(bits);
                        } else {
                            int len = (int) bits.readGamma();
                            StringBuilder sb = new StringBuilder();
                            for (int c = 0; c < len; c++) {
                                sb.append((char) ('a' + bits.read(5)));
                            }
                            suffix = sb.toString();
                        }
                    }
                    out.add(prefix + digits + suffix + "-" + readValue(bits));
                }
                done += size;
            }
            return String.join(",", out);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String decodeV1(String token) {
        try {
            byte[] data = Base64.getUrlDecoder().decode(token.substring(PREFIX_V1.length()));
            byte[] out = inflate(data);
            return out == null ? null : new String(out, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // =====================================================================
    // 64文字で書く
    // =====================================================================

    private static String toText(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int count = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            count += 8;
            while (count >= 6) {
                count -= 6;
                sb.append(ALPHABET.charAt((buffer >> count) & 63));
            }
        }
        if (count > 0) {
            sb.append(ALPHABET.charAt((buffer << (6 - count)) & 63));
        }
        return sb.toString();
    }

    private static byte[] fromText(String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            int v = ALPHABET.indexOf(text.charAt(i));
            if (v < 0) {
                return null;
            }
            buffer = (buffer << 6) | v;
            count += 6;
            if (count >= 8) {
                count -= 8;
                out.write((buffer >> count) & 0xFF);
            }
        }
        return out.toByteArray();
    }

    // =====================================================================
    // Deflate
    // =====================================================================

    private static byte[] deflate(byte[] input) {
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
        return out.toByteArray();
    }

    private static byte[] inflate(byte[] data) {
        try {
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
            return out.toByteArray();
        } catch (DataFormatException e) {
            return null;
        }
    }

    // =====================================================================
    // 文字の値（CodeValues と同じ書き方。ここだけで動くように同じものを持つ）
    // =====================================================================

    private static String encodeText(String text) {
        if (text.isEmpty()) {
            return "~";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            boolean plain = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || ":/.?&=_-#@!$*()'+[]".indexOf(c) >= 0;
            if (plain) {
                sb.append((char) c);
            } else {
                sb.append('%').append(String.format("%02X", c));
            }
        }
        return sb.toString();
    }

    private static String decodeText(String code) {
        if (code.equals("~")) {
            return "";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '%' && i + 2 < code.length()) {
                try {
                    out.write(Integer.parseInt(code.substring(i + 1, i + 3), 16));
                    i += 2;
                    continue;
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            if (c > 0x7F) {
                return null;
            }
            out.write(c);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    // =====================================================================
    // ビットの読み書き
    // =====================================================================

    private static final class Bits {
        private final ByteArrayOutputStream out;
        private final byte[] in;
        private int current;
        private int used;
        private int pos;

        Bits() {
            out = new ByteArrayOutputStream();
            in = null;
        }

        Bits(byte[] data) {
            out = null;
            in = data;
        }

        void write(long value, int n) {
            for (int i = n - 1; i >= 0; i--) {
                current = (current << 1) | (int) ((value >>> i) & 1);
                if (++used == 8) {
                    out.write(current);
                    current = 0;
                    used = 0;
                }
            }
        }

        /** Elias ガンマ符号（1以上）。小さい数ほど短い。 */
        void gamma(long n) {
            gamma(BigInteger.valueOf(n));
        }

        void gamma(BigInteger n) {
            int len = n.bitLength();
            write(0, len - 1);
            for (int i = len - 1; i >= 0; i--) {
                write(n.testBit(i) ? 1 : 0, 1);
            }
        }

        long read(int n) {
            long v = 0;
            for (int i = 0; i < n; i++) {
                int byteIndex = pos >> 3;
                if (byteIndex >= in.length) {
                    throw new IllegalStateException("end of data");
                }
                int bit = (in[byteIndex] >> (7 - (pos & 7))) & 1;
                v = (v << 1) | bit;
                pos++;
            }
            return v;
        }

        long readGamma() {
            return readGammaBig().longValueExact();
        }

        BigInteger readGammaBig() {
            int zeros = 0;
            while (read(1) == 0) {
                if (++zeros > 200) {
                    throw new IllegalStateException("bad number");
                }
            }
            BigInteger v = BigInteger.ONE;
            for (int i = 0; i < zeros; i++) {
                v = v.shiftLeft(1).or(BigInteger.valueOf(read(1)));
            }
            return v;
        }

        byte[] toBytes() {
            if (used > 0) {
                out.write(current << (8 - used));
                current = 0;
                used = 0;
            }
            return out.toByteArray();
        }
    }
}
