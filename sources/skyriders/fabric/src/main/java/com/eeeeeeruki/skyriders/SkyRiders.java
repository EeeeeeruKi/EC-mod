package com.eeeeeeruki.skyriders;

import com.eeeeeeruki.skyriders.block.CourseMarkerBlock;
import com.eeeeeeruki.skyriders.block.CoursePointBlock;
import com.eeeeeeruki.skyriders.block.RailBlock;
import com.eeeeeeruki.skyriders.block.RailBlockEntity;
import com.eeeeeeruki.skyriders.item.RailBlockItem;
import net.minecraft.block.entity.BlockEntityType;
import com.eeeeeeruki.skyriders.net.RaceControlPayload;
import com.eeeeeeruki.skyriders.course.RaceManager;
import com.eeeeeeruki.skyriders.entity.MachineEntity;
import com.eeeeeeruki.skyriders.item.CourseToolItem;
import com.eeeeeeruki.skyriders.net.CourseEditPayload;
import com.eeeeeeruki.skyriders.net.CourseScreenPayload;
import com.eeeeeeruki.skyriders.net.RaceStatePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.sound.BlockSoundGroup;
import com.eeeeeeruki.skyriders.entity.PatchKind;
import com.eeeeeeruki.skyriders.entity.StarShotEntity;
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

    public static final EntityType<StarShotEntity> STAR_SHOT = Registry.register(
            Registries.ENTITY_TYPE, id("star_shot"),
            EntityType.Builder.<StarShotEntity>create(StarShotEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 0.6f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(1)
                    .build("star_shot"));

    public static final Item MACHINE_ITEM = Registry.register(
            Registries.ITEM, id("machine"),
            new MachineItem(new Item.Settings().maxCount(1)));

    public static final Block GOAL_BLOCK = Registry.register(Registries.BLOCK, id("goal_block"),
            new CourseMarkerBlock(markerSettings(MapColor.WHITE), true));
    public static final Block CHECKPOINT_BLOCK = Registry.register(Registries.BLOCK, id("checkpoint_block"),
            new CourseMarkerBlock(markerSettings(MapColor.BLUE), false));
    public static final Item GOAL_BLOCK_ITEM = Registry.register(Registries.ITEM, id("goal_block"),
            new BlockItem(GOAL_BLOCK, new Item.Settings()));
    public static final Item CHECKPOINT_BLOCK_ITEM = Registry.register(Registries.ITEM, id("checkpoint_block"),
            new BlockItem(CHECKPOINT_BLOCK, new Item.Settings()));
    public static final Block START_POINT = Registry.register(Registries.BLOCK, id("start_point"),
            new CoursePointBlock(markerSettings(MapColor.GREEN), false));
    public static final Block TA_POINT = Registry.register(Registries.BLOCK, id("ta_point"),
            new CoursePointBlock(markerSettings(MapColor.ORANGE), true));
    public static final Item START_POINT_ITEM = Registry.register(Registries.ITEM, id("start_point"),
            new BlockItem(START_POINT, new Item.Settings()));
    public static final Item TA_POINT_ITEM = Registry.register(Registries.ITEM, id("ta_point"),
            new BlockItem(TA_POINT, new Item.Settings()));
    public static final Block RAIL = Registry.register(Registries.BLOCK, id("rail"),
            new RailBlock(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).noCollision().nonOpaque()
                    .strength(0.3f).sounds(BlockSoundGroup.METAL)));
    public static final BlockEntityType<RailBlockEntity> RAIL_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, id("rail"),
            BlockEntityType.Builder.create(RailBlockEntity::new, RAIL).build(null));
    /** レール9種（左右・上下の寄せ）。真ん中を最後に登録して、ブロックを選んだ時は真ん中になるようにする */
    public static final java.util.List<Item> RAIL_ITEMS = new java.util.ArrayList<>();

    static {
        String[] names = {"left", "right", "top", "bottom", "top_left", "bottom_left", "top_right", "bottom_right", "center"};
        int[][] off = {{-1, 0}, {1, 0}, {0, 1}, {0, -1}, {-1, 1}, {-1, -1}, {1, 1}, {1, -1}, {0, 0}};
        for (int i = 0; i < names.length; i++) {
            RAIL_ITEMS.add(net.minecraft.item.Items.register(id("rail_" + names[i]),
                    new RailBlockItem(RAIL, new Item.Settings(), off[i][0], off[i][1])));
        }
    }

    public static final Item COURSE_TOOL = Registry.register(Registries.ITEM, id("course_tool"),
            new CourseToolItem(new Item.Settings().maxCount(1)));

    /** 通り抜けられて、少し透けるブロック */
    private static AbstractBlock.Settings markerSettings(MapColor color) {
        return AbstractBlock.Settings.create().mapColor(color).noCollision().nonOpaque().strength(0.3f)
                .sounds(BlockSoundGroup.GLASS)
                .allowsSpawning((s, w, p, t) -> false)
                .solidBlock((s, w, p) -> false)
                .suffocates((s, w, p) -> false)
                .blockVision((s, w, p) -> false);
    }

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
                        entries.add(COURSE_TOOL);
                        entries.add(GOAL_BLOCK_ITEM);
                        entries.add(CHECKPOINT_BLOCK_ITEM);
                        entries.add(START_POINT_ITEM);
                        entries.add(TA_POINT_ITEM);
                        entries.add(RAIL_ITEMS.get(RAIL_ITEMS.size() - 1));
                        for (int i = 0; i < RAIL_ITEMS.size() - 1; i++) entries.add(RAIL_ITEMS.get(i));
                    })
                    .build());

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(MachineActionPayload.ID, MachineActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MachineHitPayload.ID, MachineHitPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CourseScreenPayload.ID, CourseScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RaceStatePayload.ID, RaceStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RaceControlPayload.ID, RaceControlPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.eeeeeeruki.skyriders.net.BgmPayload.ID, com.eeeeeeruki.skyriders.net.BgmPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CourseEditPayload.ID, CourseEditPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CourseEditPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> RaceManager.handleEdit(player, payload.action(), payload.text(), payload.value()));
        });
        ServerTickEvents.END_SERVER_TICK.register(RaceManager::tick);
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
