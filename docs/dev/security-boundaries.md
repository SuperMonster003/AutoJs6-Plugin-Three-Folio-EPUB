# 安全边界复核 (路线图 P7.1 / P7.2)

日期: 2026-09-21. 目的: 在 "书是不可信输入, 宿主是可信调用方" 的前提下, 记录插件每一层的边界与证据: 容器与解析层 (P7.1 敌意输入矩阵), WebView 层 (决策 D6 全部允许下的底线), 组件暴露面, 存储与日志. 每条结论都指向代码位置或设备证据; Readium 升级 (D18) 后本文的第 3 节必须重验.

## 1. 威胁模型与前提

- 输入: `.epub` 经三条门进入: 宿主文件管理器 (`EpubReaderActivity`, `PLUGIN` 权限), 其他应用的 `content://` 文档 (`ExternalViewerActivity`, 只收 `content` scheme + `application/epub+zip`), 宿主的 Binder 描述符 (`ReadiumEpubReaderPluginService`, `PLUGIN` 权限 + `CallerGuard` 限定宿主包名). 书的内容一律不可信.
- 调用方: AutoJs6 宿主可信 (权限门 + 包名校验); `ExternalViewerActivity` 与 `LauncherActivity` 是仅有的无权限门, 前者只取一个 URI, 后者不取数据.
- 权限: `INTERNET` (D6 远程资源 + D28 更新检查), `org.autojs.permission.PLUGIN`, 朗读的三条前台服务权限 (AGENTS.md 第 6 节的例外记录); 无存储, 位置, 相机, 通讯录. media3 传递声明的 `ACCESS_NETWORK_STATE` / `WAKE_LOCK` 在 manifest 中 `tools:node="remove"` (D19).
- 不在范围: 宿主自身的边界, LCP / ADEPT 书 (一律拒绝), 对 Readium 内部实现的漏洞挖掘. 本文只回答 "一本恶意的书能让插件做什么".

## 2. 容器与解析层 (P7.1 敌意输入矩阵)

生成器 `.python/generate_fixtures.py` 提供 17 个损坏样本 (`docs/fixtures/malformed-*.epub`), 另有两个在设备上现场生成 (50 000 条目 14,254,116 字节, 1 GiB 零字节单资源压成 1,045,316 字节). 两条测试走完全部样本: `service/HostileInputInstrumentationTest` (Binder 服务) 与 `EpubReaderHostileInputInstrumentationTest` (阅读器 Activity, 以文件管理器的方式打开). 服务侧在 7 台设备 (API 24 / 28 / 33 x2 / 35 / 36 / 37) 全部通过; 阅读器侧在 6 台通过, 小米平板 (API 35) 因锁屏 (`isKeyguardShowing=true`) 无法执行阅读器用例, 记录为未执行.

