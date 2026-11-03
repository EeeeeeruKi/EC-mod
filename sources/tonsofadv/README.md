# Tons of Advancements（進捗どっさり）

進捗を1700個以上追加するmod／データパック。

- `forge/` … Forge 1.20.1 版（`data/tonsofadv/advancements`）
- `neoforge/` … NeoForge 1.21.1 版（`data/tonsofadv/advancement`）
- `datapack/` … データパック版（1つのZIPで1.20.1と1.21.1に対応。1.21.1用は `ov_1211` オーバーレイ）
- `tools/gen.py` … 進捗JSONと言語ファイルを生成するスクリプト  
  `python3 tools/gen.py <レジストリのフォルダ> <出力先>`（レジストリは misode/mcmeta の `<版>-summary/registries/data.min.json` を `reg-<版>.json` として置く）
