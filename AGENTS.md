# AutoJs6-Plugin-Three-Folio-EPUB AGENTS.md

本文件是本仓库的工程约定, 由 `docs/development/repository-standard.md` (AutoJs6 新插件仓库参考规范, 与 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 同源) 裁剪而来, 只保留对本仓库真实有效的条款. 路线图与阶段性决策见 `ROADMAP.md`; 本文件描述的是 "怎样改仓库", 路线图描述的是 "改什么".

## 1. 规则等级与本仓库的适用范围

- `MUST`: 必须遵循. `SHOULD`: 默认遵循, 偏离时在仓库文档中说明原因. `CONDITIONAL`: 仅在对应能力落地后适用.
- 用户在当前任务中的明确要求优先于本文件.
- 本仓库不包含原生库, 模型, 上游源码快照或 ABI 拆分, 参考规范中对应的 CONDITIONAL 条款不适用 (见第 5.4 节的省略理由).
- 本仓库是 Explorer Action 家族成员 (与 HTML Previewer / Markdown Previewer 同形), 同时承载 Readium Kotlin Toolkit 阅读器; 独立应用形态 (Launcher, `ACTION_VIEW`, 设置页, 发行历史, 手动更新检查; P4 已落地) 与 TTS 前台服务 (P3 已落地) 的规则见第 6, 14 节; `org.autojs.plugin.EPUB` Binder 服务 (P5.2 提取能力与 P5.3 阅读器会话均已落地) 与宿主 `epub-api` 契约的规则见第 6, 7, 8 节.

## 2. 仓库身份

下列值在 Gradle (`resValue`), Manifest, Kotlin 常量 (`ThreeFolioEpubPlugin`), 资源, 文档 (`.readme/common.json`), 测试和宿主注册信息中 MUST 完全一致. 修改任一值时同步修改全部位置, 并运行 `EpubReaderExplorerCompatibilityTest` 与 `PluginContractInstrumentationTest`.

| 项目 | 值 |
|---|---|
| 仓库与目录名 | `AutoJs6-Plugin-Three-Folio-EPUB` |
| `rootProject.name` | `autojs6-plugin-three-folio-epub` |
| 应用标题 (不可翻译) | `3-Folio EPUB` |
| `applicationId` / namespace | `io.github.supermonster003.autojs6.plugin.three.folio.epub` |
| 插件 ID / engine / variant | `three-folio-epub` / `explorer-action` / `default` |
| Explorer Action 动作 ID | `three-folio-epub.primary` (主按钮, placement 2) 与 `three-folio-epub` (溢出菜单) |
| 动作标签资源 / 兜底文案 | `action_three_folio_epub` / `3-Folio EPUB` |
| 执行 Activity | `EpubReaderActivity`, action `org.autojs.plugin.EXPLORER_ACTION_EXECUTE` |
| 发现服务 | `ExplorerActionService` (`org.autojs.plugin.EXPLORER_ACTION`), `PluginInfoService` (`org.autojs.plugin.INFO`) |
| 声明协议 / 最低宿主 / 审计宿主 | v2 / 5318 / 5318 (协议 v22), 单点定义于 `gradle/explorer-action-compatibility.properties` |
| 脚本全局对象 | `epub` (宿主侧, 路线图 D1 / P6, 尚未落地) |
| 专用 API | `epub-api` (宿主 `plugin-api/epub-api`, 契约版本 1 与 2: 对外基线 1, 最新 2 (P9.4); P5.1 起以 `libs/epub-api.aar` 锁定, 来源宿主提交见 `libs/README.md`) |
| 阅读引擎 | Readium Kotlin Toolkit `3.4.0` (`readium-shared` / `readium-streamer` / `readium-navigator` / `readium-navigator-media-tts`, 路线图 D2 / D18) |
| 持久化 (高亮 / 笔记) | Room `2.8.1` (`room-runtime` 运行时, `room-compiler` 经 KSP; KSP 插件版本来自平台版本插件的 `gradle.ksp.version`; schema 导出到 `app/schemas/`, 路线图 D4 / P9) |
| 平台版本插件 | `io.github.supermonster003.autojs6-platform-versions` 1.8.3 |
| 发布文件名 | `autojs6-plugin-three-folio-epub-v{VERSION_NAME}-{CRC32}.apk` (单 APK) |

## 3. 工作区与提交

### 3.1 会话开始

- MUST 运行 `git status --short`, 检查当前分支, 最近提交和相关文件差异.
- MUST 将已有未提交内容视为用户工作. 不覆盖, 不回滚, 不擅自整理与当前任务无关的改动.
- 禁止使用 `git reset --hard`, `git checkout -- <path>` 或其他可能丢失用户内容的命令, 除非用户明确授权.
- 先阅读 `ROADMAP.md` 的 "阶段总览" 与最后一条 "会话记录", 从路线图建议的起点开始.

### 3.2 开发过程

- 每个行为改动应同时考虑实现, 测试, 10 语言资源, README, changelog, 宿主入口和公共契约.
- 不提交本地缓存, IDE 状态, 调试输出, 真实书籍或无意生成的二进制文件; `docs/fixtures/` 只放生成器产物或许可证明确的样本 (第 15.4 节).
- Gradle 自动修改 `BUILD_TIME` 时, 在确认来源后与相关变更一并处理. 若 Gradle 修改 `VERSION_BUILD`, 必须按第 3.4 节的提交计数规则校正; `VERSION_NAME` 只按语义化版本规则调整.
- 修改第三方依赖时同步记录版本, 来源, 校验值与许可证 (`THIRD_PARTY_NOTICES.md`), 并在 changelog 的 `dependency` 分类记录.
- 路线图条目完成后在 `ROADMAP.md` 勾选并写入证据 (设备, API, 样本, 度量值), 不勾选没有证据的条目.

### 3.3 提交

- 除非用户明确要求本次会话不要提交, 会话结束前 MUST 将本次范围内的全部文件按逻辑提交, 一个路线图子项一个提交.
- 使用 Conventional Commits 风格: `feat:`, `fix:`, `docs:`, `build:`, `test:`, `ci:`, `chore:`, 可加作用域, 例如 `feat(reader): ...`, `feat(explorer): ...`, `feat(binder): ...`.
- 一个提交表达一个完整意图; 行为实现, 对应测试和对应 changelog 通常放在同一提交.
- 提交前 MUST 审阅 `git diff --check`, `git diff --cached`, `git status --short`, 确认没有密钥, 密码, 令牌, 本地路径, 临时 APK, 真实书籍或无关改动.
- 本仓库的提交作者邮箱 MUST 为 `30370009+SuperMonster003@users.noreply.github.com` (仓库级 `git config user.email`), 不使用个人邮箱.
- 会话结束时最终 `git status --short` 无输出; 若发现无法纳入本次提交的用户改动, 停止自动提交并向用户说明.

### 3.4 提交计数

- `VERSION_BUILD` MUST 与当前分支 `HEAD` 可达的 Git 提交数一致.
- 每次准备新提交时, 先用当前提交数加 1 得到即将产生的 build number, 写入 `version.properties`, 再把该文件与本次逻辑改动一并提交. 不要先写成当前提交数再提交.

```bash
next=$(( $(git rev-list --count HEAD 2>/dev/null || echo 0) + 1 ))
sed -i "s/^VERSION_BUILD=.*/VERSION_BUILD=$next/" version.properties
```

最后一笔提交完成后 MUST 验证 `VERSION_BUILD == git rev-list --count HEAD` 且 `git status --short` 无输出. 若发现不一致, 将 `VERSION_BUILD` 设置为 "当前提交数 + 1" 并创建一笔有明确含义的校正提交.

### 3.5 版本名称

- `VERSION_NAME` 从 1.0.0 开始, 按语义化版本管理, 与提交数量不绑定; 1.0.0 在路线图 P8 发布, 之前的提交都属于 1.0.0 的开发构建; 1.1.0 在路线图 P9 发布 (2026-09-21), P9.1 至 P9.5 的提交都属于 1.1.0 的开发构建. 未发布的 1.2.0 开发内容随本次改名纳入 2.0.0; 新安装身份与 5318 宿主下限见文末身份决策.
- 修改 `VERSION_NAME` 时同步更新全部 changelog JSON 的版本 key, README, 发布文件名断言与测试夹具, 再运行文档生成器.

## 4. 仓库结构

