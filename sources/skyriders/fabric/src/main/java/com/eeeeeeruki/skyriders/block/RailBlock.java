package com.eeeeeeruki.skyriders.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

/**
 * レール。細い線で、ブロックの中の9か所（真ん中・上下左右・ななめ）のどこかを通る。
 * 見た目は RailRenderer が、となりのレールとつないだ曲線で描く。当たり判定はない（マシンはグラインドする）。
 */
public class RailBlock extends Block implements BlockEntityProvider {
    public static final IntProperty OX = IntProperty.of("ox", 0, 2);
    public static final IntProperty OY = IntProperty.of("oy", 0, 2);
    public static final IntProperty OZ = IntProperty.of("oz", 0, 2);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public RailBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(OX, 1).with(OY, 1).with(OZ, 1).with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(OX, OY, OZ, FACING);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Vec3d a = RailGeometry.anchor(state, BlockPos.ORIGIN);
        double r = 0.16;
        return VoxelShapes.cuboid(a.x - r, a.y - r, a.z - r, a.x + r, a.y + r, a.z + r);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RailBlockEntity(pos, state);
    }
}
