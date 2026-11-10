package dev.eeeeeeruki.layeredarmor.mixin;

import dev.eeeeeeruki.layeredarmor.Layers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /** 胴の重ね着スロットのエリトラで滑空を続けられるようにする */
    @ModifyVariable(method = "updateFallFlying", at = @At("STORE"), ordinal = 0, require = 0)
    private ItemStack layeredarmor$fallFlying(ItemStack chest) {
        return Layers.elytra((LivingEntity) (Object) this, chest);
    }
}
