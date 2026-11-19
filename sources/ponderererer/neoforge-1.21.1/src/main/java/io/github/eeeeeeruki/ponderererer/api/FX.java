package io.github.eeeeeeruki.ponderererer.api;

import io.github.eeeeeeruki.ponderererer.world.TransmitterLinker;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;

import io.github.eeeeeeruki.ponderererer.element.BeamElement;
import io.github.eeeeeeruki.ponderererer.element.FxElement;
import io.github.eeeeeeruki.ponderererer.element.FxOverlay;
import io.github.eeeeeeruki.ponderererer.element.GaugeElement;
import io.github.eeeeeeruki.ponderererer.element.LightningElement;
import io.github.eeeeeeruki.ponderererer.element.PanelElement;
import io.github.eeeeeeruki.ponderererer.element.PlayerElement;
import io.github.eeeeeeruki.ponderererer.element.ScreenTintElement;
import io.github.eeeeeeruki.ponderererer.element.SoundElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Entry point of the Ponderererer API. Wrap your scene once and call the effects:
 * <pre>
 * FX fx = FX.of(scene);
 * var laser = fx.laser(util.vector().centerOf(1, 1, 1), util.vector().centerOf(4, 1, 1), 0xFFFF2020, 0.08f);
 * scene.idle(40);
 * fx.hide(laser, 10);
 * </pre>
 * Every call adds an instruction at the current point of the scene, just like the normal Ponder instructions.
 */
public final class FX {
    private final SceneBuilder scene;

    private FX(SceneBuilder scene) {
        this.scene = scene;
    }

    public static FX of(SceneBuilder scene) {
        return new FX(scene);
    }

    // ------------------------------------------------------------------ generic

    private <T extends PonderElement> ElementLink<T> add(Class<T> type, Supplier<T> factory) {
        ElementLink<T> link = new ElementLinkImpl<>(type);
        scene.addInstruction(ps -> {
            T element = factory.get();
            ps.addElement(element);
            ps.linkElement(element, link);
        });
        return link;
    }

    private <T extends PonderElement> void modify(ElementLink<T> link, Consumer<T> action) {
        scene.addInstruction(ps -> ps.resolveOptional(link).ifPresent(action));
    }

    /** Fades out and removes any Ponderererer element. */
    public void hide(ElementLink<? extends PonderElement> link, int fadeTicks) {
        scene.addInstruction(ps -> ps.resolveOptional(link).ifPresent(e -> {
            if (e instanceof FxElement fx) fx.fadeOut(fadeTicks);
            else if (e instanceof FxOverlay ov) ov.fadeOut(fadeTicks);
            else e.setVisible(false);
        }));
    }

    // ------------------------------------------------------------------ beams / lasers

    /** A glowing laser beam. color is ARGB (alpha 0 means opaque), width is in blocks (0.05 - 0.2 looks good). */
    public ElementLink<BeamElement> laser(Vec3 from, Vec3 to, int color, float width) {
        return add(BeamElement.class, () -> {
            BeamElement b = new BeamElement(from, to, color, width);
            b.fadeIn(4);
            return b;
        });
    }

    /** Same as {@link #laser} but the beam shoots out from the start over the given ticks. */
    public ElementLink<BeamElement> laserShoot(Vec3 from, Vec3 to, int color, float width, int ticks) {
        return add(BeamElement.class, () -> {
            BeamElement b = new BeamElement(from, to, color, width).growIn(ticks);
            b.fadeIn(1);
            return b;
        });
    }

    public void moveLaserEnd(ElementLink<BeamElement> link, Vec3 to, int ticks) {
        modify(link, b -> b.moveEnd(to, ticks));
    }

    public void moveLaserStart(ElementLink<BeamElement> link, Vec3 to, int ticks) {
        modify(link, b -> b.moveStart(to, ticks));
    }

    // ------------------------------------------------------------------ lightning

    /** A crackling electric arc. Stays until hidden. */
    public ElementLink<LightningElement> lightning(Vec3 from, Vec3 to, int color) {
        return lightning(from, to, color, 8, 0.15f, 1);
    }