| 输入 | 服务结果 | 阅读器结果 | 机制 |
|---|---|---|---|
| 非 zip | `NOT_EPUB` | 本地化 "无法打开" 面板 | `AssetRetriever` 嗅探失败 -> `BookOpenError.Retrieve` |
| 空 zip, 缺 `container.xml`, 缺 OPF, OPF 截断 | `PARSE_FAILED` | 同上 | `PublicationOpener` 返回 `Try.failure` -> `BookOpenError.Open` |
| NCX 截断 | `PARSE_FAILED` (P7.1 前为 `INTERNAL`) | 同上 (P7.1 前进程会崩溃) | Readium `XmlParser.parse` 对畸形 XML 抛 `AssertionError` (是 `Error`, Readium 自己只捕获 `Exception`); `BookOpener.open` 现在捕获 `Throwable` (取消除外) 并映射为 `BookOpenError.Malformed` |
| 缺 `mimetype` | 打开 | 打开 | Readium 以 `container.xml` 为准, 与 EPUB 规范的宽容读法一致 |
| 外部实体 (`<!ENTITY canary SYSTEM "file://...">`, `http://127.0.0.1:9/`, 内部实体) | 打开; 标题 `XXE null`, 正文中 `&canary;` 原样保留, 内部实体未展开, 搜索 canary 无命中 | 打开 | `XmlPullParserFactory` (KXml) 不处理 DTD, 不解析外部实体, 不发网络请求; 测试用 UUID 写入 `files/xxe-canary.txt` 并断言它从未出现在标题 / 正文 / 搜索结果 |
| 穿越 href (`../`, `%2e%2e`, `/etc/hosts`, `file:///etc/hosts`, `..\..\`, `OEBPS/../../etc/hosts`, `content://`) | `RESOURCE_NOT_FOUND`; 反斜杠形式 `INVALID_ARGUMENT` | 打开, 阅读顺序中的穿越条目正文为 `RESOURCE_NOT_FOUND` | `StreamingZipContainer` 只按条目名在归档内查找, 从不解压到磁盘; `Url.fromEpubHref` 百分号编码; 插件 `Limits.href` 拒绝控制字符与超长 (2048) |
| 3000 字符文件名 | 打开 | 打开 | 条目名只是归档内的键 |
| 重复条目 | 打开, 提供第一份 | 打开 | `ZipFile.getEntry` 返回首个同名条目 |
| 2003 / 50 000 条目 | 打开 3.7 s (AVD 33) .. 18.2 s (Sony API 28); `readingOrder` 截断 5000 + `hasMore`; positions 5.1 .. 21.3 s; 只命中最后一个资源的搜索 50.0 s 后 `TIMEOUT` | 打开 3.3 .. 13.8 s | `MAX_READING_ORDER_ENTRIES`; `Limits.SEARCH_BUDGET_MS` (50 s, 低于宿主 `CALL_TIMEOUT_MS` 60 s) 用 `withTimeoutOrNull` 包住 Readium 的逐资源扫描, 此前 Binder 线程会忙 10 分钟以上 |
| 1 GiB 零字节资源 | 打开; `openResource` -> `LIMIT_EXCEEDED`, `getText` -> `RESOURCE_NOT_FOUND` (非文本); 堆增长为负, 原生增长 < 1 MiB | (阅读器只打开 `malformed-high-ratio.epub` 小样本) | `Limits.resourceBytes` (`MAX_RESOURCE_BYTES` 64 MiB) 在读取前按条目长度拒绝 |
| LCP (`encryption.xml`), 仅 `license.lcpl`, ADEPT | `ENCRYPTED` | "不支持 DRM 保护的书" | `FallbackContentProtection` 标记 restricted -> `BookOpenError.Protected` |

存储: 每个被拒绝的样本前后比对 `files/` `cache/` `no_backup/` 与外部私有目录的完整文件快照, 无变化 (API 36 首次安装时 ART 写入的 `files/profileInstalled` 与书无关, 测试排除). 服务在整轮之后仍应答 (`engine epub`).

## 3. WebView 层

Readium 为阅读顺序中的每个资源创建一个 `R2BasicWebView` (`R2EpubPageFragment.onCreateView`), 页面从 `https://readium_package/` (书 + `FontsContainer` 提供的导入字体) 与 `https://readium_assets/` (`assets/readium/`) 加载; 二者由 `WebViewServer` 在 `shouldInterceptRequest` 中于进程内应答, 没有本地 HTTP 端口. 插件的 `reader/WebViewBoundary` 通过 `FragmentManager.registerFragmentLifecycleCallbacks(recursive = true)` 在每个 fragment 视图创建时收紧下表的设置; 首个加载在 `onCreateView` 里已经开始, 但这些开关按请求生效, 页面内容能发出任何请求之前它们已就位.

| 设置 | Readium (`R2EpubPageFragment`) | 插件 (`WebViewBoundary`) | 说明 |
|---|---|---|---|
| `javaScriptEnabled` | `true` | 保持 | D6: Readium 的分页, 选择, 装饰, 定位全部依赖注入脚本 |
| `allowFileAccess` | 未设置 (API < 30 默认 `true`) | `false` | 书从不落盘, 页面没有理由读文件系统 |
| `allowContentAccess` | 未设置 (默认 `true`) | `false` | 页面不得经 WebView 读取任何 content provider |
| `allowFileAccessFromFileURLs`, `allowUniversalAccessFromFileURLs` | 未设置 (默认 `false`) | 显式 `false` | 页面不是 `file://` 源, 显式关闭以防默认值变动 |
| `domStorageEnabled` | 未设置 (`false`) | 保持 | |
| `useWideViewPort`, `loadWithOverviewMode`, 缩放, `textZoom` | Readium 设置 | 不改 | 排版所需 |
| `setWebContentsDebuggingEnabled` | `R2BasicWebView.init` 传入 Readium AAR 自己的 `BuildConfig.DEBUG`, 值为 `false` (`javap` 验证) | 不改 | 插件的 debug 包也不开启远程调试 |
| `addJavascriptInterface` | `Android` (`R2BasicWebView` 自身) + `Configuration.javascriptInterfaces` | 插件的 `navigatorConfiguration` 只设置 `selectionActionModeCallback` 与字体声明, 不添加接口 | 页面可见的宿主对象只有 Readium 自己的 |
| 链接 | `shouldOverrideUrlLoading` -> navigator: 包内链接自行导航, 其余交给 `Listener.onExternalLinkActivated` | `reader/LinkPolicy` 只放行 `http` / `https` / `mailto` / `tel`, 其余丢弃 | `file:` / `content:` / `intent:` / `javascript:` 链接不会被 WebView 加载 |
| 明文 | `usesCleartextTraffic="true"` | 保持 (D31) | 书里的 `http://` 远程资源可加载; 更新检查只走 HTTPS |

页内探针 (`EpubReaderWebViewBoundaryInstrumentationTest`, 设备证据 `files/p2-evidence/webview-boundary-api<N>.txt`): 打开 `minimal-epub3.epub` 后遍历 `reader_container` 下所有 WebView, 断言每一个都带边界且 JavaScript 开启; `location.origin` 为 `https://readium_package`; `fetch(location.href)` 得到 `ok 200` (探针本身有效); `fetch('content://<测试提供器>/documents/boundary-probe.txt')` 与 `<img src="content://...">` 均失败, 且测试提供器的 `openCount` 不变 (对照: 测试进程直接打开同一文档计数 +1); `fetch('file:///etc/hosts')` 失败. 结果见第 6 节.

## 4. 组件暴露面 (`AndroidManifest.xml`)

| 组件 | exported | 权限 | 接收的数据 |
|---|---|---|---|
| `WakeActivity` | 是 | `PLUGIN` | 无 (`Theme.NoDisplay`, 立即结束) |
| `launcher.LauncherActivity` | 是 | 无 | 无 (启动器图标) |
| `settings.SettingsActivity`, `settings.ReleaseHistoryActivity` | 否 | - | - |
| `ExternalViewerActivity` | 是 | 无 | `ACTION_VIEW` + `content` scheme + `application/epub+zip`; 请求规则在 `EpubRequestPolicy` (D27) |
| `EpubReaderActivity` | 是 | `PLUGIN` | Explorer Action 请求 (P1) 与宿主会话 (`EPUB_READER_OPEN`, 一次性 token, P5.3) |
| `PluginInfoService`, `ExplorerActionService` | 是 | `PLUGIN` | 宿主协议 |
| `service.ReadiumEpubReaderPluginService` | 是 | `PLUGIN` + `CallerGuard` (宿主包名) | 只读描述符与有界参数 (`service/Limits`) |
| `tts.TtsForegroundService` | 否 | - | 只有阅读器绑定 |
| debug `EpubReaderTestContentProvider` | 仅 debug 包 | - | 测试文档 |

## 5. 存储与日志

- `android:allowBackup="false"`; 书从不复制 (D11, `PfdResource` 直接在描述符上定位读); 被拒绝的书不写盘 (第 2 节).
- `files/` 只保存进度, 书签, 最近列表, 导入字体与设置; 无书正文缓存.
- 日志: 插件源码没有 `Log.*` / Timber 调用; Readium 内部的 Timber 没有被种树, 其输出为空操作 (P7.7 记录).

## 6. 设备证据

| 设备 | API | WebView 提供器 (`WebViewCompat.getCurrentWebViewPackage`) | P7.1 服务 | P7.1 阅读器 | P7.2 探针 |
|---|---|---|---|---|---|
| AVD `emulator-5554` | 24 | com.android.chrome 69.0.3497.100 | 通过 | 通过 | 通过 (2 个页面 WebView, 提供器 0 次) |
| Sony G8441 | 28 | com.android.chrome 126.0.6478.186 | 通过 | 通过 | 通过 (2, 0) |
| Redmi 22120RN86C | 33 | com.google.android.webview 111.0.5563.116 | 通过 | 通过 | 通过 (2, 0) |
| AVD `emulator-5560` | 33 | com.google.android.webview 109.0.5414.123 | 通过 | 通过 | 通过 (2, 0) |
| Xiaomi 23046RP50C | 35 | 130.0.6723.86 (`dumpsys webviewupdate`) | 通过 | 未执行 (锁屏) | 未执行 (锁屏) |
| AVD `emulator-5558` | 36 | com.google.android.webview 134.0.6998.135 | 通过 | 通过 | 通过 (2, 0) |
| AVD `emulator-5556` (16 KB 页) | 37 | com.google.android.webview 149.0.7827.5 | 通过 | 通过 | 通过 (2, 0) |

探针在每台设备上的读数一致: 每个页面 WebView 四个开关均为 `false`, `javaScriptEnabled=true`, `domStorageEnabled=false`, origin `https://readium_package`; `fetch(location.href)` -> `ok 200`; `fetch(content://...)` -> `TypeError: Failed to fetch`, `<img src=content://...>` -> `error`, `fetch(file:///etc/hosts)` -> `TypeError: Failed to fetch`; 测试提供器被页面打开 0 次, 被测试进程直接打开 1 次 (对照).

原始笔记: 设备上的 `files/p2-evidence/hostile-*-api<N>.txt` 与 `webview-boundary-api<N>.txt`; 本机副本 `build/p71-evidence-<serial>/` 与 `build/p72-evidence-<serial>/` (不入库).

## 7. 保留项

- 明文流量 (D31) 与 JavaScript (D6) 是有意保留的; 变更需要新的决策记录.
- `WebSettings` 的地理位置开关默认允许, 但插件没有位置权限, 也没有实现 `onGeolocationPermissionsShowPrompt`, 系统默认拒绝; 未改动.
- 第 3 节的 Readium 行为 (`R2EpubPageFragment` 的设置列表, `BuildConfig.DEBUG`, `WebViewServer` 的两个主机名) 随 Readium 版本变化, 升级时按 D18 重验.
- 小米平板的阅读器侧用例在解锁后补跑 (路线图 P7.3 兼容矩阵一并记录).
