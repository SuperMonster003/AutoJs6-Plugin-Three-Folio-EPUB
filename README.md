<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="readium-epub-reader-ic-launcher" border="0" width="128" />
  </p>

  <p>阅读 EPUB 电子书并提供目录, 搜索, 朗读与脚本提取能力</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言 (Languages)

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ar.md)

******

### 简介

******

一键阅读: 在 AutoJs6 文件管理器中直接打开 `.epub` 文件, 既可点按主按钮 `阅读 EPUB`, 也可从溢出菜单进入. 阅读器基于 [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0 构建, 这是众多商业阅读器共同采用的开源引擎.

插件直接通过宿主授予的临时文件描述符读取书籍, 不会拿到文件系统路径, 不会把书籍复制到任何位置, 也不会解压到存储.

> 1.0.0 是首个正式版本. 阅读器打开 EPUB 2 与 EPUB 3 书籍并提供目录, 记住每本书的阅读位置, 提供滚动模式, 点按区, 音量键翻页与沉浸模式, 偏好面板 (字号, 字体, 间距, 对齐, 列数与可跟随宿主夜间模式的主题), 导入的 TTF / OTF 字体, CJK 竖排与从右到左的书籍, 以单页或双页显示的固定版式书籍, 全文搜索, 书签, 书内链接, 注释与图片, 以及使用系统文字转语音引擎的朗读. 应用图标打开带最近书籍与系统文档选择器的启动器, 其它应用可通过 `ACTION_VIEW` 交来 EPUB, 设置页涵盖阅读器默认值, 设备上保存的数据与手动更新检查. `epub` 脚本 API, 宿主阅读器会话与三份示例脚本随 AutoJs6 6.8.0 (版本号 5282) 提供. 高亮, 笔记与导出计划在 1.1.0 (ROADMAP.md, P9).

******

### 截图

******

在手机上以 `docs/fixtures` 生成的样本书截取 (不展示任何第三方书籍); 界面跟随 AutoJs6 的语言, 此处为英文:

<table>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/reader.png?raw=true" alt="reader" width="180" /><br/>阅读</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/table-of-contents.png?raw=true" alt="table-of-contents" width="180" /><br/>目录</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/preferences.png?raw=true" alt="preferences" width="180" /><br/>阅读偏好</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/search.png?raw=true" alt="search" width="180" /><br/>全文搜索</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/bookmarks.png?raw=true" alt="bookmarks" width="180" /><br/>书签</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/read-aloud.png?raw=true" alt="read-aloud" width="180" /><br/>朗读</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/dark-theme.png?raw=true" alt="dark-theme" width="180" /><br/>深色主题</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/sepia-theme.png?raw=true" alt="sepia-theme" width="180" /><br/>羊皮纸主题</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/vertical-ja.png?raw=true" alt="vertical-ja" width="180" /><br/>日文竖排</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/fixed-layout.png?raw=true" alt="fixed-layout" width="180" /><br/>固定版式</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/launcher.png?raw=true" alt="launcher" width="180" /><br/>最近书籍</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/settings.png?raw=true" alt="settings" width="180" /><br/>设置</td>
  </tr>
</table>

******

### 功能亮点

******

- Readium 引擎: EPUB 2 (NCX) 与 EPUB 3 (NAV) 书籍经 Readium 导航器与 Readium CSS 渲染, 支持内链, 脚注与图片.
- 不复制文件: EPUB 容器通过只读描述符按位置读取, 即使大体积书籍也无需缓存文件即可打开.
- 目录导航: 从工具栏跳转到任意章节, 嵌套条目保留层级.
- 阅读位置记忆: 每本书的最后位置按内容指纹保存在插件私有存储中, 书籍移动或改名后仍能续读; `从头开始` 可清除.
- 阅读器界面: 工具栏显示书名与章节, 进度条显示位置与百分比, 点按中央切换沉浸模式, 点按区与音量键翻页, 可选滚动或分页模式.
- 阅读偏好: 底部面板可设置字号, 字体族, 行距, 页边距, 段间距, 对齐, 连字符, 出版商样式, 列数与分页或滚动布局; 修改即时生效并对所有书籍记忆. 浅色, 护眼, 深色三套主题, 或跟随宿主夜间模式; 工具栏与系统栏采用主题配色.
- 字体导入: 通过系统文档选择器选取 TTF 或 OTF 文件; 文件经校验后私有存放在插件内 (最多 10 个, 单个 20 MB), 列在偏好面板的内置字体之后, 对所有书籍生效, 并可在同一面板中删除.
- CJK 竖排与从右到左的书籍: 阅读进程跟随出版物, 从右到左的书籍点按区随之镜像; 页面进程为从右到左的日文与中文书籍竖排呈现, `文字方向` 偏好可强制横排或竖排. 界面自身的布局方向跟随 AutoJs6 语言, 与书籍无关.
- 固定版式书籍: 页码显示为 `第 x / N 页`, 面板提供 `双页显示` 选择 (自动在横屏时并排显示两页) 并隐藏不生效的文字偏好; 双指缩放与拖动由 Readium 内建.
- 全文搜索: 工具栏 `搜索` 入口查找书中的每一处命中, 每批 50 处 (上限 500), 按章节分组并显示前后文; 点击结果即跳转, 页面上高亮命中处, 进度栏上方提供上一处 / 下一处.
- 书签: 工具栏图标标记当前页 (本页已加书签时图标填充), `书签` 入口按时间倒序列出每个书签的章节名, 文本片段与时间, 可跳转, 删除或全部清除; 按书存储 (每本上限 500), 与阅读位置放在一起.
- 手势, 按键与链接: 点按翻页 (关闭, 左右或上下), 音量键, 硬件键盘按键与选中文本菜单 (复制, 分享, 网页搜索与文本处理应用); 书内链接带返回栈, 注释在对话框中显示, 外部链接确认后打开或直接打开, 点按图片全屏查看.
- 朗读: 溢出菜单的 `朗读` 用系统文字转语音引擎从当前页开始朗读, 高亮正在朗读的句子并自动翻页跟随; 页面下方的工具条与媒体通知提供播放 / 暂停, 上一句 / 下一句与停止, 支持耳机按键, 可调节语速, 音调, 语言与语音, 熄屏后继续朗读, 关闭阅读器即停止 (开启 `后台继续朗读` 后除外), 朗读设置还提供睡眠定时器 (15 / 30 / 60 分钟或本章结束) 与屏幕常亮开关.
- 外部链接: 点按 `http` 或 `https` 链接时先显示完整地址, 确认后才交给系统浏览器.
- 独立启动器: 应用图标打开最近书籍网格 (封面, 书名, 作者, 进度与最后阅读时间) 与 `打开 EPUB` 按钮, 后者通过系统文档选择器选书; 阅读器与文件管理器打开的是同一个.
- 从其它应用打开: 文件管理器, 浏览器或邮件应用可以通过 `ACTION_VIEW` 交给阅读器一个 `content://` EPUB; 溢出菜单中的 `加入最近书籍` 在发送方允许持久访问时把它留在启动器中.
- 设置页: 主题, 翻页, 朗读默认值, 链接与数据管理, 另有发行历史与只在点按时询问 GitHub 的手动更新检查
- 脚本服务: `org.autojs.plugin.EPUB` Binder 服务让 AutoJs6 宿主无需打开阅读器即可读取书籍 (元数据, 目录, 阅读顺序, 纯文本或轻量 Markdown 的章节文本, 资源, 全文搜索与位置数), 请求有上限, 同时最多打开 8 本书, 且只对宿主开放; AutoJs6 6.8.0 以 `epub` 模块把它提供给脚本 (见下文 "脚本调用").
- 宿主阅读器会话: AutoJs6 宿主可经 `org.autojs.plugin.EPUB` 服务在某本书上打开阅读器并跟随它 (位置, 书签与关闭事件), 跳到 locator, href 或进度, 翻页或跳章, 设置阅读偏好; 阅读器只由宿主携带一次性会话令牌显式启动, 关闭会话时阅读器仍留给用户, 除非宿主要求结束.
- 宿主集成: 菜单与对话框跟随 AutoJs6 的语言和暗色模式; 打开任何内容之前都会严格校验 Explorer Action 信封.
- 多语言: 界面, 说明, README 与更新日志均提供 10 种语言.

******

### 安装

******

1. 从插件中心: 在 AutoJs6 中打开 `插件`, 在官方列表中找到 `Readium EPUB Reader` 并点按安装; 插件中心会下载已签名的 APK, 完成安装并提供启用开关.
2. 从 GitHub: 在 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) 页面下载 APK (文件名带 CRC32, `SHA256SUMS` 列出校验值), 安装后在插件中心启用插件.
3. 要求: 文件管理器入口需要 AutoJs6 内部版本号 5269 及以上, `epub` 脚本 API 需要 AutoJs6 6.8.0 (版本号 5282) 及以上, Android 7.0 及以上, 以及系统 WebView.