    public ElementLink<LightningElement> lightning(Vec3 from, Vec3 to, int color, int segments, float jitter, int branches) {
        return add(LightningElement.class, () -> new LightningElement(from, to, color, segments, jitter, branches));
    }

    /** A short zap: shows an arc for the given ticks then fades it out. */
    public void zap(Vec3 from, Vec3 to, int color, int ticks) {
        ElementLink<LightningElement> link = lightning(from, to, color);
        scene.addInstruction(new DelayedInstruction(ticks, ps -> ps.resolveOptional(link).ifPresent(e -> e.fadeOut(3))));
    }

    // ------------------------------------------------------------------ explosions, particles, sounds

    /**
     * An explosion: particles, a flash, a sound and (optionally) removes the blocks in the radius.
     */
    public void explosion(Vec3 center, float radius, boolean breakBlocks) {
        scene.addInstruction(ps -> {
            PonderLevel world = ps.getWorld();
            RandomSource r = RandomSource.create();
            if (breakBlocks) {
                BlockPos c = BlockPos.containing(center);
                int ri = (int) Math.ceil(radius);
                BlockState air = Blocks.AIR.defaultBlockState();
                for (BlockPos pos : BlockPos.betweenClosed(c.offset(-ri, -ri, -ri), c.offset(ri, ri, ri))) {
                    if (Vec3.atCenterOf(pos).distanceTo(center) > radius) continue;
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;
                    // keep the base plate and unbreakable blocks (barriers, bedrock) intact
                    if (pos.getY() <= world.getBounds().minY()) continue;
                    if (state.getDestroySpeed(world, pos) < 0) continue;
                    world.addBlockDestroyEffects(pos.immutable(), state);
                    world.setBlock(pos.immutable(), air, 3);
                }
                ps.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
            }
            for (int i = 0; i < 6 + radius * 4; i++) {
                Vec3 p = center.add((r.nextDouble() - 0.5) * radius * 1.6, (r.nextDouble() - 0.5) * radius * 1.6, (r.nextDouble() - 0.5) * radius * 1.6);
                world.addParticle(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 1, 0, 0);
            }
            for (int i = 0; i < 20; i++) {
                world.addParticle(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z,
                        (r.nextDouble() - 0.5) * 0.3, r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.3);
            }
            ps.addElement(new SoundElement(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.9f + r.nextFloat() * 0.2f));
            ps.addElement(new ScreenTintElement(0x80FFFFFF, 0, 2));
        });
    }

    // ------------------------------------------------------------------ cables and pipes

    /**
     * Connects the cables, pipes and tubes (Mekanism transmitters) inside {@code area} to each other
     * and to the machines next to them, so they render like they do in game. Call it after placing
     * or removing blocks; calling it again is safe.
     */
    public void connectTransmitters(Selection area) {
        scene.addInstruction(ps -> {
            if (TransmitterLinker.link(ps.getWorld(), area) > 0)
                ps.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
        });
    }

    /** Sets which sides one transmitter connects to, when the automatic rule is not what the scene needs. */
    public void setTransmitterSides(BlockPos pos, Direction[] transmitterSides, Direction[] acceptorSides) {
        scene.addInstruction(ps -> {
            if (TransmitterLinker.set(ps.getWorld(), pos, transmitterSides, acceptorSides))
                ps.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
        });
    }

    /** Spawns particles in a straight line, e.g. for a dotted beam or a stream of matter. */
    public void particleLine(ParticleOptions particle, Vec3 from, Vec3 to, int count) {
        scene.addInstruction(ps -> {
            for (int i = 0; i <= count; i++) {
                Vec3 p = from.lerp(to, (double) i / Math.max(1, count));
                ps.getWorld().addParticle(particle, p.x, p.y, p.z, 0, 0, 0);
            }
        });
    }

    /** Plays a sound once (not repeated when the player skips through the scene). */
    public void sound(SoundEvent sound, float volume, float pitch) {
        scene.addInstruction(ps -> ps.addElement(new SoundElement(sound, volume, pitch)));
    }

    public void sound(Holder<SoundEvent> sound, float volume, float pitch) {
        sound(sound.value(), volume, pitch);
    }

    // ------------------------------------------------------------------ screen tints

