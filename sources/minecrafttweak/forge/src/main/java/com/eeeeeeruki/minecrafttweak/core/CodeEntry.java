package com.eeeeeeruki.minecrafttweak.core;

import net.minecraft.network.chat.Component;

/** 設定コードで変えられる1項目（バニラや他MODの設定）。 */
public interface CodeEntry {
    /** 設定コードでの項目番号（例: v1, s3, se12）。 */
    String key();

    /** 画面やチャットに出す名前。 */
    Component name();

    /** 今の環境で使えるか（MODが入っていない、バージョンが違う、など）。 */
    boolean available();

    /** 設定コードに書くときの値（t / f / 数値）。 */
    String textValue();

    /** 文字列から値を入れる。うまくいけば null、だめならエラーの文を返す。 */
    Component apply(String raw);
}
