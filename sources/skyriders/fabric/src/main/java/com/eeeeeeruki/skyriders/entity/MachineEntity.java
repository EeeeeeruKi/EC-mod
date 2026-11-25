package com.eeeeeeruki.skyriders.entity;

import com.eeeeeeruki.skyriders.SkyRiders;
import com.eeeeeeruki.skyriders.net.MachineActionPayload;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;

/**
 * 仮マシン。
 * 動き（物理）は乗っているプレイヤーのクライアントで計算して、位置はバニラの乗り物同期でサーバーへ送る。
 * 攻撃・吸い込み・スペシャルなど「ほかの物に影響すること」はサーバーで処理する。
 */
public class MachineEntity extends Entity {

    // ===== 仮マシンの性能（あとでマシンごとに変えられるようにする） =====
    public static final float TOP_SPEED = 0.86f;     // 最高速度（ブロック/tick）≒ 62km/h
    private static final float ACCEL = 0.03f;        // 自動加速
    private static final float BRAKE_SPEED = 0.15f;  // チャージ中に落ちていく速度
    private static final float BRAKE_RATE = 0.045f;  // チャージ中の減速の強さ（小さいほど勢いが残る）
    private static final float CHARGE_TICKS = 30f;   // チャージ満タンまでの時間
    private static final float BOOST_POWER = 1.2f;   // 満タンで放した時に足される速度
    private static final float TURN = 4.0f;          // 通常の旋回（度/tick）
    private static final float DRIFT_TURN = 6.0f;    // チャージ中（ドリフト）の旋回
    // グリップ：横すべりをどれだけ消すか（1=レールの上、0=氷の上）
    private static final float GRIP = 0.22f;
    private static final float DRIFT_GRIP = 0.035f;  // チャージ中はほぼ滑る＝前の勢いが残る
    private static final float AIR_GRIP = 0.03f;
    private static final double GRAVITY = 0.08;      // 地面にいる時・空中でプッシュした時
    private static final double FAST_FALL_MAX = -2.0;
    private static final double GLIDE_FALL = -0.12;  // 普通の滑空で落ちる速さ（2.4ブロック/秒）
    private static final double DIVE_FALL = -0.8;    // W：機首を下げた時
    private static final double CLIMB_RISE = 0.14;   // S：機首を上げた時（速度を使って上がる）
    private static final double SNAP_DOWN = 1.05;    // これ以下の段差は地面に吸い付く

    private static final float SPECIAL_FILL_PER_TICK = 100f / (20f * 25f); // 25秒で満タン
    private static final int SPECIAL_READY_LIMIT = 200; // 満タンから10秒で消える
    private static final int SPECIAL_DURATION = 60;     // 3秒
    private static final int SPIN_DURATION = 12;

