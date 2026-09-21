# 性能基线 (路线图 P7.4)

日期: 2026-09-21. 目的: 记录阅读器与 EPUB 服务在四种规模样本上的打开, 首屏, 位置计算, 指纹, 翻页帧, 搜索与内存读数, 作为后续版本的对照; 与正确性测试分开 (`EpubReaderPerformanceInstrumentationTest`, 不进入兼容矩阵的整包运行). 样本由用例在设备上现场生成 (与 `.python/generate_fixtures.py --perf` 同一形状): 约 1 MB (5 章, 每章一张 197 KB 噪声 PNG), 约 20 MB (100 章), 约 200 MB (1000 章; 缓存目录空余不足 1.2 GiB 时跳过), 5000 章纯文本 (约 4 MB). 读数为单次, 设备未预热, 只用于量级判断.

## 1. 读法

- 首屏: 从 `startActivitySync` 到 `navigatorReady` (Readium 首页已渲染并报出 locator).
- 位置: 到 `positionCount > 0` (`publication.positions()` 在后台完成); 指纹: 到 `bookKey` 变为整本文件的 SHA-256 (后台哈希, 200 MB 的书要读完整本).
- 翻页: 20 次 `goForward(animated = true)`, 每次间隔 600 ms, 用 `Window.OnFrameMetricsAvailableListener` 记录窗口每帧 `TOTAL_DURATION`; 报告帧数, 超过 16.7 ms 的帧占比与 p50 / p90 / p99.
- PSS 峰值: 独立线程每 250 ms 采一次 `Debug.getPss()`, 取整个阅读器会话 (打开到关闭) 的最大值.
- 冷 / 热: 同一进程内第一次打开该书 / 紧接着的第二次 (插件不缓存书的内容; 差别来自 Readium 与 WebView 的进程内状态).
- 服务侧: `openBook` 耗时, `metadata` (含位置数) 耗时, 只命中最后一章的 `search` 耗时 (`Limits.SEARCH_BUDGET_MS` 50 s 内答复或 `TIMEOUT`).
- 朗读 30 分钟: `-e tts 30` 时在有引擎的真机上朗读 5000 章样本, 每分钟记录 PSS, 断言第 5 分钟之后的增长小于 128 MB.

## 2. 结果

### Sony G8441 (API 28, 真机, 2017 年机型)

API 28 Sony G8441; free cache 8227 MiB; 样本生成 perf-1mb.epub: 990992 B / 150 ms; perf-20mb.epub: 19797037 B / 826 ms; perf-200mb.epub: 197963218 B / 6718 ms; perf-5000ch.epub: 4131538 B / 2178 ms; 用例总时长 273 s.

阅读器 (`EpubReaderActivity`, 冷 / 热各一次):

| book | run | first screen | positions | fingerprint | 20 page turns (frames, jank, p50 / p90 / p99) | PSS peak |
|---|---|---|---|---|---|---|
| 1 MB | cold | 2463 ms | 2463 ms | 2463 ms | 373 frames, 7% > 16.7 ms, 9.4 / 14.4 / 26.0 ms | 155 MB |
| 1 MB | warm | 1016 ms | 1016 ms | 1016 ms | 375 frames, 7% > 16.7 ms, 9.7 / 14.9 / 26.3 ms | 166 MB |
| 20 MB | cold | 1053 ms | 1053 ms | 1053 ms | 373 frames, 6% > 16.7 ms, 10.0 / 14.9 / 21.5 ms | 166 MB |
| 20 MB | warm | 1063 ms | 1063 ms | 1063 ms | 375 frames, 4% > 16.7 ms, 9.9 / 13.8 / 20.8 ms | 169 MB |
| 200 MB | cold | 1970 ms | 1978 ms | 1978 ms | 376 frames, 7% > 16.7 ms, 10.2 / 15.3 / 24.8 ms | 191 MB |
| 200 MB | warm | 1836 ms | 1837 ms | 1837 ms | 375 frames, 5% > 16.7 ms, 10.2 / 14.4 / 25.4 ms | 209 MB |
| 5000 chapters | cold | 12395 ms | 12406 ms | 12406 ms | 375 frames, 6% > 16.7 ms, 10.5 / 15.4 / 23.7 ms | 296 MB |
| 5000 chapters | warm | 12271 ms | 12271 ms | 12272 ms | 374 frames, 6% > 16.7 ms, 10.7 / 15.2 / 23.7 ms | 256 MB |

