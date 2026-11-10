package dev.eeeeeeruki.layeredarmor.mixin;

import dev.eeeeeeruki.layeredarmor.Layers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    /** 防具を全部調べる処理に重ね着の防具も渡す */
    @Inject(method = "getArmorSlots", at = @At("RETURN"), cancellable = true)
    private void layeredarmor$armorSlots(CallbackInfoReturnable<Iterable<ItemStack>> cir) {
        Iterable<ItemStack> wrapped = Layers.wrapArmorSlots((Player) (Object) this, cir.getReturnValue());
        if (wrapped != null) cir.setReturnValue(wrapped);
    }

    /** 胴の重ね着スロットのエリトラでも滑空を始められるようにする */
    @ModifyVariable(method = "tryToStartFallFlying", at = @At("STORE"), ordinal = 0, require = 0)
    private ItemStack layeredarmor$startFallFlying(ItemStack chest) {
        return Layers.elytra((Player) (Object) this, chest);
    }
}
