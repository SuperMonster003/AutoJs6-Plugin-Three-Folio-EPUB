******

### 发行历史

******

# v1.0.0

###### 2026/09/19

* `提示` 开发构建: 路线图 P0 阶段 (骨架, Readium 验证, 测试样本) 进行中; 首个公开版本随路线图 P8 阶段发布
* `新增` AutoJs6 文件管理器中为 `.epub` 文件提供 `阅读 EPUB` 主按钮与溢出菜单动作 (插件 ID `readium-epub-reader`, Explorer Action v2)
* `新增` 阅读器基线: EPUB 2 与 EPUB 3 书籍经 Readium 导航器渲染, 提供目录与需确认的外部链接
* `新增` 书籍通过宿主授予的文件描述符按位置就地读取, 不复制也不解压到存储
* `新增` 界面, 说明, README 与更新日志提供 10 种语言
* `依赖` 附加 Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