服务侧 (`IEpubBook` Binder):

| book | Binder open | metadata + positions | search (last chapter) |
|---|---|---|---|
| 1 MB | 77 ms | 7 ms (5 positions) | 339 ms (1 hits) |
| 20 MB | 188 ms | 17 ms (100 positions) | 3987 ms (1 hits) |
| 200 MB | 1067 ms | 121 ms (1000 positions) | 50044 ms (TIMEOUT) |
| 5000 chapters | 20071 ms | 613 ms (5000 positions) | 50021 ms (TIMEOUT) |

### AVD API 37 (x86_64, 16 KB 页, 软件渲染)

API 37 Google sdk_gphone16k_x86_64; free cache 3835 MiB; 样本生成 perf-1mb.epub: 990992 B / 124 ms; perf-20mb.epub: 19797037 B / 304 ms; perf-200mb.epub: 197963218 B / 4177 ms; perf-5000ch.epub: 4131538 B / 1671 ms; 用例总时长 260 s.

阅读器 (`EpubReaderActivity`, 冷 / 热各一次):

| book | run | first screen | positions | fingerprint | 20 page turns (frames, jank, p50 / p90 / p99) | PSS peak |
|---|---|---|---|---|---|---|
| 1 MB | cold | 2074 ms | 2074 ms | 2074 ms | 342 frames, 97% > 16.7 ms, 18.6 / 31.5 / 48.1 ms | 195 MB |
| 1 MB | warm | 1045 ms | 1045 ms | 1045 ms | 0 frames, 0% > 16.7 ms, - / - / - ms | 211 MB |
| 20 MB | cold | 1134 ms | 1135 ms | 1135 ms | 356 frames, 98% > 16.7 ms, 18.6 / 32.8 / 52.2 ms | 210 MB |
| 20 MB | warm | 1243 ms | 1243 ms | 1243 ms | 358 frames, 98% > 16.7 ms, 18.5 / 34.0 / 50.4 ms | 216 MB |
| 200 MB | cold | 2179 ms | 2184 ms | 2184 ms | 354 frames, 97% > 16.7 ms, 18.7 / 34.2 / 48.4 ms | 232 MB |
| 200 MB | warm | 1974 ms | 1974 ms | 1975 ms | 358 frames, 98% > 16.7 ms, 18.5 / 33.0 / 50.5 ms | 226 MB |
| 5000 chapters | cold | 22245 ms | 22260 ms | 22260 ms | 354 frames, 97% > 16.7 ms, 18.6 / 30.5 / 36.4 ms | 258 MB |
| 5000 chapters | warm | 29950 ms | 29950 ms | 29950 ms | 305 frames, 98% > 16.7 ms, 34.6 / 69.7 / 222.3 ms | 272 MB |

服务侧 (`IEpubBook` Binder):

| book | Binder open | metadata + positions | search (last chapter) |
|---|---|---|---|
| 1 MB | 63 ms | 3 ms (5 positions) | 77 ms (1 hits) |
| 20 MB | 46 ms | 46 ms (100 positions) | 1205 ms (1 hits) |
| 200 MB | 308 ms | 457 ms (1000 positions) | 19815 ms (1 hits) |
| 5000 chapters | 15735 ms | 2101 ms (5000 positions) | 50040 ms (TIMEOUT) |

### Redmi 12C (API 33, 真机)

API 33 Xiaomi 22120RN86C; free cache 32244 MiB; 样本生成 perf-1mb.epub: 1316387 B / 195 ms; perf-20mb.epub: 26304937 B / 2461 ms; perf-200mb.epub: 263042218 B / 23827 ms; perf-5000ch.epub: 4131540 B / 2805 ms (噪声图随机, 该机生成的图片书更大: 200 MB 样本 263 MB); 用例总时长 338 s.

阅读器 (`EpubReaderActivity`, 冷 / 热各一次):

