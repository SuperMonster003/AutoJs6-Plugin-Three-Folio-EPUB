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

- [ ] (测试) instrumentation (API 28 Sony G8441 + API 35 Xiaomi): `PluginContractInstrumentationTest` (Wake / INFO / EXPLORER_ACTION 发现唯一, 显式绑定, descriptor, `getInfo` 字段与 ABI 空数组), `EpubReaderIntentPolicyInstrumentationTest` (真实 Intent 与 ClipData), `EpubReaderActivityInstrumentationTest` (用 debug `FileProvider` 打开 EPUB 2 / EPUB 3 样本, 目录跳转, 翻页, 进度恢复, 损坏样本错误态), `RepositoryContractInstrumentationTest` (Manifest 导出与权限). 部分完成 (2026-09-19): AVD API 24 (x86, `Android SDK built for x86`) `:app:connectedDebugAndroidTest` 23/23: `PluginContractInstrumentationTest` 6, `EpubReaderIntentPolicyInstrumentationTest` 5, `EpubReaderProgressInstrumentationTest` 5 (进度 / 别名 / 重建 / 音量键与滚动 / 就绪前跳转重放), `EpubReaderUiInstrumentationTest` 2, `book/PfdResourceInstrumentationTest` 4, `book/BookFingerprintInstrumentationTest` 1; 实名与规划名不同 (无 `EpubReaderActivityInstrumentationTest` / `RepositoryContractInstrumentationTest`, 对应用例分布在上述类中). Sony API 28 与 Xiaomi API 35 本会话未连接, 真机矩阵待补.
- [ ] (测试) 宿主真机联调: 安装宿主 debug 与插件, 文件管理器点击 `.epub` 主动作与溢出菜单均打开阅读器; 卸载插件后出现安装引导; 禁用插件后出现激活引导; 记录设备与 API. 部分完成 (2026-09-19, 模拟器而非真机): AVD API 24 (x86, `Android SDK built for x86`) 安装宿主 debug (`40f8a4206`, 5282) 与插件 debug (1.0.0 (7)); 文件管理器 `Scripts/` 内 `minimal-epub3.epub` 行显示 `▤` 字形与 `Readium EPUB Reader` 主图标, 点击主图标与溢出菜单 `Readium EPUB Reader` 条目均以 v2 信封 (`content://org.autojs.autojs6.fileprovider/external_files/Scripts/minimal-epub3.epub`, `application/epub+zip`, 两项 ClipData) 启动 `EpubReaderActivity`, 并从同指纹的上次位置 (chapter 3) 恢复; 卸载插件后对话框 `Plugin "Readium EPUB Reader" not found in installed apps` (插件中心 / 取消 / 其它应用打开); 插件中心关闭开关后对话框 `... is not enabled in Plugin Center`. 真机待补.

验收: 两台设备上从文件管理器打开三种样本 (EPUB 2 / EPUB 3 / FXL) 均可阅读并恢复进度; JVM 与 instrumentation 全绿; 宿主 `DocumentPreviewerFileTypeTest` 通过; 10 语言字符串与 changelog 已补齐.

P1 验收状态 (2026-09-19): JVM 58 / 58, AVD API 24 instrumentation 23 / 23, 宿主 `DocumentPreviewerFileTypeTest` 6 / 6, 10 语言字符串 (21 键) 与 changelog / README / 说明书已补齐, `:app:lintDebug` 0 错误, release APK 2,801,108 B (P0 基线 2,749,948 B, +51,160 B), `verifyNativePageAlignment` 通过; 设备矩阵只覆盖 AVD API 24 (EPUB 2 / EPUB 3 样本经宿主与 instrumentation 打开), FXL 样本待 P2.4 引入, Sony API 28 / Xiaomi API 35 真机待下次会话补跑后再关闭 P1.5.

---

## P2: 阅读体验

目标: 交付 D5 的基线偏好全集与四项扩展中的三项 (搜索, FXL, 字体导入 + CJK 竖排), 以及书签, 手势, 链接与图片行为.

### P2.1 偏好面板与主题