******

### 使用方法

******

1. 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) 页面下载最新的插件 APK 并安装到设备.
2. 打开 AutoJs6 的插件中心, 启用 `Readium EPUB Reader` 插件.
3. 在 AutoJs6 文件管理器中点按 `.epub` 文件, 或打开其溢出菜单 (更多操作) 并选择 `阅读 EPUB`.
4. 使用工具栏上的目录按钮在章节间跳转, 使用偏好按钮调整文字与主题; 点按页面左右三分之一或按音量键翻页, 点按中央隐藏或显示工具栏; 按返回键关闭阅读器, 阅读位置会被记住.
5. 不经文件管理器时, 点按应用图标: 启动器列出最近书籍, `打开 EPUB` 通过系统文档选择器选书; 这样打开的书籍会带着封面与进度留在列表中.
6. 在其它应用 (文件管理器, 浏览器的下载, 邮件附件) 中为 `.epub` 文件选择本阅读器; 书籍以同样方式打开, 溢出菜单中的 `加入最近书籍` 在发送方允许持久访问时把它留在启动器的列表中.
7. 在启动器菜单或阅读器溢出菜单中打开 `设置`, 可设置主题, 翻页, 朗读默认值与链接, 清除插件保存的数据, 阅读发行历史或检查更新 (只有点按时才会联系 GitHub).
8. 从脚本: `epub.open(path)` 读取书籍 (元数据, 目录, 文本, 搜索), `epub.read(path)` 打开本阅读器并报告阅读位置; 见下文 "脚本调用" 与 AutoJs6 中的 `电子书` 示例.