```text
AutoJs6-Plugin-Three-Folio-EPUB/
|-- .changelog/                 lang_*.json x 10 + template_changelog.md (文案源)
|-- .github/workflows/          build.yml, markdown.yml
|-- .python/                    generate_markdown.py (+ .bat), check_markdown.bat, generate_launcher_icons.py, generate_fixtures.py, tests/
|-- .readme/                    common.json, lang_*.json x 10, template_readme.md, README-*.md (生成)
|-- app/                        Android 插件 (Explorer Action 服务, 阅读器, 资源, JVM 与 instrumentation 测试)
|   |-- sm003.jks               本地签名密钥, Git 忽略
|   `-- src/{main,test,androidTest}
|       `-- main/java/.../reader/          插件根包: 服务, Activity, 策略, 兼容记录
|           `-- book/                      PfdResource, BookOpener, BookFingerprint, ByteRanges (Readium 接入层)
|-- build-logic/                org.autojs.build.{utils,versions,signs,jvm-convention,...} 约定插件
|-- docs/                       16kb.md, explorer-action-compatibility.md, development/repository-standard.md
|   |-- dev/                    阶段证据 (p0-readium-spike.md 等)
|   |-- fixtures/               生成的 EPUB 样本 + SHA256SUMS.txt + README.md (androidTest assets 来源)
|   `-- images/evidence/        真机截图证据 (降采样 PNG, P2.3 起)
|-- gradle/                     libs.versions.toml, explorer-action-compatibility.properties, wrapper/
|-- libs/                       宿主 API AAR (哈希锁定, 见 libs/README.md)
|-- locks/                      host-api-aars.lock
|-- AGENTS.md, ROADMAP.md, README.md (生成, 简体中文), LICENSE (MPL-2.0), THIRD_PARTY_NOTICES.md
|-- build.gradle.kts, settings.gradle.kts, gradle.properties, version.properties
`-- sign.properties             本地签名配置, Git 忽略
```

不要仅为目录整齐创建空模块. 宿主侧的契约模块, 脚本 API 与文档位于各自仓库 (第 10 节), 不放入本仓库. `tools/` 被 `.gitignore` 忽略, 可复用的脚本一律放 `.python/`.

## 5. Gradle 与版本平台

### 5.1 在线平台版本插件

- MUST 使用在线仓库中的 `io.github.supermonster003.autojs6-platform-versions` (当前 1.8.3). 升级时先确认新版本已能从公共仓库解析, 并与其他官方插件仓库统一升级.
- 禁止使用 `mavenLocal()`, 禁止本地平台版本实现, 禁止提交 `gradle/data` 消费端覆盖.
- 平台插件只在根 `settings.gradle.kts` 应用一次, 且整个 `plugins` 块位于 `includeBuild("build-logic")` 之前; `build-logic/settings.gradle.kts` 不应用它.
- 根 `build.gradle.kts` 用 `System.getProperty("gradle.agp.version")` 声明 `com.android.application` 并 `apply false`; 模块只应用插件, 不硬编码版本. 版本逃生门只用 `version.properties` 的 `OVERRIDDEN_*`, 常规构建保持 `NONE`.
- `app` 模块从 `version.properties` 和 `org.autojs.build.versions` 读取 compileSdk, minSdk, targetSdk, versionCode, versionName.
- 不声明 `org.jetbrains.kotlin.android`; Kotlin 支持由 AGP 内置能力与约定插件提供. `gradle/libs.versions.toml` 的 AndroidX 版本与 Readium 3.4.0 传递依赖保持一致, 不低于 Readium 的要求.
- `isCoreLibraryDesugaringEnabled = true` MUST 保留 (Readium 经 kotlinx-datetime 使用 `java.time`, minSdk 24).

验收命令 (模拟 GitHub Actions 的 Temurin 环境, 日志 MUST 只有一段 `Version information for IDE platform and Gradle plugins`):

```powershell
.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' :app:assembleDebug :app:testDebugUnitTest
```

### 5.2 仓库边界

- Gradle 构建 MUST 自包含. 禁止引用仓库外部的 JAR, AAR, `flatDir` 或兄弟项目路径 (例如 `../AutoJs6/...`).
- 宿主 API AAR MUST 复制到 `libs/` 并在 `locks/host-api-aars.lock` 记录小写 SHA-256; `app/build.gradle.kts` 在配置期校验文件存在, 非 debug 命名, 哈希匹配, 锁文件键集合精确, 且 `explorer-action-api.sha256` 与 `gradle/explorer-action-compatibility.properties` 的 `explorerActionApiSha256` 一致. 更新 AAR 时同步更新锁文件, 兼容属性, `docs/explorer-action-compatibility.md`, `THIRD_PARTY_NOTICES.md` 与契约测试. `epub-api.aar` (P5.1) 单独记录来源宿主提交 (`libs/README.md`); 三个 AAR 不要求出自同一宿主提交, 但被替换时锁文件, 声明文件与 `libs/README.md` 必须在同一提交更新.
- `explorer-action-api.aar` 是冻结的 v1 描述符 (协议 v2 复用); 升级到 v4+ 需要不同的 host session 资源模型, 只改目录版本号是禁止的 (见兼容矩阵 "Upgrade procedure").
- 宿主与插件需要同步更新时分别修改各仓库 (宿主 `D:/idea-projects/AutoJs6`), 不通过跨仓库相对路径制造隐式耦合.

### 5.3 签名与发布构建

- `sign.properties` 与 `app/sm003.jks` 从宿主复制到相同相对路径, MUST 保持被 Git 忽略 (`git check-ignore` 验证). 仓库中不得出现密码, token, 私钥或开发者绝对路径; `local.properties` 同样忽略.
- 保留 `org.autojs.build.signs`, `signingConfigs` 与 release 签名选择逻辑.
- `appendDigestToReleasedFiles` 任务 MUST 保留该名称, 依赖 `assembleRelease` 与 `verifySignedReleaseArtifacts`, 在签名缺失时失败, 校验实际 APK 集合恰为 `autojs6-plugin-three-folio-epub-v{VERSION_NAME}.apk`, 并追加 CRC32 生成 `autojs6-plugin-three-folio-epub-v{VERSION_NAME}-{CRC32}.apk` 到 `app/releases/` (不入库).

### 5.4 不启用 ABI 拆分的理由

插件完全由 Kotlin / Java 字节码与普通资源构成 (Readium 及其传递依赖 media3, jsoup, kotlinx 与 Room 的 `room-runtime` 均为纯 JVM 库; Room 使用系统 SQLite, 不引入 `sqlite-bundled`), 拆分包内容实质相同, 不会带来下载或兼容性收益. 因此:

- 不配置 `splits.abi`, 不配置 `ndk.abiFilters`, 每次发布只有一个 APK; `nativeAlignment { expectNoNativeLibraries }` 在构建期拒绝意外引入的原生库.
- `getInfo()` MUST 显式写有 `supportedAbis = emptyArray()`, 测试断言其为显式空数组.
- 16 KB page size 检查不适用 (`NATIVE_PAGE_ALIGNMENT` meta-data 为 0); 若未来引入含原生库的依赖, 本节作废并需补齐 ABI 与 16 KB 验证.

### 5.5 R8 与 Readium

- `app/proguard-rules.pro` 只保留插件包, `PluginInfo` 与 Explorer Action API; Readium 通过各 AAR 自带的 consumer 规则保活, 不在本仓库复制其规则. 任何 R8 缺失类告警 MUST 以显式规则或依赖调整解决, 不用 `-dontwarn` 一刀切.
- 引入或升级运行时依赖后 MUST 执行 `:app:assembleRelease` (缺失类只会在这里暴露), 并把 release APK 体积记入 changelog 或 `docs/dev/`; Readium 升级需重跑路线图 P0.2 的完整清单 (D18).
- `isShrinkResources = true` 保持开启; 由 Readium 导航器按名称加载的资源 (Readium CSS, 脚本资产) 由其 consumer 规则保护, 若出现运行时缺资源, 先查 R8 报告再决定是否 `keep`.

## 6. Manifest 与激活协议

- Manifest MUST 声明 `org.autojs.permission.PLUGIN`, `<queries>` 宿主包名, `org.autojs.plugin.WAKE_ACTIVITY` 与 `org.autojs.plugin.info.AUTHOR` meta-data, `NATIVE_PAGE_ALIGNMENT=0`.
- `WakeActivity` MUST 为 `exported=true`, `Theme.NoDisplay`, `excludeFromRecents`, `finishOnTaskLaunch`, 受 PLUGIN 权限保护, 响应 `org.autojs.plugin.action.WAKE` + DEFAULT category, 启动后立即结束, 不做任何副作用.
- `PluginInfoService`, `ExplorerActionService` 与 `EpubReaderActivity` MUST `exported=true` 且受 PLUGIN 权限保护; `EpubReaderActivity` 只响应 `org.autojs.plugin.EXPLORER_ACTION_EXECUTE` (以及自家朗读通知的显式组件 Intent `ACTION_RESUME_READ_ALOUD`: 不带数据, 只领回进程内托管的朗读会话, 无会话时直接关闭; 路线图 D26), 独立入口 (路线图 P4): `launcher.LauncherActivity` (`MAIN` / `LAUNCHER`, 导出且不带权限, 不接收任何数据) 作为四个图标别名的真实目标 Activity, 它以自家显式动作 `EpubReaderIntentPolicy.ACTION_OPEN_RECENT` (带读授权标志的纯 `content://` 文档 + 显示名) 打开 `EpubReaderActivity` (自家组件不受 PLUGIN 权限限制; 该动作只接受显式组件 Intent, 没有 ClipData, 授权已失效时 `startActivity` 抛 `SecurityException` 由启动器标记不可用); `ExternalViewerActivity` (P4.2, D27) 承载 `ACTION_VIEW` + `content` scheme + `application/epub+zip` (不加 `application/octet-stream` / pathPattern 兜底), 导出且不带权限, 继承 `EpubReaderActivity` 只改入口校验 (`RequestReceiver.EXTERNAL_VIEWER`, 显示名先问 provider 的 `_display_name`), 不把执行 Activity 直接导出给任意应用; 该入口打开的书不入最近列表, 除非用户在溢出菜单选 `加入最近书籍` (`takePersistableUriPermission` 成功才 `RecentBooksStore.upsert`, 失败提示不入列).
- `service.ThreeFolioEpubPluginService` (路线图 P5.2 / D10) MUST `exported=true` 且受 PLUGIN 权限保护, intent-filter 只含 `org.autojs.plugin.EPUB` + category `epub` (不加 DEFAULT category, 不响应 INFO / EXPLORER_ACTION), 由宿主按 action + category 发现; `CallerGuard` 在每次 Binder 调用分发前校验调用 uid 属于 `org.autojs.autojs6` 且签名与插件一致 (`checkSignatures`), 插件自身 uid 只在 debug 构建放行 (instrumentation 用); 服务不持有 URI 授权, 不从后台启动 Activity, 解绑时关闭全部书籍与阅读器会话.
- `EpubReaderActivity` 自 P5.3 起多一个 intent-filter `org.autojs.plugin.EPUB_READER_OPEN` + DEFAULT (仍在 PLUGIN 权限之后): 只接受显式组件 + 32 位十六进制会话令牌 (`HostSessionPolicy`), 令牌以常量时间比较, 不匹配 / 已认领 / 已关闭的令牌只显示无效请求面板, 不读取任何数据; 该 Activity 只由宿主启动 (D12), 插件服务从不自行启动它.
- 所有对外组件逐项审查 `android:exported`; 除契约入口 (含 P5.2 的 EPUB 服务), `LauncherActivity`, 指向它的四个图标 alias 与 `ExternalViewerActivity` (P4) 外不得导出其他组件. `settings.SettingsActivity` 与 `settings.ReleaseHistoryActivity` (P4.3) 不导出, 不接收数据, 只从启动器菜单与阅读器溢出菜单进入.
- `android:usesCleartextTraffic="true"` 是维护者决定 (路线图 D31: 书内 `http://` 资源照常加载), Manifest 注释 MUST 保留该说明; 更新检查 (D28) 仍只走 HTTPS.
- 权限清单为 `INTERNET`, PLUGIN 与 D15 三项 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`); 不申请存储, 媒体, 无障碍或悬浮窗权限 (D19). 新增任何权限 (含普通权限) MUST 在本节显式记录豁免理由, 并同步 `PluginContractInstrumentationTest` 的权限集合断言, README 安全章节与 changelog. 导出组件审计同样在该用例 (P4.1 起断言 当前启用的图标 alias 是唯一导出且无权限的 `MAIN` / `LAUNCHER` 入口且 targetActivity 为 `LauncherActivity`; P4.2 起断言 `ExternalViewerActivity` 导出且无权限, `content://` + `application/epub+zip` 的 `ACTION_VIEW` 只解析到它, `application/octet-stream` / `file://` / `https://` 无解析; P4.3 起断言 `SettingsActivity` 与 `ReleaseHistoryActivity` 不导出; P5.2 起断言包内恰好四个服务, EPUB 服务导出且受权限保护, 只由 action + category 解析, 不解析 INFO / EXPLORER_ACTION).
- D15 权限豁免 (2026-09-20, 路线图 P3): `FOREGROUND_SERVICE` 与 `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 仅用于用户显式开始朗读后的 `tts.TtsForegroundService` (不导出的 `mediaPlayback` 前台服务, 承载 media3 MediaSession 通知与耳机按键, 使熄屏后朗读继续); Android 13+ 的 `POST_NOTIFICATIONS` 仅在首次开始朗读时请求一次, 拒绝后照常朗读但没有通知控制, 不再重复请求. 服务只在朗读期间存在: 用户停止, 到达书末, 引擎出错或阅读器 Activity 销毁 (D26 关闭时) 即停止并释放; 它不持有 URI 授权, 不新增数据收集或网络用途 (语音合成由系统 TTS 引擎在其自身进程完成), `WAKE_LOCK` 仍然移除.

## 7. PluginInfo 与能力协商

- `PluginRuntimeInfo.kt` 的 `threeFolioExplorerPluginInfo()` 负责 Explorer Action 身份的 Android 侧读取 (包版本, 本地化描述, `@raw/plugin_instruction`, 构建日期); 身份常量集中在 `ThreeFolioEpubPlugin`, 审计数值集中在 `EpubReaderExplorerCompatibility` (来自 BuildConfig). `threeFolioEpubPluginInfo()` 与 `epubCapabilities()` (P5.2) 构造 EPUB 服务的身份: 与 Explorer 身份共用同一构造函数, 只换 `engine` (`EpubIds.ENGINE`) 与 `capabilities` (`EpubCapabilityKeys` 五键: 宿主版本, 契约版本基线 `epubContractVersion` = 1, 最新版本 `epubMaxContractVersion` = 2 (P9.4, `service/ContractVersions`), 特性数组, Readium 版本); `ThreeFolioEpubPlugin.EPUB_FEATURES` 是特性数组的唯一来源 (P5.2: `search`, `cover`, `resource-export`, `markdown`; P5.3: `reader-session`; 不声明 `tts`, 因为契约 v1 没有朗读控制面, 声明了宿主也无从调用).
- `name` 与不可翻译的 `app_name` 一致; `description` 来自当前 locale 的 `plugin_description`; `versionName` / `versionCode` 来自 `PackageInfo`; `versionDate` 来自 `plugin_version_date` (`MMM d, yyyy`, `GMT+08:00`); `id` / `engine` / `variant` 与第 2 节一致.
- `capabilities` 包含 `PluginCapabilityKeys.REQUIRES_HOST_VERSION` (Long) 与 `ExplorerActionCapabilityKeys.PROTOCOL_VERSION` (Int); 路线图 P5 的 `epub` 服务另有 `EpubCapabilityKeys`, 两个服务的 `id` / `variant` / 版本字段 MUST 一致, 只有 `engine` 与 `capabilities` 不同 (D10).

## 8. Explorer Action 契约与 Binder

- 公共常量, key, 动作 ID 与 placement MUST 来自 `explorer-action-api` AAR 与 `ThreeFolioEpubPlugin`, 禁止散落字符串字面量.
- 目录 (`threeFolioEpubActionCatalog()`) 声明恰好两个动作 (主按钮 + 溢出), `TARGET_FILE`, `ACCESS_READ_ONLY`, MIME `application/epub+zip`, 扩展名 `epub`; 目录键集合由 instrumentation 测试精确断言.
- `EpubReaderIntentPolicy.resolve(intent, receiver, suppliedName)` 是唯一的入口校验: 先把 Intent 压成 Android-free 的 `RequestShape` 交给 `EpubRequestPolicy.admit` (P4.2: 接收组件决定可用动作, 阅读器只开 Explorer 信封与 `ACTION_OPEN_RECENT`, 外部查看器只开 `ACTION_VIEW`; 所有入口都要求纯 `content://` 文档 (非空叶段, 无 query / fragment) + 读授权 + 显示名清洗 + EPUB 格式门 `isSupportedEpub`; Explorer 另需前缀授权与两项 ClipData, 启动器动作需显式组件且无 ClipData, 外部入口拒绝 `tree/<id>` 目录 URI 与 `vnd.android.document/directory`), 再补 Explorer 信封的 Android 侧字段: 动作 ID, 协议版本恰为 v2, 宿主版本 >= 5269, 主界面来源, 父目录与父子关系 (`EpubReaderPathPolicy.isDescendant`), ClipData 两项内容. 任何一项不满足即拒绝, 不猜测.
- 父目录 URI 只做校验, 永不访问 (EPUB 自包含, D9).
- `org.autojs.plugin.EPUB` 服务 (`service/` 包, P5.2 起): 已发布 AIDL 演进只在末尾追加方法并通过契约版本协商 (P9.4: 能力里 `epubContractVersion` 恒为基线 1 以保持旧宿主接纳, `epubMaxContractVersion` 为最新 2; 宿主把协商结果写进 `openBook` / `openReader` 的 options, `ContractVersions.of` 取出后作为该书籍 / 会话整个生命期内每个应答与事件的 `KEY_CONTRACT_VERSION` (缺失或更低为 1, 更高钳制为最新); 版本 2 追加 `IEpubBook.getAnnotations` (分页, 阅读顺序, `MAX_ANNOTATIONS_PAGE` / `MAX_ANNOTATIONS_BYTES` 截断, 版本 1 的书籍答 `INVALID_ARGUMENT`) 与会话的 `highlight` 事件 (`added` / `updated` / `removed`, 只发给版本 2 宿主)); 请求 / 响应为 `Bundle` 固定 key 下的 JSON (`book/BookJson` 生成元数据, 目录, 阅读顺序与搜索命中文档), 每个 Bundle 答复都带 `KEY_CONTRACT_VERSION`, 失败以 `KEY_ERROR_CODE` / `KEY_ERROR_MESSAGE` 返回, 非 Bundle 方法 (`openBook`, `openResource`, `openReader`) 抛以错误码开头的 `IllegalArgumentException` / `IllegalStateException` (`Answers`); 所有 Binder 输入经 `Limits` 边界校验, 上限常量只来自 `EpubContract` (与路线图附录 B.5 一致), 错误详情按 UTF-8 字节截断; `openBook` 先校验 options 的 parcel 大小, 描述符为普通文件 (`fstat`) 与 8 本并发上限再解析, 打开超时 30 s, 失败时关闭自己的描述符副本; `BookRegistry` 执行 8 本上限与 5 分钟空闲回收, 解绑与销毁关闭全部; `getText` 用 jsoup 遍历该资源的 XHTML DOM (`book/HtmlBlockExtractor`: 标题级别, 引用, 脚注, 列表项, 代码块, 图片与 figcaption, 表格行; Readium 的 content 迭代器把所有文本标为 Body, 因此不用它; jsoup 在版本目录中与 Readium 3.4.0 的传递版本对齐), `book/TextExtractor` 渲染纯文本或轻量 Markdown 并按 `KEY_MAX_CHARS` 分块 (不切代理对); `openResource` 只解析 manifest 内资源, 校验 64 MiB 上限后经可靠管道流式返回 (`DescriptorIo`, 读失败 `closeWithError`); `search` 复用 `SearchResultPager` (`offset + limit <= 500`); 阅读器会话经宿主两步启动 (D12, P5.3), 插件服务不从后台启动 Activity.
- 阅读器会话 (`service/ReaderSession` / `ReaderSessionRegistry` / `ReaderSessionBinder`, P5.3): `openReader` 用阅读器自己的字体容器打开书籍, 校验起始位置 (`SessionTarget`: locator JSON <= 16 KiB 且 href 在书内, href 经 `Limits.href` 且可在 manifest 解析, 进度在 0..1) 与偏好 (`ReaderPreferencesJson`: 契约 `PREFERENCES` 子集, 类型与范围严格, 范围取自 `PreferenceRanges`, 未知键收集为 `UNSUPPORTED_PREFERENCE` 错误事件而非拒绝), 然后由注册表登记 (进程内单会话; 新会话把旧会话以 `replaced` 关闭并结束其阅读器; 60 s 内无人认领以 `timeout` 关闭并释放书籍); 令牌 (`HostSessionPolicy`, SecureRandom 128 位) 只经 `getState` 交给宿主, Activity 以 `ReaderSessionRegistry.claim` 认领后 `EpubReaderViewModel.adoptSession` 接管 publication 与描述符 (会话此后只查询 publication); 事件经 `IEpubReaderCallback.onEvent(generation, seq, bundle)`, 每会话一个 generation, seq 严格递增, `open` 一次 (导航器首个 locator), `progress` 经 500 ms 节流 (`ProgressThrottle`), `bookmark` 按书签列表差分 (首个列表为基线), `error` 用于未知偏好键与不可达的 `goTo` 目标, `close` 恰好一次并带原因, 事件 parcel 超过 32 KiB 先去掉 locator / bookmarks 再放弃; 认领前 `goTo` 改写起始位置, `setPreferences` 合并为待应用补丁, `navigate` 为 `READER_NOT_VISIBLE`; 认领后三者经主线程交给 Activity / ViewModel (`ReaderSessionController` / `SessionAdopter`), 高级偏好 (行高, 对齐, 连字符) 像面板一样关闭出版商样式; 宿主 `close` 只在 `KEY_FINISH` 为 true 时结束 Activity (D32), 否则阅读器作为普通阅读器继续; 回调 Binder 死亡 / 解绑时静默结束会话, 阅读器保留; 用户离开阅读器 (ViewModel 清理) 向宿主发 `close(user)`; 会话书籍不进最近列表.

