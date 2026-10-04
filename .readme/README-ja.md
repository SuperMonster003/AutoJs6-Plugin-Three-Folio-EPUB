<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="three-folio-epub-ic-launcher" border="0" width="128" />
  </p>

  <p>EPUB 電子書籍を読み, 目次, 検索, 読み上げとスクリプト抽出を提供</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語 (Languages)

******

現在の README.md は次の言語をサポートします:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-ar.md)

******

### 概要

******

ワンタップで読書: AutoJs6 のファイルマネージャーから `.epub` ファイルを直接開けます. メインボタン `EPUB を読む` でもメニューからでも開けます. リーダーは多くの商用リーダーが採用するオープンソースエンジン [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0 を基盤としています.

プラグインはホストが付与した一時的なファイル記述子から本を直接読み取ります. ファイルシステムのパスは受け取らず, 本をどこにもコピーせず, ストレージへ展開することもありません.

> この 1.2.0 開発ビルドではランチャーアイコンに 4 つの選択肢を追加しました. まだ公開されていません. 1.1.0 が現在のリリースで, 1.0.0 が最初のリリースです. リーダーは EPUB 2 と EPUB 3 の本を目次付きで開き, 本ごとの読書位置を記憶し, スクロールモード, タップゾーン, 音量キー, 没入モード, 設定パネル (文字サイズ, フォント, 間隔, 揃え, 段組, ホストの夜間モードに従えるテーマ), インポートした TTF / OTF フォント, CJK 縦書きと右から左の本, 単ページまたは見開きの固定レイアウトの本, 全文検索, ブックマーク, 本の中のリンク, 注と画像, そしてシステムのテキスト読み上げエンジンによる読み上げを提供します. アプリアイコンは最近の本とシステムのドキュメントピッカーを持つランチャーを開き, 他のアプリは `ACTION_VIEW` で EPUB を渡せ, 設定ページはリーダーの既定値, 端末に保存されるデータ, 手動のアップデート確認を扱います. `epub` スクリプト API, ホストのリーダーセッション, 3 つのサンプルスクリプトは AutoJs6 6.8.0 (ビルド 5318) に同梱されます. 1.1.0 はハイライトとノートを追加します (ROADMAP.md, P9): 選択したテキストを 4 色でハイライトまたは下線にしてノートを添えられ, ハイライトはページ上に描画されパネルに一覧されます (移動, 編集, 削除). 本のハイライトとノートはシステムの共有で Markdown として書き出すかファイルに保存できます. EPUB コントラクトバージョン 2 を持つホスト (5318 より新しい AutoJs6 ビルド) は `book.annotations()` でそれらを読み, リーダーセッションで `highlight` イベントを受け取ります. AutoJs6 6.8.0 (ビルド 5318) はコントラクトバージョン 1 のまま動作します.

******

### スクリーンショット

******

`docs/fixtures` で生成したサンプル本をスマートフォンで撮影したものです (第三者の本は一切写していません). 画面の言語は AutoJs6 の言語に従い, ここでは英語です:

<table>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/reader.png?raw=true" alt="reader" width="180" /><br/>読書</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/table-of-contents.png?raw=true" alt="table-of-contents" width="180" /><br/>目次</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/preferences.png?raw=true" alt="preferences" width="180" /><br/>読書設定</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/search.png?raw=true" alt="search" width="180" /><br/>全文検索</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/bookmarks.png?raw=true" alt="bookmarks" width="180" /><br/>ブックマーク</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/read-aloud.png?raw=true" alt="read-aloud" width="180" /><br/>読み上げ</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/dark-theme.png?raw=true" alt="dark-theme" width="180" /><br/>ダークテーマ</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/sepia-theme.png?raw=true" alt="sepia-theme" width="180" /><br/>セピアテーマ</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/vertical-ja.png?raw=true" alt="vertical-ja" width="180" /><br/>日本語縦書き</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/fixed-layout.png?raw=true" alt="fixed-layout" width="180" /><br/>固定レイアウト</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/launcher.png?raw=true" alt="launcher" width="180" /><br/>最近の本</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/settings.png?raw=true" alt="settings" width="180" /><br/>設定</td>
  </tr>
</table>

******

### 主な機能

******

- Readium エンジン: EPUB 2 (NCX) と EPUB 3 (NAV) の本を Readium ナビゲーターと Readium CSS で表示し, 内部リンク, 脚注, 画像に対応します.
- コピーなし: EPUB コンテナは読み取り専用の記述子から位置指定で読み取るため, 大きな本でもキャッシュファイルなしで開けます.
- 目次: ツールバーから任意の章へ移動でき, 入れ子の項目は階層を保ちます.
- 読書位置の記憶: 各書籍の最後の位置を内容の指紋でプラグインの私有ストレージに保存するため, 本を移動したり名前を変えたりしても続きから読めます; `最初から読む` で消去できます.
- リーダー画面: ツールバーに書名と章, 位置と割合を示す進捗バー, 中央タップでの没入モード, タップ領域と音量キーによるページ送り, スクロールまたはページモード.
- 読書設定: 下部パネルで文字サイズ, フォント, 行間, ページ余白, 段落間隔, 配置, ハイフネーション, 出版社のスタイル, 段組, ページまたはスクロールレイアウトを設定できます. 変更は即座に反映され, すべての書籍で記憶されます. ライト, セピア, ダークのテーマ, またはホストの夜間モードに追従; ツールバーとシステムバーはテーマの配色になります.
- フォントの取り込み: システムのドキュメントピッカーで TTF または OTF ファイルを選ぶと, 検証のうえプラグイン内に非公開で保存され (最大 10 個, 1 ファイル 20 MB), 読書設定パネルの内蔵フォントの後に並び, すべての本に適用され, 同じパネルから削除できます.
- CJK の縦書きと右から左へ読む本: 読み進める方向は出版物に従い, 右から左の本ではタップ領域が反転します. ページ進行が右から左の日本語と中国語の本は縦書きで表示され, `文字方向` 設定で横書きまたは縦書きを強制できます. インターフェース自体のレイアウト方向は AutoJs6 の言語に従い, 本とは独立です.
- 固定レイアウトの本: ページ番号は `x / N ページ` と表示され, パネルは `見開き表示` の選択 (自動では横向きで 2 ページを並べて表示) を提供し, 効かない文字設定を隠します. ピンチズームとドラッグは Readium 内蔵です.
- 全文検索: ツールバーの `検索` から本の中の一致箇所を 50 件ずつ (最大 500 件) 章ごとに前後の文脈と共に一覧し, タップでその位置へ移動してページ上の一致箇所をハイライトし, 進捗バーの上で前へ / 次へを操作できます.
- ブックマーク: ツールバーのアイコンが現在のページに印を付け (ブックマーク済みのページでは塗りつぶし表示), `ブックマーク` からすべてのブックマークを章名, 抜粋, 時刻と共に新しい順で一覧して, 移動, 削除, すべて消去ができます. 本ごとに (最大 500 件) 読書位置と並べて保存します.
- ジェスチャー, キー, リンク: タップでのページ送り (オフ, 左右, 上下), 音量キー, ハードウェアキーボード, 選択テキストのメニュー (コピー, 共有, ウェブ検索, テキスト処理アプリ). 本の中のリンクは戻る履歴を持ち, 注はダイアログに表示し, 外部リンクは確認後またはそのまま開き, タップした画像は全画面で表示します.
- 読み上げ: オーバーフローメニューの `読み上げ` はシステムのテキスト読み上げエンジンで現在のページから本を読み上げ, 読み上げ中の文をハイライトしてページを自動で送ります. ページ下のバーとメディア通知で再生 / 一時停止, 前の文 / 次の文, 停止を操作でき, ヘッドセットのボタンも使え, 速度, ピッチ, 言語, 音声を調整でき, 画面を消しても読み上げは続き, リーダーを閉じると停止します (`バックグラウンドで続ける` をオンにした場合を除く). 読み上げの設定にはスリープタイマー (15 / 30 / 60 分またはこの章の終わり) と画面点灯スイッチもあります.
- 外部リンク: `http` または `https` のリンクをタップすると完全なアドレスを表示し, 確認後にのみシステムブラウザーを開きます.
- スタンドアロンのランチャー: アプリアイコンから表紙, タイトル, 著者, 進捗, 最終閲覧時刻付きの最近の本のグリッドと, システムのドキュメントピッカーで本を選ぶ `EPUB を開く` ボタンを開けます. リーダーはファイルマネージャーから開くものと同じです.
- 他のアプリから開く: ファイルマネージャー, ブラウザー, メールアプリは `ACTION_VIEW` で `content://` の EPUB を渡せます. オーバーフローメニューの `最近の本に追加` は, 送信元が永続的なアクセスを許可している場合にランチャーへ残します.
- 設定ページ: テーマ, ページめくり, 読み上げの既定値, リンク, データ管理に加え, リリース履歴とタップしたときだけ GitHub に問い合わせる手動のアップデート確認
- スクリプトサービス: `org.autojs.plugin.EPUB` Binder サービスにより, AutoJs6 ホストはリーダーを開かずに本を読み取れます (メタデータ, 目次, 読書順序, プレーンテキストまたは軽量 Markdown の章テキスト, リソース, 全文検索, 位置数). リクエストには上限があり, 同時に開ける本は 8 冊まで, アクセスはホストに限られます. AutoJs6 6.8.0 はこれを `epub` モジュールとしてスクリプトに公開します (下記 "スクリプトからの利用" を参照).
- ホストのリーダーセッション: AutoJs6 ホストは `org.autojs.plugin.EPUB` サービスを通じて本のリーダーを開いて追従でき (位置, しおり, 閉じるイベント), ロケーター, href, 進捗へジャンプし, ページや章を送り, 読書設定を変更できます. リーダーはホストが使い捨てのセッショントークンを付けて明示的に起動したときだけ開き, セッションを閉じてもホストが終了を求めない限りリーダーは利用者に残ります.
- ホスト連携: メニューとダイアログは AutoJs6 の言語とダークモードに従い, Explorer Action の封筒はコンテンツを開く前に厳密に検証されます.
- 多言語: インターフェース, 説明, README, changelog を 10 言語で提供します.
- 設定でランチャーアイコンを選択: アダプティブ明色, アダプティブ暗色, アダプティブ自動 (既定), 透明背景. 自動配色と透過表示はランチャーに依存し, アイコンがキャッシュされたり背景が追加されたりする場合があります. 変更後にホーム画面のショートカットを再追加する必要がある場合があります.
- 言語, ナイトモード, テーマ色, ランチャーアイコンの設定を統一し, 確定後に保存. 16 色のプリセットと HEX/RGB の部分プレビューに対応. アプリの外観は既定で AutoJs6 に従い, ホストが利用できない場合は安全にフォールバック.

******

### インストール

******

1. プラグインセンターから: AutoJs6 で `プラグイン` を開き, 公式リストから `3-Folio EPUB` を選んでインストールをタップします. プラグインセンターが署名済み APK をダウンロードしてインストールし, プラグインを有効化できます.
2. GitHub から: [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/releases) ページから APK をダウンロードし (ファイル名に CRC32 が付き, `SHA256SUMS` にチェックサムがあります), インストールしてからプラグインセンターでプラグインを有効化します.
3. 要件: ファイルマネージャーの入口には AutoJs6 内部ビルド 5318 以降, `epub` スクリプト API には AutoJs6 6.8.0 (ビルド 5318) 以降, Android 7.0 以降, そしてシステム WebView が必要です.

******

### 使い方

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/releases) ページから最新のプラグイン APK をダウンロードして端末にインストールします.
2. AutoJs6 のプラグインセンターを開き, `3-Folio EPUB` プラグインを有効化します.
3. AutoJs6 のファイルマネージャーで `.epub` ファイルをタップするか, メニュー (その他の操作) を開いて `EPUB を読む` を選びます.
4. ツールバーの目次ボタンで章を移動し, 設定ボタンで文字とテーマを調整します. ページの左右 3 分の 1 をタップするか音量キーでページをめくり, 中央をタップしてツールバーを隠したり表示したりします. 戻るキーでリーダーを閉じると位置が記憶されます.
5. ファイルマネージャーを使わない場合はアプリアイコンをタップします. ランチャーが最近の本を一覧し, `EPUB を開く` でシステムのドキュメントピッカーから本を選べます. この方法で開いた本は表紙と進捗付きでリストに残ります.
6. 他のアプリ (ファイルマネージャー, ブラウザーのダウンロード, メールの添付) で `.epub` ファイルにこのリーダーを選びます. 本は同じように開き, オーバーフローメニューの `最近の本に追加` は送信元が永続的なアクセスを許可している場合にランチャーのリストへ残します.
7. ランチャーのメニューまたはリーダーのオーバーフローメニューから `設定` を開くと, テーマ, ページめくり, 読み上げの既定値, リンクを設定し, プラグインが保存するデータを消去し, リリース履歴を読み, アップデートを確認できます (確認はタップしたときだけ GitHub に接続します).
8. スクリプトから: `epub.open(path)` で本を読み取り (メタデータ, 目次, テキスト, 検索), `epub.read(path)` でこのリーダーを開いて位置の報告を受け取ります. 下記 "スクリプトからの利用" と AutoJs6 の `電子書籍` サンプルを参照してください.

