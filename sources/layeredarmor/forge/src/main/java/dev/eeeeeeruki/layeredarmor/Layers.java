package dev.eeeeeeruki.layeredarmor;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.*;
import java.util.function.Predicate;

/**
 * 重ね着スロット（Curios）の中身を扱う共通処理。
 */
public final class Layers {
    private Layers() {}

    /** バニラの getArmorSlots と同じ順番（足→脚→胴→頭） */
    public static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    public static final Map<EquipmentSlot, String> SLOT_IDS = new EnumMap<>(EquipmentSlot.class);
    private static final Map<String, EquipmentSlot> ID_TO_SLOT = new HashMap<>();

    /** 重ね着スロットに入れられるアイテム */
    public static final Set<Item> LAYERABLE = Collections.newSetFromMap(new IdentityHashMap<>());

    static {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            String id = "layered_" + shortName(slot);
            SLOT_IDS.put(slot, id);
            ID_TO_SLOT.put(id, slot);
        }
    }

    public static String shortName(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "head";
            case CHEST -> "chest";
            case LEGS -> "legs";
            case FEET -> "feet";
            default -> slot.getName();
        };
    }

    /** Curiosのスロット名から部位を返す（重ね着スロット以外は null） */
    public static EquipmentSlot slotOf(String identifier) {
        return ID_TO_SLOT.get(identifier);
    }

    public static EquipmentSlot defaultArmorSlot(Item item) {
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(new ItemStack(item));
        return slot.isArmor() ? slot : null;
    }

    public static boolean canLayer(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || !LAYERABLE.contains(stack.getItem())) return false;
        return LivingEntity.getEquipmentSlotForItem(stack) == slot;
    }

    // ------------------------------------------------------------------
    // 中身の取得
    // ------------------------------------------------------------------

    /**
     * 重ね着スロットの装備を「インベントリの仮想番号」で扱うときの先頭番号（AE2系のメニューが番号で装備を探すため）。
     * 番号は「部位（足→脚→胴→頭）×8 ＋ スロット内の位置」で決める。ほかのマスが空いたり埋まったりしても番号が変わらない。
     */
    public static final int VIRTUAL_BASE = 100;
    private static final int VIRTUAL_PER_SLOT = 8;

    public static int virtualIndex(Entry e) {
        int pos = 0;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) if (ARMOR_SLOTS[i] == e.slot()) pos = i;
        return VIRTUAL_BASE + pos * VIRTUAL_PER_SLOT + Math.min(e.index(), VIRTUAL_PER_SLOT - 1);
    }

    /** 仮想番号（VIRTUAL_BASE以上）に対応する重ね着の装備。VIRTUAL_BASE未満はnull、何も無ければEMPTY */
    public static ItemStack virtualStack(Player player, int index) {
        if (index < VIRTUAL_BASE) return null;
        int n = index - VIRTUAL_BASE;
        int pos = n / VIRTUAL_PER_SLOT;
        int i = n % VIRTUAL_PER_SLOT;
        if (pos >= ARMOR_SLOTS.length) return ItemStack.EMPTY;
        ICuriosItemHandler inv = inventory(player);
        if (inv == null) return ItemStack.EMPTY;
        Optional<ICurioStacksHandler> opt = inv.getStacksHandler(SLOT_IDS.get(ARMOR_SLOTS[pos]));
        if (opt.isEmpty()) return ItemStack.EMPTY;
        IDynamicStackHandler stacks = opt.get().getStacks();
        return i < stacks.getSlots() ? stacks.getStackInSlot(i) : ItemStack.EMPTY;
    }

    public record Entry(EquipmentSlot slot, SlotContext context, IDynamicStackHandler handler, int index, ItemStack stack) {}

    private static ICuriosItemHandler inventory(LivingEntity entity) {
        if (entity == null) return null;
        try {
            return CuriosApi.getCuriosInventory(entity).resolve().orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    /** その部位の重ね着スロットにある防具（空は含まない） */
    public static List<Entry> entries(LivingEntity entity, EquipmentSlot slot) {
        ICuriosItemHandler inv = inventory(entity);
        if (inv == null) return List.of();
        List<Entry> out = new ArrayList<>(1);
        collect(inv, entity, slot, out);
        return out;
    }

    /** 全部位の重ね着スロットにある防具（足→脚→胴→頭の順、空は含まない） */
    public static List<Entry> allEntries(LivingEntity entity) {
        ICuriosItemHandler inv = inventory(entity);
        if (inv == null) return List.of();
        List<Entry> out = new ArrayList<>(4);
        for (EquipmentSlot slot : ARMOR_SLOTS) collect(inv, entity, slot, out);
        return out;
    }

    private static void collect(ICuriosItemHandler inv, LivingEntity entity, EquipmentSlot slot, List<Entry> out) {
        String id = SLOT_IDS.get(slot);
        if (id == null) return;
        Optional<ICurioStacksHandler> opt = inv.getStacksHandler(id);
        if (opt.isEmpty()) return;
        ICurioStacksHandler handler = opt.get();
        IDynamicStackHandler stacks = handler.getStacks();
        NonNullList<Boolean> renders = handler.getRenders();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            boolean visible = i >= renders.size() || renders.get(i);
            out.add(new Entry(slot, new SlotContext(id, entity, i, false, visible), stacks, i, stack));
        }
    }

    /** その部位の重ね着スロットで最初に見つかった防具（無ければ EMPTY） */
    public static ItemStack first(LivingEntity entity, EquipmentSlot slot, Predicate<ItemStack> filter) {
        for (Entry e : entries(entity, slot)) {
            if (filter.test(e.stack())) return e.stack();
        }
        return ItemStack.EMPTY;
    }

    /** 見た目に出す重ね着防具（Curiosの表示切り替えがオンのものだけ） */
    public static ItemStack visible(LivingEntity entity, EquipmentSlot slot) {
        for (Entry e : entries(entity, slot)) {
            if (e.context().visible()) return e.stack();
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------
    // getArmorSlots の拡張
    // ------------------------------------------------------------------

    public static Iterable<ItemStack> wrapArmorSlots(Player player, Iterable<ItemStack> original) {
        if (original == null) return null;
        List<Entry> extra = allEntries(player);
        if (extra.isEmpty()) return null;
        if (original instanceof List<ItemStack> list) {
            return LayeredArmorList.create(list, extra);
        }
        List<ItemStack> extraStacks = new ArrayList<>(extra.size());
        for (Entry e : extra) extraStacks.add(e.stack());
        return com.google.common.collect.Iterables.concat(original, extraStacks);
    }

    // ------------------------------------------------------------------
    // 耐久値（バニラ欄を優先）
    // ------------------------------------------------------------------

    public static void hurtLayers(Player player, NonNullList<ItemStack> vanillaArmor, DamageSource source, float amount, int[] slots) {
        if (player == null || player.level().isClientSide || amount <= 0.0F) return;
        float per = amount / 4.0F;
        if (per < 1.0F) per = 1.0F;
        int damage = (int) per;
        for (int i : slots) {
            if (i < 0 || i >= vanillaArmor.size()) continue;
            ItemStack vanilla = vanillaArmor.get(i);
            // バニラの欄に防具があればそっちが削れるので、重ね着は削らない
            if (vanilla.getItem() instanceof ArmorItem) continue;
            EquipmentSlot slot = EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i);
            for (Entry e : entries(player, slot)) {
                ItemStack stack = e.stack();
                if (!(stack.getItem() instanceof ArmorItem)) continue;
                if (source.is(DamageTypeTags.IS_FIRE) && stack.getItem().isFireResistant()) continue;
                SlotContext ctx = e.context();
                stack.hurtAndBreak(damage, player, p -> CuriosApi.broadcastCurioBreakEvent(ctx));
            }
        }
    }

    // ------------------------------------------------------------------
    // エンチャント
    // ------------------------------------------------------------------

    public static int enchantLevel(Enchantment enchantment, LivingEntity entity, EquipmentSlot[] slots, int current) {
        if (!(entity instanceof Player) || slots == null) return current;
        int best = current;
        for (EquipmentSlot slot : slots) {
            if (!slot.isArmor()) continue;
            for (Entry e : entries(entity, slot)) {
                int lvl = e.stack().getEnchantmentLevel(enchantment);
                if (lvl > best) best = lvl;
            }
        }
        return best;
    }

    public static Map.Entry<EquipmentSlot, ItemStack> randomWith(Enchantment enchantment, LivingEntity entity, EquipmentSlot[] slots,
                                                                 Predicate<ItemStack> filter, Map.Entry<EquipmentSlot, ItemStack> current) {
        if (!(entity instanceof Player) || slots == null) return current;
        List<Map.Entry<EquipmentSlot, ItemStack>> candidates = new ArrayList<>();
        for (EquipmentSlot slot : slots) {
            if (!slot.isArmor()) continue;
            for (Entry e : entries(entity, slot)) {
                ItemStack stack = e.stack();
                if (stack.getEnchantmentLevel(enchantment) > 0 && filter.test(stack)) {
                    candidates.add(new AbstractMap.SimpleImmutableEntry<>(slot, stack));
                }
            }
        }
        if (candidates.isEmpty()) return current;
        int total = candidates.size() + (current != null ? 1 : 0);
        int pick = entity.getRandom().nextInt(total);
        if (pick < candidates.size()) return candidates.get(pick);
        return current;
    }

    // ------------------------------------------------------------------
    // エリトラ
    // ------------------------------------------------------------------

    public static ItemStack elytra(LivingEntity entity, ItemStack vanillaChest) {
        if (!(entity instanceof Player)) return vanillaChest;
        try {
            if (vanillaChest.canElytraFly(entity)) return vanillaChest;
            ItemStack layered = first(entity, EquipmentSlot.CHEST, s -> s.canElytraFly(entity));
            return layered.isEmpty() ? vanillaChest : layered;
        } catch (Throwable t) {
            return vanillaChest;
        }
    }

    // ------------------------------------------------------------------
    // 他MODの「胴の装備を見る」処理向け
    // ------------------------------------------------------------------

    /**
     * バニラ欄のアイテムが指定MODのものならそれを、違えば重ね着スロットにある指定MODのアイテムを返す。
     * どちらにも無ければバニラ欄のアイテムを返す。
     */
    public static ItemStack prefer(LivingEntity entity, EquipmentSlot slot, String namespace) {
        ItemStack vanilla = entity.getItemBySlot(slot);
        if (!slot.isArmor() || !(entity instanceof Player)) return vanilla;
        if (isFrom(vanilla, namespace)) return vanilla;
        try {
            ItemStack layered = first(entity, slot, s -> isFrom(s, namespace));
            return layered.isEmpty() ? vanilla : layered;
        } catch (Throwable t) {
            return vanilla;
        }
    }

    public static boolean isFrom(ItemStack stack, String namespace) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && key.getNamespace().equals(namespace);
    }
}