## 9. 阅读器核心与网络约束

- 书籍 MUST 只经只读 `ParcelFileDescriptor` 或 `content://` URI 进入 (D11): `book/PfdResource` 以 `FileChannel` 定位读实现 Readium `Resource`, `sourceUrl` 为 null 以走 `StreamingZipArchiveProvider`; 任何路径都不把 EPUB 复制到缓存或解压到磁盘. `PfdResource.close()` 关闭描述符, 调用方不得重复关闭.
- `book/BookOpener` 是唯一的 Readium 打开入口 (`AssetRetriever` + `DefaultPublicationParser` + `PublicationOpener`, `pdfFactory = null`), 先确认 `Format.conformsTo(Specification.Epub)` 再解析; 错误映射为 `BookOpenError`, 其 `message` 可直接展示. LCP 标记的书籍由 Readium fallback content protection 拒绝, 不做解密.
- `book/BookFingerprint` (D23): `quickKey` (大小 + 首 1 MiB + 末 64 KiB) 为临时键, `fullKey` (全文件 SHA-256) 为正式键; 二者与 `ByteRanges` 保持 Android-free 以便 JUnit 覆盖.
- 书内脚本与远程资源保持 Readium 默认行为 (D6): 不剥离 `<script>`, 不拦截请求, 不注入 Readium 之外的 JavaScript 接口, 不开放 `file://`. 外部链接 (`reader/LinkPolicy`, D25): `http` / `https` 默认先确认再交给系统浏览器, 菜单可改为直接打开; `mailto:` / `tel:` 交给系统; 其它 scheme 拒绝. Readium 3.4.0 只把层级 URL 交到 `onExternalLinkActivated`, `mailto:` / `tel:` 由 WebView 自行处理, 书内链接到达回调时已丢失 fragment (路线图 P2.7 附带发现 2 / 3).
- 键盘翻页在 `EpubReaderActivity.dispatchKeyEvent` 截获 (焦点视图 `onCheckIsTextEditor()` 为真时放行), 不依赖 Readium `InputListener.onKey`: 有焦点的 WebView 会吞掉方向键, 且导航器只转发带 `KeyboardEvent.code` 的按键 (路线图 P2.7 附带发现 1). 点按区 / 键盘 / 外链 / 图片降采样策略保持 Android-free (`reader/PageTurnPolicy`, `reader/LinkPolicy`, `reader/ImageDecoding`).
- 阅读器状态 (`Publication`, `EpubNavigatorFactory`, 最近 `Locator`) 只驻留 `EpubReaderViewModel`; 进程被杀后从 Intent 重新打开, 不恢复导航器片段 (`createDummyFactory`).
- 程序化跳转 (目录, 从头开始, 后续的书签 / 搜索结果) MUST 经 `EpubReaderActivity.jumpTo` 排队到导航器就绪 (`PaginationListener.onPageChanged` 首次触发) 之后再调用 `go()`: Readium 3.4.0 在初始资源加载完成前收到 `go()` 会永久停止 `currentLocator` 更新 (路线图 2026-09-19 会话记录).
- 插件不上报遥测, 不发起书籍之外的网络请求; 手动更新检查 (P4, D28) 只访问 GitHub Releases 且只走 HTTPS.
- 朗读 (路线图 P3): `tts/TtsController` 驻留 `EpubReaderViewModel` 并持有 Readium `TtsNavigator` (`tts/TtsSession`), `tts/TtsForegroundService` (media3 `MediaSessionService`, 不导出, `mediaPlayback`) 只持有 MediaSession 与通知, 上一句 / 下一句 / 停止走自定义 `SessionCommand` (Readium 的 media3 适配器不声明 seek 命令), 耳机上一曲 / 下一曲映射到句子; 关闭阅读器, 手动翻页或跳转都停止朗读. 引擎经 `tts/SystemTtsEngine` (改编自 Readium `AndroidTtsEngine`, 保留 BSD-3 版权头) 绑定: 无参 `TextToSpeech` 构造只在已装引擎为系统应用时才会回退到它, 小米设备的小爱引擎装在 `/data/app` 且 `tts_default_synth` 为空, 默认查找失败而显式包名可用 (API 33 Redmi 12C 探针: 无参 24 ms 失败, 指定 `com.xiaomi.mibrain.speech` 317 ms 成功 4 个语音), 因此默认失败后按 `SystemTtsEngine.fallbackEngine` 指定引擎并在重连时复用; API 30+ 还 MUST 在 `<queries>` 声明 `android.intent.action.TTS_SERVICE` 与 `android.speech.tts.engine.INSTALL_TTS_DATA`, 否则引擎对插件不可见. 语音 / 语言与引擎选择策略保持 Android-free (`tts/TtsVoicePolicy`, `SystemTtsEngine.fallbackEngine`). 后台朗读 (D26): `ReaderSettings.readAloudInBackground` (默认关) 开启且正在播放时, `EpubReaderViewModel.onCleared` 把会话连同 `Publication` / `PfdResource` (`tts/ReadAloudHandle`, `OrphanBook`) 托管给 `TtsForegroundService` (服务收事件 / 定时器 / 通知停止, 停止时写入进度并关闭书籍), 通知点击以 `ACTION_RESUME_READ_ALOUD` (新任务) 重开阅读器并领回会话与书籍, 经宿主再次打开同一本书 (`bookKey` 相同) 时 `openBook` 领回会话并从朗读句开始; 阅读器与服务任一时刻只有一方持有会话. 睡眠定时器住在 `TtsSession` (固定时长 `delay`, 本章结束等 `location.href` 变化), 到期发 `TtsEvent.SleepTimerEnded` 由持有者停止, 每次会话单独设置不持久化; `readAloudKeepScreenOn` 只在朗读期间给窗口加 `FLAG_KEEP_SCREEN_ON`; 定时器算术保持 Android-free (`tts/SleepTimerPolicy`).
- 纯逻辑 (Intent 策略, 路径策略, 指纹, 范围裁剪, 目录扁平化, 版本比较, 偏好编解码与主题配色 (`prefs/`), 字体文件校验与字体目录编解码 (`fonts/`), 后续的文本分块) 保持 Android-free, 由 JUnit4 覆盖.

