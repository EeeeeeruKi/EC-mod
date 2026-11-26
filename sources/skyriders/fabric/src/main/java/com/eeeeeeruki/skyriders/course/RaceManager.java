package com.eeeeeeruki.skyriders.course;

import com.eeeeeeruki.skyriders.block.CourseMarkerBlock;
import com.eeeeeeruki.skyriders.entity.MachineEntity;
import com.eeeeeeruki.skyriders.net.CourseScreenPayload;
import com.eeeeeeruki.skyriders.net.RaceStatePayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * レースの判定。マシンに乗ってゴールを通るとスタート。
 * チェックポイントを 1, 2, 3… の順に全部通ってからゴールを通ると1周。周回数を回るとゴール。
 */
public final class RaceManager {
    private RaceManager() {
    }

    private static final int GOAL_COOLDOWN = 40;
    private static final int WARN_COOLDOWN = 40;

    private static final class Progress {
        String course;
        int lap = 1;
        int laps;
        int next = 1;   // 次に通るチェックポイントの番号
        int total;      // チェックポイントの数（一番大きい番号）
        int startTick;
    }

    private static final Map<UUID, Progress> RACES = new HashMap<>();
    private static final Map<UUID, Vec3d> LAST_POS = new HashMap<>();
    private static final Map<UUID, Integer> GOAL_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Integer> WARN_COOLDOWNS = new HashMap<>();

