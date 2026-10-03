# AutoJs6 3-Folio EPUB

AutoJs6 のファイルマネージャーから 3-Folio EPUB を使う:

1. `3-Folio EPUB` プラグインをインストールして有効化します.
2. `.epub` ファイルをタップするか, メニューを開いて `EPUB を読む` を選びます.
3. Readium Kotlin Toolkit を基盤とするリーダーで本が開きます.

ページの左右 3 分の 1 をタップするか音量キーでページをめくり, 中央をタップしてツールバーを隠したり表示したりします. 読書位置は本ごとに保存され, 次回開いたときに復元されます. メニューの `最初から読む` で消去できます.

プラグインは content URI を通じて選択したファイルと親ディレクトリへの一時的な読み取り権限を受け取ります. 生のファイルシステムパスは受け取らず, 本をストレージへコピーすることもなく, 付与されたファイル記述子から EPUB コンテナを直接読み取ります.

2.0.0 が現在のリリースで, 1.0.0 が最初のリリースです. リーダーは EPUB 2 と EPUB 3 の本を目次付きで開き, 本ごとの読書位置を記憶し, スクロールモード, タップゾーン, 音量キー, 没入モード, 設定パネル (文字サイズ, フォント, 間隔, 揃え, 段組, ホストの夜間モードに従えるテーマ), インポートした TTF / OTF フォント, CJK 縦書きと右から左の本, 単ページまたは見開きの固定レイアウトの本, 全文検索, ブックマーク, 本の中のリンク, 注と画像, そしてシステムのテキスト読み上げエンジンによる読み上げを提供します. アプリアイコンは最近の本とシステムのドキュメントピッカーを持つランチャーを開き, 他のアプリは `ACTION_VIEW` で EPUB を渡せ, 設定ページはリーダーの既定値, 端末に保存されるデータ, 手動のアップデート確認を扱います. 1.1.0 はハイライトとノートを追加します (ROADMAP.md, P9): 選択したテキストを 4 色でハイライトまたは下線にしてノートを添えられ, ハイライトはページ上に描画されパネルに一覧されます (移動, 編集, 削除). 本のハイライトとノートはシステムの共有で Markdown として書き出すかファイルに保存できます. その背後の `org.autojs.plugin.EPUB` サービスは AutoJs6 ホストにメタデータ, 目次, テキスト, リソース, 検索を返し, ホスト主導のリーダーセッションを開きます (`epub.open(path)`, `epub.read(path)`, サンプルは `サンプル > 電子書籍`).

3-Folio EPUB に改名し, 新しいパッケージ名 io.github.supermonster003.autojs6.plugin.three.folio.epub を使用. Android では別のアプリとなり, Readium EPUB Reader の設定, 最近の書籍と注釈は自動移行されません. AutoJs6 5318 以降が必要

本にはスクリプトやリモートリソースが含まれることがあります. プラグインは Readium の既定動作を維持し, 平文の `http://` リソースを含めて遮断しません. 信頼できる本だけを開いてください.

Explorer Action v2 は単一ファイルのメインボタンとメニューの両方に対応します. AutoJs6 ビルド 5318 以降が必要です.

[Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit), [Readium CSS](https://github.com/readium/readium-css), [初期実装の参考プロジェクト](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/THIRD_PARTY_NOTICES.md#initial-project-references)の開発者に感謝します. 権利や表記に関する連絡は[協力方針](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/RIGHTS_AND_TAKEDOWN.md)をご覧ください.
