package dev.eeeeeeruki.layeredarmor.mixin;

import dev.eeeeeeruki.layeredarmor.Layers;
import net.minecraft.core.NonNullList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 防具の耐久値の減り：バニラの防具欄に防具が無い部位だけ、重ね着の防具が削れる */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow @Final public Player player;
    @Shadow @Final public NonNullList<ItemStack> armor;

    /** 仮想番号（100〜）で重ね着の装備を取れるようにする（AdvancedAEの量子アーマー設定画面などが使う） */
    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void layeredarmor$getItem(int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < Layers.VIRTUAL_BASE) return;
        try {
            ItemStack stack = Layers.virtualStack(this.player, index);
            if (stack != null) cir.setReturnValue(stack);
        } catch (Throwable t) {
            dev.eeeeeeruki.layeredarmor.LayeredArmor.LOGGER.debug("[LayeredArmor] getItem failed", t);
        }
    }

    @Inject(method = "hurtArmor", at = @At("HEAD"))
    private void layeredarmor$hurtArmor(DamageSource source, float amount, int[] slots, CallbackInfo ci) {
        try {
            Layers.hurtLayers(this.player, this.armor, source, amount, slots);
        } catch (Throwable t) {
            dev.eeeeeeruki.layeredarmor.LayeredArmor.LOGGER.debug("[LayeredArmor] hurtArmor failed", t);
        }
    }
}
