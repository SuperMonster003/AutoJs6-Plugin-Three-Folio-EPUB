package io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeMappingTest {

    @Test
    fun fixedModesIgnoreTheHost() {
        for (hostDark in listOf(false, true)) {
            assertEquals(ReaderTheme.LIGHT, ThemeMapping.resolve(ThemeMode.LIGHT, hostDark))
            assertEquals(ReaderTheme.SEPIA, ThemeMapping.resolve(ThemeMode.SEPIA, hostDark))
            assertEquals(ReaderTheme.DARK, ThemeMapping.resolve(ThemeMode.DARK, hostDark))
        }
    }

    @Test
    fun followingTheHostMapsItsNightModeToLightOrDark() {
        assertEquals(ReaderTheme.LIGHT, ThemeMapping.resolve(ThemeMode.HOST, hostDark = false))
        assertEquals(ReaderTheme.DARK, ThemeMapping.resolve(ThemeMode.HOST, hostDark = true))
        assertEquals(ThemeMode.HOST, ThemeMode.DEFAULT)
    }

    @Test
    fun keysRoundTripAndUnknownKeysAreRejected() {
        for (mode in ThemeMode.entries) assertEquals(mode, ThemeMode.fromKey(mode.key))
        for (theme in ReaderTheme.entries) assertEquals(theme, ReaderTheme.fromKey(theme.key))
        assertNull(ThemeMode.fromKey("neon"))
        assertNull(ThemeMode.fromKey(null))
        assertNull(ReaderTheme.fromKey("host"))
        assertEquals(listOf("light", "sepia", "dark", "host"), ThemeMode.entries.map { it.key })
    }

    @Test
    fun themeColorsMirrorReadiumCss() {
        assertEquals(0xFFFFFFFF.toInt(), ReaderTheme.LIGHT.backgroundColor)
        assertEquals(0xFF121212.toInt(), ReaderTheme.LIGHT.contentColor)
        assertEquals(0xFFFAF4E8.toInt(), ReaderTheme.SEPIA.backgroundColor)
        assertEquals(0xFF121212.toInt(), ReaderTheme.SEPIA.contentColor)
        assertEquals(0xFF000000.toInt(), ReaderTheme.DARK.backgroundColor)
        assertEquals(0xFFFEFEFE.toInt(), ReaderTheme.DARK.contentColor)
    }
}
