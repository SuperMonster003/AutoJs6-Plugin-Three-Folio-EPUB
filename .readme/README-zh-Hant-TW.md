<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="readium-epub-reader-ic-launcher" border="0" width="128" />
  </p>

  <p>閱讀 EPUB 電子書並提供目錄, 搜尋, 朗讀與指令碼擷取能力</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言 (Languages)

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ar.md)

******

### 簡介

******

一鍵閱讀: 在 AutoJs6 檔案管理器中直接開啟 `.epub` 檔案, 既可點按主按鈕 `閱讀 EPUB`, 也可從溢位選單進入. 閱讀器基於 [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0 建置, 這是眾多商業閱讀器共同採用的開源引擎.

外掛程式直接透過宿主授予的暫時檔案描述元讀取書籍, 不會取得檔案系統路徑, 不會把書籍複製到任何位置, 也不會解壓縮到儲存空間.

> 目前階段 (1.0.0 開發建置): 閱讀器以 Readium 預設設定開啟書籍, 提供目錄, 記住每本書的閱讀位置, 提供捲動模式, 點按區, 音量鍵翻頁與沉浸模式, 並提供偏好面板設定字級, 字型, 間距, 對齊, 欄數與主題 (可跟隨宿主夜間模式), 並可匯入自己的 TTF / OTF 字型. 書籤, 全文搜尋, 朗讀, 固定版式, 獨立啟動入口與 `epub` 指令碼 API 已在 ROADMAP.md 中排程, 目前尚不可用.

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
- 外部連結: 點按 `http` 或 `https` 連結時先顯示完整位址, 確認後才交給系統瀏覽器.
- 宿主整合: 選單與對話方塊跟隨 AutoJs6 的語言和深色模式; 開啟任何內容之前都會嚴格檢驗 Explorer Action 信封.
- 多語言: 介面, 說明, README 與更新日誌均提供 10 種語言.

******

### 使用方法

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) 頁面下載最新的外掛程式 APK 並安裝到裝置.
2. 開啟 AutoJs6 的外掛程式中心, 啟用 `Readium EPUB Reader` 外掛程式.
3. 在 AutoJs6 檔案管理器中點按 `.epub` 檔案, 或開啟其溢位選單 (更多操作) 並選擇 `閱讀 EPUB`.
4. 使用工具列上的目錄按鈕在章節間跳轉, 使用偏好按鈕調整文字與主題; 點按頁面左右三分之一或按音量鍵翻頁, 點按中央隱藏或顯示工具列; 按返回鍵關閉閱讀器, 閱讀位置會被記住.

> 若外掛程式中心未顯示該外掛程式, 請先將 AutoJs6 升級到較新版本 (內部版本號 5269 及以上). Explorer Action v2 同時支援單一檔案的主按鈕和溢位選單, 透過暫時唯讀授權存取文件及其父目錄.

******

### 支援的格式

******

外掛程式識別以下檔案副檔名, 同時接受宿主明確標記為 `application/epub+zip` 的無副檔名檔案:

```text
epub
```

僅支援 EPUB: EPUB 2 或 EPUB 3 的可重排與固定版式書籍. 漫畫封存檔 (CBZ), 有聲書, PDF 與 LCP 加密書籍不在範圍內; 標記為 LCP 加密的書籍會提示無法讀取, 而不是算繪亂碼.

******

### 常見問題

******

#### 閱讀位置是如何記住的?

每本書的最後位置按檔案內容指紋 (而非路徑) 儲存在外掛私有儲存中, 再次開啟同一本書時從上次位置繼續. 在溢出選單中選擇 `從頭開始` 可清除.

#### 可以更改字型, 文字大小或主題嗎?

可以. 從工具列開啟偏好面板即可設定字級, 字型族 (出版商預設, 襯線, 無襯線, 等寬或 Readium 內建的無障礙字型), 行距, 邊距, 間距, 對齊, 欄數與主題 (淺色, 護眼, 深色或跟隨宿主). 在面板中點按 `匯入字型` 即可加入自己的 TTF 或 OTF 檔案; 字型私有存放在外掛內, 可透過 `管理字型` 刪除.

#### 這個外掛程式會把我的書上傳到某處嗎?

不會. 外掛程式沒有自己的伺服器. 只有當書籍本身引用遠端資源時, 以及獨立設定頁規劃中的手動更新檢查, 才會使用網路.

******

### 權限與安全

******

外掛程式對書籍內容保留 Readium 的預設行為: 不移除也不攔截書內的指令碼與遠端資源, 包括明文 `http://` 資源. 請只開啟可信任的書籍.

