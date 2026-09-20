# AutoJs6 Readium EPUB Reader 插件 Roadmap

本文是 `AutoJs6-Plugin-Readium-EPUB-Reader` (基础电子书阅读器: 文件管理器与独立入口打开 EPUB, 脚本侧全局对象 `epub` 提供只读提取与阅读器控制) 的可执行状态表.
以 2026-09-18 的宿主本地代码快照 (`AutoJs6 master@1db2d9b87`, `VERSION_NAME=6.8.0`, `VERSION_BUILD=5282`),
Readium Kotlin Toolkit `3.4.0` (2026-09-11, BSD-3-Clause, minSdk 24), 平台版本插件 `1.8.2` 为起点, 每个条目均可独立 Check 并落地, 后续会话按阶段逐步推进.

使用方式:

1. 每次会话开始时, 从 "阶段总览" 选取一个或多个未完成条目, 优先级按阶段顺序; 单次会话可完成多个小节, 除非单个小节已足够繁杂.
2. 条目完成后勾选 `[x]`, 并在条目后追加证据 (提交 hash / 测试类名 / 设备型号与 API / 样本文件名), 证据等级见附录 E.
3. 条目前缀标明主要落点: `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(索引)` `D:/idea-projects/AutoJs6-Official-Plugins-Index`, `(文档)` 文档 / d.ts / Ace / 离线文档四个关联仓库, `(测试)`, `(发布)`.
4. 涉及宿主公开契约或脚本 API 的条目, 完成后必须同步宿主 `docs/dev/`, 宿主 `.changelog` (10 语言) 与本仓库 `.changelog`.
5. 附录 D 的 "待决事项" 在进入对应阶段前由维护者拍板, 拍板结果回填到 "固定决策" (Q1-Q11 已于 2026-09-18 拍板, 回填为 D23-D33).
6. 仓库骨架 (Gradle / Manifest / 资源 / CI) 与 `git init` 已于 2026-09-19 在 P0.1 按 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成, 该文件的裁剪版即本仓库 `AGENTS.md`; 工程约定以 `AGENTS.md` 为准, 本文件只记录 "改什么" 与证据.

---

## 1. 固定决策

以下决策 D1-D8 已由维护者于 2026-09-18 确认 (两轮选择题), 后续阶段不再重新讨论; D9-D22 为据此派生的技术决策, 进入对应阶段前可推翻 (推翻点见附录 D), 之后视同固定; D23-D33 是附录 D 全部 11 个待决事项的拍板结果 (2026-09-18), 视同固定.

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D1 | 命名 | 仓库 `AutoJs6-Plugin-Readium-EPUB-Reader`; `rootProject.name=autojs6-plugin-readium-epub-reader`; 标题 `Readium EPUB Reader` (英文, 不可翻译); `applicationId=io.github.supermonster003.autojs6.plugin.readium.epub.reader`; 插件 ID `readium-epub-reader`; variant `default`; Explorer Action 动作 ID `readium-epub-reader.primary` (主动作) 与 `readium-epub-reader` (溢出菜单); 脚本全局对象 `epub` (别名 `$epub`). 名称同时点明引擎与格式, 未来若接入 Readium 的其它格式 (附录 F) 不改仓库名, 只扩展描述. |
| D2 | 渲染引擎 | Readium Kotlin Toolkit 3.4.0 完整栈: `readium-shared` + `readium-streamer` (解析) + `readium-navigator` (`EpubNavigatorFragment`, View + WebView + Readium CSS) + `readium-navigator-media-tts` (朗读). 不自研分页 / 定位 / 搜索 / 高亮. P0.2 spike 验证依赖树, Kotlin 元数据, R8 体积与随机访问方案; 不成立时退回附录 E.2 (Readium 解析 + 自研 WebView 导航器). |
| D3 | 入口与脚本 API | 三层全部纳入 1.0.0: (a) 文件管理器 Explorer Action 主动作 + 溢出菜单 (与 HTML / Markdown Previewer 同形); (b) 脚本只读提取 API (metadata / 目录 / 阅读顺序 / 章节纯文本 / 封面 / 资源导出 / 全文搜索); (c) 脚本打开阅读器并控制 (定位 / 翻页 / 偏好 / 进度与书签事件). (b) 与 (c) 需要宿主新增契约模块 `plugin-api/epub-api`, 宿主客户端 `core/plugin/epub/` 与 augment `epub`. |
| D4 | 持久化 | 1.0.0 按每本书记录最后阅读位置 (Readium `Locator` JSON) 与书签列表; 高亮与笔记推迟到 1.1.0 (P9, Room). 数据存于插件私有目录, 键为文件内容指纹, 不落盘明文路径 (与 3-Ember Player 进度记忆同一红线); 独立入口的 "最近书籍" 只保留用户经系统文档选择器明确授予的持久读取权. |
| D5 | 1.0.0 功能全集 | 基线 (目录导航, 进度条与位置记忆, 分页 / 滚动切换, 字号 / 字体 / 行距 / 边距 / 主题, 点按区与音量键翻页, 书签, 跟随宿主语言与暗色, RTL) + 全文搜索 + TTS 朗读 + 固定版式 FXL + 自定义字体导入与 CJK 竖排. 四项扩展全部进入 1.0.0. |
| D6 | 书内脚本与网络 | 全部允许: 保留 Readium 默认行为, 不剥离 EPUB 资源内的 `<script>` 与事件属性, 不拦截远程资源. 插件仍不向 WebView 注入 Readium 之外的 JavaScript 接口, 不开放 `file://` 访问, 不申请存储权限; 明文 HTTP 按 D31 放行. "安全模式" 开关记入附录 F, 不排期. |
| D7 | 格式范围 | 仅 EPUB: EPUB 2 (NCX) 与 EPUB 3 (NAV), 可重排与固定版式. CBZ / 有声书 / PDF / LCP 只在附录 F 预留接入点, 不排期; 插件保持纯 JVM, 无 ABI 拆分, `expectNoNativeLibraries=true`. |
| D8 | 界面形态 | 独立应用形态: Launcher 入口 (最近书籍 + 系统文档选择器打开) + `ACTION_VIEW application/epub+zip` 入口 (可被其它应用调用) + 独立设置页 (默认阅读偏好, 数据管理, 关于, 发行历史, 更新检查) + 阅读器内偏好面板. Wake Activity 与 Explorer Action 入口照常存在, 独立入口不能替代它们. |
| D9 | Explorer Action 协议 v2 | 目录声明协议 v2, 最低宿主 5269, 双 URI (目标 + 父目录) 只读信封, 主动作 + 溢出菜单, 与 HTML Previewer 的审计口径 (`docs/explorer-action-compatibility.md`, `gradle/explorer-action-compatibility.properties`, AAR SHA-256 门禁) 完全同形. EPUB 自包含, 不需要 v4+ host session / `readSiblings` / `relatedFileSuffixes`; 父目录 URI 只做校验不做访问. 宿主当前协议 v22 (`ExplorerActionProtocol.kt:4-11`) 仍接受 v2 目录. |
| D10 | 双 Binder 服务与身份 | 插件导出两个契约服务: `ExplorerActionService` (`org.autojs.plugin.EXPLORER_ACTION`, `PluginInfo.engine` 必须为 `explorer-action`, 宿主 `ExplorerActionRegistry.kt:388` 硬性检查) 与 `ReadiumEpubReaderPluginService` (`org.autojs.plugin.EPUB`, category `epub`, `PluginInfo.engine=epub`). 两者 `id` / `variant` / 版本字段一致, 只有 `engine` 与 `capabilities` 不同. 插件中心 `InstalledPluginRepository` 按包名分组 (行 127 / 461), P5.1 必须验证分组后的 engine 取值与 `SERVICE_ACTION_BY_ENGINE` 路由; 若冲突, 以 Explorer Action 身份为插件中心展示身份, `epub` 服务由 `EpubPluginHost` 按 action + category + 包名直接发现. 官方索引只读一个 `plugin_engine` resValue, 固定为 `explorer-action` (与 3-Ember Player 一致); `epub` 服务的 engine 取自契约常量 `EpubIds.ENGINE`, 不再另设 resValue. |
| D11 | 传书方式 | 一律以只读 `ParcelFileDescriptor` 或 `content://` URI 传书, 永不复制整本书到缓存. 插件实现 `PfdResource : Resource` (FileChannel 定位读, `length()` 取 `statSize`) 喂给 Readium `AssetRetriever` / `StreamingZipArchiveProvider` (`readium/shared/.../util/zip/StreamingZipContainer.kt`, `ReadableChannelAdapter.kt`); Explorer / ACTION_VIEW / 脚本三条路径都归一到 PFD. 退路: Readium 内建的 content URI 资源工厂. P0.2 验证. |
| D12 | 阅读器会话与脚本启动 | 脚本 `epub.read()` 由宿主两步完成: 先经 Binder `openReader(pfd, options, callback)` 取得会话与随机 token, 再由宿主自己 `startActivity` 启动插件 `EpubReaderActivity` (action `org.autojs.plugin.EPUB_READER_OPEN`, 受 `org.autojs.permission.PLUGIN` 保护, extra 只含 token). 插件服务不自行从后台启动 Activity (Android 10+ 后台启动限制). 同一宿主同时只保留一个脚本阅读会话, 新会话取代旧会话并向旧会话发 `close(reason=replaced)`. |
| D13 | 数据模型 | `files/books/<指纹>/progress.json` 与 `bookmarks.json`, `AtomicFile` 原子写, 每本书书签上限 500, 书目上限 500 (LRU 淘汰); 指纹算法见 D23 (全文件 SHA-256, 打开期间以临时键过渡). 最近书籍列表 (P4) 另存持久 URI + 显示名 + 指纹 + 封面缩略图, 只来自 Launcher 的文档选择器授权. |
| D14 | 偏好模型 | 直接序列化 Readium `EpubPreferences` (JSON), 全局一份默认值 + 阅读器内即时修改; 1.0.0 不做按书覆盖. 主题固定三档 (浅色 / 护眼 / 深色) 加 "跟随宿主"; 自定义字体经 Readium `fontFamilyDeclarations` + 插件私有目录托管. |
| D15 | TTS 形态 | `readium-navigator-media-tts` + 系统 `TextToSpeech`; 朗读期间以 `mediaPlayback` 前台服务 + MediaSession 通知承载, 熄屏可继续, 退出阅读器即停止. 新增权限 `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` (仅开启朗读时请求) 按 3-Ember Player M9 的豁免格式记入 `AGENTS.md` 红线小节. |
| D16 | 事件桥 | 阅读器事件 (`open` / `progress` / `bookmark` / `close` / `error`) 经 `IEpubReaderCallback.onEvent(generation, seq, Bundle)` 单向回调, 宿主 `EpubReaderBridge` 校验 generation / seq 后经 `ScriptAsyncDispatcher` 单跳投递到脚本 `EventEmitter`; 进度事件节流 (默认 500 ms 合并). 形态与 Angus Mail `MailWatchBridge` / Three-Stone-AI `AiStream` 一致. |
| D17 | 版本 | 1.0.0 在 P8 发布, 包含 P0-P7 全部内容; P9 (高亮 / 笔记) 为 1.1.0. `VERSION_BUILD` 与提交数一致的规则见 `AGENTS.md`. |
| D18 | 上游版本固定 | Readium 固定 `3.4.0`, 记入 `gradle/libs.versions.toml` 与 `THIRD_PARTY_NOTICES.md`; 升级需重跑 P0.2 的完整清单 (元数据 / 体积 / 原生库 / 随机访问 / 样本矩阵). 上游以 Kotlin 2.4.20 编译, 消费方编译器需不低于 2.3 (平台版本插件 1.8.2 = Kotlin 2.3.20, Angus Mail P0 已用同一平台构建通过). |
| D19 | 权限集合 | `INTERNET` (D6 远程资源 + 更新检查), `org.autojs.permission.PLUGIN`, D15 的三项前台服务 / 通知权限; 不申请任何存储 / 媒体 / 无障碍 / 悬浮窗权限, `allowBackup=false`. 文件访问只通过宿主授权的 `content://`, 其它应用的 `ACTION_VIEW` 授权与文档选择器授权. |
| D20 | 宿主退化 | 插件未安装 / 未启用 / 未授权 / 版本过低时: 文件管理器复用 `ExplorerDocumentPreviewerPluginUi` 的引导对话框 (安装 / 激活 / 用其它应用打开), 脚本 API 抛 `EpubError` (`PLUGIN_UNAVAILABLE` 家族, 文案来自 `AidlPluginHost.buildSelectionFailure`). 宿主不保留任何 EPUB 解析或渲染实现. |
| D21 | 宿主 EPUB 类型行 | 宿主 `FileUtils.TYPE` 新增 `EPUB("epub", TypeDataHolder.EPUB_READER)`, `TypeDataHolder.EPUB_READER` 的 identity 仅含 `IDENTITY_PREVIEWABLE` (二进制, 不可文本编辑), `PreviewerType` 新增 `EPUB`. 图标沿用 Unicode 字符方案且码点不超过 U+FFFF (`FileUtils.kt:302-329` 说明). |
| D22 | 测试样本 | `docs/fixtures/` 只放许可证明确的样本: 自生成的最小 EPUB 2 / EPUB 3 / 损坏样本 (脚本 `.python/generate_fixtures.py`; `.gitignore` 忽略 `/tools/*`, 生成器不能放在 `tools/`), IDPF `epub3-samples` 中公有领域或 CC 许可的可重排 / FXL / 日文竖排 / 阿拉伯语 RTL 样本, W3C `epub-tests` 用例; 每个样本在 `docs/fixtures/README.md` 记录来源, 许可与 SHA-256. 大体积性能样本 (20 MB / 200 MB) 由脚本合成, 不入库. |
| D23 | 书籍指纹 (Q1=b) | 正式指纹为全文件 SHA-256. 打开书籍时先以临时键 (文件大小 + 首 1 MiB + 末 64 KiB 的 SHA-256) 读取进度并开始阅读, 后台完成全量哈希后把 `files/books/<临时键>/` 迁移到 `files/books/<正式指纹>/` (目标已存在时合并, 进度取时间戳较新者, 书签去重); 临时键目录只在迁移期间存在. 全量哈希在 IO 线程分块计算, 200 MB 样本的耗时记入 P1.3 证据. |
| D24 | 归档内 EPUB (Q2=b) | 宿主 `MAX_ARCHIVE_DOCUMENT_BYTES` 改为按 `PreviewerType` 取值: EPUB 256 MiB, MARKDOWN / HTML 保持 8 MiB; 超限时沿用 "先解压" 提示. 宿主改动记入附录 C. |
| D25 | 外部链接 (Q3=c) | 阅读器设置项 "外部链接": "确认后打开" (默认, 对话框显示完整 URL) / "直接打开浏览器". 只放行 `http` / `https`, 其它 scheme (含 `intent:` / `file:` / `javascript:`) 一律拒绝并提示. |
| D26 | 后台朗读 (Q4=b) | 设置项 "后台继续朗读", 默认关. 关闭时退出阅读器即停止 (D15 基线); 开启时阅读器 Activity 销毁后 TTS 前台服务继续朗读到书末或定时器结束, 通知栏可暂停 / 停止, 重新打开同一本书时衔接当前朗读位置. |
| D27 | `ACTION_VIEW` MIME (Q5=a) | 只声明 `application/epub+zip` (含 `content` 与 `file` scheme 的 data 过滤器由 P4.2 决定, 默认只 `content`); 不追加 `application/octet-stream` + `pathPattern` 兜底. |
| D28 | 更新检查 (Q6=a) | 仅手动: 设置页 "检查更新" 按钮触发一次 GitHub Releases 请求; 不做定时 / 启动时自动检查, 不做后台网络请求. |
| D29 | 默认启用 (Q7=a) | 插件在宿主插件中心默认启用 (`PluginDefaultEnabledPolicy` 按 Explorer Action 家族口径), 与 HTML / Markdown Previewer 一致. |
| D30 | `epub.text()` 形态 (Q8=c) | `epub.text(target, { format })` 提供 `format: 'text' \| 'markdown'`, 默认 `'text'`. `text` 为纯文本 (段落 `\n` 分隔, 标题不加标记); `markdown` 为轻量转换 (标题 `#`, 列表 `-`, 强调 `*` / `**`, 链接 `[]()`, 代码块围栏), 转换在插件侧完成, 宿主只透传. |
| D31 | 明文 HTTP (Q9=b) | `android:usesCleartextTraffic="true"`, 与 D6 "全部允许" 一致; README 安全章节与插件说明如实写明书内 `http://` 资源会被加载. 更新检查 (D28) 仍只走 HTTPS. |
| D32 | 脚本退出时阅读器去留 (Q10=b) | 脚本退出 (或 `session.close()`) 只关闭会话: token 失效, 事件停止, 阅读器 Activity 保留为普通阅读并继续记录进度. `session.close({ finish: true })` 可显式结束 Activity. |
| D33 | P9 排期 (Q11=a) | 高亮 / 笔记 (P9) 排期 1.1.0, 1.0.0 发布 (P8) 后立即进入; 数据模型 (Room) 与导出格式在 P9.1 前不预埋到 1.0.0 的 JSON 文件. |

由 D2 / D6 / D11 派生的硬约束:

- 插件不复制宿主 `PluginInfo` 或 AIDL 伪实现; `common-plugin-api` / `explorer-action-api` / `epub-api` 三个 AAR 复制到本仓库 `libs/` 并以 SHA-256 锁定 (`locks/host-api-aars.lock`, MCP Server 形态); `epub-api` 在宿主发布前可先以受控源码模块临时引入, 发布前切换为锁定 AAR.
- 已发布 AIDL 演进只在末尾追加方法并通过 `CONTRACT_VERSION` 协商; 破坏性变更同步升级宿主与插件.
- 任何路径都不把 EPUB 解压到磁盘; 需要落盘的只有用户显式导出的资源 / 封面 (脚本 API, 写到脚本指定路径, 由宿主执行写入) 与用户导入的字体.
- 纯逻辑 (Intent 策略, 指纹, 偏好编解码, 上限, 目录树扁平化, 文本分块, 错误映射, 版本比较) 保持 Android-free, 由 JUnit4 覆盖.

---

## 2. 范围与非目标

范围内:

- 本仓库: 插件 APK (Explorer Action 服务与 Activity, `epub` 能力 Binder 服务, 阅读器 Activity 与偏好面板, TTS 前台服务, Launcher / ACTION_VIEW 入口, 设置页 / 发行历史 / 更新检查, 数据存储, 10 语言资源, README / changelog 生成, JVM 与 instrumentation 测试, CI).
- 宿主 `D:/idea-projects/AutoJs6`: `PreviewerType.EPUB` 与 `.epub` 类型行, 文件管理器引导对话框, `plugin-api/epub-api` 契约模块, `core/plugin/epub/` 宿主客户端, `runtime/api/augment/epub/` 脚本 API, 插件中心注册, `docs/dev/epub-plugin-protocol-v1.md`, changelog, 示例脚本.
- 索引仓库: `plugins.official.generated.json` 条目与 `release-manifests/` 准入清单.
- 关联仓库: `AutoJs6-Documentation` (`api/epub.md`), `AutoJs6-TypeScript-Declarations` (`aj6-int-epub.d.ts`), `AutoJs6-Plugin-Ace-Editor` (内置声明再生成), `AutoJs6-Plugin-Offline-Docs` (离线文档同步).

范围内但在 1.0.0 之后交付:

- 高亮, 笔记与导出 (P9, 目标 1.1.0).

非目标 (本 Roadmap 不处理):

- CBZ / 漫画, 有声书, PDF, LCP / Adobe DRM, OPDS 在线书库, 云同步, 词典 / 翻译服务集成 (附录 F 只记录接入点).
- EPUB 编辑, 转换 (MOBI / AZW / TXT 互转), 元数据写回.
- 宿主内置阅读器 UI; 宿主只提供入口, 引导, 契约与脚本 API.
- 对 `app/src/main/java/com/stardust/**` 兼容包的任何改动.

---

## 3. 现状诊断

以下是 2026-09-18 探查得到的事实, 是各阶段条目的直接依据. 行号以宿主快照 `1db2d9b87` 为准.

### 3.1 可直接复用的宿主与兄弟仓库能力

| 事实 | 锚点 |
| --- | --- |
| Explorer Action 协议: `VERSION = 22`, `MIN_SUPPORTED_VERSION = 1`; 服务 action `org.autojs.plugin.EXPLORER_ACTION`, Activity action `org.autojs.plugin.EXPLORER_ACTION_EXECUTE`; 目录 key / 值常量 (`PLACEMENT_PRIMARY = 2`, `ACCESS_READ_ONLY`, `TARGET_FILE`, `MIME_TYPES`, `EXTENSIONS`); 启动器按目录声明版本分支, v2 仍发两项 `ClipData` (目标 + 父目录) 的 legacy envelope | `plugin-api/explorer-action-api/.../ExplorerActionProtocol.kt:4-11`, `ExplorerActionPluginActions.kt:4-5`, `ExplorerActionCatalogKeys.kt`, `ExplorerActionValues.kt`, `core/plugin/explorer/ExplorerActionLauncher.kt:136-180` |
| 宿主对 Explorer Action 插件的 engine 硬性检查与可用性策略 (协议区间, `requiresHostVersion`, Activity 必须 exported + permission) | `ExplorerActionRegistry.kt:388`, `ExplorerActionAvailabilityPolicy.kt:9-45`, `center/PluginCapabilityResolver.kt:12-41` |
| 文档预览器的宿主接入模板 (HTML / Markdown): `PreviewerType` 枚举, `TypeDataHolder.*_PREVIEWER`, `TYPE` 类型行, `isPreviewable()` 驱动主动作, `ExplorerDocumentPreviewerPluginUi.specFor` 的包名 / MIME / 名称, 未安装 / 未启用 / 未授权 / 不可用四态引导, 归档内文档 8 MiB 上限 | `util/FileUtils.kt:313-315, 330-331, 844-874, 3161, 3182, 3633-3648`, `ui/explorer/ExplorerPrimaryAction.kt:15-22`, `ExplorerPluginActionController.kt:89-100, 142-144`, `ExplorerDocumentPreviewerPluginUi.kt:94-106, 148-176`, `ExplorerArchiveCoordinator.kt:136-139`, `res/values/strings_donottranslate.xml:327-328`, `test/.../DocumentPreviewerFileTypeTest.kt` |
| 宿主已有 MIME 常量 `APPLICATION_EPUB_ZIP = "application/epub+zip"`, 全仓库唯一的 EPUB 痕迹; 无 `.epub` 类型行, 无 `PreviewerType.EPUB` | `runtime/api/Mime.kt:944` |
| 宿主 Manifest `<queries>` 已按 action 声明 `EXPLORER_ACTION` / `EXPLORER_ACTION_EXECUTE` / `INFO`, 两个 Previewer 均未按包名列出; ProGuard 无插件包名规则 | `app/src/main/AndroidManifest.xml:37-44, 70-75`, `app/proguard-rules.pro:55, 105` |
| 单能力插件的宿主链路模板: 契约模块 (`IXxxPlugin.aidl` 首方法 `PluginInfo getInfo()`, `XxxIds` / `XxxActions` / `XxxContract` / capability keys), `AidlPluginHost` (发现 / 授权 / 版本 / 专用绑定租约 / 死亡通知), 插件中心四处注册, 默认启用策略, 统一错误文案 | `plugin-api/opencc-api/`, `core/plugin/AidlPluginHost.kt:135-156, 270-372, 446-601`, `core/plugin/ServiceBindingLease.kt`, `center/InstalledPluginRepository.kt:127, 153-223, 461`, `center/PluginCenterViewModel.kt:1035-1055`, `center/PluginCenterFragment.kt:985-1015`, `center/PluginDefaultEnabledPolicy.kt` |
| 会话型契约与事件桥的成熟先例: Angus Mail (`IMailSession` / `IMailWatchCallback.onEvent(generation, seq, Bundle)`, `MailWatchBridge`, `MailSessionClient` 租约与死亡处理), MCP Server (`IMcpServerSession` / `IMcpServerCallback`), AI 流 (`AiStream : EventEmitter` 经 `ScriptAsyncDispatcher.dispatchValues` 单跳回脚本线程) | `plugin-api/mail-api/`, `core/plugin/mail/`, `core/plugin/mcp/McpServerSessionController.kt`, `augment/ai/AiStream.kt:16-123`, `augment/ScriptAsyncDispatcher.kt` |
| 脚本全局对象定义方式 (`Augmentable` + `AugmentableKey`, `selfAssignmentFunctions`, `@RhinoSingletonFunctionInterface` / `@RhinoRuntimeFunctionInterface`, `runBlocking` 同步桥接, `ScriptPromiseAdapter.toJsPromise` 的 Async 版本), 注册于 `ScriptRuntime` | `augment/opencc/OpenCC.kt:18-22, 58-80`, `augment/PromiseInterop.kt:7-16`, `runtime/ScriptRuntime.kt:1031-1039` |
| 宿主向插件提供只读 PFD 的模板 (`MediainfoPluginHost`), 内存到 PFD 管道 (`BarcodePluginHost.createPipe`), 插件输出流式落盘 (`ArchiveEntryMaterializer`) | `core/plugin/mediainfo/MediainfoPluginHost.kt:157-159`, `core/plugin/barcode/BarcodePluginHost.kt:393-412`, `core/plugin/explorer/archive/ArchiveEntryMaterializer.kt:40-85` |
| 现有脚本级文件查看只是通用 `ACTION_VIEW` 选择器 (`app.viewFile` / `IntentUtils.viewFile`), 不经 Explorer Action 注册表; 没有任何 "reader" / "viewer" 脚本模块 | `augment/app/App.kt:824-839`, `runtime/api/AppUtils.kt:165-171`, `util/IntentUtils.kt:170-222` |
| 官方插件只读设置快照 (语言 / 夜间模式 / 主题色), 供插件跟随宿主外观; Previewer 侧实现 `PreviewerHostActivity` (`attachBaseContext` 包装 locale 与 uiMode, `onResume` 变化时 `recreate`) | `docs/dev/official-plugin-settings-contract-v1.md:21`, `AutoJs6-Plugin-HTML-Previewer/.../PreviewerHostActivity.kt` |
| Previewer 仓库可直接复制的骨架与门禁: 平台版本插件 + native-alignment (`expectNoNativeLibraries`), `gradle/explorer-action-compatibility.properties` + `verifyExplorerActionApiCompatibility` (AAR SHA-256), `HtmlPreviewerIntentPolicy` (v2 信封逐字段校验), `HtmlPreviewerExplorerCompatibility` (检查点表), `docs/explorer-action-compatibility.md`, `.python/generate_markdown.py` + `tests/test_repository_contract.py`, CI 两段 (JVM + lint + APK; API 35 模拟器 instrumentation) | `AutoJs6-Plugin-HTML-Previewer/app/build.gradle.kts`, `gradle/explorer-action-compatibility.properties`, `docs/explorer-action-compatibility.md`, `.github/workflows/build.yml` |
| Markdown Previewer 的用户文件导入先例 (自定义 CSS: SAF 选择, 256 KiB 上限, `AtomicFile` 私有目录, 清除) 可直接改造为字体导入 | `AutoJs6-Plugin-Markdown-Previewer/.../MarkdownPreviewerPreferences.kt` |
| 3-Ember Player 的独立应用形态: `LauncherActivity`, `ExternalViewerActivity` (`ACTION_VIEW content:// + MIME`), `VideoRequestPolicy` (Explorer 边界与外部边界分离), `PlaybackPositionStore` (URI 摘要键, 不存路径), `SettingsActivity` / `AppUpdateCoordinator` / `ReleaseHistoryActivity`, `BackgroundPlaybackService` (mediaPlayback 前台服务 + 权限豁免记录) | `AutoJs6-Plugin-Three-Ember-Player/app/src/main/AndroidManifest.xml:47-147`, 同目录 `*.kt`, `Roadmap.md` 红线小节 |
| 官方索引生成器读取 `plugin_id` / `plugin_engine` / `plugin_variant` / `plugin_requires_host_version` resValue 与 manifest `requiresHostVersion` meta-data; 准入清单 `release-manifests/<package>/<versionCode>.json` | `AutoJs6-Official-Plugins-Index/README.md`, `tools/generate_official_plugin_index.py` |
| 宿主构建事实: `minSdk 24`, `compileSdk / targetSdk 37`, core library desugaring 开启, `core/plugin` 全面使用协程 | `version.properties`, `app/build.gradle.kts` |

### 3.2 缺口 (需要新建或修改)

| 缺口 | 处理阶段 |
| --- | --- |
| 宿主没有 EPUB 类型行, `PreviewerType` 只有 `MARKDOWN` / `HTML`, `specFor` 只有两个分支, `plugin_*_name` 字符串只有两个 Previewer | P1.4 |
| 没有 `plugin-api/epub-api`, 没有 `core/plugin/epub/`, 没有 `augment/epub/`; 全局名 `epub` / `$epub` 在宿主 augment, d.ts (`aj6-int-init.d.ts`) 与文档 `api/` 中均未被占用 | P5 / P6 |
| 现有 Explorer Action 插件都以 `explorer-action` 为唯一 engine; 没有 "同一包同时提供 Explorer Action 服务与独立能力服务" 的先例, 插件中心分组后的 engine 语义未验证 | P5.1 (D10) |
| 现有插件没有 "宿主以 token 启动插件 Activity 并绑定会话" 的先例 (3-Ember 的外部入口由 Intent 直接携带 URI) | P5.3 (D12) |
| 没有基于 `ParcelFileDescriptor` 的 Readium `Resource` 实现, 没有 EPUB 测试样本与许可证记录 | P0.2 / P0.3 |
| 没有 `docs/dev/epub-*.md`, 文档 / d.ts / Ace / 离线文档均无 `epub` | P5.5 / P6.3 / P8 |

### 3.3 外部事实

