<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="three-folio-epub-ic-launcher" border="0" width="128" />
  </p>

  <p>閱讀 EPUB 電子書並提供目錄, 搜尋, 朗讀與指令碼擷取能力</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言 (Languages)

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/.readme/README-ar.md)

******

### 簡介

******

一鍵閱讀: 在 AutoJs6 檔案管理器中直接開啟 `.epub` 檔案, 既可點按主按鈕 `閱讀 EPUB`, 也可從溢位選單進入. 閱讀器基於 [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0 建置, 這是眾多商業閱讀器共同採用的開源引擎.

外掛程式直接透過宿主授予的暫時檔案描述元讀取書籍, 不會取得檔案系統路徑, 不會把書籍複製到任何位置, 也不會解壓縮到儲存空間.

> 目前 1.2.0 開發版本增加四種啟動器圖示選項, 尚未發布. 1.1.0 是目前版本, 1.0.0 是首個正式版本. 閱讀器開啟 EPUB 2 與 EPUB 3 書籍並提供目錄, 記住每本書的閱讀位置, 提供捲動模式, 點按區, 音量鍵翻頁與沉浸模式, 偏好面板 (字級, 字型, 間距, 對齊, 欄數與可跟隨宿主夜間模式的主題), 匯入的 TTF / OTF 字型, CJK 直排與從右到左的書籍, 以單頁或跨頁顯示的固定版面書籍, 全文搜尋, 書籤, 書內連結, 註釋與圖片, 以及使用系統文字轉語音引擎的朗讀. 應用程式圖示開啟帶最近書籍與系統文件選擇器的啟動器, 其它應用程式可透過 `ACTION_VIEW` 交來 EPUB, 設定頁涵蓋閱讀器預設值, 裝置上儲存的資料與手動更新檢查. `epub` 指令碼 API, 宿主閱讀器工作階段與三份範例指令碼隨 AutoJs6 6.8.0 (版本號 5318) 提供. 1.1.0 新增螢光標示與筆記 (ROADMAP.md, P9): 選取的文字可以用四種顏色螢光標示或加底線並附上筆記, 螢光標示繪製在頁面上並在面板中列出 (跳轉, 編輯, 刪除), 一本書的螢光標示與筆記可以經系統分享匯出為 Markdown 或儲存為檔案; 攜帶 EPUB 契約版本 2 的宿主 (比 5318 更新的 AutoJs6 建置) 透過 `book.annotations()` 讀取它們並在閱讀器工作階段上收到 `highlight` 事件, AutoJs6 6.8.0 (版本號 5318) 仍以契約版本 1 正常運作.

******

### 截圖

******

在手機上以 `docs/fixtures` 產生的樣本書擷取 (不展示任何第三方書籍); 介面跟隨 AutoJs6 的語言, 此處為英文:

<table>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/reader.png?raw=true" alt="reader" width="180" /><br/>閱讀</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/table-of-contents.png?raw=true" alt="table-of-contents" width="180" /><br/>目錄</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/preferences.png?raw=true" alt="preferences" width="180" /><br/>閱讀偏好</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/search.png?raw=true" alt="search" width="180" /><br/>全文搜尋</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/bookmarks.png?raw=true" alt="bookmarks" width="180" /><br/>書籤</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/read-aloud.png?raw=true" alt="read-aloud" width="180" /><br/>朗讀</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/dark-theme.png?raw=true" alt="dark-theme" width="180" /><br/>深色主題</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/sepia-theme.png?raw=true" alt="sepia-theme" width="180" /><br/>羊皮紙主題</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/vertical-ja.png?raw=true" alt="vertical-ja" width="180" /><br/>日文直排</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/fixed-layout.png?raw=true" alt="fixed-layout" width="180" /><br/>固定版面</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/launcher.png?raw=true" alt="launcher" width="180" /><br/>最近書籍</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/images/screenshots/settings.png?raw=true" alt="settings" width="180" /><br/>設定</td>
  </tr>
</table>

******

### 功能亮點

******

- Readium 引擎: EPUB 2 (NCX) 與 EPUB 3 (NAV) 書籍經 Readium 導覽器與 Readium CSS 算繪, 支援內部連結, 註腳與圖片.
- 不複製檔案: EPUB 容器透過唯讀描述元按位置讀取, 即使大體積書籍也無需快取檔案即可開啟.
- 目錄導覽: 從工具列跳轉到任意章節, 巢狀條目保留層級.
- 閱讀位置記憶: 每本書的最後位置按內容指紋儲存在外掛私有儲存中, 書籍移動或改名後仍能續讀; `從頭開始` 可清除.
- 閱讀器介面: 工具列顯示書名與章節, 進度列顯示位置與百分比, 點按中央切換沉浸模式, 點按區與音量鍵翻頁, 可選捲動或分頁模式.
- 閱讀偏好: 底部面板可設定字級, 字型族, 行距, 頁面邊距, 段落間距, 對齊, 連字號, 出版商樣式, 欄數與分頁或捲動配置; 修改即時生效並對所有書籍記憶. 淺色, 護眼, 深色三套主題, 或跟隨宿主夜間模式; 工具列與系統列採用主題配色.
- 字型匯入: 透過系統文件選擇器選取 TTF 或 OTF 檔案; 檔案經校驗後私有存放在外掛內 (最多 10 個, 單個 20 MB), 列在偏好面板的內建字型之後, 對所有書籍生效, 並可在同一面板中刪除.
- CJK 直排與由右至左的書籍: 閱讀進程跟隨出版物, 由右至左的書籍點按區隨之鏡像; 頁面進程為由右至左的日文與中文書籍直排呈現, `文字方向` 偏好可強制橫排或直排. 介面自身的配置方向跟隨 AutoJs6 語言, 與書籍無關.
- 固定版式書籍: 頁碼顯示為 `第 x / N 頁`, 面板提供 `雙頁顯示` 選擇 (自動在橫向時並排顯示兩頁) 並隱藏不生效的文字偏好; 雙指縮放與拖曳由 Readium 內建.
- 全文搜尋: 工具列 `搜尋` 入口尋找書中每一處命中, 每批 50 處 (上限 500), 依章節分組並顯示前後文; 點選結果即跳轉, 頁面上標示命中處, 進度列上方提供上一處 / 下一處.
- 書籤: 工具列圖示標記目前頁 (本頁已加書籤時圖示填滿), `書籤` 入口依時間倒序列出每個書籤的章節名, 文字片段與時間, 可跳轉, 刪除或全部清除; 依書儲存 (每本上限 500), 與閱讀位置放在一起.
- 手勢, 按鍵與連結: 點按翻頁 (關閉, 左右或上下), 音量鍵, 硬體鍵盤按鍵與選取文字選單 (複製, 分享, 網頁搜尋與文字處理應用程式); 書內連結帶返回堆疊, 註釋在對話框中顯示, 外部連結確認後開啟或直接開啟, 點按圖片全螢幕檢視.
- 朗讀: 溢出選單的 `朗讀` 用系統文字轉語音引擎從目前頁開始朗讀, 高亮正在朗讀的句子並自動翻頁跟隨; 頁面下方的工具列與媒體通知提供播放 / 暫停, 上一句 / 下一句與停止, 支援耳機按鍵, 可調節語速, 音調, 語言與語音, 熄屏後繼續朗讀, 關閉閱讀器即停止 (開啟 `背景繼續朗讀` 後除外), 朗讀設定還提供睡眠計時器 (15 / 30 / 60 分鐘或本章結束) 與螢幕常亮開關.
- 外部連結: 點按 `http` 或 `https` 連結時先顯示完整位址, 確認後才交給系統瀏覽器.
- 獨立啟動器: 應用程式圖示開啟最近書籍格線 (封面, 書名, 作者, 進度與最後閱讀時間) 與 `開啟 EPUB` 按鈕, 後者透過系統文件選擇器選書; 閱讀器與檔案管理器開啟的是同一個.
- 從其它應用程式開啟: 檔案管理器, 瀏覽器或郵件應用程式可以透過 `ACTION_VIEW` 交給閱讀器一個 `content://` EPUB; 溢出選單中的 `加入最近書籍` 在傳送方允許持久存取時把它留在啟動器中.
- 設定頁: 主題, 翻頁, 朗讀預設值, 連結與資料管理, 另有發行歷史與只在點按時詢問 GitHub 的手動更新檢查
- 指令碼服務: `org.autojs.plugin.EPUB` Binder 服務讓 AutoJs6 宿主無需開啟閱讀器即可讀取書籍 (中繼資料, 目錄, 閱讀順序, 純文字或輕量 Markdown 的章節文字, 資源, 全文搜尋與位置數), 請求有上限, 同時最多開啟 8 本書, 且只對宿主開放; AutoJs6 6.8.0 以 `epub` 模組把它提供給指令碼 (見下文 "指令碼呼叫").
- 宿主閱讀器工作階段: AutoJs6 宿主可經 `org.autojs.plugin.EPUB` 服務在某本書上開啟閱讀器並跟隨它 (位置, 書籤與關閉事件), 跳到 locator, href 或進度, 翻頁或跳章, 設定閱讀偏好; 閱讀器只由宿主攜帶一次性工作階段權杖顯式啟動, 關閉工作階段時閱讀器仍留給使用者, 除非宿主要求結束.
- 宿主整合: 選單與對話方塊跟隨 AutoJs6 的語言和深色模式; 開啟任何內容之前都會嚴格檢驗 Explorer Action 信封.
- 多語言: 介面, 說明, README 與更新日誌均提供 10 種語言.
- 設定頁提供自適應亮色, 自適應暗色, 自適應自動 (預設)與透明背景四種啟動器圖示. 自動配色與透明效果取決於啟動器, 部分系統可能快取圖示或加入背景. 切換後部分主畫面捷徑可能需要重新加入.
- 統一語言, 夜間模式, 主題色與啟動器圖示設定, 選擇後須確認儲存, 支援 16 種預設色彩與 HEX/RGB 局部預覽. 應用程式外觀預設跟隨 AutoJs6, 主程式無法使用時安全回退.

******

### 安裝

******

1. 從外掛中心: 在 AutoJs6 中開啟 `外掛`, 在官方列表中找到 `3-Folio EPUB` 並點按安裝; 外掛中心會下載已簽署的 APK, 完成安裝並提供啟用開關.
2. 從 GitHub: 在 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/releases) 頁面下載 APK (檔名帶 CRC32, `SHA256SUMS` 列出校驗值), 安裝後在外掛中心啟用外掛.
3. 需求: 檔案管理器入口需要 AutoJs6 內部版本號 5318 及以上, `epub` 指令碼 API 需要 AutoJs6 6.8.0 (版本號 5318) 及以上, Android 7.0 及以上, 以及系統 WebView.

