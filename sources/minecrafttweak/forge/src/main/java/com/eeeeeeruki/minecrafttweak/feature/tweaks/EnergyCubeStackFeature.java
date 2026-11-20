package com.eeeeeeruki.minecrafttweak.feature.tweaks;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mekanism のエネルギーキューブを重ねられるようにする（クリエイティブは除く）。
 *
 * 仕様:
 * - 同じティアで、同じFEのキューブは重なる。
 * - 同じティアのキューブを拾ったとき、持っているキューブと重ならないなら、FEを捨てて重ねる。
 *   低いほうに合わせる（例: 2% と 0% → 2% のFEを捨てて 0% にする。60% と 70% → 70% を 60% に。
 *   そのあと 50% を拾ったら 60% を 50% に）。
 * - 「50%以上」「50%未満」「100%」の3つの組に分けて、組をまたいではFEを捨てない。
 *   100% のキューブはFEを捨てない（100%どうしはそのまま重なる）。
 *   例: 20%・40%・50%・70%・100% を拾うと、20%×2・50%×2・100%×1 になる。
 * - 重ねられる数はゲームを起動したときに決まる（設定を変えたら再起動が必要）。
 *
 * 1.20.1 の Mekanism は、FEを NBT の mekData.EnergyContainers に入れている。
 */
public class EnergyCubeStackFeature extends Feature {
    public static EnergyCubeStackFeature INSTANCE;
    public ForgeConfigSpec.IntValue maxStack;

    private static final String MEK_DATA = "mekData";
    private static final String ENERGY = "EnergyContainers";

    public EnergyCubeStackFeature() {
        super("energy_cube_stack", FeatureCategory.TWEAKS,
                "Mekanism energy cubes of the same tier and the same energy stack. Picking up cubes that do not stack throws away energy so that they do (restart needed for the stack size).");
        INSTANCE = this;
    }

    @Override
    protected void addOptions(ForgeConfigSpec.Builder builder) {
        maxStack = intOption(builder, "max_stack", "How many energy cubes can be stacked (restart needed).", 64, 2, 64);
    }

    @Override
    public void register(IEventBus modBus) {
        modBus.addListener(this::onSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onPickup);
    }

    public static boolean isCube(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals("mekanism") && id.getPath().endsWith("energy_cube")
                && !id.getPath().startsWith("creative");
    }

    // ===== 重ねられる数（起動時） =====