- [ ] (插件) `PreferencesPanel` (底部面板): 字号 (滑块 + 步进, 50%-300%), 字体族 (出版商默认 / 系统衬线 / 无衬线 / 内置开源字体若引入 / 已导入字体), 行距, 页边距, 段间距, 对齐, 连字符, 出版商样式开关, 列数 (自动 / 1 / 2, 横屏生效), 主题 (浅色 / 护眼 / 深色 / 跟随宿主), 分页 / 滚动; 修改即时 `submitPreferences`.
- [ ] (插件) `PreferencesStore`: `EpubPreferences` JSON 序列化 (Readium 序列化器) + 版本号字段, 损坏时回退默认; "恢复默认" 按钮; 主题 "跟随宿主" 映射到 `Theme.LIGHT / DARK` 并在宿主暗色切换时同步.
- [ ] (插件) 工具栏 / 系统栏配色随主题 (浅 / 护眼 / 深三套), 沉浸模式过渡无闪烁 (参考两个 Previewer 的 `PreviewerChromeColors` 对比度规则).
- [ ] (测试) JVM: `PreferencesCodecTest` (全字段往返, 未知字段忽略, 越界值钳制), `ThemeMappingTest`; instrumentation: 偏好跨进程重启持久化, 字号变化后 WebView `textZoom` / Readium CSS 变量断言.

### P2.2 自定义字体导入

- [ ] (插件) `FontStore`: 设置页 / 偏好面板 "导入字体" 经 SAF (`ACTION_OPEN_DOCUMENT`, `font/ttf` / `font/otf` / `application/x-font-ttf` / `*/*` + 扩展名校验), 校验 TTF / OTF 魔数与大小 (单个 20 MiB, 总数 10), 复制到 `files/fonts/<sha256>.ttf`, 记录显示名 (从 `name` 表读取, 失败用文件名); 删除字体; 字体经 `EpubNavigatorFragment.Configuration.fontFamilyDeclarations` + `servedAssets` 注入.
- [ ] (测试) JVM: `FontFileValidatorTest` (魔数, 大小, 名称表解析, 非法文件); instrumentation: 导入后偏好面板可选且正文字体变化 (WebView `document.fonts` 检查或截图对比).

### P2.3 CJK 竖排与 RTL

- [ ] (插件) 偏好 `verticalText` (自动 / 强制横排 / 强制竖排; 自动 = 出版物 `page-progression-direction` 与语言判断), `readingProgression` 跟随出版物 (RTL 书籍翻页方向与点按区镜像), 界面 RTL 布局 (阿拉伯语 UI) 与正文 RTL 独立.
- [ ] (测试) 日文竖排样本与阿拉伯语样本在 API 28 / 35 各截图存档 (`docs/images/evidence/`), 验收无破版, 翻页方向正确, 目录跳转正确; 中文样本在竖排下标点位置合理 (记录 Readium CSS 的实际表现, 不自研排版).

### P2.4 固定版式 FXL

- [ ] (插件) FXL 出版物: `spread` 偏好 (自动 / 从不 / 总是), 横屏双页, 双指缩放与拖动 (Readium 内建), 页码显示改为 `第 x / N 页`, 隐藏对 FXL 无效的偏好 (字号 / 字体 / 行距 / 滚动).
- [ ] (测试) FXL 样本在手机竖屏 / 横屏与 Xiaomi Pad 上验证; instrumentation 断言 FXL 时偏好面板隐藏项.

### P2.5 全文搜索

- [ ] (插件) `SearchPanel`: 工具栏搜索入口, `Publication.search(query)` 迭代器分页加载 (每批 50, 上限 500 条), 结果按章节分组显示上下文 (`before` / `highlight` / `after`), 点击跳转并用 decoration 高亮命中, 上一处 / 下一处, 取消正在进行的搜索, 空结果与过短查询提示; 搜索在后台协程, 切换书籍时取消.
- [ ] (测试) JVM: `SearchResultPagerTest` (分页, 上限, 取消); instrumentation: 样本关键字计数与跳转后 `currentLocator.href` 断言.

### P2.6 书签