| 事实 | 依据 |
| --- | --- |
| Readium Kotlin Toolkit 最新稳定版 3.4.0 (2026-09-11), Maven Central 组 `org.readium.kotlin-toolkit`, 许可证 BSD-3-Clause; 3.4.0 起最低 Android API 24; 新增实验性 `TapEvent.targetElement` (点按命中的图片元素, 可做全屏图片查看); 修复 EPUB HREF 含未编码空格加 fragment / query 时 TOC 与 Media Overlays 链接解析; PDFium 相关变更与本插件无关 | GitHub Releases 页 (2026-09-18 读取), `readium-navigator/maven-metadata.xml` (`<release>3.4.0</release>`, `lastUpdated 20260911173748`) |
| 可用制品: `readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-common`, `readium-navigator-media-common`, `readium-navigator-media-tts`, `readium-navigator-media-audio`, `readium-navigator-web-*` (实验性 Compose 导航器, 不采用), `readium-opds`, `readium-lcp`, `readium-adapter-exoplayer*`, `readium-adapter-pdfium*`, `readium-adapter-pspdfkit*` | Maven Central 目录列表 (2026-09-18 `curl`) |
| `readium-shared` 3.4.0 依赖: `kotlin-stdlib 2.4.20`, `kotlin-parcelize-runtime`, `kotlin-reflect` (runtime), `kotlinx-coroutines-android 1.11.0`, `kotlinx-datetime 0.8.0`, `kotlinx-serialization-json 1.11.0`, `jsoup 1.23.2`, `timber 5.0.1`, `androidx.annotation 1.10.0` | `readium-shared-3.4.0.pom` |
| `readium-streamer` 3.4.0 依赖: `readium-shared`, `com.mcxiaoke.koi:core 0.5.5` (Maven Central 可解析, HTTP 200), `timber`, 排除 `support-v4` | `readium-streamer-3.4.0.pom` |
| `readium-navigator` 3.4.0 依赖: `readium-shared`, `androidx.databinding:viewbinding 9.3.1`, `appcompat 1.8.0`, `browser 1.10.0`, `constraintlayout 2.2.2`, `core-ktx 1.19.0`, `fragment-ktx 1.9.0`, `legacy-support-core-ui 1.0.0`, `lifecycle-common-java8 2.11.0`, `recyclerview 1.4.0`, `media3-session / media3-common-ktx / media3-exoplayer 1.11.0`, `webkit 1.17.0`, `jsoup`, `kotlinx-serialization-json`; 无原生库声明, media3 为纯 Java (P0.2 用 native-alignment 门禁复核) | `readium-navigator-3.4.0.pom` |
| `readium-navigator-media-tts` 3.4.0 依赖: `readium-navigator-media-common` (再依赖 `readium-navigator`), `media3-session`, `media3-common-ktx`; 朗读引擎为系统 `TextToSpeech` | `readium-navigator-media-tts-3.4.0.pom`, `readium-navigator-media-common-3.4.0.pom` |
| 上游以 Kotlin 2.4.20 编译; Kotlin 编译器可读取高一个 minor 的库元数据 (N+1), 平台版本插件 1.8.2 提供 Kotlin 2.3.20 (Angus Mail 2026-09-18 以 Gradle 9.5.0 / AGP 9.3.2 / Kotlin 2.3.20 / JDK 21 构建通过), MCP Server 已用 2.3.20 消费 2.4.0 元数据的 SDK; 仍需 P0.2 实测 | `AutoJs6-Plugin-Angus-Mail/ROADMAP.md` 3.3, `AutoJs6-Plugin-MCP-Server/ROADMAP.md` D4 |
| `readium-shared` 内建 zip 随机访问抽象: `util/zip/StreamingZipArchiveProvider.kt`, `StreamingZipContainer.kt`, `ReadableChannelAdapter.kt`, `FileZipArchiveProvider.kt`; 资源变换 `util/resource/TransformingContainer.kt` / `TransformingResource.kt`; 资产打开 `util/asset/AssetRetriever.kt` / `AssetSniffer.kt` | GitHub 3.4.0 tag 目录列表 (2026-09-18) |
| `EpubNavigatorFragment.Configuration` 提供 `Listener` (含 `OverflowableNavigator.Listener`, `HyperlinkNavigator.Listener`), `PaginationListener` (`onPageLoaded` / `onPageChanged`), `addInputListener` (`TapEvent` / drag / key), `addDecorationListener` (高亮), `currentLocator: StateFlow<Locator>`, `settings: StateFlow<EpubSettings>`; WebView 请求经 `viewModel.shouldInterceptRequest` | `readium/navigator/.../epub/EpubNavigatorFragment.kt:119-142, 270-292, 696-751, 796-829, 875, 1004` |
| `EpubPreferences` 字段: `backgroundColor`, `columnCount`, `fontFamily`, `fontSize`, `fontWeight`, `hyphens`, `imageFilter`, `language`, `letterSpacing`, `ligatures`, `lineHeight`, `pageMargins`, `paragraphIndent`, `paragraphSpacing`, `publisherStyles`, `readingProgression`, `scroll`, `spread`, `textAlign`, `textColor`, `textNormalization`, `theme`, `typeScale`, `verticalText`, `wordSpacing`; 上游自带 SharedPreferences 序列化示例 | `readium/navigator/.../epub/EpubPreferences.kt:52-76, 146` |
| Readium test-app (同仓库 `test-app/`, BSD-3-Clause) 提供阅读器 UI, 偏好面板, 搜索, TTS, 书签, 高亮的参考实现, 可裁剪改写但不整体复制 (Compose + Room 形态与本插件不同) | `github.com/readium/kotlin-toolkit/tree/3.4.0/test-app` |
| 平台版本插件 1.8.2 已发布 (Angus Mail 2026-09-18 以 `curl` 读取 maven-metadata 核实); HTML / Markdown Previewer 当前用 1.8.1 + native-alignment 1.8.1 | `AutoJs6-Gradle-Platform-Versions/version.properties` (`VERSION_NAME=1.8.2`), 两个 Previewer 的 `settings.gradle.kts` |

---

## 4. 目标架构

### 4.1 数据流

```
文件管理器 (宿主 Explorer)                     其它应用 / Launcher                     脚本 (Rhino)
  主动作 / 溢出菜单 (v2 双 URI 信封)            ACTION_VIEW content:// / 文档选择器       epub.open(path) / epub.read(path)
  |                                             |                                        |  同步: runBlocking   异步: ScriptPromiseAdapter   事件: EpubReaderSession (EventEmitter)
  v                                             v                                        v
插件进程 (io.github.supermonster003.autojs6.plugin.readium.epub.reader)              宿主进程 (AutoJs6)  org.autojs.autojs.core.plugin.epub
  EpubReaderActivity <-- EpubRequestPolicy (三条入口各自的信任边界)                       EpubPluginHost (AidlPluginHost: 发现 / 授权 / 版本 / 专用绑定租约)
      |  PfdResource -> AssetRetriever -> Publication (readium-streamer)                 EpubBookClient (openBook: metadata / toc / text / cover / search)
      |  EpubNavigatorFragment (readium-navigator) + ReaderChrome (工具栏 / 目录 / 进度 / 偏好面板 / 搜索 / 书签)   EpubReaderSessionClient (openReader + token, 事件桥, 死亡 -> close)
      |  TtsController (readium-navigator-media-tts) <-> TtsForegroundService (mediaPlayback)                        EpubReaderLauncher (宿主 startActivity 插件 Activity, extra = token)
      |  BookDataStore (progress.json / bookmarks.json, 指纹键)   FontStore   PreferencesStore (EpubPreferences JSON)   EpubSource (脚本路径 -> 只读 PFD)   EpubJson / EpubErrorMapper
      ^                                                                                     |
  ReadiumEpubReaderPluginService (IEpubPlugin) <----------- AIDL -----------------------------+
      EpubBookBinder (IEpubBook)   ReaderSessionRegistry (token -> IEpubReaderSession, Activity attach)   CallerGuard (宿主包名 / 签名)   Limits
  ExplorerActionService (IExplorerActionPlugin: getInfo / getActionCatalog)   PluginInfoService (INFO)   WakeActivity
```

### 4.2 目标包结构

插件 (`io.github.supermonster003.autojs6.plugin.readium.epub.reader`):

```
:app
    plugin/      ExplorerActionService, PluginInfoService, WakeActivity, PluginRuntimeInfo (两份 PluginInfo + 动作目录), EpubReaderExplorerCompatibility, EpubReaderIntentPolicy (v2 信封校验)
    service/     ReadiumEpubReaderPluginService (IEpubPlugin.Stub), EpubBookBinder (IEpubBook.Stub), ReaderSessionBinder (IEpubReaderSession.Stub), ReaderSessionRegistry, CallerGuard, Limits, ErrorCodes, DescriptorIo
    book/        PfdResource, PublicationOpener (AssetRetriever + Streamer 封装), BookFingerprint, TocFlattener, TextExtractor (ContentService 分块), CoverExtractor, SearchRunner
    reader/      EpubReaderActivity, ReaderChrome, TocSheet, SearchPanel, BookmarkSheet, PreferencesPanel, PageTurnController (点按区 / 音量键 / 手势), LinkPolicy (内链 / 外链 / 脚注 / 图片), ImageViewerDialog, ReaderStateCodec (进程重建)
    tts/         TtsController, TtsForegroundService, TtsNotification, TtsVoicePolicy
    store/       BookDataStore (progress / bookmarks), PreferencesStore, FontStore, RecentBooksStore (持久 URI), StoreLimits
    app/         LauncherActivity (最近书籍 + 打开), ExternalViewerActivity (ACTION_VIEW), EpubRequestPolicy, SettingsActivity, AboutActivity, ReleaseHistoryActivity, AppUpdateCoordinator / AppUpdateRepository / AppVersionPolicy, HostAppearance (宿主设置快照)
```

宿主新增 (`org.autojs.autojs.core.plugin.epub` 与 `org.autojs.autojs.runtime.api.augment.epub`):

```
core/plugin/epub/
    EpubPluginHost               (AidlPluginHost 封装; discover / probe / queryServiceCount; openBook / openReader 返回带租约的客户端)
    EpubBookClient               (metadata / toc / readingOrder / text / resource / search / positions / close; 上限与错误映射)
    EpubReaderSessionClient      (getState / goTo / navigate / setPreferences / bookmarks / close; 死亡 -> close 事件; 纯逻辑可 JVM 测试)
    EpubReaderBridge             (IEpubReaderCallback -> generation / seq 校验 -> 节流 -> EventEmitter)
    EpubReaderLauncher           (宿主 startActivity 插件 EpubReaderActivity, extra 仅 token)
    EpubSource                   (脚本路径 -> 只读 PFD, 大小 / 扩展名 / 魔数预检)
    EpubOutputSink               (封面 / 资源导出: 插件 PFD 读端 -> 宿主流式落盘到脚本指定路径, 原子重命名)
    EpubJson                     (请求 / 响应 / 事件 / Locator 的宿主侧模型, 纯 Kotlin)
    EpubErrorMapper              (错误码 -> EpubError; 发现 / 绑定失败 -> PLUGIN_UNAVAILABLE 家族)
runtime/api/augment/epub/
    Epub                         (Augmentable, keys = ["epub"], open / metadata / toc / text / cover / search / read / progress / bookmarks 及 *Async, EpubError)
    EpubBook                     (脚本可见实例; 同步 + Async 方法对; close)
    EpubReaderSession            (EventEmitter; goTo / next / prev / setPreferences / locator / isOpen / close)
    EpubObjects                  (Locator / TocEntry / Metadata / SearchResult 的 JS 对象构造)
```

宿主契约 (`plugin-api/epub-api`, 包 `org.autojs.plugin.epub.api`):

```
IEpubPlugin.aidl             PluginInfo getInfo(); Bundle getCapabilities(); IEpubBook openBook(in ParcelFileDescriptor source, in Bundle options); IEpubReaderSession openReader(in ParcelFileDescriptor source, in Bundle options, IEpubReaderCallback callback);
IEpubBook.aidl               Bundle getMetadata(); Bundle getToc(); Bundle getReadingOrder(); Bundle getText(in Bundle request); ParcelFileDescriptor openResource(String href); Bundle search(in Bundle request); Bundle getPositions(); void close();
IEpubReaderSession.aidl      Bundle getState(); void goTo(in Bundle target); void navigate(int direction); void setPreferences(in Bundle preferences); Bundle getBookmarks(); void close();
IEpubReaderCallback.aidl     oneway: void onEvent(long generation, long seq, in Bundle event);
EpubContract.kt              CONTRACT_VERSION / MIN / MAX, KEY_* 常量, EVENT_* 常量, 上限常量 (附录 B.5)
EpubActions.kt               SERVICE_ACTION = "org.autojs.plugin.EPUB", SERVICE_CATEGORY = "epub", READER_ACTIVITY_ACTION = "org.autojs.plugin.EPUB_READER_OPEN", EXTRA_SESSION_TOKEN
EpubIds.kt                   PLUGIN_ID = "readium-epub-reader", ENGINE = "epub", VARIANT = "default", DEFAULT_PACKAGE_NAME, REQUIRED_HOST_VERSION_CODE
EpubCapabilityKeys.kt        REQUIRES_HOST_VERSION, CONTRACT_VERSION, FEATURES (search / tts / reader-session / cover / resource-export)
EpubErrorCodes.kt            附录 B.4 的错误码字符串常量
```

设计原则:

1. 单一事实来源: Explorer Action 兼容矩阵只存在于 `gradle/explorer-action-compatibility.properties` (经 `BuildConfig` 进入代码 / 测试 / 文档); `epub` 契约的 key / 事件 / 上限只存在于 `EpubContract`; 脚本 API 方法表从附录 A 派生并由快照测试锁定.
2. 纯 JVM 可测: 三条入口的请求策略, 指纹, 偏好编解码, 目录扁平化, 文本分块, 错误映射, 版本比较, 更新检查策略全部不依赖 Android / Binder / Readium 运行时.
3. 失败闭合: 非法信封, 非 EPUB, 加密, 上限超出都返回明确错误并保留用户上下文; 插件死亡时脚本会话显式收到 `close(reason=plugin-died)` 而不是挂起.
4. 宿主改动最小且可退化: 四态引导复用既有对话框与文案; 插件禁用或卸载后宿主没有任何 EPUB 能力残留.

---

## 5. 阶段总览

| 阶段 | 目标 | 主要落点 | 前置 |
| --- | --- | --- | --- |
| P0 | 仓库骨架 + Readium 可行性 spike + 样本与许可证 + 决策点 | 插件 | 无 |
| P1 | 文件管理器最小可用阅读器 (Explorer Action v2) + 进度记忆 + 宿主类型行与引导 | 插件 + 宿主 (小) | P0 |
| P2 | 阅读体验: 偏好全集, 字体导入, CJK 竖排 / RTL, FXL, 全文搜索, 书签, 手势 / 按键 / 链接 / 图片 | 插件 | P1 |
| P3 | TTS 朗读与前台服务 | 插件 | P2 |
| P4 | 独立应用形态: Launcher, 文档选择器, 最近书籍, ACTION_VIEW, 设置页, 发行历史, 更新检查 | 插件 | P2 |
| P5 | 宿主契约 `plugin-api/epub-api`, 插件能力服务, 阅读器会话, 宿主客户端, 插件中心注册, 协议文档 | 宿主 + 插件 | P1 (P2-P4 可并行) |
| P6 | 脚本 API `epub` (提取 + 阅读器控制), 示例脚本, 文档 / d.ts / Ace / 离线文档同步 | 宿主 + 文档 | P5 |
| P7 | 健壮性, 安全, 兼容矩阵, 性能, 体积, 无障碍 | 全部 | P3, P4, P6 |
| P8 | README, changelog, 截图, 第三方声明, CI, 1.0.0 发布 gate, 索引条目 | 文档 + 发布 + 索引 | P7 |
| P9 | 高亮, 笔记与导出 (目标 1.1.0) | 插件 (+ 宿主小) | P8 |

依赖顺序:

```text
P0 ──> P1 ──> P2 ──> P3 ──┐
       │      └───> P4 ──┤
       └────> P5 ──> P6 ──┴──> P7 ──> P8 ──> P9
```

建议会话切分: P0 一次会话 (骨架 + spike 可拆两次); P1 两次 (契约 / Activity / 进度为一次, 宿主接入 + 测试为一次); P2 三次 (偏好与字体; 竖排 / RTL / FXL; 搜索 / 书签 / 手势 / 链接); P3 一次; P4 两次 (入口与最近书籍; 设置页 / 发行历史 / 更新检查); P5 两到三次 (契约 + 插件能力服务; 阅读器会话 + 宿主客户端; 注册 + 往返测试 + 协议文档); P6 两次 (提取 API; 阅读器控制 + 文档同步); P7 一到两次; P8 一次; P9 两次.

---

## P0: 仓库骨架与 Readium 可行性 spike

目标: 让 `AutoJs6-Plugin-Readium-EPUB-Reader` 成为一个可构建, 可安装, 能用 `EpubNavigatorFragment` 打开一本样本 EPUB 并翻页的最小 APK, 并在阶段末决定 D2 / D11 是否成立.

### P0.1 仓库骨架

- [x] (插件) 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成骨架: `settings.gradle.kts` (平台版本插件 1.8.2 + native-alignment 1.8.2, 位于 `includeBuild("build-logic")` 之前), 根 `build.gradle.kts` (`com.android.application` 版本来自 `gradle.agp.version`, `apply false`), `build-logic` 约定插件 (从 HTML Previewer 复制后精简), `version.properties` (`VERSION_NAME=1.0.0`, `VERSION_BUILD=1`, SDK 24 / 37 / 37, `OVERRIDDEN_*=NONE`), `gradle/libs.versions.toml` (Readium 3.4.0 五个制品, AndroidX, Material, media3 对齐 1.11.0). 证据: `46c7b12`; Temurin 验收构建 BUILD SUCCESSFUL, 日志只有一段平台版本决策 (2026-09-19).
- [x] (插件) 从宿主复制 `.gitignore`, `sign.properties`, `app/sm003.jks` 到相同相对路径, `git check-ignore` 验证后两者不入索引; `sign.properties` 的 `storeFile` 能从新项目解析. 证据: `git check-ignore` 命中 `/sign.properties`, `*.jks`, `/local.properties`; `:app:validateSigningRelease` 通过.
- [x] (插件) `app/build.gradle.kts`: `resValue` 六键 (`app_name` = `Readium EPUB Reader`, `plugin_author`, `plugin_id`, `plugin_engine`, `plugin_variant`, `plugin_version_date`) + `plugin_requires_host_version`; `buildFeatures { aidl; buildConfig; resValues; viewBinding }`; `nativeAlignment { expectNoNativeLibraries.set(true) }`; `appendDigestToReleasedFiles` + `verifySignedReleaseArtifacts` (单 APK 形态); `libs/common-plugin-api.aar` + `libs/explorer-action-api.aar` (从 HTML Previewer 复制, 同一 SHA-256) + `locks/host-api-aars.lock` 配置期校验. 证据: `46c7b12`; `locks/host-api-aars.lock` 与 `gradle/explorer-action-compatibility.properties` 哈希一致, `verifyExplorerActionApiCompatibility` 通过; `PluginContractInstrumentationTest.resValueIdentityMatchesTheKotlinConstants`.
- [x] (插件) Manifest 骨架: `org.autojs.permission.PLUGIN`, `INTERNET`, `<queries><package android:name="org.autojs.autojs6" /></queries>`, `WAKE_ACTIVITY` / `info.AUTHOR` / `requiresHostVersion` / `contract.NATIVE_PAGE_ALIGNMENT=0` meta-data, `WakeActivity` (`Theme.NoDisplay`, exported, permission, `org.autojs.plugin.action.WAKE`), `PluginInfoService` (`org.autojs.plugin.INFO`), `ExplorerActionService` (`org.autojs.plugin.EXPLORER_ACTION`), `EpubReaderActivity` (`EXPLORER_ACTION_EXECUTE`, exported + permission), `allowBackup=false`, `usesCleartextTraffic` 按 D31 为 `true`. 证据: `f705d54`; `usesCleartextTraffic` 按 D31 为 `true`; media3-exoplayer 注入的 `ACCESS_NETWORK_STATE` / `WAKE_LOCK` 以 `tools:node="remove"` 移除, `PluginContractInstrumentationTest.manifestRequestsOnlyTheInternetAndPluginPermissions` 在 API 24 / 35 通过.
- [x] (插件) 10 语言 `strings.xml` (`plugin_description`: 简体中文 `阅读 EPUB 电子书并提供目录, 搜索, 朗读与脚本提取能力`, 英文 `Reads EPUB e-books with navigation, search, read-aloud and scripting access`, 句尾无标点, 按 name 排序, ASCII 标点), `strings_donottranslate.xml`, `locales_config.xml`, `raw-*/plugin_instruction.md`, `mipmap/ic_launcher.png` (书本 + Readium 风格页角, 与两个 Previewer 的铅笔图案区分, 浅 / 深背景可辨). 证据: `b25f0b6`; 图标由 `.python/generate_launcher_icons.py` 生成 (翻开的书, 青绿底); `raw*/plugin_instruction.md` 手工维护 (不用模板).
- [x] (插件) `.readme/` + `.changelog/` JSON 源 (10 语言) + `template_readme.md` / `template_plugin_instruction.md` / `template_changelog.md`, `.python/generate_markdown.py` (`--check` 模式, 从 HTML Previewer 复制并改名), `.python/tests/test_repository_contract.py`, `.python/*.bat`; `README.md` 明确标识为简体中文版本. 证据: `b25f0b6`; `generate_markdown.py` 输出 25 个产物, `--check` 与 `py -3 -m unittest discover -s .python/tests` (7 用例) 通过; 未引入 `template_plugin_instruction.md` (说明文件直接手写).
- [x] (插件) `.github/workflows/build.yml` (JVM 测试 + lint + debug / androidTest APK; API 35 模拟器 instrumentation) 与 `markdown.yml` (Windows `check_markdown.bat`), CI 参数含 `--max-workers=2` 与关闭自动版本递增. 证据: `bae2eb4`; instrumentation 矩阵为 API 24 (x86) + API 35 (x86_64); 工作流尚未在 GitHub 上运行 (仓库未推送).
- [x] (插件) `AGENTS.md` (裁剪版规范 + 本插件专属约束: 双服务身份, 无原生库, 权限红线, 发布任务名, 验收命令), `LICENSE` (MPL-2.0 全文, 与其它插件一致), `THIRD_PARTY_NOTICES.md` (Readium BSD-3-Clause, jsoup MIT, media3 / koi / timber / kotlinx Apache-2.0, 之后按实际依赖树补齐). 证据: `46c7b12`; `THIRD_PARTY_NOTICES.md` 已按实际依赖树补齐 (jsoup, kotlinx, Timber, media3, Guava, koi).
- [x] (插件) `git init` 并按逻辑拆分初始提交 (骨架 / 契约 / 资源与文档 / 测试与 CI), 每笔提交前把 `VERSION_BUILD` 写为 `git rev-list --count HEAD + 1`; 骨架完成后 Temurin 验收构建 (`--no-daemon -Djava.vendor="Eclipse Adoptium" -Djava.vendor.version=Temurin-21.0.12.1+1 :app:assembleDebug :app:testDebugUnitTest`) 只输出一段平台版本决策. 证据: 6 笔初始提交 `46c7b12` / `f705d54` / `b25f0b6` / `bae2eb4` / `b6e65de` / `27af413` (骨架 / 契约 / 资源与文档 / 测试与 CI / 样本 / 阅读器), 作者邮箱 `30370009+SuperMonster003@users.noreply.github.com`, `VERSION_BUILD` 与提交数同步.

验收: 空壳 APK 可安装; 插件中心能发现并显示 `Readium EPUB Reader` (激活按钮可用); `py .python/generate_markdown.py --check` 与 `py -3 -m unittest discover -s .python/tests` 通过; `git status --short` 为空且 `VERSION_BUILD` 与提交数一致.

### P0.2 Readium 可行性 spike

- [x] (插件) 依赖解析: 引入 `readium-shared` / `readium-streamer` / `readium-navigator` / `readium-navigator-media-tts`, 记录 `:app:dependencies` 中 Kotlin stdlib 冲突解析结果 (预期 2.4.20), `viewbinding 9.3.1` 与平台 AGP 的兼容性, `koi` 解析来源; 编译通过且无 Kotlin 元数据版本错误. 失败则记录错误并评估 `OVERRIDDEN_KOTLIN_GRADLE_PLUGIN_VERSION` 之外的正规路径 (先升级平台版本插件). 证据: `docs/dev/p0-readium-spike.md` 1 (stdlib 2.3.20 -> 2.4.20, viewbinding 9.3.2, koi 0.5.5, 零原生库).
- [x] (插件) Release 构建: R8 通过 (`-dontwarn` 与 `-keep` 最小集写入 `proguard-rules.pro` 并逐条注释原因, 重点 kotlinx-serialization, kotlin-reflect, Readium 反射 / Parcelize), 记录 universal APK 体积 (debug / release) 作为体积基线, `verifyNativePageAlignment` 报告零原生库. 证据: `docs/dev/p0-readium-spike.md` 2 (release 2,749,948 B, debug 15,463,211 B, `missing_rules.txt` 未生成, `verifyNativePageAlignment` 零条目).
- [x] (插件) 最小阅读: `EpubReaderActivity` 用 `AssetRetriever` + `PublicationOpener` 打开 `docs/fixtures` 的 EPUB 3 可重排样本, `EpubNavigatorFragment` 渲染, 左右翻页, 目录跳转 (直接 `go(Link)`), `currentLocator` 打印; 在 AVD API 24 与 Xiaomi 23046RP50C (API 35) 各跑一次并截图. 证据: `27af413`; `EpubReaderUiInstrumentationTest` 在 AVD API 24 与 Xiaomi 23046RP50C API 35 通过 (打开 753 / 577 ms, 翻页, 目录跳转, 重建恢复), 截图见 spike 记录 4 (不入库).
- [x] (插件) 随机访问 (D11): 实现 `PfdResource` (FileChannel 定位读 + `statSize`), 经 `content://` 打开的 PFD 交给 `AssetRetriever` / `StreamingZipArchiveProvider` 完成同一样本的打开与翻页; 记录打开耗时与是否有整文件读取 (用 `strace` 或 `ContentResolver` 计数器判断); 失败则退回 Readium content URI 资源工厂并记录原因. 证据: `book/PfdResource.kt`, `PfdResourceInstrumentationTest` (定位读, 并发, EPUB 2 / 3 打开, positions / search / content 服务); 每次打开只取一次描述符 (`openCount == 1`), 无整本复制; 未用 strace, 以描述符计数与 `sourceUrl = null` 判定.
- [ ] (插件) 阅读器要点验证并写入 `docs/dev/p0-readium-spike.md`: `EpubPreferences` 生效方式 (`submitPreferences`), 主题 / 字号 / 滚动 / 竖排各改一次; `fontFamilyDeclarations` 注入一枚外部字体; `SearchService` 在样本上返回结果; `TapEvent.targetElement` 是否可用于图片; `TtsNavigator` 在真机上朗读一段; WebView `shouldInterceptRequest` 是否能拦截远程请求 (为附录 F 的安全模式留证据); Activity 重建后 `Locator` 恢复. 部分完成 (2026-09-19): 渲染 / 翻页 / 目录 / `currentLocator` / 重建恢复 / Search / Positions / Content 已验证; `EpubPreferences`, 字体注入, `TapEvent`, `TtsNavigator`, `shouldInterceptRequest` 推迟到 P2 / P3 (见 spike 记录 5), 不影响决策点.
- [x] (测试) spike 期间为 `PfdResource` (长度, 越界读, 并发读) 与 `BookFingerprint` 写 JVM 用例 (以 `RandomAccessFile` 模拟). 证据: JVM `ByteRangesTest` / `BookFingerprintTest` (Android-free 部分), 设备 `PfdResourceInstrumentationTest` (长度 / 越界 / 32 路并发读, `ParcelFileDescriptor` 无法在 JVM 模拟).
- [x] 决策点: 以上全部成立则 D2 / D11 固定; 任一不成立记录到附录 E.2 并由维护者决定退路. 已决定 (2026-09-19): D2 / D11 固定, 见 `docs/dev/p0-readium-spike.md` 6.

验收: `docs/dev/p0-readium-spike.md` 含依赖树摘要, 体积基线 (数值), 两台设备的打开 / 翻页证据, 随机访问结论与 R8 规则清单.

### P0.3 测试样本与许可证

- [x] (插件) `.python/generate_fixtures.py`: 生成最小 EPUB 2 (NCX, 3 章), 最小 EPUB 3 (NAV, 3 章, 内链 / 脚注 / 图片), 损坏样本 (非 zip, 缺 `container.xml`, 缺 OPF, 坏 NCX, 路径穿越 href, 超多条目, 高压缩比单资源), 加密标记样本 (`META-INF/encryption.xml` 声明 LCP); 输出 `docs/fixtures/*.epub` 与 SHA-256 清单; 单元测试覆盖生成器. 证据: `b6e65de`; 10 个样本 + `SHA256SUMS.txt` + `docs/fixtures/README.md`; `.python/tests/test_fixtures.py` 校验确定性, 校验和, 文档覆盖与总体积.
- [ ] (插件) 引入外部样本并记录来源 / 许可 / SHA-256 到 `docs/fixtures/README.md`: IDPF `epub3-samples` 的公有领域可重排样本 (如 `moby-dick`), FXL 样本 (`page-blanche` 类 CC0 / 公有领域), 日文竖排样本 (`kusamakura` 系列, CC BY-SA, 注明署名), 阿拉伯语 RTL 样本 (`regime-anticancer-arabic` 系列, 核实许可后再入库), W3C `epub-tests` 若干用例; 任何许可不明的样本不入库. 未做 (2026-09-19): 需要逐个核实许可证并下载, 当前网络易触发 502 / 429, 留待 P2 (FXL / 竖排 / RTL 落地前) 一并引入.
- [ ] (插件) 性能样本生成参数 (20 MB 图片书, 200 MB 图片书, 5000 章文字书) 写入 `make_fixtures.py --perf`, 产物不入库 (`.gitignore`). 未做 (2026-09-19): 留待 P7 性能条目.

验收: `docs/fixtures/README.md` 每个文件都有来源, 许可, SHA-256; 生成器测试通过; 仓库内样本总体积不超过 15 MB.

---

## P1: 文件管理器最小可用阅读器与宿主接入

目标: 在版本代码不低于 5269 的 AutoJs6 文件管理器中点击 `.epub` 打开阅读器, 具备目录, 进度, 分页 / 滚动与位置记忆; 宿主对 `.epub` 有类型识别与未安装引导.

### P1.1 Explorer Action 契约 (v2)

