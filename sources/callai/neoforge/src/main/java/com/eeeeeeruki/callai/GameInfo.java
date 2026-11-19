package com.eeeeeeruki.callai;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AIに渡す、いまのゲームの様子（話しかけたプレイヤーの状態・時間・ほかのプレイヤー・使えるコマンド）。
 */
public final class GameInfo {
    private GameInfo() {}

    /** systemPrompt の後ろに付ける、いまの様子（サーバーのスレッドで呼んでください） */
    public static String promptFor(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        String name = player.getGameProfile().getName();
        StringBuilder sb = new StringBuilder("\n\n【いまのゲームの様子】（質問に答えるときに使ってください）");

        // 話しかけたプレイヤー
        sb.append("\n").append(name).append("の状態: ")
                .append("ディメンション ").append(dimension(player))
                .append("、座標 ").append(player.getBlockX()).append(" ").append(player.getBlockY()).append(" ").append(player.getBlockZ())
                .append("、体力 ").append(fmt(player.getHealth())).append("/").append(fmt(player.getMaxHealth()))
                .append("、空腹度 ").append(player.getFoodData().getFoodLevel()).append("/20")
                .append("（隠し満腹度 ").append(fmt(player.getFoodData().getSaturationLevel())).append("）")
                .append("、経験値レベル ").append(player.experienceLevel)
                .append("、ゲームモード ").append(player.gameMode.getGameModeForPlayer().getName())
                .append("、権限レベル ").append(server.getProfilePermissions(player.getGameProfile()));
        sb.append("\n").append(name).append("のエフェクト: ").append(effects(player));

        // 時間と天気（そのプレイヤーのいるディメンション）
        ServerLevel level = player.serverLevel();
        long dayTime = level.getDayTime();
        long t = Math.floorMod(dayTime, 24000L);
        long minutes = ((t + 6000L) % 24000L) * 1440L / 24000L;
        String weather = level.isThundering() ? "雷雨" : level.isRaining() ? "雨" : "晴れ";
        sb.append("\n時間: ").append(dayTime / 24000L + 1).append("日目 ")
                .append(String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60))
                .append("（").append(t < 12000 ? "昼" : t < 13000 ? "夕方" : t < 23000 ? "夜" : "明け方").append("）")
                .append("、天気 ").append(weather);

        // オンラインのプレイヤー
        List<String> others = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            others.add(p.getGameProfile().getName() + "（" + dimension(p) + "）");
        }
        sb.append("\nオンラインのプレイヤー（").append(others.size()).append("人）: ").append(String.join("、", others));

        // 使えるコマンド
        sb.append("\n").append(commands(player));
        return sb.toString();
    }

    private static String commands(ServerPlayer player) {
        boolean canRun = CommandRunner.allowed(false) || CommandRunner.allowed(true);
        int level = canRun ? CommandRunner.levelFor(player)
                : player.getServer().getProfilePermissions(player.getGameProfile());
        CommandSourceStack src = player.createCommandSourceStack().withPermission(level).withSuppressedOutput();
        Set<String> blocked = CommandRunner.blocked();
        List<String> names = new ArrayList<>();
        player.getServer().getCommands().getDispatcher().getRoot().getChildren().forEach(node -> {
            String n = node.getName();
            if (n.contains(":") || blocked.contains(n)) return;
            if (node.canUse(src)) names.add(n);
        });
        names.sort(String::compareTo);
        return (canRun ? "あなたが実行できるコマンド（権限レベル " + level + "）: "
                : "このプレイヤーが使えるコマンド（あなたは実行できません）: ")
                + String.join(", ", names);
    }

    private static String effects(ServerPlayer player) {
        List<String> list = new ArrayList<>();
        for (MobEffectInstance e : player.getActiveEffects()) {
            String dur = e.isInfiniteDuration() ? "無限" : (e.getDuration() / 20) + "秒";
            list.add(effectName(e) + " " + (e.getAmplifier() + 1) + "（残り" + dur + "）");
        }
        return list.isEmpty() ? "なし" : String.join("、", list);
    }

    private static String effectName(MobEffectInstance e) {
        return e.getEffect().value().getDisplayName().getString();
    }

    private static String dimension(ServerPlayer p) {
        return p.level().dimension().location().toString();
    }

    private static String fmt(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v);
    }
}
