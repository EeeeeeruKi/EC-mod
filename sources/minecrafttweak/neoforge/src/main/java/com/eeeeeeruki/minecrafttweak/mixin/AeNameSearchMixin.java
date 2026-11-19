package com.eeeeeeruki.minecrafttweak.mixin;

import com.eeeeeeruki.minecrafttweak.client.feature.EnglishNames;
import com.eeeeeeruki.minecrafttweak.client.feature.EnglishSearch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * AE2 の端末の名前検索で、名前が合わなかったときに英語の名前でも調べる。
 * AE2 はコンパイルのときには使わず、AE2 の中のものはリフレクションで読む。
 */
@Pseudo
@Mixin(targets = "appeng.client.gui.me.search.NameSearchPredicate", remap = false)
public abstract class AeNameSearchMixin {
    @Shadow
    @Final
    private String term;

    @Inject(method = "test(Ljava/lang/Object;)Z", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void minecrafttweak$english(Object entry, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || term == null || term.isEmpty() || !EnglishSearch.active()) {
            return;
        }
        String en = englishName(entry);
        if (en != null && en.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT))) {
            cir.setReturnValue(true);
        }
    }

    private static String englishName(Object entry) {
        try {
            Object key = entry.getClass().getMethod("getWhat").invoke(entry);
            if (key == null) {
                return null;
            }
            // アイテム（AEItemKey）は toStack() で、ほかは名前のキーを id から作る
            try {
                Method toStack = key.getClass().getMethod("toStack");
                Object stack = toStack.invoke(key);
                if (stack instanceof ItemStack s) {
                    return EnglishNames.of(s);
                }
            } catch (NoSuchMethodException ignored) {
            }
            Object id = key.getClass().getMethod("getId").invoke(key);
            if (id instanceof ResourceLocation rl) {
                if (BuiltInRegistries.FLUID.containsKey(rl)) {
                    return EnglishNames.of("fluid_type." + rl.getNamespace() + "." + rl.getPath());
                }
                return EnglishNames.of("item." + rl.getNamespace() + "." + rl.getPath());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
        return null;
    }
}
