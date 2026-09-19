package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ReaderTheme
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.StoredPreferences
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMapping
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * The reader's global preferences as the Activity and the panel see them (roadmap D14): the theme
 * mode plus Readium's [EpubPreferences] without a theme. The theme is derived from the mode and
 * the host's night mode every time the preferences are handed to the navigator, so "follow the
 * host" stays in sync when the host switches its night mode.
 */
@OptIn(ExperimentalReadiumApi::class)
internal data class ReaderPreferencesState(val themeMode: ThemeMode, val epub: EpubPreferences) {

    fun resolvedTheme(hostDark: Boolean): ReaderTheme = ThemeMapping.resolve(themeMode, hostDark)

    /** What the navigator receives: the stored preferences plus the resolved theme. */
    fun effective(hostDark: Boolean): EpubPreferences = epub.copy(theme = resolvedTheme(hostDark).toReadium())

    /** The file form: Readium's own JSON for the preferences, the mode next to it. */
    fun toStored(): StoredPreferences =
        StoredPreferences(themeMode, JSONObject(SERIALIZER.serialize(epub.copy(theme = null))))

    companion object {
        private val SERIALIZER = EpubPreferencesSerializer()

        val DEFAULT = ReaderPreferencesState(ThemeMode.DEFAULT, EpubPreferences())

        /** Decodes through Readium's serializer; the codec already dropped whatever Readium rejects. */
        fun fromStored(stored: StoredPreferences): ReaderPreferencesState {
            val epub = runCatching { SERIALIZER.deserialize(stored.readium.toString()) }
                .getOrDefault(EpubPreferences())
            return ReaderPreferencesState(stored.themeMode, epub.copy(theme = null))
        }
    }
}

internal fun ReaderTheme.toReadium(): Theme = when (this) {
    ReaderTheme.LIGHT -> Theme.LIGHT
    ReaderTheme.SEPIA -> Theme.SEPIA
    ReaderTheme.DARK -> Theme.DARK
}