    /** Item の「重ねられる数」のフィールド。名前に頼らず、バニラのアイテムの値で見分ける。 */
    private static Field stackSizeField() {
        for (Field f : Item.class.getDeclaredFields()) {
            if (f.getType() != int.class || Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            try {
                f.setAccessible(true);
                if (f.getInt(Items.DIRT) == 64 && f.getInt(Items.ENDER_PEARL) == 16 && f.getInt(Items.DIAMOND_SWORD) == 1) {
                    return f;
                }
            } catch (Throwable ignored) {
                // 次のフィールドへ
            }
        }
        return null;
    }

    private void onSetup(FMLCommonSetupEvent event) {
        if (!isEnabled()) {
            return;
        }
        int size = maxStack.get();
        event.enqueueWork(() -> {
            Field field = stackSizeField();
            if (field == null) {
                MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not change the stack size of energy cubes");
                return;
            }
            for (Item item : BuiltInRegistries.ITEM) {
                if (isCube(item)) {
                    try {
                        field.setInt(item, size);
                        MinecraftTweak.LOGGER.info("minEcraft twEEk: {} stacks to {}", BuiltInRegistries.ITEM.getKey(item), size);
                    } catch (Throwable t) {
                        MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not change the stack size of {}", item, t);
                    }
                }
            }
        });
    }

    // ===== FEの読み書き =====

    private enum Group { LOW, HIGH, FULL, UNKNOWN }

    private static IEnergyStorage energy(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
    }

    private static double ratio(ItemStack stack) {
        IEnergyStorage e = energy(stack);
        if (e == null || e.getMaxEnergyStored() <= 0) {
            return -1;
        }
        return (double) e.getEnergyStored() / e.getMaxEnergyStored();
    }

    private static Group group(ItemStack stack) {
        IEnergyStorage e = energy(stack);
        if (e == null || e.getMaxEnergyStored() <= 0) {
            return Group.UNKNOWN;
        }
        if (e.getEnergyStored() >= e.getMaxEnergyStored()) {
            return Group.FULL;
        }
        return (double) e.getEnergyStored() / e.getMaxEnergyStored() >= 0.5 ? Group.HIGH : Group.LOW;
    }

    private static Tag energyTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MEK_DATA, Tag.TAG_COMPOUND)) {
            return null;
        }
        return tag.getCompound(MEK_DATA).get(ENERGY);
    }

    private static boolean sameEnergy(ItemStack a, ItemStack b) {
        return Objects.equals(energyTag(a), energyTag(b));
    }

    /** from のFEを to にそのまま写す（to のFEは捨てる）。 */
    private static void copyEnergy(ItemStack from, ItemStack to) {
        Tag value = energyTag(from);
        CompoundTag tag = to.getOrCreateTag();
        CompoundTag mek = tag.getCompound(MEK_DATA);
        if (value != null) {
            mek.put(ENERGY, value.copy());
        } else {
            mek.remove(ENERGY);
        }
        if (mek.isEmpty()) {
            tag.remove(MEK_DATA);
        } else {
            tag.put(MEK_DATA, mek);
        }
        if (tag.isEmpty()) {
            to.setTag(null);
        }
    }

    // ===== 拾ったとき =====

    private void onPickup(EntityItemPickupEvent event) {
        if (!isEnabled()) {
            return;
        }
        Player player = event.getEntity();
        ItemEntity entity = event.getItem();
        ItemStack incoming = entity.getItem();
        if (player.level().isClientSide() || incoming.isEmpty() || !isCube(incoming.getItem())) {
            return;
        }
        if (incoming.getMaxStackSize() <= 1) {
            return; // 重ねられる数が変わっていない（再起動していない）
        }
        Group group = group(incoming);
        if (group == Group.FULL || group == Group.UNKNOWN) {
            return;
        }
        Inventory inv = player.getInventory();
        List<ItemStack> same = new ArrayList<>();
        for (ItemStack s : inv.items) {
            if (!s.isEmpty() && s.getItem() == incoming.getItem() && group(s) == group) {
                same.add(s);
            }
        }
        for (ItemStack s : inv.offhand) {
            if (!s.isEmpty() && s.getItem() == incoming.getItem() && group(s) == group) {
                same.add(s);
            }
        }
        if (same.isEmpty()) {
            return;
        }
        // 組の中でいちばん低いキューブに合わせる
        ItemStack lowest = incoming;
        double low = ratio(incoming);
        for (ItemStack s : same) {
            double r = ratio(s);
            if (r >= 0 && r < low) {
                lowest = s;
                low = r;
            }
        }
        boolean changed = false;
        if (lowest != incoming && !sameEnergy(lowest, incoming)) {
            ItemStack copy = incoming.copy();
            copyEnergy(lowest, copy);
            entity.setItem(copy);
            incoming = copy;
            changed = true;
        }
        for (ItemStack s : same) {
            if (s != lowest && !sameEnergy(lowest, s)) {
                copyEnergy(lowest, s);
                changed = true;
            }
        }
        if (changed) {
            compact(inv, incoming.getItem());
            inv.setChanged();
        }
    }

    /** 同じになったキューブを、インベントリの中で1つの山にまとめる。 */
    private static void compact(Inventory inv, Item item) {
        List<ItemStack> slots = new ArrayList<>(inv.items);
        slots.addAll(inv.offhand);
        for (int i = 0; i < slots.size(); i++) {
            ItemStack a = slots.get(i);
            if (a.isEmpty() || a.getItem() != item) {
                continue;
            }
            for (int j = i + 1; j < slots.size() && a.getCount() < a.getMaxStackSize(); j++) {
                ItemStack b = slots.get(j);
                if (!b.isEmpty() && ItemStack.isSameItemSameTags(a, b)) {
                    int move = Math.min(b.getCount(), a.getMaxStackSize() - a.getCount());
                    a.grow(move);
                    b.shrink(move);
                }
            }
        }
    }
}
