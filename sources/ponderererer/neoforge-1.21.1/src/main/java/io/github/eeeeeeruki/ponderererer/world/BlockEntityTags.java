package io.github.eeeeeeruki.ponderererer.world;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Changes how a block entity looks by handing it an update tag, the same way a client gets changes from the server:
 * painted transporters, turbine blades and anything else a block entity reads from its update tag.
 */
public final class BlockEntityTags {
    private BlockEntityTags() {
    }

    /** Merges {@code extra} into the block entity's current update tag and applies it. Returns false if nothing changed. */
    public static boolean apply(PonderLevel world, BlockPos pos, CompoundTag extra) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return false;
        CompoundTag tag;
        try {
            tag = be.getUpdateTag(world.registryAccess());
        } catch (RuntimeException e) {
            tag = new CompoundTag();
        }
        tag.merge(extra.copy());
        try {
            be.handleUpdateTag(tag, world.registryAccess());
            be.requestModelDataUpdate();
            LiveModelData.mark(world, pos);
            return true;
        } catch (RuntimeException e) {
            return false; // never let one block entity break the scene
        }
    }
}
