package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSnippetTest {

    @Test
    fun shortContextIsKeptAsIs() {
        val snippet = SearchSnippet.trim("The lighthouse ", "keeper", " counted the ships.")

        assertEquals(Snippet("The lighthouse ", "keeper", " counted the ships."), snippet)
    }

    @Test
    fun longContextIsCutAtWordBoundariesWithEllipses() {
        val before = "one two three four five six seven eight nine ten eleven twelve thirteen fourteen "
        val after = " alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu"

        val snippet = SearchSnippet.trim(before, "hit", after, context = 20)

        assertTrue(snippet.before, snippet.before.startsWith(SearchSnippet.ELLIPSIS))
        assertTrue(snippet.before, snippet.before.endsWith("thirteen fourteen "))
        assertTrue(snippet.before, snippet.before.length <= SearchSnippet.ELLIPSIS.length + 20)
        assertTrue(snippet.after, snippet.after.endsWith(SearchSnippet.ELLIPSIS))
        assertEquals(" alpha beta gamma" + SearchSnippet.ELLIPSIS, snippet.after)
    }

    @Test
    fun textWithoutSpacesIsCutAtTheLimit() {
        val before = "一二三四五六七八九十"
        val after = "甲乙丙丁戊己庚辛壬癸"

        val snippet = SearchSnippet.trim(before, "灯塔", after, context = 4)

        assertEquals(SearchSnippet.ELLIPSIS + "七八九十", snippet.before)
        assertEquals("甲乙丙丁" + SearchSnippet.ELLIPSIS, snippet.after)
    }

    @Test
    fun whitespaceRunsCollapseAndNullsBecomeEmpty() {
        val snippet = SearchSnippet.trim("end of\n\n  paragraph ", " the\tword ", null)

        assertEquals(Snippet("end of paragraph ", " the word ", ""), snippet)
    }

    @Test
    fun theDefaultContextIsShortEnoughForARow() {
        assertTrue(SearchSnippet.DEFAULT_CONTEXT in 24..80)
    }
}