- [ ] (插件) `BookmarkSheet`: 添加当前位置 (含章节名与文本片段), 列表 (时间倒序), 跳转, 删除, 全部清除; 存储 `bookmarks.json` (每本上限 500); 工具栏书签图标反映当前页是否已加书签.
- [ ] (测试) JVM: `BookmarkStoreCodecTest`; instrumentation: 添加 / 跳转 / 删除 / 重启后保留.

### P2.7 手势, 按键, 链接与图片

- [ ] (插件) `PageTurnController`: 点按区可配置 (关闭 / 左右 / 上下), 音量键翻页开关, 硬件键盘方向键与空格翻页; 长按选择文本后的系统 ActionMode 增加 "复制" / "分享" / "搜索" (`ACTION_WEB_SEARCH`) / "翻译或处理" (`ACTION_PROCESS_TEXT`, 存在时).
- [ ] (插件) `LinkPolicy`: 书内链接在本 Activity 内跳转并压入返回栈 (返回键先回到跳转前位置), 脚注 / 尾注链接 (EPUB 3 `epub:type=noteref` 或目标为 `aside`) 弹出对话框显示注释内容, 外部 `http(s)` 链接按 D25 处理 (设置项二选一: 确认后打开为默认, 或直接交给浏览器), `mailto:` / `tel:` 交给系统, 其它 scheme 拒绝.
- [ ] (插件) 图片: `TapEvent.targetElement` (3.4.0 实验 API) 命中图片时打开 `ImageViewerDialog` (缩放 / 保存到相册不做, 只查看); API 不可用时降级为无动作.
- [ ] (测试) JVM: `LinkPolicyTest` (内链 / 外链 / 脚注 / 危险 scheme), `PageTurnPolicyTest`; instrumentation: 内链跳转与返回, 脚注对话框, 音量键翻页.

验收: 三种样本在 API 28 / 35 上完成偏好 / 字体 / 竖排 / FXL / 搜索 / 书签 / 链接全流程; 新增字符串覆盖 10 语言; JVM 与 instrumentation 全绿.

---

## P3: TTS 朗读

目标: 从当前位置开始朗读, 句级高亮跟随, 熄屏可继续, 退出阅读器即停止.

- [ ] (插件) `TtsController`: `readium-navigator-media-tts` 的 `TtsNavigator` + `AndroidTtsEngine`, 播放 / 暂停 / 上一句 / 下一句, 语速 / 音调, 语音与语言选择 (系统 TTS 引擎可用语音列表, 默认跟随出版物语言), 当前句 decoration 高亮并自动翻页跟随, 到达书末自动停止; 无可用引擎或语言不支持时给出安装 / 设置引导.
- [ ] (插件) `TtsForegroundService` (`foregroundServiceType="mediaPlayback"`, 不导出) + MediaSession 通知 (播放 / 暂停 / 上一句 / 下一句 / 停止), 音频焦点与耳机按键; 朗读开始时前台化, 停止 / Activity 销毁 / 错误时立即停止并释放; Android 13+ 首次朗读前请求 `POST_NOTIFICATIONS`, 拒绝后仍可朗读但无通知 (记录行为); 权限豁免理由写入 `AGENTS.md` 红线小节 (D15).
- [ ] (插件) 睡眠定时器 (15 / 30 / 60 分钟 / 本章结束) 与朗读时屏幕常亮开关; 偏好持久化.
- [ ] (测试) JVM: `TtsVoicePolicyTest` (语言匹配与回退), `SleepTimerPolicyTest`; 真机 (API 28 / 33 / 35): 朗读 3 分钟, 熄屏继续, 通知控制, 来电 / 其它音频抢占后恢复, 退出阅读器停止; 记录设备与系统 TTS 引擎名称.

验收: 三台真机朗读全流程通过; Manifest 新增权限仅 D15 三项; 10 语言字符串补齐.

---

## P4: 独立应用形态

目标: 不经宿主也能打开 EPUB, 并具备设置页, 发行历史与更新检查.

### P4.1 Launcher 与最近书籍