> 若插件中心未显示该插件, 请先将 AutoJs6 升级到较新版本 (内部版本号 5269 及以上). Explorer Action v2 同时支持单文件的主按钮和溢出菜单, 通过临时只读授权访问文档及其父目录.

******

### 脚本调用

******

AutoJs6 6.8.0 新增全局模块 `epub` (别名 `$epub`), 由本插件提供服务: 不打开阅读器即可读取书籍, 也可以从脚本打开阅读器并跟踪阅读位置. AutoJs6 在 `示例 > 电子书` 下附带三份示例脚本, 完整参考见 [AutoJs6 文档](https://docs.autojs6.com/#/epub):

元数据, 目录与章节文本:

```javascript
let book = epub.open('./books/lighthouse.epub');
console.log(book.metadata.title, '-', (book.metadata.authors || []).join(', '));
book.toc.forEach(entry => console.log(entry.title, entry.href, (entry.children || []).length, 'children'));
console.log(book.readingOrder.length, 'resources,', book.positions, 'positions');
let first = book.readingOrder[0];
console.log(book.text(first.href, { format: 'markdown' }));
book.close();
```

封面, 搜索与便捷函数:

```javascript
let path = './books/lighthouse.epub';
let book = epub.open(path);
try {
    console.log('cover saved to', book.cover(files.cwd(), { overwrite: true }));
} catch (e) {
    if (!(e instanceof epub.EpubError) || e.code !== 'RESOURCE_NOT_FOUND') throw e;
    console.log('this book has no cover');
}
book.search('lighthouse', { limit: 20 }).forEach(hit => console.log(hit.title || hit.href, ':', hit.text));
files.write('./lighthouse.txt', book.textAll({ maxChars: 2 * 1024 * 1024 }));
book.close();
console.log(epub.metadata(path).language); // the convenience functions open and close the book themselves
epub.tocAsync(path).then(toc => console.log(toc.length, 'entries'));
```

打开阅读器并跟踪位置:

```javascript
let session = epub.read('./books/lighthouse.epub', { progression: 0.25, preferences: { theme: 'sepia' } });
session.on('open', e => console.log('opened', e.title, 'at', e.href, '|', e.positions, 'positions'));
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('bookmark', e => console.log('bookmark', e.action, e.locator.href, '| total', session.bookmarks().length));
session.on('close', e => console.log('closed:', e.reason)); // user, host, replaced, timeout, error or overflow
setTimeout(() => session.isOpen && session.nextChapter(), 30 * 1000);
setTimeout(() => session.isOpen && session.close(), 60 * 1000);
```

路径相对脚本工作目录或使用绝对路径 (不接受 `content://` URI). 插件或书籍不可用时每个调用都会抛出带 `code` 的 `EpubError` (`PLUGIN_UNAVAILABLE`, `NOT_EPUB`, `ENCRYPTED`, `PARSE_FAILED`, `TIMEOUT` 等), `epub.isAvailable()` 告知插件是否已安装并启用, 每个方法都有返回 Promise 的 `*Async` 版本.

******

### 支持的格式

******

插件识别以下文件扩展名, 同时接受宿主明确标记为 `application/epub+zip` 的无扩展名文件:

```text
epub
```

仅支持 EPUB: EPUB 2 或 EPUB 3 的可重排与固定版式书籍. 漫画归档 (CBZ), 有声书, PDF 与 LCP 加密书籍不在范围内; 标记为 LCP 加密的书籍会提示无法读取, 而不是渲染乱码.

******

### 兼容性

******

插件的运行要求, 已验证的环境与不在范围内的内容:

- AutoJs6: 文件管理器入口 (Explorer Action v2) 需要内部版本号 5269 及以上; `epub` 脚本 API, 宿主阅读器会话与示例脚本需要 AutoJs6 6.8.0 (版本号 5282), 这也是本次发布审计过的最高宿主版本.
- Android 7.0 (API 24) 至 Android 16 (API 37, 目标版本); 页面在设备的 WebView 中渲染, 需要较新的 Android System WebView 或 Chrome. 插件不含原生库, 在 16 KB 页设备上无需改动即可运行.
- 已在 AVD API 24 / 33 / 36 / 37, Sony Xperia XZ1 Compact (Android 9), Redmi 12C (Android 13, MIUI) 与 Xiaomi Pad 6 (Android 15, 服务侧) 上验证; 设备 x 场景矩阵, 偏差与 WebView 版本见 `docs/dev/compatibility-matrix.md`.
- 书籍: EPUB 2 与 EPUB 3, 可重排与固定版式, CJK 竖排与从右到左. 受 DRM 保护的书籍 (LCP, Adobe ADEPT) 会提示受保护而不会渲染; PDF, MOBI, AZW, CBZ 与有声书不在范围内.
- 朗读需要带有书籍语言语音数据的文字转语音引擎 (Google 语音服务, 厂商引擎或其它已安装引擎); 没有可用引擎的设备会在约 20 秒后给出提示, 而不是一直无声.
- 体积与性能: release APK 约 3.3 MB; 200 MB 的书在 2017 年的手机上 1 到 3 秒打开, 位置与指纹计算不会拖慢首屏, 数千章的书打开明显更慢 (`docs/dev/performance-baseline.md`).

******

### 常见问题

******

#### 阅读位置是如何记住的?

每本书的最后位置按文件内容指纹 (而非路径) 保存在插件私有存储中, 再次打开同一本书时从上次位置继续. 在溢出菜单中选择 `从头开始` 可清除.

#### 可以更改字体, 字号或主题吗?

可以. 从工具栏打开偏好面板即可设置字号, 字体族 (出版商默认, 衬线, 无衬线, 等宽或 Readium 内置的无障碍字体), 行距, 边距, 间距, 对齐, 列数与主题 (浅色, 护眼, 深色或跟随宿主). 在面板中点击 `导入字体` 即可添加自己的 TTF 或 OTF 文件; 字体私有存放在插件内, 可通过 `管理字体` 删除.

#### 这个插件会把我的书上传到某处吗?

不会. 插件没有自己的服务器. 只有当书籍本身引用远程资源时, 以及设置页中的手动更新检查 (只在你点按时通过 HTTPS 询问 GitHub Releases API, 从不下载任何文件), 才会使用网络.

#### 为什么不支持 PDF, MOBI 或 AZW?

阅读器基于 Readium 工具包构建, 它只渲染 EPUB. PDF 需要另一套渲染器, MOBI / AZW 是 Amazon 的格式且没有开放的渲染引擎; 请先用 Calibre 之类的工具转换为 EPUB. 漫画归档 (CBZ) 与有声书同样不在范围内.

#### 朗读没有声音

插件通过系统设置中选择的文字转语音引擎发声 (`无障碍 > 文字转语音输出`). 请确认已安装带有书籍语言语音数据的引擎, 媒体音量已调高, 且没有其它应用占用音频焦点 (来电或音乐会暂停朗读). 引擎不会说的语言会使用引擎的默认语音; 没有可用引擎的设备会在约 20 秒后给出提示.

#### 导入的字体在书中没有生效

出版方样式可能固定了自己的字体: 在偏好面板中关闭 `出版方样式`, 再重新选择导入的字体. 只接受 TTF 与 OTF 文件 (字体集 `.ttc` 会以专门的提示拒绝), 字体应用于正文, 出版方指定了特定字族的标题保持不变.

#### 日文或中文竖排书籍如何处理?

spine 声明从右到左翻页且语言为日文或中文的书籍会竖排渲染并从右向左翻页; `文字方向` 偏好可以为任何书籍强制横排或竖排. 界面保持 AutoJs6 语言的方向, 因此英文界面仍从左到右, 而书籍从右向左阅读.

******

### 权限与安全

******

插件对书籍内容保留 Readium 的默认行为: 不移除也不拦截书内的脚本与远程资源, 包括明文 `http://` 资源. 请只打开可信任的书籍.

- 最小权限: 插件只接收宿主授予的临时 content URI 读取权限, 不接触文件系统路径, 不把书籍写入存储.
- 严格信封: Explorer Action 请求必须恰好携带一个 EPUB 目标, 其父目录, 匹配的协议版本, 受支持的宿主构建以及两项读取授权; 其余一律在打开文件前拒绝.
- 面向其它应用的独立入口: `ACTION_VIEW` 由单独导出的 Activity 承载, 只接受带读取授权的 `content://` 文档 (不接受 `file://`, 也不接受目录), Explorer Action 的 Activity 仍受 AutoJs6 插件权限保护; 只有当你选择 `加入最近书籍` 时才会保留发送方的授权.
- 受保护的脚本服务: `org.autojs.plugin.EPUB` 服务在插件权限之后导出, 只服务签名匹配的 AutoJs6 宿主包, 每个请求都按固定上限校验 (href 长度, 文本窗口, 搜索分页, 选项大小, 8 本同时打开, 单个资源 64 MB), 并且从不从后台启动阅读器: 阅读器会话只把一次性令牌交给宿主, 由宿主自己启动阅读器 Activity, 无人认领的会话 60 s 后关闭, 错误令牌什么也打不开.
- 只按需检查更新: 当你点按 `检查更新` 时, 设置页通过 HTTPS 询问 GitHub Releases API (每天至多一次, 不跟随重定向, 限制响应大小), 显示结果并在浏览器中打开发布页面; 插件自身从不下载或安装任何东西.
- 有界解析: 损坏的容器 (非 zip, 缺 `container.xml`, 缺包文档, manifest 路径穿越) 以错误提示结束, 而不是崩溃.
- 外部链接完整显示并在确认后才交给系统浏览器; `http` 与 `https` 之外的 scheme 一律拒绝.
- 阅读数据只在本地: 位置以内容指纹为键, 不会把文件路径或文件名写入存储.
- 朗读在一个未导出的媒体播放服务中进行, 它只在朗读期间存在, 在你停止, 到达书末或关闭阅读器时结束 (开启 `后台继续朗读` 后则在你从通知栏停止时结束); 文本交给系统设置中选定的文字转语音引擎, 插件不持有唤醒锁.

清单申请网络权限, AutoJs6 插件权限, 以及朗读所需的前台服务权限 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) 和 Android 13+ 的 `POST_NOTIFICATIONS` (仅在开始朗读时请求一次, 可以拒绝, 拒绝后朗读继续但通知栏中没有控制按钮). AndroidX 还会附带一个包内签名权限, 用于保护未导出的动态接收器, 它不授予任何设备数据访问. 不申请存储, 媒体, 相机, 位置, 无障碍或悬浮窗权限.

