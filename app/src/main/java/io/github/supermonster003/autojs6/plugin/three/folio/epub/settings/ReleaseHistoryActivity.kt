package io.github.supermonster003.autojs6.plugin.three.folio.epub.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import io.github.supermonster003.autojs6.plugin.three.folio.epub.HostAppearanceActivity

/**
 * Renders the bundled changelog for the current locale (English when that one is missing) as one group per released
 * version, under the title "Release history" with the plugin name as the subtitle
 * (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md); reached from the settings page and from the update dialog. Not exported
 * and takes no data. The language, night mode and colors are those of the settings page ([HostAppearanceActivity]).
 */
internal class ReleaseHistoryActivity : HostAppearanceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge to edge like the settings page; the scaffold pads the system bars and the display cutout.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val candidates = ReleaseHistoryAssetPolicy.candidatesFor(resources.configuration.locales[0])
        val entries = loadReleaseHistory(candidates) { path ->
            assets.open(path).use { stream -> stream.readBytes().toString(Charsets.UTF_8) }
        }
        val palette = appPalette
        val colors = ReleaseHistoryColors(
            dark = palette.isDark,
            primary = palette.primary,
            primaryContainer = palette.primaryContainer,
            background = palette.background,
            surface = palette.appBar,
            onSurface = palette.onSurface,
            onSurfaceVariant = palette.onSurfaceVariant,
            outlineVariant = palette.outlineVariant,
        )
        setContent {
            ReleaseHistoryTheme(colors) {
                ReleaseHistoryScreen(entries = entries, onBack = ::finish)
            }
        }
    }
}
