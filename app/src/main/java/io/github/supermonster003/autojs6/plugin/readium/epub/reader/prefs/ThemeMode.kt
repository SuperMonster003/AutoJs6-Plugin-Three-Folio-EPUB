package io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs

/**
 * The reader's colour scheme choice (roadmap D14): the three Readium themes plus "follow the host",
 * which resolves to light or dark from the host's night mode at display time.
 */
internal enum class ThemeMode(val key: String) {
    LIGHT("light"),
    SEPIA("sepia"),
    DARK("dark"),
    HOST("host"),
    ;

    companion object {
        val DEFAULT = HOST

        fun fromKey(key: String?): ThemeMode? = entries.firstOrNull { it.key == key }
    }
}

/**
 * A concrete Readium theme with the colours Readium CSS uses for the page, mirrored here so the
 * chrome colours and the mapping stay Android-free (`org.readium.r2.navigator.preferences.Theme`
 * initialises through `android.graphics.Color`).
 */
internal enum class ReaderTheme(val key: String, val backgroundColor: Int, val contentColor: Int) {
    LIGHT("light", 0xFFFFFFFF.toInt(), 0xFF121212.toInt()),
    SEPIA("sepia", 0xFFFAF4E8.toInt(), 0xFF121212.toInt()),
    DARK("dark", 0xFF000000.toInt(), 0xFFFEFEFE.toInt()),
    ;

    companion object {
        fun fromKey(key: String?): ReaderTheme? = entries.firstOrNull { it.key == key }
    }
}

internal object ThemeMapping {

    /** [hostDark] is the host's night mode, or the system's when the host settings are unavailable. */
    fun resolve(mode: ThemeMode, hostDark: Boolean): ReaderTheme = when (mode) {
        ThemeMode.LIGHT -> ReaderTheme.LIGHT
        ThemeMode.SEPIA -> ReaderTheme.SEPIA
        ThemeMode.DARK -> ReaderTheme.DARK
        ThemeMode.HOST -> if (hostDark) ReaderTheme.DARK else ReaderTheme.LIGHT
    }
}
