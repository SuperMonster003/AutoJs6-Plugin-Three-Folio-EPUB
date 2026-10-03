# AutoJs6 3-Folio EPUB

在 AutoJs6 檔案管理器中使用 3-Folio EPUB:

1. 安裝並啟用 `3-Folio EPUB` 外掛程式.
2. 點按 `.epub` 檔案, 或開啟其溢出選單並選擇 `閱讀 EPUB`.
3. 書籍將在基於 Readium Kotlin Toolkit 的閱讀器中開啟.

點按頁面左右三分之一或按音量鍵翻頁; 點按中央隱藏或顯示工具列. 閱讀位置按書儲存並在下次開啟時恢復; 在溢出選單中選擇 `從頭開始` 可清除.

外掛程式透過 content URI 取得所選檔案及其父目錄的臨時唯讀授權, 不會取得原始檔案系統路徑, 不會把書籍複製到儲存空間, 而是直接透過授權的檔案描述符讀取 EPUB 容器.

2.0.0 是目前版本, 1.0.0 是首個正式版本. 閱讀器開啟 EPUB 2 與 EPUB 3 書籍並提供目錄, 記住每本書的閱讀位置, 提供捲動模式, 點按區, 音量鍵翻頁與沉浸模式, 偏好面板 (字號, 字型, 間距, 對齊, 欄數與可跟隨宿主夜間模式的主題), 匯入的 TTF / OTF 字型, CJK 直排與從右到左的書籍, 以單頁或雙頁顯示的固定版式書籍, 全文搜尋, 書籤, 書內連結, 註釋與圖片, 以及使用系統文字轉語音引擎的朗讀. 應用程式圖示開啟帶最近書籍與系統文件選擇器的啟動器, 其它應用程式可透過 `ACTION_VIEW` 交來 EPUB, 設定頁涵蓋閱讀器預設值, 裝置上儲存的資料與手動更新檢查. 背後的 `org.autojs.plugin.EPUB` 服務向 AutoJs6 宿主答覆中繼資料, 目錄, 文字, 資源與搜尋, 並開啟由宿主驅動的閱讀器工作階段 (`epub.open(path)`, `epub.read(path)`, 範例見 `範例 > 電子書`).

更名為 3-Folio EPUB, 新安裝套件名稱為 io.github.supermonster003.autojs6.plugin.three.folio.epub. Android 將其視為獨立應用程式, 原 Readium EPUB Reader 的設定, 最近書籍與註記不會自動遷移. 需要 AutoJs6 5318 或更新版本

書籍內可能包含指令碼與遠端資源; 外掛程式保留 Readium 的預設行為, 不做攔截, 包括明文 `http://` 資源. 請只開啟可信任的書籍.

Explorer Action v2 同時支援單檔案的主按鈕與溢出選單. 需要 AutoJs6 組建 5318 或更新版本.

感謝 [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit), [Readium CSS](https://github.com/readium/readium-css) 及[起始工程參考項目](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/THIRD_PARTY_NOTICES.md#initial-project-references)的開發者. 署名或權利相關問題可參閱[權利與配合說明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/RIGHTS_AND_TAKEDOWN.md).
