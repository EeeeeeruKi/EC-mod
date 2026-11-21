package com.eeeeeeruki.hijikiaddon;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class ModArmorMaterial implements ArmorMaterial {
    private static final int[] BASE_DURABILITY = {13, 15, 16, 11}; // boots, leggings, chest, helmet
    private final String name;
    private final int durabilityMul;
    private final int[] defense; // boots, leggings, chest, helmet
    private final int enchant;
    private final float toughness;
    private final float knockback;
    private final Supplier<Ingredient> repair;

    public ModArmorMaterial(String name, int durabilityMul, int[] defense, int enchant, float toughness, float knockback, Supplier<Ingredient> repair) {
        this.name = name; this.durabilityMul = durabilityMul; this.defense = defense;
        this.enchant = enchant; this.toughness = toughness; this.knockback = knockback; this.repair = repair;
    }

    private static int index(ArmorItem.Type type) {
        return switch (type) { case BOOTS -> 0; case LEGGINGS -> 1; case CHESTPLATE -> 2; case HELMET -> 3; };
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return BASE_DURABILITY[index(type)] * durabilityMul; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return defense[index(type)]; }
    @Override public int getEnchantmentValue() { return enchant; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
    @Override public String getName() { return HijikiAddon.MOD_ID + ":" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockback; }
}