******

### 使用方法

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/releases) 頁面下載最新的外掛程式 APK 並安裝到裝置.
2. 開啟 AutoJs6 的外掛程式中心, 啟用 `3-Folio EPUB` 外掛程式.
3. 在 AutoJs6 檔案管理器中點按 `.epub` 檔案, 或開啟其溢位選單 (更多操作) 並選擇 `閱讀 EPUB`.
4. 使用工具列上的目錄按鈕在章節間跳轉, 使用偏好按鈕調整文字與主題; 點按頁面左右三分之一或按音量鍵翻頁, 點按中央隱藏或顯示工具列; 按返回鍵關閉閱讀器, 閱讀位置會被記住.
5. 不經檔案管理器時, 點按應用程式圖示: 啟動器列出最近書籍, `開啟 EPUB` 透過系統文件選擇器選書; 這樣開啟的書籍會帶著封面與進度留在清單中.
6. 在其它應用程式 (檔案管理器, 瀏覽器的下載, 郵件附件) 中為 `.epub` 檔案選擇本閱讀器; 書籍以同樣方式開啟, 溢出選單中的 `加入最近書籍` 在傳送方允許持久存取時把它留在啟動器的清單中.
7. 在啟動器選單或閱讀器溢出選單中開啟 `設定`, 可設定主題, 翻頁, 朗讀預設值與連結, 清除外掛程式保存的資料, 閱讀發行歷史或檢查更新 (只有點按時才會聯絡 GitHub).
8. 從指令碼: `epub.open(path)` 讀取書籍 (中繼資料, 目錄, 文字, 搜尋), `epub.read(path)` 開啟本閱讀器並回報閱讀位置; 見下文 "指令碼呼叫" 與 AutoJs6 中的 `電子書` 範例.

