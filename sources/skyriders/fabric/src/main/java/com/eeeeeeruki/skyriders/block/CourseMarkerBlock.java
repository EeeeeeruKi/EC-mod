package com.eeeeeeruki.skyriders.block;

import com.eeeeeeruki.skyriders.course.CourseState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * ゴール判定ブロック／チェックポイントブロック。マシンで通り抜けられる（当たり判定なし）。
 * 置いた人が管理画面で選んでいるコース（チェックポイントは番号も）に入る。
 */
public class CourseMarkerBlock extends Block {
    private final boolean goal;

    public CourseMarkerBlock(Settings settings, boolean goal) {
        super(settings);
        this.goal = goal;
    }

    public boolean isGoal() {
        return this.goal;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!(world instanceof ServerWorld sw)) return;
        CourseState cs = CourseState.get(sw.getServer());
        String course = CourseState.DEFAULT_COURSE;
        int number = 1;
        if (placer instanceof PlayerEntity p) {
            course = cs.selectedCourse(p.getUuid());
            number = cs.selectedNumber(p.getUuid());
        }
        cs.putMarker(world, pos, new CourseState.Marker(course, this.goal ? 0 : number, this.goal));
        if (placer instanceof PlayerEntity p) {
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld sw) {
            CourseState.get(sw.getServer()).removeMarker(world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    /** 同じブロックどうしの境目は描かない（ガラスと同じ） */
    @Override
    protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        return stateFrom.isOf(this) || super.isSideInvisible(state, stateFrom, direction);
    }
}
