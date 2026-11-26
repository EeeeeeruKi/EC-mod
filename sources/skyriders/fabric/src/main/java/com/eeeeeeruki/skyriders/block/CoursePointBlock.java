package com.eeeeeeruki.skyriders.block;

import com.eeeeeeruki.skyriders.course.CourseState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * スタート地点／TA地点。床に置く薄い板で、マシンで上を走れる。
 * 置いた時に向いていた方向が、ワープした時のマシンの向きになる。
 */
public class CoursePointBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 1, 16);
    private final boolean ta;

    public CoursePointBlock(Settings settings, boolean ta) {
        super(settings);
        this.ta = ta;
        this.setDefaultState(this.getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
    }

    public boolean isTa() {
        return this.ta;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!(world instanceof ServerWorld sw)) return;
        CourseState cs = CourseState.get(sw.getServer());
        String course = placer instanceof PlayerEntity p ? cs.selectedCourse(p.getUuid()) : CourseState.DEFAULT_COURSE;
        cs.putPoint(course, this.ta, world, pos, state.get(FACING).asRotation());
        if (placer instanceof PlayerEntity p) {
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld sw) {
            CourseState.get(sw.getServer()).removePoint(world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
