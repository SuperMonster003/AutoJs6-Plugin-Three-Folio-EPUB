package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ReaderPreferencesJsonTest {

    private fun codeOf(block: () -> Unit): String {
        try {
            block()
        } catch (e: ContractViolation) {
            return e.code
        }
        fail("expected a ContractViolation")
        throw AssertionError()
    }

    @Test
    fun everyContractKeyParsesWithItsType() {
        val parse = ReaderPreferencesJson.parse(
            """{"fontSize":1.5,"fontFamily":" Literata ","lineHeight":1.2,"pageMargins":2,"theme":"sepia","scroll":true,""" +
                """"columnCount":"2","verticalText":false,"textAlign":"justify","hyphens":true,"publisherStyles":false}""",
        )
        assertTrue(parse.unsupported.isEmpty())
        val patch = parse.patch
        assertEquals(EpubContract.PREFERENCES, patch.keys)
        assertEquals(1.5, patch.double(EpubContract.PREFERENCE_FONT_SIZE)!!, 0.0)
        assertEquals("Literata", patch.string(EpubContract.PREFERENCE_FONT_FAMILY))
        assertEquals(1.2, patch.double(EpubContract.PREFERENCE_LINE_HEIGHT)!!, 0.0)
        assertEquals(2.0, patch.double(EpubContract.PREFERENCE_PAGE_MARGINS)!!, 0.0)
        assertEquals("sepia", patch.string(EpubContract.PREFERENCE_THEME))
        assertEquals(true, patch.boolean(EpubContract.PREFERENCE_SCROLL))
        assertEquals("2", patch.string(EpubContract.PREFERENCE_COLUMN_COUNT))
        assertEquals(false, patch.boolean(EpubContract.PREFERENCE_VERTICAL_TEXT))
        assertEquals("justify", patch.string(EpubContract.PREFERENCE_TEXT_ALIGN))
        assertEquals(true, patch.boolean(EpubContract.PREFERENCE_HYPHENS))
        assertEquals(false, patch.boolean(EpubContract.PREFERENCE_PUBLISHER_STYLES))
    }

    @Test
    fun nullResetsAKeyAndAbsentKeysAreLeftAlone() {
        val parse = ReaderPreferencesJson.parse("""{"fontSize":null,"theme":null}""")
        val patch = parse.patch
        assertEquals(setOf("fontSize", "theme"), patch.keys)
        assertTrue(patch.has(EpubContract.PREFERENCE_FONT_SIZE))
        assertNull(patch.double(EpubContract.PREFERENCE_FONT_SIZE))
        assertFalse(patch.has(EpubContract.PREFERENCE_SCROLL))
        assertTrue(ReaderPreferencesJson.parse("{}").patch.isEmpty)
    }

    @Test
    fun unknownKeysAreCollectedNotApplied() {
        val parse = ReaderPreferencesJson.parse("""{"fontSize":1.1,"bogus":1,"wordSpacing":0.2}""")
        assertEquals(listOf("bogus", "wordSpacing"), parse.unsupported)
        assertEquals(setOf("fontSize"), parse.patch.keys)
    }

    @Test
    fun wrongTypesAndOutOfRangeValuesAreInvalidArguments() {
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"fontSize":"big"}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"fontSize":0.4}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"fontSize":3.01}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"lineHeight":2.5}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"pageMargins":-1}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"scroll":"yes"}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"theme":"blue"}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"columnCount":3}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"textAlign":"middle"}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"fontFamily":""}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("""{"fontFamily":"${"x".repeat(121)}"}""") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("[1]") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse("not json") })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { ReaderPreferencesJson.parse(null) })
    }

    @Test
    fun theSizeCeilingIsLimitExceeded() {
        val padding = "x".repeat(EpubContract.MAX_PREFERENCES_BYTES)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, codeOf { ReaderPreferencesJson.parse("""{"note":"$padding"}""") })
    }

    @Test
    fun patchesMergeWithTheLaterOneWinning() {
        val first = ReaderPreferencesJson.parse("""{"fontSize":1.1,"scroll":true}""").patch
        val second = ReaderPreferencesJson.parse("""{"fontSize":1.9,"theme":"dark"}""").patch
        val merged = first.merge(second)
        assertEquals(setOf("fontSize", "scroll", "theme"), merged.keys)
        assertEquals(1.9, merged.double(EpubContract.PREFERENCE_FONT_SIZE)!!, 0.0)
        assertEquals(true, merged.boolean(EpubContract.PREFERENCE_SCROLL))
        assertEquals("dark", merged.string(EpubContract.PREFERENCE_THEME))
        assertTrue(PreferencePatch.EMPTY.merge(PreferencePatch.EMPTY).isEmpty)
    }

    @Test
    fun theAdvancedKeysAreTheOnesThePanelAppliesWithoutPublisherStyles() {
        assertEquals(setOf("lineHeight", "textAlign", "hyphens"), ReaderPreferencesJson.ADVANCED_KEYS)
        assertTrue(EpubContract.PREFERENCES.containsAll(ReaderPreferencesJson.ADVANCED_KEYS))
    }
}
