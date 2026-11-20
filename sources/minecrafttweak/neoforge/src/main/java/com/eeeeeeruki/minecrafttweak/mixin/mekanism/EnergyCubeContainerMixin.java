package com.eeeeeeruki.minecrafttweak.mixin.mekanism;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.attachments.containers.ComponentBackedContainer;
import mekanism.common.attachments.containers.energy.ComponentBackedEnergyCubeContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * 重なっているエネルギーキューブの充電・放電を、重なっている数だけ遅くする。
 * Mekanism はキューブの山ぜんぶで1つのFEの値を持っているので、そのままだと
 * 1つ分の電気で山ぜんぶが充電されてしまう。そこで、入れる量（出す量）を数で割って、
 * 受け取った量は数をかけて返す（山ぜんぶでちょうどの量になる）。
 */
@Mixin(value = ComponentBackedEnergyCubeContainer.class, remap = false)
public abstract class EnergyCubeContainerMixin {
    private static final ThreadLocal<Boolean> MT_INSIDE = ThreadLocal.withInitial(() -> false);
    private static Field mtAttachedTo;

    private int mt$count() {
        try {
            if (mtAttachedTo == null) {
                Field f = ComponentBackedContainer.class.getDeclaredField("attachedTo");
                f.setAccessible(true);
                mtAttachedTo = f;
            }
            ItemStack stack = (ItemStack) mtAttachedTo.get(this);
            return stack == null ? 1 : stack.getCount();
        } catch (Throwable t) {
            return 1;
        }
    }

    @Inject(method = "insert(JLmekanism/api/Action;Lmekanism/api/AutomationType;)J", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mt$insert(long amount, Action action, AutomationType type, CallbackInfoReturnable<Long> cir) {
        int count = mt$count();
        if (count <= 1 || MT_INSIDE.get() || amount <= 0) {
            return;
        }
        MT_INSIDE.set(true);
        try {
            long each = amount / count;
            if (each <= 0) {
                cir.setReturnValue(amount);
                return;
            }
            long left = ((IEnergyContainer) (Object) this).insert(each, action, type);
            long taken = (each - left) * count;
            cir.setReturnValue(amount - taken);
        } finally {
            MT_INSIDE.set(false);
        }
    }

    @Inject(method = "extract(JLmekanism/api/Action;Lmekanism/api/AutomationType;)J", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mt$extract(long amount, Action action, AutomationType type, CallbackInfoReturnable<Long> cir) {
        int count = mt$count();
        if (count <= 1 || MT_INSIDE.get() || amount <= 0) {
            return;
        }
        MT_INSIDE.set(true);
        try {
            long each = amount / count;
            if (each <= 0) {
                cir.setReturnValue(0L);
                return;
            }
            long got = ((IEnergyContainer) (Object) this).extract(each, action, type);
            cir.setReturnValue(got * count);
        } finally {
            MT_INSIDE.set(false);
        }
    }
}
