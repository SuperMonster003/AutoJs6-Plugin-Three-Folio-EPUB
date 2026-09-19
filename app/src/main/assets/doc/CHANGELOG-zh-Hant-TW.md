******

### 發行歷史

******

# v1.0.0

###### 2026/09/19

* `提示` 開發組建: 路線圖 P0 階段 (骨架, Readium 驗證, 測試樣本) 進行中; 首個公開版本隨路線圖 P8 階段發布
* `新增` AutoJs6 檔案管理器中為 `.epub` 檔案提供 `閱讀 EPUB` 主按鈕與溢位選單動作 (外掛程式 ID `readium-epub-reader`, Explorer Action v2); 宿主回報為 `application/zip` 且副檔名為 `.epub` 的檔案同樣接受
* `新增` 閱讀器基線: EPUB 2 與 EPUB 3 書籍經 Readium 導覽器算繪, 提供目錄與需確認的外部連結
* `新增` 書籍透過宿主授予的檔案描述元按位置就地讀取, 不複製也不解壓縮到儲存空間
* `新增` 介面, 說明, README 與更新日誌提供 10 種語言
* `相依性` 附加 Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
