package com.eeeeeeruki.tonsofadv;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
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
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
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
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 「何かをした回数」を、このMOD独自の統計（tonsofadv:...）として数えます。進捗はこの統計を見て達成されます。
 * Iron's Spells・Ars Nouveau・AE2 はリフレクションでつなぐので、入っていなくても動きます。
 */
final class ActionStats {
    static final String[] NAMES = {
            "turned_right", "turned_left", "looked_up", "looked_down", "looked_straight_up", "looked_straight_down", "moved_backward", "moved_left", "moved_right", "sneak_presses",
            "crafted_total", "smelted_total", "blocks_placed", "blocks_broken", "xp_gained", "max_level", "food_eaten",
            "potions_drunk", "arrows_shot", "animals_tamed", "advancements_earned",
            "irons_spells_cast", "irons_scrolls_used", "ars_spells_cast", "ae2_max_bytes", "ae2_max_items", "ae2_max_types",
            "irons_school_fire", "irons_school_ice", "irons_school_lightning", "irons_school_holy", "irons_school_ender",
            "irons_school_blood", "irons_school_evocation", "irons_school_nature", "irons_school_eldritch",
            "chat_messages", "commands_used", "dims_changed", "crit_hits", "projectile_hits", "xp_orbs", "crops_harvested",
            "logs_chopped", "ores_mined", "entity_interactions", "anvil_repairs"};
    private static final DeferredRegister<ResourceLocation> STATS = DeferredRegister.create(Registries.CUSTOM_STAT, TonsOfAdv.MOD_ID);
    private static final Map<String, DeferredHolder<ResourceLocation, ResourceLocation>> IDS = new LinkedHashMap<>();

    static {
        for (String name : NAMES) {
            IDS.put(name, STATS.register(name, () -> ResourceLocation.fromNamespaceAndPath(TonsOfAdv.MOD_ID, name)));
        }
    }

    private ActionStats() {
    }

