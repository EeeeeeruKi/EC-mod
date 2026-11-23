package com.eeeeeeruki.minecrafttweak.mixin.compat;

import com.eeeeeeruki.minecrafttweak.feature.fixes.DeepAetherEotsFixFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Deep Aether（1.21.1-1.1.5.1）の不具合よけ。
 * ボス「EOTS」の体の一部（EOTSSegment）の攻撃のAIは、攻撃の途中で狙う相手がいなくなる（倒れた・離れたなど）と、
 * いない相手（null）を動かそうとしてサーバーが止まる（NullPointerException）。
 * 相手がいないときは、その「相手を動かす」処理だけを飛ばす。Deep Aether が入っていないときは何もしない。
 */
@Pseudo
@Mixin(targets = "io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment$EotsAttackGoal", remap = false)
public abstract class DeepAetherEotsGoalMixin {
    @WrapOperation(method = "tick()V", remap = false, require = 0,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void minecrafttweak$skipMissingTarget(LivingEntity target, Vec3 motion, Operation<Void> original) {
        if (target != null || !DeepAetherEotsFixFeature.active()) {
            original.call(target, motion);
        }
    }
}
