package dev.eeeeeeruki.layeredarmor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.AbstractList;
import java.util.List;

/**
 * Player#getArmorSlots の戻り値。
 * バニラの防具欄（4マス）のうしろに、重ね着スロットの防具をつなげたリスト。
 * 「装備している防具を全部調べる」系の処理（防護エンチャント・トゲ・Apotheosisの効果・Cold Sweatの防寒など）が
 * 重ね着の防具も見てくれるようになる。
 */
public final class LayeredArmorList extends NonNullList<ItemStack> {

    private LayeredArmorList(List<ItemStack> delegate) {
        super(delegate, ItemStack.EMPTY);
    }

    public static LayeredArmorList create(List<ItemStack> vanilla, List<Layers.Entry> extra) {
        return new LayeredArmorList(new View(vanilla, extra));
    }

    private static final class View extends AbstractList<ItemStack> {
        private final List<ItemStack> vanilla;
        private final List<Layers.Entry> extra;

        View(List<ItemStack> vanilla, List<Layers.Entry> extra) {
            this.vanilla = vanilla;
            this.extra = extra;
        }

        @Override
        public ItemStack get(int index) {
            int v = vanilla.size();
            if (index < v) return vanilla.get(index);
            Layers.Entry e = extra.get(index - v);
            return e.handler().getStackInSlot(e.index());
        }

        @Override
        public ItemStack set(int index, ItemStack element) {
            int v = vanilla.size();
            if (index < v) return vanilla.set(index, element);
            Layers.Entry e = extra.get(index - v);
            ItemStack old = e.handler().getStackInSlot(e.index());
            e.handler().setStackInSlot(e.index(), element);
            return old;
        }

        @Override
        public int size() {
            return vanilla.size() + extra.size();
        }
    }
}