## 10. 主项目职责

若改动同时需要修改 `D:/idea-projects/AutoJs6`, MUST 遵循:

- 宿主只保留入口 (`FileUtils.TYPE.EPUB` / `PreviewerType.EPUB` / `ExplorerDocumentPreviewerPluginUi.specFor` 引导, 归档上限按类型 (D24), 契约模块, Binder 客户端, 脚本 API `epub`), 插件拥有解析与渲染的真实实现 (D20); 插件未安装或被禁用时宿主不得提供重复实现.
- 宿主先区分 `未安装`, `已安装但未激活或禁用`, `版本不兼容`, `调用失败`, `可用`, 各状态有对应提示与引导 (安装来源, 激活按钮, 所需版本).
- 更新包名, action, category, ID 或 API 时同步检查宿主注册表 (`InstalledPluginRepository`, `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE`, `PluginDefaultEnabledPolicy`), ProGuard/R8, 安装 URL, 启用状态缓存和测试夹具.
- 涉及公开脚本 API 时再同步 `AutoJs6-Documentation`, `AutoJs6-TypeScript-Declarations`, `AutoJs6-Plugin-Offline-Docs`, `AutoJs6-Plugin-Ace-Editor`.

## 11. 应用标题与字符串资源

- 用户可见字符串 MUST 覆盖 `values`, `values-en`, `values-ar`, `values-es`, `values-fr`, `values-ja`, `values-ko`, `values-ru`, `values-zh`, `values-zh-rHK`, `values-zh-rTW`; `values` 与 `values-en` 共有条目内容一致, 各语言占位符与转义一致 (`.python/tests/test_repository_contract.py` 与 `generate_markdown.py --check` 共同校验).
- `app_name` 位于 `strings_donottranslate.xml` 且 `translatable="false"`; `plugin_author`, `plugin_id`, `plugin_engine`, `plugin_variant`, `plugin_requires_host_version`, `plugin_version_date` 由 Gradle `resValue` 生成.
- 每个 locale MUST 有 `plugin_description`, 与 `.readme/lang_*.json` 的 `text_plugin_synopsis` 逐字一致: 简洁说明能力, 句尾不加终止标点, 不写 "EPUB 插件" 前缀, 不写 "适用于 AutoJs6" 等限定表述.
- `<string>` 按 `name` 升序; plurals 与数组放入各自文件.
- 所有资源与文档字符串使用 ASCII 标点 (`, . : ; ! ? ( ) [ ] / -`), 省略号用 `...` 并加 `tools:ignore="TypographyEllipsis"`; 禁止全角标点, 顿号, 弯引号 (生成器与仓库契约测试会扫描).

### 11.1 启动器图标

- 原有绿色书本品牌资源及 `.python/generate_launcher_icons.py` 保持不变. 新启动器选项由 `.python/generate_launcher_variants.py` 复用同一 `draw_glyph` 造型生成独立的启动器与插件中心黑白资源, `--check` 只读校验. 不手工编辑 PNG, 不改变 README 或应用内品牌引用.
- 设置页提供自适应亮色, 自适应暗色, 自适应自动 (默认), 透明背景四项. 固定亮色为 `#272727` / `#FAFAFA`, 暗色为 `#D8D8D8` / `#212121`; 自适应系统资源独立命名为 `ic_launcher_system*`, 透明选项为 `ic_launcher_transparent` 及其 night 变体. 自动选项必须有独立资源 ID: 默认暗色与 notnight 亮色 bitmap XML, 并配套 default / notnight 的 v26 adaptive XML. 禁止 values mipmap alias: PackageManager 在安装解析时会提前解引用并锁定图标 ID.
- 四个稳定 `${applicationId}.launcher.*IconAlias` 指向原 `.launcher.LauncherActivity`, 原 Activity 保持启用并仅移除 MAIN / LAUNCHER filter. 任意时刻只有一个别名可在启动器解析. `LauncherIcons` 使用 PackageManager 持久化选择, 先启用目标再禁用旧入口, API 33+ 最终状态批量应用, DONT_KILL_APP, 失败回滚, 不轮询或清除启动器数据.
- 设置项说明自动主题可能被启动器缓存, 透明背景可能被系统添加背景/遮罩, 切换后部分主屏幕快捷方式可能需要重新添加. 新选项覆盖旧启动器配色限制; 原书本造型及应用品牌仍保留.
- `LauncherIconResourceTest` 和 `LauncherIconOptionsTest` 验证资源亮暗/API选择, 四别名目标与唯一入口, 实际切换与精确恢复原状态. 偏移与抗锯齿之后的真实非零 alpha 必须在 66 dp 自适应安全圆内. 不把资源测试当作所有启动器自动刷新保证.

## 12. README 与多语言生成

- README 与 changelog MUST 由 `.readme/*.json`, `.changelog/*.json` 与模板通过 `.python/generate_markdown.py` 生成; 生成产物不得手工编辑. `raw*/plugin_instruction.md` 手工维护, 生成器只校验存在与字符卫生.
- 修改 JSON 或模板后先运行 `py .python/generate_markdown.py`, 再运行 `py .python/generate_markdown.py --check` (CI `markdown.yml` 也会执行). 生成器校验语言集合, JSON 键与列表形状, 全角符号, 未替换占位符, 版本对齐, Explorer Action 兼容属性与矩阵, 孤儿产物与漂移.
- 根 `README.md` 是简体中文版本, 与 `.readme/README-zh-Hans.md` 同源; 语言导航必须出现 `简体中文`.
- README 先说明用户能完成什么, 再说明安装与使用; `p_status` 段 MUST 如实标明当前构建已交付与尚未交付的能力 (未落地的路线图能力不得写成已提供). 不写 Android Studio 或 IntelliJ IDEA 版本信息, 不向普通用户解释 `supportedAbis`, 签名过程等实现细节.
- README 链接必须指向本仓库的真实 release, issue, license 与生成 changelog.

## 13. Changelog

- `.changelog/` 只存放 10 个 `lang_*.json` 与模板; 生成的多语言 changelog 位于 `app/src/main/assets/doc/`.
- 涉及 `feature`, `fix`, `improvement`, `dependency` 的提交 MUST 更新当前 `VERSION_NAME` 对应 `vX.Y.Z` 条目的全部语言 JSON, `released_date` 更新为当日 `YYYY/MM/DD`.
- 分类 key 只用 `hint`, `feature`, `fix`, `improvement`, `dependency`; 标签沿用既有固定翻译 (简体中文 `提示`, `新增`, `修复`, `优化`, `依赖`; 英文 `Hint`, `Feature`, `Fix`, `Improvement`, `Dependency`; 其他语言见现有 JSON).
- `feature` 条目不以 `新增` 开头, `fix` 条目不以 `修复` 开头; `dependency` 只记录 Gradle 依赖变化, 使用 `附加`, `升级`, `移除` 等固定动作词.
- 与 AutoJs6 GitHub Issue 有关时按既有格式写明 Issue 引用.

