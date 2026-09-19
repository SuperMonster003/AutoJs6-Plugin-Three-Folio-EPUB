<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="readium-epub-reader-ic-launcher" border="0" width="128" />
  </p>

  <p>EPUB 電子書籍を読み, 目次, 検索, 読み上げとスクリプト抽出を提供</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語 (Languages)

******

現在の README.md は次の言語をサポートします:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ar.md)

******

### 概要

******

ワンタップで読書: AutoJs6 のファイルマネージャーから `.epub` ファイルを直接開けます. メインボタン `EPUB を読む` でもメニューからでも開けます. リーダーは多くの商用リーダーが採用するオープンソースエンジン [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0 を基盤としています.

プラグインはホストが付与した一時的なファイル記述子から本を直接読み取ります. ファイルシステムのパスは受け取らず, 本をどこにもコピーせず, ストレージへ展開することもありません.

> 現在の段階 (1.0.0 開発ビルド): リーダーは Readium の既定設定で本を開き, 目次を提供し, 各書籍の読書位置を記憶し, スクロールモード, タップ領域, 音量キー, 没入モードを提供します. ブックマーク, 設定, 全文検索, 読み上げ, 固定レイアウト, フォントの取り込み, 独立したランチャー入口, `epub` スクリプト API は ROADMAP.md で計画されており, まだ利用できません.

******

### 主な機能

******

- Readium エンジン: EPUB 2 (NCX) と EPUB 3 (NAV) の本を Readium ナビゲーターと Readium CSS で表示し, 内部リンク, 脚注, 画像に対応します.
- コピーなし: EPUB コンテナは読み取り専用の記述子から位置指定で読み取るため, 大きな本でもキャッシュファイルなしで開けます.
- 目次: ツールバーから任意の章へ移動でき, 入れ子の項目は階層を保ちます.
- 読書位置の記憶: 各書籍の最後の位置を内容の指紋でプラグインの私有ストレージに保存するため, 本を移動したり名前を変えたりしても続きから読めます; `最初から読む` で消去できます.
- リーダー画面: ツールバーに書名と章, 位置と割合を示す進捗バー, 中央タップでの没入モード, タップ領域と音量キーによるページ送り, スクロールまたはページモード.
- 外部リンク: `http` または `https` のリンクをタップすると完全なアドレスを表示し, 確認後にのみシステムブラウザーを開きます.
- ホスト連携: メニューとダイアログは AutoJs6 の言語とダークモードに従い, Explorer Action の封筒はコンテンツを開く前に厳密に検証されます.
- 多言語: インターフェース, 説明, README, changelog を 10 言語で提供します.

******

### 使い方

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) ページから最新のプラグイン APK をダウンロードして端末にインストールします.
2. AutoJs6 のプラグインセンターを開き, `Readium EPUB Reader` プラグインを有効化します.
3. AutoJs6 のファイルマネージャーで `.epub` ファイルをタップするか, メニュー (その他の操作) を開いて `EPUB を読む` を選びます.
4. ツールバーの目次ボタンで章を移動し, ページの左右 3 分の 1 をタップするか音量キーでページをめくり, 中央をタップしてツールバーを隠したり表示したりします. 戻るキーでリーダーを閉じると位置が記憶されます.

> プラグインセンターにこのプラグインが表示されない場合は, まず AutoJs6 を新しいバージョン (内部ビルド 5269 以降) に更新してください. Explorer Action v2 は単一ファイルのメインボタンとメニューに対応し, 文書と親ディレクトリへの一時的な読み取り権限を使用します.

******

### 対応形式

******

プラグインは次の拡張子を認識し, ホストが `application/epub+zip` と明示した拡張子なしファイルも受け付けます:

```text
epub
```

EPUB のみ対応: EPUB 2 または EPUB 3 のリフロー型と固定レイアウトの本. コミックアーカイブ (CBZ), オーディオブック, PDF, LCP 保護された本は対象外です. LCP 暗号化と示された本は文字化けを表示せず, 読み取れない旨を報告します.

******

### よくある質問

******

#### 読書位置はどのように記憶されますか?

各書籍の最後の位置は, パスではなくファイル内容の指紋を鍵としてプラグインの私有ストレージに保存され, 同じ本を再び開くと続きから始まります. メニューの `最初から読む` で消去できます.

#### フォント, 文字サイズ, テーマは変更できますか?

まだできません. 読書設定 (フォント, サイズ, 行間, 余白, テーマ) は設定マイルストーンで提供されます. 現在のビルドはスクロールまたはページモードを提供し, それ以外は Readium の既定値を使用します.

#### このプラグインは本をどこかへアップロードしますか?

いいえ. プラグインは独自のサーバーを持ちません. ネットワークは本自体がリモートリソースを参照する場合と, 独立した設定ページで計画中の手動アップデート確認にのみ使用されます.