- [ ] (插件) `LauncherActivity` (`MAIN` / `LAUNCHER`): 最近书籍网格 (封面缩略图 / 书名 / 作者 / 进度百分比 / 最后阅读时间, 上限 100), "打开 EPUB" 经 `ACTION_OPEN_DOCUMENT` (`application/epub+zip` + `application/octet-stream` 兜底, `EXTRA_MIME_TYPES`), `takePersistableUriPermission`, 长按移除记录并 `releasePersistableUriPermission`; 授权失效 (文件被删 / 移动) 时标记不可用并允许清理; 空状态引导.
- [ ] (插件) `RecentBooksStore`: 持久 URI + 显示名 + 指纹 + 封面缩略图 (`files/covers/<指纹>.webp`, 最长边 512) + 元数据快照; 只有 Launcher 路径写入 (Explorer / ACTION_VIEW 路径不持久化第三方授权, 见 D4); 封面由 `CoverExtractor` 后台生成.
- [ ] (测试) JVM: `RecentBooksCodecTest`, `RecentBooksLimitTest`; instrumentation: 文档选择器返回后出现在列表, 重启后仍可打开, 移除后授权释放.

### P4.2 ACTION_VIEW 入口

- [ ] (插件) `ExternalViewerActivity`: intent-filter `ACTION_VIEW` + `content` scheme + `application/epub+zip` (D27: 不加 `application/octet-stream` + pathPattern 兜底); `EpubRequestPolicy` 区分三条入口 (Explorer 信封 / 外部 ACTION_VIEW / Launcher 持久授权) 的校验规则, 外部入口只接受 `content://` 且必须带读授权, 不接受 `file://`; 打开后行为与 Explorer 入口一致, 但不写入最近书籍 (除非用户在溢出菜单选择 "加入最近书籍", 此时尝试 `takePersistableUriPermission`, 失败则提示).
- [ ] (测试) JVM: `EpubRequestPolicyTest` (三条入口交叉: Explorer 信封不得走外部规则, 外部 Intent 不得伪装 Explorer, 缺授权 / 错 scheme / 目录 URI 拒绝); instrumentation: 用 `adb shell am start -a android.intent.action.VIEW -d content://... -t application/epub+zip` 从测试 `FileProvider` 打开.

### P4.3 设置页, 发行历史与更新检查

- [ ] (插件) `SettingsActivity` (跟随宿主外观, 无宿主时跟随系统): 默认阅读偏好 (进入 P2.1 面板的全局默认), 翻页行为 (点按区 / 音量键), 朗读默认 (语速 / 语音 / 定时器), 外部链接策略, 数据管理 (清除进度 / 书签 / 最近书籍 / 字体 / 封面缓存, 各自显示占用), 关于 (版本 / 作者 / 许可证 / 第三方声明), 发行历史, 检查更新; 从宿主插件中心与阅读器溢出菜单均可进入.
- [ ] (插件) `ReleaseHistoryActivity`: 按当前 locale 选择 `doc/CHANGELOG-{tag}.md`, 回退英语, 失败显示本地化错误 (Three-Stone-AI 形态).
- [ ] (插件) `AppUpdateCoordinator` / `AppUpdateRepository` / `AppVersionPolicy`: GitHub Releases API (`SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader`), 超时 / 取消 / 失败提示 / 忽略版本 / 频率限制 (每日一次) / 计量网络下不自动检查, 不做自动检查 (D28); 更新对话框 Neutral 按钮 = 内置发行历史, Positive = 打开发布页, 不下载 APK.
- [ ] (测试) JVM: `AppVersionPolicyTest` (语义化比较, 预发布, 忽略版本), `UpdateSchedulePolicyTest` (频率, 计量网络), `ReleaseHistoryTest` (locale 候选); instrumentation: 设置项持久化, 发行历史打开, 数据清除后 store 为空.

验收: 无宿主的裸设备上 Launcher 可打开并阅读 EPUB, 其它文件管理器 `ACTION_VIEW` 可唤起; 设置 / 发行历史 / 更新检查全流程通过; 权限集合仍为 D19.

---

## P5: 宿主契约, 插件能力服务与宿主客户端

目标: 宿主能经 Binder 打开一本 EPUB 取回元数据 / 目录 / 文本 / 封面 / 搜索结果, 能以 token 启动阅读器并收到事件; 插件中心正确展示双服务插件.