> 若外掛程式中心未顯示該外掛程式, 請先將 AutoJs6 升級到較新版本 (內部版本號 5318 及以上). Explorer Action v2 同時支援單一檔案的主按鈕和溢位選單, 透過暫時唯讀授權存取文件及其父目錄.

******

### 指令碼呼叫

******

AutoJs6 6.8.0 新增全域模組 `epub` (別名 `$epub`), 由本外掛提供服務: 不開啟閱讀器即可讀取書籍, 也可以從指令碼開啟閱讀器並追蹤閱讀位置. AutoJs6 在 `範例 > 電子書` 下附帶三份範例指令碼, 完整參考見 [AutoJs6 文件](https://docs.autojs6.com/#/epub):

中繼資料, 目錄與章節文字:

```javascript
let book = epub.open('./books/lighthouse.epub');
console.log(book.metadata.title, '-', (book.metadata.authors || []).join(', '));
book.toc.forEach(entry => console.log(entry.title, entry.href, (entry.children || []).length, 'children'));
console.log(book.readingOrder.length, 'resources,', book.positions, 'positions');
let first = book.readingOrder[0];
console.log(book.text(first.href, { format: 'markdown' }));
book.close();
```

封面, 搜尋與便捷函式:

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

開啟閱讀器並追蹤位置:

```javascript
let session = epub.read('./books/lighthouse.epub', { progression: 0.25, preferences: { theme: 'sepia' } });
session.on('open', e => console.log('opened', e.title, 'at', e.href, '|', e.positions, 'positions'));
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('bookmark', e => console.log('bookmark', e.action, e.locator.href, '| total', session.bookmarks().length));
session.on('close', e => console.log('closed:', e.reason)); // user, host, replaced, timeout, error or overflow
setTimeout(() => session.isOpen && session.nextChapter(), 30 * 1000);
setTimeout(() => session.isOpen && session.close(), 60 * 1000);
```

路徑相對指令碼工作目錄或使用絕對路徑 (不接受 `content://` URI). 外掛或書籍不可用時每個呼叫都會擲出帶 `code` 的 `EpubError` (`PLUGIN_UNAVAILABLE`, `NOT_EPUB`, `ENCRYPTED`, `PARSE_FAILED`, `TIMEOUT` 等), `epub.isAvailable()` 告知外掛是否已安裝並啟用, 每個方法都有回傳 Promise 的 `*Async` 版本.

******

### 支援的格式

******

外掛程式識別以下檔案副檔名, 同時接受宿主明確標記為 `application/epub+zip` 的無副檔名檔案:

```text
epub
```

僅支援 EPUB: EPUB 2 或 EPUB 3 的可重排與固定版式書籍. 漫畫封存檔 (CBZ), 有聲書, PDF 與 LCP 加密書籍不在範圍內; 標記為 LCP 加密的書籍會提示無法讀取, 而不是算繪亂碼.

******

### 相容性

******

外掛的執行需求, 已驗證的環境與不在範圍內的內容:

- AutoJs6: 檔案管理器入口 (Explorer Action v2) 需要內部版本號 5318 及以上; `epub` 指令碼 API, 宿主閱讀器工作階段與範例指令碼需要 AutoJs6 6.8.0 (版本號 5318), 這也是本次發布稽核過的最高宿主版本.
- Android 7.0 (API 24) 至 Android 16 (API 37, 目標版本); 頁面在裝置的 WebView 中繪製, 需要較新的 Android System WebView 或 Chrome. 外掛不含原生程式庫, 在 16 KB 頁裝置上無需改動即可執行.
- 已在 AVD API 24 / 33 / 36 / 37, Sony Xperia XZ1 Compact (Android 9), Redmi 12C (Android 13, MIUI) 與 Xiaomi Pad 6 (Android 15, 服務側) 上驗證; 裝置 x 情境矩陣, 偏差與 WebView 版本見 `docs/dev/compatibility-matrix.md`.
- 書籍: EPUB 2 與 EPUB 3, 可重排與固定版面, CJK 直排與從右到左. 受 DRM 保護的書籍 (LCP, Adobe ADEPT) 會提示受保護而不會繪製; PDF, MOBI, AZW, CBZ 與有聲書不在範圍內.
- 朗讀需要帶有書籍語言語音資料的文字轉語音引擎 (Google 語音服務, 廠商引擎或其它已安裝引擎); 沒有可用引擎的裝置會在約 20 秒後給出提示, 而不是一直無聲.
- 體積與效能: release APK 約 3.3 MB; 200 MB 的書在 2017 年的手機上 1 到 3 秒開啟, 位置與指紋計算不會拖慢首屏, 數千章的書開啟明顯更慢 (`docs/dev/performance-baseline.md`).

******

### 常見問題

******

#### 閱讀位置是如何記住的?

每本書的最後位置按檔案內容指紋 (而非路徑) 儲存在外掛私有儲存中, 再次開啟同一本書時從上次位置繼續. 在溢出選單中選擇 `從頭開始` 可清除.

#### 可以更改字型, 文字大小或主題嗎?

可以. 從工具列開啟偏好面板即可設定字級, 字型族 (出版商預設, 襯線, 無襯線, 等寬或 Readium 內建的無障礙字型), 行距, 邊距, 間距, 對齊, 欄數與主題 (淺色, 護眼, 深色或跟隨宿主). 在面板中點按 `匯入字型` 即可加入自己的 TTF 或 OTF 檔案; 字型私有存放在外掛內, 可透過 `管理字型` 刪除.

#### 這個外掛程式會把我的書上傳到某處嗎?

不會. 外掛程式沒有自己的伺服器. 只有當書籍本身引用遠端資源時, 以及設定頁中的手動更新檢查 (只在你點按時透過 HTTPS 詢問 GitHub Releases API, 從不下載任何檔案), 才會使用網路.

#### 為什麼不支援 PDF, MOBI 或 AZW?

閱讀器基於 Readium 工具組建構, 它只繪製 EPUB. PDF 需要另一套繪製器, MOBI / AZW 是 Amazon 的格式且沒有開放的繪製引擎; 請先用 Calibre 之類的工具轉換為 EPUB. 漫畫封存 (CBZ) 與有聲書同樣不在範圍內.

#### 朗讀沒有聲音

外掛透過系統設定中選擇的文字轉語音引擎發聲 (`無障礙 > 文字轉語音輸出`). 請確認已安裝帶有書籍語言語音資料的引擎, 媒體音量已調高, 且沒有其它應用程式佔用音訊焦點 (來電或音樂會暫停朗讀). 引擎不會說的語言會使用引擎的預設語音; 沒有可用引擎的裝置會在約 20 秒後給出提示.

#### 匯入的字型在書中沒有生效

出版方樣式可能固定了自己的字型: 在偏好面板中關閉 `出版方樣式`, 再重新選擇匯入的字型. 只接受 TTF 與 OTF 檔案 (字型集 `.ttc` 會以專門的提示拒絕), 字型套用於內文, 出版方指定了特定字族的標題保持不變.

#### 日文或中文直排書籍如何處理?

spine 宣告從右到左翻頁且語言為日文或中文的書籍會直排繪製並從右向左翻頁; `文字方向` 偏好可以為任何書籍強制橫排或直排. 介面保持 AutoJs6 語言的方向, 因此英文介面仍從左到右, 而書籍從右向左閱讀.

******

### 權限與安全

******

外掛程式對書籍內容保留 Readium 的預設行為: 不移除也不攔截書內的指令碼與遠端資源, 包括明文 `http://` 資源. 請只開啟可信任的書籍.

- 最小權限: 外掛程式只接收宿主授予的暫時 content URI 讀取權限, 不接觸檔案系統路徑, 不把書籍寫入儲存空間.
- 嚴格信封: Explorer Action 請求必須恰好攜帶一個 EPUB 目標, 其父目錄, 相符的協定版本, 受支援的宿主組建以及兩項讀取授權; 其餘一律在開啟檔案前拒絕.
- 面向其它應用程式的獨立入口: `ACTION_VIEW` 由單獨匯出的 Activity 承載, 只接受帶讀取授權的 `content://` 文件 (不接受 `file://`, 也不接受目錄), Explorer Action 的 Activity 仍受 AutoJs6 外掛程式權限保護; 只有當你選擇 `加入最近書籍` 時才會保留傳送方的授權.
- 受保護的指令碼服務: `org.autojs.plugin.EPUB` 服務在外掛程式權限之後匯出, 只服務簽章相符的 AutoJs6 宿主套件, 每個請求都按固定上限校驗 (href 長度, 文字視窗, 搜尋分頁, 選項大小, 8 本同時開啟, 單個資源 64 MB), 並且從不從背景啟動閱讀器: 閱讀器工作階段只把一次性權杖交給宿主, 由宿主自己啟動閱讀器 Activity, 無人認領的工作階段 60 s 後關閉, 錯誤權杖什麼也打不開.
- 只按需檢查更新: 當你點按 `檢查更新` 時, 設定頁透過 HTTPS 詢問 GitHub Releases API (每天至多一次, 不跟隨重新導向, 限制回應大小), 顯示結果並在瀏覽器中開啟發佈頁面; 外掛程式自身從不下載或安裝任何東西.
- 有界解析: 損壞的容器 (非 zip, 缺 `container.xml`, 缺套件文件, manifest 路徑穿越) 以錯誤提示結束, 而不是當機.
- 外部連結完整顯示並在確認後才交給系統瀏覽器; `http` 與 `https` 之外的 scheme 一律拒絕.
- 閱讀資料只在本機: 位置以內容指紋為鍵, 不會把檔案路徑或檔案名稱寫入儲存.
- 朗讀在一個未匯出的媒體播放服務中進行, 它只在朗讀期間存在, 在你停止, 到達書末或關閉閱讀器時結束 (開啟 `背景繼續朗讀` 後則在你從通知列停止時結束); 文字交給系統設定中選定的文字轉語音引擎, 外掛程式不持有喚醒鎖.

資訊清單申請網路權限, AutoJs6 外掛程式權限, 以及朗讀所需的前台服務權限 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) 和 Android 13+ 的 `POST_NOTIFICATIONS` (僅在開始朗讀時請求一次, 可以拒絕, 拒絕後朗讀繼續但通知列中沒有控制按鈕). AndroidX 還會附帶一個套件內簽章權限, 用於保護未匯出的動態接收器, 它不授予任何裝置資料存取. 不申請儲存空間, 媒體, 相機, 位置, 無障礙或懸浮視窗權限.

