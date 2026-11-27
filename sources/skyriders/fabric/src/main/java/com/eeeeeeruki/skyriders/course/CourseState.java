package com.eeeeeeruki.skyriders.course;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * コースの保存データ（ワールドごと）。
 * ゴール・チェックポイントの場所と番号、コースの周回数と記録、プレイヤーが選んでいるコースと番号。
 */
public class CourseState extends PersistentState {
    public static final String DEFAULT_COURSE = "course";
    private static final int MAX_RECORDS = 10;

    public static final PersistentState.Type<CourseState> TYPE =
            new PersistentState.Type<>(CourseState::new, CourseState::fromNbt, null);

    public static class Course {
        public final String name;
        public int laps = 3;
        public String bgm = "";  // 音のID（空ならなし）
        @Nullable
        public Spot start;  // スタート地点
        @Nullable
        public Spot ta;     // TA地点
        public final List<Record> records = new ArrayList<>();

        Course(String name) {
            this.name = name;
        }
    }

    public record Record(String player, int ticks) {
    }

    /** スタート地点・TA地点：ディメンション・場所・向き */
    public record Spot(String dim, long pos, float yaw) {
        public BlockPos blockPos() {
            return BlockPos.fromLong(this.pos);
        }

        NbtCompound toNbt() {
            NbtCompound t = new NbtCompound();
            t.putString("Dim", this.dim);
            t.putLong("Pos", this.pos);
            t.putFloat("Yaw", this.yaw);
            return t;
        }

        static Spot fromNbt(NbtCompound t) {
            return new Spot(t.getString("Dim"), t.getLong("Pos"), t.getFloat("Yaw"));
        }
    }

    /** スタート地点（ta=false）かTA地点（ta=true）を置く。同じ場所が別のコースにあれば外す */
    public void putPoint(String course, boolean ta, World world, BlockPos pos, float yaw) {
        removePoint(world, pos);
        Spot spot = new Spot(world.getRegistryKey().getValue().toString(), pos.asLong(), yaw);
        if (ta) course(course).ta = spot;
        else course(course).start = spot;
        this.markDirty();
    }

    public void removePoint(World world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        long p = pos.asLong();
        for (Course c : this.courses.values()) {
            if (c.start != null && c.start.dim().equals(dim) && c.start.pos() == p) {
                c.start = null;
                this.markDirty();
            }
            if (c.ta != null && c.ta.dim().equals(dim) && c.ta.pos() == p) {
                c.ta = null;
                this.markDirty();
            }
        }
    }

    /** その場所がどのコースのスタート地点／TA地点か（無ければ null） */
    @Nullable
    public String pointOwner(World world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        long p = pos.asLong();
        for (Course c : this.courses.values()) {
            if ((c.start != null && c.start.dim().equals(dim) && c.start.pos() == p)
                    || (c.ta != null && c.ta.dim().equals(dim) && c.ta.pos() == p)) return c.name;
        }
        return null;
    }

    /** number は チェックポイントの番号（ゴールは 0） */
    public record Marker(String course, int number, boolean goal) {
    }

    public final Map<String, Course> courses = new LinkedHashMap<>();
    public final Map<String, Marker> markers = new HashMap<>();
    private final Map<UUID, String> selectedCourse = new HashMap<>();
    private final Map<UUID, Integer> selectedNumber = new HashMap<>();