> プラグインセンターにこのプラグインが表示されない場合は, まず AutoJs6 を新しいバージョン (内部ビルド 5318 以降) に更新してください. Explorer Action v2 は単一ファイルのメインボタンとメニューに対応し, 文書と親ディレクトリへの一時的な読み取り権限を使用します.

******

### スクリプトからの利用

******

AutoJs6 6.8.0 はグローバルモジュール `epub` (別名 `$epub`) を追加し, このプラグインがそれに応えます: リーダーを開かずに本を読み取ることも, スクリプトからリーダーを開いて位置を追うこともできます. AutoJs6 には `サンプル > 電子書籍` の下に 3 つのサンプルスクリプトが同梱され, リファレンスは [AutoJs6 ドキュメント](https://docs.autojs6.com/#/epub) にあります:

メタデータ, 目次, 章テキスト:

```javascript
let book = epub.open('./books/lighthouse.epub');
console.log(book.metadata.title, '-', (book.metadata.authors || []).join(', '));
book.toc.forEach(entry => console.log(entry.title, entry.href, (entry.children || []).length, 'children'));
console.log(book.readingOrder.length, 'resources,', book.positions, 'positions');
let first = book.readingOrder[0];
console.log(book.text(first.href, { format: 'markdown' }));
book.close();
```

表紙, 検索, 便利関数:

```javascript
let path = './books/lighthouse.epub';
let book = epub.open(path);
try {
    console.log('cover saved to', book.cover(files.cwd(), { overwrite: true }));
} catch (e) {
    if (!(e instanceof epub.EpubError) || e.code !== 'RESOURCE_NOT_FOUND') throw e;
    console.log('this book has no cover');
}
book.search('lighthouse', { limit: 20 }).forEach(hit => console.log(hit.title || hit.href, ':', hit.text));
files.write('./lighthouse.txt', book.textAll({ maxChars: 2 * 1024 * 1024 }));
book.close();
console.log(epub.metadata(path).language); // the convenience functions open and close the book themselves
epub.tocAsync(path).then(toc => console.log(toc.length, 'entries'));
```

リーダーを開いて位置を追う:

```javascript
let session = epub.read('./books/lighthouse.epub', { progression: 0.25, preferences: { theme: 'sepia' } });
session.on('open', e => console.log('opened', e.title, 'at', e.href, '|', e.positions, 'positions'));
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('bookmark', e => console.log('bookmark', e.action, e.locator.href, '| total', session.bookmarks().length));
session.on('close', e => console.log('closed:', e.reason)); // user, host, replaced, timeout, error or overflow
setTimeout(() => session.isOpen && session.nextChapter(), 30 * 1000);
setTimeout(() => session.isOpen && session.close(), 60 * 1000);
```

パスはスクリプトの作業ディレクトリからの相対パスまたは絶対パスです (`content://` URI は受け付けません). プラグインや本が使えないとき, どの呼び出しも `code` 付きの `EpubError` を投げます (`PLUGIN_UNAVAILABLE`, `NOT_EPUB`, `ENCRYPTED`, `PARSE_FAILED`, `TIMEOUT` など). `epub.isAvailable()` はプラグインがインストールされ有効かを返し, どのメソッドにも Promise を返す `*Async` 版があります.

******

### 対応形式

******

プラグインは次の拡張子を認識し, ホストが `application/epub+zip` と明示した拡張子なしファイルも受け付けます:

```text
epub
```

EPUB のみ対応: EPUB 2 または EPUB 3 のリフロー型と固定レイアウトの本. コミックアーカイブ (CBZ), オーディオブック, PDF, LCP 保護された本は対象外です. LCP 暗号化と示された本は文字化けを表示せず, 読み取れない旨を報告します.

******

### 互換性

******

プラグインに必要なもの, 検証した環境, 対象外のもの:

- AutoJs6: ファイルマネージャーの入口 (Explorer Action v2) には内部ビルド 5318 以降が必要です. `epub` スクリプト API, ホストのリーダーセッション, サンプルスクリプトには AutoJs6 6.8.0 (ビルド 5318) が必要で, これは本リリースで監査した最後のホストビルドです.
- Android 7.0 (API 24) から Android 16 (API 37, ターゲット) まで. ページは端末の WebView で描画されるため, 最新の Android System WebView または Chrome が前提です. プラグインにネイティブライブラリはなく, 16 KB ページの端末でもそのまま動作します.
- 検証済み: AVD API 24 / 33 / 36 / 37, Sony Xperia XZ1 Compact (Android 9), Redmi 12C (Android 13, MIUI), Xiaomi Pad 6 (Android 15, サービス側). 端末 x シナリオの表, 逸脱, WebView のバージョンは `docs/dev/compatibility-matrix.md` にあります.
- 本: EPUB 2 と EPUB 3, リフロー型と固定レイアウト, CJK 縦書きと右から左. DRM で保護された本 (LCP, Adobe ADEPT) は保護されていると報告し, 描画しません. PDF, MOBI, AZW, CBZ, オーディオブックは対象外です.
- 読み上げには本の言語の音声データを持つテキスト読み上げエンジンが必要です (Google 音声サービス, メーカーのエンジン, その他のインストール済みエンジン). 使えるエンジンのない端末では無音のままにならず, 約 20 秒後に報告します.
- サイズと性能: release APK は約 3.3 MB. 200 MB の本は 2017 年のスマートフォンで 1 から 3 秒で開き, 位置と指紋の計算は最初のページを遅らせません. 数千章の本は開くのに目立って時間がかかります (`docs/dev/performance-baseline.md`).

******

### よくある質問

******

#### 読書位置はどのように記憶されますか?

各書籍の最後の位置は, パスではなくファイル内容の指紋を鍵としてプラグインの私有ストレージに保存され, 同じ本を再び開くと続きから始まります. メニューの `最初から読む` で消去できます.

#### フォント, 文字サイズ, テーマは変更できますか?

はい. ツールバーから設定パネルを開くと, 文字サイズ, フォント (出版社の既定, セリフ, サンセリフ, 等幅, または Readium 同梱のアクセシビリティフォント), 行間, 余白, 間隔, 配置, 段組, テーマ (ライト, セピア, ダーク, ホストに従う) を設定できます. パネルの `フォントを取り込む` から独自の TTF または OTF ファイルを追加できます. フォントはプラグイン内に非公開で保存され, `フォントを管理` から削除できます.

#### このプラグインは本をどこかへアップロードしますか?

いいえ. プラグインは独自のサーバーを持ちません. ネットワークは本自体がリモートリソースを参照する場合と, 設定ページの手動アップデート確認 (タップしたときだけ HTTPS で GitHub Releases API に問い合わせ, 何もダウンロードしません) にのみ使用されます.

#### PDF, MOBI, AZW に対応しないのはなぜですか?

リーダーは Readium ツールキットの上に作られており, EPUB のみを描画します. PDF には別のレンダラーが必要で, MOBI / AZW は Amazon の形式でオープンな描画エンジンがありません. まず Calibre などのツールで EPUB に変換してください. コミックアーカイブ (CBZ) とオーディオブックも対象外です.

#### 読み上げの音が出ません

プラグインはシステム設定で選んだテキスト読み上げエンジンで話します (`ユーザー補助 > テキスト読み上げの出力`). 本の言語の音声データを持つエンジンがインストールされていること, メディア音量が上がっていること, 他のアプリがオーディオフォーカスを持っていないこと (通話や音楽は読み上げを一時停止します) を確認してください. エンジンが話せない言語の本はエンジンの既定の音声を使い, 使えるエンジンのない端末では約 20 秒後にメッセージが表示されます.

#### インポートしたフォントが本に反映されません

出版社のスタイルが独自のフォントを固定していることがあります: 設定パネルで `出版社のスタイル` をオフにして, インポートしたフォントを選び直してください. 受け付けるのは TTF と OTF ファイルのみで (フォントコレクション `.ttc` は専用のメッセージで拒否されます), フォントは本文に適用され, 出版社が特定のファミリーを指定した見出しはそのままです.

#### 日本語や中国語の縦書きの本はどう扱われますか?

spine が右から左のページ進行を宣言し, 言語が日本語または中国語の本は縦書きで描画され, 右から左にページをめくります. `文字方向` の設定でどの本でも横書きまたは縦書きを強制できます. 画面は AutoJs6 の言語の方向を保つため, 英語の画面は左から右のまま, 本は右から左に読みます.

******

### 権限とセキュリティ

******

プラグインは本の内容に対して Readium の既定動作を維持します. 本に含まれるスクリプトやリモートリソースは削除も遮断もされず, 平文の `http://` リソースも含まれます. 信頼できる本だけを開いてください.

- 最小権限: プラグインはホストが付与した一時的な content URI の読み取り権限のみを受け取り, ファイルシステムのパスを見ることも, 本をストレージへ書き込むこともありません.
- 厳密な封筒: Explorer Action リクエストは EPUB の対象を 1 つだけ, その親ディレクトリ, 一致するプロトコルバージョン, 対応ホストビルド, 2 つの読み取り権限を伴う必要があります. それ以外はファイルを開く前に拒否されます.
- 他のアプリ向けの別の入口: `ACTION_VIEW` は専用にエクスポートされたアクティビティが受け取り, 読み取り権限付きの `content://` ドキュメントだけを受け付けます (`file://` もディレクトリも不可). Explorer Action のアクティビティは引き続き AutoJs6 のプラグイン権限で保護されます. 送信元のアクセス権は `最近の本に追加` を選んだ場合にのみ保持されます.
- 保護されたスクリプトサービス: `org.autojs.plugin.EPUB` サービスはプラグイン権限の背後でエクスポートされ, 署名が一致する AutoJs6 ホストパッケージにのみ応答し, すべてのリクエストを固定の上限 (href の長さ, テキストウィンドウ, 検索ページ, オプションのサイズ, 同時に開く 8 冊, リソースごとに 64 MB) で検査し, バックグラウンドからリーダーを起動することはありません. リーダーセッションはホストに使い捨てのトークンを渡すだけで, リーダー Activity はホスト自身が起動し, 受け取られないセッションは 60 秒後に閉じ, 誤ったトークンでは何も開きません.
- アップデート確認は要求時のみ: `更新を確認` をタップしたときだけ, 設定ページが HTTPS で GitHub Releases API に問い合わせ (1 日 1 回まで, リダイレクトなし, 応答サイズ上限あり), 結果を表示してリリースページをブラウザーで開きます. プラグイン自身が何かをダウンロードやインストールすることはありません.
- 境界のある解析: 不正なコンテナ (ZIP でない, `container.xml` がない, パッケージ文書がない, manifest のパストラバーサル) はクラッシュせずエラーメッセージで終了します.
- 外部リンクは完全なアドレスを表示し, 確認後にのみシステムブラウザーで開きます. `http` と `https` 以外のスキームは拒否されます.
- 読書データは端末内に留まります: 位置は内容の指紋を鍵とし, ファイルのパスや名前はストレージに書き込みません.
- 読み上げはエクスポートされないメディア再生サービスで動き, 音声が読んでいる間だけ存在し, 停止, 本の終わり, リーダーの終了で止まります (`バックグラウンドで続ける` がオンなら通知から停止したときに止まります). テキストはシステム設定で選ばれたテキスト読み上げエンジンに渡され, プラグインはウェイクロックを保持しません.

マニフェストが要求するのはネットワーク権限, AutoJs6 プラグイン権限, そして読み上げのためのフォアグラウンドサービス権限 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) と Android 13+ の `POST_NOTIFICATIONS` (読み上げ開始時に一度だけ求め, 拒否しても読み上げは通知の操作ボタンなしで続きます) です. AndroidX はエクスポートされない動的レシーバーを保護するパッケージ限定の署名権限を追加しますが, 端末データへのアクセスは付与しません. ストレージ, メディア, カメラ, 位置情報, ユーザー補助, オーバーレイの権限は要求しません.

