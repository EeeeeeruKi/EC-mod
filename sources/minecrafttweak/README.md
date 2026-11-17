# minEcraft twEEk

Quark を前提MODにして、Quark にない小さな改善をたくさん詰め込んでいくためのMODです。
Quark にある機能は入れません。機能は最初はすべてオフです。

- `neoforge/` … Minecraft 1.21.1 / NeoForge
- `forge/` … Minecraft 1.20.1 / Forge

## 機能の足し方

1. `feature/<分類>/` に `Feature` を継承したクラスを作る（`ArmorStandArmsFeature` が見本）。
2. `core/Features.java` の static ブロックの一番下に `add(new ○○Feature());` を1行足す。
   Quark にすでにある機能は作らない。
3. 機能ごとの詳細設定は `addOptions()` の中で `intOption` / `doubleOption` / `boolOption` を使って作る。
   作った順に `1a`, `1b`… の番号が付き、設定画面の「詳細」ボタンから変更できる。
4. `lang/ja_jp.json` と `en_us.json` に `minecrafttweak.feature.<id>` と `.desc`、詳細設定の `minecrafttweak.feature.<id>.<設定id>`（と `.desc`）を足す。

イベントの処理の最初で `isEnabled()` を確認すれば、設定画面でオフにした瞬間から止まります。

## 設定コード（インポート/エクスポート）

「項目番号-値」を「,」でつないだ文字列で、まとめて設定できます。

- `N` … N番の機能のオン/オフ（`t` / `f`）
- `N`＋英字 … N番の機能の詳細設定（`a` が1つ目、`b` が2つ目…。値は数値、または `t` / `f`）

例: `1-t,1a-2`

- 設定画面の下の欄に貼って「インポート」。「コピー」で今の設定コードをクリップボードに写せます。
- コマンド: `/minecrafttweak import 1-t,1a-2`、`/minecrafttweak list`（OP権限が必要）。

番号は Features.java に足した順なので、新しい機能は必ず一番下に足してください。
