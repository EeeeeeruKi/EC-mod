package dev.eeeeeeruki.layeredarmor.compat;

import dev.eeeeeeruki.layeredarmor.LayeredArmor;
import dev.eeeeeeruki.layeredarmor.Layers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

/**
 * AdvancedAE：量子アーマーの設定画面を開くホットキー。
 * 本家は「防具欄の何番目か＋36」をインベントリ番号として使うので、重ね着の装備が混ざると番号がズレて開けなくなる。
 * ここで「バニラの防具欄（36〜39）→ 重ね着（仮想番号100〜）」の順に探して開く。
 * AdvancedAE本体には依存せず、リフレクションで呼ぶ。
 */
public final class AdvancedAECompat {
    private static Field locatableField;
    private static Field openerField;
    private static Method openMethod;
    private static boolean failed;

    private AdvancedAECompat() {}

    @SuppressWarnings("unchecked")
    public static boolean run(Object self, Player player) {
        if (failed) return false;
        try {
            if (locatableField == null) {
                Class<?> c = self.getClass();
                locatableField = c.getDeclaredField("locatable");
                openerField = c.getDeclaredField("opener");
                locatableField.setAccessible(true);
                openerField.setAccessible(true);
                openMethod = openerField.getType().getMethod("open", Player.class, int.class, ItemStack.class);
            }
            Predicate<ItemStack> locatable = (Predicate<ItemStack>) locatableField.get(self);
            Object opener = openerField.get(self);

            // バニラの防具欄（足・すね・胴・頭 = インベントリ36〜39）
            List<ItemStack> armor = player.getInventory().armor;
            for (int i = 0; i < armor.size(); i++) {
                ItemStack stack = armor.get(i);
                if (locatable.test(stack) && (boolean) openMethod.invoke(opener, player, 36 + i, stack)) return true;
            }
            // 重ね着スロット（仮想番号）
            List<Layers.Entry> layered = Layers.allEntries(player);
            for (int i = 0; i < layered.size(); i++) {
                Layers.Entry e = layered.get(i);
                ItemStack stack = e.handler().getStackInSlot(e.index());
                if (locatable.test(stack) && (boolean) openMethod.invoke(opener, player, Layers.virtualIndex(e), stack)) return true;
            }
            return false;
        } catch (java.lang.reflect.InvocationTargetException t) {
            // 設定画面を開く処理の中で起きた一時的なエラー。次回また試せるように、対応は止めない
            LayeredArmor.LOGGER.warn("[LayeredArmor] AdvancedAEの設定画面を開く処理でエラーが出ました", t.getCause());
            return false;
        } catch (Throwable t) {
            failed = true;
            LayeredArmor.LOGGER.warn("[LayeredArmor] AdvancedAEの設定ホットキー対応に失敗しました（本家の動作に戻します）", t);
            return false;
        }
    }

    public static boolean isFailed() {
        return failed;
    }
}
