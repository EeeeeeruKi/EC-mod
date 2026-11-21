package com.eeeeeeruki.minecrafttweak.server;

import java.security.MessageDigest;
import java.util.HexFormat;

/** 画像ファイルの種類の見分けと、画像の番号（中身から作るので、同じ画像なら同じ番号）。 */
public final class ChatImageFormat {
    public static final String TAG_PREFIX = "minecrafttweak:img:";

    private ChatImageFormat() {
    }

    /** png / jpeg / gif / bmp / webp か、わからなければ null。 */
    public static String detect(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) {
            return "jpeg";
        }
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
            return "gif";
        }
        if (b[0] == 'B' && b[1] == 'M') {
            return "bmp";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }

    /** 送れる形式か（webp は読めないので送れない）。 */
    public static boolean supported(byte[] b) {
        String f = detect(b);
        return f != null && !f.equals("webp");
    }

    public static String id(byte[] b) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-1").digest(b);
            return HexFormat.of().formatHex(h).substring(0, 20);
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(b));
        }
    }

    public static boolean validId(String id) {
        return id != null && id.length() >= 4 && id.length() <= 40 && id.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
    }
}