## 14. 数据存储, 独立界面与发行历史 (CONDITIONAL, 路线图 P1.3 / P4)

- 进度与书签 (D13) 存于插件私有目录 `files/books/<指纹>/`, 键为内容指纹, 不落盘明文路径; 原子写用纯 JVM 的 `store/AtomicFiles` (临时文件 + fsync + 重命名, 不用 `android.util.AtomicFile`, 便于 JUnit 覆盖); 临时键迁移到正式指纹后在 `files/books/aliases/<临时键>` 记录别名, 打开时先经 `BookDataStore.resolveKey` 解析; 每本书书签上限 500 (`bookmarks.json`, `store/BookmarkCodec` 信封 `format` / `bookmarks[]`, 整文件原子写, 空表删文件, 迁移时两边并集), 书目上限 500 (LRU, 淘汰时清理悬空别名). 用户导入的字体是唯一的其它落盘内容: `files/fonts/<sha256>.<ttf|otf>` + `files/fonts/index.json` (`store/FontStore`, 信封 `format` / `fonts[]`, 单个 20 MiB, 最多 10 个, 经 `fonts/FontFileValidator` 校验 SFNT 签名与 `name` 表后才落盘, 同哈希去重, CSS 族名经 `FontFamilyNames.resolve` 避开保留名与重名并固化在条目中); 字体经 `book/FontsContainer` 以 `https://readium_package/fonts/<file>` 服务给导航器, 不复制到 assets, 不经 `servedAssets`; 导入 / 删除后重建导航器.
- 高亮与笔记 (D4 / P9, `annotations/` 包) 存于 Room 数据库 `annotations.db` (单表 `annotations`: `bookKey` 指纹, `href`, locator JSON, `style` highlight / underline, `color` 不透明 ARGB, `note`, `quote`, `chapter`, 时间; schema 导出到 `app/schemas/`, 版本变更 MUST 附迁移与 schema 文件); 规则集中在 Android-free 的 `AnnotationPolicy` (每书 2000 条, 笔记 4000 / 引文 1000 / 章节 400 字符, 同一 href + locator 视为同一处: 再次高亮只改样式与颜色, 阅读顺序排序, 迁移并集) 与 `AnnotationStore` (事务, 临时键到正式指纹的迁移与 LRU 淘汰书目的 `retainOnly`, 与 `BookDataStore` 的书目一致); `AnnotationJson` 是契约 JSON (`book.annotations()` / `highlight` 事件, P9.4) 的唯一来源, 字段名与 `EpubContract.FIELD_*` 由 `AnnotationJsonTest` 钉住. 宿主侧 (P9.4, 契约版本 2): `EpubBookBinder.getAnnotations` 按书籍的完整指纹 (加尚未迁移的快速别名, 同处以完整键为准) 读 `AnnotationStore` 并按阅读顺序分页; 视图模型在会话认领后以 `observeAll` 过滤该书历经的全部键 (`announcedBookKeys`, 指纹迁移不算变化) 向 `ReaderSession.updateAnnotations` 投递列表, 首个列表为基线, 之后按行 id 差分成 `highlight` 事件. 阅读器 (P9.2): 选择工具栏的 `高亮` 用上次颜色 / 样式直接落库, `添加笔记` 打开 `reader/AnnotationDialog` (样式, 五色调色板, 笔记; 选择在 `reader_settings` 的 `annotation_color` / `annotation_style` 记住); 全部高亮作为一个 decoration 组 `annotations` 经 `applyDecorations` 渲染 (id `annotations-<rowId>`, 与搜索 / 朗读组共用 `decorationMutex`), 点按 decoration 打开编辑器; `reader/AnnotationSheet` 面板按章节以阅读顺序分组 (`AnnotationListing`), 支持跳转 / 编辑 / 删除 / 全部清除. 导出 (P9.3): `AnnotationMarkdown` (Android-free) 渲染语言中立的 Markdown (书名标题, 作者行, 每章一节, 引文为块引用, 笔记为其后段落, 时间为斜体行; 行首的 Markdown 标记转义), 文件名为净化后的书名 + `.md`; `reader/AnnotationExport` 构造 `ACTION_SEND` 分享 (纯文本 + 书名主题) 与写入用户经 `CreateDocument("text/markdown")` 选定的文档 (`wt` 截断); 不申请存储权限, 不落盘到公共目录 (D19).
- 全局阅读偏好 (D14) 存于 `files/reader-preferences.json` (`store/ReaderPreferencesStore`, 信封 `format` / `themeMode` / `preferences`, `preferences` 为 Readium `EpubPreferencesSerializer` 的 JSON); 读取经 `prefs/PreferencesCodec` 白名单 + 钳制 + 枚举校验, 损坏回退默认; 文件只存 `themeMode`, `theme` 在提交导航器时由 `ThemeMapping.resolve(themeMode, hostDarkMode)` 派生; 旧 `reader_settings.scroll_mode` 只在文件不存在时迁移一次; 写入 400 ms 去抖, `onPause` / `onCleared` 冲刷.
- 设置页与 Launcher 入口 (P4) SHOULD 跟随宿主的语言, 夜间模式和主题色 (`HostAppearanceActivity`), 宿主配置不可用时安全回退; 最近书籍只保存用户经系统文档选择器明确授予的持久 URI.
- 最近书籍 (P4.1, D4): `files/recent-books.json` (`launcher/RecentBooksCodec` 信封 `format` / `books[]`: 持久 URI 字符串, 显示名, 加入 / 最后阅读时间, 指纹, 书名, 作者, 封面文件名, 总进度, 可用标志; `launcher/RecentBooksPolicy` 最新在前, 同 URI 合并, 上限 100 (LRU), 算术 Android-free), 封面缩略图 `files/covers/<URI 的 SHA-256 前 32 位>.webp` (最长边 512, `launcher/CoverExtractor` 取 Readium `coverFitting`); 只有启动器在 `takePersistableUriPermission` 成功后写入条目 (`RecentBooksStore.upsert`), 阅读器只更新已存在的条目 (`update`: 指纹 / 元数据 / 封面 / 进度, 打开失败标记不可用), 移除条目时 `releasePersistableUriPermission`; 整文件原子写, 空表删文件, 所有方法持进程级锁.
- 设置页 (P4.3, `settings/SettingsActivity`, 启动器菜单与阅读器溢出菜单进入) 新增独立的应用外观设置 (`app_settings` / `app_theme`); 阅读分组仍只编辑阅读器下次启动时读回的东西: `reader-preferences.json` 的 `themeMode` (其余 Readium 偏好仍由阅读器内的面板负责, D14) 与 `恢复阅读偏好` (删文件), `reader_settings` 的开关 (点击翻页区域, 音量键, 外部链接, 朗读常亮 / 后台) 与新键 `read_aloud_sleep_timer` (开始朗读时自动布防的定时器, 默认关), `tts-preferences.json` 的语速 / 音调; 阅读器 `onStart` 经 `EpubReaderViewModel.reloadPreferences` / `TtsController.reloadPreferences` 重读这两个文件 (有未冲刷的本地编辑时跳过). 数据区在确认后清空: 进度与书签 (`BookDataStore.clearAll`), 高亮与笔记 (`AnnotationDao.deleteEverything`, 摘要经 IO 协程异步填充), 最近书籍与封面 (`RecentBooksStore.clear` 并逐条 `releasePersistableUriPermission`), 导入字体 (`FontStore.clear`), 偏好与设置 (三个文件与 `reader_settings` 全部清空). 关于区显示版本 / 构建号 / 日期与 Readium 版本 (`BuildConfig.READIUM_VERSION`), 以 `ACTION_VIEW` 打开作者, LICENSE, THIRD_PARTY_NOTICES 与源码页 (无浏览器时提示).
- 设置页 MUST 提供独立的 `发行历史` 入口 (`settings/ReleaseHistoryActivity`, 经 `ReleaseHistory.kt` 按当前 locale 读取 `doc/CHANGELOG-{LANGUAGE_TAG}.md`, 找不到时回退英语, 由 Android-free 的 `settings/MarkdownLite` 解析 (标题 / 项目符号 / 段落 / 分隔线; 行内代码 / 粗体 / 链接) 并经 `MarkdownRenderer` 渲染为 Spanned, 只有 `https` 链接可点击); 更新检查仅手动 (D28).
- 更新检查 (P4.3, D28, `update/`): `AppUpdateRepository` 只对 `https://api.github.com/repos/<仓库>/releases/latest` 发一次 GET (10 s 超时, 不跟随重定向, 256 KB 上限, 404 视为尚无发行, 取消经 `disconnect()`); `ReleaseInfoCodec` 只接受 `https://github.com/` 的发布页, 丢弃草稿, 说明截断 4000 字; `AppUpdateCoordinator` 以 `UpdateSchedulePolicy` 限制每天至多一次网络 (24 h 内重放缓存的答案, 时钟回拨容忍), 进度对话框可取消, 有新版本 (`AppVersionPolicy` 语义版本比较, 支持 `v` 前缀与预发布后缀) 时对话框提供打开发布页 / 内置发行历史 / 忽略此版本 (再次检查时改为取消忽略), 已是最新 / 尚无发行 / 失败均为 toast; 状态存 `reader_settings` (`last_update_check_at`, `cached_release`, `ignored_update_version`). 自动检查保持关闭 (`AUTOMATIC_CHECKS_ENABLED = false`), 不下载 APK. 测试经 `AppUpdateCoordinator.sourceOverride` 注入 `UpdateSource`.
- 所有界面覆盖无障碍标签, RTL, 大字体, 夜间模式与进程恢复.

## 15. 测试要求

### 15.1 JVM 单元测试 (`app/src/test`)

