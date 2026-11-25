package com.eeeeeeruki.skyriders.item;

import com.eeeeeeruki.skyriders.entity.MachineEntity;
import com.eeeeeeruki.skyriders.entity.PatchKind;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** パッチ。マシンで触れるか、乗ったまま右クリックで使う。kind が null ならオール（全部+1） */
public class PatchItem extends Item {
    private static final String FAKE_KEY = "SkyRidersFake";
    @Nullable
    private final PatchKind kind;

    public PatchItem(Settings settings, @Nullable PatchKind kind) {
        super(settings);
        this.kind = kind;
    }

    @Nullable
    public PatchKind kind() {
        return this.kind;
    }

    public int color() {
        return this.kind == null ? 0xFFFFFF : this.kind.color;
    }

    /** 偽パッチ：見た目は普通のパッチと同じ */
    public static boolean isFake(ItemStack stack) {
        NbtComponent c = stack.get(DataComponentTypes.CUSTOM_DATA);
        return c != null && c.copyNbt().getBoolean(FAKE_KEY);
    }

    public static ItemStack makeFake(Item item) {
        ItemStack stack = new ItemStack(item);
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean(FAKE_KEY, true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        return stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getVehicle() instanceof MachineEntity machine) {
            if (!world.isClient) {
                machine.applyPatch(stack, user);
                if (!user.getAbilities().creativeMode) stack.decrement(1);
            }
            return TypedActionResult.success(stack, world.isClient);
        }
        return TypedActionResult.pass(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.skyriders.patch").formatted(Formatting.GRAY));
    }
}
