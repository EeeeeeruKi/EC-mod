package net.ramune1256.hijikimod.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 右クリックすると、自分に弱体化（レベル＝これまで使った回数）を30秒かける。
 * 使った回数はバニラの統計「使用回数」に記録するので、死んでもリセットされない。
 */
public class YowaiyoItem extends Item {
    private static final int DURATION_TICKS = 30 * 20;
    private static final int MAX_LEVEL = 255;

    public YowaiyoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            int uses = serverPlayer.getStats().getValue(Stats.ITEM_USED.get(this));
            int effectLevel = Math.max(1, Math.min(uses, MAX_LEVEL));
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DURATION_TICKS, effectLevel - 1));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