- [x] (插件) `gradle/explorer-action-compatibility.properties` (`declaredProtocolVersion=2`, `minimumHostVersionCode=5269`, `maximumAuditedHostVersionCode=5282`, `maximumAuditedHostProtocolVersion=22`, AAR SHA-256), `verifyExplorerActionApiCompatibility` 门禁挂在 `preBuild`, `EpubReaderExplorerCompatibility` (检查点表含 5268 / 5269 / 5276 / 5277 / 5279 / 5282), `docs/explorer-action-compatibility.md` (审计边界, 为何不用 v4+). 证据: `27ed186`; 属性文件, `preBuild` 门禁与文档在 P0.1 已建, 本轮补 5279 检查点 (v22) 并在 `docs/explorer-action-compatibility.md` 记为 Audited.
- [x] (插件) `PluginRuntimeInfo`: Explorer Action 服务的 `PluginInfo` (engine `explorer-action`, `supportedAbis = emptyArray()`, capabilities: `REQUIRES_HOST_VERSION=5269`, `explorerActionProtocolVersion=2`) 与动作目录 (两条动作: `readium-epub-reader.primary` / `PLACEMENT_PRIMARY`, `readium-epub-reader` / `PLACEMENT_OVERFLOW`; `TARGET_FILE`, `ACCESS_READ_ONLY`, MIME `application/epub+zip`, 扩展名 `epub`, `PRIORITY 100`, `labelResourceName=action_readium_epub_reader`). 证据: `27ed186`; 两条动作以纯数据 `ExplorerActionSpec` 描述 (`actionSpecs()`), 目录 Bundle 由规格映射, `ACTION_PRIORITY = 100`.
- [x] (插件) `EpubReaderIntentPolicy`: 逐字段校验 `EXECUTE` action, 动作 ID, 协议恰为 2, `HOST_VERSION_CODE >= 5269`, `SOURCE_SURFACE_MAIN`, 读 + 前缀授权标志, 纯 `content://` 目标与父目录, 目标为父目录后代, ClipData 恰两项且一致, 显示名净化 (255 字符, 去控制字符), MIME / 扩展名判定 (`application/epub+zip` 或 `.epub`; `application/zip` + `.epub` 亦接受; 其它扩展名拒绝). 证据: `27ed186`; 冲突容器只保留 `application/x-cbz` / `application/vnd.comicbook+zip` / `application/pdf`, `application/zip` + `.epub` 接受, `application/zip` + 无扩展名或 `.zip` 拒绝; 源码中的 NUL 字面量改为 `\u0000` 转义 (原文件被 git 视为二进制).
- [x] (测试) JVM: `EpubReaderIntentPolicyTest` (合法 / 每个字段缺失或错误 / v22 信封 / 额外目标 / 目录), `EpubReaderExplorerCompatibilityTest` (检查点连续, 审计状态), `PluginRuntimeInfoTest` (纯数据映射, 两条动作形状). 证据: `27ed186`; `PluginRuntimeInfoTest` 3 用例 (ID / 位置 / 优先级 / MIME / 扩展名, 共享标签与 Activity, 位置互异); 本轮结束时 JVM 全量 58 用例通过 (2026-09-19).

### P1.2 阅读器 Activity 基线

- [x] (插件) `EpubReaderActivity` 继承 `HostAppearanceActivity` (从 Previewer `PreviewerHostActivity` 改造: 跟随宿主语言与暗色, 宿主不可用时跟随系统); 从信封取 `content://` -> PFD -> `PfdResource` -> `Publication`; 后台线程打开, 加载中 / 失败状态 (非 EPUB, 损坏, 加密提示 "不支持受 DRM 保护的书籍", 超时). 证据: `1d880ab`; `OpenFailure` (`CannotRead` / `NotAnEpub` / `Protected` / `TimedOut` / `Other`) 映射到 `text_cannot_read_file` / `text_open_failed_*`, 打开超时 60 s (`withTimeoutOrNull`); 损坏样本用例 `aMalformedFileShowsAnErrorInsteadOfCrashing` 在 AVD API 24 (x86, `Android SDK built for x86`) 通过.
- [x] (插件) `ReaderChrome`: 顶部工具栏 (书名 / 当前章节名, 目录, 搜索占位, 溢出菜单), 底部进度条 (章节内进度 + 全书百分比 + 位置 `x / N`, `PositionsService` 后台计算, 未就绪时只显示百分比), 点击正文切换沉浸 (系统栏隐藏 / 显示, 与 Previewer 全屏模式同形), 横竖屏与分屏布局. 证据: `1d880ab`; `reader/ReaderChrome` + `reader/ReaderProgress` (`x / N (p%)`, 位置未就绪时只显示百分比, `positions()` 后台计算), 沉浸态经 `WindowInsetsControllerCompat` 并写入 `savedInstanceState`; 搜索入口留到 P2.5; AVD API 24 经宿主打开的截图显示 `Minimal EPUB 3 / Chapter 3` 与 `3 / 3 (67%)`.
- [x] (插件) 目录 `TocSheet`: `Publication.tableOfContents` 树形 (EPUB 2 NCX / EPUB 3 NAV 皆由 Readium 归一), 缺目录时回退 `readingOrder`, 当前章节高亮, 点击跳转; `TocFlattener` 纯逻辑 (深度上限 8, 条目上限 5000). 证据: `1d880ab`; `book/TocFlattener` (深度 8, 5000 条, 按资源 href 匹配当前章节) + `TocFlattenerTest` 4 用例; `TocSheet` 用 `AlertDialog` + 缩进列表, 当前行加粗并滚动到位; UI 用例在目录第 4 行点击跳到 chapter 3.
- [x] (插件) 分页 / 滚动切换 (`EpubPreferences.scroll`), 左右 / 上下翻页手势由 Readium 提供; 音量键翻页 (默认开, 可在偏好关闭); 点按区 (左 1/3 上一页, 右 1/3 下一页, 中间切换沉浸) 经 `InputListener`. 证据: `1d880ab`; `reader/PageTurnPolicy` (点按三分区, RTL 镜像, 音量键映射) + `PageTurnPolicyTest` 4 用例; `store/ReaderSettings` 持久化 `scroll_mode` / `volume_keys_turn_pages`; 用例 `volumeKeysAndScrollModeDriveTheNavigator` (AVD API 24) 验证音量键翻页与 `settings.value.scroll` 切换.
- [x] (插件) 进程重建: `Locator` 与偏好写入 `savedInstanceState`, 重建后恢复位置; 屏幕旋转不丢位置. 证据: `1d880ab`; `STATE_LOCATOR` / `STATE_IMMERSIVE` 写入 `savedInstanceState`, 保存的 `Locator` 优先于磁盘进度; 用例 `aSavedLocatorInTheInstanceStateWinsOverTheStoredProgress` 在 `recreate()` 后保持 chapter 2 与沉浸态.

### P1.3 进度记忆

- [x] (插件) `BookFingerprint` (Q1 默认算法) 在打开时后台计算; `BookDataStore` 以 `files/books/<指纹>/progress.json` 保存 `Locator` JSON + 更新时间 + 总进度, `AtomicFile`, 书目上限 500 (LRU 淘汰); 打开时若有记录则跳转, 否则从 `readingOrder` 首项开始; 关闭 / 暂停 / 翻页节流写入. 证据: `be75f11` (store) + `1d880ab` (接线); 原子写为纯 JVM `store/AtomicFiles` (临时文件 + fsync + 重命名) 而非 `android.util.AtomicFile`, 便于 JUnit 覆盖; 临时键目录迁移到正式指纹后写 `aliases/<临时键>` 别名, 重开先经别名命中 (否则迁移后重开会丢进度, 2026-09-19 实测); 节流 2 s + 延迟冲刷 + `onPause` / `onCleared` 冲刷 (`persistScope` 独立于 ViewModel 生命周期); 用例 `thePositionIsStoredUnderTheFullFingerprintAndRestoredOnReopen` (AVD API 24) 通过; 200 MiB 样本临时键 9 ms, 全量哈希 608 ms (328.9 MiB/s, x86 AVD), 3390 B 样本全量哈希 1 ms.
- [x] (插件) 溢出菜单 "从头开始" (清除本书进度) 与设置页 "清除全部阅读数据" (P4) 共用同一 store API. 证据: `1d880ab`; `text_restart_book` 二次确认后 `clearProgress()` 并跳回 `readingOrder` 首项 (用例末段验证 chapter1 被重新持久化); `clearAll()` 已在 store 中, P4 设置页接入.
- [x] (测试) JVM: `BookFingerprintTest` (同内容不同路径同指纹, 改首字节 / 尾字节 / 长度变指纹, 小于 1 MiB 文件), `BookDataStoreCodecTest` (Locator 往返, 损坏 JSON 忽略, LRU 淘汰), `ProgressThrottleTest`. 证据: `be75f11`; 实名为 `store/ProgressCodecTest` (5) + `store/BookDataStoreTest` (13, 含别名 3 例, LRU, 迁移合并, 非法键) + `store/ProgressThrottleTest` (6); `BookFingerprintTest` 沿用 P0; JVM 测试引入真实 `org.json:json:20260814` (android.jar 只有存根).

### P1.4 宿主接入 (最小)

- [x] (宿主) `FileUtils.kt`: `PreviewerType.EPUB`; `TypeDataHolder.EPUB_READER = TypeData(IconData(<BMP 字符>), TYPE.IDENTITY_PREVIEWABLE, PreviewerType.EPUB)`; `TYPE.EPUB("epub", TypeDataHolder.EPUB_READER)` 置于 `PDF` 行附近 (`FileUtils.kt:1099`); 确认 `isPreviewable()` 让 `ExplorerPrimaryAction` 走 `DOCUMENT_PREVIEWER`. 证据: 宿主 `40f8a4206`; `EPUB_READER = TypeData(IconData("\u25A4", size = 22, toTop = 1), TYPE.IDENTITY_PREVIEWABLE, PreviewerType.EPUB)`, `TYPE.EPUB` 紧随 `PDF`; 测试断言 `ExplorerPrimaryActionResolver.resolve` 得到 `DOCUMENT_PREVIEWER`.
- [x] (宿主) `ExplorerDocumentPreviewerPluginUi.kt`: `specFor` 新增 `PreviewerType.EPUB -> PreviewerSpec(READIUM_EPUB_READER_PACKAGE, R.string.plugin_readium_epub_reader_name, MIME_EPUB)`; 常量 `io.github.supermonster003.autojs6.plugin.readium.epub.reader` 与 `application/epub+zip`; 归档内 EPUB 按 D24 处理 (宿主上限按类型取值, EPUB 为 256 MiB, 超限提示先解压). 证据: 宿主 `40f8a4206`; `maximumArchiveDocumentBytes(previewerType)` EPUB 256 MiB / 其它 8 MiB, `ExplorerArchiveCoordinator` 传入 `item.type.previewerType`; 超限提示沿用既有文案.
- [x] (宿主) `strings_donottranslate.xml`: `plugin_readium_epub_reader_name = Readium EPUB Reader`; 10 语言 changelog `feature` 条目 (文件管理器识别 EPUB 并引导安装 Readium EPUB Reader); `docs/dev/readium-epub-reader-plugin-integration.md` (入口, 类型行, 引导, 后续契约指针). 证据: 宿主 `40f8a4206`; 10 语言 `feature` 条目插在 Markdown/HTML 图标动作条目之后 (zh-Hans 索引 103, 其余 131), README / CHANGELOG 已再生成; `docs/dev/readium-epub-reader-plugin-integration.md`.
- [x] (宿主/测试) `DocumentPreviewerFileTypeTest`: `.epub` 可预览且 `previewerType == EPUB`, 内容为非 zip 的 `.epub` 仍按扩展名识别 (与 Markdown 的内容嗅探规则区分, 若 `TYPE` 有 `CandidateCriterion` 冲突在此定案); 引导对话框四态单元测试若已有则补 EPUB 分支. 证据: 宿主 `:app:testAppDebugUnitTest --tests DocumentPreviewerFileTypeTest` 6/6 (新增 `epubIsPreviewableAsEpubByExtensionRegardlessOfContent`, `archiveDocumentLimitIsPerPreviewerType`); 引导对话框四态由既有 `ExplorerDocumentPreviewerAvailabilityPolicy` 覆盖, 与类型无关, 未另加分支.
- [x] (宿主) 宿主提交 (`feat(explorer): recognize EPUB files and hand them to Readium EPUB Reader`), 不推送. 证据: `40f8a4206` (2026-09-19, 38 个文件), 未推送.

### P1.5 P1 验收测试

- [x] (测试) instrumentation (API 28 Sony G8441 + API 35 Xiaomi): `PluginContractInstrumentationTest` (Wake / INFO / EXPLORER_ACTION 发现唯一, 显式绑定, descriptor, `getInfo` 字段与 ABI 空数组), `EpubReaderIntentPolicyInstrumentationTest` (真实 Intent 与 ClipData), `EpubReaderActivityInstrumentationTest` (用 debug `FileProvider` 打开 EPUB 2 / EPUB 3 样本, 目录跳转, 翻页, 进度恢复, 损坏样本错误态), `RepositoryContractInstrumentationTest` (Manifest 导出与权限). 部分完成 (2026-09-19): AVD API 24 (x86, `Android SDK built for x86`) `:app:connectedDebugAndroidTest` 23/23: `PluginContractInstrumentationTest` 6, `EpubReaderIntentPolicyInstrumentationTest` 5, `EpubReaderProgressInstrumentationTest` 5 (进度 / 别名 / 重建 / 音量键与滚动 / 就绪前跳转重放), `EpubReaderUiInstrumentationTest` 2, `book/PfdResourceInstrumentationTest` 4, `book/BookFingerprintInstrumentationTest` 1; 实名与规划名不同 (无 `EpubReaderActivityInstrumentationTest` / `RepositoryContractInstrumentationTest`, 对应用例分布在上述类中). Sony API 28 与 Xiaomi API 35 本会话未连接, 真机矩阵待补. 完成 (2026-09-19 下午): Sony G8441 API 28 `:app:connectedDebugAndroidTest` 23/23 (1 m 07 s), Xiaomi 23046RP50C API 35 23/23 (42 s), 与 AVD API 24 同一用例集; 真机证据 API 28: `reader-chrome-api28.txt` (positionCount=3, 3390 B 全量哈希 1 ms), `fingerprint-api28.txt` (200 MiB 样本临时键 4 ms, 全量哈希 400 ms, 500.0 MiB/s), `reader-api28.txt` (打开到首个定位 840 ms, 描述符打开 1 次); API 35: 9 ms / 3390 B, 2 ms / 177 ms (1129.9 MiB/s), 563 ms. P2.1 之后三台设备再次全绿 (AVD API 24 26/26, Xiaomi 23046RP50C API 35 26/26, Sony G8441 API 28 26/26, 2026-09-19 11:24).
- [x] (测试) 宿主真机联调: 安装宿主 debug 与插件, 文件管理器点击 `.epub` 主动作与溢出菜单均打开阅读器; 卸载插件后出现安装引导; 禁用插件后出现激活引导; 记录设备与 API. 部分完成 (2026-09-19, 模拟器而非真机): AVD API 24 (x86, `Android SDK built for x86`) 安装宿主 debug (`40f8a4206`, 5282) 与插件 debug (1.0.0 (7)); 文件管理器 `Scripts/` 内 `minimal-epub3.epub` 行显示 `▤` 字形与 `Readium EPUB Reader` 主图标, 点击主图标与溢出菜单 `Readium EPUB Reader` 条目均以 v2 信封 (`content://org.autojs.autojs6.fileprovider/external_files/Scripts/minimal-epub3.epub`, `application/epub+zip`, 两项 ClipData) 启动 `EpubReaderActivity`, 并从同指纹的上次位置 (chapter 3) 恢复; 卸载插件后对话框 `Plugin "Readium EPUB Reader" not found in installed apps` (插件中心 / 取消 / 其它应用打开); 插件中心关闭开关后对话框 `... is not enabled in Plugin Center`. 真机待补. 完成 (2026-09-19 下午, Xiaomi 23046RP50C API 35, 宿主 debug `40f8a4206` (5282, arm64-v8a) + 插件 debug): 文件管理器 `Scripts/` 内 `.epub` 行显示 `▤` 字形与右侧主图标, 点击主图标与溢出菜单 `阅读 EPUB` 条目均启动 `EpubReaderActivity` 并恢复同指纹的上次位置 (`Minimal EPUB 3 / Chapter 3 / 3 / 3 (67%)`); 截图 `build/p15-host-open-epub3-api35.png` / `build/p15-host-overflow-epub3-api35.png` (gitignored). 点击文件行本身不打开 (身份可预览类型只由主图标 / 溢出条目触发, 与 AVD 一致); 未安装 / 未启用引导对话框沿用 AVD API 24 的验证, 真机未重复.

验收: 两台设备上从文件管理器打开三种样本 (EPUB 2 / EPUB 3 / FXL) 均可阅读并恢复进度; JVM 与 instrumentation 全绿; 宿主 `DocumentPreviewerFileTypeTest` 通过; 10 语言字符串与 changelog 已补齐.

P1 验收状态 (2026-09-19): JVM 58 / 58, AVD API 24 instrumentation 23 / 23, 宿主 `DocumentPreviewerFileTypeTest` 6 / 6, 10 语言字符串 (21 键) 与 changelog / README / 说明书已补齐, `:app:lintDebug` 0 错误, release APK 2,801,108 B (P0 基线 2,749,948 B, +51,160 B), `verifyNativePageAlignment` 通过; 设备矩阵只覆盖 AVD API 24 (EPUB 2 / EPUB 3 样本经宿主与 instrumentation 打开), FXL 样本待 P2.4 引入, Sony API 28 / Xiaomi API 35 真机待下次会话补跑后再关闭 P1.5. 补跑 (2026-09-19 下午): Sony G8441 API 28 与 Xiaomi 23046RP50C API 35 instrumentation 均 23/23, Xiaomi 宿主联调通过 (主图标 + 溢出条目), P1.5 关闭; FXL 样本仍待 P2.4.

---

## P2: 阅读体验

目标: 交付 D5 的基线偏好全集与四项扩展中的三项 (搜索, FXL, 字体导入 + CJK 竖排), 以及书签, 手势, 链接与图片行为.

### P2.1 偏好面板与主题

- [x] (插件) `PreferencesPanel` (底部面板): 字号 (滑块 + 步进, 50%-300%), 字体族 (出版商默认 / 系统衬线 / 无衬线 / 内置开源字体若引入 / 已导入字体), 行距, 页边距, 段间距, 对齐, 连字符, 出版商样式开关, 列数 (自动 / 1 / 2, 横屏生效), 主题 (浅色 / 护眼 / 深色 / 跟随宿主), 分页 / 滚动; 修改即时 `submitPreferences`. 证据: `c1f719c`; 实名 `reader/PreferencesSheet` (`BottomSheetDialogFragment`, 布局 `sheet_preferences.xml`, 工具栏新增 `action_preferences` / `ic_tune_24`): 主题四档切换组 (浅色 / 护眼 / 深色 / 跟随宿主), 分页 / 滚动, 字号滑块 (50%-300%, 步进 10%) + `-` / `+` (0.1 步进), 字体族下拉 (出版商默认 / 衬线 / 无衬线 / 等宽 / OpenDyslexic / AccessibleDfA / IA Writer Duospace, 后三者为 Readium 自带), 页边距, 列数 (滚动时禁用), 出版商样式开关 + 提示, 行距 / 段间距 / 对齐 / 连字符 (Readium CSS 只在出版商样式关闭时应用, 修改任一项自动关闭出版商样式, 开启时以 50% 透明标示无效), 恢复默认; 滑块抬手时提交; 面板显示导航器解析后的 `EpubSettings`, 写回 `EpubPreferences`; FXL 时隐藏文字项只留主题 (P2.4 验证). 溢出菜单的 "滚动模式" 保留, 改写同一偏好.
- [x] (插件) `PreferencesStore`: `EpubPreferences` JSON 序列化 (Readium 序列化器) + 版本号字段, 损坏时回退默认; "恢复默认" 按钮; 主题 "跟随宿主" 映射到 `Theme.LIGHT / DARK` 并在宿主暗色切换时同步. 证据: `498b2b4` (编解码与存储) + `c1f719c` (接线); 实名 `prefs/PreferencesCodec` + `store/ReaderPreferencesStore` (`files/reader-preferences.json`, 信封 `{format: 1, themeMode, preferences}`, `preferences` 为 Readium `EpubPreferencesSerializer` 输出; 读取时白名单 24 键, 类型校验, 数值钳制到 `PreferenceRanges`, 枚举校验, 丢弃 `theme` 与未知键, 损坏回退默认), 写入经 `AtomicFiles`, 400 ms 去抖 + `onPause` / `onCleared` 冲刷; `ReaderPreferencesState` (Android 侧) 持有 `ThemeMode` + `EpubPreferences`, 提交导航器前按 `ThemeMapping.resolve(themeMode, hostDarkMode)` 补 `theme`; 宿主暗色切换经 `HostAppearanceActivity.recreate()` 后 `showReader()` 对保留的导航器重新 `submitPreferences`; 旧 `reader_settings.scroll_mode` 在文件不存在时迁移一次; "恢复默认" 即 `resetPreferences()` (回到 Readium 默认 + 跟随宿主).
- [x] (插件) 工具栏 / 系统栏配色随主题 (浅 / 护眼 / 深三套), 沉浸模式过渡无闪烁 (参考两个 Previewer 的 `PreviewerChromeColors` 对比度规则). 证据: `c1f719c`; `prefs/ReaderThemeColors` (栏底色 = 页面色向文字色混 6%, 前景取主题文字色且要求对比度 >= 4.5 否则黑 / 白, 次级前景 72%, 强调 55%, `lightBars` 按亮度决定系统栏图标深浅), `ReaderChrome.applyTheme` 统一刷新窗口背景 / 根视图 / 工具栏与菜单图标 / 进度面板 / 加载面板 / 状态栏与导航栏; 全 API 边到边 (`setDecorFitsSystemWindows(false)` + 根视图 insets 监听: 左右给根视图, 顶部给工具栏, 底部给进度面板与状态面板), API 24 / 25 亮色主题保留黑色导航栏 (系统无法绘制深色图标); 沉浸切换时栏与页面同色, 无异色闪烁 (AVD API 24 / Xiaomi API 35 目测).
- [x] (测试) JVM: `PreferencesCodecTest` (全字段往返, 未知字段忽略, 越界值钳制), `ThemeMappingTest`; instrumentation: 偏好跨进程重启持久化, 字号变化后 WebView `textZoom` / Readium CSS 变量断言. 证据: `498b2b4` / `c1f719c`; JVM `prefs/PreferencesCodecTest` (9: 24 键往返, 未知与 `theme` 键丢弃, 越界钳制, 类型错误丢弃, 字体族 / 语言修剪与上限, 损坏信封为 null, 损坏载荷逐字段回退, 信封键集, 默认判定), `prefs/ThemeMappingTest` (4), `prefs/ReaderThemeColorsTest` (5), `prefs/PreferenceRangesTest` (4), `store/ReaderPreferencesStoreTest` (5), 全量 85 / 85; instrumentation `EpubReaderPreferencesInstrumentationTest` (3: 字号 1.5 + 护眼主题到达 `EpubSettings`, chrome 配色, Readium CSS `--USER__fontSize` = `150%` (导航器默认 `useReadiumCssFontSize = true`, 不走 `textZoom`), 落盘并在重启后恢复, 恢复默认; 面板控件 `+` / `-` / 深色 / 滚动 / 两端对齐 (自动关闭出版商样式) / 恢复默认; 旧 `scroll_mode` 迁移), AVD API 24 26/26, Xiaomi 23046RP50C API 35 26/26, Sony G8441 API 28 26/26, 2026-09-19 11:24.

### P2.2 自定义字体导入

- [x] (插件) `FontStore`: 设置页 / 偏好面板 "导入字体" 经 SAF (`ACTION_OPEN_DOCUMENT`, `font/ttf` / `font/otf` / `application/x-font-ttf` / `*/*` + 扩展名校验), 校验 TTF / OTF 魔数与大小 (单个 20 MiB, 总数 10), 复制到 `files/fonts/<sha256>.ttf`, 记录显示名 (从 `name` 表读取, 失败用文件名); 删除字体; 字体经 `EpubNavigatorFragment.Configuration.fontFamilyDeclarations` + `servedAssets` 注入. 证据: `07a6a4f` (校验 / 目录 / 存储) + `ffcbd78` (接线); 实名 `fonts/FontFileValidator` (SFNT 签名 `00010000` / `true` / `OTTO`, 拒绝 `ttcf` 集合, 表目录或表越界即判截断, `name` 表 nameID 16 优先于 1, Windows 英语 > Windows 其它语言 > Unicode > Mac Roman, 名称清洗去控制字符与 CSS 定界符并截到 60 字符), `fonts/FontCatalog` (+ `FontCatalogCodec`: `files/fonts/index.json` 信封 `{format, fonts[]}`, 损坏条目丢弃; `FontFamilyNames`: 保留名 (CSS 通用族, CSS 关键字, Readium 自带三款) 与目录内重名追加 8 位哈希后缀并随条目固化; `FontLimits`: 单个 20 MiB, 最多 10 个), `store/FontStore` (临时文件边复制边哈希与限长, 校验后重命名为 `<sha256>.<ttf|otf>`, 同哈希去重, 删除, 缺文件条目自动剔除); 偏好面板 "导入字体" 经 `ActivityResultContracts.OpenDocument` (`font/ttf` / `font/otf` / `application/x-font-ttf` / `application/x-font-opentype` / `application/font-sfnt` / `*/*`, 文档显示名的扩展名预检), "管理字体" 列表点选后确认删除 (使用中的字体删除后偏好回到出版商默认), 导入后自动选中; 注入不走 `servedAssets` (它只经 `WebViewAssetLoader` 服务 APK assets), 改为 `book/FontsContainer` 以 `CompositeContainer` 挂在出版物容器之后按需从磁盘服务 `https://readium_package/fonts/<sha256>.<ext>`, `EpubNavigatorFragment.Configuration.addFontFamilyDeclaration` 用该绝对 URL 声明 `@font-face`; 声明在导航器创建时固定, 导入 / 删除后以 `lastLocator` 重建导航器, 面板经 `activeNavigator` 流切到新导航器的 `settings`.
- [x] (测试) JVM: `FontFileValidatorTest` (魔数, 大小, 名称表解析, 非法文件); instrumentation: 导入后偏好面板可选且正文字体变化 (WebView `document.fonts` 检查或截图对比). 证据: `07a6a4f` / `ffcbd78`; JVM `fonts/FontFileValidatorTest` (6: 家族名优先级, 平台回退, OTF / 旧 Apple 签名 / TTC / 非字体, 无 `name` 表, 截断, 名称清洗), `fonts/FontCatalogCodecTest` (5), `store/FontStoreTest` (11), 合成 SFNT 构造器 `SyntheticFonts`, 全量 107/107; instrumentation `EpubReaderFontsInstrumentationTest` (3: 导入 Readium 自带的 `iAWriterDuospace-Regular.ttf` 后目录 / 偏好 / 重建导航器的 `EpubSettings.fontFamily` 跟随, WebView `document.fonts` 中对应 FontFace 状态 `loaded` 且 `--USER__fontFamily` 与 body 计算样式含该族名, 重启后仍加载, 删除后偏好清空且文件移除; 重复 / 非字体 / TTC / 不存在的 URI 均不改目录; 面板列出导入字体, 选中后偏好与导航器跟随), AVD API 24 / Xiaomi 23046RP50C API 35 / Sony G8441 API 28 各 29/29, 2026-09-19 12:13-12:16; 真机 `document.fonts` 证据 三台设备 `document.fonts` 中的 FontFace `status=loaded`, `--USER__fontFamily` 与 body 计算样式均为 `"iA Writer Duospace (79e8378e)"`, 显示名 `iA Writer Duospace`, 文件 81,636 B, sha256 `79e8378e17d79ca6...`.

### P2.3 CJK 竖排与 RTL

- [x] (插件) 偏好 `verticalText` (自动 / 强制横排 / 强制竖排; 自动 = 出版物 `page-progression-direction` 与语言判断), `readingProgression` 跟随出版物 (RTL 书籍翻页方向与点按区镜像), 界面 RTL 布局 (阿拉伯语 UI) 与正文 RTL 独立. 证据: `99d5038`; 面板 "文字方向" 三态 (自动 / 横排 / 竖排 = `verticalText` null / false / true), 自动判断交给 Readium `EpubSettingsResolver` (语言 CJK 且阅读进程 RTL 即竖排; 阅读进程 = 偏好 > 语言偏好 RTL > 出版物 `page-progression-direction` > 语言 RTL > LTR, `Language.isRtl` 含 `zh-Hant` / `zh-TW`), 竖排时 Readium 强制 `scroll = true`, 面板禁用分页 / 滚动组并提示; `readingProgression` 不提供 UI, 跟随出版物 (P1 的 `PageTurnPolicy` 已按 `overflow.readingProgression` 镜像点按区); 界面方向来自 `HostAppearance.wrap` 的 `setLayoutDirection(宿主语言)` (`supportsRtl`), 与正文 `dir` / `page-progression-direction` 无关; 11 目录 5 键字符串, 10 语言 README / changelog / 说明书.
- [x] (测试) 日文竖排样本与阿拉伯语样本在 API 28 / 35 各截图存档 (`docs/images/evidence/`), 验收无破版, 翻页方向正确, 目录跳转正确; 中文样本在竖排下标点位置合理 (记录 Readium CSS 的实际表现, 不自研排版). 证据: `360dd61` (夹具) / `99d5038` (用例), 截图随本条目的文档提交入库; 生成器新增 `vertical-ja.epub` (ja, `page-progression-direction="rtl"`, 出版商 CSS `writing-mode: vertical-rl`), `vertical-zh.epub` (zh-Hant, rtl 进程, CSS 无书写模式), `rtl-ar.epub` (ar, `dir="rtl"`, rtl 进程), 旧夹具字节不变; `EpubReaderDirectionInstrumentationTest` (5: 日文样本 `verticalText` / `scroll` / `readingProgression = RTL` 且页面 `writing-mode` 竖排, 目录跳转第 3 章后 `goBackward` 回到第 2 章, 强制横排后设置跟随; 中文样本仅凭元数据竖排, 强制横排得 `horizontal-tb`, 回到自动再竖排; 阿拉伯语样本 RTL 且 `direction: rtl`, 目录往返, 界面方向与正文无关; 阿拉伯语界面 (无宿主时经 `AppCompatDelegate.setApplicationLocales`) chrome RTL 而英文书仍 LTR; `HostAppearance.wrap` 对 `ar` / `en` 的布局方向), AVD API 24 / Xiaomi 23046RP50C API 35 / Sony G8441 API 28 各 34/34, 2026-09-19 13:57-14:00; 截图 `docs/images/evidence/p23-{ja,zh,ar}-api{28,35}.png` 与 `p23-ui-ar-api28.png`, 日文 三台设备 `verticalText=true`, `scroll=true`, `readingProgression=RTL`, 页面 `writing-mode: vertical-rl`; 强制横排后 `verticalText=false`, `scroll=false`, 页面仍 `vertical-rl` (出版商 CSS); 竖排下 「」、。 位于字格右上, 无破版; 中文 `zh-Hant` + rtl 进程自动 `vertical-rl`, 强制横排 `horizontal-tb`, 回到自动再 `vertical-rl`; 竖排下 ，。： 位于字格右上 (Readium CSS 未做标点压缩, 如实记录); 阿拉伯语 `readingProgression=RTL`, `verticalText=false`, 页面 `dir="rtl"` / `direction: rtl`, 正文右对齐; 三台设备界面均为 LTR (`layoutDirection=0`), 与正文无关; 界面 AVD API 24 (无宿主) 与 Sony API 28 (宿主 `en`, `hostWins=false`) 界面语言 `ar`, `layoutDirection=1`, 工具栏镜像且进度以阿拉伯-印度数字显示, 英文书 `direction: ltr`; Xiaomi API 35 (宿主 `zh-Hans`, `hostWins=true`) 界面 LTR.

