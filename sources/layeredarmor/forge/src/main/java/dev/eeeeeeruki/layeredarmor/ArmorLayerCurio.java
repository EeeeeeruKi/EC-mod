package dev.eeeeeeruki.layeredarmor;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Map;
import java.util.UUID;

/**
 * 重ね着スロットに入れた防具の振る舞い。
 * 重ね着スロット以外（ほかのCuriosスロット）では何もしない。
 */
public final class ArmorLayerCurio implements ICurioItem {
    public static final ArmorLayerCurio INSTANCE = new ArmorLayerCurio();

    private ArmorLayerCurio() {}

    private static EquipmentSlot slot(SlotContext ctx) {
        return ctx == null ? null : Layers.slotOf(ctx.identifier());
    }

    /** 元の修飾子UUIDとスロットのUUIDを混ぜて、バニラ欄の同じ修飾子とぶつからないUUIDにする */
    static UUID remap(UUID original, UUID slotUuid) {
        return new UUID(original.getMostSignificantBits() ^ slotUuid.getMostSignificantBits() ^ 0x4C61796572656421L,
                original.getLeastSignificantBits() ^ slotUuid.getLeastSignificantBits());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext ctx, UUID uuid, ItemStack stack) {
        EquipmentSlot slot = slot(ctx);
        if (slot == null) return ICurioItem.super.getAttributeModifiers(ctx, uuid, stack);
        Multimap<Attribute, AttributeModifier> out = LinkedHashMultimap.create();
        // 防御力・防具強度・ノックバック耐性のほか、Apotheosisなど他MODが付けた修飾子もここに入ってくる
        for (Map.Entry<Attribute, AttributeModifier> e : stack.getAttributeModifiers(slot).entries()) {
            AttributeModifier m = e.getValue();
            out.put(e.getKey(), new AttributeModifier(remap(m.getId(), uuid), m.getName(), m.getAmount(), m.getOperation()));
        }
        return out;
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        EquipmentSlot slot = slot(ctx);
        if (slot == null || !(ctx.entity() instanceof Player player)) return;
        // バニラの防具欄と同じように毎tick呼ぶ（ジェットパックやメカスーツのモジュールなどがここで動く）
        try {
            stack.inventoryTick(player.level(), player, slot.getIndex(), false);
            stack.onArmorTick(player.level(), player);
        } catch (Throwable t) {
            LayeredArmor.LOGGER.debug("[LayeredArmor] armor tick failed for {}", stack, t);
        }
        // カメの甲羅（水中呼吸）はバニラが頭の欄だけ見ているのでここで再現する
        if (slot == EquipmentSlot.HEAD && stack.is(Items.TURTLE_HELMET) && !player.level().isClientSide
                && !player.isEyeInFluid(FluidTags.WATER) && !player.getItemBySlot(EquipmentSlot.HEAD).is(Items.TURTLE_HELMET)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
        }
    }

    @Override
    public boolean canEquip(SlotContext ctx, ItemStack stack) {
        EquipmentSlot slot = slot(ctx);
        if (slot == null) return ICurioItem.super.canEquip(ctx, stack);
        return Layers.canLayer(stack, slot);
    }

    @Override
    public boolean canUnequip(SlotContext ctx, ItemStack stack) {
        if (slot(ctx) == null) return ICurioItem.super.canUnequip(ctx, stack);
        // 束縛の呪いはバニラと同じく外せない（クリエイティブは除く）
        if (EnchantmentHelper.hasBindingCurse(stack) && ctx.entity() instanceof Player p && !p.isCreative()) return false;
        return true;
    }

    @Override
    public boolean canEquipFromUse(SlotContext ctx, ItemStack stack) {
        // 右クリック装備はバニラの防具欄を優先したいので使わない
        return false;
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext ctx, ItemStack stack) {
        if (stack.getItem() instanceof Equipable eq) {
            SoundEvent sound = eq.getEquipSound();
            if (sound != null) return new ICurio.SoundInfo(sound, 1.0F, 1.0F);
        }
        return ICurioItem.super.getEquipSound(ctx, stack);
    }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> getAttributesTooltip(java.util.List<net.minecraft.network.chat.Component> tooltips, ItemStack stack) {
        // 性能はバニラ欄に着けたときと同じなので、Curios側の性能表示は出さない（説明が二重になるのを防ぐ）
        return new java.util.ArrayList<>();
    }

    @Override
    public boolean makesPiglinsNeutral(SlotContext ctx, ItemStack stack) {
        if (slot(ctx) == null) return ICurioItem.super.makesPiglinsNeutral(ctx, stack);
        LivingEntity e = ctx.entity();
        return e != null && stack.makesPiglinsNeutral(e);
    }

    @Override
    public boolean canWalkOnPowderedSnow(SlotContext ctx, ItemStack stack) {
        if (slot(ctx) == null) return ICurioItem.super.canWalkOnPowderedSnow(ctx, stack);
        LivingEntity e = ctx.entity();
        return e != null && stack.canWalkOnPowderedSnow(e);
    }

    @Override
    public boolean isEnderMask(SlotContext ctx, EnderMan enderMan, ItemStack stack) {
        if (slot(ctx) == null) return ICurioItem.super.isEnderMask(ctx, enderMan, stack);
        return ctx.entity() instanceof Player p && stack.isEnderMask(p, enderMan);
    }
}
