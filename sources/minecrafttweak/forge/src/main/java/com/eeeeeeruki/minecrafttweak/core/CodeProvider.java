package com.eeeeeeruki.minecrafttweak.core;

import net.minecraft.network.chat.Component;

import java.util.List;

/** 頭文字（v, s, se など）ごとの、設定コードの項目のまとまり。 */
public interface CodeProvider {
    /** 頭文字（英小文字）。 */
    String prefix();

    /** 一覧に出す名前。 */
    Component displayName();

    /** そのMODが入っているか。 */
    boolean available();

    /** クライアントの設定か（サーバーのコマンドからは変えない）。 */
    boolean clientOnly();

    List<CodeEntry> entries();

    /** いくつか変えたあとに1回だけ呼ぶ（保存・描画の更新など）。 */
    void afterApply();
}