### P2.4 固定版式 FXL

- [x] (插件) FXL 出版物: `spread` 偏好 (自动 / 从不 / 总是), 横屏双页, 双指缩放与拖动 (Readium 内建), 页码显示改为 `第 x / N 页`, 隐藏对 FXL 无效的偏好 (字号 / 字体 / 行距 / 滚动). 证据: `caf2ebf`; `spread` 偏好三态 (自动 / 单页 / 双页 = null / `NEVER` / `ALWAYS`, `EpubPreferences` 拒绝 `AUTO`); Readium 3.4.0 的 `Spread.AUTO` 只当单页 (`EpubNavigatorFragment` 里 `DualPage.AUTO` 与 `OFF` 同分支, 源码 FIXME), 所以 "自动" 由 `EpubReaderActivity.effectivePreferences` 按方向解析 (横屏 `ALWAYS`, 竖屏 `NEVER`, 仅固定版式; 可重排书籍不设 `spread` 以免旋转时无谓重建分页器), Manifest 的 `configChanges` 保留 Activity, `onConfigurationChanged` 重新提交; 双指缩放与拖动是 Readium `R2FXLLayout` (`ScaleGestureDetector`, 1x-3x, 双击) 加 WebView `builtInZoomControls`, 插件未加代码; 进度栏经 `ProgressSnapshot.pages` 显示 `第 x / N 页` (`text_progress_page_of`, 10 语言), 位置服务对固定版式每页一个位置; 面板隐藏分页 / 滚动与全部文字偏好, 只留主题与双页组 + 提示, 溢出菜单隐藏 "滚动模式"; 11 目录 6 键新字符串 + 固定版式提示改写, 10 语言 README / changelog / 说明书.
- [x] (测试) FXL 样本在手机竖屏 / 横屏与 Xiaomi Pad 上验证; instrumentation 断言 FXL 时偏好面板隐藏项. 证据: `df61de4` (夹具 `fixed-layout.epub`: 6 版 600x800 视口, `rendition:layout` pre-paginated, 书脊 `page-spread-center` / `-left` / `-right`, 每版一色) / `caf2ebf` (用例); `EpubReaderFixedLayoutInstrumentationTest` (4: 竖屏单页翻页与 `第 x / 6 页` 标签, 目录跳转, 强制双页后 1 -> 2 -> 4, 回到自动再单页; 横屏自动双页 1 -> 2 -> 4 -> 2, 强制单页 2 -> 3, 再自动后落在 2-3 跨页 (定位器指左页 2) -> 4; 面板隐藏可重排组 / 分页组 / 标签并显示固定版式组与双页组, 点选写回偏好且导航器跟随, 菜单无 "滚动模式"; 经 `Activity.dispatchTouchEvent` 分发双指张开后 `R2FXLLayout.scale` > 1.2, 单指拖动后 `posX` 变化), AVD API 24 / Xiaomi 23046RP50C API 35 / Sony G8441 API 28 各 38/38, 2026-09-19 15:29-15:36; 截图 `docs/images/evidence/p24-fxl-{portrait,landscape,zoomed}-api{28,35}.png`; 手机 (Sony API 28) 竖屏 `Page 1 of 6`, 强制双页 `Page 4 of 6`, 横屏 2-3 跨页且 6 个 WebView 已附加, 缩放 2.13x, 拖动 219 px; Xiaomi Pad (API 35) 竖屏 `第 1 / 6 页` (宿主 zh-Hans), 强制双页 `第 4 / 6 页`, 横屏 2-3 跨页 (2880x1800 窗口) 且 6 个 WebView 已附加, 缩放 1.92x, 拖动 200 px; 缩放 `R2FXLLayout.scale` 1.0 -> 1.27 (API 24) / 2.13 (API 28) / 1.92 (API 35), 拖动后 `posX` 0 -> 194 / 219 / 200.

### P2.5 全文搜索

- [x] (插件) `SearchPanel`: 工具栏搜索入口, `Publication.search(query)` 迭代器分页加载 (每批 50, 上限 500 条), 结果按章节分组显示上下文 (`before` / `highlight` / `after`), 点击跳转并用 decoration 高亮命中, 上一处 / 下一处, 取消正在进行的搜索, 空结果与过短查询提示; 搜索在后台协程, 切换书籍时取消. 证据: `0d14146`; 实名 `reader/SearchSheet` (`BottomSheetDialogFragment`, 布局 `sheet_search.xml`, 工具栏 `action_search` / `ic_search_24`) + 底部搜索条 (`activity_epub_reader.xml` 的 `search_bar`: 上一处 / `第 x / N 处` / 下一处 / 关闭, 只在打开某条结果后显示); 纯 Kotlin `search/` 包: `SearchQueryPolicy` (修剪 + 折叠空白, 拉丁文至少 2 个码点, 单个汉字 / 假名 / 谚文即可), `SearchResultPager` (每批 50, 上限 500 后 `truncated`, 取消保留已到达项, `close` 释放迭代器), `SearchGrouping` (按 `href` 变化插章节头, 标题取 `Locator.title` 否则 `href`), `SearchSnippet` (每侧 48 字符, 词边界 + `...`), `SearchState` / `SearchSession` (`StateFlow`, 状态 IDLE / TOO_SHORT / SEARCHING / DONE / CANCELLED / FAILED / NOT_SEARCHABLE, `viewModelScope` 协程, `release()` 时 `detach` 取消); Readium `Publication.search(query)` 走 `StringSearchService` (ICU, 大小写与变音不敏感), 迭代器每次返回一个资源的全部命中, 由分页器切成 50 一批, 列表滚到尾部 8 行内自动续载; 点击结果 `go(locator)` (Readium 以 `text.highlight` 文本锚定滚到命中处), 同时把当前命中作为 decoration 组 `search` 的唯一一条应用 (`Decoration.Style.Highlight`, 琥珀色 `color_secondary`, `isActive` 加下划线; id 带序号, 换命中时旧页移除 / 新页添加), 上一处 / 下一处移动高亮, 关闭清空 decoration 与结果但保留查询词 (重开面板仍显示上次查询与状态); 固定版式只跳转不高亮 (Readium 3.4.0 只对可重排页渲染 decoration); 11 目录 15 键字符串, 10 语言 README / changelog / 说明书.
- [x] (测试) JVM: `SearchResultPagerTest` (分页, 上限, 取消); instrumentation: 样本关键字计数与跳转后 `currentLocator.href` 断言. 证据: `0d14146`; JVM `search/SearchResultPagerTest` (7: 多小批凑页, 大批拆页不重复拉取, 上限截断, 恰好上限也标截断, 空源, 取消保留已到达项且 `close` 释放源, 默认 50 / 500), `search/SearchSnippetTest` (5), `search/SearchGroupingTest` (3), `search/SearchQueryPolicyTest` (4), 全量 127 / 127; instrumentation `EpubReaderSearchInstrumentationTest` (4: `minimal-epub3.epub` 的 `lighthouse` 命中数与从夹具 XHTML 数出的期望一致 (2), 章节头 `Chapter 1` / `Chapter 3`, 打开末条后 `currentLocator.href` 为 `chapter3.xhtml`, 搜索条 `2 of 2`, 页面 `[data-group="search"] > div` >= 1, 上一处回到 `chapter1.xhtml` 且 `1 of 2`, 关闭后条隐藏 / IDLE / decoration 为 0; ` a ` 过短提示, `zeppelin` 空结果提示, 重开面板保留查询; `malformed-many-entries.epub` 的 `filler` 首批 50 (`结果: 50+`), 续载到 500 截断 (`只显示前 500 处结果`), 提交后立即取消得 CANCELLED 且不再变化, 再搜后打开首条到 `filler/0.xhtml`; `fixed-layout.epub` 的 `reef` 跳到 `page2.xhtml` 并显示搜索条), AVD API 24 与 Xiaomi 23046RP50C API 35 各 43 用例 (42 通过 + 1 条外部样本用例按 `Assume` 跳过), 2026-09-19 17:36-17:40; Sony G8441 API 28 46 用例 (45 通过 + 1 跳过, 含 P2.6 的 3 条书签用例, 以 `52fb62b` 构建) 21:19-21:21 (补跑: 首次门禁时手机被指纹锁屏, 且 `stay_on_while_plugged_in` 只对 USB 生效而该机报告 AC 充电, 2 min 熄屏后即锁); 截图 `docs/images/evidence/p25-search-{panel,hit}-api{24,28,35}.png`; 2000 页样本分页耗时 首批 50 条到达 AVD API 24 499 ms / Xiaomi Pad API 35 369 ms / Sony API 28 699 ms, 续载 10 批到 500 条上限累计 4,019 / 4,717 / 6,596 ms (Sony 为 21:19 补跑的数字, 16:54 旧构建为 835 / 6,229 ms); 本地真实书籍度量 (`EpubReaderExternalSamplesTest`, 需 runner 参数 `external=true`, 书不入库) mega-5000 (5001 章, 1.8 MB) 打开到首个定位 AVD API 24 15.8 s / Pad API 35 12.6 s / Sony API 28 20.6 s (5026 个位置, 耗时几乎全是位置计算), `chap` 首批 50 条 411 / 610 / 487 ms, 续载到 500 条上限 2.2 / 2.5 / 3.6 s, 跳转末条 0.6 / 0.3 / 0.7 s; mega-1000 打开 3.0 / 2.5 / 3.1 s; 《沟通的艺术》(16.5 MB, 73 章) 打开 1.1 / 1.0 / 1.2 s, `沟通` 首批 202 ms, 500 条 1.8 s, 跳转末条 1.2 / 0.7 / 1.9 s; 《福尔摩斯探案全集》(60.6 MB, 156 章) 打开 1.0 / 0.9 / 1.3 s, `福尔摩斯` 首批 205 ms, 500 条 1.8 s; 《JavaScript 函数式编程》(12.8 MB, 23 章) `函数` 500 条 1.0 s, 跳转末条 4.8 / 1.5 / 4.9 s (目标章只有 4 处命中, 耗时在页面本身); Gutenberg pg79599 (25.7 MB, 27 章, 法文) `the` 共 300 处 (含 thé 等变音匹配) 1.0 s 搜完, 打开 1.0 / 0.9 / 1.1 s; HowToLiveBetter (0.7 MB, 40 章) `life` 6 处 0.6-1.0 s; Galahit (351 MB 固定版式, 199 页, 仅 Pad) 打开 1.3 s, `the` 2 处 0.6 s, 跳到 page183 0.3 s (横屏跨页时定位器指邻页, 用例接受相邻页). Sony 补跑 (单条 decoration 构建 `52fb62b`, 21:22-21:25): mega-5000 打开 21.1 s (5026 个位置), `chap` 首批 488 ms, 500 条 3.8 s, 跳转末条 0.6 s; mega-1000 打开 2.9 s; 《沟通的艺术》打开 1.1 s, 首批 202 ms, 500 条 1.9 s, 跳转末条 1.4 s; 《福尔摩斯》打开 1.1 s, 首批 202 ms, 500 条 1.8 s, 跳转末条 (42 处) 2.2 s; 《JavaScript 函数式编程》500 条 1.0 s, 跳转末条 2.4 s; Gutenberg 打开 1.0 s, 300 处 1.0 s, 跳转末条 (204 处) 1.1 s; HowToLiveBetter `life` 6 处 0.8 s, 跳转 0.7 s.

### P2.6 书签

- [x] (插件) `BookmarkSheet`: 添加当前位置 (含章节名与文本片段), 列表 (时间倒序), 跳转, 删除, 全部清除; 存储 `bookmarks.json` (每本上限 500); 工具栏书签图标反映当前页是否已加书签. 证据: `52fb62b`; `reader/BookmarkSheet` (`BottomSheetDialogFragment`, 布局 `sheet_bookmarks.xml` + 行布局 `item_bookmark.xml`: 章节名 / 文本片段 / 时间 / 删除图标; 头部 `为本页添加书签` (当前页已有书签时禁用) 与 `全部清除` (`AlertDialog` 确认并显示条数), 空态文案), 溢出菜单 `action_bookmarks` 打开面板, 工具栏 `action_bookmark` 图标切换当前页书签 (`ic_bookmark_border_24` / `ic_bookmark_24`, 标题 `添加书签` / `移除书签`, 由 `currentBookmark` 流驱动 `invalidateOptionsMenu`, 添加 / 移除 / 达上限各有 Toast); 书签定位器 = 导航器 `currentLocator` (页内 progression / position / totalProgression / 目录标题) + `firstVisibleElementLocator()` 给出的首个可见元素 `cssSelector` 与文本引用 (`text.highlight`, 折叠空白后取 200 字符), 跳转 `go(locator)` 时 Readium 先按文本锚定再退回 progression; 章节名取 `Locator.title` -> 目录匹配 -> 固定版式 `第 x / N 页` -> 文件名, 片段 `BookmarkPolicy.snippet` (120 字符, 词边界 + `...`); "当前页是否已加书签" 由纯 Kotlin `reader/BookmarkPolicy.onPage` 判定: 同资源下分页模式比较书签 progression 映射到当前版面的页号 (`(p * numPages).roundToInt()`, 与 Readium `loadLocator` 同式, 换字号后按新版面比较), 滚动模式比较 position, 固定版式按资源; 当前页由 `PaginationListener.onPageChanged` (可重排) 与 `currentLocator` (固定版式) 更新; 存储 `store/Bookmark` + `BookmarkCodec` (`bookmarks.json`: `{format: 1, bookmarks: [{id, locator, createdAt, chapter?, snippet?}]}`, 解码跳过损坏 / 重复 id / 缺 href 或 type 的条目, 标签截到 400 字符, 上限 500), `BookDataStore.readBookmarks / writeBookmarks` (整文件原子写, 空列表删文件), 临时键 -> 完整指纹迁移时两边并集 (目标保留 id, 来源按位置去重后追加新 id, 超限按时间保留最新; 一侧为空时原样移动), `EpubReaderViewModel` 开书时随进度读入, 增 / 删 / 清在内存改后整文件异步写 (`persistScope` + `storeMutex`, 写失败只丢这次), 达 500 时拒绝 (`BookmarkAddResult.Full`); 11 目录 11 键字符串, 10 语言 README / changelog / 说明书.
- [x] (测试) JVM: `BookmarkStoreCodecTest`; instrumentation: 添加 / 跳转 / 删除 / 重启后保留. 证据: `52fb62b`; JVM `store/BookmarkCodecTest` (9, 即路线图所称 `BookmarkStoreCodecTest`: 往返, 信封键, 空表, 外来格式 / 不可读为 null, 损坏 / 重复 / 不完整条目跳过与标签截断, 解码上限 500, 访问器, 并集保留主 id / 追加新位置 / 跳过已知位置, 并集超限保留最新), `reader/BookmarkPolicyTest` (9: `pageOf` 取整与钳位, 分页按当前版面页号, 单页与固定版式按资源, 滚动按 position, 当前页取首个, 列表时间倒序且同刻按 id, 组合定位器不改原对象 / 无元素时去掉 text, 片段折叠与词边界), `store/BookDataStoreTest` +4 (整文件写 / 空表删文件, 损坏读为空, 迁移并集, 迁入无书签目标与损坏来源), 全量 149 / 149; instrumentation `EpubReaderBookmarksInstrumentationTest` (3: `minimal-epub3.epub` 工具栏切换添加 (章节 `Chapter 1`, 片段与 `text.highlight` 非空, 标题变 `移除书签`) 再切换移除, 再加 chapter1 并跳到 chapter3 加第二条, 面板两行时间倒序且 `bookmark_add` 禁用, 点旧行回到 `chapter1.xhtml` 且图标填充, 面板删一条, `bookmarks.json` 落盘, 结束后重开: 书签仍在且恢复页显示填充图标, 全部清除后空态与文件删除; 预写 500 条到落盘文件后重开: 全部读入, `addBookmark` 返回 `Full`, 切换不增, 面板 500 行最新在前, 删一条后可再加; `fixed-layout.epub` page1 按资源加书签 (无片段), 跳到 page2 图标为空, 面板跳回 page1 图标填充), AVD API 24, Redmi 12C 22120RN86C API 33 与 Xiaomi 23046RP50C API 35 各 46 用例 (45 通过 + 1 条外部样本用例按 `Assume` 跳过), 2026-09-19 18:46-18:53; Sony G8441 API 28 46 用例 (45 通过 + 1 跳过) 21:19-21:21 (解锁后补跑); 截图 `docs/images/evidence/p26-bookmarks-{page,panel}-api{24,28,33,35}.png`; 添加耗时 (切换到书签落入内存, 含首个可见元素脚本) AVD API 24 102 ms / Sony API 28 101 ms / Redmi API 33 103 ms / Pad API 35 101 ms (`minimal-epub3.epub` 第一章, 可见元素 `#chapter-1`).

### P2.7 手势, 按键, 链接与图片

- [x] (插件) `PageTurnController`: 点按区可配置 (关闭 / 左右 / 上下), 音量键翻页开关, 硬件键盘方向键与空格翻页; 长按选择文本后的系统 ActionMode 增加 "复制" / "分享" / "搜索" (`ACTION_WEB_SEARCH`) / "翻译或处理" (`ACTION_PROCESS_TEXT`, 存在时). 证据: `1dbfd0b`; `reader/PageTurnPolicy` 重写 (`resolveTap` 按 `TapZones` 关闭 / 左右 / 上下 三分, 左右在 RTL 镜像, 关闭只切换 chrome; `resolveKeyboard` 以 W3C `KeyboardEvent.code` 映射 ArrowLeft / ArrowRight (RTL 镜像), PageUp / PageDown, Space (Shift 反向), ArrowUp / ArrowDown (滚动模式下交回页面); `keyboardCode` 把 Android keycode 译成同一套 code), `store/ReaderSettings.tapZones` (`tap_zones`), 菜单 `action_tap_zones` 单选子菜单 (关闭 / 左右 / 上下); 键盘翻页在 `EpubReaderActivity.dispatchKeyEvent` 截获, 焦点视图 `onCheckIsTextEditor()` 为真 (搜索框, 页内表单) 时放行 (附带发现 1); `reader/SelectionActions` (分享 / 网页搜索 / `ACTION_PROCESS_TEXT` intent, 处理器最多 4 个) + `SelectionActionMode` (`EpubNavigatorFragment.Configuration.selectionActionModeCallback`, `menu_text_selection`: 复制 / 分享 / 网页搜索 + 每个处理器一项; 选中文本在工具条出现与刷新时经 `currentSelection()` 记下, 点击时先取活选区再取记下的), 复制在 API < 33 时 Toast; 图标 `ic_copy_24` / `ic_share_24` / `ic_web_search_24` (`colorControlNormal` 着色); 11 目录 9 键字符串; 音量键翻页开关沿用 P1.2.
- [x] (插件) `LinkPolicy`: 书内链接在本 Activity 内跳转并压入返回栈 (返回键先回到跳转前位置), 脚注 / 尾注链接 (EPUB 3 `epub:type=noteref` 或目标为 `aside`) 弹出对话框显示注释内容, 外部 `http(s)` 链接按 D25 处理 (设置项二选一: 确认后打开为默认, 或直接交给浏览器), `mailto:` / `tel:` 交给系统, 其它 scheme 拒绝. 证据: `1bc0fae`; `reader/LinkPolicy` (`classifyExternal`: `http` / `https` -> Web, `mailto` / `tel` -> System, 其它 (含 `javascript:` / `file:` / `intent:` / `content:` / `data:`) -> Rejected) 与 `LinkHistory` (上限 20, 后进先出), `EpubReaderViewModel.linkHistory` (释放时清空), `EpubReaderActivity.shouldFollowInternalLink` (`FootnoteContext` -> `text_note` 对话框, `HtmlCompat` 渲染 Readium 已用 Jsoup 清洗的注释; 其它书内链接把当前 `Locator` 压栈后 `jumpTo`, 返回 false), `OnBackPressedCallback` 只在栈非空时启用 (弹栈跳回, 空栈时返回键照旧退出), `onExternalLinkActivated` -> `openExternalLink` (Web 按 `external_links_direct` 直开或先确认, System 直接 `ACTION_VIEW`, Rejected 提示 `text_cannot_open_link`), 菜单 `action_external_links_direct`; 11 目录 3 键字符串 (`text_external_links_direct`, `text_note`, `dialog_button_close`). 两处上游限制 (附带发现 2 / 3): Readium 3.4.0 在回调前已把 URL 解析为清单 `Link` (fragment 丢失), 书内链接落在目标资源开头, 目标为 `aside` 的非 `noteref` 链接无法识别; `AbsoluteUrl` 只接受层级 URL, `mailto:` / `tel:` 不会到达回调 (WebView 自行处理并显示错误页), System 分支只经 `openExternalLink` 可达, changelog 因此不宣称 mailto / tel.
- [x] (插件) 图片: `TapEvent.targetElement` (3.4.0 实验 API) 命中图片时打开 `ImageViewerDialog` (缩放 / 保存到相册不做, 只查看); API 不可用时降级为无动作. 证据: `5440ba4`; `reader/ImageViewerDialog` (`DialogFragment`, `Theme_Black_NoTitleBar_Fullscreen`, `dialog_image_viewer.xml`: 黑底 `fitCenter` 图 + 底部说明条 (`image_caption_scrim`, 根布局 `fitsSystemWindows` 避开手势导航栏), 点按任意处关闭, 说明文字取 `Content.ImageElement.text` (figcaption, 否则 alt), 读不到时显示 `text_image_unavailable`), `reader/ImageDecoding` (`inSampleSize` 取 2 的幂使长边不超过屏幕长边, 下限 1024), `EpubReaderViewModel.decodeImage` (IO 线程 `publication.get(url)` 读字节), `EpubReaderActivity.onTap` 先看 `event.targetElement?.content as? Content.ImageElement` (`@ExperimentalReadiumApi`), 命中即打开并消费点按; 固定版式的 `targetElement` 恒为 null (Readium 只为可重排页构建), 降级为普通点按; 11 目录 2 键字符串.
- [x] (测试) JVM: `LinkPolicyTest` (内链 / 外链 / 脚注 / 危险 scheme), `PageTurnPolicyTest`; instrumentation: 内链跳转与返回, 脚注对话框, 音量键翻页. 证据: JVM `reader/PageTurnPolicyTest` (7: 原 4 条 + 关闭 / 上下 / RTL 镜像, 键盘 code 映射含滚动模式与 Shift, keycode 映射), `reader/LinkPolicyTest` (4: web, 系统, 9 种拒绝的 scheme, 历史栈顺序与上限), `reader/ImageDecodingTest` (3), 全量 159 / 159; instrumentation `EpubReaderControlsInstrumentationTest` (3: 点按区 关闭只切换 chrome 且页面不动, 上下 (下 = 下一页, 上 = 上一页), 左右; 键盘 方向键 / 空格 / PageUp / PageDown 在无焦点与 WebView 有焦点时都翻页 (`sendKeyDownUpSync`, 首章 2 页所以 ArrowRight 按 2 次); JS 选中首段后 `performSelectionAction(selection_copy)` 落入剪贴板, 网页搜索 / 分享 / 文本处理 intent 的 extra 正确), `EpubReaderLinksInstrumentationTest` (3: 页内 JS 点击 `chapter3` 链接跳转 (AVD API 37 289 ms / Sony API 28 315 / Redmi API 33 422 / Pad API 35 257), 返回键回到 chapter1 (259 / 276 / 371 / 240 ms), 栈空后再按返回退出; `noteref` 点击弹出注释 (204 / 137 / 216 / 105 ms, 页面与栈不变); 外链 确认后打开 / 直开 / mailto / tel 经阻塞 `ActivityMonitor` 计数 4 次, `intent:` / `file:` (经 `onExternalLinkActivated`) 与 `javascript:` (经 `openExternalLink`) 拒绝且不弹框), `EpubReaderImagesInstrumentationTest` (2: 点击 `img` 打开查看器 (91 / 102 / 160 / 73 ms, href `OEBPS/images/beacon.png`, 说明文字为 alt `A small beacon glyph`), 关闭后页面不动; 固定版式点击不打开); 音量键翻页由 `EpubReaderProgressInstrumentationTest` 继续覆盖; 四台设备全量 connected 各 54 用例 (53 通过 + 1 跳过) 22:33-22:44 (会话记录有明细); 截图 `docs/images/evidence/p27-{links-note,images-viewer}-api{28,33,35,37}.png`.

验收: 三种样本在 API 28 / 35 上完成偏好 / 字体 / 竖排 / FXL / 搜索 / 书签 / 链接全流程; 新增字符串覆盖 10 语言; JVM 与 instrumentation 全绿.

---

## P3: TTS 朗读

目标: 从当前位置开始朗读, 句级高亮跟随, 熄屏可继续, 退出阅读器即停止.

- [x] (插件) `TtsController`: `readium-navigator-media-tts` 的 `TtsNavigator` + `AndroidTtsEngine`, 播放 / 暂停 / 上一句 / 下一句, 语速 / 音调, 语音与语言选择 (系统 TTS 引擎可用语音列表, 默认跟随出版物语言), 当前句 decoration 高亮并自动翻页跟随, 到达书末自动停止; 无可用引擎或语言不支持时给出安装 / 设置引导. 证据: `737a27a`; `tts/TtsController` (驻留 `EpubReaderViewModel`: `start` / `play` / `pause` / `previous` / `next` / `stop`, 偏好经 `tts/TtsPreferencesStore` 持久化到 `files/tts-preferences.json`), `tts/TtsSession` (包装 Readium `TtsNavigator`, 主线程状态 / 位置 / 事件流, 书末 `Ended` 与引擎错误映射), `tts/SystemTtsEngine` (改编自 Readium `AndroidTtsEngine`, BSD-3 版权头保留: 无参 `TextToSpeech` 失败时按包名显式绑定已装引擎, 见会话记录附带发现 1), `tts/TtsSheet` (语速 50-300%, 音调 50-200%, 语言 (自动 = 句子 / 出版物语言) 与语音 (引擎默认或按 `tts/TtsVoicePolicy` 排序: 精确区域, 离线优先, 质量, 网络语音标注), 系统 TTS 设置与语音数据安装入口), 当前句 decoration (`tts` 组, `Highlight` 主色) 与每秒至多一次的 `go(locator, animated = false)` 翻页跟随, 书末自动停止并提示, 无引擎 -> 对话框引导到系统 TTS 设置, 缺语音数据 -> 对话框引导 `requestInstallVoice`, 无文本 / 网络 / 引擎错误 -> toast; 手动翻页与目录 / 书签 / 链接跳转即停止朗读.
- [x] (插件) `TtsForegroundService` (`foregroundServiceType="mediaPlayback"`, 不导出) + MediaSession 通知 (播放 / 暂停 / 上一句 / 下一句 / 停止), 音频焦点与耳机按键; 朗读开始时前台化, 停止 / Activity 销毁 / 错误时立即停止并释放; Android 13+ 首次朗读前请求 `POST_NOTIFICATIONS`, 拒绝后仍可朗读但无通知 (记录行为); 权限豁免理由写入 `AGENTS.md` 红线小节 (D15). 证据: `737a27a`; `tts/TtsForegroundService` (media3 `MediaSessionService`, 不导出, `mediaPlayback`, `DefaultMediaNotificationProvider` 通知 2001 / 渠道 `read_aloud`; 上一句 / 下一句 / 停止为自定义 `SessionCommand` 按钮, 因为 Readium 的 media3 适配器不声明 seek 命令; 耳机上一曲 / 下一曲映射到句子; 音频焦点由 Readium 适配器处理: 永久抢占暂停且不自动恢复, 短暂抢占继续朗读), 会话 detach 即 `stopForeground(REMOVE)` + `stopSelf`, `TtsController.stop` / 视图模型清理 / 书末 / 错误都走同一路径; API 33+ 首次朗读前请求 `POST_NOTIFICATIONS` 一次 (`ReaderSettings.readAloudNotificationAsked`), 拒绝后照常朗读并 toast 说明无通知控制; Manifest 新增权限恰为 D15 三项, `<queries>` 增加 `TTS_SERVICE` / `INSTALL_TTS_DATA` (API 30+ 引擎可见性), `AGENTS.md` 第 6 节记录豁免, `PluginContractInstrumentationTest` 权限集合与服务审计同步; `media3-session` 1.11.0 直接声明 (`THIRD_PARTY_NOTICES.md`), release APK 3,698,482 字节 (P2.7 末 3,298,525, +400 KB, 已超出 P7 的 3.3 MB 预算, 见会话记录).
- [x] (插件) 睡眠定时器 (15 / 30 / 60 分钟 / 本章结束) 与朗读时屏幕常亮开关; 偏好持久化. 证据: `89d8859`; `tts/SleepTimer` + `SleepTimerPolicy` (关闭 / 15 / 30 / 60 分钟 / 本章结束, 剩余分钟向上取整), 定时器住在 `TtsSession` (固定时长 `delay`, 本章结束等待 `location.href` 变化, 到期发 `SleepTimerEnded` 由持有者停止并 toast), 面板显示剩余分钟 (15 s 刷新), 每次会话单独设置; `ReaderSettings.readAloudKeepScreenOn` 只在朗读期间给窗口加 `FLAG_KEEP_SCREEN_ON`; D26 `ReaderSettings.readAloudInBackground` (默认关): 开启且播放中时 `EpubReaderViewModel.onCleared` 把会话与书籍 (`tts/ReadAloudHandle` / `OrphanBook`: `Publication` + `PfdResource`) 托管给服务, 服务收事件 / 定时器 / 通知停止并在停止时写入进度, 通知点击 `EpubReaderActivity.ACTION_RESUME_READ_ALOUD` (新任务) 重开阅读器领回会话与同一 `Publication` 并从朗读句打开, 经宿主重开同一本书 (`bookKey` 相同) 时 `openBook` 也领回; 两个开关持久化在 `reader_settings`, 面板新增定时器选择器与两个开关 (10 语言).
- [x] (测试) JVM: `TtsVoicePolicyTest` (语言匹配与回退), `SleepTimerPolicyTest`; 真机 (API 28 / 33 / 35): 朗读 3 分钟, 熄屏继续, 通知控制, 来电 / 其它音频抢占后恢复, 退出阅读器停止; 记录设备与系统 TTS 引擎名称. 证据: JVM `tts/TtsVoicePolicyTest` (5), `tts/SystemTtsEngineTest` (3), `tts/SleepTimerPolicyTest` (5), 全量 172 / 172; instrumentation `EpubReaderTtsInstrumentationTest` (6: 朗读 + 高亮 + 暂停 / 播放 / 上一句 / 下一句 + 通知 + 停止释放 + 语速落盘, 逐句进入第 2 章时页面跟随 + 手动跳转停止, 关闭阅读器停止服务, 熄屏 3 分钟 (每分钟句子前进计数), 音频焦点短暂 / 永久抢占与播放恢复, 通知动作 暂停 / 播放 / 下一句 / 上一句 / 停止), `EpubReaderReadAloudBackgroundInstrumentationTest` (5: 定时器固定时长 (缩短 4 s) 与本章结束, 屏幕常亮标志, 后台继续 + 重开同书领回, 通知重开 + 无会话关闭, 开关关闭时关闭即停止); 设备与引擎: Redmi 12C API 33 与 Xiaomi Pad API 35 `com.xiaomi.mibrain.speech` (显式绑定, 4 个语音 en / zh / zh-Hans / zh-Hant), AVD API 37 `com.google.android.tts` (473 个语音), Sony G8441 API 28 Google TTS (图案锁未解锁, 待补跑); 明细见会话记录.