### P5.1 契约模块与插件中心注册

- [ ] (宿主) `plugin-api/epub-api` (附录 B 的 AIDL 与常量), `settings.gradle.kts` 的 `pluginApi` 列表, `app/build.gradle.kts` `implementation(project(":plugin-api:epub-api"))`, Manifest `<queries>` 增加 `org.autojs.plugin.EPUB`; 构建 AAR 并复制到插件 `libs/epub-api.aar` + 锁文件.
- [ ] (宿主) 插件中心四处注册: `InstalledPluginRepository.queryDeclaredPluginServices` 增加 `ServiceQuery(EpubActions.SERVICE_ACTION, category = "epub")` 与 `discoverPackage` 分支, `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE` 增加 `EpubIds.ENGINE`, `PluginCenterFragment.probeAidlPluginService`, `PluginDefaultEnabledPolicy` (Q7).
- [ ] (宿主/测试) D10 验证: 安装插件后插件中心只出现一个 `Readium EPUB Reader` 条目, 展示身份 (id / engine / 版本 / 描述 / 说明) 正确, 激活 / 启用 / 禁用对两个服务一致生效; 结论写入 `docs/dev/epub-plugin-protocol-v1.md` 的 "身份" 小节; 若分组取值不确定, 按 D10 的退路修改 `InstalledPluginRepository` 分组归并规则并补单元测试.

### P5.2 插件能力服务 (提取)

- [ ] (插件) `ReadiumEpubReaderPluginService` (`IEpubPlugin.Stub`): `getInfo` (engine `epub`, `REQUIRES_HOST_VERSION` = 契约首发宿主版本, `CONTRACT_VERSION=1`, `FEATURES`), `getCapabilities`, `openBook(pfd, options)` -> `EpubBookBinder`; `CallerGuard` 校验调用方包名 `org.autojs.autojs6` 与签名 (调试签名白名单只在 debug 构建); 并发上限 (同时打开 8 本), 每本空闲 5 分钟自动关闭.
- [ ] (插件) `EpubBookBinder`: `getMetadata` (标题 / 作者 / 语言 / 标识符 / 出版方 / 日期 / 描述 / 主题 / 版式 / 阅读方向 / 位置数), `getToc` (树 -> 扁平数组 + depth, 上限 5000), `getReadingOrder`, `getText(request{href?|index?, offset, limit})` (Readium `ContentService` 段落迭代, 纯文本, 单次上限 1 MiB, 分页续取), `openResource(href)` (只允许 manifest 内资源, 大小上限 64 MiB, 返回只读 PFD 管道), `search(request{query, offset, limit})` (上限 500), `getPositions`, `close`; 所有 Bundle 大小受附录 B.5 约束.
- [ ] (测试) JVM: `TextExtractorTest` (分块边界, 上限, 空章节), `TocFlattenerTest`, `LimitsTest`; instrumentation: 显式绑定 `IEpubPlugin`, 用样本 PFD 往返全部方法, 非法 href / 越界 offset / 超限 limit / 非 EPUB PFD 的错误码, 关闭后调用返回 `SESSION_CLOSED`, 绑定解绑不泄漏 FD.

### P5.3 阅读器会话

- [ ] (插件) `openReader(pfd, options{locator?|href?|progression?, preferences?}, callback)`: 预打开 `Publication`, 生成 128 位随机 token, 注册到 `ReaderSessionRegistry` (超时 60 s 未被 Activity 认领则关闭并发 `close(reason=timeout)`), 返回 `IEpubReaderSession`; `EpubReaderActivity` 收到 `EPUB_READER_OPEN` + token 后认领会话, 复用已打开的 `Publication`; 会话方法 `getState` (locator / 进度 / 章节 / 是否可见), `goTo` (locator / href / progression), `navigate` (`NEXT_PAGE` / `PREV_PAGE` / `NEXT_CHAPTER` / `PREV_CHAPTER`), `setPreferences` (附录 A.6 的子集), `getBookmarks`, `close` (结束 Activity).
- [ ] (插件) 事件: `open` (Activity 可见), `progress` (节流 500 ms, 含 locator / 总进度 / 章节), `bookmark` (added / removed), `close` (reason: user / host / replaced / timeout / error), `error`; `generation` 每次会话递增, `seq` 单调; 回调异常 (宿主死亡) 时关闭会话.
- [ ] (测试) instrumentation: 用测试 Activity 模拟宿主两步启动, 断言事件序列与 seq 单调, `goTo` 后 `getState` 一致, 重复 `openReader` 取代旧会话, token 错误的 Intent 被拒绝并 finish.