******

### 外掛介面

******

以下資訊面向開發者, 宿主透過這些識別資訊探索並執行外掛程式:

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

Explorer Action v2 同時支援單一檔案的主按鈕和溢位選單, 透過暫時唯讀授權存取文件及其父目錄. 需要 AutoJs6 組建 5318 或更新版本.

- [檢視 Explorer Action 相容性矩陣](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/explorer-action-compatibility.md)

******

### 開發路線圖

******

ROADMAP.md 以可勾選的列表追蹤每個里程碑, 附驗收標準與證據: P0 至 P9 (閱讀器, 偏好與字型, 搜尋與書籤, 朗讀, 獨立入口, 宿主契約, `epub` 指令碼 API, 強健性, 1.0.0 發布 gate 與 1.1.0 的螢光標示, 筆記與匯出) 已勾選. 未勾選的條目是規劃而非已交付的能力. 歡迎透過 Issues 回饋.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v2.0.1

_2026/10/04_

- `優化` 外掛程式中心圖示採用統一工作台調整後的尺寸, 位置, 明暗圖稿與圓形底色, 保留可重建原稿和參數

#### v2.0.0

_2026/10/03_

- `提示` 更名為 3-Folio EPUB, 新安裝套件名稱為 io.github.supermonster003.autojs6.plugin.three.folio.epub. Android 將其視為獨立應用程式, 原 Readium EPUB Reader 的設定, 最近書籍與註記不會自動移轉. 需要 AutoJs6 5318 或更新版本
- `新增` 設定頁提供自適應亮色, 自適應暗色, 自適應自動 (預設)與透明背景四種啟動器圖示. 自動配色與透明效果取決於啟動器, 部分系統可能快取圖示或加入背景. 切換後部分主畫面捷徑可能需要重新加入
- `新增` 統一語言, 夜間模式, 主題色與啟動器圖示設定, 選擇後須確認儲存, 支援 16 種預設色彩與 HEX/RGB 局部預覽. 應用程式外觀預設跟隨 AutoJs6, 主程式無法使用時安全回退
- `新增` 應用程式外觀設定與閱讀內容配色互相獨立
- `修復` 應用程式外觀設定不再固定閱讀器的視窗方向和尺寸, 橫向版面與自動雙頁顯示可隨旋轉正確更新
- `修復` 閱讀偏好尚未寫入完成時返回閱讀器, 不再以磁碟舊值覆蓋待儲存的選擇, 包括舊版捲動模式的移轉
- `優化` 採用中性灰階底色與清晰可讀的主題色 Material 3 控制項和對話方塊, 統一間距, 線性圖示與分隔線. 啟動器圖示預設改為自動, 升級保留明確選擇並修正重複入口
- `優化` 啟動器與外掛中心圖示按統一視覺尺寸標準調整, 外掛中心採用透明背景和黑白或中性灰階圖案

