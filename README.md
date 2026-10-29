# EC-mod

Claude が作った Minecraft のMODの配布用リポジトリです。

- jar は `jars/<MOD ID>/` に置いています。古い版も消さずに残します。
- `index.json` は **Claude MOD Updater** が起動時に読む更新情報です。MODの新しい版を出したら、ここに追記します。

## index.json の書き方

`mods` の中に MOD ID ごとに版を並べます。`mc` と `loader`（`forge` / `neoforge`）が合っていて、今入っている版より新しいものが更新対象になります。`sha256` は jar の SHA-256 です（一致しないとダウンロードしても入れ替えません）。

## ライセンス

MIT License（[LICENSE](LICENSE)）。ただし `jars/hijiki/`（ramune1256さんのひじきmodの移植）と `jars/hyperdaimc/`（SakurafuldさんのHyperlinkの移植）は対象外で、元のmodのライセンスに従います。