******

### 権限とセキュリティ

******

プラグインは本の内容に対して Readium の既定動作を維持します. 本に含まれるスクリプトやリモートリソースは削除も遮断もされず, 平文の `http://` リソースも含まれます. 信頼できる本だけを開いてください.

- 最小権限: プラグインはホストが付与した一時的な content URI の読み取り権限のみを受け取り, ファイルシステムのパスを見ることも, 本をストレージへ書き込むこともありません.
- 厳密な封筒: Explorer Action リクエストは EPUB の対象を 1 つだけ, その親ディレクトリ, 一致するプロトコルバージョン, 対応ホストビルド, 2 つの読み取り権限を伴う必要があります. それ以外はファイルを開く前に拒否されます.
- 境界のある解析: 不正なコンテナ (ZIP でない, `container.xml` がない, パッケージ文書がない, manifest のパストラバーサル) はクラッシュせずエラーメッセージで終了します.
- 外部リンクは完全なアドレスを表示し, 確認後にのみシステムブラウザーで開きます. `http` と `https` 以外のスキームは拒否されます.
- 読書データは端末内に留まります: 位置は内容の指紋を鍵とし, ファイルのパスや名前はストレージに書き込みません.

マニフェストが要求するのはネットワーク権限と AutoJs6 プラグイン権限だけです. AndroidX はエクスポートされない動的レシーバーを保護するパッケージ限定の署名権限を追加しますが, 端末データへのアクセスは付与しません. ストレージ, メディア, カメラ, 位置情報, ユーザー補助, オーバーレイの権限は要求しません.

******

### プラグインインターフェース

******

以下は開発者向けの情報です. ホストは次の識別情報でプラグインを検出して実行します:

```text
application id: io.github.supermonster003.autojs6.plugin.readium.epub.reader
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: readium-epub-reader
engine: explorer-action
variant: default
protocol version: 2
minimum host build: 5269
audited host build: 5282
audited host protocol: 22
```

Explorer Action v2 は単一ファイルのメインボタンとメニューに対応し, 文書と親ディレクトリへの一時的な読み取り権限を使用します. AutoJs6 ビルド 5269 以降が必要です.

- [Explorer Action 互換性マトリックスを表示](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/explorer-action-compatibility.md)

******

### ロードマップ

******

計画中の機能と完了状況は ROADMAP.md にチェック可能なリストとして記録され, 受け入れ基準付きのマイルストーンで整理されています: 読書位置の記憶とブックマーク, 設定とフォントの取り込み, 全文検索, 読み上げ, 固定レイアウト, 独立アプリの入口, ホスト契約と `epub` スクリプト API. 未チェックの項目は出荷済みの機能ではなく計画を示します. Issues でのフィードバックを歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/09/19_

- `ヒント` 開発ビルド: ロードマップの P0 フェーズ (スケルトン, Readium 検証, テストフィクスチャ) が進行中. 最初の公開リリースはロードマップ P8 フェーズで提供されます
- `機能` AutoJs6 のファイルマネージャーで `.epub` ファイルに `EPUB を読む` のメインボタンとメニュー操作を提供 (プラグイン ID `readium-epub-reader`, Explorer Action v2); ホストが `application/zip` として報告する拡張子 `.epub` のファイルも受け付けます
- `機能` リーダーの基盤: EPUB 2 と EPUB 3 の本を Readium ナビゲーターで表示し, 目次と確認付きの外部リンクを提供
- `機能` 読書位置の記憶: 各書籍の最後の位置を内容の指紋 (開く際は簡易キー, その後は全ファイルの SHA-256) で保存し, 次回開いたときに復元します; `最初から読む` で消去できます
- `機能` 本はホストが付与したファイル記述子から位置指定でその場で読み取られ, ストレージへのコピーや展開は行いません
- `機能` インターフェース, 説明, README, changelog を 10 言語で提供
- `依存関係` Readium Kotlin Toolkit 3.4.0 を追加 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)

##### その他のリリース履歴

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルド

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release ビルド:

```powershell
.\gradlew.bat :app:assembleRelease
```

ビルド設定は `version.properties` から読み込みます. 現在の最小 SDK は 24, ターゲット SDK は 37 です.

******

### ローカライズとドキュメント生成

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` はプラグイン情報とリーダー UI をローカライズし, `plugin_instruction.md` はホストに表示する使用説明を提供します. README と changelog は必ず `.readme/` と `.changelog/` の JSON ソースを編集し, `py .python/generate_markdown.py` を実行して再生成します. 生成物を手で編集することはありません. `py .python/generate_markdown.py --check` でソースと生成物の同期を検証できます.

******

### リンク

******

- AutoJs6 ドキュメント: https://docs.autojs6.com
- EPUB 3.3 仕様: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md)
