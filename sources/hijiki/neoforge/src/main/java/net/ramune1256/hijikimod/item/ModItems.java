package net.ramune1256.hijikimod.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ramune1256.hijikimod.HijikiMod;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HijikiMod.MOD_ID);

    public static final DeferredItem<Item> HIJIKI =
            ITEMS.register("hijiki", () -> new SoundItem(new Item.Properties()));

    public static final DeferredItem<Item> YOWAIYO =
            ITEMS.register("yowaiyo", () -> new YowaiyoItem(new Item.Properties()));

    public static final DeferredItem<Item> IRI =
            ITEMS.register("iri", () -> new IriItem(new Item.Properties()));

    public static final DeferredItem<Item> HIJI_WITHER_SPAWN_EGG =
            ITEMS.register("hiji_wither_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    net.ramune1256.hijikimod.entity.ModEntities.HIJI_WITHER, 0xF3E9A6, 0x22DD22, new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
