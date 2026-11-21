package net.ramune1256.hijikimod.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ramune1256.hijikimod.HijikiMod;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HijikiMod.MOD_ID);

    // 見た目は1ブロックの立方体を2.5倍。ウィザー（高さ3.5）よりひと回り小さい
    public static final DeferredHolder<EntityType<?>, EntityType<HijiWitherEntity>> HIJI_WITHER =
            ENTITY_TYPES.register("hiji_wither", () -> EntityType.Builder.of(HijiWitherEntity::new, MobCategory.MONSTER)
                    .sized(2.5F, 3.0F)
                    .clientTrackingRange(10)
                    .build("hiji_wither"));

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
        bus.addListener(ModEntities::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(HIJI_WITHER.get(), HijiWitherEntity.createAttributes().build());
    }
}
