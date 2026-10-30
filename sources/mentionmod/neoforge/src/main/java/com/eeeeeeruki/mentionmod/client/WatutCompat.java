package com.eeeeeeruki.mentionmod.client;

import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What Are They Up To (Watut) との連携。
 * Watut が入っていなくても動くように、Watut のクラスはリフレクションで触る。
 */
public final class WatutCompat {
    private static boolean ready;
    private static Method getManager;
    private static Method getGuiState;
    private static Method getChatState;
    private static Field lookup;
    private static Field cfgVisible;
    private static Field cfgServerVisible;
    private static Field cfgX;
    private static Field cfgY;

    private static boolean suppressed;
    private static boolean savedVisible = true;

    private WatutCompat() {}

    public static void init() {
        if (!ModList.get().isLoaded("watut")) return;
        try {
            getManager = Class.forName("com.corosus.watut.WatutMod").getMethod("getPlayerStatusManagerClient");
            lookup = Class.forName("com.corosus.watut.PlayerStatusManager").getField("lookupPlayerToStatus");
            Class<?> status = Class.forName("com.corosus.watut.PlayerStatus");
            getGuiState = status.getMethod("getPlayerGuiState");
            getChatState = status.getMethod("getPlayerChatState");
            Class<?> client = Class.forName("com.corosus.watut.config.ConfigClient");
            cfgVisible = client.getField("screenTypingVisible");
            cfgX = client.getField("screenTypingRelativePosition_X");
            cfgY = client.getField("screenTypingRelativePosition_Y");
            cfgServerVisible = Class.forName("com.corosus.watut.config.ConfigServerControlledSyncedToClient")
                    .getField("screenTypingVisible");
            ready = true;
        } catch (Throwable t) {
            ready = false;
        }
    }

    public static boolean isReady() {
        return ready;
    }

    /** Watut 自身の英語の「〇〇 is typing」を描く直前に止める（描いたあと restore で戻す） */
    public static void suppressOriginalText() {
        if (!ready || suppressed) return;
        try {
            savedVisible = cfgVisible.getBoolean(null);
            cfgVisible.setBoolean(null, false);
            suppressed = true;
        } catch (Throwable t) {
            ready = false;
        }
    }

    public static void restore() {
        if (!suppressed) return;
        suppressed = false;
        try {
            cfgVisible.setBoolean(null, savedVisible);
        } catch (Throwable ignored) {
        }
    }

    /** Watut の設定で入力中の表示がオンになっているか（本人の設定とサーバーの設定の両方） */
    public static boolean shouldShow() {
        if (!ready) return false;
        try {
            boolean own = suppressed ? savedVisible : cfgVisible.getBoolean(null);
            return own && cfgServerVisible.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    public static int offsetX() {
        try {
            return ready ? cfgX.getInt(null) : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int offsetY() {
        try {
            return ready ? cfgY.getInt(null) : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** チャット画面で文字を打っているプレイヤー（Watut の判定そのまま） */
    public static List<UUID> typingPlayers() {
        List<UUID> out = new ArrayList<>();
        if (!ready) return out;
        try {
            Object manager = getManager.invoke(null);
            if (manager == null) return out;
            Map<?, ?> map = (Map<?, ?>) lookup.get(manager);
            for (Map.Entry<?, ?> e : new ArrayList<>(map.entrySet())) {
                Object s = e.getValue();
                if (s == null || !(e.getKey() instanceof UUID uuid)) continue;
                if ("CHAT_SCREEN".equals(name(getGuiState.invoke(s)))
                        && "CHAT_TYPING".equals(name(getChatState.invoke(s)))) {
                    out.add(uuid);
                }
            }
        } catch (Throwable t) {
            // Watut の中身が変わっていたら、表示しないだけにする
        }
        return out;
    }

    private static String name(Object o) {
        return o instanceof Enum<?> en ? en.name() : String.valueOf(o);
    }
}