#### v1.1.0

_2026/09/21_

- `提示` 1.1.0 完成路線圖 P9 (螢光標示, 筆記與匯出): 閱讀器對選取的文字螢光標示並添加筆記, 在面板中列出螢光標示並匯出為 Markdown; EPUB 契約版本 2 讓攜帶它的宿主透過 `book.annotations()` 讀取螢光標示並收到 `highlight` 事件, AutoJs6 6.8.0 (版本號 5282) 仍以契約版本 1 正常運作
- `新增` 閱讀器內的螢光標示與筆記 (路線圖 P9.2): 文字選取工具列可用上次選擇的顏色標示所選段落, 或開啟筆記編輯器 (螢光標示 / 底線, 五種顏色, 筆記); 標示呈現在頁面中, 點按即可重新開啟編輯器; "螢光標示與筆記" 面板按章節以閱讀順序列出, 支援跳轉 / 編輯 / 刪除 / 全部清除; 設定頁顯示並可清除已儲存的標示
- `新增` 將一本書的螢光標示與筆記匯出為 Markdown (路線圖 P9.3): 面板的匯出按鈕可經系統分享面板分享文字, 或將 `.md` 檔案儲存到文件選擇器中選定的位置; 文件按閱讀順序列出書名, 作者與各章節, 含每段引文, 其筆記與時間
- `新增` 面向宿主的 EPUB 契約版本 2 (路線圖 P9.4): `IEpubBook.getAnnotations` 按閱讀順序分頁列出一本書的螢光標示與筆記, 閱讀器會話在螢光標示被新增, 編輯或刪除時發送 `highlight` 事件; 外掛仍以契約版本 1 作為對外聲明的基線, 在 `epubMaxContractVersion` 中聲明版本 2, 每本書與每個會話均以宿主開啟請求所攜帶的版本應答, 版本 1 的宿主 (AutoJs6 6.8.0 build 5282) 不受影響
- `相依性` 附加 `androidx.room:room-runtime` 2.8.1 (螢光標示與筆記資料庫, 路線圖 D4 / P9); `room-compiler` 僅在建置期經 KSP 執行
- `相依性` 升級 `epub-api.aar` 至 EPUB 契約版本 2 的發布建置 (宿主模組 `plugin-api/epub-api`; 來源宿主提交與摘要記錄於 `libs/README.md` 與 `locks/host-api-aars.lock`)

