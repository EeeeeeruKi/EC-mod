package com.eeeeeeruki.skyriders.entity;

import com.eeeeeeruki.skyriders.SkyRiders;
import com.eeeeeeruki.skyriders.item.PatchItem;
import com.eeeeeeruki.skyriders.net.MachineActionPayload;
import com.eeeeeeruki.skyriders.net.MachineHitPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
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
import net.minecraft.util.Formatting;
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
 * マシン。
 * 動き（物理）は乗っているプレイヤーのクライアントで計算して、位置はバニラの乗り物同期でサーバーへ送る。
 * 攻撃・吸い込み・パッチ・HPなど「ほかの物に影響すること」はサーバーで処理する。
 *
 * 速度は「加算」ではなく「最高速への収束」で決まる。加速＝収束力で、
 * ブーストで最高速を超えた分が戻る速さにも同じ値が効く。
 */
public class MachineEntity extends Entity {

    // ===== マシンに関係なく共通の値 =====
    private static final double BRAKE_SPEED = 0.15;   // チャージ中に向かう速さ（止まりきらない）
    private static final double BRAKE_RATE = 0.045;   // チャージ中の減速（重量で変わる）
    private static final double MIN_STEP = 0.004;     // 収束の最後のひと押し
    private static final float BOOST_POWER = 1.2f;    // 満タンで放した時に足される速度
    private static final float DRIFT_TURN_MUL = 1.5f; // チャージ中（ドリフト）の旋回
    private static final float DRIFT_GRIP_MUL = 0.16f;// チャージ中はほぼ滑る＝前の勢いが残る
    private static final float AIR_GRIP = 0.6f;       // 空中は慣性ほぼなし：向いた方へすぐ進む
    private static final float AIR_TURN_MUL = 0.9f;
    private static final double AIR_ACCEL_MUL = 1.6;  // 空中の加速（地上の何倍か）
    private static final float AIR_CHARGE_MUL = 0.25f;// 空中ではチャージがじわじわたまる
    private static final double GRAVITY = 0.08;       // 地面・空中でプッシュした時（重量で変わる）
    private static final double FAST_FALL_MAX = -2.0;
    private static final float MAX_PITCH = 30f;       // 機首の上げ下げの最大角度
    private static final double EXCHANGE = 0.05;      // 機首の角度で速度⇔高度を交換する強さ
    private static final double CLIMB_K = 0.9;        // 機首を上げた時、速さのどれだけを上昇に使うか
    private static final double STALL_SPEED = 0.28;   // これより遅くなると失速
    private static final double SNAP_DOWN = 1.05;     // これ以下の段差は地面に吸い付く
    private static final int LANDING_MIN_AIR = 8;     // これ以上浮いてから着地したら判定する
    private static final float NICE_ANGLE = 12f;      // 機首の角度がこれ以内ならナイス着地
    private static final double RAM_MIN = 0.55;       // 体当たりになる速さ
    /** マイクラのダメージ → マシンのHP（100）へのダメージ */
    public static final float MACHINE_DAMAGE_SCALE = 4f;

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
    private static final TrackedData<Float> HP = DataTracker.registerData(MachineEntity.class, TrackedDataHandlerRegistry.FLOAT);
    @SuppressWarnings("unchecked")
    private static final TrackedData<Integer>[] LEVELS = new TrackedData[PatchKind.ALL.length];

    static {
        for (int i = 0; i < LEVELS.length; i++) {
            LEVELS[i] = DataTracker.registerData(MachineEntity.class, TrackedDataHandlerRegistry.INTEGER);
        }
    }

    /** クライアント側で操作パケットを送る処理（クライアントの初期化で差し込む） */
    public static IntConsumer clientActionSender = action -> {};

    private final MachineSpec spec = MachineSpec.PROTOTYPE;