验收: 三台真机朗读全流程通过; Manifest 新增权限仅 D15 三项; 10 语言字符串补齐. 验收结论 (2026-09-20): Redmi 12C API 33 与 AVD API 37 两阶段朗读全流程通过, Xiaomi Pad API 35 通过第一阶段 (熄屏用例把它锁进安全锁屏后余下用例与第二阶段待用户解锁补跑), Sony API 28 处于图案锁待补跑; Manifest 新增权限恰为 D15 三项 (契约用例断言); 37 个新字符串覆盖 10 语言 (11 目录). Sony G8441 API 28 补跑 (2026-09-20): `EpubReaderTtsInstrumentationTest` + `EpubReaderReadAloudBackgroundInstrumentationTest` + 契约 17 / 17 通过, 全量门 65 条中 1 条书签上限用例超时 (单独重跑通过, 记为偶发); Pad 仍待解锁.

---

## P4: 独立应用形态

目标: 不经宿主也能打开 EPUB, 并具备设置页, 发行历史与更新检查.

### P4.1 Launcher 与最近书籍

- [x] (插件) `LauncherActivity` (`MAIN` / `LAUNCHER`): 最近书籍网格 (封面缩略图 / 书名 / 作者 / 进度百分比 / 最后阅读时间, 上限 100), "打开 EPUB" 经 `ACTION_OPEN_DOCUMENT` (`application/epub+zip` + `application/octet-stream` 兜底, `EXTRA_MIME_TYPES`), `takePersistableUriPermission`, 长按移除记录并 `releasePersistableUriPermission`; 授权失效 (文件被删 / 移动) 时标记不可用并允许清理; 空状态引导. 证据: `37479c4` (build 43); `launcher/LauncherActivity` (`MAIN` / `LAUNCHER`, 导出且不带权限, 不接收数据) 网格 (`RecentBooksAdapter`: 封面 / 书名 / 作者 / 进度 / 最后阅读时间, 空状态引导), `打开 EPUB` 经 `ACTION_OPEN_DOCUMENT` (`application/epub+zip` + `application/octet-stream` 兜底, `EXTRA_MIME_TYPES`), 选择后 `takePersistableUriPermission` 成功才入列 (失败时提示并只打开一次), 长按菜单 打开 / 从列表移除 (释放授权), 授权失效或文件不可读时标记不可用并可移除; 以自家显式动作 `ACTION_OPEN_RECENT` 打开 `EpubReaderActivity`, 阅读器回填指纹 / 书名 / 作者 / 封面 / 进度. JVM 180 / 180, lint 0 错误 / 45 警告, release APK 3,748,990 B; `EpubReaderLauncherInstrumentationTest` (4) 在 Sony G8441 API 28, Redmi 12C API 33 (两台均无宿主) 与 AVD API 37 各 4 / 4: 预置条目经 `ACTION_OPEN_RECENT` 打开, 阅读后条目补齐 `title=Minimal EPUB 3`, 作者, 指纹与 `progression=0.333` (夹具无封面, `cover=absent`), 移除后授权释放并显示空状态, 不可读文件标记不可用, 同进程 provider 无法持久授权时只打开不入列; 截图 `docs/images/evidence/p41-launcher-api{28,33,37}.png`.
- [x] (插件) `RecentBooksStore`: 持久 URI + 显示名 + 指纹 + 封面缩略图 (`files/covers/<指纹>.webp`, 最长边 512) + 元数据快照; 只有 Launcher 路径写入 (Explorer / ACTION_VIEW 路径不持久化第三方授权, 见 D4); 封面由 `CoverExtractor` 后台生成. 证据: `37479c4`; `launcher/RecentBooksStore` (`files/recent-books.json`, `RecentBooksCodec` 信封, `RecentBooksPolicy` 最新在前 / 同 URI 合并 / 上限 100 LRU), 封面 `files/covers/<URI SHA-256 前 32 位>.webp` (最长边 512, `CoverExtractor` 后台生成); 只有启动器在持久授权成功后 `upsert`, Explorer / `ACTION_VIEW` 入口只 `update` 已有条目 (D4).
- [x] (测试) JVM: `RecentBooksCodecTest`, `RecentBooksLimitTest`; instrumentation: 文档选择器返回后出现在列表, 重启后仍可打开, 移除后授权释放. 证据: `37479c4`; `RecentBooksCodecTest`, `RecentBooksLimitTest` (JVM); instrumentation 见第 1 条 (选择器的结果处理函数直接调用, 重建启动器后磁贴刷新, 移除后 `releasePersistableUriPermission`).

### P4.2 ACTION_VIEW 入口

- [x] (插件) `ExternalViewerActivity`: intent-filter `ACTION_VIEW` + `content` scheme + `application/epub+zip` (D27: 不加 `application/octet-stream` + pathPattern 兜底); `EpubRequestPolicy` 区分三条入口 (Explorer 信封 / 外部 ACTION_VIEW / Launcher 持久授权) 的校验规则, 外部入口只接受 `content://` 且必须带读授权, 不接受 `file://`; 打开后行为与 Explorer 入口一致, 但不写入最近书籍 (除非用户在溢出菜单选择 "加入最近书籍", 此时尝试 `takePersistableUriPermission`, 失败则提示). 证据: `62d12aa` (build 44); `ExternalViewerActivity : EpubReaderActivity` (导出, 无权限, 过滤器 `ACTION_VIEW` + `content` + `application/epub+zip`), `EpubRequestPolicy` (Android-free `RequestShape` / `RequestAdmission`; 三条入口: Explorer 信封需前缀授权与两项 ClipData, 启动器动作需显式组件且无 ClipData, 外部入口拒绝 `file://`, 无读授权, query / fragment, `tree/<id>` 与目录 MIME), 显示名先问 provider 的 `_display_name`; 溢出菜单 `加入最近书籍` 在 `takePersistableUriPermission` 成功后入列, 失败提示. JVM 185 / 185, lint 0 错误 / 45 警告, release APK 3,757,654 B; `EpubReaderExternalViewInstrumentationTest` (3, 以 `UiAutomation.executeShellCommand` 的 `am start` 扮演其它应用): AVD API 37 3 / 3, Redmi API 33 与 Sony API 28 各 2 通过 + 1 `Assume` 跳过 (shell 对 `com.android.externalstorage.documents` 的持久授权被拒, `shell-grant=denied`); 三台均 `entry=EXTERNAL listed=false addToRecent=refused` (同进程 provider 不可持久), 无授权 / `file://` / 指向查看器的 Explorer 信封均拒绝, 指向阅读器的 `ACTION_VIEW` 被系统以 PLUGIN 权限拒绝 (`view-at-reader=denied-by-system`); AVD 上夹具复制到 Download 后 `加入最近书籍` 入列 `persisted=true`, 启动器重开 `reopened-from-launcher=true`; 契约 (6) 与启动器 (4) 用例三台通过; 手工 `ACTION_VIEW` 截图 `docs/images/evidence/p42-view-api37.png`.
- [x] (测试) JVM: `EpubRequestPolicyTest` (三条入口交叉: Explorer 信封不得走外部规则, 外部 Intent 不得伪装 Explorer, 缺授权 / 错 scheme / 目录 URI 拒绝); instrumentation: 用 `adb shell am start -a android.intent.action.VIEW -d content://... -t application/epub+zip` 从测试 `FileProvider` 打开. 证据: `62d12aa`; `EpubRequestPolicyTest` (9, 取代 `EpubReaderIntentPolicyTest`); instrumentation 见上 (debug 的 `EpubReaderTestContentProvider` 为此导出只读并回答 `_display_name` / `_size`).

### P4.3 设置页, 发行历史与更新检查

- [x] (插件) `SettingsActivity` (跟随宿主外观, 无宿主时跟随系统): 默认阅读偏好 (进入 P2.1 面板的全局默认), 翻页行为 (点按区 / 音量键), 朗读默认 (语速 / 语音 / 定时器), 外部链接策略, 数据管理 (清除进度 / 书签 / 最近书籍 / 字体 / 封面缓存, 各自显示占用), 关于 (版本 / 作者 / 许可证 / 第三方声明), 发行历史, 检查更新; 从宿主插件中心与阅读器溢出菜单均可进入. 证据: `9e5edec` (build 45); `settings/SettingsActivity` (`HostAppearanceActivity`, 不导出, 从启动器菜单与阅读器溢出菜单进入; 宿主插件中心没有插件设置动作, 记录为偏差): 阅读 (主题模式写入 `reader-preferences.json`, `恢复阅读偏好` 删文件; 字体 / 字号 / 间距 / 版式仍由阅读器内面板负责, 该面板与阅读器耦合且 D14 已使其编辑全局默认, 不重复实现), 翻页 (点击区域 / 音量键), 朗读 (语速 / 音调写入 `tts-preferences.json`, 默认睡眠定时 `reader_settings.read_aloud_sleep_timer` 在朗读开始时布防, 常亮 / 后台; 语音仍在朗读面板按语言选择), 链接, 数据 (进度与书签 / 最近书籍与封面 (逐条释放授权) / 导入字体 / 偏好与设置, 各显示条数与占用, 确认后清除), 关于 (版本 / 构建号 / 日期 / Readium 版本 / 作者 / MPL-2.0 / 第三方声明 / 源码, 经 `ACTION_VIEW` 打开), 发行历史, 检查更新 (显示上次检查与忽略的版本); 阅读器 `onStart` 经 `reloadPreferences` 重读两个偏好文件. JVM 201 / 201, lint 0 错误 / 61 警告 (较 P4.2 +16: `text_update_checking` 的 `...` 11 处, `%d books / fonts` 复数候选 4 处, 行布局 overdraw 1 处, 启动器菜单多于一项后的 `showAsAction=always` 提示 1 处, `text_settings` 不再未用 -1; 均与既有同类), release APK 3,841,934 B; `EpubReaderSettingsInstrumentationTest` (4) 在 Sony API 28 / Redmi API 33 / AVD API 37 各 4 / 4: 开关行翻转 (音量键 / 常亮 / 外部链接), 点击区域 `VERTICAL`, 默认睡眠 30 分钟, 主题 `SEPIA`, 语速 150% / 音调 80% 落盘; 截图 `docs/images/evidence/p43-settings-api{28,33,37}.png`; 契约用例断言两页不导出 (6 / 6 三台).
- [x] (插件) `ReleaseHistoryActivity`: 按当前 locale 选择 `doc/CHANGELOG-{tag}.md`, 回退英语, 失败显示本地化错误 (Three-Stone-AI 形态). 证据: `9e5edec`; `settings/ReleaseHistoryActivity` (不导出) 经 `ReleaseHistory.kt` 按 locale 选择 `doc/CHANGELOG-{tag}.md` 并回退英语, `settings/MarkdownLite` (Android-free: 标题 / 项目符号 / 段落 / 分隔线; 行内代码 / 粗体 / 链接) + `MarkdownRenderer` 渲染为 Spanned (只有 `https` 链接可点击, 无浏览器时提示), 失败显示 `release_history_unavailable`; 旧的对话框实现移除. 三台设备渲染 6602 字符 (首行 `Release History`).
- [x] (插件) `AppUpdateCoordinator` / `AppUpdateRepository` / `AppVersionPolicy`: GitHub Releases API (`SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader`), 超时 / 取消 / 失败提示 / 忽略版本 / 频率限制 (每日一次) / 计量网络下不自动检查, 不做自动检查 (D28); 更新对话框 Neutral 按钮 = 内置发行历史, Positive = 打开发布页, 不下载 APK. 证据: `9e5edec`; `update/AppUpdateRepository` (`https://api.github.com/repos/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases/latest`, 10 s 超时, 不跟随重定向, 256 KB 上限, 404 = 尚无发行, 取消经 `disconnect()`), `ReleaseInfoCodec` (只接受 `https://github.com/` 发布页, 拒绝草稿, 说明截断 4000), `AppVersionPolicy` (semver, `v` 前缀, 预发布, 忽略判定), `UpdateSchedulePolicy` (手动每日一次, 24 h 内重放缓存, 时钟回拨容忍; `automaticCheckAllowed` 含计量网络规则但自动检查关闭, `AUTOMATIC_CHECKS_ENABLED = false`), `AppUpdateCoordinator` (可取消的进度对话框; 新版本对话框 Positive = 打开发布页, Neutral = 内置发行历史, Negative = 忽略此版本 / 取消忽略; 已最新 / 尚无发行 / 失败为 toast; 状态存 `reader_settings`); 不下载 APK. 三台设备以 `sourceOverride` 注入的假源: v9.9.9 对话框, 忽略后取消忽略, 一天内复用缓存 (fetches 不增), 网络失败不改状态, v0.0.1 视为最新 (`settings-update-api<N>.txt`).
- [x] (测试) JVM: `AppVersionPolicyTest` (语义化比较, 预发布, 忽略版本), `UpdateSchedulePolicyTest` (频率, 计量网络), `ReleaseHistoryTest` (locale 候选); instrumentation: 设置项持久化, 发行历史打开, 数据清除后 store 为空. 证据: `9e5edec`; JVM `AppVersionPolicyTest` (5), `UpdateSchedulePolicyTest` (3), `ReleaseInfoTest` (4), `MarkdownLiteTest` (4), `ReleaseHistoryTest` (2, 既有); instrumentation `EpubReaderSettingsInstrumentationTest` (4): 设置项持久化, 发行历史打开, 数据清除后 `books=0 recent=0` 且无偏好文件, 更新检查全流程.

验收: 无宿主的裸设备上 Launcher 可打开并阅读 EPUB, 其它文件管理器 `ACTION_VIEW` 可唤起; 设置 / 发行历史 / 更新检查全流程通过; 权限集合仍为 D19. 验收结论 (2026-09-20): Redmi 12C API 33 与 Sony G8441 API 28 均未安装宿主 (只有 `org.autojs.autojs6.inrt`), 启动器 / 外部查看器 / 设置用例在两台上通过, AVD API 37 (有宿主) 同样通过; 其它应用的 `ACTION_VIEW` 由 shell 身份的 `am start` 验证; 权限集合仍为 D19 (契约用例); Xiaomi Pad API 35 仍处于安全锁屏, 未纳入 P4 矩阵.

---

## P5: 宿主契约, 插件能力服务与宿主客户端

目标: 宿主能经 Binder 打开一本 EPUB 取回元数据 / 目录 / 文本 / 封面 / 搜索结果, 能以 token 启动阅读器并收到事件; 插件中心正确展示双服务插件.

### P5.1 契约模块与插件中心注册

- [x] (宿主) `plugin-api/epub-api` (附录 B 的 AIDL 与常量), `settings.gradle.kts` 的 `pluginApi` 列表, `app/build.gradle.kts` `implementation(project(":plugin-api:epub-api"))`, Manifest `<queries>` 增加 `org.autojs.plugin.EPUB`; 构建 AAR 并复制到插件 `libs/epub-api.aar` + 锁文件. 证据: 宿主 `261417e90` (`plugin-api/epub-api`: `IEpubPlugin` / `IEpubBook` / `IEpubReaderSession` / `IEpubReaderCallback`, `EpubActions` / `EpubIds` / `EpubCapabilityKeys` / `EpubContract` / `EpubErrorCodes`, `EpubAidlOrderTest` + `EpubContractTest`, `settings.gradle.kts` / `app` 依赖 / Manifest `<queries>`, v6.8.0 changelog 10 语言) 与插件 `2c0b219` (build 47: `libs/epub-api.aar` + `locks/host-api-aars.lock` + proguard + `libs/README.md`; 偏差: 两个 AAR 不来自同一宿主提交, 锁文件按制品记录来源).
- [x] (宿主) 插件中心四处注册: `InstalledPluginRepository.queryDeclaredPluginServices` 增加 `ServiceQuery(EpubActions.SERVICE_ACTION, category = "epub")` 与 `discoverPackage` 分支, `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE` 增加 `EpubIds.ENGINE`, `PluginCenterFragment.probeAidlPluginService`, `PluginDefaultEnabledPolicy` (Q7). 证据: 宿主 `9f86ea439` (`EpubPluginHost` 发现 / 身份 / 契约版本 / 宿主版本校验与绑定前检查, `InstalledPluginRepository` / `PluginCenterViewModel` / `PluginCenterFragment` 注册 `epub`, `PluginCenterEpubRegistrationTest` 4 条; D29 默认启用).
- [x] (宿主/测试) D10 验证: 安装插件后插件中心只出现一个 `Readium EPUB Reader` 条目, 展示身份 (id / engine / 版本 / 描述 / 说明) 正确, 激活 / 启用 / 禁用对两个服务一致生效; 结论写入 `docs/dev/epub-plugin-protocol-v1.md` 的 "身份" 小节; 若分组取值不确定, 按 D10 的退路修改 `InstalledPluginRepository` 分组归并规则并补单元测试. 证据: 宿主 `e45601626` `EpubPluginCenterEntryDeviceTest` (AVD API 37 与 AVD API 33): 插件包在插件中心恰一条目, 条目的 id / variant / 版本与 `epub` 服务的 `PluginInfo` 一致, 关闭该条目开关后 `EpubPluginHost.getCapabilities` 为 `PLUGIN_DISABLED`, 恢复后契约版本 1; 条目保留 Explorer Action 身份 (engine `explorer-action`, 其 `requiresHostVersion` 5269), `epub` 服务的 id / variant / 版本与之一致; 截图 `docs/images/evidence/p51-plugin-center-api33.png` / `-api37.png`; 结论写入协议文档 Decision 节, 分组规则无需修改.

### P5.2 插件能力服务 (提取)

- [x] (插件) `ReadiumEpubReaderPluginService` (`IEpubPlugin.Stub`): `getInfo` (engine `epub`, `REQUIRES_HOST_VERSION` = 契约首发宿主版本, `CONTRACT_VERSION=1`, `FEATURES`), `getCapabilities`, `openBook(pfd, options)` -> `EpubBookBinder`; `CallerGuard` 校验调用方包名 `org.autojs.autojs6` 与签名 (调试签名白名单只在 debug 构建); 并发上限 (同时打开 8 本), 每本空闲 5 分钟自动关闭. 证据: `1fdc182` (build 48); `service/ReadiumEpubReaderPluginService` (`getInfo` / `getCapabilities` 共用 `epubCapabilities()`: `requiresHostVersion` 5282, `epubContractVersion` 1, `epubFeatures`, `epubReadiumVersion`), `CallerGuard` (调用方 uid 须持有 `org.autojs.autojs6` 且签名与插件一致, 自身 uid 仅 debug), `BookRegistry` (8 本上限, 5 min 空闲回收, 解绑全关), `Limits` / `Answers` (Bundle 错误与 `CODE: detail` 异常两种形态), `DescriptorIo` (常规文件校验, parcel 尺寸, 管道流式导出).
- [x] (插件) `EpubBookBinder`: `getMetadata` (标题 / 作者 / 语言 / 标识符 / 出版方 / 日期 / 描述 / 主题 / 版式 / 阅读方向 / 位置数), `getToc` (树 -> 扁平数组 + depth, 上限 5000), `getReadingOrder`, `getText(request{href?|index?, offset, limit})` (Readium `ContentService` 段落迭代, 纯文本, 单次上限 1 MiB, 分页续取), `openResource(href)` (只允许 manifest 内资源, 大小上限 64 MiB, 返回只读 PFD 管道), `search(request{query, offset, limit})` (上限 500), `getPositions`, `close`; 所有 Bundle 大小受附录 B.5 约束. 证据: `1fdc182`; `service/EpubBookBinder` + `book/BookJson` / `HtmlBlockExtractor` / `TextExtractor`: 元数据 (含 `cover` / `positions`), 扁平目录 (`hasMore`), 阅读顺序, `getText` (`href` | `index`, `offset`, `maxChars`, `format` text / markdown; jsoup DOM 遍历而非 Readium 内容迭代器, 记为偏差; 非 HTML spine 项为空), `openResource` (manifest 内, 64 MiB, 管道读端), `search` (Readium 搜索服务分页, 50 / 500), `getPositions`, `close` / 空闲 / 解绑后 `SESSION_CLOSED`.
- [x] (测试) JVM: `TextExtractorTest` (分块边界, 上限, 空章节), `TocFlattenerTest`, `LimitsTest`; instrumentation: 显式绑定 `IEpubPlugin`, 用样本 PFD 往返全部方法, 非法 href / 越界 offset / 超限 limit / 非 EPUB PFD 的错误码, 关闭后调用返回 `SESSION_CLOSED`, 绑定解绑不泄漏 FD. 证据: `1fdc182`; JVM `TextExtractorTest`, `HtmlBlockExtractorTest`, `LimitsTest`, `PluginRuntimeInfoTest` (`TocFlattenerTest` 既有); instrumentation `PluginServiceInstrumentationTest` (4: 往返全部方法, 错误码, 第 9 本被拒 / 关一本再开, 解绑后 `SESSION_CLOSED`, 描述符不泄漏) + `PluginContractInstrumentationTest` (7) 在 Sony API 28 / Redmi API 33 / AVD API 37 各 11 / 11; 证据 `files/p2-evidence/service-*-api<N>.txt`; lint 0 错误, release APK 3,898,082 B.

### P5.3 阅读器会话

- [x] (插件) `openReader(pfd, options{locator?|href?|progression?, preferences?}, callback)`: 预打开 `Publication`, 生成 128 位随机 token, 注册到 `ReaderSessionRegistry` (超时 60 s 未被 Activity 认领则关闭并发 `close(reason=timeout)`), 返回 `IEpubReaderSession`; `EpubReaderActivity` 收到 `EPUB_READER_OPEN` + token 后认领会话, 复用已打开的 `Publication`; 会话方法 `getState` (locator / 进度 / 章节 / 是否可见), `goTo` (locator / href / progression), `navigate` (`NEXT_PAGE` / `PREV_PAGE` / `NEXT_CHAPTER` / `PREV_CHAPTER`), `setPreferences` (附录 A.6 的子集), `getBookmarks`, `close` (结束 Activity). 证据: `2c08db6` (build 49); `service/ReaderSession` / `ReaderSessionRegistry` / `ReaderSessionBinder` / `HostSessionPolicy` / `SessionTarget` / `ReaderPreferencesJson`, `EpubReaderViewModel.adoptSession` (`SessionAdopter`) 与 `EpubReaderActivity` (`ReaderSessionController`, 第二个 intent-filter `EPUB_READER_OPEN`), `PfdResource.duplicateDescriptor`; 令牌 128 位 / 32 位十六进制, 常量时间比较, 只经 `getState` 交付; 60 s 未认领 `timeout`; 新会话 `replaced` 并结束旧阅读器 (偏差: 草案未定旧 Activity 去留); 错误令牌只显示无效请求面板不 finish (偏差: 草案写 "拒绝并 finish"); 会话书籍不进最近列表.
- [x] (插件) 事件: `open` (Activity 可见), `progress` (节流 500 ms, 含 locator / 总进度 / 章节), `bookmark` (added / removed), `close` (reason: user / host / replaced / timeout / error), `error`; `generation` 每次会话递增, `seq` 单调; 回调异常 (宿主死亡) 时关闭会话. 证据: `2c08db6`; `open` 一次 (visible / 书名 / locator / href / positions), `progress` 500 ms 节流 (locator / 总进度 / 章节标题 / href), `bookmark` 差分 (首个列表为基线, 单元素数组), `error` (未知偏好键 `UNSUPPORTED_PREFERENCE`, 认领后不可达 `goTo`), `close` 恰一次 (user / host / replaced / timeout / error); generation 取毫秒时钟, seq 从 1 递增; 事件 parcel 超 32 KiB 先去 locator / bookmarks 再放弃; 回调 RemoteException 静默结束会话, 阅读器保留; `close(finish)` 语义 D32; 偏好范围越界为 `INVALID_ARGUMENT` 不截断, 高级键关闭出版商样式 (偏差记入协议文档).
- [x] (测试) instrumentation: 用测试 Activity 模拟宿主两步启动, 断言事件序列与 seq 单调, `goTo` 后 `getState` 一致, 重复 `openReader` 取代旧会话, token 错误的 Intent 被拒绝并 finish. 证据: `2c08db6`; JVM `HostSessionPolicyTest` (3), `ReaderPreferencesJsonTest` (7), `SessionTargetTest` (3), 全量 233 / 233; `ReaderSessionInstrumentationTest` (3: 会话往返 11 事件同 generation seq 1..11, 替换 / 错误令牌 / 无 finish 的 close / 缩短的认领超时, 拒绝码与描述符计数) + service (4) + 契约 (7) 在 Sony API 28 / Redmi API 33 / AVD API 37 各 14 / 14 (2026-09-21); 证据 `files/p2-evidence/reader-session-*-api<N>.txt`; lint 0 错误 / 62 警告; release APK 3,922,786 B.

### P5.4 宿主客户端

- [x] (宿主) `core/plugin/epub/` 全部类 (4.2 列表): `EpubPluginHost` 的 `discover / probe / queryServiceCount / selectOrThrow(engine = "epub")`, `openBook` 与 `openReader` 的专用绑定租约 (`callWithDedicatedBindingLease`), `EpubSource` (路径 -> 只读 PFD, 大小上限 Q-无 / 扩展名 / 魔数 `PK`), `EpubOutputSink` (封面 / 资源导出落盘), `EpubReaderLauncher` (显式 Component + action + token, `FLAG_ACTIVITY_NEW_TASK` 视宿主上下文而定), `EpubReaderBridge` (generation / seq / 节流), `EpubJson`, `EpubErrorMapper`. 证据: 宿主 `e45601626`; `EpubPluginHost` (`discover` / `probe` / `queryServiceCount` / `getCapabilities` / `openBook` / `openReader`, 专用绑定租约, `PLUGIN_DISABLED` 经 `PluginEnableStore`), `EpubSource` (路径校验: 存在 / 常规文件 / 可读 / `.epub` 或 ZIP 魔数, 只读 PFD, 调用后关闭), `EpubBookClient` / `EpubReaderClient` / `EpubReaderState` / `EpubBundles` (`EpubBinders.kt`), `EpubReaderBridge` (generation / seq / close 检查, 丢弃计数), `EpubReaderLauncher` (包内唯一导出的 `EPUB_READER_OPEN` Activity, 显式组件 + 令牌 extra, 非 Activity 上下文加 NEW_TASK), `EpubOutputSink` (`.part` 暂存, 64 MiB, 管道错误检查), `EpubErrorMapper` / `EpubError` / `EpubPluginException`; 偏差: 未单设 `EpubJson`, JSON 文档原样交给脚本层 (P6).
- [x] (测试) JVM: `EpubJsonTest`, `EpubReaderBridgeTest` (乱序 / 重复 / 旧 generation 丢弃), `EpubErrorMapperTest`; instrumentation (宿主 androidTest, 安装插件后): `EpubPluginHostRoundTripTest` (openBook 全方法, openReader + 启动 + 事件 + close). 证据: 宿主 `e45601626`; JVM `EpubReaderBridgeTest` (6), `EpubErrorMapperTest` (6), `EpubSourceTest` (4), `EpubOutputSinkTest` (5) 共 21 / 21; instrumentation `EpubPluginRoundTripTest` (3: `openBook` 全方法与分页 / Markdown / 搜索 / 导出 / 错误码, 宿主与插件拒绝码, `am force-stop` 后 `SESSION_CLOSED` 与重开) + `EpubReaderRoundTripTest` (1: 前台 `AboutActivity` 上下文两步启动, open / progress / error / close 事件, `state` / `goTo` 三形态 / `navigate` / `setPreferences` / `bookmarks`, 拒绝码, `close(finish)` 结束阅读器) + `EpubPluginCenterEntryDeviceTest` (1) 在 AVD API 37 与 AVD API 33 通过 (2026-09-21).

### P5.5 协议文档与 changelog

- [x] (宿主) `docs/dev/epub-plugin-protocol-v1.md` (身份, 发现, AIDL, Bundle key, 事件, 上限, 错误码, 两步启动, 版本协商, 混合版本行为), 宿主 `.changelog` 10 语言 (`feature`: 新增 EPUB 插件契约与 Readium EPUB Reader 接入), 本仓库 `.changelog`. 证据: 宿主 `f1219c70f` (`docs/dev/epub-plugin-protocol-v1.md`: Decision (身份 / 发现 / D10), Binder Surface 与方法语义, Bundle Keys, JSON Documents, Operations, Reader Sessions (两步启动 / 事件 / 关闭语义), Error Codes, Ceilings, Versioning (混合版本行为), Security Boundary, Host Client, Open Items); 宿主 changelog v6.8.0 已含契约 (P5.1) / 客户端 (P5.4) 两条 10 语言, 协议文档为纯文档提交不另加条目 (与 mail 协议文档相同); 本仓库 changelog 含服务 (P5.2) / 会话 (P5.3) 两条.
- [x] (宿主) 宿主提交按逻辑拆分 (契约模块 / 客户端 / 注册与文档), 不推送. 证据: `261417e90` (契约模块), `9f86ea439` (插件中心注册), `e45601626` (客户端 + 测试), `f1219c70f` (协议文档); 均未推送.

验收: 宿主 androidTest 往返在两台设备通过; 插件中心单条目; 协议文档与两侧 changelog 同步. 验收结论 (2026-09-21): 宿主往返三类用例在 AVD API 37 与 AVD API 33 通过 (真机未安装宿主, 与 P4 相同); 插件中心单条目由 `EpubPluginCenterEntryDeviceTest` 断言并截图; 协议文档与两侧 changelog 同步; Pad API 35 仍锁定.

---

## P6: 脚本 API `epub`

目标: 脚本可用同步与 Async 两种形态提取 EPUB 内容, 并可打开阅读器接收事件.

### P6.1 提取 API

