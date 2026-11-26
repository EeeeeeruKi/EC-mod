package com.eeeeeeruki.skyriders.mixin.client;

import com.eeeeeeruki.skyriders.entity.MachineEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 滑空中は、ライダーが足を後ろに伸ばした姿勢になる */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void skyriders$glidePose(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress,
                                     float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity.getVehicle() instanceof MachineEntity m) || !m.isGlidingVisual()) return;
        BipedEntityModel<?> self = (BipedEntityModel<?>) (Object) this;
        self.rightLeg.pitch = 1.15f;
        self.leftLeg.pitch = 1.15f;
        self.rightLeg.yaw = 0f;
        self.leftLeg.yaw = 0f;
        self.rightLeg.roll = 0.08f;
        self.leftLeg.roll = -0.08f;
    }
}
