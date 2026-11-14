package dev.eeeeeeruki.layeredarmor.client;

import dev.eeeeeeruki.layeredarmor.Layers;
import dev.eeeeeeruki.layeredarmor.compat.CorpseCompat;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 見た目の処理（クライアント専用）。
 *
 * ルール：
 *  ・バニラの防具欄（Cosmetic Armor Reworkedが入っていればその結果）に何か見えていれば、それを表示する
 *  ・何も見えていない（空・Cosmetic Armorで透明）なら、重ね着スロットの防具を表示する
 *  ・重ね着スロットの見た目はCosmetic Armorの設定の影響を受けない（Curiosの表示切り替えだけ見る）
 */
public final class ClientLayers {
    private ClientLayers() {}

    /** レイヤー描画中の entity / slot（描画スレッドだけで使う） */
    public static LivingEntity renderEntity;
    public static EquipmentSlot renderSlot;

    /** 描画前（Cosmetic Armorが中身を入れ替える前）のバニラ防具欄。死体（Corpse）の見た目用 */
    private static final Map<Player, ItemStack[]> CAPTURED = new WeakHashMap<>();
    private static final Map<ItemStack, ItemStack> UNDER_CACHE = new WeakHashMap<>();

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ClientLayers::onRenderPre);
    }

    private static void onRenderPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        NonNullList<ItemStack> armor = player.getInventory().armor;
        boolean tagged = false;
        for (ItemStack s : armor) {
            if (CorpseCompat.hasDisplayTag(s)) {
                tagged = true;
                break;
            }
        }
        if (tagged) {
            CAPTURED.put(player, armor.toArray(new ItemStack[0]));
        } else {
            CAPTURED.remove(player);
        }
    }

    /** 表示ルールを当てはめた「見た目のアイテム」 */
    public static ItemStack display(LivingEntity entity, EquipmentSlot slot, ItemStack current) {
        if (current == null) current = ItemStack.EMPTY;
        if (!current.isEmpty() || entity == null || slot == null || !slot.isArmor() || !(entity instanceof Player player)) {
            return current;
        }
        try {
            ItemStack layered = Layers.visible(player, slot);
            if (!layered.isEmpty()) return layered;
            ItemStack[] captured = CAPTURED.get(player);
            if (captured != null && slot.getIndex() < captured.length) {
                ItemStack c = captured[slot.getIndex()];
                if (CorpseCompat.isLayeredCopy(c)) return c;
                CompoundTag under = CorpseCompat.underTag(c);
                if (under != null) return UNDER_CACHE.computeIfAbsent(c, k -> ItemStack.of(under));
            }
        } catch (Throwable ignored) {
        }
        return current;
    }

    /**
     * 防具レイヤー用。バニラの胴欄がエリトラなどの「防具ではないもの」なら、胴の重ね着の防具も表示する
     * （エリトラは防具として描かれないので、重ね着の胴防具が隠れてしまわないように）。
     */
    public static ItemStack displayArmor(LivingEntity entity, EquipmentSlot slot, ItemStack current) {
        if (slot == EquipmentSlot.CHEST && current != null && !current.isEmpty() && !(current.getItem() instanceof ArmorItem)
                && entity instanceof Player player) {
            try {
                ItemStack layered = Layers.visible(player, slot);
                if (!layered.isEmpty()) return layered;
            } catch (Throwable ignored) {
            }
        }
        return display(entity, slot, current);
    }

    /**
     * エリトラレイヤー用。バニラの胴欄がエリトラならそれを、そうでなければ胴の重ね着スロットのエリトラを表示する
     * （胴の欄に胴防具を着けたまま、重ね着スロットでエリトラを使う場合でも翼が見えるように）。
     */
    public static ItemStack displayElytra(LivingEntity entity, ItemStack current) {
        if (current != null && !current.isEmpty() && entity instanceof Player player) {
            try {
                if (current.is(Items.ELYTRA) || current.canElytraFly(player)) return current;
                for (Layers.Entry e : Layers.entries(player, EquipmentSlot.CHEST)) {
                    ItemStack s = e.stack();
                    if (e.context().visible() && (s.is(Items.ELYTRA) || s.canElytraFly(player))) return s;
                }
            } catch (Throwable ignored) {
            }
            return current;
        }
        return display(entity, EquipmentSlot.CHEST, current);
    }

    public static ItemStack displayBySlot(LivingEntity entity, EquipmentSlot slot) {
        return display(entity, slot, entity.getItemBySlot(slot));
    }
}
