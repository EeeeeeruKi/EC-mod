package com.eeeeeeruki.tonsofadv;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 進捗は data/tonsofadv/advancement にあります（26.2 Fabric 版）。
 * 「何かをした回数」は独自の統計（tonsofadv:...）として数えます。Fabric API のイベントと、
 * 失敗しても止まらない（require = 0）ミックスイン、それとバニラの統計の合計から出します。
 */
public class TonsOfAdv implements ModInitializer {
    public static final String MOD_ID = "tonsofadv";
    static final String[] NAMES = {
            "crafted_total", "smelted_total", "blocks_placed", "blocks_broken", "xp_gained", "max_level", "food_eaten",
            "potions_drunk", "arrows_shot", "animals_tamed", "advancements_earned",
            "chat_messages", "commands_used", "dims_changed", "crops_harvested",
            "logs_chopped", "ores_mined", "entity_interactions", "anvil_repairs"};
    private static final Map<String, Identifier> IDS = new LinkedHashMap<>();
    private static List<Item> blockItems;
    private static List<Item> foods;

    @Override
    public void onInitialize() {
        for (String name : NAMES) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
            Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);
            Stats.CUSTOM.get(id, StatFormatter.DEFAULT);
            IDS.put(name, id);
        }
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (player.isCreative()) return;
            add(player, "blocks_broken", 1);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) add(player, "crops_harvested", 1);
            if (state.is(BlockTags.LOGS)) add(player, "logs_chopped", 1);
            if (state.is(ConventionalBlockTags.ORES)) add(player, "ores_mined", 1);
        });
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, type) -> add(sender, "chat_messages", 1));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> add(player, "dims_changed", 1));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (hand == InteractionHand.MAIN_HAND) add(player, "entity_interactions", 1);
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(TonsOfAdv::tick);
    }

    private static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        if (t % 20 != 0) return;
        boolean slow = t % 100 == 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            max(p, "max_level", p.experienceLevel);
            if (!slow) continue;
            // バニラの統計の合計から出す回数（置いたブロック＝ブロックの「使用」の合計 など）
            if (blockItems == null) {
                blockItems = new ArrayList<>();
                foods = new ArrayList<>();
                for (Item item : BuiltInRegistries.ITEM) {
                    if (item instanceof BlockItem) blockItems.add(item);
                    if (new ItemStack(item).has(DataComponents.FOOD)) foods.add(item);
                }
            }
            max(p, "blocks_placed", sumUsed(p, blockItems));
            max(p, "food_eaten", sumUsed(p, foods));
            max(p, "potions_drunk", used(p, Items.POTION));
            max(p, "arrows_shot", used(p, Items.BOW) + used(p, Items.CROSSBOW));
            int done = 0;
            for (AdvancementHolder adv : server.getAdvancements().getAllAdvancements()) {
                if (adv.value().display().isPresent() && !adv.id().getPath().endsWith("root")
                        && p.getAdvancements().getOrStartProgress(adv).isDone()) done++;
            }
            max(p, "advancements_earned", done);
        }
    }

    private static long used(ServerPlayer p, Item item) {
        return p.getStats().getValue(Stats.ITEM_USED.get(item));
    }

    private static long sumUsed(ServerPlayer p, List<Item> items) {
        long sum = 0;
        for (Item item : items) sum += used(p, item);
        return sum;
    }

    private static Stat<Identifier> stat(String name) {
        return Stats.CUSTOM.get(IDS.get(name));
    }

    public static void add(Player player, String name, int amount) {
        if (amount > 0 && player instanceof ServerPlayer sp && !(sp instanceof net.fabricmc.fabric.api.entity.FakePlayer)) sp.awardStat(stat(name), amount);
    }

    static void max(ServerPlayer player, String name, long value) {
        int v = (int) Math.min(value, Integer.MAX_VALUE);
        Stat<Identifier> s = stat(name);
        int current = player.getStats().getValue(s);
        if (v > current) player.awardStat(s, v - current);
    }
}