| book | run | first screen | positions | fingerprint | 20 page turns (frames, jank, p50 / p90 / p99) | PSS peak |
|---|---|---|---|---|---|---|
| 1 MB | cold | 2657 ms | 2658 ms | 2659 ms | 283 frames, 19% > 16.7 ms, 14.8 / 22.1 / 43.6 ms | 205 MB |
| 1 MB | warm | 1162 ms | 1162 ms | 1163 ms | 0 frames, 0% > 16.7 ms, - / - / - ms | 190 MB |
| 20 MB | cold | 1435 ms | 1435 ms | 1436 ms | 347 frames, 15% > 16.7 ms, 14.2 / 21.5 / 57.4 ms | 214 MB |
| 20 MB | warm | 1364 ms | 1365 ms | 1365 ms | 354 frames, 14% > 16.7 ms, 13.1 / 29.3 / 45.1 ms | 213 MB |
| 200 MB | cold | 2508 ms | 2509 ms | 2509 ms | 352 frames, 17% > 16.7 ms, 13.7 / 30.4 / 45.6 ms | 220 MB |
| 200 MB | warm | 2359 ms | 2359 ms | 2360 ms | 353 frames, 18% > 16.7 ms, 14.4 / 19.5 / 64.6 ms | 237 MB |
| 5000 chapters | cold | 32378 ms | 32389 ms | 32389 ms | 347 frames, 22% > 16.7 ms, 15.0 / 27.1 / 44.6 ms | 263 MB |
| 5000 chapters | warm | 32967 ms | 32968 ms | 32969 ms | 351 frames, 14% > 16.7 ms, 13.2 / 30.5 / 44.6 ms | 262 MB |

服务侧 (`IEpubBook` Binder):

| book | Binder open | metadata + positions | search (last chapter) |
|---|---|---|---|
| 1 MB | 160 ms | 14 ms (5 positions) | 292 ms (1 hits) |
| 20 MB | 249 ms | 63 ms (100 positions) | 4774 ms (1 hits) |
| 200 MB | 1149 ms | 445 ms (1000 positions) | 50025 ms (TIMEOUT) |
| 5000 chapters | 17176 ms | 2223 ms (5000 positions) | 50038 ms (TIMEOUT) |

### 朗读 30 分钟 (Sony G8441)

`-e tts 30`, 5000 章样本, 系统引擎, 速度默认; 每分钟一次 `Debug.getPss()`:

| 分钟 | PSS | 位置 |
|---|---|---|
| 1 | 175 MB | OEBPS/c/1.xhtml @ 0.2 |
| 2 | 183 MB | OEBPS/c/1.xhtml @ 0.6 |
| 3 | 194 MB | OEBPS/c/1.xhtml @ 0.8 |
| 4 | 184 MB | OEBPS/c/2.xhtml @ 0.2 |
| 5 | 192 MB | OEBPS/c/2.xhtml @ 0.4 |
| 6 | 179 MB | OEBPS/c/2.xhtml @ 0.8 |
| 7 | 194 MB | OEBPS/c/3.xhtml @ 0.16666666666666666 |
| 8 | 183 MB | OEBPS/c/3.xhtml @ 0.5 |
| 9 | 190 MB | OEBPS/c/3.xhtml @ 0.6666666666666666 |
| 10 | 185 MB | OEBPS/c/4.xhtml @ 0.0 |
| 11 | 198 MB | OEBPS/c/4.xhtml @ 0.3333333333333333 |
| 12 | 181 MB | OEBPS/c/4.xhtml @ 0.6666666666666666 |
| 13 | 195 MB | OEBPS/c/5.xhtml @ 0.0 |
| 14 | 186 MB | OEBPS/c/5.xhtml @ 0.2857142857142857 |
| 15 | 194 MB | OEBPS/c/5.xhtml @ 0.5714285714285714 |
| 16 | 184 MB | OEBPS/c/5.xhtml @ 0.7142857142857143 |
| 17 | 177 MB | OEBPS/c/6.xhtml @ 0.16666666666666666 |
| 18 | 186 MB | OEBPS/c/6.xhtml @ 0.5 |
| 19 | 200 MB | OEBPS/c/6.xhtml @ 0.8333333333333334 |
| 20 | 192 MB | OEBPS/c/7.xhtml @ 0.16666666666666666 |
| 21 | 177 MB | OEBPS/c/7.xhtml @ 0.5 |
| 22 | 191 MB | OEBPS/c/7.xhtml @ 0.6666666666666666 |
| 23 | 183 MB | OEBPS/c/8.xhtml @ 0.14285714285714285 |
| 24 | 197 MB | OEBPS/c/8.xhtml @ 0.2857142857142857 |
| 25 | 187 MB | OEBPS/c/8.xhtml @ 0.5714285714285714 |
| 26 | 179 MB | OEBPS/c/9.xhtml @ 0.0 |
| 27 | 193 MB | OEBPS/c/9.xhtml @ 0.2857142857142857 |
| 28 | 179 MB | OEBPS/c/9.xhtml @ 0.5714285714285714 |
| 29 | 195 MB | OEBPS/c/10.xhtml @ 0.0 |
| 30 | 184 MB | OEBPS/c/10.xhtml @ 0.2857142857142857 |

