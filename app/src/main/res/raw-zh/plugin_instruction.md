在 AutoJs6 文件管理器中使用 Readium EPUB Reader:

1. 安装并启用 `Readium EPUB Reader` 插件.
2. 点按 `.epub` 文件, 或打开其溢出菜单并选择 `阅读 EPUB`.
3. 书籍将在基于 Readium Kotlin Toolkit 的阅读器中打开.

插件通过 content URI 获得所选文件及其父目录的临时只读授权, 不会拿到原始文件系统路径, 不会把书籍复制到存储, 而是直接通过授权的文件描述符读取 EPUB 容器.

当前阶段: 阅读器以 Readium 默认设置显示书籍并提供目录. 阅读进度, 书签, 偏好设置, 搜索, 朗读, 固定版式, 字体导入, 独立启动入口与 `epub` 脚本 API 在 ROADMAP.md 中排期, 将在后续版本提供.

书籍内可能包含脚本与远程资源; 插件保留 Readium 的默认行为, 不做拦截, 包括明文 `http://` 资源. 请只打开可信任的书籍.

Explorer Action v2 同时支持单文件的主按钮与溢出菜单. 需要 AutoJs6 构建 5269 或更高版本.
