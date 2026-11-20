package com.eeeeeeruki.minecrafttweak.mixin.mekanism;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.ItemCapabilityWrapper;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
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
@Mixin(targets = "mekanism.common.capabilities.energy.item.RateLimitEnergyHandler$EnergyCubeRateLimitEnergyContainer", remap = false)
public abstract class EnergyCubeContainerMixin {
    private static final ThreadLocal<Boolean> MT_INSIDE = ThreadLocal.withInitial(() -> false);
    private static Field mtListener;

    private int mt$count() {
        try {
            if (mtListener == null) {
                Field f = BasicEnergyContainer.class.getDeclaredField("listener");
                f.setAccessible(true);
                mtListener = f;
            }
            Object listener = mtListener.get(this);
            if (listener instanceof ItemCapabilityWrapper.ItemCapability cap && cap.getStack() != null) {
                return cap.getStack().getCount();
            }
        } catch (Throwable ignored) {
            // わからなければ1つとして扱う
        }
        return 1;
    }

    @Inject(method = "insert(Lmekanism/api/math/FloatingLong;Lmekanism/api/Action;Lmekanism/api/AutomationType;)Lmekanism/api/math/FloatingLong;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mt$insert(FloatingLong amount, Action action, AutomationType type, CallbackInfoReturnable<FloatingLong> cir) {
        int count = mt$count();
        if (count <= 1 || MT_INSIDE.get() || amount.isZero()) {
            return;
        }
        MT_INSIDE.set(true);
        try {
            FloatingLong each = amount.divide(count);
            if (each.isZero()) {
                cir.setReturnValue(amount);
                return;
            }
            FloatingLong left = ((IEnergyContainer) (Object) this).insert(each, action, type);
            FloatingLong taken = each.subtract(left).multiply(count);
            cir.setReturnValue(amount.subtract(taken));
        } finally {
            MT_INSIDE.set(false);
        }
    }

    @Inject(method = "extract(Lmekanism/api/math/FloatingLong;Lmekanism/api/Action;Lmekanism/api/AutomationType;)Lmekanism/api/math/FloatingLong;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mt$extract(FloatingLong amount, Action action, AutomationType type, CallbackInfoReturnable<FloatingLong> cir) {
        int count = mt$count();
        if (count <= 1 || MT_INSIDE.get() || amount.isZero()) {
            return;
        }
        MT_INSIDE.set(true);
        try {
            FloatingLong each = amount.divide(count);
            if (each.isZero()) {
                cir.setReturnValue(FloatingLong.ZERO);
                return;
            }
            FloatingLong got = ((IEnergyContainer) (Object) this).extract(each, action, type);
            cir.setReturnValue(got.multiply(count));
        } finally {
            MT_INSIDE.set(false);
        }
    }
}
