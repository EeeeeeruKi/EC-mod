package net.ramune1256.hijikimod.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** ひじき豆。右クリックで声が鳴って、目の前にランダムなモブが1体出る。 */
public class SoundItem extends Item {
    /** 出すと危なすぎるボスは候補から外す */
    private static final List<EntityType<?>> EXCLUDED = List.of(EntityType.ENDER_DRAGON, EntityType.WITHER);
    private static List<EntityType<?>> candidates;

    public SoundItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSoundEvents.HIJIKI_VOICE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                summonRandomMob(serverLevel, player);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void summonRandomMob(ServerLevel level, Player player) {
        List<EntityType<?>> list = getCandidates(level);
        if (list.isEmpty()) return;
        EntityType<?> type = list.get(level.getRandom().nextInt(list.size()));
        Vec3 look = player.getViewVector(1.0F);
        BlockPos pos = BlockPos.containing(player.getX() + look.x * 2.0D, player.getY(), player.getZ() + look.z * 2.0D);
        type.spawn(level, pos, MobSpawnType.MOB_SUMMONED);
    }

    /** /summon で出せるモブ（Mob を継承しているもの）を全部集める。他のmodのモブも入る。 */
    private static synchronized List<EntityType<?>> getCandidates(ServerLevel level) {
        if (candidates != null) return candidates;
        List<EntityType<?>> found = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!type.canSummon() || EXCLUDED.contains(type)) continue;
            try {
                Entity test = type.create(level);
                if (test instanceof Mob) found.add(type);
                if (test != null) test.discard();
            } catch (Throwable ignored) {
                // 作るのに失敗するモブは候補に入れない
            }
        }
        candidates = List.copyOf(found);
        return candidates;
    }
}
