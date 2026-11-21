package net.ramune1256.hijikimod.item;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * 右クリックで自分を倒すアイテム。
 * Hyperlink / Hyperlink-E（modid: hyperdaimc）が入っていれば、
 * ノベルカリバーの攻撃（novelize）を自分に当ててから kill する。
 */
public class IriItem extends Item {
    private static final String HYPERLINK_ID = "hyperdaimc";
    private static final String NOVEL_HANDLER = "com.sakurafuld.hyperdaimc.content.hyper.novel.system.NovelHandler";

    private static boolean lookedUp = false;
    private static Method novelize;
    private static Method playSound;

    public IriItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            novelCaliburSelf(serverLevel, player, hand);
            player.kill();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void novelCaliburSelf(ServerLevel level, Player player, InteractionHand hand) {
        if (!ModList.get().isLoaded(HYPERLINK_ID)) return;
        lookUp();
        if (novelize == null) return;
        try {
            player.swing(hand, true);
            novelize.invoke(null, player, player, true);
            if (playSound != null) playSound.invoke(null, level, player.position());
        } catch (Throwable ignored) {
            // Hyperlink 側の仕様が変わっていたら普通の kill だけにする
        }
    }

    private static synchronized void lookUp() {
        if (lookedUp) return;
        lookedUp = true;
        try {
            Class<?> handler = Class.forName(NOVEL_HANDLER);
            novelize = handler.getMethod("novelize", LivingEntity.class, Entity.class, boolean.class);
            try {
                playSound = handler.getMethod("playSound", ServerLevel.class, Vec3.class);
            } catch (NoSuchMethodException ignored) {
                playSound = null;
            }
        } catch (Throwable t) {
            novelize = null;
        }
    }
}
