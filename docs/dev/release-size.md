# Release APK 体积 (路线图 P7.5)

日期: 2026-09-21. 目的: 记录 release universal APK 的体积构成, 本阶段的削减项与其证据, 并设定后续预算. 构建: `:app:assembleRelease :app:verifySignedReleaseArtifacts` (R8, `isMinifyEnabled` + `isShrinkResources`, Windows 11 / JDK 21 Temurin 属性 / AGP 平台版本). 体积单位为字节, 取 APK 文件大小 (压缩后).

## 1. 历史

| 时点 | 体积 | 说明 |
|---|---|---|
| P0.2 基线 | 2,749,948 | 未含朗读 (media3 几乎被 R8 全部移除); 预算建议 "基线 + 20%" = 3,299,938 |
| P3 后 | 3,721,374 | 朗读引入 media3-session / TtsNavigator |
| P4 后 | 3,841,934 | 启动器, 设置页, 最近列表 |
| P5 后 | 3,922,786 | EPUB 能力服务, 会话, AIDL |
| P7.5 (本文) | 3,328,220 | DiViNa 资源剔除, 插件包整体 keep 移除, 资源表按 10 种语言过滤 |

## 2. 本阶段削减项

| 项 | 做法 | 效果 |
|---|---|---|
| DiViNa 播放器 (`assets/readium/divina/`, 427,450 B 未压缩, 3 个文件) | `androidResources.ignoreAssetsPatterns` 加入 `<dir>divina` (AGP 默认模式一并写明, 因为非空列表会取代默认) | 合并资源不再含该目录; 插件只渲染 EPUB, Readium 的 DiViNa 导航器未被引用 |
| 插件包整体 keep (`-keep class io.github...reader.** { *; }`) | 删除. 组件由 manifest 规则保留, fragment / view model 由 androidx consumer 规则保留构造器, Parcelable / enum 由默认 optimize 文件保留; 宿主只按包名与契约包 (`org.autojs.plugin.*.api`, 仍 keep) 与插件交互, 从不按插件类名查找 (`ExplorerDocumentPreviewerPluginUi` 只含包名字符串) | mapping 中插件类 576 -> 328 (其余被内联 / 合并), `classes.dex` 4,703,156 -> 4,132,540 B (未压缩) |
| 资源表语言过滤 | `androidResources.localeFilters` = `ar, en, es, fr, ja, ko, ru, zh, zh-rCN, zh-rHK, zh-rTW` (插件 `locales_config.xml` 的 10 种 + 库使用的 zh-rCN 拼写) | `resources.arsc` (必须未压缩存放) 826,920 -> 515,036 B |

保留项 (评估后不动): Readium 的三套字体 (`AccessibleDfA.otf` 145,384 / `iAWriterDuospace-Regular.ttf` 81,636 / `OpenDyslexic-Regular.otf` 41,088, 均在偏好面板的字体家族列表中), ReadiumCSS 与注入脚本 (渲染所需), `assets/doc/CHANGELOG-*.md` (发布历史页面, 14 个文件约 120 KB 未压缩, 压缩后约 45 KB), `res/` 536,863 B 未压缩 / 305,239 B 压缩 (576 项, 已经 `isShrinkResources`).

## 3. 构成 (P7.5 之后)

| 条目 | 未压缩 | 压缩后 | 说明 |
|---|---|---|---|
| `classes.dex` + `classes2.dex` | 4,325,724 | 2,086,927 | R8 后; 插件类 328, Readium / jsoup / media3-session / AndroidX 其余 |
| `resources.arsc` | 515,036 | 515,036 (stored) | 11 种语言配置 + 密度 / 版本限定符 |
| `res/` | 536,863 | 305,239 | 576 项 (drawable, layout, 图标) |
| `assets/readium/` | 638,473 | 约 290,000 | ReadiumCSS, 注入脚本, 三套字体; DiViNa 已剔除 |
| `assets/doc/` | 约 120,000 | 约 45,000 | 发布历史 (14 份 changelog markdown) |
| 其余 (`META-INF`, `AndroidManifest.xml`, `kotlin/`, 签名) | - | 约 85,000 | |
| APK 合计 (705 项) | 6,288,781 | 3,328,220 | |


## 4. 验证