- `EpubReaderExplorerCompatibilityTest`: 审计数值 (v2 / 5269 / 5282 / v22), 检查点单调, 宿主版本分类, 身份常量.
- `EpubRequestPolicyTest` (取代 `EpubReaderIntentPolicyTest`): 三条入口交叉 (Explorer 信封不得走外部查看器, `ACTION_VIEW` 不得进阅读器, 伪装 Explorer 动作仍按信封规则要前缀授权与两项 ClipData), 启动器动作需显式且无 ClipData, 外部入口的 scheme / 授权 / query / 空叶段 / 目录 URI / 目录 MIME, 显示名来源 (发送方 / 叶段) 与格式门; `isSupportedEpub` 的扩展名 / MIME / 冲突容器规则, `sanitizeDisplayName`.
- `book/ByteRangesTest`, `book/BookFingerprintTest`: 范围裁剪边界, 指纹的确定性 / 覆盖范围 / 与文件名无关.
- `ReleaseHistoryTest`: locale 到 changelog 资产的映射与回退.
- `PluginRuntimeInfoTest`: 两条动作规格 (ID / 位置 / 优先级 / 目标 / 访问 / MIME / 扩展名), 共享标签与 Activity.
- `store/ProgressCodecTest`, `store/BookDataStoreTest`, `store/ProgressThrottleTest`: 进度 JSON 往返与损坏输入, 原子写 / 迁移合并 / LRU / 别名 / 非法键, 节流与冲刷.
- `book/TocFlattenerTest`, `reader/PageTurnPolicyTest`, `reader/ReaderProgressTest`: 目录扁平化上限与当前章节匹配, 点按区 (关闭 / 左右 / 上下, RTL 镜像) 与音量键 / 键盘键 (`KeyboardEvent.code` 与 keycode), 进度快照.
- `prefs/PreferencesCodecTest`, `prefs/ThemeMappingTest`, `prefs/ReaderThemeColorsTest`, `prefs/PreferenceRangesTest`, `store/ReaderPreferencesStoreTest`: 偏好信封往返 / 白名单 / 钳制 / 损坏输入, 主题模式映射, 对比度与系统栏亮度, 步进吸附, 文件读写与清除.
- `fonts/FontFileValidatorTest`, `fonts/FontCatalogCodecTest`, `store/FontStoreTest`: SFNT 签名 / 截断 / `name` 表优先级与清洗, 目录信封往返与损坏条目, 导入 (哈希命名, 去重, 限长, 上限, 族名冲突) / 删除 / 缺文件剔除.
- `annotations/AnnotationPolicyTest`, `AnnotationJsonTest`, `AnnotationListingTest`, `AnnotationMarkdownTest` (P9): 归一化 (href / 引文 / 时间), 同处判定, 阅读顺序与最新优先排序, 迁移并集与上限, 预览截断; 契约 JSON 字段; 面板的章节分组; Markdown 文档结构, 转义与文件名净化.
- `store/BookmarkCodecTest`, `reader/BookmarkPolicyTest` (+ `store/BookDataStoreTest` 的书签用例): `bookmarks.json` 往返 / 损坏条目 / 上限 / 并集, 当前页判定 (分页页号, 滚动 position, 固定版式资源), 定位器合成, 片段; 整文件写与空表删文件, 迁移并集.
- `reader/LinkPolicyTest`, `reader/ImageDecodingTest`: 外链分类 (web / 系统 / 拒绝) 与链接历史栈; 图片降采样倍率.
- `launcher/RecentBooksCodecTest`, `launcher/RecentBooksLimitTest`: 信封往返与可选字段, 损坏 / 未知格式为 null, 无 URI / 无名条目丢弃, 越界进度忽略; 排序, 同 URI 合并保留旧字段, 上限淘汰最旧, 重读前移, 百分比.
- `update/AppVersionPolicyTest`, `update/UpdateSchedulePolicyTest`, `update/ReleaseInfoTest`, `settings/MarkdownLiteTest`: 语义版本解析 / 比较 (前缀 `v`, 预发布, 构建元数据) 与忽略判定; 每日一次的手动抓取节流与时钟回拨, 自动检查开关与计费网络; GitHub 响应解码 (草稿拒绝, 非 github.com 页面拒绝, 说明截断) 与缓存往返; Markdown 子集解析与 `plainText`.
- `tts/TtsVoicePolicyTest`, `tts/SystemTtsEngineTest`, `tts/SleepTimerPolicyTest`: 语言标签规范化与匹配, 出版物语言默认, 语音排序 (精确区域, 离线优先, 质量, 稳定 id 序); 默认引擎不可用时的显式引擎选择; 定时器时长 / 剩余时间 (向上取整) / 到期 / 本章结束判定 / 键往返.
- `book/HtmlBlockExtractorTest`, `book/TextExtractorTest`, `service/LimitsTest` (P5.2): XHTML 块遍历 (标题 / 引用 / 脚注 / 列表 / 代码 / 图片与 figcaption / 表格行, 跳过 script 与隐藏元素, 实体解码, 文档字符集, 相对 src 解析); 纯文本 / Markdown 渲染 (标题级别钳制, 多行引用, 图片只进 Markdown, 空块跳过), 列表项与代码块的两种渲染, 分块 (7 字符窗口拼回全文, 末尾与越界, 代理对不切, 非法参数); href / 文本请求 / 搜索请求 / 阅读顺序索引 / 并发与字节上限的错误码, 错误详情按 UTF-8 字节截断. `PluginRuntimeInfoTest` 另断言 `EPUB_FEATURES` 的内容 (含 `reader-session` 与 `annotations`, 不含 `tts`). `service/LimitsTest` 另覆盖 `getAnnotations` 分页请求 (默认页 100, 上限 200, 负数与越界的错误码); `service/ContractVersionsTest` (P9.4): 基线 1 / 最新 2, 缺失或更低协商为 1, 更高钳制为 2, 仅版本 2 含高亮.
- `service/HostSessionPolicyTest`, `service/ReaderPreferencesJsonTest`, `service/SessionTargetTest` (P5.3): 令牌格式 / 唯一性 / 显式组件与动作校验 / 常量时间比较; 偏好子集逐键类型与范围, null 重置, 未知键收集, 尺寸上限, 补丁合并, 高级键集合; 起始位置的三种形状与错误码.
- 后续阶段按路线图补充: 阅读器会话的令牌与事件序列 (P5.3).

### 15.2 Android instrumentation (`app/src/androidTest`)

