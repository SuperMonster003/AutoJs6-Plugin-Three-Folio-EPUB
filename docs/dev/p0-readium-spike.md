# P0.2 Readium 可行性 spike 记录

日期: 2026-09-19. 目的: 为路线图 D2 (Readium Kotlin Toolkit 3.4.0 完整栈) 与 D11 (只读描述符传书, 不复制) 提供证据, 并决定二者是否固定. 全部命令在 Windows 11 / JDK 21 (`E:/.java/jdk-21.0.1`, 以 Temurin 属性模拟 CI) / Gradle 9.5.0 / AGP 9.3.2 / Kotlin 2.3.20 (平台版本插件 1.8.2) 下执行.

## 1. 依赖解析

`./gradlew :app:dependencies --configuration releaseRuntimeClasspath` (完整输出见构建目录 `build/deps-release-runtime.txt`, 不入库) 的关键结论:

| 项目 | 结论 |
|---|---|
| Kotlin stdlib | 消费方 2.3.20 请求, 被 Readium 传递依赖提升为 `kotlin-stdlib:2.4.20` (同时 `kotlin-reflect 1.8.22 -> 2.4.20`); Kotlin 2.3.20 编译器读取 2.4.x 元数据 (N+1) 无错误, 与 MCP Server 插件消费 2.4.0 SDK 的经验一致 |
| `androidx.databinding:viewbinding` | Readium 请求 9.3.1, 被平台 AGP 9.3.2 对齐为 9.3.2, 无冲突 |
| `com.mcxiaoke.koi:core` | 0.5.5, 来自 Maven Central (Readium `readium-shared` 声明), 无原生库 |
| AndroidX 提升 | `constraintlayout 2.2.1 -> 2.2.2`, `lifecycle 2.9.4 -> 2.11.0` 由 Readium 传递依赖决定; 已把 `gradle/libs.versions.toml` 改为与解析结果一致 |
| Readium 传递依赖 | `jsoup 1.23.2`, `kotlinx-serialization-json 1.11.0`, `kotlinx-datetime 0.8.0`, `kotlinx-coroutines 1.11.0`, `timber 5.0.1`, `media3 1.11.0` (common / common-ktx / session / exoplayer / container / database / datasource / decoder / extractor), `guava 33.3.1-android`; 全部为纯 JVM 库 |
| 原生库 | `verifyNativePageAlignment` 的 `Debug.json` / `Release.json` 均报告 `entries: []`, `summary.ok = true` (零原生库, 与 `expectNoNativeLibraries` 一致) |

首次 Temurin 验收构建 (`:app:assembleDebug :app:testDebugUnitTest`) BUILD SUCCESSFUL 2m 54s, 日志只出现一段 `Version information for IDE platform and Gradle plugins`; 18/18 JVM 用例通过. 无 Kotlin 元数据版本错误, 无需 `OVERRIDDEN_*`.

## 2. Release 构建与体积基线

`:app:assembleRelease` (R8 8.13.19, `isMinifyEnabled` + `isShrinkResources`) 通过, `missing_rules.txt` 未生成 (没有缺失类). `app/proguard-rules.pro` 只保留插件包, `PluginInfo`, Explorer Action API 三条 keep 与一条 `-dontwarn kotlinx.parcelize.Parcelize`; Readium / media3 / kotlinx-serialization 依赖各自 AAR 自带的 consumer 规则 (`configuration.txt` 909 行, 其中 media3-exoplayer 的解码器 keep 规则全部指向不存在的可选扩展类, 不产生代码).

| 制品 | 体积 (字节) | 说明 |
|---|---|---|
| `autojs6-plugin-readium-epub-reader-v1.0.0.apk` (debug) | 15,463,211 | 未混淆 |
| `autojs6-plugin-readium-epub-reader-v1.0.0.apk` (release) | 2,749,948 | 体积基线 (路线图 P7 预算: 不超过基线 + 20%, 即 3.3 MB) |
| `app-debug-androidTest.apk` | 606,960 | 测试包 |

Release APK 内容 (587 个条目, 解压后 5,276,106 字节): `classes.dex` 2,944,576 + `classes2.dex` 191,036; `assets/readium/` 包含 Readium CSS 三套 (默认 / cjk-horizontal / cjk-vertical), 字体 (AccessibleDfA, OpenDyslexic, iA Writer Duospace), 以及 DiViNa 播放器 `divinaPlayer.js` (424,586 字节, 本插件不使用 DiViNa, P7 体积条目可评估用 packaging 排除).

R8 mapping 中存活类数: Readium 612, jsoup 199, media3 1 (ExoPlayer 与 session 几乎完全被移除, 因为当前代码尚未引用 `TtsNavigator`; P3 引入朗读后体积会上升, 届时重新记录).

## 3. 随机访问 (D11): `PfdResource`