##### 更多發行歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 建置:

```powershell
.\gradlew.bat :app:assembleRelease
```

建置參數來自 `version.properties`, 目前最低 SDK 為 24, 目標 SDK 為 37.

******

### 本地化與文件產生

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

`strings.xml` 提供外掛資訊和閱讀器介面的本地化, `plugin_instruction.md` 提供宿主端顯示的使用說明. README 與更新日誌一律修改 `.readme/` 與 `.changelog/` 下的 JSON 來源檔案, 再執行 `py .python/generate_markdown.py` 重新產生, 產生產物不手動編輯; 執行 `py .python/generate_markdown.py --check` 可校驗來源檔案與產生產物是否同步.

******

### 授權條款與第三方聲明

******

外掛以 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/LICENSE) 授權. Readium Kotlin Toolkit (BSD 3-Clause), AndroidX Media3 與 Jsoup, AutoJs6 契約程式庫以及 APK 中附帶的其它元件的版本, 校驗值與授權條款列於 [第三方聲明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/THIRD_PARTY_NOTICES.md).

感謝 [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit), [Readium CSS](https://github.com/readium/readium-css) 及[初始專案參考項目](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/THIRD_PARTY_NOTICES.md#initial-project-references)的開發者. 署名或權利相關問題可參閱[權利與配合說明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/RIGHTS_AND_TAKEDOWN.md).

******

### 相關連結

******

- AutoJs6 文件: https://docs.autojs6.com
- `epub` 指令碼 API 參考: https://docs.autojs6.com/#/epub
- EPUB 3.3 規範: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/THIRD_PARTY_NOTICES.md
- 16 KB 頁對齊與建構驗證: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/docs/16kb.md