    public static CourseState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "skyriders_courses");
    }

    public static String key(World world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "|" + pos.asLong();
    }

    public Course course(String name) {
        return this.courses.computeIfAbsent(name, Course::new);
    }

    // ===== 選んでいるコース・番号 =====

    public String selectedCourse(UUID player) {
        return this.selectedCourse.getOrDefault(player, DEFAULT_COURSE);
    }

    public int selectedNumber(UUID player) {
        return this.selectedNumber.getOrDefault(player, 1);
    }

    public void select(UUID player, String course) {
        this.selectedCourse.put(player, course);
        course(course);
        this.markDirty();
    }

    public void setNumber(UUID player, int number) {
        this.selectedNumber.put(player, Math.max(1, Math.min(99, number)));
        this.markDirty();
    }

    // ===== ブロック =====

    public void putMarker(World world, BlockPos pos, Marker marker) {
        this.markers.put(key(world, pos), marker);
        course(marker.course());
        this.markDirty();
    }

    public void removeMarker(World world, BlockPos pos) {
        if (this.markers.remove(key(world, pos)) != null) this.markDirty();
    }

    @Nullable
    public Marker marker(World world, BlockPos pos) {
        return this.markers.get(key(world, pos));
    }

    /** コースにあるチェックポイントの番号（重なりなし、小さい順） */
    public TreeSet<Integer> checkpointNumbers(String course) {
        TreeSet<Integer> set = new TreeSet<>();
        for (Marker m : this.markers.values()) {
            if (!m.goal() && m.course().equals(course)) set.add(m.number());
        }
        return set;
    }

    public int goalCount(String course) {
        int n = 0;
        for (Marker m : this.markers.values()) if (m.goal() && m.course().equals(course)) n++;
        return n;
    }

    /** 記録を足す。ベスト更新なら true */
    public boolean addRecord(String course, String player, int ticks) {
        Course c = course(course);
        boolean best = c.records.isEmpty() || ticks < c.records.get(0).ticks();
        c.records.add(new Record(player, ticks));
        c.records.sort((a, b) -> Integer.compare(a.ticks(), b.ticks()));
        while (c.records.size() > MAX_RECORDS) c.records.remove(c.records.size() - 1);
        this.markDirty();
        return best;
    }

    // ===== 保存 =====

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList cl = new NbtList();
        for (Course c : this.courses.values()) {
            NbtCompound t = new NbtCompound();
            t.putString("Name", c.name);
            t.putInt("Laps", c.laps);
            t.putString("Bgm", c.bgm);
            NbtList rl = new NbtList();
            for (Record r : c.records) {
                NbtCompound rt = new NbtCompound();
                rt.putString("Player", r.player());
                rt.putInt("Ticks", r.ticks());
                rl.add(rt);
            }
            t.put("Records", rl);
            if (c.start != null) t.put("Start", c.start.toNbt());
            if (c.ta != null) t.put("Ta", c.ta.toNbt());
            cl.add(t);
        }
        nbt.put("Courses", cl);

        NbtList ml = new NbtList();
        for (Map.Entry<String, Marker> e : this.markers.entrySet()) {
            NbtCompound t = new NbtCompound();
            t.putString("Key", e.getKey());
            t.putString("Course", e.getValue().course());
            t.putInt("Number", e.getValue().number());
            t.putBoolean("Goal", e.getValue().goal());
            ml.add(t);
        }
        nbt.put("Markers", ml);

        NbtList sl = new NbtList();
        for (Map.Entry<UUID, String> e : this.selectedCourse.entrySet()) {
            NbtCompound t = new NbtCompound();
            t.putUuid("Player", e.getKey());
            t.putString("Course", e.getValue());
            t.putInt("Number", this.selectedNumber(e.getKey()));
            sl.add(t);
        }
        nbt.put("Selected", sl);
        return nbt;
    }

    public static CourseState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        CourseState s = new CourseState();
        for (NbtElement el : nbt.getList("Courses", NbtElement.COMPOUND_TYPE)) {
            NbtCompound t = (NbtCompound) el;
            Course c = s.course(t.getString("Name"));
            c.laps = Math.max(1, t.getInt("Laps"));
            c.bgm = t.getString("Bgm");
            if (t.contains("Start")) c.start = Spot.fromNbt(t.getCompound("Start"));
            if (t.contains("Ta")) c.ta = Spot.fromNbt(t.getCompound("Ta"));
            for (NbtElement re : t.getList("Records", NbtElement.COMPOUND_TYPE)) {
                NbtCompound rt = (NbtCompound) re;
                c.records.add(new Record(rt.getString("Player"), rt.getInt("Ticks")));
            }
        }
        for (NbtElement el : nbt.getList("Markers", NbtElement.COMPOUND_TYPE)) {
            NbtCompound t = (NbtCompound) el;
            s.markers.put(t.getString("Key"), new Marker(t.getString("Course"), t.getInt("Number"), t.getBoolean("Goal")));
        }
        for (NbtElement el : nbt.getList("Selected", NbtElement.COMPOUND_TYPE)) {
            NbtCompound t = (NbtCompound) el;
            s.selectedCourse.put(t.getUuid("Player"), t.getString("Course"));
            s.selectedNumber.put(t.getUuid("Player"), t.getInt("Number"));
        }
        return s;
    }
}
