package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryPolicyTest {

    @Test
    fun queriesAreTrimmedAndTheirWhitespaceCollapsed() {
        assertEquals("light house", SearchQueryPolicy.normalize("  light \t\n house  "))
        assertEquals("", SearchQueryPolicy.normalize(" \n "))
    }

    @Test
    fun latinQueriesNeedTwoCharacters() {
        assertTrue(SearchQueryPolicy.isTooShort(""))
        assertTrue(SearchQueryPolicy.isTooShort("a"))
        assertTrue(SearchQueryPolicy.isTooShort("é"))
        assertFalse(SearchQueryPolicy.isTooShort("ab"))
        assertFalse(SearchQueryPolicy.isTooShort("a b"))
    }

    @Test
    fun aSingleCjkCharacterIsAWord() {
        assertFalse(SearchQueryPolicy.isTooShort("灯"))
        assertFalse(SearchQueryPolicy.isTooShort("燈"))
        assertFalse(SearchQueryPolicy.isTooShort("あ"))
        assertFalse(SearchQueryPolicy.isTooShort("ア"))
        assertFalse(SearchQueryPolicy.isTooShort("한"))
        assertTrue(SearchQueryPolicy.isTooShort("ا"))
        assertTrue(SearchQueryPolicy.isTooShort("1"))
    }

    @Test
    fun surrogatePairsCountAsOneCharacter() {
        // U+20000 (CJK Extension B) is one ideograph in two UTF-16 units.
        assertFalse(SearchQueryPolicy.isTooShort("𠀀"))
        // An emoji is one character, not a CJK word.
        assertTrue(SearchQueryPolicy.isTooShort("😀"))
    }
}
