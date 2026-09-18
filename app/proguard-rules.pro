# Plugin contract classes are looked up by the host through Binder and Intent extras.
-keep class io.github.supermonster003.autojs6.plugin.readium.epub.reader.** { *; }
-keep class org.autojs.plugin.common.api.PluginInfo { *; }
-keep class org.autojs.plugin.explorer.api.** { *; }
-dontwarn kotlinx.parcelize.Parcelize

# Readium keeps its own consumer rules inside the AARs; nothing extra is required here.
# Any missing-class warning surfaced by :app:assembleRelease must be resolved explicitly, not silenced.
