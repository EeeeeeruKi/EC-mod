package io.github.eeeeeeruki.ponderererer.world;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

/**
 * Light for a Ponder scene. Ponder normally draws everything at full brightness; once a scene turns this on,
 * the sky light drops to the given level and block light comes only from the sources the scene adds
 * (falling off by one per block, like real light, but without walls blocking it).
 * Cleared automatically when the scene restarts.
 */
public final class SceneLight {
    private record Source(BlockPos pos, int level) {
    }

    private static final class State {
        volatile int sky = 15;
        final List<Source> sources = new CopyOnWriteArrayList<>();
    }

    private static final Map<PonderLevel, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    private SceneLight() {
    }

    public static void setSky(PonderLevel level, int sky) {
        STATES.computeIfAbsent(level, l -> new State()).sky = Math.max(0, Math.min(15, sky));
    }

    public static void addSource(PonderLevel level, BlockPos pos, int light) {
        State s = STATES.computeIfAbsent(level, l -> new State());
        BlockPos p = pos.immutable();
        s.sources.removeIf(src -> src.pos().equals(p));
        if (light > 0) s.sources.add(new Source(p, Math.min(15, light)));
    }

    public static void clear(PonderLevel level) {
        STATES.remove(level);
    }

    /** The light at a position, or -1 when the scene does not control light (Ponder's own value is used). */
    public static int get(PonderLevel level, LightLayer layer, BlockPos pos) {
        State s = STATES.get(level);
        if (s == null) return -1;
        if (layer == LightLayer.SKY) return s.sky;
        int best = 0;
        for (Source src : s.sources) {
            BlockPos p = src.pos();
            int d = Math.abs(p.getX() - pos.getX()) + Math.abs(p.getY() - pos.getY()) + Math.abs(p.getZ() - pos.getZ());
            best = Math.max(best, src.level() - d);
        }
        return best;
    }
}
