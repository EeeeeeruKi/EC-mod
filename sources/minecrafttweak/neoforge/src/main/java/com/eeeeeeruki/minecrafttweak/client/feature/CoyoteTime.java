package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.feature.tweaks.CoyoteTimeFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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
        NeoForge.EVENT_BUS.addListener(CoyoteTime::onTickPre);
        NeoForge.EVENT_BUS.addListener(CoyoteTime::onTickPost);
    }

    private static boolean enabled() {
        return CoyoteTimeFeature.INSTANCE != null && CoyoteTimeFeature.INSTANCE.isEnabled();
    }

    private static void onTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
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

    private static void onTickPre(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !enabled()) {
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
        double power = player.getAttributeValue(Attributes.JUMP_STRENGTH);
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