    // ===== 操作する人のクライアントで使う値 =====
    public boolean inForward, inBack, inLeft, inRight, inCharge;
    private boolean prevCharge, prevLeft, prevRight, wasCharging, wasControlled;
    private double velX, velY, velZ; // 慣性：実際に進んでいる方向と速さ（向きとは別）
    private float charge;
    private int airTicks;
    private int stunTicks;
    private float flightPitch;       // 機首の角度（+で下がる）
    private boolean stalled;
    private int leftTapAge = -100, rightTapAge = -100;
    private int spinCooldown;
    private int localSpin;
    public int landingResult, landingTimer; // 1=ナイス着地 2=着地失敗

    // 見た目用（全クライアント）
    public float roll, prevRoll;
    public float visPitch, prevVisPitch;

    // ほかのクライアント用の補間
    private int lerpTicks;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;

    // サーバー用
    private int specialReadyTicks;
    private int serverSpinCooldown;
    private int hurtCooldown;
    private int ramCooldown;
    @Nullable
    private Vec3d lastServerPos;

    public MachineEntity(EntityType<? extends MachineEntity> type, World world) {
        super(type, world);
        this.intersectionChecked = true;
        this.setNoGravity(true); // 重力は自前で計算する
        this.dataTracker.set(HP, this.maxHp());
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SPECIAL, 0f);
        builder.add(SPECIAL_ACTIVE, 0);
        builder.add(SPIN, 0);
        builder.add(ABILITY, "");
        builder.add(CHARGING, false);
        builder.add(HP, 100f);
        for (TrackedData<Integer> l : LEVELS) builder.add(l, 0);
    }

    // =====================================================================
    // ステータス（基礎値 × パッチの段階）
    // =====================================================================

    public MachineSpec spec() {
        return this.spec;
    }

    /** パッチの表示段階 */
    public int level(PatchKind k) {
        return this.dataTracker.get(LEVELS[k.ordinal()]);
    }

    /** パッチの内部値を -1〜1 にしたもの */
    private float t(PatchKind k) {
        return MathHelper.clamp(k.internal(level(k)) / 16f, -1f, 1f);
    }

    /** 重量（マシンの基礎＋パッチ） */
    private float tw() {
        return MathHelper.clamp(t(PatchKind.WEIGHT) + this.spec.weight(), -1f, 1f);
    }

    /** t=1 で max 倍、t=-1 で min 倍になる割合の計算 */
    private static double curve(float t, double max, double min) {
        return t >= 0 ? 1 + (max - 1) * t : 1 + (min - 1) * -t;
    }

    /** 最高速：基礎値への割合。重量の倍率（最大+50%）と掛け算 */
    public double topSpeed() {
        return this.spec.topSpeed() * curve(t(PatchKind.TOP_SPEED), this.spec.topMaxMul(), 0.3) * curve(tw(), 1.5, 0.85);
    }

    /** 加速＝収束力 */
    public double accelK() {
        return this.spec.accel() * curve(t(PatchKind.ACCEL), 2.5, 0.4);
    }

    public float chargeTicks() {
        return (float) (this.spec.chargeTicks() / curve(t(PatchKind.CHARGE), 2.5, 0.4));
    }

    /** 旋回：機首のヨーとピッチの応答速度 */
    public float turnMul() {
        return (float) curve(t(PatchKind.TURN), 1.8, 0.5);
    }

    /** 飛行速：空中の最高速（地上とは別） */
    public double flyTop() {
        return this.spec.flySpeed() * curve(t(PatchKind.FLY), 2.5, 0.4);
    }

    /** 落下・急降下の速さ（重量） */
    public double fallMul() {
        return curve(tw(), 1.6, 0.6);
    }

    /** 揚力：普通に滑空している時に落ちる速さ。スター型だけ。重量で下がる */
    public double glideSink() {
        if (!this.spec.star()) return -0.6 * fallMul();
        float tl = MathHelper.clamp(t(PatchKind.FLY) - 0.4f * tw(), -1f, 1f);
        double m = tl >= 0 ? 1 - 0.8 * tl : 1 + 1.5 * -tl;
        return this.spec.lift() * m * fallMul();
    }

    /** チャージ中に止まる速さ（重いほど早く止まる） */
    public double brakeRate() {
        return BRAKE_RATE * curve(tw(), 2.2, 0.5);
    }

    /** 与ダメージの倍率：攻撃パッチ × 固有倍率 × 重量 */
    public float attackMul() {
        return (float) (curve(t(PatchKind.ATTACK), 4.5, 0.2) * this.spec.attack() * curve(tw(), 1.25, 0.9));
    }

    /** 被ダメージの軽減倍率：防御パッチ × 重量 */
    public float defenseMul() {
        return (float) (curve(t(PatchKind.DEFENSE), this.spec.defenseMax(), 0.5) * curve(tw(), 1.3, 0.8));
    }

    public float maxHp() {
        return this.spec.baseHp() * Math.max(0.3f, 1f + 0.1f * level(PatchKind.HP));
    }

    /** 体当たりの威力と吹っ飛ばし量（重量） */
    public double ramPower() {
        return curve(tw(), 1.8, 0.6);
    }

    // =====================================================================
    // 乗る・降りる
    // =====================================================================

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

    // =====================================================================
    // 毎tick
    // =====================================================================

    @Override
    public void tick() {
        super.tick();

        this.prevRoll = this.roll;
        this.prevVisPitch = this.visPitch;
        if (this.landingTimer > 0) this.landingTimer--;
        if (this.localSpin > 0) this.localSpin--;
        if (this.spinCooldown > 0) this.spinCooldown--;

        boolean mine = false;
        if (this.isLogicalSideForUpdatingMovement()) {
            this.lerpTicks = 0;
            if (this.getControllingPassenger() instanceof PlayerEntity) {
                tickControlled();
                mine = true;
            } else {
                tickUnmanned();
            }
        } else {
            tickLerp();
            this.setVelocity(Vec3d.ZERO);
        }

        // 左右の傾き（見た目）：曲がった量から計算するので、ほかの人から見ても傾く
        float yawDelta = MathHelper.wrapDegrees(this.getYaw() - this.prevYaw);
        float targetRoll = MathHelper.clamp(yawDelta * 4f, -28f, 28f);
        this.roll += (targetRoll - this.roll) * 0.3f;

        // 前後の傾き（見た目）：乗っている人は機首の角度、ほかの人からは上下の動きで計算する
        float pitchGoal = mine ? this.flightPitch
                : MathHelper.clamp((float) -(this.getY() - this.prevY) * 30f, -MAX_PITCH, MAX_PITCH);
        this.visPitch += (pitchGoal - this.visPitch) * (mine ? 1f : 0.25f);

        if (!this.getWorld().isClient) tickServer();
    }

    /** 乗っている人のクライアント：マシンの物理 */
    private void tickControlled() {
        if (!this.wasControlled) {
            this.velX = this.velY = this.velZ = 0;
            this.charge = 0f;
            this.flightPitch = 0f;
            this.stalled = false;
            this.wasControlled = true;
        }

        boolean onGround = this.isOnGround();
        this.airTicks = onGround ? 0 : this.airTicks + 1;
        boolean grounded = this.airTicks < 3;
        boolean special = this.getSpecialActiveTicks() > 0;
        boolean pressEdge = this.inCharge && !this.prevCharge;
        boolean stunned = this.stunTicks > 0;
        if (stunned) this.stunTicks--;

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

        // --- 機首の向き（ヨー）：旋回ステータス ---
        float turnMul = this.turnMul();
        boolean charging = this.inCharge && grounded && !special && !stunned; // 地上でプッシュ＝ブレーキ＋チャージ
        float steer = stunned ? 0f : (this.inRight ? 1f : 0f) - (this.inLeft ? 1f : 0f);
        float turn = this.spec.turn() * turnMul * (charging ? DRIFT_TURN_MUL : grounded ? 1f : AIR_TURN_MUL);
        this.setYaw(this.getYaw() + steer * turn);

        double rad = Math.toRadians(this.getYaw());
        double fx = -Math.sin(rad), fz = Math.cos(rad); // 前
        double rx = -fz, rz = fx;                       // 横
        double fwd = this.velX * fx + this.velZ * fz;   // 今の勢いのうち「前」の分
        double side = this.velX * rx + this.velZ * rz;  // 今の勢いのうち「横」の分（すべり）

        // --- 機首の上げ下げ（ピッチ）：旋回ステータスで応答が速くなる ---
        float pitchTarget = 0f;
        if (!stunned) {
            if (this.inForward) pitchTarget = MAX_PITCH;
            else if (this.inBack) pitchTarget = -MAX_PITCH;
        }
        if (grounded) pitchTarget *= 0.35f;
        else if (this.stalled) pitchTarget = 22f; // 失速すると機首が下がる
        if (stunned) pitchTarget += MathHelper.sin(this.stunTicks * 1.3f) * 12f;
        this.flightPitch += (pitchTarget - this.flightPitch) * 0.12f * turnMul;

        // --- チャージとブースト ---
        if (charging && !this.wasCharging) {
            clientActionSender.accept(MachineActionPayload.CHARGE_START);
        }
        if (!charging && this.wasCharging) {
            clientActionSender.accept(MachineActionPayload.CHARGE_STOP);
            // 地上でボタンを離した時だけブースト（押したまま飛び出したらチャージは持ち越し）
            if (grounded && !this.inCharge) fwd = boost(fwd, 1f);
        }
        this.wasCharging = charging;

        double vy = this.velY;
        if (grounded) {
            // ===== 地上 =====
            if (special) {
                fwd = Math.max(fwd, this.topSpeed() * 1.8);
            } else if (charging) {
                fwd += (BRAKE_SPEED - fwd) * this.brakeRate();
                this.charge = Math.min(1f, this.charge + 1f / this.chargeTicks());
            } else if (stunned) {
                fwd *= 0.9;
            } else {
                fwd = converge(fwd, this.topSpeed(), this.accelK());
            }
            float grip = this.spec.grip() * (charging ? DRIFT_GRIP_MUL : 1f);
            side *= (1.0 - grip);
            vy = Math.max(vy - GRAVITY * this.fallMul(), FAST_FALL_MAX * this.fallMul());
            this.stalled = false;
        } else {
            // ===== 空中：速度と高度を交換する =====
            double theta = Math.toRadians(this.flightPitch); // +で機首が下がる
            double sin = Math.sin(theta);
            double fallMul = this.fallMul();

            if (!special) this.charge = Math.min(1f, this.charge + AIR_CHARGE_MUL / this.chargeTicks());

            // 失速の判定
            if (!this.stalled && fwd < STALL_SPEED && this.flightPitch < 5f) this.stalled = true;
            if (this.stalled && fwd > STALL_SPEED * 1.5) this.stalled = false;

            if (special) {
                fwd = Math.max(fwd, this.flyTop() * 1.8);
            } else {
                // 機首を下げると速くなり、上げると遅くなる（重いほど落ちる力が強い）
                fwd += EXCHANGE * sin * (sin > 0 ? fallMul : 1.0);
                // 機首を上げていない時だけ、飛行速へ収束する
                if (this.flightPitch > -5f) fwd = converge(fwd, this.flyTop(), this.accelK() * AIR_ACCEL_MUL);
                fwd = Math.max(0, Math.min(fwd, this.flyTop() * 1.8));
            }

            if (this.inCharge && !this.hasAbility()) {
                vy = Math.max(vy - GRAVITY * fallMul, FAST_FALL_MAX * fallMul); // 空中でプッシュ：急降下
            } else if (this.stalled || !this.spec.star()) {
                vy = Math.max(vy - GRAVITY * 0.75 * fallMul, FAST_FALL_MAX * fallMul);
            } else {
                // 揚力で決まる落ち方＋機首の角度ぶんの上昇／下降。重いと上がりにくい
                double climb = -fwd * sin * CLIMB_K;
                if (climb > 0) climb /= fallMul;
                double target = this.glideSink() + climb;
                vy += (target - vy) * 0.15;
                vy = Math.max(vy, -1.6 * fallMul);
            }
            side *= (1.0 - AIR_GRIP);
        }
        if (this.isTouchingWater()) {
            vy = Math.min(vy + 0.12, 0.2); // 水の上はうかぶ
            fwd *= 0.97;
        }

        this.velX = fx * fwd + rx * side;
        this.velZ = fz * fwd + rz * side;
        this.velY = vy;
        int airBefore = this.airTicks;
        this.setVelocity(this.velX, this.velY, this.velZ);
        this.move(MovementType.SELF, this.getVelocity());

        // 小さい下り段差では浮かずに地面に吸い付く（崖からは飛び出す）
        if (onGround && !this.isOnGround() && this.velY <= 0 && !this.isTouchingWater()
                && !this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(0, -SNAP_DOWN, 0))) {
            this.move(MovementType.SELF, new Vec3d(0, -SNAP_DOWN, 0));
        }

        // 壁や地面に当たった方向の勢いは消える（moveが0にしてくれる）
        Vec3d after = this.getVelocity();
        this.velX = after.x;
        this.velY = this.isOnGround() ? 0 : after.y;
        this.velZ = after.z;

        // --- 着地の判定（角度） ---
        if (!onGround && this.isOnGround() && airBefore >= LANDING_MIN_AIR && !special) {
            land(fx, fz);
        }

        // --- エフェクト ---
        double speedNow = getSpeed();
        if (speedNow > this.topSpeed() * 1.05 || special) {
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

    /** 今の速さを目標へ近づける（遅い時は加速、ブーストで超えた時は減速、どちらも同じ収束力） */
    private static double converge(double value, double target, double k) {
        double d = target - value;
        double step = d * k;
        if (Math.abs(step) < MIN_STEP) step = Math.copySign(Math.min(MIN_STEP, Math.abs(d)), d);
        return value + step;
    }

    /** ためたチャージをブーストに変える（最高速を一時的に超える） */
    private double boost(double fwd, float power) {
        if (this.charge >= 0.15f) {
            fwd = Math.max(fwd, 0) + BOOST_POWER * this.charge * power;
            this.getWorld().playSound(this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0f, 0.8f + this.charge * 0.6f, false);
        }
        this.charge = 0f;
        return fwd;
    }

    /** 着地：機首の角度が地面とそろっていればナイス着地、ずれていれば失敗 */
    private void land(double fx, double fz) {
        boolean nice = Math.abs(this.flightPitch) <= NICE_ANGLE;
        if (!nice) {
            this.landingResult = 2;
            this.stunTicks = 12;
            this.charge = 0f;
            this.velX *= 0.35;
            this.velZ *= 0.35;
            this.getWorld().playSound(this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0f, 0.7f, false);
            for (int i = 0; i < 10; i++) {
                this.getWorld().addParticle(ParticleTypes.POOF,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.4, this.getY() + 0.2,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.4, 0, 0.05, 0);
            }
        } else {
            this.landingResult = 1;
            // 運動量はほぼそのまま＋小さな加速。ボタンを離していれば、空中でたまったチャージでブースト
            double fwd = this.velX * fx + this.velZ * fz;
            double nf = (this.inCharge ? fwd : boost(fwd, 0.9f)) + 0.08;
            this.velX += fx * (nf - fwd);
            this.velZ += fz * (nf - fwd);
            this.getWorld().playSound(this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.4f, false);
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI * 2 / 12;
                this.getWorld().addParticle(ParticleTypes.CRIT, this.getX(), this.getY() + 0.15, this.getZ(),
                        Math.cos(a) * 0.4, 0.1, Math.sin(a) * 0.4);
            }
        }
        this.landingTimer = 30;
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

    /** サーバーから届いた吹っ飛び（乗っている人のクライアント） */
    public void applyHitClient(float vx, float vy, float vz, int stun) {
        if (!this.isLogicalSideForUpdatingMovement()) return;
        this.velX += vx;
        this.velY += vy;
        this.velZ += vz;
        this.stunTicks = Math.max(this.stunTicks, stun);
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

    // =====================================================================
    // サーバー側
    // =====================================================================

    private void tickServer() {
        ServerWorld world = (ServerWorld) this.getWorld();
        PlayerEntity rider = this.getControllingPassenger() instanceof PlayerEntity p ? p : null;

        if (this.serverSpinCooldown > 0) this.serverSpinCooldown--;
        if (this.hurtCooldown > 0) this.hurtCooldown--;
        if (this.ramCooldown > 0) this.ramCooldown--;
        int spin = this.dataTracker.get(SPIN);
        if (spin > 0) this.dataTracker.set(SPIN, spin - 1);

        // サーバーでの速さ（位置の変化から計算。動きは乗っている人の画面で決まるため）
        Vec3d pos = this.getPos();
        Vec3d vel = this.lastServerPos == null ? Vec3d.ZERO : pos.subtract(this.lastServerPos);
        this.lastServerPos = pos;
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (hSpeed > 8) hSpeed = 0; // テレポートなど

        // スペシャル発動中：まわりにダメージ
        int active = this.dataTracker.get(SPECIAL_ACTIVE);
        if (active > 0) {
            this.dataTracker.set(SPECIAL_ACTIVE, active - 1);
            if (rider != null && active % 4 == 0) {
                for (LivingEntity e : nearbyTargets(rider, 3.5)) {
                    hitTarget(e, rider, 6f * this.attackMul(), 1.2);
                }
                world.spawnParticles(ParticleTypes.FIREWORK, this.getX(), this.getY() + 0.5, this.getZ(), 12, 1.2, 0.4, 1.2, 0.05);
            }
        }

        if (rider == null) {
            this.specialReadyTicks = 0;
            return;
        }

        // 体当たり：速さ×重量で威力と吹っ飛ばしが上がる
        if (this.ramCooldown <= 0 && hSpeed >= RAM_MIN) {
            Vec3d dir = new Vec3d(vel.x, 0, vel.z);
            for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(0.35),
                    e -> e.isAlive() && e != rider && e.getVehicle() != this && !e.isSpectator())) {
                Vec3d to = e.getPos().subtract(pos);
                if (to.x * dir.x + to.z * dir.z <= 0) continue; // 進んでいる方にいる相手だけ
                float dmg = (float) ((hSpeed - RAM_MIN + 0.1) * 6.0 * this.attackMul() * this.ramPower());
                if (hitTarget(e, rider, dmg, hSpeed * 1.2 * this.ramPower())) {
                    this.ramCooldown = 10;
                    world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1f, 0.9f);
                }
            }
        }

        // パッチを拾う
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, this.getBoundingBox().expand(1.2, 0.8, 1.2),
                i -> i.isAlive() && i.getItemAge() > 10 && i.getStack().getItem() instanceof PatchItem)) {
            ItemStack stack = item.getStack();
            for (int n = 0; n < stack.getCount(); n++) applyPatch(stack, rider);
            item.discard();
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
                    hitTarget(e, player, 4f * this.attackMul(), 0.9);
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
            hitTarget(target, player, 10f * this.attackMul(), 1.0);
        }

        double len = start.distanceTo(end);
        for (double d = 0; d < len; d += 0.7) {
            Vec3d p = start.add(forward.multiply(d));
            world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1f, 0.7f);
    }

    /** E：近くに空いているマシンがあれば乗り換え（パッチも持っていく）、無ければ降りる */
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
            for (PatchKind k : PatchKind.ALL) {
                other.dataTracker.set(LEVELS[k.ordinal()], this.level(k));
                this.dataTracker.set(LEVELS[k.ordinal()], 0);
            }
            other.dataTracker.set(HP, Math.min(other.dataTracker.get(HP), other.maxHp()));
            this.dataTracker.set(HP, Math.min(this.dataTracker.get(HP), this.maxHp()));
            player.sendMessage(Text.translatable("message.skyriders.swap"), true);
        }
    }

    private List<LivingEntity> nearbyTargets(PlayerEntity rider, double radius) {
        return this.getWorld().getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(radius),
                e -> e.isAlive() && e != rider && e.getVehicle() != this && e.squaredDistanceTo(this) <= radius * radius);
    }

    /**
     * 攻撃を当てる。相手がマシンに乗っていれば、マシンにダメージ（防御・重量で軽減）。
     * dmg はマイクラのダメージ（攻撃倍率をかけたあと）
     */
    private boolean hitTarget(LivingEntity e, PlayerEntity rider, float dmg, double knock) {
        Vec3d d = e.getPos().subtract(this.getPos());
        Vec3d dir = new Vec3d(d.x, 0, d.z);
        if (e.getVehicle() instanceof MachineEntity other) {
            if (other == this) return false;
            boolean hit = other.takeHit(dmg * MACHINE_DAMAGE_SCALE, rider, dir, knock * 0.6);
            if (hit) addSpecial(5f);
            return hit;
        }
        boolean hurt = e.damage(this.getDamageSources().playerAttack(rider), dmg);
        if (dir.lengthSquared() > 1e-4) e.takeKnockback(knock, -dir.x, -dir.z);
        if (hurt) {
            addSpecial(5f);
            if (!e.isAlive() && e instanceof Monster) dropPatchFrom(e);
        }
        return hurt;
    }

    /**
     * マシンがダメージを受ける。防御は割合で軽減（上限90%）、よろけと吹っ飛びも減る。
     */
    public boolean takeHit(float raw, @Nullable Entity attacker, Vec3d dir, double knock) {
        if (this.getWorld().isClient || this.isRemoved() || this.hurtCooldown > 0) return false;
        this.hurtCooldown = 8;
        float def = this.defenseMul();
        float taken = Math.max(raw / def, raw * 0.1f);
        addSpecial(taken * 0.15f); // 受けてもスペシャルがたまる

        ServerWorld world = (ServerWorld) this.getWorld();
        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, this.getX(), this.getY() + 0.6, this.getZ(),
                Math.max(1, (int) (taken / 5)), 0.4, 0.2, 0.4, 0.1);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.9f, 0.8f);

        if (this.getControllingPassenger() instanceof ServerPlayerEntity sp && knock > 0) {
            Vec3d n = dir.lengthSquared() > 1e-4 ? dir.normalize() : Vec3d.ZERO;
            double k = knock / def;
            ServerPlayNetworking.send(sp, new MachineHitPayload(this.getId(),
                    (float) (n.x * k), (float) (0.15 * k), (float) (n.z * k), (int) Math.ceil(10 / def)));
        }
        damageHp(taken);
        return true;
    }

    private void damageHp(float amount) {
        float hp = this.dataTracker.get(HP) - amount;
        this.dataTracker.set(HP, Math.max(0f, hp));
        if (hp <= 0f) destroy();
    }

    /** HPが0：こわれて、ためたパッチをまわりにばらまく */
    private void destroy() {
        ServerWorld world = (ServerWorld) this.getWorld();
        world.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 4, 0.6, 0.3, 0.6, 0.0);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1.2f, 0.7f);

        int dropped = 0;
        for (PatchKind k : PatchKind.ALL) {
            int lv = this.level(k);
            int n = lv > 0 ? (lv + 1) / 2 : 0;
            for (int i = 0; i < n && dropped < 24; i++, dropped++) {
                spawnPatch(world, new ItemStack(SkyRiders.PATCHES.get(k)), this.getX(), this.getY() + 0.6, this.getZ(), 0.45);
            }
        }
        if (this.getControllingPassenger() instanceof PlayerEntity p) {
            p.sendMessage(Text.translatable("message.skyriders.destroyed"), true);
        }
        this.removeAllPassengers();
        this.discard();
    }

    private void spawnPatch(ServerWorld world, ItemStack stack, double x, double y, double z, double spread) {
        ItemEntity item = new ItemEntity(world, x, y, z, stack,
                (this.random.nextDouble() - 0.5) * spread, 0.3 + this.random.nextDouble() * 0.2, (this.random.nextDouble() - 0.5) * spread);
        item.setPickupDelay(30);
        world.spawnEntity(item);
    }

    /** マシンの攻撃で倒した敵は、パッチを落とすことがある（たまに偽パッチ） */
    private void dropPatchFrom(LivingEntity e) {
        ServerWorld world = (ServerWorld) this.getWorld();
        float r = this.random.nextFloat();
        Item item = SkyRiders.PATCHES.get(PatchKind.ALL[this.random.nextInt(PatchKind.ALL.length)]);
        ItemStack stack;
        if (r < 0.04f) stack = new ItemStack(SkyRiders.PATCH_ALL);
        else if (r < 0.10f) stack = PatchItem.makeFake(item);
        else if (r < 0.45f) stack = new ItemStack(item);
        else return;
        spawnPatch(world, stack, e.getX(), e.getY() + 0.5, e.getZ(), 0.2);
    }

    /** パッチを使う（拾った時・右クリックした時） */
    public void applyPatch(ItemStack stack, PlayerEntity rider) {
        if (!(stack.getItem() instanceof PatchItem patch) || this.getWorld().isClient) return;
        ServerWorld world = (ServerWorld) this.getWorld();
        PatchKind kind = patch.kind();
        if (PatchItem.isFake(stack)) {
            PatchKind k = kind != null ? kind : PatchKind.ALL[this.random.nextInt(PatchKind.ALL.length)];
            addLevel(k, -1);
            damageHp(15f);
            rider.sendMessage(Text.translatable("message.skyriders.patch_fake", k.displayName()).formatted(Formatting.RED), true);
            world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1f, 1f);
            return;
        }
        if (kind == null) {
            for (PatchKind k : PatchKind.ALL) addLevel(k, 1);
            rider.sendMessage(Text.translatable("message.skyriders.patch_all"), true);
        } else {
            addLevel(kind, 1);
            int lv = this.level(kind);
            rider.sendMessage(Text.translatable("message.skyriders.patch_get", kind.displayName(), lv > 0 ? "+" + lv : String.valueOf(lv)), true);
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.3f);
    }

    private void addLevel(PatchKind k, int delta) {
        int before = this.level(k);
        int after = MathHelper.clamp(before + delta, PatchKind.MIN, k.max);
        this.dataTracker.set(LEVELS[k.ordinal()], after);
        float hp = this.dataTracker.get(HP);
        if (k == PatchKind.HP && after > before) hp += this.spec.baseHp() * 0.1f * (after - before); // 増えた分だけ回復
        this.dataTracker.set(HP, Math.min(hp, this.maxHp()));
    }

    private void addSpecial(float amount) {
        if (this.dataTracker.get(SPECIAL_ACTIVE) > 0) return;
        float g = this.dataTracker.get(SPECIAL);
        if (g < 100f) this.dataTracker.set(SPECIAL, Math.min(99.9f, g + amount));
    }

    // =====================================================================
    // こわす・当たり判定
    // =====================================================================

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

    // =====================================================================
    // 保存
    // =====================================================================

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.dataTracker.set(SPECIAL, nbt.getFloat("Special"));
        this.dataTracker.set(ABILITY, nbt.getString("Ability"));
        NbtCompound patches = nbt.getCompound("Patches");
        for (PatchKind k : PatchKind.ALL) {
            this.dataTracker.set(LEVELS[k.ordinal()], MathHelper.clamp(patches.getInt(k.id), PatchKind.MIN, k.max));
        }
        this.dataTracker.set(HP, nbt.contains("Hp") ? Math.min(nbt.getFloat("Hp"), this.maxHp()) : this.maxHp());
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("Special", this.dataTracker.get(SPECIAL));
        nbt.putString("Ability", this.dataTracker.get(ABILITY));
        NbtCompound patches = new NbtCompound();
        for (PatchKind k : PatchKind.ALL) patches.putInt(k.id, this.level(k));
        nbt.put("Patches", patches);
        nbt.putFloat("Hp", this.dataTracker.get(HP));
    }

    // =====================================================================
    // HUD・描画用
    // =====================================================================

    public float getSpeed() {
        return (float) Math.sqrt(this.velX * this.velX + this.velZ * this.velZ);
    }

    public int getLandingResult() {
        return this.landingTimer > 0 ? this.landingResult : 0;
    }

    public boolean isStalled() {
        return this.stalled && this.airTicks > 0;
    }

    public float getCharge() {
        return this.charge;
    }

    public float getHp() {
        return this.dataTracker.get(HP);
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
