package io.github.eeeeeeruki.ponderererer.world;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.render.VirtualRenderHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Ponder draws every block with an empty model data, so blocks whose looks come from their block entity
 * (Mekanism cable connections, energy cube sides) are always drawn in their default state. Positions marked
 * here are drawn with their block entity's model data instead (still flagged as virtual, like Ponder does).
 * Only marked positions change, so other mods' scenes look exactly as before.
 */
public final class LiveModelData {
    private static final Map<PonderLevel, Set<BlockPos>> LIVE = Collections.synchronizedMap(new WeakHashMap<>());

    private LiveModelData() {}

    public static void mark(PonderLevel level, BlockPos pos) {
        LIVE.computeIfAbsent(level, l -> Collections.synchronizedSet(new HashSet<>())).add(pos.immutable());
    }

    /** Model data to draw {@code pos} with, or null to let Ponder do what it normally does. */
    public static ModelData get(BlockAndTintGetter level, BlockPos pos) {
        if (!(level instanceof PonderLevel ponder)) return null;
        Set<BlockPos> set = LIVE.get(ponder);
        if (set == null || !set.contains(pos)) return null;
        BlockEntity be = ponder.getBlockEntity(pos);
        if (be == null) return null;
        try {
            return be.getModelData().derive().with(VirtualRenderHelper.VIRTUAL_PROPERTY, true).build();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
