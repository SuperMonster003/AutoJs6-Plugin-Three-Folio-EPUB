# Roadmap P7.5: the plugin package carries no blanket keep. Its components are kept through the manifest
# (AGP rules), fragments and view models through the androidx consumer rules, Parcelables and enums through
# the default optimize file; the host names nothing in the plugin package (docs/dev/release-size.md).
# The contract packages below are shared with the host by name, through Binder and Intent extras.
-keep class org.autojs.plugin.common.api.PluginInfo { *; }
-keep class org.autojs.plugin.explorer.api.** { *; }
-keep class org.autojs.plugin.epub.api.** { *; }
-dontwarn kotlinx.parcelize.Parcelize

# Readium keeps its own consumer rules inside the AARs; nothing extra is required here.
# Any missing-class warning surfaced by :app:assembleRelease must be resolved explicitly, not silenced.