- 最小權限: 外掛程式只接收宿主授予的暫時 content URI 讀取權限, 不接觸檔案系統路徑, 不把書籍寫入儲存空間.
- 嚴格信封: Explorer Action 請求必須恰好攜帶一個 EPUB 目標, 其父目錄, 相符的協定版本, 受支援的宿主組建以及兩項讀取授權; 其餘一律在開啟檔案前拒絕.
- 有界解析: 損壞的容器 (非 zip, 缺 `container.xml`, 缺套件文件, manifest 路徑穿越) 以錯誤提示結束, 而不是當機.
- 外部連結完整顯示並在確認後才交給系統瀏覽器; `http` 與 `https` 之外的 scheme 一律拒絕.
- 閱讀資料只在本機: 位置以內容指紋為鍵, 不會把檔案路徑或檔案名稱寫入儲存.

資訊清單只申請網路權限與 AutoJs6 外掛程式權限. AndroidX 還會附帶一個套件內簽章權限, 用於保護未匯出的動態接收器, 它不授予任何裝置資料存取. 不申請儲存空間, 媒體, 相機, 位置, 無障礙或懸浮視窗權限.

******

### 外掛介面

******

以下資訊面向開發者, 宿主透過這些識別資訊探索並執行外掛程式:

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

Explorer Action v2 同時支援單一檔案的主按鈕和溢位選單, 透過暫時唯讀授權存取文件及其父目錄. 需要 AutoJs6 組建 5269 或更新版本.

- [檢視 Explorer Action 相容性矩陣](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/explorer-action-compatibility.md)

******

### 開發路線圖

******

規劃中的能力及其完成狀態以可勾選清單的形式記錄在 ROADMAP.md 中, 按里程碑組織並附驗收標準: 閱讀位置記憶與書籤, 偏好設定與字型匯入, 全文搜尋, 朗讀, 固定版式, 獨立應用程式入口, 宿主契約與 `epub` 指令碼 API. 未勾選的條目表示規劃而非已交付能力. 歡迎透過 Issues 回饋.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/09/19_

- `提示` 開發組建: 路線圖 P0 階段 (骨架, Readium 驗證, 測試樣本) 進行中; 首個公開版本隨路線圖 P8 階段發布
- `新增` AutoJs6 檔案管理器中為 `.epub` 檔案提供 `閱讀 EPUB` 主按鈕與溢位選單動作 (外掛程式 ID `readium-epub-reader`, Explorer Action v2); 宿主回報為 `application/zip` 且副檔名為 `.epub` 的檔案同樣接受
- `新增` 閱讀器基線: EPUB 2 與 EPUB 3 書籍經 Readium 導覽器算繪, 提供目錄與需確認的外部連結
- `新增` 閱讀位置記憶: 每本書的最後位置按內容指紋儲存 (開啟時用快速鍵, 隨後遷移到全檔案 SHA-256), 下次開啟自動恢復; `從頭開始` 可清除
- `新增` 閱讀器介面: 工具列顯示書名與目前章節, 進度列顯示合成位置與百分比, 點按中央切換沉浸模式, 點按區與音量鍵翻頁, 可切換捲動模式
- `新增` 閱讀偏好面板: 字級, 字型族, 行距, 頁面邊距, 段落間距, 對齊, 連字號, 出版商樣式, 欄數與分頁 / 捲動配置即時生效並對所有書籍記憶; 淺色, 護眼, 深色三套主題加 `跟隨宿主`, 工具列與系統列配色隨主題變化
- `新增` 字型匯入: 經系統文件選擇器選取的 TTF / OTF 檔案先校驗 (SFNT 簽名, `name` 表, 單個 20 MB, 最多 10 個), 再私有存放於 `files/fonts/<sha256>` 並以 `@font-face` 宣告提供給 Readium 導航器; 匯入的字型出現在偏好面板的內建字型之後, 可在面板中刪除
- `新增` 書籍透過宿主授予的檔案描述元按位置就地讀取, 不複製也不解壓縮到儲存空間
- `新增` 介面, 說明, README 與更新日誌提供 10 種語言
- `修復` AGP 9.1 建置時的 SDK XML v4 解析警告及 JVM 單元測試組裝工作誤觸發 APK 原生程式庫對齊檢查的問題 (共用建置外掛 1.8.3)
- `相依性` 附加 Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)

##### 更多發行歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

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

### 相關連結

******

- AutoJs6 文件: https://docs.autojs6.com
- EPUB 3.3 規範: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md)
