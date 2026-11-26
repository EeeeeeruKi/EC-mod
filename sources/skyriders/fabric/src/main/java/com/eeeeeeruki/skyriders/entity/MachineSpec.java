package com.eeeeeeruki.skyriders.entity;

/**
 * マシンの基礎値（10項目＋α）。パッチはこの値に段階として乗る。
 *
 * @param star        スター型か（揚力が効くのはスター型だけ）
 * @param topSpeed    最高速（ブロック/tick、内部値0の時）
 * @param topMaxMul   最高速パッチ最大の時の倍率（基礎が低いマシンほど大きくする）
 * @param accel       加速＝最高速への収束力（1tickで差の何割を詰めるか）
 * @param chargeTicks チャージ満タンまでのtick
 * @param turn        旋回（度/tick）。滑空中の機首の上げ下げの速さにも効く
 * @param grip        グリップ＝地面での横すべりを1tickで消す割合
 * @param lift        揚力（0〜100）＝機首上げの上昇力・離陸の上昇・飛行時間。スター型だけ。
 *                    飛べるマシンと飛べないマシンで分かれる（32〜67 の値は使わない）
 * @param flySpeed    飛行速＝空中の最高速
 * @param attack      マシン固有の攻撃倍率（表示されない隠し倍率）
 * @param baseHp      耐久力（HP）
 * @param defenseMax  防御パッチ最大の時の軽減倍率（スター型3倍、バイク系3.5倍）
 * @param weight      重量の基礎（-1〜1、0が普通）
 * @param airChargeTicks 滑空中にチャージが満タンになるまでのtick（地上のチャージとは別の隠し値。0ならエアチャージなし）
 * @param driftTurnMul   プッシュ中（ドリフト）の旋回の倍率。1より小さいと「押した瞬間に曲がりにくくなる」マシン
 * @param chargeHoldTicks 満タンのまま押し続けてオーバーヒートするまでのtick
 * @param slowForward    停止状態でWを押した時の低速移動
 * @param slowBack       停止状態でSを押した時の後退
 * @param vapor          滑空中にヴェイパー（飛行機雲）を出すか
 */
public record MachineSpec(String id, boolean star, double topSpeed, double topMaxMul, double accel, float chargeTicks,
                          float turn, float grip, double lift, double flySpeed, float attack, float baseHp,
                          float defenseMax, float weight, float airChargeTicks,
                          float driftTurnMul, int chargeHoldTicks, double slowForward, double slowBack, boolean vapor) {

    /** 仮マシン：スター型の標準。パッチ0で約30km/h */
    public static final MachineSpec PROTOTYPE = new MachineSpec("prototype", true,
            0.465, 3.0, 0.06, 30f, 4.0f, 0.22f, 80, 0.94, 1.0f, 100f, 3.0f, 0f, 100f,
            1.5f, 60, 0.12, 0.08, true);
}