### P5.4 宿主客户端

- [ ] (宿主) `core/plugin/epub/` 全部类 (4.2 列表): `EpubPluginHost` 的 `discover / probe / queryServiceCount / selectOrThrow(engine = "epub")`, `openBook` 与 `openReader` 的专用绑定租约 (`callWithDedicatedBindingLease`), `EpubSource` (路径 -> 只读 PFD, 大小上限 Q-无 / 扩展名 / 魔数 `PK`), `EpubOutputSink` (封面 / 资源导出落盘), `EpubReaderLauncher` (显式 Component + action + token, `FLAG_ACTIVITY_NEW_TASK` 视宿主上下文而定), `EpubReaderBridge` (generation / seq / 节流), `EpubJson`, `EpubErrorMapper`.
- [ ] (测试) JVM: `EpubJsonTest`, `EpubReaderBridgeTest` (乱序 / 重复 / 旧 generation 丢弃), `EpubErrorMapperTest`; instrumentation (宿主 androidTest, 安装插件后): `EpubPluginHostRoundTripTest` (openBook 全方法, openReader + 启动 + 事件 + close).

### P5.5 协议文档与 changelog

- [ ] (宿主) `docs/dev/epub-plugin-protocol-v1.md` (身份, 发现, AIDL, Bundle key, 事件, 上限, 错误码, 两步启动, 版本协商, 混合版本行为), 宿主 `.changelog` 10 语言 (`feature`: 新增 EPUB 插件契约与 Readium EPUB Reader 接入), 本仓库 `.changelog`.
- [ ] (宿主) 宿主提交按逻辑拆分 (契约模块 / 客户端 / 注册与文档), 不推送.

验收: 宿主 androidTest 往返在两台设备通过; 插件中心单条目; 协议文档与两侧 changelog 同步.

---

## P6: 脚本 API `epub`

目标: 脚本可用同步与 Async 两种形态提取 EPUB 内容, 并可打开阅读器接收事件.

### P6.1 提取 API

- [ ] (宿主) `augment/epub/Epub.kt`: `epub.open(path | options)` -> `EpubBook`; 便捷层 `epub.metadata(path)`, `epub.toc(path)`, `epub.readingOrder(path)`, `epub.text(path, target?, options?)`, `epub.cover(path, outputPath)`, `epub.search(path, query, options?)`; 全部有 `*Async` (Promise); `EpubError` (code / message); 注册到 `ScriptRuntime`; 路径解析遵循脚本工作目录规则 (`files.path`).
- [ ] (宿主) `EpubBook`: `metadata`, `toc`, `readingOrder`, `positions`, `text(target?, options?)`, `textAll(options?)` (按阅读顺序拼接, 受总上限), `cover(outputPath?)` (无参返回 `Image`), `resource(href, outputPath)`, `search(query, options?)`, `close()`, `isClosed`; 脚本退出时自动关闭未关闭的书 (与其它资源清理同一钩子).
- [ ] (测试) JVM: `EpubObjectsTest` (JS 对象形状快照), `EpubTextOptionsTest`; 设备 smoke (Rhino 脚本, 两台设备): 打开三种样本, 打印元数据 / 目录 / 首章文本 / 保存封面 / 搜索; 未安装插件时错误码为 `PLUGIN_UNAVAILABLE` 且文案本地化.

### P6.2 阅读器控制

