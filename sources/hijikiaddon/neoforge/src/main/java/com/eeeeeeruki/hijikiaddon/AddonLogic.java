package com.eeeeeeruki.hijikiaddon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** iri と弱いよの装備・道具の効果（ローダーに関係ない部分）。 */
public final class AddonLogic {
    private static final String FLIGHT_TAG = "hijikiaddon_iri_flight";
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ARMOR_PIECES = {"helmet", "chestplate", "leggings", "boots"};
    /** 「強い弱体化」：レベル5を30秒 */
    private static final int WEAKNESS_TICKS = 30 * 20;
    private static final int WEAKNESS_AMPLIFIER = 4;

    private AddonLogic() {}

    private static String path(ItemStack stack) {
        if (stack.isEmpty()) return "";
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return HijikiAddon.MOD_ID.equals(key.getNamespace()) ? key.getPath() : "";
    }

    private static boolean isTool(ItemStack stack, String material) {
        String p = path(stack);
        return p.equals(material + "_sword") || p.equals(material + "_axe")
                || p.equals(material + "_pickaxe") || p.equals(material + "_paxel");
    }

    public static boolean isIriTool(ItemStack stack) { return isTool(stack, "iri"); }
    public static boolean isYowaiyoTool(ItemStack stack) { return isTool(stack, "yowaiyo"); }

    public static boolean fullSet(LivingEntity entity, String material) {
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            if (!path(entity.getItemBySlot(ARMOR_SLOTS[i])).equals(material + "_" + ARMOR_PIECES[i])) return false;
        }
        return true;
    }

    public static boolean fullIri(LivingEntity entity) { return fullSet(entity, "iri"); }

    private static MobEffectInstance strongWeakness() {
        return new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, WEAKNESS_AMPLIFIER);
    }

    /**
     * ダメージを受ける直前に呼ぶ。true を返したらそのダメージは無かったことにする。
     */
    public static boolean onIncomingDamage(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide()) return false;
        LivingEntity attacker = source.getDirectEntity() instanceof LivingEntity living && living != victim ? living : null;
        boolean victimIri = fullIri(victim);

        // iri の道具で殴った
        if (attacker != null && isIriTool(attacker.getMainHandItem())) {
            boolean attackerIri = fullIri(attacker);
            if (victimIri && attackerIri) {
                // どっちも iri 装備フルセット：音だけ鳴らして無効
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.6F);
                return true;
            }
            if (victimIri) {
                // 殴られた側だけ iri 装備：殴ったほうが即死
                attacker.kill();
                return true;
            }
            // 殴られた側が iri 装備じゃない：即死
            victim.kill();
            return true;
        }

        // iri 装備フルセットは無敵（/kill と奈落は除く）。触れて（殴って）きたやつは即死
        if (victimIri) {
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            if (attacker != null && !fullIri(attacker)) attacker.kill();
            return true;
        }

        // 弱いよ
        if (attacker != null && isYowaiyoTool(attacker.getMainHandItem())) {
            victim.addEffect(strongWeakness());
        }
        if (attacker != null && fullSet(victim, "yowaiyo")) {
            attacker.addEffect(strongWeakness());
        }
        return false;
    }

    /** 毎tick（サーバー側）呼ぶ。 */
    public static void onLivingTick(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        boolean iri = fullIri(entity);
        if (iri && entity.isAlive()) {
            // 触れたやつから即死
            for (LivingEntity other : entity.level().getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(0.05D),
                    o -> o != entity && o.isAlive() && !o.isSpectator() && !fullIri(o))) {
                other.kill();
            }
            if (entity.isOnFire()) entity.clearFire();
        }
        if (entity instanceof ServerPlayer player) updateFlight(player, iri);
        if (entity.tickCount % 20 == 0 && fullSet(entity, "yowaiyo")) {
            // 耐性II
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, true, false, true));
        }
    }

    private static void updateFlight(ServerPlayer player, boolean iri) {
        boolean granted = player.getPersistentData().getBoolean(FLIGHT_TAG);
        if (iri) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
                player.getPersistentData().putBoolean(FLIGHT_TAG, true);
            }
        } else if (granted) {
            player.getPersistentData().remove(FLIGHT_TAG);
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    /** ブロックを左クリックしたとき（サーバー側）。iri の道具なら岩盤でも一瞬で壊す。true で通常の処理を止める。 */
    public static boolean onLeftClickBlock(Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (!isIriTool(serverPlayer.getMainHandItem())) return false;
        if (serverPlayer.level().getBlockState(pos).isAir()) return false;
        return serverPlayer.gameMode.destroyBlock(pos);
    }
}