- [x] (宿主) `augment/epub/Epub.kt`: `epub.open(path | options)` -> `EpubBook`; 便捷层 `epub.metadata(path)`, `epub.toc(path)`, `epub.readingOrder(path)`, `epub.text(path, target?, options?)`, `epub.cover(path, outputPath)`, `epub.search(path, query, options?)`; 全部有 `*Async` (Promise); `EpubError` (code / message); 注册到 `ScriptRuntime`; 路径解析遵循脚本工作目录规则 (`files.path`). 证据: 宿主 `a969fdfb0`; `augment/epub/{Epub, EpubBookNativeObject, EpubCalls, EpubPromises, EpubJsErrors}.kt` + `runtime/api/epub/{EpubService, EpubBookOperations, EpubScriptArguments, EpubScriptValues, EpubTextAssembler}.kt`, `ScriptRuntime` 注册 `epub` / `$epub`; 便捷层每次调用开关书, 另有 `epub.isAvailable()`; 全部 `*Async` 在脚本线程 settle; 路径遵循脚本工作目录并拒绝 URI; 参数形状在纯 Kotlin 规范化并拒绝未知键; `EpubError` (`code` / `message` / 隐藏 `javaException`); 插件不可用时错误码 `PLUGIN_UNAVAILABLE` / `PLUGIN_DISABLED` 附本地化插件中心提示 (`error_epub_plugin_unavailable`, 10 语言). 偏差: `epub.cover(path, outputPath?)` 的 `outputPath` 可省略 (返回 `Image`).
- [x] (宿主) `EpubBook`: `metadata`, `toc`, `readingOrder`, `positions`, `text(target?, options?)`, `textAll(options?)` (按阅读顺序拼接, 受总上限), `cover(outputPath?)` (无参返回 `Image`), `resource(href, outputPath)`, `search(query, options?)`, `close()`, `isClosed`; 脚本退出时自动关闭未关闭的书 (与其它资源清理同一钩子). 证据: 同上; `metadata` / `toc` (由契约扁平条目重建的 `{ title, href, children }` 树) / `readingOrder` / `positions` 一次取回后稳定; `text(target?, options?)` 接受 href / 阅读顺序下标 / 含 `href` 的对象 / 省略 (全书), 经契约 `getText` 按 `offset` / `maxChars` (默认 4 MiB) 分窗, `format` 为 text 或 markdown; `textAll` 按阅读顺序以空行拼接; `cover(outputPath?, options?)` 返回 `Image` 或落盘; `resource(href, outputPath, options?)` 经 spooled sink; `search` 在契约上限内分页; `close()` 幂等; `EpubService` 在脚本退出时关闭遗留的书.
- [x] (测试) JVM: `EpubObjectsTest` (JS 对象形状快照), `EpubTextOptionsTest`; 设备 smoke (Rhino 脚本, 两台设备): 打开三种样本, 打印元数据 / 目录 / 首章文本 / 保存封面 / 搜索; 未安装插件时错误码为 `PLUGIN_UNAVAILABLE` 且文案本地化. 证据: JVM `EpubScriptValuesTest` (5), `EpubScriptArgumentsTest` (6), `EpubTextOptionsTest` (5), `EpubJsErrorsTest` (4), `EpubObjectsTest` (4) 共 24; 设备 `EpubScriptSmokeDeviceTest` (3: `extract-smoke.js` 对夹具 EPUB 142 项断言 (元数据 / 目录 / 阅读顺序 / 文本窗口 / Markdown / 封面 / 资源导出 / 搜索 / 便捷层 / Async), 脚本退出关闭遗留的书, 插件禁用时 `PLUGIN_DISABLED` 与本地化提示) 在 AVD API 37 与 AVD API 33 各 3 / 3 (`build/p61-host-evidence-emulator-5556` / `-5560`). 偏差: 设备 smoke 用宿主 androidTest 自生成的夹具一本而非三种样本 (样本差异由插件侧 P2 / P5.2 覆盖); "未安装插件" 以禁用插件 (`PLUGIN_DISABLED`, 同一本地化提示) 代替, 两台设备均已安装插件.

### P6.2 阅读器控制

- [x] (宿主) `epub.read(path, options?)` -> `EpubReaderSession` (EventEmitter): `on('open' | 'progress' | 'bookmark' | 'close' | 'error')`, `goTo(target)`, `next()` / `prev()` / `nextChapter()` / `prevChapter()`, `setPreferences(partial)`, `locator`, `progress`, `isOpen`, `close()`; `epub.progress(path)` 与 `epub.bookmarks(path)` 只读 (经 `openBook` 附带的 store 读取, 或阅读器会话); 脚本退出时关闭会话, 阅读器 Activity 保留为普通阅读 (D32). 证据: 宿主 `6b9d45ace` (`augment/epub/EpubReaderSession.kt`, `EpubScriptArguments.read / goTo / preferences / close`, `EpubReaderLauncher` 恒加 `FLAG_ACTIVITY_SINGLE_TOP`); 事件 `open` / `progress` / `bookmark` / `error` / `close` 在脚本线程送达; `goTo(href | progression | locator)`, `next` / `prev` / `nextChapter` / `prevChapter`, `setPreferences(partial)`, `bookmarks()`, `close({ finish? })`; 属性 `locator` / `progress` / `title` / `chapterTitle` / `href` / `positions` / `state` / `reason` / `isOpen` / `isClosed`; 起始目标最多一种; 事件积压 -> `error(LIMIT_EXCEEDED)` + `close(overflow)`; 脚本退出静默取消会话并保留阅读器 (D32), `EpubService.close()` 先取消 runner 再关书. 插件 `3684eb4` (build 51): `EpubReaderActivity.onNewIntent` 收到宿主会话时 `finish()` 并以 NEW_TASK 重启自身 (single-top 送达到栈顶实例时). 偏差: 无 `epub.progress(path)` / `epub.bookmarks(path)` (契约 v1 没有 store 只读通道, 由 `session.bookmarks()` 与 `progress` 事件覆盖); `open` 载荷为 `{ locator, title, href, positions }`; `bookmark` 载荷 `{ action, locator, createdAt, title, text }`; close reason 增加 `overflow`, 插件死亡以 `error` 事件到达; `progress` / `positions` 为原始类型 (首个报告前 -1 / 0), 因为默认 `javaPrimitiveWrap` 下 Rhino 把装箱数字交给脚本时是 Java 对象.
- [x] (测试) 设备 smoke: 脚本打开阅读器并在 `open` 事件后 `goTo` 第二章, 监听 `progress`, 人工翻页后事件到达, `close()` 后 Activity 结束; 宿主被杀后插件会话关闭 (通知栏无残留). 证据: 宿主 `EpubReaderScriptSmokeDeviceTest` (3: `reader-smoke.js` 经 `open` -> `goTo` 第二章 -> `progress` -> `close`, 脚本退出保留阅读器, 阅读器在栈顶时二次 `read` 认领新会话) 在 AVD API 33 与 AVD API 37 各 3 / 3 (`build/p62-host-evidence-emulator-5560` / `-5556`, `activity-manager-api37.log` 留有加 SINGLE_TOP 前 `result code=2 / 3` 只前置任务不投递 intent 的记录); 插件 `ReaderSessionInstrumentationTest` (4, 新增 `hostLaunchOnTopOfAnOpenReaderOpensAFreshInstance`) 在 Sony API 28 / Redmi API 33 / AVD API 33 / AVD API 37 各 4 / 4 (`build/p62-plugin-evidence-*/reader-session-single-top-api<SDK>.txt`). 偏差: "人工翻页后事件到达" 与 "宿主被杀后插件会话关闭 (通知栏无残留)" 未自动化: 前者由 `goTo` / `nextChapter` 触发的 `progress` 事件等价覆盖, 后者依赖 P5.3 回调异常即关闭会话的路径 (插件侧已测).

### P6.3 示例, 文档与关联仓库

- [x] (宿主) `assets/sample/` 新增示例脚本 (10 语言目录规则同既有示例): `EPUB 元数据与目录`, `导出章节文本`, `打开阅读器并监听进度`; 宿主 `.changelog` 10 语言 (`feature`: 脚本 API `epub`). 证据: 宿主 `228715dea` (`assets-app/sample/电子书/` 下 `EPUB 元数据与目录`, `导出章节文本`, `打开阅读器并监听进度` 三份 `[v6.8.0+].js`; changelog v6.8.0 `feature` 10 语言 (P6.1 / P6.2 / P6.3 各一条), README / CHANGELOG 再生成) + `586f43046` (阅读器示例把 open 时的进度哨兵 -1 打成 -100%, 改为显示 `-`; 设备运行发现). 偏差: 示例目录沿用既有示例的单一中文目录名 (既有示例目录无 10 语言镜像).
- [x] (文档) `AutoJs6-Documentation` (`api/epub.md` + 类型页 `epubBookType.md` / `epubReaderSessionType.md` / `epubLocatorType.md`), `AutoJs6-TypeScript-Declarations` (`aj6-int-epub.d.ts`, 全局 `epub` 与 `$epub`), `AutoJs6-Plugin-Ace-Editor` (内置声明再生成), `AutoJs6-Plugin-Offline-Docs` (同步); 各仓库按其 `AGENTS.md` 处理版本与提交. 证据: Documentation `65c838e` (`api/epub.md` + `epubBookType.md` / `epubReaderSessionType.md` / `epubLocatorType.md`, 注册到 sidebar / toc / progress / dataTypes / changelog / README, 生成 `docs/*.html` 与 `json/*.json`) + `3d2e38d` (versionCode 72, 离线同步); Declarations `5b77d01` (4.19.0: `aj6-int-epub.d.ts`, `aj6-int-init.d.ts` 的 `epub` / `$epub`, `index.d.ts`, README / CHANGELOG / package.json; `docs/smoke/epub-smoke.ts` 以 tsc strictNullChecks 通过, 7 个负例); Ace `a2320f6` (1.12.0 build 106, 内置声明 4.19.0) + `7667888` (build 107, `autojs6_indices.js` / `lib.autojs6.d.ts` 再生成, 校验脚本与补全器测试通过) 与宿主 `26d3dee4e` (`tools/ace-completion/autojs6_indices.source.json`); Offline-Docs `7a48013` (build 52, 140 个文档文件同步, `SOURCE_PROVENANCE` 基线 `65c838e`, 单元测试 + `verifyOfflineDocsApks` 通过); 宿主协议文档 `838068231` 记录 P6 关闭与偏差. 宿主与四个关联仓库本次均未推送.

验收: 三份示例脚本在两台设备运行通过; 文档 / d.ts / Ace / 离线文档均含 `epub` 且版本号已更新. 验收结论 (2026-09-21): 三份示例脚本经宿主 `RunIntentActivity` 在 AVD API 37 与 AVD API 33 运行通过 (`build/p63_sample_run.py` 生成的三章样书, 宿主经 appops 授予 MANAGE_EXTERNAL_STORAGE / SYSTEM_ALERT_WINDOW; `build/p63-sample-evidence-emulator-5556` / `-5560`: 元数据 / 目录树 / 阅读顺序 / 便捷层输出, 章节导出与封面落盘到 `AutoJs6/epub/`, 阅读器 `open` -> 30 s `nextChapter` -> 60 s `close` 事件序列); 文档 / d.ts / Ace / 离线文档均含 `epub` 且版本号已更新 (Documentation versionCode 72, Declarations 4.19.0, Ace 1.12.0 build 107, Offline-Docs build 52); Pad API 35 仍锁定.

---

## P7: 健壮性, 安全, 兼容矩阵, 性能, 体积, 无障碍

- [ ] (插件/测试) 敌意输入矩阵 (P0.3 的损坏样本 + 新增): 非 zip, 空 zip, 缺 `mimetype` / `container.xml` / OPF, 坏 XML (含外部实体声明, 断言不解析外部实体), 路径穿越 href (`../`, 绝对路径, URL 编码), 超多条目 (50 000), 高压缩比单资源 (1 GiB 零字节), 超长文件名, 重复条目, 加密 (`encryption.xml` / LCP `license.lcpl`) -> 明确错误码与本地化提示, 不崩溃, 不写磁盘, 打开耗时有上限 (30 s 取消).
- [ ] (插件) WebView 边界复核 (D6 全部允许下的底线): `allowFileAccess=false`, `allowContentAccess=false`, 无自定义 `addJavascriptInterface`, Readium 本地服务域之外不响应 `file://`; `usesCleartextTraffic=true` (D31); 结论写入 `docs/dev/security-boundaries.md`.
- [ ] (测试) 兼容矩阵: AVD API 24, Sony G8441 API 28, Sony XQ-AT72 API 31, Redmi 22120RN86C API 33, Xiaomi 23046RP50C API 35, AVD API 36 / 37: 打开三种样本, 翻页, 搜索, 朗读 (真机), 进度恢复, 大字体, 夜间, 横屏, 分屏, 进程重建; WebView 版本记录; ColorOS 激活实测若无设备则明确记录 "未执行真实设备激活验证".
- [ ] (测试) 性能: 冷 / 热打开耗时 (1 MB / 20 MB / 200 MB / 5000 章样本), 首屏时间, 翻页帧率 (`dumpsys gfxinfo`), 搜索耗时, 位置计算耗时 (大书应在后台且不阻塞阅读), 内存峰值 (`dumpsys meminfo`), 朗读 30 分钟内存不增长; 结果表写入 `docs/dev/performance-baseline.md`, 与正确性测试分开.
- [ ] (插件) 体积: R8 规则最小化, 移除未用的 media3 解码路径 (`media3-exoplayer` 仅因 Readium 传递引入, 确认 R8 后不残留), 资源压缩 (`isShrinkResources`), 记录 release universal APK 体积并设预算 (Q-附录 D 由 P0.2 基线决定, 建议不超过基线 + 20%; P3 后 3,721,374 B, P4 后 3,841,934 B, P5 后 3,922,786 B, 超出该预算, 候选削减项见 2026-09-20 会话记录 (P3)); `verifyNativePageAlignment` 零原生库; 16 KB 设备 (Xiaomi Pad) 安装运行一次.
- [ ] (插件) 无障碍与输入: TalkBack 标签 (工具栏 / 面板 / 列表), 键盘与遥控导航 (Tab 焦点顺序, 方向键翻页), 大字体 UI 不裁切, RTL UI, 触摸目标不小于 48 dp (Previewer 修复过同类问题).
- [ ] (插件) 错误与日志: Timber 在 release 不植树或只记录 WARN 以上且不含书名 / 路径 / 正文; 崩溃前保存进度.

验收: 矩阵表 (设备 x 场景) 全部通过或有明确记录; 敌意样本全部失败闭合; 性能表与体积数值入库.

---

## P8: 文档, changelog 与 1.0.0 发布 gate

- [ ] (插件) README 10 语言 (`.readme/*.json`): 简介 -> 功能 (阅读 / 偏好 / 搜索 / 朗读 / 书签 / 独立入口 / 脚本 API) -> 安装 (插件中心与 Release) -> 使用 (文件管理器 / Launcher / 其它应用 / 脚本示例三段) -> 兼容性 (宿主版本, Android 版本, 不支持 DRM) -> FAQ (为何不支持 PDF / MOBI, 朗读无声音, 字体不生效, 竖排) -> 权限说明 (D19) -> 发行历史 -> 许可证与第三方声明; 截图 (`docs/images/screenshots`, 真机, 只用 `docs/fixtures` 样本).
- [ ] (插件) `plugin_instruction.md` 10 语言, `.changelog` 10 语言 (`v1.0.0`, `released_date`), `THIRD_PARTY_NOTICES.md` 与 `assets/licenses/` 按最终依赖树核对, `generate_markdown.py --check` 通过.
- [ ] (插件/测试) 发布 gate: Temurin 验收构建, `:app:testDebugUnitTest`, `:app:lintDebug` (0 error), `:app:assembleDebugAndroidTest`, API 35 模拟器 CI 通过, `:app:appendDigestToReleasedFiles` 产出 `autojs6-plugin-readium-epub-reader-v1.0.0-<CRC32>.apk`, `apksigner verify`, 安装到两台真机做 Explorer / Launcher / ACTION_VIEW / 脚本四路 smoke.
- [ ] (发布) GitHub Release `v1.0.0` (签名 APK + 变更摘要); 维护者确认后推送.
- [ ] (索引) `AutoJs6-Official-Plugins-Index`: 运行 `tools/generate_official_plugin_index.py` 生成条目, `release-manifests/io.github.supermonster003.autojs6.plugin.readium.epub.reader/<versionCode>.json` 准入清单; 宿主插件中心能看到并安装.
- [ ] (宿主) 宿主 changelog 汇总条目复核, `docs/dev/readium-epub-reader-plugin-integration.md` 更新为 1.0.0 状态; 宿主发布节奏由维护者决定.

验收: `git status --short` 为空, `VERSION_BUILD` 与提交数一致, Release 资产 CRC32 与文件一致, 索引条目可被宿主消费.

---

## P9: 高亮, 笔记与导出 (1.1.0)

- [ ] (插件) Room 数据库 `annotations` (指纹, locator JSON, 颜色, 笔记文本, 时间), 选择文本后的高亮 / 笔记动作, decorations 渲染, 列表面板 (跳转 / 编辑 / 删除), 导出为 Markdown (经系统分享或保存到用户选择的目录).
- [ ] (宿主) `EpubReaderSession` 新增 `highlight` 事件与 `book.annotations()` 只读 (契约版本 2, 末尾追加方法).
- [ ] (测试/文档) JVM + instrumentation + 文档 / d.ts 同步, `v1.1.0` changelog.

---

## 附录 A: 脚本 API 草案

### A.1 命名与通用约定

- 全局对象 `epub` (别名 `$epub`); 同步方法阻塞脚本线程 (内部 `runBlocking`), 同名 `*Async` 返回 Promise; 事件对象为 `EventEmitter`.
- 路径参数接受相对脚本工作目录的路径或绝对路径; `content://` 不接受 (脚本侧无授权模型).
- 所有错误抛 `EpubError { code, message, cause? }`, 错误码见 B.4.
- `Locator` 对象形状与 Readium 一致: `{ href, type, title?, locations: { progression?, totalProgression?, position?, cssSelector?, fragments? }, text?: { before?, highlight?, after? } }`.

### A.2 `epub` 方法表

| 方法 | 返回 | 说明 |
| --- | --- | --- |
| `epub.open(path)` / `openAsync` | `EpubBook` | 打开并保持 Binder 会话, 需 `close()` |
| `epub.metadata(path)` | `EpubMetadata` | 便捷层: open + metadata + close |
| `epub.toc(path)` | `EpubTocEntry[]` | 树形, 每项 `{ title, href, children }` |
| `epub.readingOrder(path)` | `EpubLink[]` | `{ href, type, title? }` |
| `epub.text(path, target?, options?)` | `string` | `target` 为 href / 索引 / 省略 (全书, 受 `maxChars` 默认 4 MiB) |
| `epub.cover(path, outputPath?)` | `string \| Image` | 有 `outputPath` 时写文件返回路径, 否则返回 `Image` |
| `epub.search(path, query, options?)` | `EpubSearchResult[]` | `options.limit` 默认 100, 上限 500 |
| `epub.read(path, options?)` | `EpubReaderSession` | 打开阅读器; `options: { locator?, href?, progression?, preferences? }` |
| `epub.progress(path)` | `Locator \| null` | 插件记录的最后位置 |
| `epub.bookmarks(path)` | `Locator[]` | 插件记录的书签 |
| `epub.isAvailable()` | `boolean` | 插件已安装, 启用且兼容 |

### A.3 `EpubBook`

| 成员 | 说明 |
| --- | --- |
| `metadata` | `{ title, authors[], language, identifier?, publisher?, published?, modified?, description?, subjects[], layout: 'reflowable' \| 'fixed', readingProgression: 'ltr' \| 'rtl' \| 'auto', positions? }` |
| `toc`, `readingOrder`, `positions` | 同 A.2 |
| `text(target?, options?)` / `textAsync` | `options: { offset?, limit?, maxChars? }`; 分页续取用 `offset` |
| `textAll(options?)` | 按阅读顺序拼接, 章节间以 `\n\n` 分隔, 受 `maxChars` |
| `cover(outputPath?)`, `resource(href, outputPath)` | 资源导出经宿主落盘, 单资源上限 64 MiB |
| `search(query, options?)` / `searchAsync` | `{ href, title?, locator, text: { before, highlight, after } }[]` |
| `close()`, `isClosed` | 幂等 |

### A.4 `EpubReaderSession` 事件

| 事件 | 载荷 | 说明 |
| --- | --- | --- |
| `open` | `{ locator, metadata }` | 阅读器可见 |
| `progress` | `{ locator, totalProgression, chapterTitle? }` | 节流 500 ms |
| `bookmark` | `{ action: 'added' \| 'removed', locator }` | |
| `close` | `{ reason: 'user' \| 'host' \| 'replaced' \| 'timeout' \| 'plugin-died' \| 'error' }` | 之后不再有事件 |
| `error` | `EpubError` | 非致命错误 (如跳转目标不存在) |

方法: `goTo(target)`, `next()`, `prev()`, `nextChapter()`, `prevChapter()`, `setPreferences(partial)`, `locator`, `progress`, `isOpen`, `close()`.

### A.5 示例

```js
// 元数据与目录
let book = epub.open('./books/moby-dick.epub');
console.log(book.metadata.title, book.metadata.authors.join(', '));
book.toc.forEach(e => console.log(e.title, e.href));
console.log(book.text(book.readingOrder[1].href, { maxChars: 2000 }));
book.cover('./books/moby-dick.webp');
book.close();

// 打开阅读器并监听进度
let session = epub.read('./books/moby-dick.epub', { href: 'OPS/chapter_003.xhtml' });
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%'));
session.on('close', e => console.log('closed:', e.reason));
```

### A.6 `preferences` 子集 (脚本可设)

`fontSize`, `fontFamily`, `lineHeight`, `pageMargins`, `theme` (`'light' | 'sepia' | 'dark'`), `scroll`, `columnCount`, `verticalText`, `textAlign`, `hyphens`, `publisherStyles`; 其它字段忽略并在 `error` 事件报告 `UNSUPPORTED_PREFERENCE`.

---

## 附录 B: 契约草案 (`plugin-api/epub-api`)

### B.1 `Bundle` key (`EpubContract.KEY_*`)

`contractVersion`, `requestId`, `href`, `index`, `offset`, `limit`, `maxChars`, `query`, `locator` (JSON 字符串), `progression`, `preferences` (JSON 字符串), `direction`, `errorCode`, `errorMessage`, `event`, `reason`, `sessionToken`, `visible`, `metadata` / `toc` / `readingOrder` / `results` / `bookmarks` (JSON 字符串, 统一用 JSON 而不是嵌套 Bundle, 便于宿主 `EpubJson` 与脚本对象共用一套模型).

### B.2 op 与事件表

| AIDL 方法 | 输入上限 | 输出上限 |
| --- | --- | --- |
| `openBook` | PFD 必须为常规文件; `options` 不超过 4 KiB | 并发 8 本 |
| `getMetadata` | - | 256 KiB |
| `getToc` | - | 5000 条 / 1 MiB |
| `getReadingOrder` | - | 5000 条 |
| `getText` | `limit` 不超过 1 MiB 字符 | 同 `limit` |
| `openResource` | `href` 不超过 2048 字符, 必须在 manifest 内 | 64 MiB |
| `search` | `query` 1-256 字符, `limit` 不超过 500 | 500 条 / 2 MiB |
| `getPositions` | - | 单值 |
| `openReader` | 同 `openBook`; 同一宿主一个活动会话 | - |
| `goTo` / `navigate` / `setPreferences` | `locator` 不超过 16 KiB; `preferences` 不超过 16 KiB | - |
| `getBookmarks` | - | 500 条 |

事件 (`EpubContract.EVENT_*`): `open`, `progress`, `bookmark`, `close`, `error`; 每个事件 Bundle 不超过 32 KiB.

### B.3 线程与所有权

- 插件在 Binder 线程池处理调用, 内部经协程调度到 IO; 单本书内方法串行, 不同书并行.
- PFD: 宿主创建并在调用返回后关闭自己的副本; 插件 `dup` 后自行管理, `close()` 或超时释放; `openResource` 返回的管道读端由宿主负责关闭.
- 阅读器会话由插件 Activity 生命周期主导; 宿主 `close()` 只是请求, 最终以 `close` 事件为准.

### B.4 错误码

`PLUGIN_UNAVAILABLE`, `PLUGIN_DISABLED`, `PLUGIN_INCOMPATIBLE`, `FILE_NOT_FOUND`, `FILE_UNREADABLE`, `NOT_EPUB`, `PARSE_FAILED`, `ENCRYPTED`, `RESOURCE_NOT_FOUND`, `LIMIT_EXCEEDED`, `INVALID_ARGUMENT`, `SESSION_CLOSED`, `SESSION_REPLACED`, `READER_NOT_VISIBLE`, `UNSUPPORTED_PREFERENCE`, `CANCELLED`, `TIMEOUT`, `IO`, `INTERNAL`.

### B.5 上限常量 (写入 `EpubContract`)

`MAX_OPEN_BOOKS=8`, `BOOK_IDLE_TIMEOUT_MS=300000`, `MAX_TOC_ENTRIES=5000`, `MAX_TEXT_CHARS_PER_CALL=1048576`, `MAX_RESOURCE_BYTES=67108864`, `MAX_SEARCH_RESULTS=500`, `MAX_QUERY_LENGTH=256`, `MAX_LOCATOR_BYTES=16384`, `MAX_EVENT_BYTES=32768`, `READER_CLAIM_TIMEOUT_MS=60000`, `PROGRESS_THROTTLE_MS=500`.

---

## 附录 C: 宿主改动清单 (按文件)

| 文件 | 改动 | 阶段 |
| --- | --- | --- |
| `app/src/main/java/org/autojs/autojs/util/FileUtils.kt` | `PreviewerType.EPUB`; `TypeDataHolder.EPUB_READER`; `TYPE.EPUB("epub", ...)` | P1.4 |
| `app/src/main/java/org/autojs/autojs/ui/explorer/ExplorerDocumentPreviewerPluginUi.kt` | `specFor` EPUB 分支; 包名 / MIME 常量; 归档内 EPUB 策略 (Q2) | P1.4 |
| `app/src/main/res/values/strings_donottranslate.xml` | `plugin_readium_epub_reader_name` | P1.4 |
| `app/src/test/java/org/autojs/autojs/ui/explorer/DocumentPreviewerFileTypeTest.kt` | EPUB 用例 | P1.4 |
| `.changelog/lang_*.json` (10 语言) | P1.4 / P5.5 / P6.3 条目 | 各阶段 |
| `docs/dev/readium-epub-reader-plugin-integration.md` | 接入说明 | P1.4 |
| `settings.gradle.kts`, `app/build.gradle.kts` | `plugin-api/epub-api` 模块与依赖 | P5.1 |
| `plugin-api/epub-api/**` | 契约模块 (AIDL + 常量) | P5.1 |
| `app/src/main/AndroidManifest.xml` | `<queries>` 增加 `org.autojs.plugin.EPUB` | P5.1 |
| `core/plugin/center/InstalledPluginRepository.kt`, `PluginCenterViewModel.kt`, `PluginCenterFragment.kt`, `PluginDefaultEnabledPolicy.kt` | `epub` engine 注册与默认启用 | P5.1 |
| `core/plugin/epub/**` | 宿主客户端 | P5.4 |
| `docs/dev/epub-plugin-protocol-v1.md` | 协议文档 | P5.5 |
| `runtime/api/augment/epub/**`, `runtime/ScriptRuntime.kt` | 脚本 API 与注册 | P6.1 / P6.2 |
| `app/src/main/assets/sample/**` | 示例脚本 | P6.3 |

不需要改动: ProGuard 规则 (无插件包名规则), `ExplorerPrimaryAction` / `ExplorerPluginActionController` (由 `isPreviewable()` 驱动), Explorer Action 协议版本 (v2 目录被 v22 宿主接受), `Mime.kt` (常量已存在).

---

## 附录 D: 待决事项 (已于 2026-09-18 全部拍板, 回填为 D23-D33)

维护者拍板: Q1=b, Q2=b, Q3=c, Q4=b, Q5=a, Q6=a, Q7=a, Q8=c, Q9=b, Q10=b, Q11=a. 各题保留原选项以记录取舍; 生效条款以 D23-D33 为准.

### Q1 (P1 前): 书籍指纹算法

拍板: (b) -> D23.

- (a) 文件大小 + 首 1 MiB + 末 64 KiB 的 SHA-256 (默认; 200 MB 书打开不需全量哈希).
- (b) 全文件 SHA-256 (最准确, 大书打开慢, 可后台计算并在完成前用 (a) 临时键).
- (c) EPUB `dc:identifier` + 修改日期 (最快, 但同书不同版本或缺标识符时冲突).

### Q2 (P1 前): 归档内 EPUB

拍板: (b) -> D24.

- (a) 沿用 8 MiB 上限, 超过提示先解压 (默认).
- (b) 为 EPUB 单独提升到 256 MiB (宿主需把 `MAX_ARCHIVE_DOCUMENT_BYTES` 改为按类型).
- (c) 归档内 EPUB 不提供主动作.

### Q3 (P2 前): 外部链接

拍板: (c) -> D25.

- (a) 弹出确认对话框显示完整 URL, 确认后交给浏览器 (默认).
- (b) 直接打开浏览器.
- (c) 设置项二选一, 默认 (a).

### Q4 (P3 前): 退出阅读器后是否继续朗读

拍板: (b) -> D26.

- (a) 退出即停止 (默认, D15).
- (b) 设置项 "后台继续朗读", 默认关, 开启时 Activity 销毁后服务继续到书末或定时器结束.

### Q5 (P4 前): `ACTION_VIEW` 的 MIME 兜底

拍板: (a) -> D27.

- (a) 只声明 `application/epub+zip` (默认).
- (b) 追加 `application/octet-stream` + `pathPattern` `.*\\.epub`, 兼容不设 MIME 的应用, 代价是选择器里可能出现在无关文件上.

### Q6 (P4 前): 更新检查默认行为

拍板: (a) -> D28.

- (a) 仅手动 (默认).
- (b) 每日一次自动, 非计量网络, 可关闭.

### Q7 (P5 前): 插件是否默认启用

拍板: (a) -> D29.

- (a) 默认启用 (与 Explorer Action 家族一致, 默认).
- (b) 默认关闭, 需在插件中心启用.

### Q8 (P6 前): `epub.text()` 的文本形态

拍板: (c) -> D30.

- (a) 纯文本, 段落以 `\n` 分隔, 标题不加标记 (默认).
- (b) 轻量 Markdown (标题 `#`, 列表 `-`, 强调保留), 便于脚本再处理.
- (c) 提供 `format: 'text' | 'markdown'` 选项, 默认 `'text'`.

### Q9 (P7 前): 明文 HTTP 资源

拍板: (b) -> D31.

- (a) `usesCleartextTraffic=false`, 书内 `http://` 图片不显示 (默认).
- (b) `usesCleartextTraffic=true`, 与 D6 "全部允许" 完全一致.

