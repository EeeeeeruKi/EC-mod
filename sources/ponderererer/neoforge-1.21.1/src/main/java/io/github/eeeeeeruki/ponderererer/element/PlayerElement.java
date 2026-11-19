package io.github.eeeeeeruki.ponderererer.element;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * A posable player figure for Ponder scenes. Wears and holds any item
 * (so mod armor like jetpacks or MekaSuit render as they do in game).
 */
public class PlayerElement extends FxElement {
    public enum PlayerPose { STAND, WALK, SPRINT, SNEAK, FLY, ELYTRA, USE_ITEM }

    private static final UUID STEVE = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");

    private final AnimatedVec position;
    private float yaw, prevYaw, yawFrom, yawTo;
    private int yawDuration, yawElapsed;
    private float pitch;
    private PlayerPose pose = PlayerPose.STAND;
    private final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    private boolean swingQueued;
    private int glideTicks;
    private FxPlayer entity;
    private final UUID skin;

    public PlayerElement(Vec3 position, float yaw) {
        this(position, yaw, STEVE);
    }

    public PlayerElement(Vec3 position, float yaw, UUID skin) {
        this.position = new AnimatedVec(position);
        this.yaw = this.prevYaw = this.yawTo = yaw;
        this.skin = skin;
        this.alpha = this.prevAlpha = 1f;
    }

    public void setPose(PlayerPose pose) {
        this.pose = pose;
        if (entity != null) applyPose();
    }

    public void equip(EquipmentSlot slot, ItemStack stack) {
        equipment.put(slot, stack.copy());
        if (entity != null) entity.setItemSlot(slot, stack.copy());
    }

    public void moveTo(Vec3 target, int ticks) {
        position.moveTo(target, ticks);
    }

    public void rotateTo(float targetYaw, int ticks) {
        yawFrom = yaw;
        yawTo = targetYaw;
        yawDuration = Math.max(0, ticks);
        yawElapsed = 0;
        if (yawDuration == 0) yaw = prevYaw = targetYaw;
    }

    public void lookPitch(float pitch) {
        this.pitch = pitch;
    }

    public void swing() {
        swingQueued = true;
    }

    private FxPlayer entity() {
        if (entity == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return null;
            entity = new FxPlayer(mc.level, skin, "");
            equipment.forEach((s, st) -> entity.setItemSlot(s, st.copy()));
            applyPose();
        }
        return entity;
    }

    private void applyPose() {
        entity.setShiftKeyDown(pose == PlayerPose.SNEAK);
        entity.setSprinting(pose == PlayerPose.SPRINT);
        entity.setPose(pose == PlayerPose.SNEAK ? Pose.CROUCHING
                : pose == PlayerPose.ELYTRA ? Pose.FALL_FLYING : Pose.STANDING);
        if (pose == PlayerPose.ELYTRA) entity.startFallFlying();
        else entity.stopFallFlying();
        if (pose == PlayerPose.USE_ITEM) {
            entity.startUsingItem(InteractionHand.MAIN_HAND);
            entity.setUsing(true);
        } else if (entity.isUsingItem()) {
            entity.stopUsingItem();
            entity.setUsing(false);
        }
    }

    @Override
    protected void update(PonderScene scene) {
        Vec3 before = position.get(1f);
        position.tick();
        prevYaw = yaw;
        if (yawElapsed < yawDuration) {
            yawElapsed++;
            yaw = Mth.rotLerp((float) yawElapsed / yawDuration, yawFrom, yawTo);
        } else {
            yaw = yawTo;
        }

        FxPlayer e = entity();
        if (e == null) return;
        Vec3 delta = position.get(1f).subtract(before);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float speed = switch (pose) {
            case WALK -> 0.6f;
            case SPRINT -> 1.0f;
            default -> (float) Math.min(1.0, horizontal * 4);
        };
        e.walkAnimation.update(pose == PlayerPose.FLY || pose == PlayerPose.ELYTRA ? 0f : speed, 0.4f);
        e.setDeltaMovement(pose == PlayerPose.ELYTRA ? Vec3.directionFromRotation(0, yaw).scale(0.5) : delta);
        if (pose == PlayerPose.ELYTRA) {
            glideTicks++;
            e.setFallFlyTicks(glideTicks);
            if (!e.isFallFlying()) e.startFallFlying();
        } else {
            glideTicks = 0;
            e.setFallFlyTicks(0);
        }
        if (swingQueued) {
            swingQueued = false;
            e.swing(InteractionHand.MAIN_HAND);
        }
        e.stepSwing();
        e.tickCount++;
    }

    @Override
    protected void draw(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt, float a) {
        FxPlayer e = entity();
        if (e == null) return;
        float y = Mth.rotLerp(pt, prevYaw, yaw);
        e.setYRot(y);
        e.yRotO = y;
        e.yBodyRot = e.yBodyRotO = y;
        e.yHeadRot = e.yHeadRotO = y;
        e.setXRot(pitch);
        e.xRotO = pitch;

        Vec3 p = position.get(pt);
        PoseStack ps = graphics.pose();
        ps.pushPose();
        ps.translate(p.x, p.y, p.z);
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        boolean shadows = Minecraft.getInstance().options.entityShadows().get();
        dispatcher.setRenderShadow(false);
        try {
            dispatcher.render(e, 0, 0, 0, y, pt, ps, buffer, LightTexture.FULL_BRIGHT);
        } finally {
            dispatcher.setRenderShadow(shadows);
        }
        ps.popPose();
    }
}
