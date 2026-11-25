package com.eeeeeeruki.skyriders;

import com.eeeeeeruki.skyriders.entity.MachineEntity;
import com.eeeeeeruki.skyriders.entity.PatchKind;
import com.eeeeeeruki.skyriders.item.MachineItem;
import com.eeeeeeruki.skyriders.item.PatchItem;
import com.eeeeeeruki.skyriders.net.MachineActionPayload;
import com.eeeeeeruki.skyriders.net.MachineHitPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.EnumMap;
import java.util.Map;

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

    public static final Map<PatchKind, Item> PATCHES = new EnumMap<>(PatchKind.class);
    public static final Item PATCH_ALL;

    static {
        for (PatchKind k : PatchKind.ALL) {
            PATCHES.put(k, Registry.register(Registries.ITEM, id("patch_" + k.id), new PatchItem(new Item.Settings(), k)));
        }
        PATCH_ALL = Registry.register(Registries.ITEM, id("patch_all"), new PatchItem(new Item.Settings(), null));
    }

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(MACHINE_ITEM))
                    .displayName(Text.translatable("itemGroup.skyriders.main"))
                    .entries((ctx, entries) -> {
                        entries.add(MACHINE_ITEM);
                        PATCHES.values().forEach(entries::add);
                        entries.add(PATCH_ALL);
                    })
                    .build());

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(MachineActionPayload.ID, MachineActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MachineHitPayload.ID, MachineHitPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MachineActionPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (player.getVehicle() instanceof MachineEntity machine) {
                    machine.handleAction(player, payload.action());
                }
            });
        });

        // 乗っている人へのダメージは、マシンが代わりに受ける
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PlayerEntity player && player.getVehicle() instanceof MachineEntity machine
                    && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                if (source.isOf(DamageTypes.IN_WALL)) return false;
                Entity attacker = source.getAttacker();
                Vec3d dir = attacker != null ? machine.getPos().subtract(attacker.getPos()) : Vec3d.ZERO;
                machine.takeHit(amount * MachineEntity.MACHINE_DAMAGE_SCALE, attacker, new Vec3d(dir.x, 0, dir.z), 0.5);
                return false;
            }
            return true;
        });
    }
}
