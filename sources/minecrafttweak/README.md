# minEcraft twEEk

Quark のように、バニラをちょっと便利にする小さな改善をたくさん詰め込んでいくためのMODです。

- `neoforge/` … Minecraft 1.21.1 / NeoForge
- `forge/` … Minecraft 1.20.1 / Forge

## 機能の足し方

1. `feature/<分類>/` に `Feature` を継承したクラスを作る（`ArmorStandArmsFeature` が見本）。
2. `core/Features.java` の static ブロックに `add(new ○○Feature());` を1行足す。
3. `lang/ja_jp.json` と `en_us.json` に `minecrafttweak.feature.<id>` と `.desc` を足す。

イベントの処理の最初で `isEnabled()` を確認すれば、設定画面でオフにした瞬間から止まります。
機能ごとの追加設定は `addOptions()` で define します。
