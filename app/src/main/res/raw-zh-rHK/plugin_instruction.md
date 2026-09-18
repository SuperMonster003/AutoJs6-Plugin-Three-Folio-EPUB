在 AutoJs6 檔案管理器中使用 Readium EPUB Reader:

1. 安裝並啟用 `Readium EPUB Reader` 外掛程式.
2. 點按 `.epub` 檔案, 或開啟其溢出選單並選擇 `閱讀 EPUB`.
3. 書籍將在基於 Readium Kotlin Toolkit 的閱讀器中開啟.

外掛程式透過 content URI 取得所選檔案及其父目錄的臨時唯讀授權, 不會取得原始檔案系統路徑, 不會把書籍複製到儲存空間, 而是直接透過授權的檔案描述符讀取 EPUB 容器.

目前階段: 閱讀器以 Readium 預設設定顯示書籍並提供目錄. 閱讀進度, 書籤, 偏好設定, 搜尋, 朗讀, 固定版式, 字型匯入, 獨立啟動入口與 `epub` 指令碼 API 在 ROADMAP.md 中排期, 將在後續版本提供.

書籍內可能包含指令碼與遠端資源; 外掛程式保留 Readium 的預設行為, 不做攔截, 包括明文 `http://` 資源. 請只開啟可信任的書籍.

Explorer Action v2 同時支援單檔案的主按鈕與溢出選單. 需要 AutoJs6 組建 5269 或更新版本.