******

### プラグインインターフェース

******

以下は開発者向けの情報です. ホストは次の識別情報でプラグインを検出して実行します:

```text
application id: io.github.supermonster003.autojs6.plugin.three.folio.epub
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: three-folio-epub
engine: explorer-action
variant: default
protocol version: 2
minimum host build: 5318
audited host build: 5318
audited host protocol: 22
```

Explorer Action v2 は単一ファイルのメインボタンとメニューに対応し, 文書と親ディレクトリへの一時的な読み取り権限を使用します. AutoJs6 ビルド 5318 以降が必要です.

- [Explorer Action 互換性マトリックスを表示](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/explorer-action-compatibility.md)

******

### ロードマップ

******

ROADMAP.md は各マイルストーンを受け入れ基準と証拠付きのチェックリストとして追跡します: P0 から P9 (リーダー, 設定とフォント, 検索とブックマーク, 読み上げ, 単独の入口, ホスト契約, `epub` スクリプト API, 堅牢性, 1.0.0 リリースゲート, 1.1.0 のハイライト, ノート, エクスポート) はチェック済みです. 未チェックの項目は出荷済みの機能ではなく計画です. Issues でのフィードバックを歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v2.0.1

_2026/10/04_

- `改善` プラグインセンターのアイコンに Icon Studio で調整したサイズ, 位置, 明暗の図稿と円形背景を適用し, 再生成可能な原稿とパラメーターを保持