- `PluginContractInstrumentationTest` MUST 覆盖: 两个服务的显式绑定, `getInfo()` 身份与能力, `resValue` 身份与 Kotlin 常量一致, 目录形状与键集合, Manifest 导出 / 权限 / intent-filter (发现, INFO, 执行, Wake), 权限集合精确; P5.2 起: EPUB 服务导出 / 权限 / 只由 action + category 解析, 包内服务集合精确 (五个: 三个导出的插件门, 不导出的 `tts.TtsForegroundService`, 以及 P9.1 起 Room 运行时清单声明的不导出 `androidx.room.MultiInstanceInvalidationService`), `threeFolioEpubPluginInfo()` 与 Explorer 身份除 `engine` / `capabilities` 外逐字段一致, 能力键集合精确 (五键; P9.4 起 `epubContractVersion` = 1, `epubMaxContractVersion` = 2).
- `service/PluginServiceInstrumentationTest` (P5.2): 经 `ServiceTestRule` 绑定 EPUB 服务并用 remote-only Binder 包装强制走生成的 Proxy / Parcel 路径 (书籍 Binder 同样包装); 身份与能力; `minimal-epub3.epub` 往返 (元数据含位置数, 目录深度, 阅读顺序, 按索引 / href / 带片段的 href 取文本一致, 7 字符分页拼回全文, 越界 offset 为空, Markdown 标题与 `![]()` 图片, 资源管道返回 XHTML 与 PNG 签名, 搜索命中与 offset / limit 分页一致, 关闭幂等且之后为 `SESSION_CLOSED`, `/proc/self/fd` 计数不增); 错误码 (非 ZIP, LCP 加密, 空描述符, 管道描述符, 超大 options, 未知 href / 索引, 越界 offset / maxChars / limit / 查询长度, 未知格式, 空查询, 空 / 过短 href, `openReader` 无回调为 INVALID_ARGUMENT); 第 9 本被拒, 关一本后可再开, 解绑后全部 `SESSION_CLOSED`; P9.4: 不带版本键打开的书籍所有应答标 1 且 `getAnnotations` 为 `INVALID_ARGUMENT`, 以版本 2 打开的书籍空页 / 直接写入 Room (完整指纹) 后按阅读顺序列出 / offset 回显与 `hasMore` 分页 / 越界 limit 为 `LIMIT_EXCEEDED` / 关闭后 `SESSION_CLOSED`; 证据 `files/p2-evidence/service-*-api<N>.txt`.
- `service/ReaderSessionInstrumentationTest` (P5.3): `openReader` 经 remote-only Binder, 认领前 `getState` 只有令牌与 `visible=false`, 未知偏好键的 `error` 事件, 认领前 `navigate` 为 `READER_NOT_VISIBLE`; 以令牌显式启动 `EpubReaderActivity` 后 `open` 事件 (visible, 书名, 起始 href, 位置数) 与 `getState` (href / index / title) 一致, `openReader` 偏好已生效; `navigate` 跳章与 `goTo` (href / 进度 / locator) 各自产生对应 `progress`; `setPreferences` 生效 (字号, 主题, 行高关闭出版商样式) 且未知键上报; 书签增删各一事件且 `getBookmarks` 一致; `close(finish)` 产生 `close(host)` 并结束 Activity, 之后 `SESSION_CLOSED`; 全程同一 generation 且 seq 严格递增. 生命周期: 新会话替换旧会话 (`replaced`), 错误 / 已替换令牌只显示无效请求面板, 不带 finish 的 `close` 保留阅读器, 缩短的认领超时产生 `timeout`. 拒绝: 空回调, 坏 locator, 未知 href, 越界进度, 坏偏好, 非 ZIP, 认领后的非法方向 / 空目标 / 外部 locator / 非法主题 / 缺失偏好, 描述符计数不增. P9.4 (`highlightsReachAVersionTwoHostOnly`): 认领前已存在的高亮是基线 (无事件), 视图模型添加 / 编辑 / 删除各产生一条 `highlight` 事件 (`added` / `updated` / `removed`, 携带契约 JSON), 不带版本键打开的会话所有事件与应答标 1 且从不收到 `highlight`; 证据 `files/p2-evidence/reader-session-*-api<N>.txt`.
- `EpubReaderIntentPolicyInstrumentationTest`: 完整 v2 信封被接受, 协议 / 身份 / 宿主版本 / 来源 / 授权 / ClipData / 父子关系 / 格式门的每条拒绝路径.
- `book/PfdResourceInstrumentationTest`: 描述符资源的长度与定位读 (含并发), Readium 经描述符打开 EPUB 2 / EPUB 3 样本 (标题, 阅读顺序, 目录, 章节内容), 损坏样本以错误结束而不崩溃.
- `EpubReaderUiInstrumentationTest`, `EpubReaderProgressInstrumentationTest`: 经 debug `EpubReaderTestContentProvider` 用完整 v2 信封启动阅读器; 翻页, 目录跳转, 重建恢复, 错误态, 进度落盘与重开恢复, 别名, 音量键与滚动模式, 就绪前跳转重放; 证据写入 `files/p0-spike/` 与 `files/p1-evidence/`, 用 `adb exec-out run-as <包名> cat` 拉取.
- `EpubReaderPreferencesInstrumentationTest`: 偏好到达导航器 (`EpubSettings` 与 Readium CSS `--USER__fontSize`), chrome 配色, 落盘与重启恢复, 面板控件, 旧 `scroll_mode` 迁移; 证据写入 `files/p2-evidence/`.
- `EpubReaderFontsInstrumentationTest`: 导入后 WebView `document.fonts` 中的 FontFace 已加载且正文字体族跟随, 重启后保留, 删除后偏好回退; 重复 / 非字体 / TTC / 缺失文档不改目录; 面板列出并可选择导入字体; 证据写入 `files/p2-evidence/fonts-api<N>.txt`.
- `EpubReaderDirectionInstrumentationTest`: 日文 / 繁体中文竖排样本的 `verticalText` / `scroll` / `readingProgression` 与页面 `writing-mode`, 强制横排 / 自动切换, 目录跳转与回退; 阿拉伯语样本 RTL 与 `direction: rtl`; 界面方向跟随宿主语言 (无宿主或 API < 33 时测试用 per-app locale 胜出) 且与正文无关; 证据写入 `files/p2-evidence/direction-*.txt` 与截图 `direction-*.png` (归档到 `docs/images/evidence/`).
- `EpubReaderFixedLayoutInstrumentationTest`: 固定版式样本的 `第 x / N 页` 标签, 竖屏单页 / 横屏自动双页 / 偏好强制, 面板隐藏文字偏好并提供双页组, 菜单无滚动模式, 经 `Activity.dispatchTouchEvent` 分发的双指缩放与拖动 (经反射读 `R2FXLLayout`), 截图用 `PixelCopy` 复制窗口; 证据写入 `files/p2-evidence/fxl-*.txt` 与截图 `fxl-*.png`.
- `EpubReaderSearchInstrumentationTest`: 搜索命中数与从夹具 XHTML 数出的期望一致, 章节头, 打开结果后的 `currentLocator.href` / 搜索条文案 / 页面 decoration 计数 (`[data-group="search"] > div`), 上一处 / 关闭, 过短与空结果提示, 2000 页样本的 50 一批与 500 截断与取消, 固定版式跳页; 证据写入 `files/p2-evidence/search-*.txt` 与截图 `search-*.png`.
- `EpubReaderExternalSamplesTest`: 只在 runner 参数 `external=true` 且 `cache/epub-reader-test-documents/` 里有约定文件名的本地真实书籍时运行 (否则 `Assume` 跳过, 门禁不带此参数), 记录打开 / 位置 / 搜索 / 跳转耗时与计数到 `files/p2-evidence/external-*.txt`; 书籍只经 `adb push` + `run-as cp` 放到设备, 永不入库.
- `annotations/AnnotationStoreInstrumentationTest` (P9.1, 内存 Room): 添加 / 同处改样式 / 2000 上限, 更新保留键与 locator, 删除与清空, 迁移 (整体移动与并集) 与 `retainOnly`, Flow 跟随变更. `EpubReaderAnnotationsInstrumentationTest` (P9.2): 以搜索命中为选区添加高亮 (章节名, 引文, 颜色), 页面 decoration 数 (`[data-group="annotations"]`), 同处再高亮只改色并记住颜色, 编辑器新建带笔记的下划线并记住样式 / 颜色, 面板按章节分组 / 跳转 / 编辑 (点按 decoration 打开) / 删除, 重开保留, 全部清除后 decoration 归零; 设置页计数与清空; 证据写入 `files/p9-evidence/annotations-*`. `EpubReaderAnnotationExportInstrumentationTest` (P9.3): 导出文本的标题 / 作者 / 章节顺序 / 引文 / 笔记 / 时间行, 分享 intent 的类型 / 文本 / 主题, 写入所选文档 (截断旧内容) 与失败时不留痕, 面板导出按钮随列表启用, 空书只提示.
- `EpubReaderBookmarksInstrumentationTest`: 工具栏切换添加 / 移除 (章节名, 片段, 菜单标题), 面板时间倒序 / 跳转 / 删除 / 全部清除, `bookmarks.json` 落盘与重开保留 (恢复页显示填充图标), 预写 500 条后的上限拒绝, 固定版式按资源; 证据写入 `files/p2-evidence/bookmarks-*.txt` 与截图 `bookmarks-*.png`.
- `EpubReaderControlsInstrumentationTest`: 点按区三种设置 (经 `Activity.dispatchTouchEvent`, 点在页面下部空白处以避开链接), 键盘键在无焦点与 WebView 有焦点时都翻页 (`sendKeyDownUpSync`), 选中文本的复制 / 分享 / 网页搜索 / 文本处理 intent; 证据写入 `files/p2-evidence/controls-*.txt`.
- `EpubReaderLinksInstrumentationTest`: 页内 JS 点击书内链接后的跳转与返回栈 (`onBackPressedDispatcher`), `noteref` 注释对话框, 外链策略经阻塞 `ActivityMonitor` 计数 (确认 / 直开 / mailto / tel / 拒绝); 证据写入 `files/p2-evidence/links-*.txt` 与截图 `links-note-*.png`.
- `EpubReaderImagesInstrumentationTest`: 可重排页点击 `img` 打开查看器 (href / 说明文字 / 关闭后页面不动), 固定版式不打开; 证据写入 `files/p2-evidence/images-*.txt` 与截图 `images-viewer-*.png`.
- `EpubReaderTtsInstrumentationTest`: 系统引擎从当前页朗读英文夹具 (API 33+ 先授予 `POST_NOTIFICATIONS`), 当前句 decoration (`[data-group="tts"] > div`, 从 instrumentation 线程轮询, 一句可占多行因此计数 >= 1) 并自动前进, 暂停 / 播放, 上一句 / 下一句, 前台服务与通知 2001 存在, 停止后全部释放且语速偏好落盘; 逐句跳到第 2 章时页面跟随, 手动跳转停止朗读; 熄屏 3 分钟每分钟句子都在前进 (`KEYCODE_SLEEP` 只在 `KeyguardManager.isDeviceSecure` 为假时发出, 安全锁屏设备改为 `KEYCODE_HOME` 置于后台并记录模式, 运行参数 `screenOff=force` 强制熄屏后需手动解锁); 音频焦点被抢占 (短暂抢占 Readium 适配器继续朗读, 永久抢占暂停且不自动恢复, 播放从暂停句继续); 通知动作 (暂停 / 播放 / 下一句 / 上一句 / 停止, 经 `Notification.Action.actionIntent.send()`); 关闭阅读器停止服务. 无引擎或无语音数据的设备记录后 `Assume` 跳过; 证据写入 `files/p2-evidence/tts-*.txt`.
- `EpubReaderExternalViewInstrumentationTest`: 经 `UiAutomation.executeShellCommand` 以 shell 身份 `am start -a android.intent.action.VIEW -d content://... -t application/epub+zip --grant-read-uri-permission -n <包名>/.ExternalViewerActivity` 扮演其它应用 (debug 的 `EpubReaderTestContentProvider` 为此导出, 只读, 并回答 `_display_name` / `_size`): 打开且不入列, 同进程 provider 的授权无法持久时 `addToRecent` 拒绝; 无授权 / `file://` / 指向查看器的 Explorer 信封均显示无效请求, 指向阅读器的 `ACTION_VIEW` 被系统以 PLUGIN 权限拒绝 (`am` 的 stderr 经 `sh -c eval ... 2>&1` 合并进 UiAutomation 的输出); shell 能对 `com.android.externalstorage.documents` 授予持久授权的设备 (AVD) 上, 夹具复制到 Download 后 `加入最近书籍` 使条目入列, 授权持久, 启动器可重开, 其它设备 `Assume` 跳过并记录; 证据 `files/p2-evidence/view-api<N>.txt`, `view-rules-api<N>.txt`, `view-persistable-api<N>.txt`. `ActivityMonitor` 每次 `am start` 单独注册并跳过已在结束中的实例 (`HostAppearanceActivity` 首次同步外观会 `recreate` 一次).
- `EpubReaderSettingsInstrumentationTest`: 开关行点击后 `ReaderSettings` 翻转, 点击区域 / 默认睡眠定时 / 主题模式 / 语速 / 音调落到阅读器读取的文件; `发行历史` 行打开 `ReleaseHistoryActivity` 并渲染内置 changelog; 数据行清空进度 / 最近书籍 / 字体 / 偏好; 以 `sourceOverride` 注入的假 `UpdateSource` 走完更新检查 (新版本对话框, 忽略与取消忽略, 一天内复用缓存不再抓取, 失败不改状态, 旧版本视为已最新); 证据 `files/p2-evidence/settings-*-api<N>.txt`.
- `EpubReaderLauncherInstrumentationTest`: 预置的条目出现在网格并经 `ACTION_OPEN_RECENT` 打开阅读器 (`Instrumentation.ActivityMonitor` 捕获), 阅读后条目补齐指纹 / 书名 / 作者 / 进度 (夹具无封面时记录 `cover=absent`), 重建启动器后磁贴刷新; 移除释放授权并显示空状态; 不可读文件打开失败后标记不可用并提供移除; 同进程测试 provider 无法持久授权时 `onDocumentPicked` 打开但不入列; 证据 `files/p2-evidence/launcher-api<N>.txt` 与 `launcher-grid-api<N>.png`. 系统文档选择器本身不驱动, 只调用其结果处理函数.
- `EpubReaderReadAloudBackgroundInstrumentationTest`: 睡眠定时器 (缩短为 4 s 的固定时长与本章结束) 停止朗读并报告 `SleepTimerEnded`, 屏幕常亮开关只在朗读期间持有窗口标志, `后台继续朗读` 开启时关闭阅读器后服务 / 通知 / 会话继续且句子前进, 再次打开同一本书领回会话并从朗读句打开, `ACTION_RESUME_READ_ALOUD` 重开阅读器显示托管的书籍 (同一 `Publication` 实例), 无会话时关闭; 开关关闭时关闭阅读器即停止; 证据写入 `files/p2-evidence/tts-{sleep-timer,keep-screen-on,background,notification-resume}-api<N>.txt`.
- `book/BookFingerprintInstrumentationTest`: 大样本 (200 MiB, 空间不足时 64 MiB) 经描述符的临时键与全量哈希耗时.
- 有设备或模拟器时执行 `:app:connectedDebugAndroidTest` (至少 API 28 与 API 35 各一次; 2026-09-19 起的矩阵为 API 28 / 33 / 35 / 37); 性能度量与正确性测试分开.

### 15.3 设备冒烟

- 阅读器界面改动后 SHOULD 在 API 24 模拟器与一台 API 33+ 真机上用 `docs/fixtures/minimal-epub3.epub` 走一次: 从宿主文件管理器打开, 翻页, 目录跳转, 旋转屏幕, 返回; 在 ColorOS 等会保持新装应用停止状态的设备上 SHOULD 做真实激活验收; 未执行时在路线图如实记录.

### 15.4 样本