### Q10 (P6 前): 脚本退出时阅读器去留

拍板: (b) -> D32.

- (a) 脚本退出关闭会话并结束阅读器 Activity (默认).
- (b) 会话关闭但阅读器保留 (变成普通阅读), 事件停止.

### Q11 (P8 后): 是否排期 P9 高亮 / 笔记

拍板: (a) -> D33.

- (a) 排期 1.1.0.
- (b) 暂缓.

---

## 附录 E: 证据等级与退路

### E.1 证据等级

| 标签 | 可以证明 | 不能证明 |
| --- | --- | --- |
| `SOURCE` | 源码存在, 结构符合设计 | 编译或行为正确 |
| `JVM` | Android-free 逻辑的单元测试 (JUnit4) | Binder / WebView / 真机行为 |
| `ANDROID_BUILD` | `assembleDebug` / `testDebugUnitTest` / `lintDebug` / `assembleRelease` 通过 | 真机行为 |
| `BINDER` | 指定设备上的 instrumentation: 发现, 绑定, 往返, 敌意输入 | 阅读渲染与用户可见行为 |
| `DEVICE` | 指定设备与 API 级别上, 对指定样本 (文件名) 完成真实操作 | 未列出设备 / API / 样本 |
| `DOCS` | README (10 语言), changelog, 协议文档, 文档 / d.ts / Ace / 离线文档已同步且版本号已更新 | - |
| `RELEASE` | 签名 APK, CRC32 文件名, GitHub Release, 官方索引 receipt | 未明确覆盖的设备 / 样本 |

条目勾选时在其后追加证据, 格式示例: `[x] ... (JVM: EpubReaderIntentPolicyTest 18 用例; DEVICE: Xiaomi 23046RP50C / API 35 + moby-dick.epub, Sony G8441 / API 28 + kusamakura-vertical.epub, 2026-09-xx; commit abc1234)`.

当前可用设备池 (以当日 `adb devices -l` 为准): Xiaomi 23046RP50C (API 35), Sony G8441 (API 28), Sony XQ-AT72 (API 31), Redmi 22120RN86C (API 33), Xiaomi Pad (16 KB 页), AVD API 24 / 33 / 36. 无 ColorOS 设备时激活验收如实记录未执行.

### E.2 D2 退路: Readium 解析 + 自研 WebView 导航器

触发条件见 P0.2 决策点 (依赖无法解析 / R8 不可收敛 / 体积不可接受 / 核心 API 在 API 24 不可用). 形态: 只保留 `readium-shared` + `readium-streamer` (解析 `Publication`, `Locator`, `PositionsService`, `SearchService`, `ContentService` 仍可用), 阅读器改为复用 HTML Previewer 的加固 `WebView` 管线逐章渲染, 分页用 CSS 多列 + 自研位置追踪 (`progression` 由滚动比例估算), 高亮 / TTS 句级同步降级或推迟, 竖排依赖 WebView 原生 `writing-mode`; D5 中的 "全文搜索" 保留 (streamer 提供), "TTS 朗读" 降级为章节级朗读 (无句高亮), "FXL" 降级为整页缩放查看. 仓库名不变.

---

## 附录 F: 预留

### F.1 安全模式 (剥离书内脚本, 拦截远程资源)

接入点: `TransformingContainer` 包裹 `Publication` 容器, Jsoup 移除 `<script>` / `on*` / 危险 URI; `EpubNavigatorFragment` 的请求拦截 (P0.2 已验证可行性) 阻断非本地请求并计数; 偏好开关 `安全模式` 默认关 (D6). 不排期.

### F.2 CBZ / 漫画

接入点: `readium-streamer` 已含 divina 解析器; 需 `ImageNavigatorFragment` 或自研图片翻页器, 宿主新增 `.cbz` 类型行与 `application/vnd.comicbook+zip`; 名称不改. 不排期.

### F.3 有声书与 Media Overlays

接入点: `readium-navigator-media-audio` + `readium-adapter-exoplayer-audio`; EPUB 3 Media Overlays 需 Readium 的 `MediaOverlayNavigator` (若上游提供); 前台服务与 P3 共用. 不排期.

### F.4 PDF 与 LCP

PDF: `readium-adapter-pdfium-*` 含原生库, 需 ABI 拆分, 16 KB 验证与体积评估, 更适合独立插件. LCP: `readium-lcp` 需要 `liblcp` 商业授权. 均不排期.

### F.5 高亮 / 笔记

见 P9.

### F.6 与其它插件联动

OpenCC 简繁转换 (对 `epub.text()` 输出或阅读器内文本), Three-Stone-AI 摘要, MCP Server 暴露 `epub_*` 工具: 均可基于附录 A 的脚本 API 在各自仓库实现, 本插件不引入依赖.

---

## 附录 G: 参考

- Readium Kotlin Toolkit: `https://github.com/readium/kotlin-toolkit` (3.4.0 tag, `test-app/`, `docs/guides/`, 迁移指南)
- Readium Maven 制品: `https://repo1.maven.org/maven2/org/readium/kotlin-toolkit/`
- Readium CSS: `https://github.com/readium/readium-css` (主题, 竖排, 用户设置变量)
- 样本: `https://github.com/IDPF/epub3-samples`, `https://github.com/w3c/epub-tests`
- 宿主协议文档: `docs/dev/explorer-action-protocol-v22.md`, `docs/dev/explorer-action-host-file-info-v1.md`, `docs/dev/official-plugin-settings-contract-v1.md`
- 兄弟仓库: `AutoJs6-Plugin-HTML-Previewer` (Explorer Action v2 审计与门禁), `AutoJs6-Plugin-Markdown-Previewer` (用户文件导入), `AutoJs6-Plugin-Three-Ember-Player` (独立入口, 前台服务, 设置页, 更新检查), `AutoJs6-Plugin-Angus-Mail` (会话契约, 事件桥, Roadmap 形态), `AutoJs6-Plugin-MCP-Server` (AAR 锁, 第三方声明)
- 规范: `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md`

---

## 会话记录

### 2026-09-18

- 探查宿主快照 `1db2d9b87` 的 Explorer Action 协议 (v22), 文档预览器接入模板, 插件中心注册与分组, 脚本 API 定义方式; 探查 HTML / Markdown Previewer, 3-Ember Player, Angus Mail 的可复用形态.
- 以 `curl` 从 Maven Central 读取 Readium 3.4.0 各制品 POM (依赖树, BSD-3-Clause), 从 GitHub 3.4.0 tag 读取 `readium-shared` zip / resource / asset 工具目录, `EpubNavigatorFragment` 与 `EpubPreferences` 的接口面.
- 两轮选择题拍板 D1-D8, 派生 D9-D22, 落盘本路线图. 仓库骨架与 `git init` 留待 P0.1.

### 2026-09-19

- Q1-Q11 拍板回填为 D23-D33; 按 P0.1 生成仓库骨架 (平台版本插件 1.8.2, 宿主 AAR 哈希锁定, Explorer Action v2 契约, 10 语言资源与文档, CI), `git init` 并以 noreply 邮箱提交 6 笔 (`46c7b12` .. `27af413`).
- P0.2 spike: Readium 3.4.0 四制品在 Kotlin 2.3.20 下解析编译, R8 收敛, release 2.7 MB, 零原生库; `PfdResource` + `FallbackContentProtection` 打开链路在 AVD API 24 与 Xiaomi API 35 通过 17/17 instrumentation 用例; D2 / D11 固定, 记录见 `docs/dev/p0-readium-spike.md`. 偏好 / 字体 / TTS / 请求拦截验证推迟到 P2 / P3.
- P0.3: 生成器与 10 个样本入库并有 Python 测试; 外部样本与性能样本参数推迟.
- 宿主侧验收: Xiaomi 23046RP50C (宿主 5282) 插件中心发现并默认启用 `Readium EPUB Reader 1.0.0 (1)`; 宿主本身尚未改动 (P1.2).
- 附带发现: media3-exoplayer 的清单会注入 `ACCESS_NETWORK_STATE` / `WAKE_LOCK`, 已用 `tools:node="remove"` 移除; Readium 不带内容保护时会把 LCP 标记的书当普通 EPUB 打开, 必须传入 `FallbackContentProtection` 并检查 `isRestricted`.
- 下一步: P1.1 (进度记忆 / 书签落盘) 与 P1.2 (宿主 `FileUtils.TYPE.EPUB` 入口与归档上限).

### 2026-09-19 (P1)

- P1.1: `application/zip` + `.epub` 纳入接受范围, 5279 检查点, `ExplorerActionSpec` 纯数据目录 (`27ed186`); 附带把 `EpubReaderIntentPolicy.kt` 里的 NUL 字面量改为转义, 并加 `.gitattributes` (`*.epub` / `*.jks` / `*.png` / `*.aar` / `*.jar` binary, `07ab6b5`): 文本型样本 `malformed-not-a-zip.epub` 在 autocrlf 检出时会被改成 CRLF, 使 Python 样本测试在 Windows 全新检出失败.
- P1.3 store (`be75f11`): `AtomicFiles` / `ProgressRecord` + `ProgressCodec` / `BookDataStore` (LRU 500, 迁移合并, 别名) / `ProgressThrottle`, 24 个 JVM 用例; `BookFingerprintInstrumentationTest` 记录 200 MiB 样本哈希耗时.
- P1.2 + P1.3 接线 (`1d880ab`, reader chrome 的 changelog 条目漏在该提交之外, 由随后的 `docs(changelog)` 提交补入): `EpubReaderViewModel` (打开 / 临时键 / 别名解析 / 后台全量哈希迁移 / 节流持久化), `EpubReaderActivity` (chrome, 目录, 点按区, 音量键, 滚动模式, 从头开始, 就绪门控), `ReaderChrome` / `TocSheet` / `PageTurnPolicy` / `ReaderProgress` / `TocFlattener` / `ReaderSettings`, 布局与菜单, 11 目录 21 键字符串, 10 语言 changelog / README / 说明书.
- P1.4 宿主 (`40f8a4206`): `TYPE.EPUB` / `PreviewerType.EPUB` / `specFor` / 按类型的归档上限 / 字符串 / 测试 / 文档 / changelog; 未推送.
- P1.5: AVD API 24 全流程 (主图标, 溢出条目, 未安装与未启用引导) 与 23/23 instrumentation 通过; 真机矩阵 (Sony API 28, Xiaomi API 35) 未连接, 留待下次会话.
- 附带发现 1: Readium 3.4.0 `EpubNavigatorFragment.go()` 若在初始资源加载完成前调用, 内部状态停在 `Loading(初始 href)`, 此后 `notifyCurrentLocation()` 一直提前返回, `currentLocator` 冻结且进度不再保存 (重开后立即 "从头开始" 时复现); 现以 `PaginationListener.onPageChanged` (只在 Ready 态触发) 作为就绪信号, 就绪前的跳转排队重放 (`navigatorReady` / `pendingJump`), 就绪前忽略翻页键; 用例 `aJumpRequestedBeforeTheFirstPageLoadsIsReplayedOnceTheNavigatorIsReady`.
- 附带发现 2: 临时键目录迁移到正式指纹后, 下次打开只知道临时键, 若无别名会当作新书; `aliases/<临时键>` 文件解决, 淘汰与清空时一并清理.
- 附带发现 3: `currentLocator` 的初始值就是 `initialLocator`, 测试若只等 href 会立刻通过; UI 用例的 "打开耗时" 改为等到 `navigatorReady` (AVD API 24: 585 ms).
- 下一步: P2.1 偏好面板与主题 (含 `EpubPreferences` 序列化), 顺带补 P1.5 真机矩阵与 P0.3 外部样本.

### 2026-09-19 (P1.5 + P2.1)

- P1.5 关闭: Sony G8441 API 28 与 Xiaomi 23046RP50C API 35 instrumentation 23/23, Xiaomi 宿主联调 (主图标 + 溢出条目 `阅读 EPUB`) 通过; FXL 样本留到 P2.4.
- P2.1 编解码与存储 (`498b2b4`): `prefs/ThemeMode` (+ `ReaderTheme`, `ThemeMapping`), `prefs/ReaderThemeColors`, `prefs/PreferenceRanges`, `prefs/PreferencesCodec` (+ `StoredPreferences`), `store/ReaderPreferencesStore`, 27 个 JVM 用例, changelog 条目.
- P2.1 面板与主题 (`c1f719c`): `ReaderPreferencesState`, `EpubReaderViewModel` 偏好加载 / 编辑 / 去抖持久化 / 旧键迁移, `EpubReaderActivity` 偏好流接线与菜单, `reader/PreferencesSheet` + `sheet_preferences.xml`, `ReaderChrome.applyTheme` + 边到边 insets, `HostAppearanceActivity.hostDarkMode`, 11 目录 30 键字符串 (+ 8 个不翻译键), 10 语言 README / 说明书, `EpubReaderPreferencesInstrumentationTest` (3), 三台设备 instrumentation 全绿 (AVD API 24 26/26, Xiaomi 23046RP50C API 35 26/26, Sony G8441 API 28 26/26, 2026-09-19 11:24), lint 0 错误, release APK 3,000,988 B.
- 附带发现 1: Readium `EpubPreferencesSerializer` 用 kotlinx `Json` 默认配置 (无 `ignoreUnknownKeys`, 数值 / 枚举非法即抛), 所以文件层先白名单清洗再交给它; `Theme` 枚举初始化调用 `android.graphics.Color.parseColor`, 纯 JVM 测试不能触碰 `EpubPreferences` / `Theme`, 主题映射与配色因此以 `prefs/ReaderTheme` 镜像 Readium CSS 的颜色.
- 附带发现 2: Material `Slider` 要求值落在 `valueFrom + n * stepSize` 网格上 (否则抛 `IllegalStateException`), 面板用整数百分比刻度并在 `setSlider` 先吸附; 值在抬手时提交, 避免拖动中反复重排.
- 附带发现 3: `targetSdk 37` 在 API 35+ 强制边到边, 原布局的工具栏会顶到状态栏之下; 现全 API 手动处理 insets, 由主题色统一涂栏, API 24 / 25 亮色主题保留黑色导航栏.
- 附带发现 4 (store 修复): `AtomicFiles.read` 原本先删除同名 `.tmp` 再读, 与另一线程正在进行的原子写 (临时文件 -> 重命名) 竞争时会让重命名失败, 写入静默丢失 (偏好用例轮询读取时在 AVD API 24 复现); 现在 `read` 不再碰临时文件, `write` 打开时自然截断.
- 附带发现 5: 行距 / 段间距 / 对齐 / 连字符等 Readium CSS "高级设置" 只在出版商样式关闭时生效 (`EpubPreferencesEditor` 的 `isEffective`), 面板改任一项即自动关闭出版商样式, 否则用户看不到变化.
- 下一步: P2.2 自定义字体导入 (`FontStore` + SAF + `fontFamilyDeclarations`), 然后 P2.3 竖排 / RTL 与 P2.4 FXL; P0.3 外部样本仍待网络许可.

### 2026-09-19 (P2.2)

- P2.2 字体校验 / 目录 / 存储 (`07a6a4f`): `fonts/FontFileValidator`, `fonts/FontCatalog` (+ `FontCatalogCodec`, `FontFamilyNames`, `FontLimits`), `store/FontStore`, 22 个 JVM 用例 (合成 SFNT 构造器 `SyntheticFonts`), changelog 条目.
- P2.2 导入与注入 (`ffcbd78`): `book/FontsContainer` + `BookOpener.open(resource, extraResources)`, `EpubReaderViewModel` 字体目录流 / 导入 / 删除, `EpubReaderActivity` 文档选择器 / `@font-face` 声明 / 导航器重建, `PreferencesSheet` 字体列表与导入 / 管理按钮, 11 目录 14 键字符串, 10 语言 README / 说明书, `EpubReaderFontsInstrumentationTest` (3), 三台设备 instrumentation 全绿 (AVD API 24 / Xiaomi 23046RP50C API 35 / Sony G8441 API 28 各 29/29, 2026-09-19 12:13-12:16), lint 0 错误, release APK 3,038,276 B.
- 附带发现 1: Readium `servedAssets` 只经 `WebViewAssetLoader` 服务 APK assets, 无法服务用户文件; 出版物资源经 `https://readium_package/` 由 `Publication.get(href)` 查容器, 清单之外的 href 以绝对 URL 回退查容器, 所以 `FontsContainer` 同时接受 `fonts/<file>` 与绝对路径, 导入后无需重开书籍; `@font-face` 用绝对 URL 声明, `normalizeAssetUrl` 对绝对 URL 原样返回, 与页面同源无 CORS 问题; 清单外资源的媒体类型来自 `MimeTypeMap`, 可能为 null, WebView 按内容嗅探字体, 不受影响.
- 附带发现 2: `fontFamilyDeclarations` 在 `EpubNavigatorFragment` 构造时复制并在 `EpubNavigatorViewModel` 初始化时写入 `ReadiumCss`, 之后不可变, 因此导入 / 删除字体后必须重建导航器 (`recreateNavigator`: 冲刷进度, 移除片段, 以 `lastLocator` 重建, 取消旧定位收集); 面板不再持有某个片段的 `settings`, 改收集 `activeNavigator.flatMapLatest`.
- 附带发现 3: Readium 自带字体名 (`OpenDyslexic`, `AccessibleDfA`, `IA Writer Duospace`) 与 CSS 通用族名不能被导入字体遮蔽, 且 CSS 族名比较不区分大小写; `FontFamilyNames.resolve` 对保留名与目录内重名追加 8 位哈希后缀并随条目固化 (删除其它条目不改名, 偏好始终可匹配); 真机导入 Readium 的 `iAWriterDuospace-Regular.ttf` 即命中此路径 (三台设备 `document.fonts` 中的 FontFace `status=loaded`, `--USER__fontFamily` 与 body 计算样式均为 `"iA Writer Duospace (79e8378e)"`, 显示名 `iA Writer Duospace`, 文件 81,636 B, sha256 `79e8378e17d79ca6...`).
- 下一步: P2.3 竖排 / RTL, P2.4 FXL; P0.3 外部样本仍待网络许可.

### 2026-09-19 (P2.3)

- P2.3 夹具 (`360dd61`): 生成器以 `Locale` 元组参数化语言 / 章节标签 / 目录标签 / 正文 / CSS / `dir` / `page-progression-direction`, 新增日文竖排, 繁体中文竖排与阿拉伯语 RTL 三个样本 (同一灯塔故事的三种改写, 无第三方内容), 旧夹具字节不变, README 表与 SHA256SUMS 同步, 单元测试校验语言 / 进程 / `dir` / 书写模式声明.
- P2.3 文字方向 (`99d5038`): `PreferencesSheet` "文字方向" 三态组与竖排提示, 竖排时禁用分页 / 滚动组; 11 目录 5 键字符串; 10 语言 README / changelog / 说明书; `EpubReaderDirectionInstrumentationTest` (5), 三台设备 instrumentation 全绿 (AVD API 24 / Xiaomi 23046RP50C API 35 / Sony G8441 API 28 各 34/34, 2026-09-19 13:57-14:00), lint 0 错误, release APK 3,045,660 B.
- P2.3 截图存档 (本提交): `docs/images/evidence/` 七张降采样 PNG (API 28 / 35 各三样本, API 28 阿拉伯语界面一张), 由 `build/p23_images.py` 从 `files/p2-evidence/direction-*.png` 生成.
- 附带发现 1: Readium 的自动竖排只看 "语言 CJK 且阅读进程 RTL"; `Language.isRtl` 把 `zh-Hant` / `zh-TW` 当作 RTL, 所以繁体中文书即使没有 `page-progression-direction` 也会 RTL + 竖排, 简体 `zh` 则必须带 `rtl` 进程; 竖排下 `EpubSettingsResolver` 强制 `scroll = true` (CSS 多列无法对竖排分页), 面板据此禁用布局组.
- 附带发现 2: Readium CSS 的 `cjk-horizontal` 布局不覆盖出版商的 `writing-mode` (Readium CSS 里只有 `cjk-vertical/ReadiumCSS-after.css` 提到 `writing-mode`), 所以对自带 `vertical-rl` 的日文书 "强制横排" 只改变 Readium 的布局与分页, 页面仍是 `vertical-rl` (日文样本 三台设备 `verticalText=true`, `scroll=true`, `readingProgression=RTL`, 页面 `writing-mode: vertical-rl`; 强制横排后 `verticalText=false`, `scroll=false`, 页面仍 `vertical-rl` (出版商 CSS); 竖排下 「」、。 位于字格右上, 无破版); 无书写模式声明的中文样本则直接得 `horizontal-tb`; 竖排的翻页方向由 `readingProgression` 决定, 与书写模式无关.
- 附带发现 3: 界面方向 = 宿主语言 (`HostAppearance.wrap` 的 `setLayoutDirection`); 测试用的 AppCompat per-app locale 与宿主包装的先后随 API 而异: API < 33 时 AppCompat 在 `attachBaseContext` 里最后包装, per-app locale 胜出 (Sony API 28 装有 AutoJs6 仍得阿拉伯语 RTL 界面); API 33+ 走系统 `LocaleManager`, 宿主包装在其之上, 宿主胜出 (Xiaomi API 35 得 `zh-Hans` LTR 界面); 生产中不设 per-app locale, 各版本都跟随宿主 (AVD API 24 (无宿主) 与 Sony API 28 (宿主 `en`, `hostWins=false`) 界面语言 `ar`, `layoutDirection=1`, 工具栏镜像且进度以阿拉伯-印度数字显示, 英文书 `direction: ltr`; Xiaomi API 35 (宿主 `zh-Hans`, `hostWins=true`) 界面 LTR). 另: Sony 上 `HostAppearance.read` 曾在单独运行时返回 null (宿主进程未起时的瞬时失败, `onResume` 会重读并重建), 用例因此不再假定宿主是否存在; 正文方向始终来自出版物, 两者独立.
- 附带发现 4 (未修, 归入 P7 "无障碍与输入" 的 "大字体 UI 不裁切" 项一并处理): Sony G8441 横屏 (720 px 高) 下工具栏 48 dp 放不下标题 + 章节两行, 章节副标题被裁掉一半 (P1 起即如此, 与方向无关); 竖屏 56 dp 正常.
- 下一步: P2.4 FXL, 然后 P2.5 搜索 / 书签 / 手势 / 链接; P0.3 外部样本仍待网络许可.

### 2026-09-19 (P2.4)

- P2.4 夹具 (`df61de4`): 生成器新增 `fxl_entries` (6 版 `600x800` 视口的图画书, `rendition:layout` pre-paginated, `rendition:spread` auto, 书脊 `page-spread-center` / `-left` / `-right`, 每版一色便于截图辨认), 旧夹具字节不变, README 表与 SHA256SUMS 同步, 单元测试校验版式 / 视口 / 书脊属性.
- P2.4 固定版式 (`caf2ebf`): `ReaderPreferencesState.effective(hostDark, autoSpread)`, `EpubReaderActivity.effectivePreferences` + `onConfigurationChanged`, `ProgressSnapshot.pages` + `ReaderChrome` 的 `第 x / N 页`, `PreferencesSheet` 固定版式组 (提示 + 双页三态 + 说明) 并隐藏分页组与文字偏好, 菜单隐藏 "滚动模式"; 11 目录 6 键字符串 + 提示改写; 10 语言 README / changelog / 说明书; JVM `ReaderProgressTest` +1; `EpubReaderFixedLayoutInstrumentationTest` (4), 三台设备 instrumentation 全绿 (AVD API 24 / Xiaomi 23046RP50C API 35 / Sony G8441 API 28 各 38/38, 2026-09-19 15:29-15:36), lint 0 错误, release APK 3054652 B.
- P2.4 截图存档 (本提交): `docs/images/evidence/p24-fxl-*.png` 六张降采样 PNG (API 28 / 35 各竖屏, 横屏双页, 缩放), 由 `build/p24_images.py` 生成.
- 附带发现 1: Readium 3.4.0 的固定版式双页配对忽略书脊 `page-spread-*` 属性: 首页单独放右侧, 之后按阅读顺序两两成对 (2-3, 4-5, 6 单独), 双页时 `currentLocator` 指向左页; `Spread.AUTO` 在 `EpubPreferences` 中被拒绝, 在导航器中等同 `NEVER`, "自动" 只能由应用按方向解析, 且 `spread` 变化会触发 `InvalidateViewPager` (以 `currentLocator` 重建分页器).
- 附带发现 2: 固定版式没有 `onPageChanged` 回调 (只对可重排 WebView 发出), 也没有 `evaluateJavascript` (只对当前可重排页), 所以 P1 的 `onPageLoaded` 就绪判定与本次用例的视图树 / 反射检查是仅有的观测手段; `R2FXLLayout` 是 Readium 的 internal 类, 用例按类名查找并经 `getScale` / `getPosX` 反射读取.
- 附带发现 3: 固定版式下 `positions()` 每页一个位置, `locator.locations.position` 即页码, `totalProgression` 由位置服务给出, 因此 `第 x / N 页` 无需插件自算; WebView 的 `textZoom` 被 Readium 固定为 100 以免系统字号缩放破坏版式.
- 附带发现 4 (上游限制, 未自研排版): Readium Android 3.4.0 的固定版式页面按宽度适配 (`useWideViewPort` + `loadWithOverviewMode`, 无 "整页包含" 逻辑), 竖屏下 600x800 的版面完整可见, 横屏双页时每页只占一半宽度, 版面下部落在视图之外 (截图 `p24-fxl-landscape-api{28,35}.png` 可见底边被裁), `R2FXLLayout` 的 `minScale = 1` 也不能缩小; 属 Readium 的固定版式实现现状, 插件不改写页面 HTML 或视图树, 记录于此待上游改进.
- 附带发现 5 (测试工具, 已在用例内处理): Xiaomi Pad (HyperOS, API 35) 忽略 Activity 的方向请求并把竖屏请求 letterbox 成 1475x1800 的窗口, 此时 `UiAutomation.takeScreenshot` 只返回按该尺寸从显示原点裁下的一块 (壁纸 + 半个应用), `UiAutomation.injectInputEvent` 的触摸流被 `InputDispatcher` 拒绝 (同步在 `ACTION_UP` 失败, 异步逐条 "injection failed"); 用例改为 `PixelCopy` 复制 Activity 窗口 (API 26+, API 24 模拟器仍用显示截图) 与 `Activity.dispatchTouchEvent` 直接分发手势, 三台设备一致. 另外双页 pager 以 `currentLocator` 重建时, 落在跨页右侧的页 (如 3) 会被报成左页 (2), 用例按此断言.
- 附带发现 6 (store 修复, `e05c57a`): 三设备门禁在 Xiaomi Pad 上一次进程崩溃: `EpubReaderViewModel.persist` 的进度写入与用例 `@After` 删除书籍目录赛跑, `AtomicFiles.write` 抛出 `IOException("Cannot replace ...")`, 而 `persistScope` 没有异常处理, 未捕获即致命 (真实场景: 目录被清理工具移除或存储不可写); 现在进度写入与指纹迁移的别名写入都以 `runCatching` 包住 (与偏好写入一致), 只丢这一条记录, 阅读器不崩溃; changelog 记 fix. 修复后门禁重跑 (AVD API 24 / Xiaomi 23046RP50C API 35 / Sony G8441 API 28 各 38/38, 2026-09-19 15:29-15:36).
- 下一步: P2.5 全文搜索, 然后 P2.6 书签 / 手势 / 链接; P0.3 外部样本仍待网络许可.

### 2026-09-19 (P2.5)

