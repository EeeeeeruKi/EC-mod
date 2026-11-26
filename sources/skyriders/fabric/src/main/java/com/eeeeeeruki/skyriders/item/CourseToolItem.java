package com.eeeeeeruki.skyriders.item;

import com.eeeeeeruki.skyriders.block.CourseMarkerBlock;
import com.eeeeeeruki.skyriders.course.CourseState;
import com.eeeeeeruki.skyriders.course.RaceManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * コース編集ツール。
 * 右クリック：管理画面を開く。ゴール／チェックポイントを右クリック：どのコースの何番か表示。
 * スニークしながらブロックを右クリック：管理画面で選んでいるコース（と番号）に付け替える。
 */
public class CourseToolItem extends Item {
    public CourseToolItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user instanceof ServerPlayerEntity sp) RaceManager.sendScreen(sp);
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockState state = world.getBlockState(ctx.getBlockPos());
        PlayerEntity player = ctx.getPlayer();
        if (!(state.getBlock() instanceof CourseMarkerBlock block)) {
            if (player instanceof ServerPlayerEntity sp) RaceManager.sendScreen(sp);
            return ActionResult.success(world.isClient);
        }
        if (!(world instanceof ServerWorld sw) || player == null) return ActionResult.SUCCESS;

        CourseState cs = CourseState.get(sw.getServer());
        if (player.isSneaking()) {
            String course = cs.selectedCourse(player.getUuid());
            int number = cs.selectedNumber(player.getUuid());
            cs.putMarker(world, ctx.getBlockPos(), new CourseState.Marker(course, block.isGoal() ? 0 : number, block.isGoal()));
            player.sendMessage(block.isGoal()
                    ? Text.translatable("message.skyriders.goal_set", course)
                    : Text.translatable("message.skyriders.checkpoint_set", course, number), true);
        } else {
            CourseState.Marker m = cs.marker(world, ctx.getBlockPos());
            String course = m != null ? m.course() : CourseState.DEFAULT_COURSE;
            player.sendMessage(block.isGoal()
                    ? Text.translatable("message.skyriders.goal_info", course)
                    : Text.translatable("message.skyriders.checkpoint_info", course, m != null ? m.number() : 1), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.skyriders.course_tool_1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.skyriders.course_tool_2").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.skyriders.course_tool_3").formatted(Formatting.GRAY));
    }
}