growth after minute 5: 7 MB (断言 < 128 MB); 30 分钟内状态一直 PLAYING, 朗读从第 1 章推进到第 10 章.

## 3. 结论与阈值

- 打开与首屏: 1 MB / 20 MB / 200 MB 样本在两台真机上冷开 1.1 - 2.7 s (热开 1.0 - 2.4 s), 与文件体积无关 (200 MB 的书并不比 20 MB 慢多少: Readium 按需读取 zip 条目, 插件不预读); 阈值: 200 MB 以内的书首屏 <= 3 s (真机).
- 章节数才是开销来源: 5000 章纯文本样本 (4 MB) 首屏 12.4 s (Sony) / 32 s (Redmi) / 22 - 30 s (AVD), Binder 打开 20.1 s / 17.2 s / 15.7 s, 而 1000 章图片书只需 1 - 2 s; 2003 章的敌意样本 (P7.1) 打开 3.4 s. 打开时间随章节数超线性增长, 来自 Readium 的 EPUB 解析 (插件的 `BookOpener` 没有按章节的循环). 记录为 P8 前的观察项: 5000 章是极端样本 (常见书籍在 20 - 300 章), 不在 1.0.0 处理; 若要处理, 应在 Readium 升级 (D18) 时复测而不是在插件侧绕过.
- 位置与指纹: 两者在首屏之前已经完成 (三列时间相同), 即位置计算与整本 SHA-256 没有把首屏拖后: 200 MB 的书指纹在 2 s 内 (与打开并行, IO 受限).
- 翻页: Sony 20 次动画翻页约 375 帧, 超过 16.7 ms 的帧 4% - 7%, p50 约 10 ms, p90 约 15 ms, p99 21 - 26 ms (偶发的章节切换帧); Redmi 12C (入门机, MIUI) 14% - 22%, p50 13 - 15 ms, p90 20 - 30 ms, p99 45 - 65 ms; AVD 软件渲染下 97% 的帧超过 16.7 ms (p50 18.6 ms), 只能用于回归对比, 不代表真机. 阈值 (真机): jank <= 25%, p90 <= 35 ms, 以入门机为准.
- 内存 (PSS 峰值, 含 WebView 进程内部分): 真机 155 - 237 MB (1 MB - 200 MB 样本), 5000 章 256 - 296 MB; AVD 195 - 272 MB. 峰值随章节数而不是文件体积增长 (Readium 为每个 spine 项保留 `Link` 与位置). 阈值: 常规样本 <= 300 MB.
- 服务侧: Binder `openBook` 与首屏同源 (见第二条); `metadata` 含位置数 7 - 613 ms (真机); 只命中最后一章的搜索: 1 MB 0.3 s, 20 MB 4.0 - 4.8 s, 200 MB (1000 章) 与 5000 章在两台真机上都在 `Limits.SEARCH_BUDGET_MS` (50 s) 内未完成而以 `TIMEOUT` 答复 (AVD 的 200 MB 用 19.8 s 完成). 搜索是线性扫描全部章节 (Readium 的 `StringSearchService`), 1000 章以上的书超预算是预期行为, 调用方拿到 `TIMEOUT` 而不是无限等待 (P7.1).
- 朗读 30 分钟: Sony 上 5000 章样本连续朗读 30 分钟, PSS 在 175 - 200 MB 之间波动 (GC 周期), 第 5 分钟之后的增长 7 MB, 远小于 128 MB 的断言; 没有随句子推进而增长的迹象. 阈值: 第 5 分钟后增长 <= 64 MB.
