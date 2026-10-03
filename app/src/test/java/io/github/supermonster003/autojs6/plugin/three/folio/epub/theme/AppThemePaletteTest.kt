package io.github.supermonster003.autojs6.plugin.three.folio.epub.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemePaletteTest {

    @Test
    fun everySupportedSeedProducesOpaqueReadableLightAndDarkRoles() {
        testSeeds.forEach { seed ->
            listOf(false, true).forEach { dark ->
                val palette = AppThemePaletteGenerator.generate(seed, dark)
                assertEquals(seed or -0x1000000, palette.source)
                assertOpaque(palette)
                assertContrastAtLeast(palette.onAppBar, palette.appBar, 4.5)
                assertContrastAtLeast(palette.onPrimary, palette.primary, 4.5)
                assertContrastAtLeast(palette.onPrimaryContainer, palette.primaryContainer, 4.5)
                assertContrastAtLeast(palette.onSecondary, palette.secondary, 4.5)
                assertContrastAtLeast(palette.onSecondaryContainer, palette.secondaryContainer, 4.5)
                assertContrastAtLeast(palette.onBackground, palette.background, 7.0)
                assertContrastAtLeast(palette.onSurface, palette.surface, 7.0)
                assertContrastAtLeast(
                    palette.bottomControlContent,
                    palette.bottomControlSurface,
                    4.5,
                )
                assertContrastAtLeast(palette.onSurfaceVariant, palette.background, 4.5)
                assertContrastAtLeast(palette.onError, palette.error, 4.5)
                assertContrastAtLeast(palette.onErrorContainer, palette.errorContainer, 4.5)
            }
        }
    }

    @Test
    fun sourceColorIsPreservedWhileSemanticRolesAdaptToMode() {
        val source = 0xFFFFDEAD.toInt()
        val light = AppThemePaletteGenerator.generate(source, dark = false)
        val dark = AppThemePaletteGenerator.generate(source, dark = true)

        assertEquals(source, light.source)
        assertEquals(source, dark.source)
        assertEquals(0xFFFFFFFF.toInt(), light.appBar)
        assertEquals(0xFF1E1E1E.toInt(), dark.appBar)
        assertEquals(0xFFF3F4F5.toInt(), light.background)
        assertEquals(0xFF121212.toInt(), dark.background)
        assertNotEquals(light.background, dark.background)
        assertNotEquals(light.primary, dark.primary)
        assertNotEquals(light.bottomControlSurface, dark.bottomControlSurface)
    }

    @Test
    fun customColorParserAcceptsRgbHexAndRejectsAmbiguousValues() {
        assertEquals(0xFF123ABC.toInt(), AppThemePaletteGenerator.parseOpaqueColor("#123abc"))
        assertEquals(0xFFFFDEAD.toInt(), AppThemePaletteGenerator.parseOpaqueColor("0xFFDEAD"))
        assertEquals(0xFF000000.toInt(), AppThemePaletteGenerator.parseOpaqueColor("000000"))
        assertEquals(null, AppThemePaletteGenerator.parseOpaqueColor("#123"))
        assertEquals(null, AppThemePaletteGenerator.parseOpaqueColor("#80123ABC"))
        assertEquals(null, AppThemePaletteGenerator.parseOpaqueColor("red"))
        assertEquals(null, AppThemePaletteGenerator.parseOpaqueColor("#GG0000"))
    }

    @Test
    fun hostAccentUsesTheSameTonalPolicyAsPrimary() {
        for (dark in listOf(false, true)) {
            val primarySeed = 0xFFE91E63.toInt()
            val accentSeed = 0xFF009688.toInt()
            val combined = AppThemePaletteGenerator.generate(primarySeed, dark, accentSeed)
            val accent = AppThemePaletteGenerator.generate(accentSeed, dark)
            assertEquals(accent.primary, combined.secondary)
            assertEquals(accent.onPrimary, combined.onSecondary)
            assertContrastAtLeast(combined.onSecondary, combined.secondary, 4.5)
        }
    }

    private fun assertOpaque(palette: AppThemePalette) {
        palette.javaClass.declaredFields
            .filter { field -> field.type == Int::class.javaPrimitiveType }
            .forEach { field ->
                field.isAccessible = true
                val color = field.getInt(palette)
                assertEquals("${field.name} must be opaque", 0xFF, color ushr 24)
            }
    }

    private fun assertContrastAtLeast(foreground: Int, background: Int, minimum: Double) {
        val actual = AppThemePaletteGenerator.contrastRatio(foreground, background)
        assertTrue(
            "${AppThemePaletteGenerator.colorHex(foreground)} on " +
                "${AppThemePaletteGenerator.colorHex(background)}: $actual < $minimum",
            actual + 0.001 >= minimum,
        )
    }

    private companion object {
        val testSeeds = listOf(
            AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE,
            0xFFF44336.toInt(),
            0xFFE91E63.toInt(),
            0xFF9C27B0.toInt(),
            0xFF673AB7.toInt(),
            0xFF3F51B5.toInt(),
            0xFF2196F3.toInt(),
            0xFF03A9F4.toInt(),
            0xFF00BCD4.toInt(),
            0xFF009688.toInt(),
            0xFF4CAF50.toInt(),
            0xFF8BC34A.toInt(),
            0xFFCDDC39.toInt(),
            0xFFFFEB3B.toInt(),
            0xFFFFC107.toInt(),
            0xFFFF9800.toInt(),
            0xFFFF5722.toInt(),
            0xFF795548.toInt(),
            0xFF9E9E9E.toInt(),
            0xFF607D8B.toInt(),
            0xFF000000.toInt(),
            0xFFFFFFFF.toInt(),
            0xFF010203.toInt(),
            0xFFFEFDFC.toInt(),
        )
    }
}
