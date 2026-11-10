package dev.eeeeeeruki.layeredarmor.mixin.client;

import dev.eeeeeeruki.layeredarmor.Layers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** ジャンプ長押しでエリトラを開くとき、胴の重ね着スロットのエリトラも見る */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @ModifyVariable(method = "aiStep", at = @At("STORE"), ordinal = 0, require = 0)
    private ItemStack layeredarmor$elytra(ItemStack chest) {
        return Layers.elytra((LocalPlayer) (Object) this, chest);
    }
}
