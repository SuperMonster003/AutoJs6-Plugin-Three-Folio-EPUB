# Third-party notices

This file records third-party components shipped with or consumed by the Readium EPUB Reader
plugin. The plugin itself is licensed under the Mozilla Public License 2.0; the components below
retain their own licenses. Runtime dependencies are added to this list in the same commit that
introduces them.

## AutoJs6 common plugin API

- Component: `common-plugin-api.aar` (Binder contract shared by AutoJs6 and its plugins: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/common-plugin-api`), release build; the same artifact vendored by the HTML Previewer and Markdown Previewer plugins
- SHA-256: `f9ff9676543f45b2ff2b64bb832d377dbd5b7003acf3bb4239e98556c68b246f` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

## AutoJs6 Explorer Action API

- Component: `explorer-action-api.aar` (Explorer Action Binder descriptor: `IExplorerActionPlugin`, `ExplorerActionCatalogKeys`, `ExplorerActionIntentExtras`, `ExplorerActionIntentValues`, `ExplorerActionPluginActions`, `ExplorerActionPluginIds`, `ExplorerActionPluginPermissions`, `ExplorerActionProtocol`, `ExplorerActionValues`)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/explorer-action-api`), frozen v1 descriptor audited for protocol v2 (see `docs/explorer-action-compatibility.md`)
- SHA-256: `40836c05b1d5fb532b21b916e57211c472225cf17e4cee0e9f5fc7aa48e21b45` (pinned in `locks/host-api-aars.lock` and `gradle/explorer-action-compatibility.properties`)
- License: Mozilla Public License 2.0

## AutoJs6 EPUB API

- Component: `epub-api.aar` (EPUB Binder contract of roadmap P5: `IEpubPlugin`, `IEpubBook`, `IEpubReaderSession`, `IEpubReaderCallback`, `EpubActions`, `EpubCapabilityKeys`, `EpubContract`, `EpubErrorCodes`, `EpubIds`)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/epub-api`), release build of host commit `261417e90` (AutoJs6 6.8.0, build 5282; contract version 1)
- SHA-256: `cabb624968959c46618c891026c328928f8f9ee4ec8cc2b45ee532e41c9ee535` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

## Readium Kotlin Toolkit

- Component: `org.readium.kotlin-toolkit:readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts` 3.4.0 (EPUB parsing, the WebView-based EPUB navigator with Readium CSS, and text-to-speech navigation)
- Source: <https://github.com/readium/kotlin-toolkit> (tag `3.4.0`, 2026-09-11)
- License: BSD 3-Clause License
- Derived source: `app/src/main/java/io/github/supermonster003/autojs6/plugin/readium/epub/reader/tts/SystemTtsEngine.kt` adapts `AndroidTtsEngine` from `readium-navigator-media-tts` (the engine is bound by explicit package name when the framework's default lookup finds none); the Readium copyright and license notice stay in the file header
- Notable transitive dependencies (all BSD / Apache / MIT licensed, no native libraries): Jsoup (MIT), kotlinx-coroutines, kotlinx-serialization, kotlinx-datetime (Apache 2.0), Timber (Apache 2.0), AndroidX Media3 (Apache 2.0, `media3-session` is also declared directly, see below), Guava for Android (Apache 2.0), `com.mcxiaoke.koi:core` (Apache 2.0). `media3-exoplayer` also declares `ACCESS_NETWORK_STATE` and `WAKE_LOCK` in its manifest; the plugin removes both (see `AndroidManifest.xml`) because R8 strips the unused player entirely

## AndroidX Media3

- Component: `androidx.media3:media3-session` 1.11.0 (the read-aloud foreground service, its media session, media buttons and notification; `media3-common` and `media3-common-ktx` come with it and with Readium)
- Source: <https://github.com/androidx/media> (tag `1.11.0`)
- License: Apache License 2.0

## Kotlin standard library and kotlinx

- Component: `org.jetbrains.kotlin:kotlin-stdlib` (provided through the Android Gradle Plugin built-in Kotlin support), `org.jetbrains.kotlinx:kotlinx-coroutines-android` 1.11.0
- Source: <https://github.com/JetBrains/kotlin>, <https://github.com/Kotlin/kotlinx.coroutines>
- License: Apache License 2.0

## AndroidX and Material Components

- Component: `androidx.activity:activity-ktx`, `androidx.appcompat:appcompat`, `androidx.constraintlayout:constraintlayout`, `androidx.core:core-ktx`, `androidx.fragment:fragment-ktx`, `androidx.lifecycle:lifecycle-runtime-ktx`, `androidx.lifecycle:lifecycle-viewmodel-ktx`, `androidx.webkit:webkit`, `com.google.android.material:material`
- Source: <https://github.com/androidx/androidx>, <https://github.com/material-components/material-components-android>
- License: Apache License 2.0

## Android desugared JDK libraries

- Component: `com.android.tools:desugar_jdk_libs` 2.1.5 (`java.time` and other JDK APIs on API 24 and 25 devices)
- Source: <https://github.com/google/desugar_jdk_libs>
- License: GNU General Public License version 2 with the Classpath Exception

## Test-only dependencies

These libraries are used by the JVM and instrumentation test source sets only and are not shipped
in the APK.

- JUnit 4 (`junit:junit`): Eclipse Public License 1.0
- AndroidX Test (`androidx.test:runner`, `androidx.test:rules`, `androidx.test.ext:junit`): Apache License 2.0
- kotlinx-coroutines-test: Apache License 2.0

## Test fixtures

The EPUB samples under `docs/fixtures/` are generated by `.python/generate_fixtures.py` and contain
only text written for this repository; they carry no third-party content. Any externally sourced
sample added later must be listed in `docs/fixtures/README.md` with its source, license and
SHA-256.