- `verifySignedReleaseArtifacts`: 签名验证通过 (apksigner).
- `verifyNativePageAlignment`: `app/build/reports/native-alignment/Release.json` (pageSize 16384, strictAbis arm64-v8a / x86_64): release APK `entries: []`, `summary.ok = true`; 插件仍是零原生库 (D7).
- 冒烟 (release APK 覆盖安装到 AVD API 37 `emulator-5556`, 16 KB 页, 宿主 debug 已安装): 覆盖安装 `Success` (versionCode 52, minSdk 24, targetSdk 37); 宿主脚本样本 s1 - s3 (`build/p63_sample_run.py`, 打开 / 元数据 / 位置 / 搜索 / 章节导出) 控制台错误行 0, s2 导出目录含章节文本与封面; 启动器, 设置页, 发布历史页三个界面 `am start` 后均 resumed; logcat `FATAL EXCEPTION` 0. 之后重新装回 debug 包 (同一签名配置, 覆盖安装不需要卸载).
- 16 KB 设备: `emulator-5556` (`getconf PAGE_SIZE` = 16384) 上完成上述冒烟; 接入的 Xiaomi Pad 6 (API 35) 页大小为 4096, 不构成 16 KB 验证. 插件零原生库, 16 KB 对齐只涉及 APK 内的 `lib/` (为空), 见上一条.

## 5. 预算

原建议 "P0.2 基线 + 20%" (3,299,938 B) 在 P3 引入朗读 (media3-session, ~1 MB dex) 后只差 28,282 B (0.9%) 未达; 本阶段的三项削减是当前依赖集合下没有功能代价的全部选项, 剩下的 (发布历史页的 14 份 changelog 副本约 45 KB 压缩后, Readium 字体) 都有功能代价. 新预算: 3,500,000 B (P7.5 体积 + 5% 余量, 取整); 超出时先重看 §2 的保留项与 Readium 升级带来的差异 (D18), 而不是加 `-dontwarn` 或关闭 `isShrinkResources`.

## 6. 1.1.0 (路线图 P9)

P9.1 (2026-09-21) 附加 Room 2.8.1 (`room-runtime` 为运行时依赖, `room-compiler` 经 KSP 只在构建期运行), 按 AGENTS.md 的规则在同一提交记录 release 体积 (同一构建命令, 主树 `build/p9_commit.py verify 0` 的 `:app:assembleRelease`, R8 无缺失类告警):

| 时点 | 体积 | 说明 |
|---|---|---|
| 1.0.0 发布 (`ca3044b`) | 3,340,340 | P8.3 发布门, GitHub Release `v1.0.0` 的资产 |
| P9.1 (Room) | 3,391,961 | +51,621 B (1.5%); `classes.dex` 2,052,337 + `classes2.dex` 80,707 (压缩后), `assets` 302,465 (含 `v1.1.0` 的 changelog 副本), `resources.arsc` 515,036 不变, `lib/` 仍为空 |
| P9.4 (契约版本 2 的 `epub-api.aar`, 含 P9.2 / P9.3 的阅读器高亮, 面板与导出) | 3,443,123 | 较 P9.1 +51,162 B (1.5%); `classes.dex` 2,071,076 + `classes2.dex` 80,705 (压缩后), `assets` 310,219 (v1.1.0 changelog 五条), `resources.arsc` 533,948 (P9.2 的选择工具条, 面板与编辑器布局及字符串), `lib/` 仍为空; 来源 `build/p9_commit.py verify 3` 的 `:app:assembleRelease` (build 72, 日志 `build/gradle-p9-commit3.log`, R8 无缺失类告警) |

P9.1 时预算 3,500,000 B (§5) 的余量为 108,039 B (3.1%); P9.4 时余量收窄到 56,877 B (1.6%), 其中契约版本 2 的 AAR 本身只多几个常量与一个 AIDL 方法, 增量几乎全部来自 P9.2 / P9.3 的阅读器类与资源 (`resources.arsc` +18,912). Room 在 Android 上使用系统 SQLite (`androidx.sqlite` 的 framework 驱动), 不带原生库, §4 与 AGENTS.md 5.4 的零原生库结论不变 (APK 条目列表中无 `lib/`). P9.5 (文档) 不改 APK; P9.6 的发布门再记一次实际体积, 若逼近预算则先考虑 §2 的削减项而不是放宽预算.
