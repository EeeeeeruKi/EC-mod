package com.eeeeeeruki.skyriders.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 星型弾。キャプチャーした敵から作られて前へ飛ぶ。少しホーミングして、敵や地形に当たると消える。
 * 2体以上を吸い込んだ時は、敵に当たっても消えない貫通弾になる（地形では消える）。
 */
public class StarShotEntity extends Entity {
    private static final TrackedData<Boolean> PIERCE = DataTracker.registerData(StarShotEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final int LIFE = 50;
    private static final double HOMING_RANGE = 10.0;
    private static final double HOMING_TURN = 0.12;

    @Nullable
    private MachineEntity machine;
    @Nullable
    private PlayerEntity owner;
    private float damage = 10f;
    private final Set<UUID> hit = new HashSet<>();
    public float spin, prevSpin;

    public StarShotEntity(EntityType<? extends StarShotEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public void setup(MachineEntity machine, PlayerEntity owner, boolean pierce, float damage) {
        this.machine = machine;
        this.owner = owner;
        this.damage = damage;
        this.dataTracker.set(PIERCE, pierce);
    }

    public boolean isPierce() {
        return this.dataTracker.get(PIERCE);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(PIERCE, false);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevSpin = this.spin;
        this.spin += 30f;
        Vec3d pos = this.getPos();
        Vec3d vel = this.getVelocity();

        if (this.getWorld().isClient) {
            this.getWorld().addParticle(this.isPierce() ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD,
                    pos.x, pos.y + 0.2, pos.z, 0, 0, 0);
            this.setPosition(pos.add(vel));
            return;
        }
        ServerWorld world = (ServerWorld) this.getWorld();
        if (this.age > LIFE || this.owner == null || this.machine == null) {
            this.discard();
            return;
        }

        // ホーミング：前にいる一番近い敵へ少しずつ向きを変える
        LivingEntity target = null;
        double best = HOMING_RANGE * HOMING_RANGE;
        Vec3d dir = vel.normalize();
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(HOMING_RANGE), this::isTarget)) {
            Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(pos);
            double d = to.lengthSquared();
            if (d < best && to.normalize().dotProduct(dir) > 0.5) {
                best = d;
                target = e;
            }
        }
        if (target != null) {
            Vec3d want = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(pos).normalize().multiply(vel.length());
            vel = vel.add(want.subtract(vel).multiply(HOMING_TURN));
        }

        // 地形に当たったら消える
        Vec3d next = pos.add(vel);
        BlockHitResult blockHit = world.raycast(new RaycastContext(pos, next,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) next = blockHit.getPos();

        // 通り道の敵に当てる
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(pos, next).expand(0.6), this::isTarget)) {
            if (this.hit.contains(e.getUuid())) continue;
            Optional<Vec3d> h = e.getBoundingBox().expand(0.4).raycast(pos, next);
            if (h.isEmpty()) continue;
            this.hit.add(e.getUuid());
            this.machine.hitWithStar(e, this.owner, this.damage, vel);
            world.spawnParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
            if (!this.isPierce()) {
                this.discard();
                return;
            }
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            world.spawnParticles(ParticleTypes.POOF, next.x, next.y, next.z, 6, 0.2, 0.2, 0.2, 0.02);
            world.playSound(null, next.x, next.y, next.z, SoundEvents.ENTITY_GENERIC_SMALL_FALL, SoundCategory.PLAYERS, 0.8f, 1.6f);
            this.discard();
            return;
        }
        this.setVelocity(vel);
        this.setPosition(next);
    }

    private boolean isTarget(LivingEntity e) {
        if (!e.isAlive() || e == this.owner || e.isSpectator()) return false;
        if (e.getVehicle() instanceof MachineEntity m) return m != this.machine;
        return e instanceof Monster;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