******

### 插件接口

******

以下信息面向开发者, 宿主通过这些标识发现并执行插件:

```text
application id: io.github.supermonster003.autojs6.plugin.readium.epub.reader
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: readium-epub-reader
engine: explorer-action
variant: default
protocol version: 2
minimum host build: 5269
audited host build: 5282
audited host protocol: 22
```

Explorer Action v2 同时支持单文件的主按钮和溢出菜单, 通过临时只读授权访问文档及其父目录. 需要 AutoJs6 构建 5269 或更高版本.

- [查看 Explorer Action 兼容矩阵](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/explorer-action-compatibility.md)

******

### 开发路线图

******

ROADMAP.md 以可勾选的列表跟踪每个里程碑, 附验收标准与证据: P0 至 P8 (阅读器, 偏好与字体, 搜索与书签, 朗读, 独立入口, 宿主契约, `epub` 脚本 API, 健壮性与 1.0.0 发布 gate) 已勾选; P9 (高亮, 笔记与导出, 1.1.0) 在计划中. 未勾选的条目是计划而非已交付的能力. 欢迎通过 Issues 反馈.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.1.0

_2026/09/21_

- `提示` 1.1.0 开发构建 (路线图 P9): 高亮, 笔记与导出正在开发中; 以下条目记录已落地的部分
- `依赖` 附加 `androidx.room:room-runtime` 2.8.1 (高亮与笔记数据库, 路线图 D4 / P9); `room-compiler` 仅在构建期经 KSP 运行

