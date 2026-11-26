package com.eeeeeeruki.skyriders.item;

import com.eeeeeeruki.skyriders.block.RailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * レールを置くアイテム。lateral（-1左 / 0 / +1右）と vertical（-1下 / 0 / +1上）で、
 * ブロックの中のどこに寄せるかが決まる。左右は置いた時の自分の向きから見た左右。
 */
public class RailBlockItem extends BlockItem {
    private final int lateral;
    private final int vertical;

    public RailBlockItem(Block block, Settings settings, int lateral, int vertical) {
        super(block, settings);
        this.lateral = lateral;
        this.vertical = vertical;
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState base = super.getPlacementState(ctx);
        if (base == null) return null;
        Direction facing = ctx.getHorizontalPlayerFacing();
        Direction right = facing.rotateYClockwise(); // 南を向いている時、右は西
        int dx = right.getOffsetX() * this.lateral;
        int dz = right.getOffsetZ() * this.lateral;
        return base.with(RailBlock.FACING, facing)
                .with(RailBlock.OX, 1 + dx)
                .with(RailBlock.OY, 1 + this.vertical)
                .with(RailBlock.OZ, 1 + dz);
    }
}
