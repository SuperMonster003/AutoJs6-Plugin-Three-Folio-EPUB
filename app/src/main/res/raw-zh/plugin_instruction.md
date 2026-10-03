# AutoJs6 3-Folio EPUB

在 AutoJs6 文件管理器中使用 3-Folio EPUB:

1. 安装并启用 `3-Folio EPUB` 插件.
2. 点按 `.epub` 文件, 或打开其溢出菜单并选择 `阅读 EPUB`.
3. 书籍将在基于 Readium Kotlin Toolkit 的阅读器中打开.

点按页面左右三分之一或按音量键翻页; 点按中央隐藏或显示工具栏. 阅读位置按书保存并在下次打开时恢复; 在溢出菜单中选择 `从头开始` 可清除.

插件通过 content URI 获得所选文件及其父目录的临时只读授权, 不会拿到原始文件系统路径, 不会把书籍复制到存储, 而是直接通过授权的文件描述符读取 EPUB 容器.

2.0.0 是当前版本, 1.0.0 是首个正式版本. 阅读器打开 EPUB 2 与 EPUB 3 书籍并提供目录, 记住每本书的阅读位置, 提供滚动模式, 点按区, 音量键翻页与沉浸模式, 偏好面板 (字号, 字体, 间距, 对齐, 列数与可跟随宿主夜间模式的主题), 导入的 TTF / OTF 字体, CJK 竖排与从右到左的书籍, 以单页或双页显示的固定版式书籍, 全文搜索, 书签, 书内链接, 注释与图片, 以及使用系统文字转语音引擎的朗读. 应用图标打开带最近书籍与系统文档选择器的启动器, 其它应用可通过 `ACTION_VIEW` 交来 EPUB, 设置页涵盖阅读器默认值, 设备上保存的数据与手动更新检查. 背后的 `org.autojs.plugin.EPUB` 服务向 AutoJs6 宿主答复元数据, 目录, 文本, 资源与搜索, 并打开由宿主驱动的阅读器会话 (`epub.open(path)`, `epub.read(path)`, 示例见 `示例 > 电子书`).

更名为 3-Folio EPUB, 新安装包名为 io.github.supermonster003.autojs6.plugin.three.folio.epub. Android 将其视为独立应用, 原 Readium EPUB Reader 的设置, 最近书籍与批注不会自动迁移. 需要 AutoJs6 5318 或更高版本

书籍内可能包含脚本与远程资源; 插件保留 Readium 的默认行为, 不做拦截, 包括明文 `http://` 资源. 请只打开可信任的书籍.

Explorer Action v2 同时支持单文件的主按钮与溢出菜单. 需要 AutoJs6 构建 5318 或更高版本.

感谢 [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit), [Readium CSS](https://github.com/readium/readium-css) 及[起始工程参考项目](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/THIRD_PARTY_NOTICES.md#initial-project-references)的开发者. 署名或权利相关问题可参阅[权利与配合说明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/blob/master/RIGHTS_AND_TAKEDOWN.md).