- [ ] (宿主) `epub.read(path, options?)` -> `EpubReaderSession` (EventEmitter): `on('open' | 'progress' | 'bookmark' | 'close' | 'error')`, `goTo(target)`, `next()` / `prev()` / `nextChapter()` / `prevChapter()`, `setPreferences(partial)`, `locator`, `progress`, `isOpen`, `close()`; `epub.progress(path)` 与 `epub.bookmarks(path)` 只读 (经 `openBook` 附带的 store 读取, 或阅读器会话); 脚本退出时关闭会话, 阅读器 Activity 保留为普通阅读 (D32).
- [ ] (测试) 设备 smoke: 脚本打开阅读器并在 `open` 事件后 `goTo` 第二章, 监听 `progress`, 人工翻页后事件到达, `close()` 后 Activity 结束; 宿主被杀后插件会话关闭 (通知栏无残留).

### P6.3 示例, 文档与关联仓库

- [ ] (宿主) `assets/sample/` 新增示例脚本 (10 语言目录规则同既有示例): `EPUB 元数据与目录`, `导出章节文本`, `打开阅读器并监听进度`; 宿主 `.changelog` 10 语言 (`feature`: 脚本 API `epub`).
- [ ] (文档) `AutoJs6-Documentation` (`api/epub.md` + 类型页 `epubBookType.md` / `epubReaderSessionType.md` / `epubLocatorType.md`), `AutoJs6-TypeScript-Declarations` (`aj6-int-epub.d.ts`, 全局 `epub` 与 `$epub`), `AutoJs6-Plugin-Ace-Editor` (内置声明再生成), `AutoJs6-Plugin-Offline-Docs` (同步); 各仓库按其 `AGENTS.md` 处理版本与提交.

验收: 三份示例脚本在两台设备运行通过; 文档 / d.ts / Ace / 离线文档均含 `epub` 且版本号已更新.

---

## P7: 健壮性, 安全, 兼容矩阵, 性能, 体积, 无障碍

- [ ] (插件/测试) 敌意输入矩阵 (P0.3 的损坏样本 + 新增): 非 zip, 空 zip, 缺 `mimetype` / `container.xml` / OPF, 坏 XML (含外部实体声明, 断言不解析外部实体), 路径穿越 href (`../`, 绝对路径, URL 编码), 超多条目 (50 000), 高压缩比单资源 (1 GiB 零字节), 超长文件名, 重复条目, 加密 (`encryption.xml` / LCP `license.lcpl`) -> 明确错误码与本地化提示, 不崩溃, 不写磁盘, 打开耗时有上限 (30 s 取消).
- [ ] (插件) WebView 边界复核 (D6 全部允许下的底线): `allowFileAccess=false`, `allowContentAccess=false`, 无自定义 `addJavascriptInterface`, Readium 本地服务域之外不响应 `file://`; `usesCleartextTraffic=true` (D31); 结论写入 `docs/dev/security-boundaries.md`.
- [ ] (测试) 兼容矩阵: AVD API 24, Sony G8441 API 28, Sony XQ-AT72 API 31, Redmi 22120RN86C API 33, Xiaomi 23046RP50C API 35, AVD API 36 / 37: 打开三种样本, 翻页, 搜索, 朗读 (真机), 进度恢复, 大字体, 夜间, 横屏, 分屏, 进程重建; WebView 版本记录; ColorOS 激活实测若无设备则明确记录 "未执行真实设备激活验证".
- [ ] (测试) 性能: 冷 / 热打开耗时 (1 MB / 20 MB / 200 MB / 5000 章样本), 首屏时间, 翻页帧率 (`dumpsys gfxinfo`), 搜索耗时, 位置计算耗时 (大书应在后台且不阻塞阅读), 内存峰值 (`dumpsys meminfo`), 朗读 30 分钟内存不增长; 结果表写入 `docs/dev/performance-baseline.md`, 与正确性测试分开.
- [ ] (插件) 体积: R8 规则最小化, 移除未用的 media3 解码路径 (`media3-exoplayer` 仅因 Readium 传递引入, 确认 R8 后不残留), 资源压缩 (`isShrinkResources`), 记录 release universal APK 体积并设预算 (Q-附录 D 由 P0.2 基线决定, 建议不超过基线 + 20%); `verifyNativePageAlignment` 零原生库; 16 KB 设备 (Xiaomi Pad) 安装运行一次.
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
