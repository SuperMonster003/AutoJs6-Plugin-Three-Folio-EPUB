******

### リリース履歴

******

# v1.0.0

###### 2026/09/19

* `ヒント` 開発ビルド: ロードマップの P0 フェーズ (スケルトン, Readium 検証, テストフィクスチャ) が進行中. 最初の公開リリースはロードマップ P8 フェーズで提供されます
* `機能` AutoJs6 のファイルマネージャーで `.epub` ファイルに `EPUB を読む` のメインボタンとメニュー操作を提供 (プラグイン ID `readium-epub-reader`, Explorer Action v2); ホストが `application/zip` として報告する拡張子 `.epub` のファイルも受け付けます
* `機能` リーダーの基盤: EPUB 2 と EPUB 3 の本を Readium ナビゲーターで表示し, 目次と確認付きの外部リンクを提供
* `機能` 本はホストが付与したファイル記述子から位置指定でその場で読み取られ, ストレージへのコピーや展開は行いません
* `機能` インターフェース, 説明, README, changelog を 10 言語で提供
* `依存関係` Readium Kotlin Toolkit 3.4.0 を追加 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