- 实现: `book/PfdResource.kt` 以 `ParcelFileDescriptor.AutoCloseInputStream(descriptor).channel` 做 `FileChannel` 定位读 (`read(buffer, position)`), `length()` 取 `statSize` (为 -1 时回退 `channel.size()`), `sourceUrl = null` 使 Readium 走 `StreamingZipArchiveProvider` 而不是文件路径.
- 打开链路: `AssetRetriever(contentResolver, DefaultHttpClient).retrieve(resource, FormatHints(EPUB))` -> `Format.conformsTo(Specification.Epub)` -> `PublicationOpener(DefaultPublicationParser(pdfFactory = null), contentProtections = [FallbackContentProtection()])`. LCP / ADEPT 标记的书被 fallback 保护开成 restricted, `BookOpener` 检查 `publication.isRestricted` 后关闭并返回 `BookOpenError.Protected`.
- 证据 (`book/PfdResourceInstrumentationTest`): 长度与定位读精确 (含越界截断与 32 路并发读), `mimetype` 条目在偏移 38..57 处可读; `minimal-epub2.epub` / `minimal-epub3.epub` 经描述符打开, 标题 / 3 个阅读顺序项 / 目录 / 第一章正文均正确; positions / search / content 三个服务在描述符之上可用 (`positions()` 3 个资源, `search("reef")` 命中第一章, `content().text()` 覆盖三章); 四个损坏样本 (非 zip, 缺 `container.xml`, 缺 OPF, LCP 标记) 都以错误结束而不崩溃.
- 是否有整文件读取: 测试断言每次打开只经 `ContentResolver` 打开一次描述符 (`EpubReaderTestContentProvider.openCount == 1`), 且 `PfdResource` 不提供 `sourceUrl`, Readium 没有路径可复制; 打开耗时见第 4 节.

## 4. 最小阅读 (设备证据)

`EpubReaderUiInstrumentationTest` 用完整的 Explorer Action v2 信封 (调试构建专用 `EpubReaderTestContentProvider` 提供 `content://` URI) 启动 `EpubReaderActivity`, 依次验证: 打开到第一章 locator, `goForward()` 翻到第二章, 目录对话框跳到第三章, `recreate()` 后 locator 仍在第三章且描述符没有被重新打开, 损坏文件显示错误面板而不崩溃. 截图与计时写入应用私有目录 `files/p0-spike/` (以 `run-as` 取回, 不入库).

| 设备 | API | 结果 | 打开到首个 locator (ms) |
|---|---|---|---|
| AVD `AVD_API_24` (x86, Chrome WebView 69) | 24 | 17/17 通过 (打开, 翻页, 目录跳转, 重建恢复, 损坏文件错误面板); 截图上方有一块黑色区域, 对应 logcat 中 goldfish `GLESv2_enc ... GL error 0x500`, 属模拟器 WebView 合成伪影, 文本与图片本身已渲染 | 753 |
| Xiaomi 23046RP50C (`968e9f18`) | 35 | 17/17 通过, 截图干净 (Readium CSS 默认排版, 16x16 图片与脚注链接可见) | 577 |

## 5. 阅读器要点 (部分推迟)

| 要点 | 状态 |
|---|---|
| `EpubNavigatorFragment` 渲染, 翻页, `go(Link)` 目录跳转, `currentLocator` 收集 | 已验证 (第 4 节) |
| Activity 重建后 `Locator` 恢复 | 已验证: `EpubReaderViewModel` 保存 `Publication` / `EpubNavigatorFactory` / 最近 `Locator`, 重建时以 `initialLocator` 重新创建导航器 |
| `SearchService`, `PositionsService`, `ContentService` | 已验证 (第 3 节), 为 P2 搜索与 D30 `epub.text()` 提供基础 |
| `EpubPreferences` / `submitPreferences` (主题, 字号, 滚动, 竖排) | 未做, 推迟到 P2.1 (API 面已在路线图记录, 不影响 D2 决策) |
| `fontFamilyDeclarations` 注入外部字体 | 未做, 推迟到 P2.4 |
| `TapEvent.targetElement` 用于图片 | 未做, 推迟到 P2 |
| `TtsNavigator` 真机朗读 | 未做, 推迟到 P3 (依赖已解析, R8 已验证可收敛) |
| WebView `shouldInterceptRequest` 拦截远程请求 | 未做, 附录 F 安全模式不排期, 需要时再验证 |

## 6. 决策

- D2 固定: 四个 Readium 制品在平台版本插件 1.8.2 下解析, 编译, R8 收敛, release 体积 2.7 MB, 零原生库, API 24 与 API 35 均能渲染并翻页. 附录 E.2 退路不触发.
- D11 固定: 描述符资源经 `AssetRetriever` / `StreamingZipArchiveProvider` 直接解析, 不需要 Readium 的 content URI 资源工厂, 也没有任何整本复制.
- 附带决定: media3-exoplayer 注入的 `ACCESS_NETWORK_STATE` / `WAKE_LOCK` 用 `tools:node="remove"` 移除, 权限集合保持 INTERNET + PLUGIN (D19); P3 若需要 `WAKE_LOCK` 再显式声明.
