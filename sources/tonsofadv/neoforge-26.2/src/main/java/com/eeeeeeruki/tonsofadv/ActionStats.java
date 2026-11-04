package com.eeeeeeruki.tonsofadv;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.AnvilCraftEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 「何かをした回数」を、このMOD独自の統計（tonsofadv:...）として数えます。進捗はこの統計を見て達成されます。
 * （26.2 版）
 */
final class ActionStats {
    static final String[] NAMES = {
            "turned_right", "turned_left", "looked_up", "looked_down", "looked_straight_up", "looked_straight_down", "moved_backward", "moved_left", "moved_right", "sneak_presses",
            "crafted_total", "smelted_total", "blocks_placed", "blocks_broken", "xp_gained", "max_level", "food_eaten",
            "potions_drunk", "arrows_shot", "animals_tamed", "advancements_earned",
            "chat_messages", "commands_used", "dims_changed", "crit_hits", "projectile_hits", "xp_orbs", "crops_harvested",
            "logs_chopped", "ores_mined", "entity_interactions", "anvil_repairs"};
    private static final DeferredRegister<Identifier> STATS = DeferredRegister.create(Registries.CUSTOM_STAT, TonsOfAdv.MOD_ID);
    private static final Map<String, DeferredHolder<Identifier, Identifier>> IDS = new LinkedHashMap<>();

    static {
        for (String name : NAMES) {
            IDS.put(name, STATS.register(name, () -> Identifier.fromNamespaceAndPath(TonsOfAdv.MOD_ID, name)));
        }
    }

    private ActionStats() {
    }

    static void register(IEventBus modBus) {
        STATS.register(modBus);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> IDS.values().forEach(id -> Stats.CUSTOM.get(id.get(), StatFormatter.DEFAULT))));
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener((PlayerEvent.ItemCraftedEvent e) -> add(e.getEntity(), "crafted_total", 1));
        bus.addListener((PlayerEvent.ItemSmeltedEvent e) -> add(e.getEntity(), "smelted_total", Math.max(1, e.getAmountRemoved())));
        bus.addListener((BlockEvent.EntityPlaceEvent e) -> {
            if (e.getEntity() instanceof Player p) add(p, "blocks_placed", 1);
        });
        bus.addListener((BreakBlockEvent e) -> {
            if (e.getPlayer().isCreative()) return;
            add(e.getPlayer(), "blocks_broken", 1);
            BlockState state = e.getState();
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) add(e.getPlayer(), "crops_harvested", 1);
            if (state.is(BlockTags.LOGS)) add(e.getPlayer(), "logs_chopped", 1);
            if (state.is(Tags.Blocks.ORES)) add(e.getPlayer(), "ores_mined", 1);
        });
        bus.addListener((ServerChatEvent e) -> add(e.getPlayer(), "chat_messages", 1));
        bus.addListener((CommandEvent e) -> add(e.getParseResults().getContext().getSource().getPlayer(), "commands_used", 1));
        bus.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> add(e.getEntity(), "dims_changed", 1));
        bus.addListener((CriticalHitEvent e) -> {
            if (e.isCriticalHit()) add(e.getEntity(), "crit_hits", 1);
        });
        bus.addListener((ProjectileImpactEvent e) -> {
            if (e.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity
                    && e.getProjectile().getOwner() instanceof Player p) add(p, "projectile_hits", 1);
        });
        bus.addListener((PlayerXpEvent.PickupXp e) -> add(e.getEntity(), "xp_orbs", 1));
        bus.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (e.getHand() == InteractionHand.MAIN_HAND) add(e.getEntity(), "entity_interactions", 1);
        });
        bus.addListener((AnvilCraftEvent.Post e) -> add(e.getEntity(), "anvil_repairs", 1));
        bus.addListener((PlayerXpEvent.XpChange e) -> add(e.getEntity(), "xp_gained", e.getAmount()));
        bus.addListener((LivingEntityUseItemEvent.Finish e) -> {
            if (!(e.getEntity() instanceof Player p)) return;
            ItemStack stack = e.getItem();
            if (stack.has(DataComponents.FOOD)) add(p, "food_eaten", 1);
            if (stack.is(Items.POTION)) add(p, "potions_drunk", 1);
        });
        bus.addListener((ArrowLooseEvent e) -> {
            if (e.hasAmmo()) add(e.getEntity(), "arrows_shot", 1);
        });
        bus.addListener((AnimalTameEvent e) -> add(e.getTamer(), "animals_tamed", 1));
        bus.addListener((AdvancementEvent.AdvancementEarnEvent e) -> {
            AdvancementHolder adv = e.getAdvancement();
            if (adv.value().display().isPresent() && !adv.id().getPath().endsWith("root")) add(e.getEntity(), "advancements_earned", 1);
        });
        bus.addListener((PlayerTickEvent.Post e) -> {
            if (!(e.getEntity() instanceof ServerPlayer sp) || sp instanceof FakePlayer) return;
            Gestures.tick(sp);
            if (sp.tickCount % 20 == 0) max(sp, "max_level", sp.experienceLevel);
        });
    }

    private static Stat<Identifier> stat(String name) {
        return Stats.CUSTOM.get(IDS.get(name).get());
    }

    static void add(Player player, String name, int amount) {
        if (player == null) return;
        if (amount > 0 && player instanceof ServerPlayer sp && !(sp instanceof FakePlayer)) sp.awardStat(stat(name), amount);
    }

    static void max(ServerPlayer player, String name, long value) {
        int v = (int) Math.min(value, Integer.MAX_VALUE);
        Stat<Identifier> s = stat(name);
        int current = player.getStats().getValue(s);
        if (v > current) player.awardStat(s, v - current);
    }

}
