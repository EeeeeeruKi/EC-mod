package com.eeeeeeruki.skyriders.block;

import com.eeeeeeruki.skyriders.SkyRiders;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** レールの描画用（データは持たない） */
public class RailBlockEntity extends BlockEntity {
    public RailBlockEntity(BlockPos pos, BlockState state) {
        super(SkyRiders.RAIL_BLOCK_ENTITY, pos, state);
    }
}
