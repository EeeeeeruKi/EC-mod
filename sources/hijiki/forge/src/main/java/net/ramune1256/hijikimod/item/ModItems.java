package net.ramune1256.hijikimod.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ramune1256.hijikimod.HijikiMod;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HijikiMod.MOD_ID);

    public static final RegistryObject<Item> HIJIKI =
            ITEMS.register("hijiki", () -> new SoundItem(new Item.Properties()));

    public static final RegistryObject<Item> YOWAIYO =
            ITEMS.register("yowaiyo", () -> new YowaiyoItem(new Item.Properties()));

    public static final RegistryObject<Item> IRI =
            ITEMS.register("iri", () -> new IriItem(new Item.Properties()));

    public static final RegistryObject<Item> HIJI_WITHER_SPAWN_EGG =
            ITEMS.register("hiji_wither_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    net.ramune1256.hijikimod.entity.ModEntities.HIJI_WITHER, 0xF3E9A6, 0x22DD22, new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
