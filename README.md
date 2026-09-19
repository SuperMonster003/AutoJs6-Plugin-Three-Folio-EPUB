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

> 当前阶段 (1.0.0 开发构建): 阅读器以 Readium 默认设置打开书籍, 提供目录, 记住每本书的阅读位置, 并提供滚动模式, 点按区, 音量键翻页与沉浸模式. 书签, 偏好设置, 全文搜索, 朗读, 固定版式, 字体导入, 独立启动入口与 `epub` 脚本 API 已在 ROADMAP.md 中排期, 目前尚不可用.

******

### 功能亮点

******

- Readium 引擎: EPUB 2 (NCX) 与 EPUB 3 (NAV) 书籍经 Readium 导航器与 Readium CSS 渲染, 支持内链, 脚注与图片.
- 不复制文件: EPUB 容器通过只读描述符按位置读取, 即使大体积书籍也无需缓存文件即可打开.
- 目录导航: 从工具栏跳转到任意章节, 嵌套条目保留层级.
- 阅读位置记忆: 每本书的最后位置按内容指纹保存在插件私有存储中, 书籍移动或改名后仍能续读; `从头开始` 可清除.
- 阅读器界面: 工具栏显示书名与章节, 进度条显示位置与百分比, 点按中央切换沉浸模式, 点按区与音量键翻页, 可选滚动或分页模式.
- 外部链接: 点按 `http` 或 `https` 链接时先显示完整地址, 确认后才交给系统浏览器.
- 宿主集成: 菜单与对话框跟随 AutoJs6 的语言和暗色模式; 打开任何内容之前都会严格校验 Explorer Action 信封.
- 多语言: 界面, 说明, README 与更新日志均提供 10 种语言.

******

### 使用方法

******

1. 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) 页面下载最新的插件 APK 并安装到设备.
2. 打开 AutoJs6 的插件中心, 启用 `Readium EPUB Reader` 插件.
3. 在 AutoJs6 文件管理器中点按 `.epub` 文件, 或打开其溢出菜单 (更多操作) 并选择 `阅读 EPUB`.
4. 使用工具栏上的目录按钮在章节间跳转, 点按页面左右三分之一或按音量键翻页, 点按中央隐藏或显示工具栏; 按返回键关闭阅读器, 阅读位置会被记住.

> 若插件中心未显示该插件, 请先将 AutoJs6 升级到较新版本 (内部版本号 5269 及以上). Explorer Action v2 同时支持单文件的主按钮和溢出菜单, 通过临时只读授权访问文档及其父目录.

******

### 支持的格式

******

插件识别以下文件扩展名, 同时接受宿主明确标记为 `application/epub+zip` 的无扩展名文件:

```text
epub
```

仅支持 EPUB: EPUB 2 或 EPUB 3 的可重排与固定版式书籍. 漫画归档 (CBZ), 有声书, PDF 与 LCP 加密书籍不在范围内; 标记为 LCP 加密的书籍会提示无法读取, 而不是渲染乱码.

******

### 常见问题

******

#### 阅读位置是如何记住的?

每本书的最后位置按文件内容指纹 (而非路径) 保存在插件私有存储中, 再次打开同一本书时从上次位置继续. 在溢出菜单中选择 `从头开始` 可清除.

#### 可以更改字体, 字号或主题吗?

暂时不能. 阅读偏好 (字体, 字号, 行距, 边距, 主题) 将随偏好设置里程碑提供; 当前构建提供滚动或分页模式, 其余使用 Readium 默认值.

#### 这个插件会把我的书上传到某处吗?

不会. 插件没有自己的服务器. 只有当书籍本身引用远程资源时, 以及独立设置页计划中的手动更新检查, 才会使用网络.

******

### 权限与安全

******

插件对书籍内容保留 Readium 的默认行为: 不移除也不拦截书内的脚本与远程资源, 包括明文 `http://` 资源. 请只打开可信任的书籍.

- 最小权限: 插件只接收宿主授予的临时 content URI 读取权限, 不接触文件系统路径, 不把书籍写入存储.
- 严格信封: Explorer Action 请求必须恰好携带一个 EPUB 目标, 其父目录, 匹配的协议版本, 受支持的宿主构建以及两项读取授权; 其余一律在打开文件前拒绝.
- 有界解析: 损坏的容器 (非 zip, 缺 `container.xml`, 缺包文档, manifest 路径穿越) 以错误提示结束, 而不是崩溃.
- 外部链接完整显示并在确认后才交给系统浏览器; `http` 与 `https` 之外的 scheme 一律拒绝.
- 阅读数据只在本地: 位置以内容指纹为键, 不会把文件路径或文件名写入存储.

清单只申请网络权限与 AutoJs6 插件权限. AndroidX 还会附带一个包内签名权限, 用于保护未导出的动态接收器, 它不授予任何设备数据访问. 不申请存储, 媒体, 相机, 位置, 无障碍或悬浮窗权限.

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

计划中的能力及其完成状态以可勾选清单的形式记录在 ROADMAP.md 中, 按里程碑组织并附验收标准: 阅读位置记忆与书签, 偏好设置与字体导入, 全文搜索, 朗读, 固定版式, 独立应用入口, 宿主契约与 `epub` 脚本 API. 未勾选的条目表示计划而非已交付能力. 欢迎通过 Issues 反馈.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.0

_2026/09/19_

- `提示` 开发构建: 路线图 P0 阶段 (骨架, Readium 验证, 测试样本) 进行中; 首个公开版本随路线图 P8 阶段发布
- `新增` AutoJs6 文件管理器中为 `.epub` 文件提供 `阅读 EPUB` 主按钮与溢出菜单动作 (插件 ID `readium-epub-reader`, Explorer Action v2); 宿主报告为 `application/zip` 且扩展名为 `.epub` 的文件同样接受
- `新增` 阅读器基线: EPUB 2 与 EPUB 3 书籍经 Readium 导航器渲染, 提供目录与需确认的外部链接
- `新增` 阅读位置记忆: 每本书的最后位置按内容指纹保存 (打开时用快速键, 随后迁移到全文件 SHA-256), 下次打开自动恢复; `从头开始` 可清除
- `新增` 阅读器界面: 工具栏显示书名与当前章节, 进度条显示合成位置与百分比, 点按中央切换沉浸模式, 点按区与音量键翻页, 可切换滚动模式
- `新增` 书籍通过宿主授予的文件描述符按位置就地读取, 不复制也不解压到存储
- `新增` 界面, 说明, README 与更新日志提供 10 种语言
- `依赖` 附加 Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)

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

### 相关链接

******

- AutoJs6 文档: https://docs.autojs6.com
- EPUB 3.3 规范: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md)