#### v1.0.0

_2026/09/21_

- `提示` 首个正式版本: 1.0.0 完成路线图 P0 至 P8 阶段 (阅读器, 偏好与字体, 搜索与书签, 朗读, 独立入口, 宿主契约, `epub` 脚本 API, 健壮性与发布 gate); `epub` 脚本 API 与示例脚本随 AutoJs6 6.8.0 (版本号 5282) 提供; 高亮, 笔记与导出将在 1.1.0 (路线图 P9) 跟进
- `新增` AutoJs6 文件管理器中为 `.epub` 文件提供 `阅读 EPUB` 主按钮与溢出菜单动作 (插件 ID `readium-epub-reader`, Explorer Action v2); 宿主报告为 `application/zip` 且扩展名为 `.epub` 的文件同样接受
- `新增` 阅读器基线: EPUB 2 与 EPUB 3 书籍经 Readium 导航器渲染, 提供目录与需确认的外部链接
- `新增` 阅读位置记忆: 每本书的最后位置按内容指纹保存 (打开时用快速键, 随后迁移到全文件 SHA-256), 下次打开自动恢复; `从头开始` 可清除
- `新增` 阅读器界面: 工具栏显示书名与当前章节, 进度条显示合成位置与百分比, 点按中央切换沉浸模式, 点按区与音量键翻页, 可切换滚动模式
- `新增` 阅读偏好面板: 字号, 字体族, 行距, 页边距, 段间距, 对齐, 连字符, 出版商样式, 列数与分页 / 滚动布局即时生效并对所有书籍记忆; 浅色, 护眼, 深色三套主题加 `跟随宿主`, 工具栏与系统栏配色随主题变化
- `新增` 字体导入: 经系统文档选择器选取的 TTF / OTF 文件先校验 (SFNT 签名, `name` 表, 单个 20 MB, 最多 10 个), 再私有存放于 `files/fonts/<sha256>` 并以 `@font-face` 声明提供给 Readium 导航器; 导入的字体出现在偏好面板的内置字体之后, 可在面板中删除
- `新增` CJK 竖排与从右到左的书籍: 阅读进程跟随出版物 (从右到左的书籍点按区镜像), 页面进程为从右到左的日文 / 中文书籍经 Readium CSS 竖排呈现, `文字方向` 偏好可强制横排或竖排, 界面布局方向与书籍无关
- `新增` 固定版式书籍: 进度栏显示 `第 x / N 页`, `双页显示` 偏好 (自动 = 横屏双页, 单页, 双页), 隐藏对固定版式无效的文字偏好, 双指缩放由 Readium 内建
- `新增` 全文搜索: 工具栏 `搜索` 入口打开结果面板, 每批加载 50 处 (上限 500), 按章节分组并显示上下文; 点击结果跳转并高亮页面上的命中处, 进度栏上方提供上一处 / 下一处
- `新增` 书签: 工具栏图标为当前页添加或移除书签 (含章节名与文本片段), `书签` 面板按时间倒序列出, 支持跳转, 删除与全部清除; 按书存储 (每本上限 500), 与阅读位置放在一起
- `新增` 阅读控制: 点按翻页可关闭或设为左右 / 上下, 硬件键盘的方向键, 翻页键与空格可翻页, 选中文本后提供复制, 分享, 网页搜索与系统的文本处理应用
- `新增` 链接: 书内链接在阅读器内跳转, 返回键先回到跳转前的位置, 脚注与尾注在对话框中显示, 外部链接确认后打开或按设置直接交给浏览器; 其它 scheme 的链接拒绝打开
- `新增` 图片: 点按图片以全屏查看并显示说明文字
- `新增` 朗读: 溢出菜单用系统文字转语音引擎从当前页开始朗读, 高亮正在朗读的句子并自动翻页跟随; 页面下方的工具条与媒体通知提供播放 / 暂停, 上一句 / 下一句与停止, 支持耳机按键, 可调节语速, 音调, 语言与语音, 熄屏后继续朗读, 关闭阅读器即停止
- `新增` 朗读睡眠定时器 (15 / 30 / 60 分钟或本章结束), 朗读时屏幕常亮开关与 `后台继续朗读` (默认关闭): 开启后关闭阅读器仍继续朗读到书末或定时器结束, 通知栏可暂停 / 停止并在朗读句处重新打开书籍, 再次打开同一本书时衔接当前朗读位置; 后台朗读停止时保存阅读位置
- `新增` 书籍通过宿主授予的文件描述符按位置就地读取, 不复制也不解压到存储
- `新增` 界面, 说明, README 与更新日志提供 10 种语言
- `新增` 独立启动器: 应用图标打开最近书籍网格 (封面, 书名, 作者, 进度与最后阅读时间, 上限 100 本) 与 `打开 EPUB` 按钮, 后者通过系统文档选择器选书; 选中的书籍保留持久读取授权, 因此可以从网格再次打开, 文件已不存在的书籍会标记为不可用, 长按可移除书籍并释放其授权
- `新增` 从其它应用打开: 文件管理器, 浏览器与邮件应用可以通过 `ACTION_VIEW` 把 `content://` EPUB 交给阅读器; 书籍照常打开但不进入启动器列表, 除非在溢出菜单选择 `加入最近书籍` 且能保留发送方的访问授权 (无法保留时会拒绝); `file://` 路径, 无读取授权的请求与目录一律拒绝
- `新增` 设置页与发行历史: 启动器菜单与阅读器溢出菜单打开设置页, 可设置主题, 点击翻页区域, 音量键翻页, 朗读语速, 音调与默认睡眠定时, 外部链接, 以及插件保存的数据 (阅读位置, 最近书籍, 导入的字体, 偏好, 均在确认后清除), 并提供关于信息, 内置发行历史与手动更新检查 (只在点按时询问 GitHub, 在浏览器中打开发布页面, 不下载任何文件, `忽略此版本` 会被记住)
- `新增` 面向 AutoJs6 宿主的 EPUB 能力服务 (路线图 P5.2): `org.autojs.plugin.EPUB` Binder 服务从宿主的只读描述符打开书籍, 提供元数据, 目录, 阅读顺序, 章节文本 (纯文本或轻量 Markdown, 分页续取), 经管道导出的资源, 全文搜索与位置数; 同时最多打开 8 本, 空闲 5 分钟自动关闭, 每个请求都做边界校验, 只有 AutoJs6 宿主可以调用该服务
- `新增` 基于 EPUB 契约的宿主阅读器会话 (路线图 P5.3): `openReader` 打开书籍, 生成一次性会话令牌并把启动交给宿主, 由宿主携带该令牌显式启动阅读器 Activity; 会话随后以同一 generation 与严格递增的序号上报 `open`, `progress` (最快每 500 ms 一次), `bookmark`, `error` 与 `close` 事件, 接受 `goTo` (locator, href 或进度), `navigate` (翻页或跳章), `setPreferences` (契约规定的偏好子集; 未知键只上报不应用), `getBookmarks` 与 `getState`, 新会话替换旧会话, 60 s 内无阅读器认领则自动关闭, 宿主 `close` 不结束阅读器, 除非明确要求
- `修复` AGP 9.1 构建时的 SDK XML v4 解析警告及 JVM 单元测试组装任务误触发 APK 原生库对齐检查的问题 (共享构建插件 1.8.3)
- `修复` 阅读进度写入失败 (书籍目录被移除, 存储不可写) 不再让阅读器崩溃, 仅丢失该条记录并继续阅读
- `修复` 宿主 AutoJs6 在阅读器读取其设置提供器时被停止或更新, 不再连带杀死阅读器; 该次读取只是失败, 不套用宿主的语言 / 夜间模式
- `修复` 宿主会话的启动 intent 到达已位于任务栈顶的阅读器时 (single-top 投递, 例如脚本把阅读器留在前台后), 现在会在新的阅读器实例中打开, 而不是无人认领地等到 60 秒超时; 原阅读器像被替换时一样结束 (路线图 P6.2)
- `修复` NCX / OPF 的 XML 被截断或格式错误的书籍现在会失败关闭: 服务回答 `PARSE_FAILED`, 阅读器显示打开失败面板, 而不是 `INTERNAL` 代码或因 Readium XML 解析器抛出的 `AssertionError` 而崩溃 (路线图 P7.1)
- `修复` 单页 `search` 在插件侧限制为 50 秒, 查询只在超大书籍 (50 000 个资源) 的靠后位置命中时回答 `TIMEOUT`, Binder 线程不再在宿主自身的 60 秒调用超时之后继续忙碌 (路线图 P7.1)
- `修复` 阅读器中 Readium 创建的每个页面 WebView 现在都在 Readium 自身设置之上带有边界: 不允许访问文件系统与内容提供器, 两个 file URL 跨源开关关闭, JavaScript 为 Readium 保持开启 (路线图 D6); WebView, 容器与组件边界的复核记录在 `docs/dev/security-boundaries.md` (路线图 P7.2)
- `修复` 阅读器进程因未捕获异常死亡时, 先同步把当前阅读位置写入磁盘, 再交给系统自身的崩溃处理; 插件本身不写日志也不为 Timber 种树, 书名, 路径与正文不会进入 logcat (路线图 P7.7)
- `修复` 选择 TrueType / OpenType 字体集 (`.ttc` / `.otc`) 作为阅读字体时, 现在会提示不支持字体集, 而不是把文件当作非字体文件; 由 `docs/dev/compatibility-matrix.md` 记录的设备 x 场景兼容矩阵运行时发现 (路线图 P7.3)
- `修复` 无障碍: 阅读偏好面板的四个滑块 (字号, 页边距, 行高, 段间距) 现在带有屏幕阅读器可朗读的标签, 阅读器工具栏在系统大字号下会随之变高而不再裁掉章节副标题; 覆盖标签, 48 dp 触控目标, 1.3 倍字体缩放, 夜间模式, 强制 RTL, 键盘翻页与横屏的 instrumentation 审计作为依据 (路线图 P7.6)
- `修复` 朗读不再无限等待一个始终无法完成初始化的语音引擎 (API 24 模拟器上没有语音数据的 Google TTS 正是如此): 20 秒后阅读器提示没有可用引擎并回到空闲状态, 之后才到来的会话会被关闭 (路线图 P7.3)
- `优化` Release APK 体积: Readium 随导航器资源附带的 DiViNa 播放器 (427 KB, EPUB 阅读器从不使用) 不再进入合并后的资源, 插件包的整体 keep 规则也已移除, R8 因此也能压缩插件自身的类; release APK 从 P5 后的 3,922,786 B 降到 3,328,220 B (路线图 P7.5, 细节见 `docs/dev/release-size.md`)
- `依赖` 附加 Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
- `依赖` 附加 `androidx.media3:media3-session` 1.11.0 (`readium-navigator-media-tts` 已间接引入; 为朗读前台服务直接声明)
- `依赖` 附加 `org.jsoup:jsoup` 1.23.2 (`readium-shared` 已间接引入; 为 EPUB 服务的章节文本提取直接声明)

##### 更多发行历史可参阅

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 构建:

```powershell
.\gradlew.bat :app:assembleRelease
```

构建参数来自 `version.properties`, 当前最低 SDK 为 24, 目标 SDK 为 37.

******

### 本地化与文档生成

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` 提供插件信息和阅读器界面的本地化, `plugin_instruction.md` 提供宿主侧展示的使用说明. README 与更新日志一律修改 `.readme/` 与 `.changelog/` 下的 JSON 源文件, 再运行 `py .python/generate_markdown.py` 重新生成, 生成产物不手工编辑; 运行 `py .python/generate_markdown.py --check` 可校验源文件与生成产物是否同步.

******

### 许可证与第三方声明

******

插件以 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE) 授权. Readium Kotlin Toolkit (BSD 3-Clause), AndroidX Media3 与 Jsoup, AutoJs6 契约库以及 APK 中附带的其它组件的版本, 校验值与许可证列于 [第三方声明](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相关链接

******

- AutoJs6 文档: https://docs.autojs6.com
- `epub` 脚本 API 参考: https://docs.autojs6.com/#/epub
- EPUB 3.3 规范: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md
- 16 KB 页对齐与构建验证: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md
