package com.eeeeeeruki.hijikiaddon;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HijikiAddon.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HijikiAddon.MOD_ID);

    private static Supplier<Ingredient> repair(String id) {
        return () -> Ingredient.of(ForgeRegistries.ITEMS.getValue(new ResourceLocation("hijiki", id)));
    }

    // iri：ネザライトより上。即死と岩盤破壊はイベントで処理する
    public static final Tier IRI_TIER = new ForgeTier(4, 99999, 100.0F, 96.0F, 30, Tags.Blocks.NEEDS_NETHERITE_TOOL, repair("iri"));
    // 弱いよ：ダイヤと同じ
    public static final Tier YOWAIYO_TIER = new ForgeTier(3, 1561, 8.0F, 3.0F, 10, BlockTags.NEEDS_DIAMOND_TOOL, repair("yowaiyo"));

    public static final ArmorMaterial IRI_ARMOR = new ModArmorMaterial("iri", 99, new int[]{6, 9, 11, 6}, 30, 6.0F, 0.5F, repair("iri"));
    public static final ArmorMaterial YOWAIYO_ARMOR = new ModArmorMaterial("yowaiyo", 33, new int[]{3, 6, 8, 3}, 10, 2.0F, 0.0F, repair("yowaiyo"));

    private static Item.Properties props(boolean fireResistant) {
        Item.Properties p = new Item.Properties();
        return fireResistant ? p.fireResistant() : p;
    }

    public static final RegistryObject<Item> IRI_SWORD = ITEMS.register("iri_sword", () -> new SwordItem(IRI_TIER, 3, -2.4F, props(true)));
    public static final RegistryObject<Item> IRI_AXE = ITEMS.register("iri_axe", () -> new AxeItem(IRI_TIER, 5.0F, -3.0F, props(true)));
    public static final RegistryObject<Item> IRI_PICKAXE = ITEMS.register("iri_pickaxe", () -> new PickaxeItem(IRI_TIER, 1, -2.8F, props(true)));
    public static final RegistryObject<Item> IRI_PAXEL = ITEMS.register("iri_paxel", () -> new PaxelItem(4.0F, -2.9F, IRI_TIER, props(true)));
    public static final RegistryObject<Item> IRI_HELMET = armor("iri_helmet", IRI_ARMOR, ArmorItem.Type.HELMET, true);
    public static final RegistryObject<Item> IRI_CHESTPLATE = armor("iri_chestplate", IRI_ARMOR, ArmorItem.Type.CHESTPLATE, true);
    public static final RegistryObject<Item> IRI_LEGGINGS = armor("iri_leggings", IRI_ARMOR, ArmorItem.Type.LEGGINGS, true);
    public static final RegistryObject<Item> IRI_BOOTS = armor("iri_boots", IRI_ARMOR, ArmorItem.Type.BOOTS, true);

    public static final RegistryObject<Item> YOWAIYO_SWORD = ITEMS.register("yowaiyo_sword", () -> new SwordItem(YOWAIYO_TIER, 3, -2.4F, props(false)));
    public static final RegistryObject<Item> YOWAIYO_AXE = ITEMS.register("yowaiyo_axe", () -> new AxeItem(YOWAIYO_TIER, 5.0F, -3.0F, props(false)));
    public static final RegistryObject<Item> YOWAIYO_PICKAXE = ITEMS.register("yowaiyo_pickaxe", () -> new PickaxeItem(YOWAIYO_TIER, 1, -2.8F, props(false)));
    public static final RegistryObject<Item> YOWAIYO_PAXEL = ITEMS.register("yowaiyo_paxel", () -> new PaxelItem(4.0F, -2.9F, YOWAIYO_TIER, props(false)));
    public static final RegistryObject<Item> YOWAIYO_HELMET = armor("yowaiyo_helmet", YOWAIYO_ARMOR, ArmorItem.Type.HELMET, false);
    public static final RegistryObject<Item> YOWAIYO_CHESTPLATE = armor("yowaiyo_chestplate", YOWAIYO_ARMOR, ArmorItem.Type.CHESTPLATE, false);
    public static final RegistryObject<Item> YOWAIYO_LEGGINGS = armor("yowaiyo_leggings", YOWAIYO_ARMOR, ArmorItem.Type.LEGGINGS, false);
    public static final RegistryObject<Item> YOWAIYO_BOOTS = armor("yowaiyo_boots", YOWAIYO_ARMOR, ArmorItem.Type.BOOTS, false);

    private static RegistryObject<Item> armor(String name, ArmorMaterial mat, ArmorItem.Type type, boolean fire) {
        return ITEMS.register(name, () -> new ArmorItem(mat, type, props(fire)));
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("hijikiaddon", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hijikiaddon"))
            .icon(() -> new ItemStack(IRI_SWORD.get()))
            .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
            .build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
