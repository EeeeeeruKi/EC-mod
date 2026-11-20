package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.feature.tweaks.CoyoteTimeFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * コヨーテタイムの中身。
 * 地面から離れて何ティックたったかを数え、「歩いて落ちた（上に飛んでいない）」ときだけ、
 * 決めたティック数のうちにジャンプボタンが押されていればジャンプさせる。1回落ちるごとに1回だけ。
 */
public final class CoyoteTime {
    private static int airTicks;
    private static boolean used;

    private CoyoteTime() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(CoyoteTime::onTick);
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.side != net.minecraftforge.fml.LogicalSide.CLIENT) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            onTickPre(event);
        } else {
            onTickPost(event);
        }
    }

    private static boolean enabled() {
        return CoyoteTimeFeature.INSTANCE != null && CoyoteTimeFeature.INSTANCE.isEnabled();
    }

    private static void onTickPost(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof LocalPlayer player)) {
            return;
        }
        if (player.onGround()) {
            airTicks = 0;
            used = false;
        } else {
            if (airTicks == 0 && player.getDeltaMovement().y > 0) {
                used = true; // 自分でジャンプして離れた
            }
            airTicks++;
        }
    }

    private static void onTickPre(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof LocalPlayer player) || !enabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (used || player.onGround() || airTicks == 0 || airTicks > CoyoteTimeFeature.INSTANCE.ticks.get()) {
            return;
        }
        if (!mc.options.keyJump.isDown() || mc.screen != null) {
            return;
        }
        if (player.getAbilities().flying || player.isFallFlying() || player.isPassenger() || player.onClimbable()
                || player.isInWater() || player.isInLava() || player.getDeltaMovement().y > 0) {
            return;
        }
        used = true;
        double power = 0.42; // 1.20.1 のふつうのジャンプの強さ
        MobEffectInstance jump = player.getEffect(MobEffects.JUMP);
        if (jump != null) {
            power += 0.1 * (jump.getAmplifier() + 1);
        }
        Vec3 v = player.getDeltaMovement();
        double vx = v.x;
        double vz = v.z;
        if (player.isSprinting()) {
            float rad = player.getYRot() * Mth.DEG_TO_RAD;
            vx += -Mth.sin(rad) * 0.2;
            vz += Mth.cos(rad) * 0.2;
        }
        player.setDeltaMovement(vx, power, vz);
        player.hasImpulse = true;
        player.resetFallDistance();
    }
}
