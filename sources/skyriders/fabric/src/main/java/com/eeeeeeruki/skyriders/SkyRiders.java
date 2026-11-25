package com.eeeeeeruki.skyriders;

import com.eeeeeeruki.skyriders.entity.MachineEntity;
import com.eeeeeeruki.skyriders.item.MachineItem;
import com.eeeeeeruki.skyriders.net.MachineActionPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SkyRiders implements ModInitializer {
    public static final String MOD_ID = "skyriders";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static final EntityType<MachineEntity> MACHINE = Registry.register(
            Registries.ENTITY_TYPE, id("machine"),
            EntityType.Builder.<MachineEntity>create(MachineEntity::new, SpawnGroup.MISC)
                    .dimensions(1.4f, 0.5f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(1)
                    .build("machine"));

    public static final Item MACHINE_ITEM = Registry.register(
            Registries.ITEM, id("machine"),
            new MachineItem(new Item.Settings().maxCount(1)));

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(MACHINE_ITEM));

        PayloadTypeRegistry.playC2S().register(MachineActionPayload.ID, MachineActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MachineActionPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (player.getVehicle() instanceof MachineEntity machine) {
                    machine.handleAction(player, payload.action());
                }
            });
        });
    }
}