    // ===== 全員に同期する値 =====
    private static final TrackedData<Float> SPECIAL = DataTracker.registerData(MachineEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SPECIAL_ACTIVE = DataTracker.registerData(MachineEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SPIN = DataTracker.registerData(MachineEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> ABILITY = DataTracker.registerData(MachineEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> CHARGING = DataTracker.registerData(MachineEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /** クライアント側で操作パケットを送る処理（クライアントの初期化で差し込む） */
    public static IntConsumer clientActionSender = action -> {};

    // ===== 操作する人のクライアントで使う値 =====
    public boolean inForward, inBack, inLeft, inRight, inCharge;
    private boolean prevCharge, prevLeft, prevRight, wasCharging, wasControlled;
    private double velX, velY, velZ; // 慣性：実際に進んでいる方向と速さ（向きとは別）
    private float charge;
    private int airTicks;
    private int leftTapAge = -100, rightTapAge = -100;
    private int spinCooldown;
    private int localSpin;

    // 見た目用（全クライアント）
    public float roll, prevRoll;

    // ほかのクライアント用の補間
    private int lerpTicks;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;

    // サーバー用
    private int specialReadyTicks;
    private int serverSpinCooldown;

    public MachineEntity(EntityType<? extends MachineEntity> type, World world) {
        super(type, world);
        this.intersectionChecked = true;
        this.setNoGravity(true); // 重力は自前で計算する
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SPECIAL, 0f);
        builder.add(SPECIAL_ACTIVE, 0);
        builder.add(SPIN, 0);
        builder.add(ABILITY, "");
        builder.add(CHARGING, false);
    }

    // ===== 乗る・降りる =====

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.shouldCancelInteraction() || this.hasPassengers()) return ActionResult.PASS;
        if (!this.getWorld().isClient) {
            return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().isEmpty();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof PlayerEntity p ? p : null;
    }

    @Override
    public boolean isLogicalSideForUpdatingMovement() {
        LivingEntity c = this.getControllingPassenger();
        if (c instanceof PlayerEntity p) return p.isMainPlayer();
        return !this.getWorld().isClient;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.getWorld().isClient) {
            this.dataTracker.set(CHARGING, false);
        }
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
        if (passenger == this.getControllingPassenger()) {
            float yawDelta = MathHelper.wrapDegrees(this.getYaw() - this.prevYaw);
            passenger.setYaw(passenger.getYaw() + yawDelta);
            passenger.setHeadYaw(passenger.getHeadYaw() + yawDelta);
            lockPassengerYaw(passenger);
        }
    }

    @Override
    public void onPassengerLookAround(Entity passenger) {
        if (passenger == this.getControllingPassenger()) lockPassengerYaw(passenger);
    }

    /** カメラ（プレイヤーの向き）をマシンの後ろに固定する。上下は自由。 */
    private void lockPassengerYaw(Entity passenger) {
        passenger.setBodyYaw(this.getYaw());
        float f = MathHelper.wrapDegrees(passenger.getYaw() - this.getYaw());
        passenger.prevYaw -= f;
        passenger.setYaw(passenger.getYaw() - f);
        passenger.setHeadYaw(passenger.getYaw());
    }

    // ===== 毎tick =====

    @Override
    public void tick() {
        super.tick();

        this.prevRoll = this.roll;
        if (this.localSpin > 0) this.localSpin--;
        if (this.spinCooldown > 0) this.spinCooldown--;

        if (this.isLogicalSideForUpdatingMovement()) {
            this.lerpTicks = 0;
            if (this.getControllingPassenger() instanceof PlayerEntity) {
                tickControlled();
            } else {
                tickUnmanned();
            }
        } else {
            tickLerp();
            this.setVelocity(Vec3d.ZERO);
        }

        // 傾き（見た目）：曲がった量から計算するので、ほかの人から見ても傾く
        float yawDelta = MathHelper.wrapDegrees(this.getYaw() - this.prevYaw);
        float targetRoll = MathHelper.clamp(yawDelta * 4f, -28f, 28f);
        this.roll += (targetRoll - this.roll) * 0.3f;

        if (!this.getWorld().isClient) tickServer();
    }

    /** 乗っている人のクライアント：マシンの物理 */
    private void tickControlled() {
        if (!this.wasControlled) {
            this.velX = this.velY = this.velZ = 0;
            this.charge = 0f;
            this.wasControlled = true;
        }

        boolean onGround = this.isOnGround();
        this.airTicks = onGround ? 0 : this.airTicks + 1;
        boolean grounded = this.airTicks < 3;
        boolean special = this.getSpecialActiveTicks() > 0;
        boolean pressEdge = this.inCharge && !this.prevCharge;

        // --- クイックスピン：左右を素早く切り返す ---
        boolean leftEdge = this.inLeft && !this.prevLeft;
        boolean rightEdge = this.inRight && !this.prevRight;
        if (leftEdge) {
            if (this.age - this.rightTapAge <= 6) doSpin();
            this.leftTapAge = this.age;
        }
        if (rightEdge) {
            if (this.age - this.leftTapAge <= 6) doSpin();
            this.rightTapAge = this.age;
        }

        // --- ほおばっている時はボタンで「はき出し」 ---
        if (pressEdge && this.hasAbility()) {
            clientActionSender.accept(MachineActionPayload.USE);
        }

        // --- 向きを変える（進む方向はすぐには変わらない） ---
        boolean charging = this.inCharge && grounded && !special;
        float steer = (this.inRight ? 1f : 0f) - (this.inLeft ? 1f : 0f);
        float turn = charging ? DRIFT_TURN : (grounded ? TURN : TURN * 0.8f);
        this.setYaw(this.getYaw() + steer * turn);

        double rad = Math.toRadians(this.getYaw());
        double fx = -Math.sin(rad), fz = Math.cos(rad); // 前
        double rx = -fz, rz = fx;                       // 横
        double fwd = this.velX * fx + this.velZ * fz;   // 今の勢いのうち「前」の分
        double side = this.velX * rx + this.velZ * rz;  // 今の勢いのうち「横」の分（すべり）

        // --- チャージとブースト ---
        if (charging && !this.wasCharging) {
            clientActionSender.accept(MachineActionPayload.CHARGE_START);
        }
        if (!charging && this.wasCharging) {
            clientActionSender.accept(MachineActionPayload.CHARGE_STOP);
            if (this.charge >= 0.15f) {
                fwd = Math.max(fwd, 0) + BOOST_POWER * this.charge;
                this.getWorld().playSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0f, 0.8f + this.charge * 0.6f, false);
            }
            this.charge = 0f;
        }
        this.wasCharging = charging;

        // --- 前への速さ（勝手に進む） ---
        if (special) {
            fwd = Math.max(fwd, TOP_SPEED * 1.8);
        } else if (charging) {
            fwd += (BRAKE_SPEED - fwd) * BRAKE_RATE;
            this.charge = Math.min(1f, this.charge + 1f / CHARGE_TICKS);
        } else if (fwd < TOP_SPEED) {
            fwd = Math.min(TOP_SPEED, fwd + ACCEL * (grounded ? 1.0 : 0.4));
        } else {
            fwd = Math.max(TOP_SPEED, fwd - (0.008 + (fwd - TOP_SPEED) * 0.03)); // ブースト分は少しずつ戻る
        }

        // --- 横すべり：グリップが低いほど前の勢いのまま流れる ---
        float grip = grounded ? (charging ? DRIFT_GRIP : GRIP) : AIR_GRIP;
        side *= (1.0 - grip);

        // --- 縦の動き ---
        double vy = this.velY;
        if (grounded && onGround) {
            vy = Math.max(vy - GRAVITY, FAST_FALL_MAX);
        } else if (this.inCharge && !this.hasAbility()) {
            vy = Math.max(vy - GRAVITY, FAST_FALL_MAX);       // 空中でプッシュ：急降下
        } else if (this.inForward) {
            vy += (DIVE_FALL - vy) * 0.12;                   // W：機首を下げる → 落ちるけど速くなる
            fwd = Math.min(TOP_SPEED * 1.3, fwd + 0.02);
        } else if (this.inBack && fwd > TOP_SPEED * 0.5) {
            vy += (CLIMB_RISE - vy) * 0.10;                  // S：機首を上げる → 速さを使って上がる
            fwd = Math.max(TOP_SPEED * 0.45, fwd - 0.016);
        } else {
            vy += (GLIDE_FALL - vy) * 0.08;                  // 何もしない：ふわっと滑空
        }
        if (this.isTouchingWater()) {
            vy = Math.min(vy + 0.12, 0.2); // 水の上はうかぶ
            fwd *= 0.97;
        }

        this.velX = fx * fwd + rx * side;
        this.velZ = fz * fwd + rz * side;
        this.velY = vy;
        boolean wasOnGround = onGround;
        this.setVelocity(this.velX, this.velY, this.velZ);
        this.move(MovementType.SELF, this.getVelocity());

        // 小さい下り段差では浮かずに地面に吸い付く（崖からは飛び出す）
        if (wasOnGround && !this.isOnGround() && this.velY <= 0 && !this.isTouchingWater()
                && !this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(0, -SNAP_DOWN, 0))) {
            this.move(MovementType.SELF, new Vec3d(0, -SNAP_DOWN, 0));
        }

        // 壁や地面に当たった方向の勢いは消える（moveが0にしてくれる）
        Vec3d after = this.getVelocity();
        this.velX = after.x;
        this.velY = this.isOnGround() ? 0 : after.y;
        this.velZ = after.z;

        // --- エフェクト ---
        double speedNow = getSpeed();
        if (speedNow > TOP_SPEED * 1.05 || special) {
            this.getWorld().addParticle(special ? ParticleTypes.FIREWORK : ParticleTypes.CLOUD,
                    this.getX() - fx * 0.9, this.getY() + 0.25, this.getZ() - fz * 0.9, 0, 0.02, 0);
        }
        if (charging && Math.abs(side) > 0.15 && this.age % 2 == 0) {
            this.getWorld().addParticle(ParticleTypes.CRIT, this.getX() - fx * 0.7, this.getY() + 0.1, this.getZ() - fz * 0.7, 0, 0.05, 0);
        }
        if (charging && this.charge >= 1f && this.age % 2 == 0) {
            this.getWorld().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + (this.random.nextDouble() - 0.5) * 1.2, this.getY() + 0.3,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 1.2, 0, 0.1, 0);
        }

