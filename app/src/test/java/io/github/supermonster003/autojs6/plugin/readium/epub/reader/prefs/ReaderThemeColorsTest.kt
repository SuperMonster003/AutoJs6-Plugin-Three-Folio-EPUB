package io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderThemeColorsTest {

    @Test
    fun luminanceAndContrastFollowWcag() {
        assertEquals(1.0, ReaderThemeColors.luminance(0xFFFFFFFF.toInt()), 1e-9)
        assertEquals(0.0, ReaderThemeColors.luminance(0xFF000000.toInt()), 1e-9)
        assertEquals(21.0, ReaderThemeColors.contrast(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 1e-9)
        assertEquals(
            ReaderThemeColors.contrast(0xFF123456.toInt(), 0xFFABCDEF.toInt()),
            ReaderThemeColors.contrast(0xFFABCDEF.toInt(), 0xFF123456.toInt()),
            1e-12,
        )
        assertEquals(0xFF000000.toInt(), ReaderThemeColors.contrastForeground(0xFFFFFFFF.toInt()))
        assertEquals(0xFFFFFFFF.toInt(), ReaderThemeColors.contrastForeground(0xFF000000.toInt()))
        assertEquals(0xFFFFFFFF.toInt(), ReaderThemeColors.contrastForeground(0xFF00695C.toInt()))
    }

    @Test
    fun everyThemeKeepsAReadableChrome() {
        for (theme in ReaderTheme.entries) {
            val colors = ReaderThemeColors.forTheme(theme)
            assertTrue(theme.name, ReaderThemeColors.contrast(colors.foreground, colors.background) >= 4.5)
            assertEquals(theme.name, theme.contentColor, colors.foreground)
            assertEquals(theme.name, 0xFF, colors.background ushr 24)
            assertTrue(theme.name, (colors.secondaryForeground ushr 24) in 0x01..0xFE)
            assertTrue(theme.name, (colors.accent ushr 24) in 0x01..0xFE)
        }
    }

    @Test
    fun lightBarsMatchTheThemeBrightness() {
        assertTrue(ReaderThemeColors.forTheme(ReaderTheme.LIGHT).lightBars)
        assertTrue(ReaderThemeColors.forTheme(ReaderTheme.SEPIA).lightBars)
        assertFalse(ReaderThemeColors.forTheme(ReaderTheme.DARK).lightBars)
    }

    @Test
    fun theBarBackgroundIsThePageColorNudgedTowardsTheText() {
        assertEquals(0xFFF1F1F1.toInt(), ReaderThemeColors.forTheme(ReaderTheme.LIGHT).background)
        assertEquals(0xFF0F0F0F.toInt(), ReaderThemeColors.forTheme(ReaderTheme.DARK).background)
        val sepia = ReaderThemeColors.forTheme(ReaderTheme.SEPIA).background
        assertTrue(ReaderThemeColors.luminance(sepia) < ReaderThemeColors.luminance(ReaderTheme.SEPIA.backgroundColor))
    }

    @Test
    fun blendAndAlphaHelpers() {
        assertEquals(0xFF808080.toInt(), ReaderThemeColors.blend(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 128 / 255.0))
        assertEquals(0xFF123456.toInt(), ReaderThemeColors.blend(0xFF123456.toInt(), 0xFFFFFFFF.toInt(), 0.0))
        assertEquals(0xFFFFFFFF.toInt(), ReaderThemeColors.blend(0xFF123456.toInt(), 0xFFFFFFFF.toInt(), 1.0))
        assertEquals(0x80123456.toInt(), ReaderThemeColors.withAlpha(0xFF123456.toInt(), 0.5))
        assertEquals(0x00123456, ReaderThemeColors.withAlpha(0xFF123456.toInt(), 0.0))
    }
}
