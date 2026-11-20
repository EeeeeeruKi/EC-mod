package com.eeeeeeruki.minecrafttweak.feature.tweaks;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.nio.file.Files;
import java.nio.file.Path;
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
 */
public class EnergyCubeStackFeature extends Feature {
    public static EnergyCubeStackFeature INSTANCE;
    public ModConfigSpec.IntValue maxStack;

    private static final ResourceLocation ENERGY_COMPONENT = ResourceLocation.fromNamespaceAndPath("mekanism", "energy");

    public EnergyCubeStackFeature() {
        super("energy_cube_stack", FeatureCategory.TWEAKS,
                "Mekanism energy cubes of the same tier and the same energy stack. Picking up cubes that do not stack throws away energy so that they do (restart needed for the stack size).");
        INSTANCE = this;
    }

    @Override
    protected void addOptions(ModConfigSpec.Builder builder) {
        maxStack = intOption(builder, "max_stack", "How many energy cubes can be stacked (restart needed).", 64, 2, 64);
    }

    @Override
    public void register(IEventBus modBus) {
        modBus.addListener(this::onModifyComponents);
        NeoForge.EVENT_BUS.addListener(this::onPickup);
    }

    // ===== どれがエネルギーキューブか =====

    public static boolean isCube(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals("mekanism") && id.getPath().endsWith("energy_cube")
                && !id.getPath().startsWith("creative");
    }

    // ===== 重ねられる数（起動時） =====

    /** 設定がまだ読み込まれていないこともあるので、そのときはファイルを直接読む。 */
    private int startupStackSize() {
        if (com.eeeeeeruki.minecrafttweak.core.TweakConfig.SPEC.isLoaded()) {
            return isEnabled() ? maxStack.get() : 1;
        }
        Path file = FMLPaths.CONFIGDIR.get().resolve(MinecraftTweak.MODID + ".toml");
        if (!Files.isRegularFile(file)) {
            return 1;
        }
        try (CommentedFileConfig config = CommentedFileConfig.of(file)) {
            config.load();
            Object enabled = config.get(List.of(category().id(), id(), "enabled"));
            if (!Boolean.TRUE.equals(enabled)) {
                return 1;
            }
            Object size = config.get(List.of(category().id(), id(), "max_stack"));
            return size instanceof Number n ? Math.max(2, Math.min(64, n.intValue())) : 64;
        } catch (Exception e) {
            return 1;
        }
    }

    private void onModifyComponents(ModifyDefaultComponentsEvent event) {
        int size = startupStackSize();
        if (size <= 1) {
            return;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (isCube(item)) {
                event.modify(item, builder -> builder.set(DataComponents.MAX_STACK_SIZE, size));
                MinecraftTweak.LOGGER.info("minEcraft twEEk: {} stacks to {}", BuiltInRegistries.ITEM.getKey(item), size);
            }
        }
    }

    // ===== FEの読み書き =====

    private enum Group { LOW, HIGH, FULL, UNKNOWN }

    private static double ratio(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy == null || energy.getMaxEnergyStored() <= 0) {
            return -1;
        }
        return (double) energy.getEnergyStored() / energy.getMaxEnergyStored();
    }

    private static Group group(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy == null || energy.getMaxEnergyStored() <= 0) {
            return Group.UNKNOWN;
        }
        if (energy.getEnergyStored() >= energy.getMaxEnergyStored()) {
            return Group.FULL;
        }
        return (double) energy.getEnergyStored() / energy.getMaxEnergyStored() >= 0.5 ? Group.HIGH : Group.LOW;
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<Object> energyType() {
        return (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ENERGY_COMPONENT);
    }

    private static boolean sameEnergy(ItemStack a, ItemStack b) {
        DataComponentType<Object> type = energyType();
        return type != null && Objects.equals(a.get(type), b.get(type));
    }

    /** from のFEを to にそのまま写す（to のFEは捨てる）。 */
    private static void copyEnergy(ItemStack from, ItemStack to) {
        DataComponentType<Object> type = energyType();
        if (type == null) {
            return;
        }
        Object value = from.get(type);
        if (value != null) {
            to.set(type, value);
        } else {
            to.remove(type);
        }
    }

    // ===== 拾ったとき =====

    private void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        ItemStack incoming = entity.getItem();
        if (player.level().isClientSide() || incoming.isEmpty() || !isCube(incoming.getItem()) || energyType() == null) {
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
                if (!b.isEmpty() && ItemStack.isSameItemSameComponents(a, b)) {
                    int move = Math.min(b.getCount(), a.getMaxStackSize() - a.getCount());
                    a.grow(move);
                    b.shrink(move);
                }
            }
        }
    }
}
