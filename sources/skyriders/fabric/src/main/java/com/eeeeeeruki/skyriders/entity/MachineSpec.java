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
 * @param lift        揚力＝普通に滑空している時に落ちる速さ（マイナス、スター型だけ）
 * @param flySpeed    飛行速＝空中の最高速
 * @param attack      マシン固有の攻撃倍率（表示されない隠し倍率）
 * @param baseHp      耐久力（HP）
 * @param defenseMax  防御パッチ最大の時の軽減倍率（スター型3倍、バイク系3.5倍）
 * @param weight      重量の基礎（-1〜1、0が普通）
 */
public record MachineSpec(String id, boolean star, double topSpeed, double topMaxMul, double accel, float chargeTicks,
                          float turn, float grip, double lift, double flySpeed, float attack, float baseHp,
                          float defenseMax, float weight) {

    /** 仮マシン：スター型の標準。パッチ0で約62km/h */
    public static final MachineSpec PROTOTYPE = new MachineSpec("prototype", true,
            0.96, 3.0, 0.06, 30f, 4.0f, 0.22f, -0.12, 0.94, 1.0f, 100f, 3.0f, 0f);
}
