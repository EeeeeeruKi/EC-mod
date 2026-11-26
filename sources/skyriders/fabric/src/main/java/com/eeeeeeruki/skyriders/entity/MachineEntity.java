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
import net.minecraft.particle.DustParticleEffect;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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
    private static final double BRAKE_RATE = 0.045;   // チャージ中の減速（重量で変わる）
    private static final double MIN_STEP = 0.004;     // 収束の最後のひと押し
    private static final double BOOST_POWER = 1.4;    // 満タンで放した時に足される速度（最高速の何倍か）
    private static final double DRIFT_BRAKE_MUL = 0.55;// ドリフト中はブレーキがゆるい
    private static final float DRIFT_CHARGE_MUL = 1.5f;// ドリフト中はチャージが早くたまる
    private static final double STOP_SPEED = 0.06;     // これより遅いと「停止状態」
    private static final float STOP_TURN_MUL = 2.2f;   // 停止状態の旋回
    private static final int CHARGE_LOCK_TICKS = 40;   // チャージのオーバーヒートのあと、チャージできない時間
    private static final float MIN_DASH = 0.08f;       // これ以上たまっていればダッシュする（ポンピング用に小さめ）
    private static final double CAPTURE_RANGE = 4.0;   // キャプチャー（吸い込み）の範囲
    private static final int CAPTURE_FIRE_DELAY = 10;  // 捕らえてから星型弾を撃つまで（この間は無敵）
    private static final float DRIFT_GRIP_MUL = 0.16f;// チャージ中はほぼ滑る＝前の勢いが残る
    private static final float AIR_GRIP = 0.6f;       // 空中は慣性ほぼなし：向いた方へすぐ進む
    private static final float AIR_TURN_MUL = 0.9f;
    private static final double AIR_ACCEL_MUL = 1.6;  // 空中の加速（地上の何倍か）
    private static final double GRAVITY = 0.08;       // 地面・空中でプッシュした時（重量で変わる）
    private static final double FAST_FALL_MAX = -2.0;
    private static final float MAX_PITCH = 30f;       // 機首の上げ下げの最大角度
    private static final double EXCHANGE = 0.05;      // 機首の角度で速度⇔高度を交換する強さ
    private static final double CLIMB_K = 0.9;        // 機首を上げた時、速さのどれだけを上昇に使うか
    private static final double STALL_SPEED = 0.28;   // これより遅くなると失速
    private static final double SNAP_DOWN = 1.05;     // これ以下の段差は地面に吸い付く
    private static final int LANDING_JUDGE_AIR = 10;  // これ以上飛んでから着地したら判定する
    private static final float NICE_ANGLE = 10f;      // 機首の角度がこれ以内ならナイス着地
    private static final float NORMAL_ANGLE = 20f;    // これ以内なら通常着地、超えたら着地失敗
    private static final double NICE_BASE = 0.05;     // ナイス着地の微加速
    private static final double NICE_FALL = 0.3;      // 下降の速さに応じた上乗せ
    private static final double TAKEOFF_RISE = 0.12;  // 離陸した時の上昇（共通）
    private static final double TAKEOFF_LIFT_RISE = 0.36; // 離陸した時の上昇（揚力ぶん）
    private static final double TAKEOFF_ACCEL = 0.06; // 離陸した時の加速
    private static final double NEUTRAL_SINK = -0.12; // 無操作の滑空：揚力0なら落ちる速さ
    private static final double NEUTRAL_LIFT = 0.15;  // 揚力1あたり、無操作で浮く強さ
    private static final double OVERHEAT_SINK = -0.12;// 点滅中の落ち方
    private static final double RAM_MIN = 0.65;       // 体当たりになる速さ（最高速の何倍か）
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
    private static final TrackedData<Boolean> CHARGING = DataTracker.registerData(MachineEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> BLINK = DataTracker.registerData(MachineEntity.class, TrackedDataHandlerRegistry.INTEGER); // 0なし 1限界が近い 2限界
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
    private boolean climbWarned;     // 上昇の限界が近い（点滅しはじめ）
    private int chargeHold;          // 満タンのまま押している時間
    private int chargeLockTicks;     // チャージのオーバーヒート後、チャージできない時間
    public int chargeOverheatTimer;  // HUD用
    private boolean overheated;      // 上昇の限界で点滅中（着地するまで上昇できない）
    private double climbUsed;        // 離陸してから上がった高さ
    private int flightTicks;
    private int leftTapAge = -100, rightTapAge = -100;
    private int spinCooldown;
    private int localSpin;
    public int landingResult, landingTimer; // 1=ナイス着地 2=着地失敗 3=通常着地

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
    private double serverSpeed;
    private int captured;            // 捕らえた敵の数
    private int captureTimer;        // 星型弾を撃つまで
    private int invulnTicks;         // キャプチャーのモーション中は無敵
    // 見た目用（全クライアント）
    private int visAirTicks;
    public float hover = 0.18f, prevHover = 0.18f;
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
        builder.add(CHARGING, false);
        builder.add(BLINK, 0);
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

    /**
     * 揚力（0〜1.5くらい）：機首を上げた時の上昇力・離陸した時の上昇・飛行時間をまとめた値。
     * スター型だけで、重いほど下がる。
     */
    public double lift() {
        if (!this.spec.star()) return 0;
        return this.spec.lift() / 100.0 * curve(t(PatchKind.FLY), 1.6, 0.4) * curve(tw(), 0.7, 1.15);
    }

    /** 飛行時間（tick）。これを過ぎると揚力がだんだん抜けていく */
    public int flightTime() {
        return (int) (40 + 100 * this.lift());
    }

    /** 今効いている揚力（飛行時間を過ぎると40tickかけて0になる） */
    private double liftNow() {
        double over = this.flightTicks - this.flightTime();
        double fade = over <= 0 ? 1 : Math.max(0, 1 - over / 40.0);
        return this.lift() * fade;
    }

    /** 上昇の限界（上がった高さの合計、ブロック） */
    public double climbLimit() {
        // 飛行パッチで大きく伸びる（+18で約100ブロック、0で約8ブロック）
        return 2 + 8 * this.lift() * curve(t(PatchKind.FLY), 9.4, 0.5);
    }

    /** 上昇する時の減速の倍率：揚力が高いほど、上がっても速さが落ちにくい */
    private double climbCost() {
        return 1.0 / (1.0 + 1.6 * Math.max(0, this.lift() - 0.75));
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
            this.noClip = false;
            this.dataTracker.set(CHARGING, false);
            this.dataTracker.set(BLINK, 0);
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
        else tickVisuals();
    }

    /** 見た目の変化（すべてのクライアント） */
    private void tickVisuals() {
        World w = this.getWorld();
        this.visAirTicks = this.isOnGround() ? 0 : this.visAirTicks + 1;
        boolean pushing = this.isPushing();

        // プッシュすると地面側へ沈む（スター型は普段は浮いている）
        this.prevHover = this.hover;
        float hoverGoal = !this.spec.star() ? 0f : pushing && this.isOnGround() ? 0.02f : 0.18f;
        this.hover += (hoverGoal - this.hover) * 0.35f;

        double rad = Math.toRadians(this.getYaw());
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double rx = -fz, rz = fx;

        // ブレーキランプ：光が尾を引く
        if (pushing && this.hasPassengers()) {
            w.addParticle(new DustParticleEffect(new Vector3f(1f, 0.1f, 0.05f), 0.9f),
                    this.getX() - fx * 0.85, this.getY() + this.hover + 0.18, this.getZ() - fz * 0.85, 0, 0, 0);
        }

        // HPが減ると煙が出る
        float hpRate = this.getHp() / this.maxHp();
        if (hpRate < 0.5f && this.age % (hpRate < 0.25f ? 2 : 4) == 0) {
            w.addParticle(hpRate < 0.25f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.8, this.getY() + 0.4,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.8, 0, 0.04, 0);
            if (hpRate < 0.25f && this.random.nextInt(6) == 0) {
                w.addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.3, this.getZ(), 0, 0.03, 0);
            }
        }

        // ヴェイパー：滑空中に後ろへ飛行機雲
        if (this.spec.vapor() && this.isGlidingVisual() && this.age % 2 == 0) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                w.addParticle(ParticleTypes.WHITE_ASH,
                        this.getX() - fx * 0.7 + rx * 0.7 * sgn, this.getY() + this.hover + 0.15,
                        this.getZ() - fz * 0.7 + rz * 0.7 * sgn, 0, 0, 0);
            }
        }

        // パッチの発光：いちばん多い色の粒が出る（多いほど多い）
        PatchKind dom = this.dominantPatch();
        if (dom != null) {
            int lv = this.level(dom);
            int every = lv >= 12 ? 2 : lv >= 6 ? 5 : 12;
            if (this.age % every == 0) {
                int c = MachineEntity.glowColor(dom);
                w.addParticle(new DustParticleEffect(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.0f),
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.4, this.getY() + 0.3,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.4, 0, 0.02, 0);
            }
        }

        // スペシャル発動可能：虹色に光る
        if (this.isSpecialReady() && this.age % 2 == 0) {
            int c = MathHelper.hsvToRgb((this.age % 40) / 40f, 0.8f, 1f);
            w.addParticle(new DustParticleEffect(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.2f),
                    this.getX() + (this.random.nextDouble() - 0.5) * 1.6, this.getY() + 0.5 + this.random.nextDouble() * 0.6,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 1.6, 0, 0.03, 0);
        }
    }

    /** 発光の色。HPは攻撃（赤系）と見分けるため白 */
    public static int glowColor(PatchKind k) {
        return k == PatchKind.HP ? 0xFFFFFF : k.color;
    }

    /** 乗っている人のクライアント：マシンの物理 */
    private void tickControlled() {
        if (!this.wasControlled) {
            this.velX = this.velY = this.velZ = 0;
            this.charge = 0f;
            this.flightPitch = 0f;
            this.stalled = false;
            this.overheated = false;
            this.climbUsed = 0;
            this.flightTicks = 0;
            this.wasControlled = true;
        }

        boolean onGround = this.isOnGround();
        this.airTicks = onGround ? 0 : this.airTicks + 1;
        boolean grounded = this.airTicks < 3;
        boolean special = this.getSpecialActiveTicks() > 0;
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

        // --- プッシュ中かどうか（キャプチャー・ブレーキランプ用に、地上でも空中でもサーバーへ知らせる） ---
        if (this.inCharge && !this.prevCharge) clientActionSender.accept(MachineActionPayload.CHARGE_START);
        if (!this.inCharge && this.prevCharge) clientActionSender.accept(MachineActionPayload.CHARGE_STOP);

        boolean chargeLocked = this.chargeLockTicks > 0;
        if (chargeLocked) this.chargeLockTicks--;

        double rad0 = Math.toRadians(this.getYaw());
        double fwd0 = this.velX * -Math.sin(rad0) + this.velZ * Math.cos(rad0);

        // --- 機首の向き（ヨー）：旋回ステータス ---
        float turnMul = this.turnMul();
        boolean braking = this.inCharge && grounded && !special && !stunned; // 地上でプッシュ＝押し付けてブレーキ＋チャージ
        float steer = stunned ? 0f : (this.inRight ? 1f : 0f) - (this.inLeft ? 1f : 0f);
        boolean stopped = braking && Math.abs(fwd0) < STOP_SPEED;
        float turn;
        if (stopped) {
            // 停止状態：ドリフトより曲がりやすい（スター型は重量、接地タイプは旋回とグリップで決まる）
            turn = this.spec.turn() * STOP_TURN_MUL * (this.spec.star()
                    ? (float) curve(tw(), 0.6, 1.4) : turnMul * (0.5f + this.spec.grip() * 2f));
        } else {
            turn = this.spec.turn() * turnMul * (braking ? this.spec.driftTurnMul() : grounded ? 1f : AIR_TURN_MUL);
        }
        this.setYaw(this.getYaw() + steer * turn);

        double rad = Math.toRadians(this.getYaw());
        double fx = -Math.sin(rad), fz = Math.cos(rad); // 前
        double rx = -fz, rz = fx;                       // 横
        double fwd = this.velX * fx + this.velZ * fz;   // 今の勢いのうち「前」の分
        double side = this.velX * rx + this.velZ * rz;  // 今の勢いのうち「横」の分（すべり）

        // --- 機首の上げ下げ（ピッチ）：旋回ステータスで応答が速くなる。空中プッシュでは変わらない ---
        float pitchTarget = 0f;
        if (!stunned) {
            if (this.inForward) pitchTarget = MAX_PITCH;
            else if (this.inBack) pitchTarget = -MAX_PITCH;
        }
        if (grounded) pitchTarget *= 0.35f;
        else if (this.stalled) pitchTarget = 22f; // 失速すると機首が下がる
        if (!grounded && this.inCharge) pitchTarget = this.flightPitch; // 急降下は機首の向きを変えない
        if (stunned) pitchTarget += MathHelper.sin(this.stunTicks * 1.3f) * 12f;
        this.flightPitch += (pitchTarget - this.flightPitch) * 0.12f * turnMul;

        // --- チャージダッシュ：地上でプッシュを離した時だけ。被弾の硬直中は止まって、離した入力は硬直のあとに効く ---
        if (!stunned) {
            if (!braking && this.wasCharging && grounded && !this.inCharge) {
                fwd = boost(fwd, 1f);
                side *= 0.5; // ダッシュで向いた方へそろう（ポンピング）
            }
            this.wasCharging = braking;
        }

        // --- チャージのオーバーヒート：満タンのまま押し続けると、ひるんでチャージが空になる ---
        if (braking && this.charge >= 1f) {
            if (++this.chargeHold > this.spec.chargeHoldTicks()) {
                this.chargeHold = 0;
                this.charge = 0f;
                this.chargeLockTicks = CHARGE_LOCK_TICKS;
                this.stunTicks = 10;
                this.chargeOverheatTimer = 40;
                this.getWorld().playSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.9f, 1.2f, false);
                for (int i = 0; i < 8; i++) {
                    this.getWorld().addParticle(ParticleTypes.LARGE_SMOKE,
                            this.getX() + (this.random.nextDouble() - 0.5), this.getY() + 0.4,
                            this.getZ() + (this.random.nextDouble() - 0.5), 0, 0.05, 0);
                }
            }
        } else {
            this.chargeHold = 0;
        }
        if (this.chargeOverheatTimer > 0) this.chargeOverheatTimer--;

        double vy = this.velY;
        if (grounded) {
            // ===== 地上 =====
            boolean drifting = braking && steer != 0f;
            if (special) {
                fwd = Math.max(fwd, this.topSpeed() * 1.8);
            } else if (braking) {
                if (stopped && (this.inForward || this.inBack)) {
                    // 停止状態でW/S：ゆっくり前後に動く
                    fwd = converge(fwd, this.inForward ? this.spec.slowForward() : -this.spec.slowBack(), 0.3);
                } else {
                    // ブレーキ：最後は止まる。ドリフト中はゆるめ、Wで弱く、Sで強く
                    double brake = this.brakeRate() * (drifting ? DRIFT_BRAKE_MUL : 1.0);
                    if (this.inForward) brake *= 0.5;
                    else if (this.inBack) brake *= 1.8;
                    fwd = converge(fwd, 0, brake);
                }
                if (!chargeLocked) {
                    this.charge = Math.min(1f, this.charge + (drifting ? DRIFT_CHARGE_MUL : 1f) / this.chargeTicks());
                }
            } else if (stunned) {
                fwd *= 0.9;
            } else {
                fwd = converge(fwd, this.topSpeed(), this.accelK());
            }
            // スター型：滑るドリフト（前の勢いが残る）、接地タイプ：曲がるドリフト（向きと一緒に変わる）
            float grip = this.spec.grip() * (braking ? (this.spec.star() ? DRIFT_GRIP_MUL : 4f) : 1f);
            side *= (1.0 - Math.min(1f, grip));
            // 離陸した直後の数tickは、離陸の上昇を消さないよう重力を弱くする
            vy = onGround ? Math.max(vy - GRAVITY * this.fallMul(), FAST_FALL_MAX * this.fallMul()) : vy - 0.02;
            this.stalled = false;
        } else {
            // ===== 空中：速度と高度を交換する =====
            double theta = Math.toRadians(this.flightPitch); // +で機首が下がる
            double sin = Math.sin(theta);
            double fallMul = this.fallMul();
            double lift = this.liftNow();
            this.flightTicks++;

            // エアチャージ：滑空中は勝手にたまる（地上のチャージとは別の隠し値）
            if (!special && this.spec.airChargeTicks() > 0) {
                this.charge = Math.min(1f, this.charge + 1f / this.spec.airChargeTicks());
            }

            // 失速の判定
            if (!this.stalled && fwd < STALL_SPEED && this.flightPitch < 5f) this.stalled = true;
            if (this.stalled && fwd > STALL_SPEED * 1.5) this.stalled = false;

            if (special) {
                fwd = Math.max(fwd, this.flyTop() * 1.8);
            } else {
                // 上昇は減速、下降は加速（重いほど下降の加速が強い）
                fwd += EXCHANGE * sin * (sin > 0 ? fallMul : this.climbCost());
                // 機首を上げていない時だけ、飛行速へ収束する（空中プッシュ中はスラスターを切るので進まない）
                if (this.inCharge) fwd *= 0.995;
                else if (this.flightPitch > -5f) fwd = converge(fwd, this.flyTop(), this.accelK() * AIR_ACCEL_MUL);
                fwd = Math.max(0, Math.min(fwd, this.flyTop() * 1.8));
            }

            if (this.inCharge) {
                vy = Math.max(vy - GRAVITY * fallMul, FAST_FALL_MAX * fallMul); // 空中でプッシュ：急降下
            } else if (this.stalled || !this.spec.star()) {
                vy = Math.max(vy - GRAVITY * 0.75 * fallMul, FAST_FALL_MAX * fallMul);
            } else {
                // 何もしなくても揚力に従って浮く。機首を上げると揚力ぶん強く上がる
                double neutral = NEUTRAL_SINK + NEUTRAL_LIFT * lift;
                double climb = -fwd * sin * CLIMB_K * (0.15 + 0.85 * Math.min(lift, 1.2));
                if (climb > 0) climb /= fallMul;
                double target = neutral + climb;
                if (target < 0) target *= fallMul; // 重いと速く落ちる
                // 上昇の限界（点滅中）：着地するまで上がれない。機首上げはゆっくり落ちるだけ
                if (this.overheated) target = Math.min(target, OVERHEAT_SINK * fallMul * (this.inBack ? 0.5 : 1.0));
                vy += (target - vy) * 0.12;
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
        // 崖際で踏みとどまる：プッシュ中は、先に地面が無いなら進まない
        if (braking && onGround && this.getWorld().isSpaceEmpty(this,
                this.getBoundingBox().offset(this.velX, -SNAP_DOWN - 0.1, this.velZ))) {
            this.velX *= 0.1;
            this.velZ *= 0.1;
        }
        int airBefore = this.airTicks;
        double fallSpeed = vy;
        this.setVelocity(this.velX, this.velY, this.velZ);
        this.move(MovementType.SELF, this.getVelocity());

        // 小さい下り段差では浮かずに地面に吸い付く（速い時は、ちょっとの凹凸でも離陸する）
        if (onGround && !this.isOnGround() && this.velY <= 0 && !this.isTouchingWater()
                && this.getSpeed() < this.topSpeed() * 1.15
                && !this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(0, -SNAP_DOWN, 0))) {
            this.move(MovementType.SELF, new Vec3d(0, -SNAP_DOWN, 0));
        }

        // 壁や地面に当たった方向の勢いは消える（moveが0にしてくれる）
        Vec3d after = this.getVelocity();
        this.velX = after.x;
        this.velY = this.isOnGround() ? 0 : after.y;
        this.velZ = after.z;

        // --- 離陸：加速と上向きの勢いが自動で入る ---
        if (onGround && !this.isOnGround() && !this.isTouchingWater()) {
            takeoff(fx, fz);
        }
        // 上昇した量を数えて、限界を超えたら点滅（着地するまで上昇できない）
        if (!this.isOnGround()) {
            if (this.velY > 0) this.climbUsed += this.velY;
            if (!this.climbWarned && this.climbUsed >= this.climbLimit() * 0.75) {
                this.climbWarned = true; // 限界が近い：点滅しはじめる
                clientActionSender.accept(MachineActionPayload.BLINK_WARN);
            }
            if (!this.overheated && this.climbUsed >= this.climbLimit()) {
                this.overheated = true;
                clientActionSender.accept(MachineActionPayload.BLINK_ON);
                this.getWorld().playSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 0.8f, 0.6f, false);
            }
        }

        // --- 着地 ---
        if (!onGround && this.isOnGround()) {
            if (airBefore >= LANDING_JUDGE_AIR && !special) {
                land(fx, fz, fallSpeed);
            } else if (airBefore >= 3 && !this.inCharge) {
                double f = this.velX * fx + this.velZ * fz; // 短い滑空：たまった分だけ解放
                double nf = boost(f, 0.8f);
                this.velX += fx * (nf - f);
                this.velZ += fz * (nf - f);
            }
            this.climbUsed = 0;
            this.flightTicks = 0;
            if (this.overheated || this.climbWarned) {
                this.overheated = false;
                this.climbWarned = false;
                clientActionSender.accept(MachineActionPayload.BLINK_OFF);
            }
        }

        // --- エフェクト ---
        double speedNow = getSpeed();
        if (speedNow > this.topSpeed() * 1.05 || special) {
            this.getWorld().addParticle(special ? ParticleTypes.FIREWORK : ParticleTypes.CLOUD,
                    this.getX() - fx * 0.9, this.getY() + 0.25, this.getZ() - fz * 0.9, 0, 0.02, 0);
        }
        if (braking && Math.abs(side) > 0.15 && this.age % 2 == 0) {
            this.getWorld().addParticle(ParticleTypes.CRIT, this.getX() - fx * 0.7, this.getY() + 0.1, this.getZ() - fz * 0.7, 0, 0.05, 0);
        }
        if (braking && this.charge >= 1f && this.age % 2 == 0) {
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
        if (this.charge >= MIN_DASH) {
            fwd = Math.max(fwd, 0) + this.topSpeed() * BOOST_POWER * this.charge * power;
            clientActionSender.accept(MachineActionPayload.DASH_BASE + Math.round(this.charge * 100));
            this.getWorld().playSound(this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0f, 0.8f + this.charge * 0.6f, false);
        }
        this.charge = 0f;
        return fwd;
    }

    /** 離陸：マシンごとの加速と上昇。揚力が上昇の強さ、重いほど低く速く前へ飛ぶ */
    private void takeoff(double fx, double fz) {
        double arc = curve(tw(), 1.6, 0.7);
        double rise = this.spec.star()
                ? (TAKEOFF_RISE + TAKEOFF_LIFT_RISE * this.lift()) / arc
                : (0.08 + 0.3 * this.flyTop() / this.spec.flySpeed()) / arc; // 揚力が無いタイプは飛行速でジャンプの高さが決まる
        this.velY = Math.max(this.velY, 0) + rise;
        this.velX += fx * TAKEOFF_ACCEL * arc;
        this.velZ += fz * TAKEOFF_ACCEL * arc;
        this.climbUsed = 0;
        this.flightTicks = 0;
    }

    /**
     * 着地は3段階。ナイス着地：微加速（よく下降してから着地するほど大きい）、
     * 通常着地：速度が少し落ちる、着地失敗：大きく減速して、たまったチャージも消える
     */
    private void land(double fx, double fz, double fallSpeed) {
        float angle = Math.abs(this.flightPitch);
        double fwd = this.velX * fx + this.velZ * fz;
        if (angle > NORMAL_ANGLE) {
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
        } else if (angle > NICE_ANGLE) {
            this.landingResult = 3;
            double nf = (this.inCharge ? fwd : boost(fwd, 0.8f)) * 0.85;
            this.velX += fx * (nf - fwd);
            this.velZ += fz * (nf - fwd);
            this.velX *= 0.95;
            this.velZ *= 0.95;
        } else {
            this.landingResult = 1;
            double extra = Math.min(0.45, NICE_BASE + NICE_FALL * Math.max(0, -fallSpeed));
            double nf = (this.inCharge ? fwd : boost(fwd, 1f)) + extra;
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

        // 人が乗っている間は、サーバーで当たり判定をやり直さない（乗っている人の画面で計算した位置をそのまま使う）。
        // やり直すと計算が少しずれた時に「引っかかった」と判断され、マシンだけ前の位置に戻されてしまう
        this.noClip = rider != null;

        if (this.serverSpinCooldown > 0) this.serverSpinCooldown--;
        if (this.hurtCooldown > 0) this.hurtCooldown--;
        if (this.invulnTicks > 0) this.invulnTicks--;
        if (this.ramCooldown > 0) this.ramCooldown--;
        int spin = this.dataTracker.get(SPIN);
        if (spin > 0) this.dataTracker.set(SPIN, spin - 1);

        // サーバーでの速さ（位置の変化から計算。動きは乗っている人の画面で決まるため）
        Vec3d pos = this.getPos();
        Vec3d vel = this.lastServerPos == null ? Vec3d.ZERO : pos.subtract(this.lastServerPos);
        this.lastServerPos = pos;
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (hSpeed > 8) hSpeed = 0; // テレポートなど
        this.serverSpeed = hSpeed;

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
        double ramMin = this.topSpeed() * RAM_MIN;
        if (this.ramCooldown <= 0 && hSpeed >= ramMin) {
            Vec3d dir = new Vec3d(vel.x, 0, vel.z);
            for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(0.35),
                    e -> e.isAlive() && e != rider && e.getVehicle() != this && !e.isSpectator())) {
                Vec3d to = e.getPos().subtract(pos);
                if (to.x * dir.x + to.z * dir.z <= 0) continue; // 進んでいる方にいる相手だけ
                float dmg = (float) ((hSpeed / this.topSpeed() - RAM_MIN + 0.1) * 3.0 * this.attackMul() * this.ramPower());
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

        // キャプチャー：プッシュ中は地上でも空中でも、まわりの雑魚敵を吸い込む
        if (this.dataTracker.get(CHARGING)) tickCapture(world, rider);
        if (this.captureTimer > 0 && --this.captureTimer == 0) fireStar(world, rider);
    }

    /** キャプチャー：範囲内なら横や後ろの敵も吸い込む。捕らえたら少しあとに星型弾を撃つ */
    private void tickCapture(ServerWorld world, PlayerEntity rider) {
        Vec3d center = this.getPos().add(0, 0.4, 0);
        List<LivingEntity> list = world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(CAPTURE_RANGE),
                e -> e instanceof Monster && e.isAlive() && e != rider && !e.hasVehicle() && e.getMaxHealth() <= 40f);
        for (LivingEntity e : list) {
            Vec3d to = center.subtract(e.getPos().add(0, e.getHeight() * 0.5, 0));
            double dist = to.length();
            if (dist > CAPTURE_RANGE) continue;
            if (dist < 1.6) {
                world.spawnParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                e.discard();
                this.captured++;
                if (this.captureTimer <= 0) this.captureTimer = CAPTURE_FIRE_DELAY;
                this.invulnTicks = Math.max(this.invulnTicks, this.captureTimer + 4);
                world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1f, 1.2f);
                continue;
            }
            e.setVelocity(e.getVelocity().multiply(0.5).add(to.normalize().multiply(0.35)));
            e.velocityModified = true;
            if (this.age % 3 == 0) {
                world.spawnParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    /** 捕らえた敵を星型弾にして前へ撃つ。2体以上なら貫通する */
    private void fireStar(ServerWorld world, PlayerEntity rider) {
        int n = this.captured;
        this.captured = 0;
        if (n <= 0 || rider == null) return;
        Vec3d forward = Vec3d.fromPolar(0f, this.getYaw());
        StarShotEntity star = new StarShotEntity(SkyRiders.STAR_SHOT, world);
        star.setup(this, rider, n >= 2, (n >= 2 ? 8f : 10f) * this.attackMul());
        star.refreshPositionAndAngles(this.getX() + forward.x * 1.4, this.getY() + 0.5, this.getZ() + forward.z * 1.4, this.getYaw(), 0f);
        star.setVelocity(forward.multiply(1.4 + this.getServerSpeed()));
        world.spawnEntity(star);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1f, 0.7f);
    }

    /** チャージダッシュの演出：たまった量で変わる（ほかの人からも見える） */
    private void dashEffect(ServerWorld world, float amount) {
        Vec3d back = Vec3d.fromPolar(0f, this.getYaw()).multiply(-0.9);
        double x = this.getX() + back.x, y = this.getY() + 0.3, z = this.getZ() + back.z;
        world.spawnParticles(ParticleTypes.CLOUD, x, y, z, 3 + (int) (amount * 12), 0.3, 0.15, 0.3, 0.03);
        if (amount >= 0.4f) world.spawnParticles(ParticleTypes.FLAME, x, y, z, (int) (amount * 14), 0.25, 0.1, 0.25, 0.04);
        if (amount >= 0.8f) {
            world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 16, 0.3, 0.2, 0.3, 0.12);
            world.spawnParticles(ParticleTypes.SONIC_BOOM, x, y, z, 1, 0, 0, 0, 0);
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
            case MachineActionPayload.BLINK_WARN -> this.dataTracker.set(BLINK, Math.max(1, this.dataTracker.get(BLINK)));
            case MachineActionPayload.BLINK_ON -> this.dataTracker.set(BLINK, 2);
            case MachineActionPayload.BLINK_OFF -> this.dataTracker.set(BLINK, 0);
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
                if (action >= MachineActionPayload.DASH_BASE && action <= MachineActionPayload.DASH_BASE + 100) {
                    dashEffect(world, (action - MachineActionPayload.DASH_BASE) / 100f);
                }
            }
        }
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

    /** 星型弾が当たった時（吹っ飛ぶ向きは弾の向き） */
    void hitWithStar(LivingEntity e, PlayerEntity owner, float dmg, Vec3d vel) {
        Vec3d dir = new Vec3d(vel.x, 0, vel.z);
        if (e.getVehicle() instanceof MachineEntity other) {
            if (other != this && other.takeHit(dmg * MACHINE_DAMAGE_SCALE, owner, dir, 0.7)) addSpecial(5f);
            return;
        }
        boolean hurt = e.damage(this.getDamageSources().playerAttack(owner), dmg);
        if (dir.lengthSquared() > 1e-4) e.takeKnockback(0.7, -dir.x, -dir.z);
        if (hurt) {
            addSpecial(5f);
            if (!e.isAlive() && e instanceof Monster) dropPatchFrom(e);
        }
    }

    /**
     * マシンがダメージを受ける。防御は割合で軽減（上限90%）、よろけと吹っ飛びも減る。
     */
    public boolean takeHit(float raw, @Nullable Entity attacker, Vec3d dir, double knock) {
        if (this.getWorld().isClient || this.isRemoved() || this.hurtCooldown > 0 || this.invulnTicks > 0) return false;
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
    void dropPatchFrom(LivingEntity e) {
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

    void addSpecial(float amount) {
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
        NbtCompound patches = nbt.getCompound("Patches");
        for (PatchKind k : PatchKind.ALL) {
            this.dataTracker.set(LEVELS[k.ordinal()], MathHelper.clamp(patches.getInt(k.id), PatchKind.MIN, k.max));
        }
        this.dataTracker.set(HP, nbt.contains("Hp") ? Math.min(nbt.getFloat("Hp"), this.maxHp()) : this.maxHp());
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("Special", this.dataTracker.get(SPECIAL));
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

    /** 上昇の限界の点滅：0なし 1限界が近い（ゆっくり） 2限界（速く）。ほかの人からも見える */
    public int blinkLevel() {
        int local = this.overheated ? 2 : this.climbWarned ? 1 : 0;
        return Math.max(local, this.dataTracker.get(BLINK));
    }

    public double getServerSpeed() {
        return this.serverSpeed;
    }

    /** プッシュ中か（ブレーキランプ・沈み込み・キャプチャー） */
    public boolean isPushing() {
        return this.dataTracker.get(CHARGING) || (this.isLogicalSideForUpdatingMovement() && this.inCharge);
    }

    public boolean isSpecialReady() {
        return this.dataTracker.get(SPECIAL) >= 100f && this.dataTracker.get(SPECIAL_ACTIVE) <= 0;
    }

    /** 滑空中の見た目（ライダーの姿勢・ヴェイパー） */
    public boolean isGlidingVisual() {
        return this.visAirTicks > 6 && !this.isTouchingWater();
    }

    /** いちばん多く集めたパッチ（無ければ null） */
    @Nullable
    public PatchKind dominantPatch() {
        PatchKind best = null;
        int lv = 0;
        for (PatchKind k : PatchKind.ALL) {
            if (this.level(k) > lv) {
                lv = this.level(k);
                best = k;
            }
        }
        return best;
    }

    public boolean isOverheated() {
        return this.overheated;
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

}
