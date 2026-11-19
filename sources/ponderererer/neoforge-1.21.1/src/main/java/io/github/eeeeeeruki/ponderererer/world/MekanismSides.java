package io.github.eeeeeeruki.ponderererer.world;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Changes how the sides of a Mekanism machine look (input / output colours, energy cube side lights),
 * through the block entity's normal update tag. Looks only: nothing is simulated in a Ponder scene.
 */
public final class MekanismSides {
    // TransmissionType order in Mekanism
    public static final int ENERGY = 0, FLUID = 1, CHEMICAL = 2, ITEM = 3, HEAT = 4;
    // DataType order in Mekanism
    public static final int NONE = 0, INPUT = 1, INPUT_1 = 2, INPUT_2 = 3, OUTPUT = 4, OUTPUT_1 = 5, OUTPUT_2 = 6,
            INPUT_OUTPUT = 7, ENERGY_SLOT = 8, EXTRA = 9;

    private MekanismSides() {}

    /** Sets all six sides (front, left, right, back, top, bottom) of one transmission type to {@code dataType}. */
    public static boolean setAll(PonderLevel world, BlockPos pos, int transmission, int dataType) {
        return set(world, pos, transmission, new int[] {dataType, dataType, dataType, dataType, dataType, dataType});
    }

    /** Sets each side in Mekanism's order: front, left, right, back, top, bottom. */
    public static boolean set(PonderLevel world, BlockPos pos, int transmission, int[] sides) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null || sides.length != 6) return false;
        CompoundTag config = new CompoundTag();
        config.put("config" + transmission, new IntArrayTag(sides.clone()));
        CompoundTag tag = new CompoundTag();
        tag.put("component_config", config);
        try {
            be.handleUpdateTag(tag, world.registryAccess());
            be.requestModelDataUpdate();
        } catch (RuntimeException e) {
            return false;
        }
        LiveModelData.mark(world, pos);
        return true;
    }
}
