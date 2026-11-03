# Tons of Advancements（進捗どっさり）

進捗を1700個以上追加するmod／データパック。

- `forge/` … Forge 1.20.1 版（`data/tonsofadv/advancements`）
- `neoforge/` … NeoForge 1.21.1 版（`data/tonsofadv/advancement`）
- `datapack/` … データパック版（1つのZIPで1.20.1と1.21.1に対応。1.21.1用は `ov_1211` オーバーレイ）
- `tools/gen.py` … 進捗JSONと言語ファイルを生成するスクリプト  
  `python3 tools/gen.py <レジストリのフォルダ> <出力先>`（レジストリは misode/mcmeta の `<版>-summary/registries/data.min.json` を `reg-<版>.json` として置く）

## MOD連携（v1.1.0〜）

`tools/modsdata.json` に、各MODの公開ソース（GitHub）の言語ファイルとモデルから集めたアイテム・モブ・バイオームのIDが入っています。
生成される進捗には、Forge 1.20.1 では `conditions`（`forge:mod_loaded`）、NeoForge 1.21.1 では `neoforge:conditions`（`neoforge:mod_loaded`）が付くので、そのMODが入っている時だけ読み込まれます。
データパック版にはMOD連携の進捗は入りません（出力の `dp1201` / `dp1211` がバニラだけの分）。

## 人気MOD100個（v1.4.0〜）

`tools/modsdata.json` の `_selected` が対応している100個のMODです（人気順の目安）。各MODのGitHubのソースから、1.20.1 Forge と 1.21.1 NeoForge の両方のブランチのデータを集めています。ブロックの種類が400を超えるMODは均等に間引いています。

## 何かをした回数（v1.5.0〜）

- バニラの統計（`minecraft:crafted` / `used` / `broken` / `killed_by` / `custom`）を使う進捗はデータだけなので、データパック版にも入ります。
- 合計クラフト回数・置いたブロックなど、バニラの統計にない回数は `ActionStats.java` が独自の統計 `tonsofadv:*` として数えます（MOD版だけ）。
- Iron's Spells（`SpellOnCastEvent`）・Ars Nouveau（`SpellCastEvent`）・AE2（`MEStorageMenu` の `storage`）はリフレクションでつないでいます。

## 26.2 版（v1.7.0〜）

- `neoforge-26.2/` … NeoForge 26.2 版、`fabric-26.2/` … Fabric 26.2 版（Fabric API が必要）
- 26.2 の進捗は新しい書き方（`minecraft:entity_type` などの部品の組み合わせ）になっているので、`gen.py` の `fix26()` で変換しています。
- MOD連携の条件は、NeoForge 版が `neoforge:conditions`、Fabric 版が `fabric:load_conditions` です。
- Fabric 版の回数は Fabric API のイベントと、失敗しても止まらない（`require = 0`）ミックスイン、ゲームの統計の合計から数えます。クリティカル・飛び道具・経験値オーブは数えないので、その進捗は入れていません。
- データパックは `ov_262` オーバーレイ（formats 81〜107）で 26.2 に対応しています。
