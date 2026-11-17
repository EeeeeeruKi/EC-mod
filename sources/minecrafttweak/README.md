# minEcraft twEEk

Quark を前提MODにして、Quark にない小さな改善をたくさん詰め込んでいくためのMODです。
Quark にある機能は入れません。機能は最初はすべてオフです。

- `neoforge/` … Minecraft 1.21.1 / NeoForge
- `forge/` … Minecraft 1.20.1 / Forge

## 機能の足し方

1. `feature/<分類>/` に `Feature` を継承したクラスを作る（`ArmorStandArmsFeature` が見本）。
2. `core/Features.java` の static ブロックの一番下に `add(new ○○Feature());` を1行足す。
   Quark にすでにある機能は作らない。
3. 機能ごとの追加設定は `addOptions()` の中で `intOption` / `doubleOption` / `boolOption` を使って作る。
4. `lang/ja_jp.json` と `en_us.json` に `minecrafttweak.feature.<id>` と `.desc` を足す。

イベントの処理の最初で `isEnabled()` を確認すれば、設定画面でオフにした瞬間から止まります。

## テキスト設定

「番号 値」を並べると、まとめて変更できます（設定画面の入力欄、または `/minecrafttweak set`）。

- `N` … N番の機能のオン/オフ（`t` / `f`）
- `N.M` … N番の機能の M番目の追加設定（数値、または `t` / `f`）

例: `1 t 1.1 0`

`/minecrafttweak list` で番号つきの一覧と今の設定の文字列が出ます（OP権限が必要）。
番号は Features.java に足した順なので、新しい機能は必ず一番下に足してください。
