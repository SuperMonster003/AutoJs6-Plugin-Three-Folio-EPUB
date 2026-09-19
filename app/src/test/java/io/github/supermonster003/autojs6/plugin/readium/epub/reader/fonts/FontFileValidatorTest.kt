package io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.SyntheticFonts.macRoman
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.SyntheticFonts.unicode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.SyntheticFonts.windows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FontFileValidatorTest {

    @Test
    fun aTrueTypeFontReportsItsTypographicFamilyBeforeTheLegacyOne() {
        val font = SyntheticFonts.build(names = listOf(windows(1, "Test Sans Light"), windows(16, "Test Sans")))

        assertEquals(FontInspection.Font(FontFormat.TRUE_TYPE, "Test Sans"), FontFileValidator.inspect(font))

        val legacyOnly = SyntheticFonts.build(names = listOf(windows(1, "Test Sans Light")))
        assertEquals(FontInspection.Font(FontFormat.TRUE_TYPE, "Test Sans Light"), FontFileValidator.inspect(legacyOnly))
    }

    @Test
    fun theFamilyNameFallsBackThroughThePlatforms() {
        val unicodeAndMac = SyntheticFonts.build(names = listOf(macRoman(1, "Mac Family"), unicode(1, "Unicode Family")))
        assertEquals("Unicode Family", (FontFileValidator.inspect(unicodeAndMac) as FontInspection.Font).familyName)

        val macOnly = SyntheticFonts.build(names = listOf(macRoman(1, "Mac Family")))
        assertEquals("Mac Family", (FontFileValidator.inspect(macOnly) as FontInspection.Font).familyName)

        val windowsGerman = SyntheticFonts.build(
            names = listOf(macRoman(1, "Mac Family"), windows(1, "Deutsche Familie", language = 0x0407), unicode(1, "Unicode Family")),
        )
        assertEquals("Deutsche Familie", (FontFileValidator.inspect(windowsGerman) as FontInspection.Font).familyName)

        val unsupportedRecords = SyntheticFonts.build(names = listOf(windows(4, "Full Name"), SyntheticFonts.NameRecord(7, 0, 0, 1, "Custom")))
        assertNull((FontFileValidator.inspect(unsupportedRecords) as FontInspection.Font).familyName)
    }

    @Test
    fun openTypeCollectionsAndForeignFilesAreToldApart() {
        val openType = SyntheticFonts.build(signature = SyntheticFonts.OPEN_TYPE, names = listOf(windows(1, "Test Serif")))
        assertEquals(FontInspection.Font(FontFormat.OPEN_TYPE, "Test Serif"), FontFileValidator.inspect(openType))

        val apple = SyntheticFonts.build(signature = 0x74727565, names = listOf(windows(1, "Apple")))
        assertEquals(FontInspection.Font(FontFormat.TRUE_TYPE, "Apple"), FontFileValidator.inspect(apple))

        assertEquals(FontInspection.Rejected.Collection, FontFileValidator.inspect(SyntheticFonts.build(signature = SyntheticFonts.COLLECTION)))
        assertEquals(FontInspection.Rejected.NotAFont, FontFileValidator.inspect("hello world, not a font".toByteArray()))
        assertEquals(FontInspection.Rejected.NotAFont, FontFileValidator.inspect(ByteArray(2)))
        assertEquals(FontInspection.Rejected.NotAFont, FontFileValidator.inspect(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0, 0)))
        assertEquals(FontInspection.Rejected.NotAFont, FontFileValidator.inspect(byteArrayOf(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)))
    }

    @Test
    fun aFontWithoutANameTableIsStillAccepted() {
        assertEquals(FontInspection.Font(FontFormat.TRUE_TYPE, null), FontFileValidator.inspect(SyntheticFonts.build(names = null)))
    }

    @Test
    fun truncatedFilesAreRejected() {
        val font = SyntheticFonts.build(names = listOf(windows(1, "Test Sans")))

        assertEquals(FontInspection.Rejected.Truncated, FontFileValidator.inspect(font.copyOf(8)))
        assertEquals(FontInspection.Rejected.Truncated, FontFileValidator.inspect(font.copyOf(20)))
        assertEquals(FontInspection.Rejected.Truncated, FontFileValidator.inspect(font.copyOf(font.size - 10)))

        val tooManyTables = font.copyOf().also { it[4] = 0; it[5] = 200.toByte() }
        assertEquals(FontInspection.Rejected.Truncated, FontFileValidator.inspect(tooManyTables))
    }

    @Test
    fun namesAreSanitized() {
        val messy = SyntheticFonts.build(names = listOf(windows(1, "  Bad\"Name; {x}  Tail  ")))
        assertEquals("BadName x Tail", (FontFileValidator.inspect(messy) as FontInspection.Font).familyName)

        val long = SyntheticFonts.build(names = listOf(windows(1, "N".repeat(80))))
        assertEquals("N".repeat(FontFileValidator.MAX_NAME_LENGTH), (FontFileValidator.inspect(long) as FontInspection.Font).familyName)

        val empty = SyntheticFonts.build(names = listOf(windows(16, "\"\"''"), windows(1, "Fallback")))
        assertEquals("Fallback", (FontFileValidator.inspect(empty) as FontInspection.Font).familyName)

        assertNull(FontFileValidator.sanitize(null))
        assertNull(FontFileValidator.sanitize("   "))
        assertEquals("a b", FontFileValidator.sanitize("a\t\nb"))
        assertEquals("abc", FontFileValidator.sanitize("abcdef", maxLength = 3))
    }
}