    public static void tick(MinecraftServer server) {
        int now = server.getTicks();
        GOAL_COOLDOWNS.replaceAll((k, v) -> v - 1);
        GOAL_COOLDOWNS.values().removeIf(v -> v <= 0);
        WARN_COOLDOWNS.replaceAll((k, v) -> v - 1);
        WARN_COOLDOWNS.values().removeIf(v -> v <= 0);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            if (!(player.getVehicle() instanceof MachineEntity machine)) {
                LAST_POS.remove(id);
                if (RACES.remove(id) != null) {
                    player.sendMessage(Text.translatable("message.skyriders.race_cancel_dismount"), false);
                    sendState(player, null, now);
                }
                continue;
            }
            Vec3d pos = machine.getPos();
            Vec3d last = LAST_POS.getOrDefault(id, pos);
            LAST_POS.put(id, pos);
            if (last.squaredDistanceTo(pos) > 400) last = pos; // テレポートした時は線でつながない

            // 前のtickの位置から今の位置まで細かく調べる（速くてもすり抜けないように）
            ServerWorld world = (ServerWorld) machine.getWorld();
            CourseState cs = CourseState.get(server);
            double dist = last.distanceTo(pos);
            int steps = Math.min(30, Math.max(1, (int) Math.ceil(dist / 0.4)));
            BlockPos prevHit = null;
            for (int i = 1; i <= steps; i++) {
                Vec3d p = last.lerp(pos, i / (double) steps);
                for (double h : new double[]{0.25, 1.2}) {
                    BlockPos bp = BlockPos.ofFloored(p.x, p.y + h, p.z);
                    if (bp.equals(prevHit)) continue;
                    BlockState st = world.getBlockState(bp);
                    if (!(st.getBlock() instanceof CourseMarkerBlock block)) continue;
                    prevHit = bp;
                    CourseState.Marker m = cs.marker(world, bp);
                    if (m == null) m = new CourseState.Marker(CourseState.DEFAULT_COURSE, block.isGoal() ? 0 : 1, block.isGoal());
                    if (m.goal()) onGoal(player, cs, m.course(), now);
                    else onCheckpoint(player, m.course(), m.number(), now);
                }
            }
        }
    }

    private static void onGoal(ServerPlayerEntity player, CourseState cs, String course, int now) {
        UUID id = player.getUuid();
        if (GOAL_COOLDOWNS.containsKey(id)) return;
        GOAL_COOLDOWNS.put(id, GOAL_COOLDOWN);
        Progress p = RACES.get(id);

        if (p == null || !p.course.equals(course)) {
            // スタート
            TreeSet<Integer> nums = cs.checkpointNumbers(course);
            if (nums.isEmpty()) {
                player.sendMessage(Text.translatable("message.skyriders.race_no_checkpoint", course).formatted(Formatting.YELLOW), false);
                return;
            }
            p = new Progress();
            p.course = course;
            p.laps = cs.course(course).laps;
            p.total = nums.last();
            p.startTick = now;
            RACES.put(id, p);
            title(player, Text.translatable("title.skyriders.race_start"), Text.translatable("title.skyriders.race_start_sub", course, p.laps));
            sound(player, 1.0f);
            sendState(player, p, now);
            return;
        }

        if (p.next <= p.total) {
            warn(player, Text.translatable("message.skyriders.race_need_all"));
            return;
        }
        if (p.lap >= p.laps) {
            // ゴール
            int time = now - p.startTick;
            RACES.remove(id);
            boolean best = cs.addRecord(course, player.getGameProfile().getName(), time);
            title(player, Text.translatable("title.skyriders.race_finish"), Text.literal(formatTime(time)));
            player.sendMessage(Text.translatable("message.skyriders.race_finish", course, formatTime(time)).formatted(Formatting.GOLD), false);
            if (best) player.sendMessage(Text.translatable("message.skyriders.race_best").formatted(Formatting.YELLOW), false);
            player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
            sendState(player, null, now);
            return;
        }
        p.lap++;
        p.next = 1;
        title(player, Text.empty(), Text.translatable("title.skyriders.race_lap", p.lap, p.laps));
        sound(player, 1.4f);
        sendState(player, p, now);
    }

    private static void onCheckpoint(ServerPlayerEntity player, String course, int number, int now) {
        Progress p = RACES.get(player.getUuid());
        if (p == null || !p.course.equals(course)) return;
        if (number == p.next) {
            p.next++;
            player.sendMessage(Text.translatable("message.skyriders.race_checkpoint", number, p.total), true);
            sound(player, 1.8f);
            sendState(player, p, now);
        } else if (number > p.next) {
            warn(player, Text.translatable("message.skyriders.race_missed", p.next));
        }
    }

    /** 管理画面から：自分のレースをやめる */
    public static void cancel(ServerPlayerEntity player) {
        if (RACES.remove(player.getUuid()) != null) {
            player.sendMessage(Text.translatable("message.skyriders.race_cancel"), false);
            sendState(player, null, player.getServer().getTicks());
        }
    }

    public static boolean isRacing(ServerPlayerEntity player) {
        return RACES.containsKey(player.getUuid());
    }

    private static void warn(ServerPlayerEntity player, Text text) {
        if (WARN_COOLDOWNS.containsKey(player.getUuid())) return;
        WARN_COOLDOWNS.put(player.getUuid(), WARN_COOLDOWN);
        player.sendMessage(text.copy().formatted(Formatting.RED), true);
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1f, 0.6f);
    }

    private static void sound(ServerPlayerEntity player, float pitch) {
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1f, pitch);
    }

    private static void title(ServerPlayerEntity player, Text title, Text subtitle) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(2, 30, 8));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        player.networkHandler.sendPacket(new TitleS2CPacket(title));
    }

    private static void sendState(ServerPlayerEntity player, Progress p, int now) {
        if (!ServerPlayNetworking.canSend(player, RaceStatePayload.ID)) return;
        ServerPlayNetworking.send(player, p == null
                ? new RaceStatePayload(false, "", 0, 0, 0, 0, 0)
                : new RaceStatePayload(true, p.course, p.lap, p.laps, p.next, p.total, now - p.startTick));
    }

    /** 0:12.34 の形 */
    public static String formatTime(int ticks) {
        int ms = ticks * 50;
        return String.format("%d:%02d.%02d", ms / 60000, (ms / 1000) % 60, (ms % 1000) / 10);
    }

    // ===== 管理画面 =====

    /** 管理画面の中身を送る */
    public static void sendScreen(ServerPlayerEntity player) {
        CourseState cs = CourseState.get(player.getServer());
        UUID id = player.getUuid();
        String selected = cs.selectedCourse(id);
        cs.course(selected);
        JsonObject root = new JsonObject();
        root.addProperty("selected", selected);
        root.addProperty("number", cs.selectedNumber(id));
        root.addProperty("racing", isRacing(player));
        JsonArray list = new JsonArray();
        for (CourseState.Course c : cs.courses.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", c.name);
            o.addProperty("laps", c.laps);
            o.addProperty("goals", cs.goalCount(c.name));
            TreeSet<Integer> nums = cs.checkpointNumbers(c.name);
            JsonArray cps = new JsonArray();
            JsonArray missing = new JsonArray();
            for (int n : nums) cps.add(n);
            if (!nums.isEmpty()) for (int n = 1; n < nums.last(); n++) if (!nums.contains(n)) missing.add(n);
            o.add("checkpoints", cps);
            o.add("missing", missing);
            JsonArray recs = new JsonArray();
            for (CourseState.Record r : c.records) {
                JsonObject ro = new JsonObject();
                ro.addProperty("player", r.player());
                ro.addProperty("time", formatTime(r.ticks()));
                recs.add(ro);
            }
            o.add("records", recs);
            list.add(o);
        }
        root.add("courses", list);
        ServerPlayNetworking.send(player, new CourseScreenPayload(root.toString()));
    }

    /** 管理画面での操作 */
    public static void handleEdit(ServerPlayerEntity player, int action, String text, int value) {
        CourseState cs = CourseState.get(player.getServer());
        UUID id = player.getUuid();
        String course = cs.selectedCourse(id);
        switch (action) {
            case com.eeeeeeruki.skyriders.net.CourseEditPayload.SELECT -> {
                String name = text.strip();
                if (!name.isEmpty() && name.length() <= 32) cs.select(id, name);
            }
            case com.eeeeeeruki.skyriders.net.CourseEditPayload.LAPS -> {
                CourseState.Course c = cs.course(course);
                c.laps = Math.max(1, Math.min(99, c.laps + value));
                cs.markDirty();
            }
            case com.eeeeeeruki.skyriders.net.CourseEditPayload.NUMBER -> cs.setNumber(id, cs.selectedNumber(id) + value);
            case com.eeeeeeruki.skyriders.net.CourseEditPayload.RESET_RECORDS -> {
                cs.course(course).records.clear();
                cs.markDirty();
            }
            case com.eeeeeeruki.skyriders.net.CourseEditPayload.CANCEL_RACE -> cancel(player);
            default -> {
            }
        }
        sendScreen(player);
    }
}