- P2.5 全文搜索 (`0d14146`): `search/` 纯 Kotlin 包 (`SearchQueryPolicy`, `SearchSnippet`, `SearchResultPager`, `SearchGrouping`, `SearchState`, `SearchSession`), `EpubReaderViewModel` 搜索会话 (`search` 流, 开书 `attach`, 释放 `detach`), `EpubReaderActivity` 搜索面板 / 结果跳转 / decoration 组 `search` / 上一处下一处 / 关闭, `reader/SearchSheet` + `sheet_search.xml` + 结果行与章节头布局, `ReaderChrome` 搜索条 (主题配色, 禁用态 38% 透明), 菜单 `action_search`, 四枚图标; 11 目录 15 键字符串; 10 语言 README / changelog / 说明书; JVM 4 个测试类 19 用例; `EpubReaderSearchInstrumentationTest` (4), 三台设备 instrumentation 全绿 (AVD API 24 与 Xiaomi 23046RP50C API 35 各 43 用例 (42 通过 + 1 条外部样本用例按 `Assume` 跳过), 2026-09-19 17:36-17:40; Sony G8441 API 28 46 用例 (45 通过 + 1 跳过, 含 P2.6 的 3 条书签用例, 以 `52fb62b` 构建) 21:19-21:21 (补跑: 首次门禁时手机被指纹锁屏, 且 `stay_on_while_plugged_in` 只对 USB 生效而该机报告 AC 充电, 2 min 熄屏后即锁)), lint 0 错误, release APK 3,234,605 B.
- P2.5 截图存档 (本提交): `docs/images/evidence/p25-search-{panel,hit}-api{24,28,35}.png` 六张降采样 PNG (AVD API 24 竖屏, Sony API 28 竖屏 (补跑时归档) 与 Xiaomi Pad API 35 横屏各一张结果面板与命中页), 由 `build/p25_images.py` 生成.
- 附带发现 1: Readium 3.4.0 的 `SearchIterator.next()` 每次返回一个阅读顺序资源的全部命中 (`LocatorCollection`), 没有命中的资源被跳过, 末尾返回 null, `resultCount` 只是累计值; "每批 50" 由插件的 `SearchResultPager` 把集合切页实现, 一个资源命中超过 50 时同一批内多次拉取不会发生 (先消费待定队列); 2000 页样本 (`malformed-many-entries.epub`) 的分页耗时: 首批 50 条到达 AVD API 24 499 ms / Xiaomi Pad API 35 369 ms / Sony API 28 699 ms, 续载 10 批到 500 条上限累计 4,019 / 4,717 / 6,596 ms (Sony 为 21:19 补跑的数字, 16:54 旧构建为 835 / 6,229 ms).
- 附带发现 2: `StringSearchService` 给出的 `Locator.text` 每侧 200 字符且不按词切, 面板显示前由 `SearchSnippet` 修剪到每侧 48 字符并在词边界加 `...`; 标题来自 `Locator.title` (目录项), 目录没有覆盖的资源为 null, 章节头回退到 `href`; HTML 文本经 Jsoup `body().text()` 提取, 所以标题元素与 `nav` 文档不参与匹配, 用例的期望计数也只数 `<body>` 内的文本.
- 附带发现 3: `EpubNavigatorFragment.applyDecorations` 是 suspend 函数, 对每个已加载的可重排 WebView 注入脚本并在资源加载时重放, 固定版式页面无操作 (`evaluateJavascript` 也只对当前可重排页返回), 所以固定版式的搜索命中只跳转不高亮; 页面 DOM 里 decoration 组容器带 `data-group="search"`, 每条 decoration 一个子 `div`, 用例据此计数.
- 附带发现 4 (配色, 已修): chrome 的 `accent` 是前景色 55% 透明的灰, 用作页面高亮色时命中呈灰底, 改用与面板片段一致的琥珀色 `color_secondary` (Readium 模板自己再乘 0.3 透明度); Sony API 28 处于系统深色模式时面板的章节头 (`?attr/colorPrimary` = 夜间 `#004D40`) 在深色底上几乎不可见, 新增 `section_header` 颜色 (日间 `#00695C`, 夜间 `#4DB6AC`). 两处均由真机截图发现.
- 附带发现 5 (decoration 代价, 已改): 最初把已加载的全部命中都作为 decoration 应用, Readium 的 `decorator.js` 对每条 decoration 逐条做文本锚定 (`dom-anchor-text-quote`, 前后文各 200 字符的模糊匹配) 并读 `getClientRects` 后插入元素 (逐条强制重排), 命中密集的章节代价随命中数线性放大: Sony API 28 打开 Gutenberg 末条 (目标章 204 处命中) 42.5 s, 《福尔摩斯》末条 (42 处) 49.4 s; AVD API 24 17.0 / 19.4 s; Pad API 35 12.0 / 7.6 s; 改为只高亮当前命中后 AVD API 24 0.79 / 2.04 s, Pad API 35 0.45 / 1.28 s (decoration 在跳转落定后 10 ms 内就位); Sony API 28 1.13 / 2.22 s (补跑, 原 42.5 / 49.4 s). 面板列表仍列出全部命中, 上一处 / 下一处移动高亮.
- 附带发现 6 (本地真实书籍, 不入库): `E:/tmp/epub-samples` 的样本经 `adb push` + `run-as cp` 放入 `cache/epub-reader-test-documents/` 后由 `EpubReaderExternalSamplesTest` (runner 参数 `external=true`, 门禁不带此参数时跳过) 度量: mega-5000 (5001 章, 1.8 MB) 打开到首个定位 AVD API 24 15.8 s / Pad API 35 12.6 s / Sony API 28 20.6 s (5026 个位置, 耗时几乎全是位置计算), `chap` 首批 50 条 411 / 610 / 487 ms, 续载到 500 条上限 2.2 / 2.5 / 3.6 s, 跳转末条 0.6 / 0.3 / 0.7 s; mega-1000 打开 3.0 / 2.5 / 3.1 s; 《沟通的艺术》(16.5 MB, 73 章) 打开 1.1 / 1.0 / 1.2 s, `沟通` 首批 202 ms, 500 条 1.8 s, 跳转末条 1.2 / 0.7 / 1.9 s; 《福尔摩斯探案全集》(60.6 MB, 156 章) 打开 1.0 / 0.9 / 1.3 s, `福尔摩斯` 首批 205 ms, 500 条 1.8 s; 《JavaScript 函数式编程》(12.8 MB, 23 章) `函数` 500 条 1.0 s, 跳转末条 4.8 / 1.5 / 4.9 s (目标章只有 4 处命中, 耗时在页面本身); Gutenberg pg79599 (25.7 MB, 27 章, 法文) `the` 共 300 处 (含 thé 等变音匹配) 1.0 s 搜完, 打开 1.0 / 0.9 / 1.1 s; HowToLiveBetter (0.7 MB, 40 章) `life` 6 处 0.6-1.0 s; Galahit (351 MB 固定版式, 199 页, 仅 Pad) 打开 1.3 s, `the` 2 处 0.6 s, 跳到 page183 0.3 s (横屏跨页时定位器指邻页, 用例接受相邻页). Sony 补跑 (单条 decoration 构建 `52fb62b`, 21:22-21:25): mega-5000 打开 21.1 s (5026 个位置), `chap` 首批 488 ms, 500 条 3.8 s, 跳转末条 0.6 s; mega-1000 打开 2.9 s; 《沟通的艺术》打开 1.1 s, 首批 202 ms, 500 条 1.9 s, 跳转末条 1.4 s; 《福尔摩斯》打开 1.1 s, 首批 202 ms, 500 条 1.8 s, 跳转末条 (42 处) 2.2 s; 《JavaScript 函数式编程》500 条 1.0 s, 跳转末条 2.4 s; Gutenberg 打开 1.0 s, 300 处 1.0 s, 跳转末条 (204 处) 1.1 s; HowToLiveBetter `life` 6 处 0.8 s, 跳转 0.7 s.
- 附带发现 7 (宿主提供器, 已修, `ec474b6`): 另一个会话在 Sony API 28 上重装宿主时, 阅读器进程被系统以 `depends on provider org.autojs.autojs6/...OfficialPluginSettingsProvider` 杀掉 (`ContentResolver.call` 走稳定的提供器连接, 提供器进程被 force-stop 时其稳定客户端一并被杀); `HostAppearance.read` 改为 `acquireUnstableContentProviderClient` + `use`, 宿主中途消失时只是这次读取失败 (不套用宿主语言 / 夜间模式), 阅读器不死; changelog 记 fix. 同一会话还把宿主卸掉, 之后 `Failed to find provider info` 返回 null, 行为与无宿主一致.
- 下一步: P2.6 书签, 然后 P2.7 手势 / 按键 / 链接; P0.3 外部样本仍待网络许可.

### 2026-09-19 (P2.6)

- P2.6 书签 (`52fb62b`): `store/Bookmark` + `BookmarkCodec` (`bookmarks.json`, 上限 500, 并集), `BookDataStore.readBookmarks / writeBookmarks` 与迁移并集, `EpubReaderViewModel` 书签流与增 / 删 / 清 / 异步落盘, `reader/BookmarkPolicy` (当前页判定, 定位器合成, 片段), `EpubReaderActivity` 当前页跟踪 / 工具栏切换 / 面板入口, `reader/BookmarkSheet` + `sheet_bookmarks.xml` + `item_bookmark.xml`, 菜单 `action_bookmark` / `action_bookmarks`, 三枚图标; 11 目录 11 键字符串; 10 语言 README / changelog / 说明书; JVM 2 个新测试类 18 用例 + 店铺测试 4 用例; `EpubReaderBookmarksInstrumentationTest` (3), 三台设备 instrumentation 全绿 (AVD API 24, Redmi 12C 22120RN86C API 33 与 Xiaomi 23046RP50C API 35 各 46 用例 (45 通过 + 1 条外部样本用例按 `Assume` 跳过), 2026-09-19 18:46-18:53; Sony G8441 API 28 46 用例 (45 通过 + 1 跳过) 21:19-21:21 (解锁后补跑)), lint 0 错误, release APK 3,266,834 B.
- P2.6 截图存档 (本提交): `docs/images/evidence/p26-bookmarks-{page,panel}-api{24,28,33,35}.png` 八张降采样 PNG (每台设备一张填充图标的正文页与一张书签面板; API 28 两张在补跑时归档), 由 `build/p26_images.py` 生成.
- 附带发现 1: `EpubNavigatorFragment.firstVisibleElementLocator()` 是 `@ExperimentalReadiumApi` 的 suspend 函数: 可重排页由 `findFirstVisibleLocator` 脚本给出 `cssSelector` 与元素 `textContent` (`text.highlight`), 没有 progression; 固定版式直接返回 `currentLocator`. 书签定位器把二者合成 (`BookmarkPolicy.composeLocator`): 页面 progression / position 来自 `currentLocator`, 锚点来自可见元素; `R2EpubPageFragment.loadLocator` 有 `text.highlight` 时先 `scrollToLocator` (cssSelector / 文本引用), 否则按 `progression * numPages` 取页, 所以换字号后书签仍落在同一元素, 而只带 `cssSelector` 不带 text 的定位器会被 Kotlin 侧忽略.
- 附带发现 2: "当前页是否已加书签" 没有稳定的页 id 可比: 可重排分页的页号随字号 / 视口变化, 所以比较的是"书签 progression 映射到当前版面的页号"与导航器报告的 `pageIndex` (`onPageChanged`), 公式与 Readium 自己跳转时取页一致; 滚动模式下 `onPageChanged` 的 `pageIndex` 恒为 0 / `totalPages` 约 1, 改比较 position (约千字一段); 固定版式没有 `onPageChanged`, 用 `currentLocator` 的资源.
- 附带发现 3 (设备矩阵): 另一会话接入的 Redmi 12C (`22120RN86C`, API 33, en-US) 已解锁且装有宿主, 本轮门禁把它作为第三台设备 (API 24 / 33 / 35); Sony G8441 API 28 在 P2.6 提交时仍处于指纹锁 (从 P2.5 起); 用户解锁后于 21:19 补跑, 见下一条.
- P2.5 / P2.6 补跑 (本提交): Sony G8441 API 28 解锁后以 `52fb62b` 构建跑全量 connected 46 用例 (45 通过 + 1 跳过) 21:19-21:21, 搜索分页首批 699 ms / 500 条 6,596 ms, 书签添加 101 ms; 外部样本度量补齐 (P2.5 条目已更新: 单条 decoration 后 Gutenberg 末条 1.13 s, 《福尔摩斯》末条 2.22 s, 原 42.5 / 49.4 s); 截图 `p25-search-*-api28.png` 与 `p26-bookmarks-*-api28.png` 归档; 手机改为插电常亮 (`svc power stayon true`). 补跑截图暴露夜间模式下书签面板的 `全部清除` 与添加图标 (`?attr/colorPrimary`, 夜间 `#004D40`) 在深色底上几乎不可见, 与 P2.5 章节头同源, 改为 `section_header` 色 (`a9194f5`, 禁用态 38% 透明的 color state list `bookmark_button`), Sony 重跑书签用例 3/3 后归档面板截图.
- 下一步: P2.7 手势 / 按键 / 链接; P0.3 外部样本仍待网络许可.

### 2026-09-19 (P2.7)

- P2.7 阅读控制 (`1dbfd0b`): 点按区设置 / 键盘翻页 / 选中文本工具条 (细节见 P2.7 第 1 条证据); `EpubReaderControlsInstrumentationTest` (3) 四台设备全绿 (AVD API 37, Sony G8441 API 28, Redmi 12C API 33, Xiaomi Pad API 35; 21:56-22:10, 含改测试点位后的重跑).
- P2.7 链接 (`1bc0fae`): 书内链接返回栈, 注释对话框, 外链策略 (第 2 条证据); `EpubReaderLinksInstrumentationTest` (3) 四台设备全绿 22:14.
- P2.7 图片 (`5440ba4`): 图片查看器 (第 3 条证据); `EpubReaderImagesInstrumentationTest` (2) 四台设备全绿 22:23.
- P2.7 门禁 (本提交): 四台设备全量 connected 各 54 用例 (53 通过 + 1 条外部样本用例按 `Assume` 跳过) 22:33-22:44, 以 `7344b87` 构建 (AVD `sdk_gphone16k_x86_64` API 37, Sony G8441 API 28, Redmi 12C 22120RN86C API 33, Xiaomi 23046RP50C API 35), JVM 159 / 159; lint 首跑报 3 个 `AppCompatResource` 错误 (`menu_text_selection.xml` 的 `android:showAsAction`: 该菜单由框架的浮动 ActionMode 充气, AppCompat 只包装主 ActionMode, 换成 `app:` 会让所有项落入溢出), 以 `tools:ignore` 说明后重跑 (`7344b87`); lint 0 错误, release APK 3,298,525 B; 截图存档 `docs/images/evidence/p27-{links-note,images-viewer}-api{28,33,35,37}.png` 八张 (对话框自身窗口的 PixelCopy, 由 `build/p27_images.py` 降采样).
- 附带发现 1 (键盘事件): `KeyInterceptorView.onKeyDown` 只在它自己持有焦点时触发 (ViewGroup 的按键分发只走焦点链), 页面 WebView 有焦点时方向键被 Chromium 吞掉 (注入的 DPAD 键根本不到 DOM), 空格虽到 DOM 但 `KeyboardEvent.code` 为空 (Chromium 由扫描码推 `code`, `sendKeyDownUpSync` 与部分键盘没有扫描码), 而 Readium 的 `R2BasicWebView.onKey` 只传 `code`; 页面脚本对非编辑元素的 keydown 会 `preventDefault` 再回调 `Android.onKey`. 因此键盘翻页改在 `Activity.dispatchKeyEvent` 截获, 用焦点视图的 `onCheckIsTextEditor()` (WebView 只在页内可编辑元素聚焦时为真) 区分编辑态; 不实现 `InputListener.onKey`.
- 附带发现 2 (链接 fragment): `EpubNavigatorViewModel.navigateToUrl` 先 `servedUrlToLink` -> `Manifest.linkWithHref` (先带 fragment 匹配, 失败再去掉 fragment) 得到清单 `Link` 再回调 `shouldFollowInternalLink(link)`, 应用拿不到原始 URL 的 fragment; Readium 自己的 `go(link)` 同样落在资源开头. 跨资源锚点 (尾注, 索引) 的精确定位要等上游或页内脚本截获, 记为后续项.
- 附带发现 3 (opaque scheme): `AbsoluteUrl.invoke` 要求 `uri.isHierarchical`, `mailto:` / `tel:` / `javascript:` 解析为 null, `WebViewListener.shouldOverrideUrlLoading` 对它们返回 false, WebView 自行加载并显示 `ERR_UNKNOWN_URL_SCHEME` 错误页 (Readium 测试应用行为相同); `intent:` / `file:` 是层级 URL, 会到达回调并被拒绝. Jsoup 只是 readium-navigator 的 runtime 依赖, 不在编译类路径, 插件不直接引用 (否则要进 THIRD_PARTY_NOTICES 与 changelog dependency).
- 附带发现 4 (设备与用例): AVD 已由 API 24 换成 `sdk_gphone16k_x86_64` API 37 (release 17, 16 KB 页), 本轮矩阵为 API 28 / 33 / 35 / 37; Sony 的点按用例首次失败是因为 (0.85w, 0.5h) 恰好落在夹具 "the last chapter" 链接上 (WebView 跟随到 chapter3, chrome 未切换), 其它设备的版面没有这个巧合, 用例改在页面下部空白处点按并记录点按次数; Pad 横屏的查看器截图显示说明条被手势导航栏遮住底部, 根布局加 `fitsSystemWindows`.
- 下一步: P3 TTS 朗读; P0.3 外部样本仍待网络许可.

### 2026-09-20 (P3)

- P3 阶段 1 (`737a27a`): 朗读控制器 / 会话 / 面板 / 前台服务 (第 1, 2 条证据); JVM 167 / 167, lint 0 错误, release APK 3,698,482 B; `EpubReaderTtsInstrumentationTest` (6) 在 Redmi 12C API 33 (引擎 `com.xiaomi.mibrain.speech`, 4 个语音: 开始到出声 322-438 ms, 逐句进入第 2 章 7 次跳句翻页跟随 102-115 ms, 熄屏 3 分钟 75 次句子前进且 `wakefulness=Asleep`, 唤醒后 keyguard `showing=false`), Xiaomi Pad 23046RP50C API 35 (同引擎, 274 ms, 跟随 202 ms, 熄屏 75 次前进, 通知动作本地化为 上一句 / 暂停 / 下一句 / 停止) 与 AVD `sdk_gphone16k_x86_64` API 37 (`com.google.android.tts`, 473 个语音: 1056-1107 ms, 跟随 205-207 ms, 熄屏 74-77 次前进) 通过; 音频焦点: 短暂抢占 (`AUDIOFOCUS_GAIN_TRANSIENT`) 期间与归还后状态都是 PLAYING, 永久抢占 (`AUDIOFOCUS_GAIN`) 立即暂停并在对方放弃焦点后保持暂停, 点播放恢复 (三台一致); 通知动作 暂停 / 播放 / 下一句 / 上一句 / 停止 三台一致.
- P3 阶段 2 (`89d8859`): 睡眠定时器, 屏幕常亮, D26 后台继续朗读 (第 3 条证据); JVM 172 / 172, lint 0 错误, release APK 3,721,374 B; `EpubReaderReadAloudBackgroundInstrumentationTest` (5) 与阶段 1 的 6 条及 `PluginContractInstrumentationTest` 在 Redmi API 33 与 AVD API 37 各 17 条通过: 4 s 定时器分别在 4104 ms / 4086 ms 后停止并报 `SleepTimerEnded`, 本章结束定时器在跳句 7 / 7 次进入第 2 章时停止, 屏幕常亮标志只在朗读期间存在, 后台继续: 关闭阅读器后会话托管于服务且句子继续前进, 重开同一本书领回并打开在朗读句所在资源, 通知的 `ACTION_RESUME_READ_ALOUD` 重开阅读器显示同一 `Publication` 实例, 通知停止释放会话, 无会话时重开即关闭; 开关关闭 (默认) 时关闭阅读器即停止服务并撤销通知. 首跑定时器用例在两台设备都按 `Assume` 跳过: 固定时长停止后第二次启动时 `lastTtsEvent` 仍是 `SleepTimerEnded`, 等待出声的辅助把 "有事件且 IDLE" 当成引擎不可用; 改为忽略已知的旧事件后单独补跑通过.
- 附带发现 1 (小米 TTS 引擎): `TextToSpeech()` 无参构造在 Redmi 12C (API 33) 与 Xiaomi Pad (API 35) 上静默失败 (`onInit` 状态 -1, 24 ms, 无 logcat), 因为 `TtsEngines.getDefaultEngine()` 先读 `Settings.Secure.tts_default_synth` (两台都为空), 再退到 `getHighestRankedEngineName()` 而后者只接受应用带 `FLAG_SYSTEM` 的引擎; MIUI 的 `com.xiaomi.mibrain.speech` 装在 `/data/app/MIUIXiaoAiSpeechEngine` (非系统应用), 于是系统认为没有默认引擎, 而 `TextToSpeech(ctx, listener, "com.xiaomi.mibrain.speech")` 显式绑定 317 ms 成功 (4 个语音). Readium 的 `AndroidTtsEngine` 只用无参构造, 因此插件以 `tts/SystemTtsEngine` (改编自上游, 版权头保留) 先试无参再按 `defaultEngine` / 已装引擎列表显式绑定, 并在 Manifest `<queries>` 声明 `android.intent.action.TTS_SERVICE` 与 `android.speech.tts.engine.INSTALL_TTS_DATA` (API 30+ 否则看不到引擎). Google TTS (AVD, Sony) 不受影响.
- 附带发现 2 (熄屏与锁屏): 用 `input keyevent KEYCODE_SLEEP` 熄屏会让带安全锁屏的设备 (Xiaomi Pad) 进入 `wm dismiss-keyguard` 无法解除的锁定, 之后所有打开阅读器的用例超时 ("Timed out: href chapter1.xhtml"); 熄屏用例改为只在 `KeyguardManager.isDeviceSecure == false` (Redmi, AVD) 或运行参数 `screenOff=force` 时真正熄屏, 否则按 HOME 退到后台 (`mode=background-screen-on`), 唤醒后以 `dumpsys window policy` 的 `showing=` 行核对 keyguard 并最多重试 3 次 (WAKEUP + `wm dismiss-keyguard` + 菜单键). Sony G8441 处于图案锁 (已 `svc power stayon usb`), Pad 处于安全锁屏, 两台都需要用户解锁后补跑 P3 两个类与全量门禁.
- 附带发现 3 (音频焦点与 media3): Readium `TtsSessionAdapter` 对短暂抢占返回 `PLAYER_COMMAND_WAIT_FOR_CALLBACK` 且不改 `playWhenReady` (继续出声, 与系统媒体播放器的 "暂停后恢复" 不同), 永久抢占置 `playWhenReady=false` 且不自动恢复; 路线图 "抢占后恢复" 按此记录为手动恢复. Readium 的 media3 适配器不声明 seek 命令, 通知的上一句 / 下一句 / 停止以自定义 `SessionCommand` 按钮实现, 耳机上一曲 / 下一曲映射到句子; `media3-session` 1.11.0 直接声明 (与 Readium 传递版本一致, lint 提示 1.11.1 可用但保持一致).
- 附带发现 4 (体积): P3 使 release APK 由 3,298,525 B 增至 3,721,374 B (media3-session 与 TTS navigator 为主, dex 4.38 MB + 193 KB), 超出 P7 "基线 + 20%" 的 3.3 MB 预算; 候选削减: `assets/readium/divina/divinaPlayer.js` (424 KB, 插件不打开 DiViNa), R8 规则收紧, `isShrinkResources`; 留待 P7 决定预算是否重设.
- 下一步: P4 独立应用形态; Sony API 28 与 Pad API 35 解锁后补跑 P3; P0.3 外部样本仍待网络许可.

### 2026-09-20 (P4)

- P4.1 (`37479c4`): 启动器与最近书籍 (网格, 文档选择器, 持久授权, 封面缩略图, 不可用标记); JVM 180 / 180, lint 0 错误 / 45 警告, release APK 3,748,990 B; `EpubReaderLauncherInstrumentationTest` (4) 在 Sony API 28 / Redmi API 33 / AVD API 37 各 4 / 4.
- P4.2 (`62d12aa`): `ExternalViewerActivity` 与三条入口共用的 `EpubRequestPolicy`, 溢出菜单 `加入最近书籍`; JVM 185 / 185, release APK 3,757,654 B; `EpubReaderExternalViewInstrumentationTest` (3) AVD 3 / 3, Redmi / Sony 各 2 + 1 跳过.
- P4.3 (`9e5edec`): 设置页, 发行历史页, 手动更新检查; JVM 201 / 201, lint 0 错误 / 61 警告, release APK 3,841,934 B; `EpubReaderSettingsInstrumentationTest` (4) 三台各 4 / 4.
- 附带发现 1 (shell 授权): `am start --grant-persistable-uri-permission` 对 `com.android.externalstorage.documents` 只在 AVD 上被接受, Redmi (MIUI) 与 Sony 拒绝 shell 的持久授权 (`shell-grant=denied`), 用例以 `Assume` 跳过并记录; shell 向 `/sdcard/Download` 写文件需经 `run-as <包名> cat <文件> | cat > <目标>` (应用 uid 的重定向在 FUSE 上得到 0 字节); `am` 的 stderr 需 `sh -c eval ... 2>&1` 合并才能在 `UiAutomation` 的输出里看到系统的权限拒绝.
- 附带发现 2 (宿主外观重建): `HostAppearanceActivity` 首次同步宿主外观会 `recreate` 一次, `ActivityMonitor` 需按次注册并跳过 finishing / destroyed 的实例, 否则等待 `hits == 1` 超时.
- 附带发现 3 (设置页范围偏差): 宿主插件中心没有插件设置动作, 设置页只从启动器菜单与阅读器溢出菜单进入; P2.1 偏好面板与阅读器耦合且 D14 已让它编辑全局默认, 设置页不重复其 Readium 偏好, 只提供主题模式与 `恢复阅读偏好`; 朗读语音仍按语言在朗读面板选择; 封面缓存随最近书籍一并清除; 阅读器在 `onStart` 重读偏好文件以吸收设置页的修改.
- 附带发现 4 (体积): P4 使 release APK 由 3,721,374 B 增至 3,841,934 B (启动器 / 设置页界面与 Material 组件), 预算问题仍留 P7.
- 附带 (P3 补跑): Sony G8441 API 28 解锁后 P3 两类用例 17 / 17, 全量门 65 条中 1 条书签上限用例超时后单跑通过; Pad API 35 仍锁定.
- 下一步: P5 宿主契约 `plugin-api/epub-api` 与插件能力服务; Pad 解锁后补跑 P3 / P4; P0.3 外部样本仍待网络许可.

### 2026-09-21 (P5)

- P5.1 (宿主 `261417e90` + `9f86ea439`, 插件 `2c0b219` build 47): 契约模块 `plugin-api/epub-api` (4 个 AIDL, 常量, 顺序与快照测试), 插件中心注册 `epub`, 插件侧 AAR 与锁文件; D10 分组无需修改.
- P5.2 (`1fdc182` build 48): 能力服务与书籍提取 (`EpubBookBinder`, jsoup 文本 / Markdown, 搜索分页, 管道导出, 8 本 / 5 min 上限, `CallerGuard`); 三台 11 / 11, lint 0 错误, release APK 3,898,082 B.
- P5.3 (`2c08db6` build 49): 阅读器会话 (令牌认领, 事件流, 书签差分, 偏好补丁, D32 关闭语义, 替换 / 超时); JVM 233 / 233, lint 0 错误 / 62 警告, release APK 3,922,786 B; `ReaderSessionInstrumentationTest` + service + 契约在 Sony API 28 / Redmi API 33 / AVD API 37 各 14 / 14.
- P5.4 (宿主 `e45601626`): `core/plugin/epub` 客户端 (书籍 / 会话 / 桥 / 启动器 / 源 / 落盘 / 错误映射), JVM 21 / 21; `EpubPluginRoundTripTest` (3) + `EpubReaderRoundTripTest` (1) + `EpubPluginCenterEntryDeviceTest` (1) 在 AVD API 37 与 AVD API 33 通过.
- P5.5 (宿主 `f1219c70f`): `docs/dev/epub-plugin-protocol-v1.md`; 宿主 changelog 两条 (契约 / 客户端), 本仓库两条 (服务 / 会话); 宿主未推送.
- 偏差汇总 (均记入协议文档): `getText` 用 `maxChars` 而非 `limit`; `IEpubReaderSession.close(in Bundle options)` 带 `finish` (默认 false); `MAX_OPTIONS_BYTES` 36 KiB (草案 4 KiB 放不下 16 KiB locator + 16 KiB 偏好); 元数据含 `positions`; jsoup DOM 遍历替代 Readium 内容迭代器; `org.autojs.autojs6.inrt` 不是契约调用方; 两个 AAR 不来自同一宿主提交; 不声明 `tts` 特性 (契约无朗读控制面); 替换会话结束旧阅读器; 错误令牌只显示无效请求面板; `openReader` 的未知偏好键在 open 后以首个 `error` 事件上报; 偏好越界为 `INVALID_ARGUMENT` 不截断; 高级偏好键关闭出版商样式; `progress` 的 `title` 为章节标题而 `open` 的为书名; 宿主未单设 `EpubJsonTest`.
- 附带发现 1 (二进制误判): 测试源码里的字面 NUL (`"a\0b"`) 让 git 把 `.kt` 当二进制提交, 改为 Kotlin 转义 `\u0000` 后 amend; 提交驱动 `build/p5_commit.py` 保持一子项一提交.
- 附带发现 2 (宿主 runner 过滤): 宿主 `connectedAppDebugAndroidTest` 以逗号分隔多个 `class` 时只跑了第一个类, 改用 `package=org.autojs.autojs.core.plugin.epub` 过滤; 插件仓库的逗号列表正常.
- 附带发现 3 (纯文本丢图): `getText` 的 text 形态连同 `figcaption` 一起丢弃图片 (D30 只在 Markdown 保留 `![caption](href)`), 宿主往返用例的断言据此修正.
- 下一步: P6 脚本 API `epub` (提取 + 阅读器控制, d.ts / 文档); Pad 解锁后补跑 P3 / P4 / P5; P0.3 外部样本仍待网络许可.

### 2026-09-21 (P6)

- P6.1 (宿主 `a969fdfb0`): 脚本 API `epub` 提取层 (`EpubBook`, 便捷层, Async, `EpubError`, 本地化插件提示); JVM 24, `EpubScriptSmokeDeviceTest` 在 AVD API 37 / API 33 各 3 / 3.
- P6.2 (宿主 `6b9d45ace`, 插件 `3684eb4` build 51): `epub.read` -> `EpubReaderSession`; 宿主启动加 `FLAG_ACTIVITY_SINGLE_TOP`, 插件对 `onNewIntent` 送达的宿主会话重启自身; JVM 34 / 34 (epub 套件), `EpubReaderScriptSmokeDeviceTest` 3 / 3 x 2 (AVD API 33 / API 37), `ReaderSessionInstrumentationTest` 4 / 4 x 4 (Sony API 28, Redmi API 33, AVD API 33, AVD API 37).
- P6.3 (宿主 `228715dea` + `586f43046` + `26d3dee4e` + `838068231`; Documentation `65c838e` + `3d2e38d`; Declarations `5b77d01`; Ace `a2320f6` + `7667888`; Offline-Docs `7a48013`): 三份示例, changelog, 文档 / d.ts / Ace / 离线文档同步; 示例在 AVD API 37 / API 33 运行通过 (`build/p63-sample-evidence-*`).
- 偏差汇总 (均记入协议文档 Open Items 与 `api/epub.md`): 无 `epub.progress(path)` / `epub.bookmarks(path)`; `open` / `bookmark` 载荷形状; close reason 增加 `overflow`, 无 `plugin-died` (以 `error` 事件到达); `progress` -1 / `positions` 0 哨兵; 宿主启动 SINGLE_TOP + 插件重启; 设备 smoke 用夹具一本; 人工翻页与宿主被杀两项未自动化; `epub.cover` 的 `outputPath` 可省略; 示例目录无 10 语言镜像.
- 附带发现 1 (单顶送达): 以 NEW_TASK 启动已是任务根的同一组件时, 活动管理器只前置任务并丢弃 intent (`result code=2 / 3`), 会话等到超时; 宿主加 `FLAG_ACTIVITY_SINGLE_TOP` 后根实例收到 `onNewIntent`; 插件不改 `launchMode` (会改变 TTS 恢复与启动器流程), 而是在 `onNewIntent` 里 finish 并重启.
- 附带发现 2 (Rhino 装箱): 宿主 `WrapFactory` 默认 `javaPrimitiveWrap = true` 且只解包 `String`, 可空 Kotlin getter 返回的 `Int?` / `Double?` 到脚本是 `NativeJavaObject` (`typeof` 为 object, `===` 失败); 面向脚本的数字 getter 一律用原始类型 + 哨兵.
- 附带发现 3 (ActivityMonitor): `Instrumentation.callActivityOnResume` 也匹配 monitor (API 28 观察到), 旧实例经 `onNewIntent` 恢复时会先被返回, 测试需循环等到不同实例.
- 附带发现 4 (JUnit 浮点): 改为原始 `Double` 后 `assertEquals(double, double)` 需要 delta.
- 附带发现 5 (Ace 再生成): 宿主 `tools/ace-completion` 目录被 `.gitignore` 忽略但 `autojs6_indices.source.json` 已跟踪, 需 `git add -f`; 再生成在 `AutoJs6/build/ace-regen/` (指向 Ace 插件 editor 目录的 junction) 运行; Offline-Docs 的 Gradle 门禁在 Git Bash 下用 `./gradlew` 而非 `cmd //c gradlew.bat`.
- 下一步: P7 (敌意输入矩阵, 兼容矩阵, 性能, 体积预算 3.3 MB 已超出, 无障碍); Pad 解锁后补跑 P3-P6; P0.3 外部样本仍待网络许可.