- `docs/fixtures/` 由 `.python/generate_fixtures.py` 生成 (英文基线 + 日文竖排 / 繁体中文竖排 / 阿拉伯语 RTL 的 `Locale` 变体 + 六版固定版式图画书), `SHA256SUMS.txt` 与 `README.md` 同步; 外部样本 (IDPF / W3C) 加入前 MUST 确认许可证并在 README 记录来源与 SHA-256; 真实书籍永不入库; 需要真实书籍度量时按 `EpubReaderExternalSamplesTest` 的约定文件名推到设备缓存目录并用 runner 参数 `external=true` 单独运行.

## 16. CI 基线

- `build.yml`: push, pull request 与手动触发; `contents: read`; JDK 21 Temurin; 运行 AAR 门禁, JVM 测试, lint, debug / androidTest APK 与原生对齐检查, 上传产物; 在 API 24 (x86) 与 API 35 (x86_64) 模拟器上执行 instrumentation 测试.
- `markdown.yml`: Windows 环境运行 `.python\check_markdown.bat`, 阻止生成文档漂移.
- CI action 使用固定大版本并定期更新; timeout 与真实构建时长匹配.
- CI 的设备测试通过 `-Preadium.test.isolate=true` 启用 Android Test Orchestrator, 每个用例使用独立进程并清除被测应用数据, 避免阅读进度, 后台朗读与异步持久化影响后续用例. 本地常规运行默认保留原有方式; 使用隔离模式时只能连接测试设备, 不用于保留个人书籍及设置的设备.

## 17. 验证顺序

按变更范围执行最小但充分的验证:

```powershell
py .python/generate_markdown.py --check
py -3 -m unittest discover -s .python/tests
.\gradlew.bat :app:verifyExplorerActionApiCompatibility
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleRelease             # 引入或升级运行时依赖后 MUST 执行: R8 缺失类只会在这里以构建失败暴露
.\gradlew.bat :app:connectedDebugAndroidTest   # 有设备或模拟器时
```

Release 前额外执行 `.\gradlew.bat :app:appendDigestToReleasedFiles`, 检查 `app/releases/` 中只出现预期的已签名单 APK 且 CRC32 与文件内容一致. 任何未执行的验证都在最终说明中明确列出原因; 构建耗时较长时给予足够时间, 不用过短 timeout 误判失败.

## 18. 许可证, 安全, 隐私与第三方内容

- 根目录 `LICENSE` 为 Mozilla Public License 2.0, README 徽章与源码头保持一致; Readium 为 BSD-3-Clause, 记入 `THIRD_PARTY_NOTICES.md`.
- `android:allowBackup="false"` 保持不变.
- 不记录书籍内容, 文件路径或用户书签到普通日志; 诊断只保留格式, 耗时, 大小与错误分类.
- 联网行为限定为书籍自身引用的远程资源 (含明文 HTTP, D31) 与手动更新检查; README 安全章节如实说明.
- 第三方代码与 AAR 必须记录来源, 版本, 校验值与许可证 (`THIRD_PARTY_NOTICES.md`); 引入或升级依赖时同一提交更新该文件.

## 19. 完成检查清单

- [ ] 第 2 节身份值在 Gradle, Manifest, Kotlin 常量, 资源, 文档与测试中一致; `PluginContractInstrumentationTest` 通过.
- [ ] 平台插件只在根 settings 应用一次, 无 `mavenLocal()`, 无外部路径引用, 无 `gradle/data`.
- [ ] `libs/` AAR 与 `locks/host-api-aars.lock`, `gradle/explorer-action-compatibility.properties` 哈希匹配, `THIRD_PARTY_NOTICES.md` 已更新.
- [ ] `sign.properties`, `app/sm003.jks`, `local.properties` 被 Git 忽略; `appendDigestToReleasedFiles` 可用.
- [ ] Wake Activity, INFO 服务, Explorer Action 服务契约完整; `getInfo()` 显式 `supportedAbis = emptyArray()`.
- [ ] 10 语言资源与文档完整, ASCII 标点, `plugin_description` 无句尾点号; 图标与样本由脚本生成.
- [ ] JSON 文案源已生成产物且 `--check` 通过; 当前版本全部语言 changelog 已更新.
- [ ] 单元测试, assemble, lint, release 通过; 有设备时 instrumentation 通过, 否则明确记录.
- [ ] `ROADMAP.md` 已勾选完成条目并写入证据; `VERSION_BUILD` 与提交数一致; `git status --short` 无输出.

## 20. 参考项目路由

只读取完成当前任务所需的参考, 不复制项目专属内容:

- Explorer Action v2 骨架, 兼容审计, 宿主外观跟随, 发行历史对话框, 文档生成器: `D:/idea-projects/AutoJs6-Plugin-HTML-Previewer`, `D:/idea-projects/AutoJs6-Plugin-Markdown-Previewer`
- 宿主 AAR 哈希锁定, `THIRD_PARTY_NOTICES.md`, 裁剪版 AGENTS, 图标生成脚本: `D:/idea-projects/AutoJs6-Plugin-Angus-Mail`, `D:/idea-projects/AutoJs6-Plugin-MCP-Server`
- 独立设置页, 跟随宿主主题与内置发行历史: `D:/idea-projects/AutoJs6-Plugin-Three-Stone-AI`
- 进度记忆与前台播放服务形态: `D:/idea-projects/AutoJs6-Plugin-3-Ember-Player`
- Readium 上游 (3.4.0 tag 的 test-app 是导航器接入的权威示例): <https://github.com/readium/kotlin-toolkit>
- 宿主入口, 插件发现, 安装和启用引导, 文档预览器模板: `D:/idea-projects/AutoJs6`

参考时以这些仓库的当前代码为准, 不以历史 README 或旧 release 中已经淘汰的写法为准.


## Standalone settings unification (2026-09-29)

- Follow the workspace `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`. Keep the shared row order language, night mode, theme color, launcher icon; default appearance follows AutoJs6. Unavailable hosts fall back to system locale/night and the real host default color `#FFDEAD`.
- Use flat grouped rows with 16 sp titles, 14 sp summaries, min 72 dp height, 24 dp side padding, a 24 dp outline icon in a 40 dp slot, and text-aligned subtle dividers. Dialog/navigation rows end in a chevron; switch rows contain only a Material 3 switch and toggle once when the row is clicked.
- Choice dialogs are centered with 24 dp corners, at least 24 dp horizontal inset, max 560 dp width and bounded scrolling content above a fixed text-button footer. Language, night, theme and launcher drafts save only on OK; Cancel, Back and owner destruction discard the draft.
- `ThemeColorChooser` / `ThemeColorValue` provide the common 16 presets, one HEX/RGB input, local swatch/button preview and independent error semantics. Preserve old saved preset/custom colors even when absent from the new preset list. Surfaces, primary/secondary text, outlines and dividers use the shared neutral tokens; theme colors affect controls and emphasis, not page backgrounds.
- Launcher AUTO is the Manifest default. `LauncherIconStatePolicy` resolves explicit PM states first; normalize via the existing Activity and a non-exported MY_PACKAGE_REPLACED receiver. An ordinary upgrade preserves explicit choices and repairs duplicate/missing entries without adding a second preference source. Existing connected devices are switched to AUTO only under the user's separate instruction.
- Validate custom-color control tint, dialog scrolling, Cancel/OK semantics, API 24/modern launcher-state migration and device rendering. Keep about/developer rounded containers with transparent inner Three glyphs. Never infer release or device evidence from compilation alone.

- Reading content colors remain a separate semantic exception: application locale/night/theme preferences must not rewrite `reader-preferences.json`, and HOST reading mode resolves the actual host/system night mode, independently of the app night selection.

## Optical icon standard (2026-10-03)

- Follow `../AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` for every standalone plugin, including the Plugin Center. `.python/icon_geometry.py` v1 is a self-contained copy of the common geometry algorithm; keep its implementation identical across the standalone plugins. Never read sibling checkouts during a build.
- Derive size from the equal-weight combination of visible bounding-box area (alpha >= 16) and alpha-weighted ink area. Target visible size is 0.52 of the canvas, with only documented optical corrections in 0.94-1.06. The adaptive ratio is always the UI ratio multiplied by 72/108. This supersedes older hardcoded UI/adaptive widths in historical notes. Preserve aspect ratio, optical placement and final nonzero-alpha safety checks.
- Current derived widths: UI 0.6562, adaptive 0.4375 (rounded documentation values, not generation constants). Readium uses optical scale=0.94 and zero offsets.
- Generate `mipmap/ic_plugin_center.png` and its night counterpart from the same geometry as the transparent UI/launcher mode. They are transparent neutral artwork for installed and catalog entries, independent of the active launcher alias. Keep them through `raw/keep_plugin_center_icon.xml`. Existing separate brand assets retain their original purpose.
- Black, white and neutral grayscale are allowed for every plugin without per-plugin approval. Pure silhouettes default to #272727 / #D8D8D8; shaded artwork may preserve meaningful tonal details with R=G=B and matching day/night alpha. Stamp Mail is one example, not an exception. Keep light-theme artwork dark enough and dark-theme artwork light enough to remain legible. Do not introduce a filled background into the Plugin Center assets.
- Run the icon generator and its read-only `--check`, `.python/tests/test_icon_geometry.py`, existing icon regressions, and review the full set at 36/48/64 px in both themes and in launcher masks. `.github/workflows/icons.yml` verifies Windows/Linux reproducibility. Synthetic previews do not replace actual launcher verification.

## 3-Folio EPUB identity and publication (2026-10-03)

The maintainer explicitly selected `io.github.supermonster003.autojs6.plugin.three.folio.epub`, version 2.0.0, repository `AutoJs6-Plugin-Three-Folio-EPUB`, and display name `3-Folio EPUB`. This is a new Android installation identity. Preserve the old Readium app and its data; settings, recent books and annotations are not automatically migrated. Host build 5318 or later recognizes the official identity. Keep the EPUB engine, Binder descriptors, protocol versions and Readium dependency names unchanged.

All existing book icon PNGs and the code-native open-book artwork are preserved byte-for-byte. Folio retains the existing `ic_launcher_transparent` / `ic_plugin_center` resource convention and the old application-brand resource names; the rename does not redesign the book. The shared icon standard must allow this established naming.

Appearance contexts MUST override only locale, layout direction and night mode using a delta `Configuration()`. Copying the entire current configuration freezes orientation and window metrics on API 35. The existing accessibility/landscape and fixed-layout automatic-spread device tests cover this behavior; keep their rotation assertions. On 2026-10-03 the old configuration reproduced the landscape failure on API 35, while the delta fix passed those tests, all three reader preference tests and four settings appearance tests. A CI-only legacy-scroll timeout gets state diagnostics, not a weaker assertion or an unconditional retry.

GitHub repository rename, push, version 2.0.0 publication and official-index replacement are authorized for this task. The AutoJs6 host stays local. README and plugin instructions include a compact acknowledgement paragraph; the initial references are detailed in THIRD_PARTY_NOTICES.md and RIGHTS_AND_TAKEDOWN.md. Do not invent a closed-source design reference absent from the original project notes. Preserve historical changelog and roadmap evidence.