        this.prevCharge = this.inCharge;
        this.prevLeft = this.inLeft;
        this.prevRight = this.inRight;
    }

    private void doSpin() {
        if (this.spinCooldown > 0) return;
        this.spinCooldown = 18;
        this.localSpin = SPIN_DURATION;
        this.velX *= 0.92;
        this.velZ *= 0.92;
        this.leftTapAge = -100;
        this.rightTapAge = -100;
        clientActionSender.accept(MachineActionPayload.SPIN);
    }

    /** 誰も乗っていない時：止まって地面に落ちる */
    private void tickUnmanned() {
        this.wasControlled = false;
        this.wasCharging = false;
        this.velX = this.velY = this.velZ = 0;
        this.charge = 0f;
        this.inForward = this.inBack = this.inLeft = this.inRight = this.inCharge = false;
        Vec3d v = this.getVelocity();
        double vy = Math.max(v.y - GRAVITY, -2.0);
        if (this.isTouchingWater()) vy = Math.min(vy + 0.12, 0.1);
        this.setVelocity(v.x * 0.8, vy, v.z * 0.8);
        this.move(MovementType.SELF, this.getVelocity());
    }

    private void tickLerp() {
        this.wasControlled = false;
        if (this.lerpTicks > 0) {
            double f = 1.0 / this.lerpTicks;
            this.setPosition(
                    this.getX() + (this.lerpX - this.getX()) * f,
                    this.getY() + (this.lerpY - this.getY()) * f,
                    this.getZ() + (this.lerpZ - this.getZ()) * f);
            this.setYaw(this.getYaw() + MathHelper.wrapDegrees(this.lerpYaw - this.getYaw()) * (float) f);
            this.lerpTicks--;
        }
    }

    @Override
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yaw;
        this.lerpTicks = 3;
    }

    @Override
    public double getLerpTargetX() {
        return this.lerpTicks > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double getLerpTargetY() {
        return this.lerpTicks > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double getLerpTargetZ() {
        return this.lerpTicks > 0 ? this.lerpZ : this.getZ();
    }

    // ===== サーバー側 =====

    private void tickServer() {
        ServerWorld world = (ServerWorld) this.getWorld();
        PlayerEntity rider = this.getControllingPassenger() instanceof PlayerEntity p ? p : null;

        if (this.serverSpinCooldown > 0) this.serverSpinCooldown--;
        int spin = this.dataTracker.get(SPIN);
        if (spin > 0) this.dataTracker.set(SPIN, spin - 1);

        // スペシャル発動中：まわりにダメージ
        int active = this.dataTracker.get(SPECIAL_ACTIVE);
        if (active > 0) {
            this.dataTracker.set(SPECIAL_ACTIVE, active - 1);
            if (rider != null && active % 4 == 0) {
                for (LivingEntity e : nearbyTargets(rider, 3.5)) {
                    e.damage(this.getDamageSources().playerAttack(rider), 6f);
                    knockback(e, 1.2);
                }
                world.spawnParticles(ParticleTypes.FIREWORK, this.getX(), this.getY() + 0.5, this.getZ(), 12, 1.2, 0.4, 1.2, 0.05);
            }
        }

        if (rider == null) {
            this.specialReadyTicks = 0;
            return;
        }

        // スペシャルゲージ：時間でたまる、満タンから10秒で消える
        float gauge = this.dataTracker.get(SPECIAL);
        if (active <= 0) {
            if (gauge < 100f) {
                gauge = Math.min(100f, gauge + SPECIAL_FILL_PER_TICK);
                this.dataTracker.set(SPECIAL, gauge);
                if (gauge >= 100f) {
                    this.specialReadyTicks = 0;
                    rider.sendMessage(Text.translatable("message.skyriders.special_ready"), true);
                    world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 1f, 1.5f);
                }
            } else if (++this.specialReadyTicks > SPECIAL_READY_LIMIT) {
                this.dataTracker.set(SPECIAL, 0f);
                rider.sendMessage(Text.translatable("message.skyriders.special_lost"), true);
            }
        }

        // チャージ中は吸い込み（ほおばっていない時だけ）
        if (this.dataTracker.get(CHARGING) && !this.hasAbility()) {
            tickInhale(world, rider);
        }
    }

    private void tickInhale(ServerWorld world, PlayerEntity rider) {
        Vec3d forward = Vec3d.fromPolar(0f, this.getYaw());
        Vec3d mouth = this.getPos().add(forward.multiply(0.8)).add(0, 0.4, 0);
        List<LivingEntity> list = this.getWorld().getEntitiesByClass(LivingEntity.class,
                this.getBoundingBox().expand(5.0), e -> e instanceof Monster && e.isAlive() && e != rider && !e.hasVehicle());
        for (LivingEntity e : list) {
            Vec3d toMouth = mouth.subtract(e.getPos().add(0, e.getHeight() * 0.5, 0));
            double dist = toMouth.length();
            if (dist > 5.0) continue;
            Vec3d fromMachine = e.getPos().subtract(this.getPos()).normalize();
            if (fromMachine.dotProduct(forward) < 0.35) continue; // 前にいる敵だけ

            if (dist < 1.8 && e.getMaxHealth() <= 40f) {
                Identifier id = Registries.ENTITY_TYPE.getId(e.getType());
                world.spawnParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                e.discard();
                this.dataTracker.set(ABILITY, id.toString());
                world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1f, 1.2f);
                return;
            }
            e.setVelocity(e.getVelocity().multiply(0.5).add(toMouth.normalize().multiply(0.35)));
            e.velocityModified = true;
            if (this.age % 3 == 0) {
                world.spawnParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    /** クライアントから届いた操作 */
    public void handleAction(ServerPlayerEntity player, int action) {
        if (player != this.getControllingPassenger()) return;
        ServerWorld world = (ServerWorld) this.getWorld();
        switch (action) {
            case MachineActionPayload.SPIN -> {
                if (this.serverSpinCooldown > 0) return;
                this.serverSpinCooldown = 15;
                this.dataTracker.set(SPIN, SPIN_DURATION);
                world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1.3f);
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 0.5, this.getZ(), 3, 0.8, 0.1, 0.8, 0.0);
                for (LivingEntity e : nearbyTargets(player, 2.8)) {
                    if (e.damage(this.getDamageSources().playerAttack(player), 4f)) addSpecial(5f);
                    knockback(e, 0.9);
                }
            }
            case MachineActionPayload.CHARGE_START -> this.dataTracker.set(CHARGING, true);
            case MachineActionPayload.CHARGE_STOP -> this.dataTracker.set(CHARGING, false);
            case MachineActionPayload.USE -> spitStar(world, player);
            case MachineActionPayload.SPECIAL -> {
                if (this.dataTracker.get(SPECIAL) >= 100f && this.dataTracker.get(SPECIAL_ACTIVE) <= 0) {
                    this.dataTracker.set(SPECIAL, 0f);
                    this.dataTracker.set(SPECIAL_ACTIVE, SPECIAL_DURATION);
                    player.sendMessage(Text.translatable("message.skyriders.special_start"), true);
                    world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.8f, 1.4f);
                }
            }
            case MachineActionPayload.EXIT_OR_SWAP -> exitOrSwap(player);
            default -> {
            }
        }
    }

    /** ほおばった敵を星にしてはき出す（前にまっすぐ飛ぶ） */
    private void spitStar(ServerWorld world, ServerPlayerEntity player) {
        if (!this.hasAbility()) return;
        this.dataTracker.set(ABILITY, "");

        Vec3d forward = Vec3d.fromPolar(0f, this.getYaw());
        Vec3d start = this.getPos().add(0, 0.6, 0).add(forward.multiply(1.2));
        Vec3d end = start.add(forward.multiply(24));
        BlockHitResult blockHit = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getPos();

        LivingEntity target = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(start, end).expand(1.0),
                e -> e.isAlive() && e != player)) {
            Optional<Vec3d> hit = e.getBoundingBox().expand(0.4).raycast(start, end);
            if (hit.isPresent()) {
                double d = start.squaredDistanceTo(hit.get());
                if (d < best) {
                    best = d;
                    target = e;
                }
            }
        }
        if (target != null) {
            end = target.getPos().add(0, target.getHeight() * 0.5, 0);
            if (target.damage(this.getDamageSources().playerAttack(player), 10f)) addSpecial(10f);
            knockback(target, 1.0);
        }

        double len = start.distanceTo(end);
        for (double d = 0; d < len; d += 0.7) {
            Vec3d p = start.add(forward.multiply(d));
            world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1f, 0.7f);
    }

    /** E：近くに空いているマシンがあれば乗り換え、無ければ降りる */
    private void exitOrSwap(ServerPlayerEntity player) {
        MachineEntity other = null;
        double best = Double.MAX_VALUE;
        for (MachineEntity m : this.getWorld().getEntitiesByClass(MachineEntity.class, this.getBoundingBox().expand(4.0),
                m -> m != this && !m.hasPassengers() && m.isAlive())) {
            double d = m.squaredDistanceTo(this);
            if (d < best) {
                best = d;
                other = m;
            }
        }
        player.stopRiding();
        if (other != null && player.startRiding(other)) {
            player.sendMessage(Text.translatable("message.skyriders.swap"), true);
        }
    }

    private List<LivingEntity> nearbyTargets(PlayerEntity rider, double radius) {
        return this.getWorld().getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(radius),
                e -> e.isAlive() && e != rider && !(e.getVehicle() == this) && e.squaredDistanceTo(this) <= radius * radius);
    }

    private void knockback(LivingEntity e, double strength) {
        e.takeKnockback(strength, this.getX() - e.getX(), this.getZ() - e.getZ());
    }

    private void addSpecial(float amount) {
        if (this.dataTracker.get(SPECIAL_ACTIVE) > 0) return;
        float g = this.dataTracker.get(SPECIAL);
        if (g < 100f) this.dataTracker.set(SPECIAL, Math.min(99.9f, g + amount));
    }

    // ===== こわす・当たり判定 =====

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient || this.isRemoved()) return true;
        if (this.isInvulnerableTo(source) || this.hasPassengers()) return false;
        if (source.getAttacker() instanceof PlayerEntity player) {
            if (!player.getAbilities().creativeMode) {
                this.dropStack(new ItemStack(SkyRiders.MACHINE_ITEM));
            }
            this.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public float getStepHeight() {
        return 1.0f; // 1ブロックの段差はそのまま登る
    }

    // ===== 保存 =====

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.dataTracker.set(SPECIAL, nbt.getFloat("Special"));
        this.dataTracker.set(ABILITY, nbt.getString("Ability"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("Special", this.dataTracker.get(SPECIAL));
        nbt.putString("Ability", this.dataTracker.get(ABILITY));
    }

    // ===== HUD・描画用 =====

    public float getSpeed() {
        return (float) Math.sqrt(this.velX * this.velX + this.velZ * this.velZ);
    }

    public float getCharge() {
        return this.charge;
    }

    public float getSpecialGauge() {
        return this.dataTracker.get(SPECIAL);
    }

    public int getSpecialActiveTicks() {
        return this.dataTracker.get(SPECIAL_ACTIVE);
    }

    public int getSpinTicks() {
        return Math.max(this.localSpin, this.dataTracker.get(SPIN));
    }

    public static int spinDuration() {
        return SPIN_DURATION;
    }

    public String getAbility() {
        return this.dataTracker.get(ABILITY);
    }

    public boolean hasAbility() {
        return !this.dataTracker.get(ABILITY).isEmpty();
    }
}