#### v2.0.0

_2026/10/03_

- `ヒント` 3-Folio EPUB に改名し, 新しいパッケージ名 io.github.supermonster003.autojs6.plugin.three.folio.epub を使用. Android では別のアプリとなり, Readium EPUB Reader の設定, 最近の書籍と注釈は自動移行されません. AutoJs6 5318 以降が必要
- `機能` 設定でランチャーアイコンを選択: アダプティブ明色, アダプティブ暗色, アダプティブ自動 (既定), 透明背景. 自動配色と透過表示はランチャーに依存し, アイコンがキャッシュされたり背景が追加されたりする場合があります. 変更後にホーム画面のショートカットを再追加する必要がある場合があります
- `機能` 言語, ナイトモード, テーマ色, ランチャーアイコンの設定を統一し, 確定後に保存. 16 色のプリセットと HEX/RGB の部分プレビューに対応. アプリの外観は既定で AutoJs6 に従い, ホストが利用できない場合は安全にフォールバック
- `機能` アプリの外観設定と読書内容の配色は独立
- `修正` アプリの外観設定がリーダーの画面方向とサイズを固定しなくなり, 回転時に横向きレイアウトと自動見開き表示が正しく更新される
- `修正` 読み取り設定の保存中にリーダーへ戻っても, 旧スクロールモードからの移行を含む保存待ちの選択がディスク上の古い値で上書きされない
- `改善` 中立的なグレーの背景と読みやすいテーマ色の Material 3 コントロールやダイアログを採用し, 余白, 線形アイコン, 区切り線を統一. ランチャーアイコンの既定を自動に変更し, 更新時は明示的な選択を保持して重複する入口を修復
- `改善` ランチャーとプラグインセンターのアイコンの見た目の大きさを統一し, 透明な背景と白黒または無彩色のグレースケールを使用