    static void register(IEventBus modBus) {
        STATS.register(modBus);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> IDS.values().forEach(id -> Stats.CUSTOM.get(id.get(), StatFormatter.DEFAULT))));
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener((PlayerEvent.ItemCraftedEvent e) -> add(e.getEntity(), "crafted_total", 1));
        bus.addListener((PlayerEvent.ItemSmeltedEvent e) -> add(e.getEntity(), "smelted_total", Math.max(1, e.getSmelting().getCount())));
        bus.addListener((BlockEvent.EntityPlaceEvent e) -> {
            if (e.getEntity() instanceof Player p) add(p, "blocks_placed", 1);
        });
        bus.addListener((BlockEvent.BreakEvent e) -> {
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
        bus.addListener((AnvilRepairEvent e) -> add(e.getEntity(), "anvil_repairs", 1));
        bus.addListener((PlayerXpEvent.XpChange e) -> add(e.getEntity(), "xp_gained", e.getAmount()));
        bus.addListener((LivingEntityUseItemEvent.Finish e) -> {
            if (!(e.getEntity() instanceof Player p)) return;
            ItemStack stack = e.getItem();
            if (stack.getFoodProperties(p) != null) add(p, "food_eaten", 1);
            if (stack.getItem() instanceof PotionItem) add(p, "potions_drunk", 1);
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
            if (sp.tickCount % 40 == 0) Ae2.check(sp);
        });
        if (ModList.get().isLoaded("irons_spellbooks")) Irons.register(bus);
        if (ModList.get().isLoaded("ars_nouveau")) Ars.register(bus);
        if (ModList.get().isLoaded("ae2")) Ae2.init();
    }

    private static Stat<ResourceLocation> stat(String name) {
        return Stats.CUSTOM.get(IDS.get(name).get());
    }

    static void add(Player player, String name, int amount) {
        if (player == null) return;
        if (amount > 0 && player instanceof ServerPlayer sp && !(sp instanceof FakePlayer)) sp.awardStat(stat(name), amount);
    }

    static void max(ServerPlayer player, String name, long value) {
        int v = (int) Math.min(value, Integer.MAX_VALUE);
        Stat<ResourceLocation> s = stat(name);
        int current = player.getStats().getValue(s);
        if (v > current) player.awardStat(s, v - current);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void listen(IEventBus bus, String className, Consumer<Event> handler) throws ClassNotFoundException {
        Class cls = Class.forName(className);
        bus.addListener(EventPriority.LOWEST, false, cls, (Consumer) handler);
    }

    /** Iron's Spells 'n Spellbooks: 呪文を唱えた数・スクロール・学派ごとの数 */
    private static final class Irons {
        static void register(IEventBus bus) {
            try {
                Class<?> cls = Class.forName("io.redspace.ironsspellbooks.api.events.SpellOnCastEvent");
                Method source = cls.getMethod("getCastSource");
                Method school = cls.getMethod("getSchoolType");
                listen(bus, cls.getName(), ev -> {
                    Player p = ((PlayerEvent) ev).getEntity();
                    if (!(p instanceof ServerPlayer)) return;
                    try {
                        Object src = source.invoke(ev);
                        if ("MOB".equals(String.valueOf(src)) || "COMMAND".equals(String.valueOf(src))) return;
                        add(p, "irons_spells_cast", 1);
                        if ("SCROLL".equals(String.valueOf(src))) add(p, "irons_scrolls_used", 1);
                        Object st = school.invoke(ev);
                        if (st != null) {
                            ResourceLocation id = (ResourceLocation) st.getClass().getMethod("getId").invoke(st);
                            String key = "irons_school_" + id.getPath();
                            if (IDS.containsKey(key)) add(p, key, 1);
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                });
            } catch (ReflectiveOperationException | RuntimeException e) {
                LogUtils.getLogger().warn("[tonsofadv] Iron's Spells の呪文を数えられません: {}", e.toString());
            }
        }
    }

    /** Ars Nouveau: 呪文を唱えた数 */
    private static final class Ars {
        static void register(IEventBus bus) {
            try {
                listen(bus, "com.hollingsworth.arsnouveau.api.event.SpellCastEvent", ev -> {
                    if (ev instanceof net.neoforged.neoforge.event.entity.living.LivingEvent le && le.getEntity() instanceof Player p) {
                        add(p, "ars_spells_cast", 1);
                    }
                });
            } catch (ReflectiveOperationException | RuntimeException e) {
                LogUtils.getLogger().warn("[tonsofadv] Ars Nouveau の呪文を数えられません: {}", e.toString());
            }
        }
    }

    /** AE2: MEターミナルを開いている間、ネットワークにしまってある量を見て最大値を記録します */
    static final class Ae2 {
        private static Class<?> menuClass;
        private static Class<?> itemKeyClass;
        private static Field storageField;
        private static Method availableStacks;
        private static Method amountPerByte;

        static void init() {
            try {
                menuClass = Class.forName("appeng.menu.me.common.MEStorageMenu");
                itemKeyClass = Class.forName("appeng.api.stacks.AEItemKey");
                storageField = menuClass.getDeclaredField("storage");
                storageField.setAccessible(true);
                availableStacks = Class.forName("appeng.api.storage.MEStorage").getMethod("getAvailableStacks");
                amountPerByte = Class.forName("appeng.api.stacks.AEKey").getMethod("getAmountPerByte");
            } catch (ReflectiveOperationException | RuntimeException e) {
                menuClass = null;
                LogUtils.getLogger().warn("[tonsofadv] AE2 の倉庫の量を見られません: {}", e.toString());
            }
        }

        static void check(ServerPlayer player) {
            if (menuClass == null) return;
            AbstractContainerMenu menu = player.containerMenu;
            if (!menuClass.isInstance(menu)) return;
            try {
                Object storage = storageField.get(menu);
                if (storage == null) return;
                long bytes = 0, items = 0, types = 0;
                for (Object o : (Iterable<?>) availableStacks.invoke(storage)) {
                    Object2LongMap.Entry<?> entry = (Object2LongMap.Entry<?>) o;
                    long amount = entry.getLongValue();
                    if (amount <= 0) continue;
                    int perByte = Math.max(1, (Integer) amountPerByte.invoke(entry.getKey()));
                    bytes += 8 + (amount + perByte - 1) / perByte;
                    types++;
                    if (itemKeyClass.isInstance(entry.getKey())) items += amount;
                }
                max(player, "ae2_max_bytes", bytes);
                max(player, "ae2_max_items", items);
                max(player, "ae2_max_types", types);
            } catch (ReflectiveOperationException | RuntimeException e) {
                menuClass = null;
                LogUtils.getLogger().warn("[tonsofadv] AE2 の倉庫の量を見られませんでした: {}", e.toString());
            }
        }
    }
}
