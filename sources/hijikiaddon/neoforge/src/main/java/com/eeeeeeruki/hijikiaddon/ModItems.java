package com.eeeeeeruki.hijikiaddon;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HijikiAddon.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, HijikiAddon.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HijikiAddon.MOD_ID);

    private static Supplier<Ingredient> repair(String id) {
        return () -> Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hijiki", id)));
    }

    // iri：ネザライトより上。即死と岩盤破壊はイベントで処理する
    public static final Tier IRI_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 99999, 100.0F, 96.0F, 30, repair("iri"));
    // 弱いよ：ダイヤと同じ
    public static final Tier YOWAIYO_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10, repair("yowaiyo"));

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> material(String name, int[] def, int ench, float tough, float kb, String repairId) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                    m.put(ArmorItem.Type.BOOTS, def[0]);
                    m.put(ArmorItem.Type.LEGGINGS, def[1]);
                    m.put(ArmorItem.Type.CHESTPLATE, def[2]);
                    m.put(ArmorItem.Type.HELMET, def[3]);
                    m.put(ArmorItem.Type.BODY, def[2]);
                }),
                ench, SoundEvents.ARMOR_EQUIP_NETHERITE, repair(repairId),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(HijikiAddon.MOD_ID, name))),
                tough, kb));
    }

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> IRI_ARMOR = material("iri", new int[]{6, 9, 11, 6}, 30, 6.0F, 0.5F, "iri");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> YOWAIYO_ARMOR = material("yowaiyo", new int[]{3, 6, 8, 3}, 10, 2.0F, 0.0F, "yowaiyo");

    private static Item.Properties props(boolean fireResistant) {
        Item.Properties p = new Item.Properties();
        return fireResistant ? p.fireResistant() : p;
    }

    // iri
    public static final DeferredItem<Item> IRI_SWORD = ITEMS.register("iri_sword",
            () -> new SwordItem(IRI_TIER, props(true).attributes(SwordItem.createAttributes(IRI_TIER, 3, -2.4F))));
    public static final DeferredItem<Item> IRI_AXE = ITEMS.register("iri_axe",
            () -> new AxeItem(IRI_TIER, props(true).attributes(DiggerItem.createAttributes(IRI_TIER, 5.0F, -3.0F))));
    public static final DeferredItem<Item> IRI_PICKAXE = ITEMS.register("iri_pickaxe",
            () -> new PickaxeItem(IRI_TIER, props(true).attributes(DiggerItem.createAttributes(IRI_TIER, 1.0F, -2.8F))));
    public static final DeferredItem<Item> IRI_PAXEL = ITEMS.register("iri_paxel",
            () -> new PaxelItem(IRI_TIER, props(true).attributes(DiggerItem.createAttributes(IRI_TIER, 4.0F, -2.9F))));
    public static final DeferredItem<Item> IRI_HELMET = armor("iri_helmet", IRI_ARMOR, ArmorItem.Type.HELMET, 99, true);
    public static final DeferredItem<Item> IRI_CHESTPLATE = armor("iri_chestplate", IRI_ARMOR, ArmorItem.Type.CHESTPLATE, 99, true);
    public static final DeferredItem<Item> IRI_LEGGINGS = armor("iri_leggings", IRI_ARMOR, ArmorItem.Type.LEGGINGS, 99, true);
    public static final DeferredItem<Item> IRI_BOOTS = armor("iri_boots", IRI_ARMOR, ArmorItem.Type.BOOTS, 99, true);

    // 弱いよ
    public static final DeferredItem<Item> YOWAIYO_SWORD = ITEMS.register("yowaiyo_sword",
            () -> new SwordItem(YOWAIYO_TIER, props(false).attributes(SwordItem.createAttributes(YOWAIYO_TIER, 3, -2.4F))));
    public static final DeferredItem<Item> YOWAIYO_AXE = ITEMS.register("yowaiyo_axe",
            () -> new AxeItem(YOWAIYO_TIER, props(false).attributes(DiggerItem.createAttributes(YOWAIYO_TIER, 5.0F, -3.0F))));
    public static final DeferredItem<Item> YOWAIYO_PICKAXE = ITEMS.register("yowaiyo_pickaxe",
            () -> new PickaxeItem(YOWAIYO_TIER, props(false).attributes(DiggerItem.createAttributes(YOWAIYO_TIER, 1.0F, -2.8F))));
    public static final DeferredItem<Item> YOWAIYO_PAXEL = ITEMS.register("yowaiyo_paxel",
            () -> new PaxelItem(YOWAIYO_TIER, props(false).attributes(DiggerItem.createAttributes(YOWAIYO_TIER, 4.0F, -2.9F))));
    public static final DeferredItem<Item> YOWAIYO_HELMET = armor("yowaiyo_helmet", YOWAIYO_ARMOR, ArmorItem.Type.HELMET, 33, false);
    public static final DeferredItem<Item> YOWAIYO_CHESTPLATE = armor("yowaiyo_chestplate", YOWAIYO_ARMOR, ArmorItem.Type.CHESTPLATE, 33, false);
    public static final DeferredItem<Item> YOWAIYO_LEGGINGS = armor("yowaiyo_leggings", YOWAIYO_ARMOR, ArmorItem.Type.LEGGINGS, 33, false);
    public static final DeferredItem<Item> YOWAIYO_BOOTS = armor("yowaiyo_boots", YOWAIYO_ARMOR, ArmorItem.Type.BOOTS, 33, false);

    private static DeferredItem<Item> armor(String name, DeferredHolder<ArmorMaterial, ArmorMaterial> mat, ArmorItem.Type type, int durabilityMul, boolean fire) {
        return ITEMS.register(name, () -> new ArmorItem(mat, type, props(fire).durability(type.getDurability(durabilityMul))));
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("hijikiaddon", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hijikiaddon"))
            .icon(() -> new ItemStack(IRI_SWORD.get()))
            .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
            .build());

    public static void register(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
