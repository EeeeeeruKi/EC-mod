# PicPop

画像やGIFを画面に表示するmodです（1.20.1 Forge / 1.21.1 NeoForge）。

- `/image <プレイヤー> <URL> [left|center|right] [down|center|up] [秒数] [大きさ%]` … 指定したプレイヤーの画面に表示します（OP権限、サーバーにも導入が必要）
- `/image clear [プレイヤー]` … 表示中の画像を消します
- `/imageview <URL> [位置…]` … 自分の画面だけに表示します（クライアントだけで使えます）
- チャットに流れた画像リンクには ［画像を表示］ ボタンが付き、押すと中央に表示します

ビルド: `forge/`（Java 17）、`neoforge/`（Java 21）でそれぞれ `gradle build`。