#### v1.1.0

_2026/09/21_

- `ヒント` 1.1.0 はロードマップ P9 (ハイライト, ノート, エクスポート) を完了します: リーダーは選択したテキストをハイライトしてノートを付け, ハイライトをパネルに一覧して Markdown として書き出します. EPUB コントラクトバージョン 2 により, それを持つホストは `book.annotations()` でハイライトを読み `highlight` イベントを受け取れます. AutoJs6 6.8.0 (ビルド 5282) はコントラクトバージョン 1 のまま動作します
- `機能` リーダー内のハイライトとノート (ロードマップ P9.2): テキスト選択ツールバーから前回選んだ色で選択範囲をハイライトするか, ノートエディター (ハイライト / 下線, 5 色, ノート) を開けます. ハイライトはページ内に描画され, タップするとエディターが再び開きます. "ハイライトとノート" パネルは章ごとに読書順で一覧し, ジャンプ / 編集 / 削除 / すべて削除に対応します. 設定ページでは保存済みのハイライトを表示および削除できます
- `機能` 本のハイライトとノートを Markdown としてエクスポート (ロードマップ P9.3): パネルのエクスポートボタンから, システムの共有シートでテキストを共有するか, ドキュメントピッカーで選んだ場所に `.md` ファイルを保存できます. 文書には書名, 著者, 読書順の各章と, それぞれの引用, ノート, 時刻が並びます
- `機能` ホスト向け EPUB コントラクトバージョン 2 (ロードマップ P9.4): `IEpubBook.getAnnotations` が本のハイライトとノートを読書順にページ単位で列挙し, リーダーセッションはハイライトの追加, 編集, 削除時に `highlight` イベントを送ります. プラグインは公開するベースラインをコントラクトバージョン 1 のまま保ち, `epubMaxContractVersion` でバージョン 2 を示し, 各ブックと各セッションにはホストのオープン要求が運んだバージョンで応答します. バージョン 1 のホスト (AutoJs6 6.8.0 build 5282) には変化がありません
- `依存関係` `androidx.room:room-runtime` 2.8.1 を追加 (ハイライトとノートのデータベース, ロードマップ D4 / P9). `room-compiler` はビルド時に KSP 経由でのみ実行されます
- `依存関係` `epub-api.aar` を EPUB コントラクトバージョン 2 のリリースビルドへ更新 (ホストモジュール `plugin-api/epub-api`. 元となるホストコミットとダイジェストは `libs/README.md` と `locks/host-api-aars.lock` に記録)

##### その他のリリース履歴

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

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

### ライセンスとサードパーティ通知

******

このプラグインは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/LICENSE) の下で提供されます. Readium Kotlin Toolkit (BSD 3-Clause), AndroidX Media3 と Jsoup, AutoJs6 のコントラクトライブラリ, その他 APK に同梱されるコンポーネントのバージョン, チェックサム, ライセンスは [サードパーティ通知](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/THIRD_PARTY_NOTICES.md) に一覧しています.

[Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit), [Readium CSS](https://github.com/readium/readium-css), [初期実装の参考プロジェクト](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/THIRD_PARTY_NOTICES.md#initial-project-references)の開発者に感謝します. 権利や表記に関する連絡は[協力方針](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/RIGHTS_AND_TAKEDOWN.md)をご覧ください.

******

### リンク

******

- AutoJs6 ドキュメント: https://docs.autojs6.com
- `epub` スクリプト API リファレンス: https://docs.autojs6.com/#/epub
- EPUB 3.3 仕様: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit
- サードパーティ通知: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/THIRD_PARTY_NOTICES.md
- 16 KB ページアライメントとビルド検証: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/16kb.md
