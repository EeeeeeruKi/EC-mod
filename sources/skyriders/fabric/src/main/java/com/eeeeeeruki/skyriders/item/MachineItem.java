package com.eeeeeeruki.skyriders.item;

import com.eeeeeeruki.skyriders.SkyRiders;
import com.eeeeeeruki.skyriders.entity.MachineEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** 地面に右クリックで仮マシンを置くアイテム */
public class MachineItem extends Item {
    public MachineItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;

        Vec3d pos = ctx.getSide() == Direction.UP
                ? ctx.getHitPos()
                : Vec3d.ofBottomCenter(ctx.getBlockPos().offset(ctx.getSide()));
        PlayerEntity player = ctx.getPlayer();
        float yaw = player != null ? player.getYaw() : 0f;

        MachineEntity machine = new MachineEntity(SkyRiders.MACHINE, world);
        machine.refreshPositionAndAngles(pos.x, pos.y + 0.05, pos.z, yaw, 0f);
        if (!world.isSpaceEmpty(machine, machine.getBoundingBox())) return ActionResult.FAIL;

        world.spawnEntity(machine);
        if (player == null || !player.getAbilities().creativeMode) {
            ctx.getStack().decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