    /** A quick full-view flash, color is ARGB (e.g. 0xA0FFFFFF). */
    public void flash(int color, int ticks) {
        scene.addInstruction(ps -> ps.addElement(new ScreenTintElement(color, 0, ticks)));
    }

    /**
     * Darkens (or tints) the view until hidden, e.g. to show night or how bright a lamp is.
     * Use 0x90000000 for night.
     */
    public ElementLink<ScreenTintElement> tint(int color, int fadeTicks) {
        return add(ScreenTintElement.class, () -> new ScreenTintElement(color, fadeTicks, -1));
    }

    // ------------------------------------------------------------------ gauges

    /**
     * A floating gauge next to a point. color is ARGB, unit is shown after the numbers (e.g. "mB", "FE").
     */
    public ElementLink<GaugeElement> gauge(Vec3 anchor, Component label, int color, float value, float max, String unit) {
        return add(GaugeElement.class, () -> new GaugeElement(anchor, label, color, value, max, unit, false));
    }

    public ElementLink<GaugeElement> horizontalGauge(Vec3 anchor, Component label, int color, float value, float max, String unit) {
        return add(GaugeElement.class, () -> new GaugeElement(anchor, label, color, value, max, unit, true));
    }

    public void setGauge(ElementLink<GaugeElement> link, float value, int ticks) {
        modify(link, g -> g.setValue(value, ticks));
    }

    public void moveGauge(ElementLink<GaugeElement> link, int offsetX, int offsetY) {
        modify(link, g -> g.setOffset(offsetX, offsetY));
    }

    // ------------------------------------------------------------------ fake GUI panels

    public ElementLink<PanelElement> panel(PanelBuilder builder) {
        return add(PanelElement.class, builder::build);
    }

    public void panelSlot(ElementLink<PanelElement> link, int slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        modify(link, p -> p.setSlot(slot, copy.copy()));
    }

    public void panelProgress(ElementLink<PanelElement> link, float progress, int ticks) {
        modify(link, p -> p.setProgress(progress, ticks));
    }

    public void panelBar(ElementLink<PanelElement> link, int bar, float value, int ticks) {
        modify(link, p -> p.setBar(bar, value, ticks));
    }

    /** Shows a cursor over a slot. click=true makes it look like it is being clicked. slot -1 removes it. */
    public void panelCursor(ElementLink<PanelElement> link, int slot, boolean click) {
        modify(link, p -> p.highlightSlot(slot, click));
    }

    // ------------------------------------------------------------------ players

    /** A player figure standing at the position (feet), facing yaw (0 = south, 90 = west, 180 = north, -90 = east). */
    public ElementLink<PlayerElement> player(Vec3 feet, float yaw) {
        return add(PlayerElement.class, () -> new PlayerElement(feet, yaw));
    }

    public void playerEquip(ElementLink<PlayerElement> link, EquipmentSlot slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        modify(link, p -> p.equip(slot, copy));
    }

    public void playerPose(ElementLink<PlayerElement> link, PlayerElement.PlayerPose pose) {
        modify(link, p -> p.setPose(pose));
    }

    public void playerMove(ElementLink<PlayerElement> link, Vec3 feet, int ticks) {
        modify(link, p -> p.moveTo(feet, ticks));
    }

    public void playerTurn(ElementLink<PlayerElement> link, float yaw, int ticks) {
        modify(link, p -> p.rotateTo(yaw, ticks));
    }

    public void playerLook(ElementLink<PlayerElement> link, float pitch) {
        modify(link, p -> p.lookPitch(pitch));
    }

    /** Swings the main hand once (like clicking with an item). */
    public void playerSwing(ElementLink<PlayerElement> link) {
        modify(link, PlayerElement::swing);
    }

    // ------------------------------------------------------------------ helpers

    /** Runs an action after a delay without blocking the rest of the scene. */
    static final class DelayedInstruction extends net.createmod.ponder.foundation.instruction.TickingInstruction {
        private final Consumer<PonderScene> action;

        DelayedInstruction(int ticks, Consumer<PonderScene> action) {
            super(false, Math.max(1, ticks));
            this.action = action;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            if (remainingTicks == 0) action.accept(scene);
        }
    }
}
