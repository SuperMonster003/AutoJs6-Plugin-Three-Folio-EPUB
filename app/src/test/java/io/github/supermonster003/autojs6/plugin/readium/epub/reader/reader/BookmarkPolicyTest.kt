package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.Bookmark
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkPolicyTest {

    private fun locator(href: String, progression: Double?, position: Int?): JSONObject = JSONObject().apply {
        put("href", href)
        put("type", "application/xhtml+xml")
        put("locations", JSONObject().apply {
            progression?.let { put("progression", it) }
            position?.let { put("position", it) }
        })
    }

    private fun bookmark(id: Long, href: String, progression: Double? = 0.0, position: Int? = null, createdAt: Long = id) =
        Bookmark(id, locator(href, progression, position), createdAt)

    private fun paginated(href: String, progression: Double?, pageIndex: Int?, totalPages: Int?) =
        PageLocation(href, progression, null, pageIndex, totalPages, scroll = false)

    @Test
    fun pageOfFollowsReadiumsRounding() {
        assertEquals(0, BookmarkPolicy.pageOf(0.0, 10))
        assertEquals(3, BookmarkPolicy.pageOf(0.25, 10)) // 2.5 rounds up, as roundToInt does
        assertEquals(5, BookmarkPolicy.pageOf(0.5, 10))
        assertEquals(9, BookmarkPolicy.pageOf(0.94, 10))
        assertEquals(9, BookmarkPolicy.pageOf(1.0, 10)) // clamped to the last page
        assertEquals(0, BookmarkPolicy.pageOf(-0.5, 10))
        assertEquals(0, BookmarkPolicy.pageOf(0.7, 1))
        assertEquals(0, BookmarkPolicy.pageOf(0.7, 0))
    }

    @Test
    fun paginatedPagesMatchByTheirPageOfTheCurrentLayout() {
        val bookmark = bookmark(1, "c1.xhtml", progression = 0.42)

        assertTrue(BookmarkPolicy.onPage(bookmark, paginated("c1.xhtml", 0.4, pageIndex = 4, totalPages = 10)))
        assertFalse(BookmarkPolicy.onPage(bookmark, paginated("c1.xhtml", 0.5, pageIndex = 5, totalPages = 10)))
        // A different font size gives another page count; the comparison is made in that layout.
        assertTrue(BookmarkPolicy.onPage(bookmark, paginated("c1.xhtml", 0.4, pageIndex = 8, totalPages = 20)))
        assertFalse(BookmarkPolicy.onPage(bookmark, paginated("c1.xhtml", 0.45, pageIndex = 9, totalPages = 20)))
        // Without a page index the page is derived from the progression.
        assertTrue(BookmarkPolicy.onPage(bookmark, paginated("c1.xhtml", 0.44, pageIndex = null, totalPages = 10)))
        assertFalse(BookmarkPolicy.onPage(bookmark, paginated("c2.xhtml", 0.42, pageIndex = 4, totalPages = 10)))
        assertFalse(BookmarkPolicy.onPage(bookmark(2, "c1.xhtml", progression = null), paginated("c1.xhtml", 0.42, 4, 10)))
        assertTrue(BookmarkPolicy.onPage(bookmark(2, "c1.xhtml", progression = null), paginated("c1.xhtml", 0.0, 0, 10)))
    }

    @Test
    fun singlePageResourcesAndFixedLayoutPagesMatchByResource() {
        val bookmark = bookmark(1, "page3.xhtml", progression = 0.0)
        assertTrue(BookmarkPolicy.onPage(bookmark, PageLocation("page3.xhtml", 0.0, 3, null, null, scroll = false)))
        assertTrue(BookmarkPolicy.onPage(bookmark, paginated("page3.xhtml", 0.9, pageIndex = 0, totalPages = 1)))
        assertFalse(BookmarkPolicy.onPage(bookmark, PageLocation("page4.xhtml", 0.0, 4, null, null, scroll = false)))
    }

    @Test
    fun scrollModeMatchesByPosition() {
        val bookmark = bookmark(1, "c1.xhtml", progression = 0.3, position = 12)
        assertTrue(BookmarkPolicy.onPage(bookmark, PageLocation("c1.xhtml", 0.31, 12, 0, 1, scroll = true)))
        assertFalse(BookmarkPolicy.onPage(bookmark, PageLocation("c1.xhtml", 0.3, 13, 0, 1, scroll = true)))
        assertFalse(BookmarkPolicy.onPage(bookmark, PageLocation("c1.xhtml", 0.3, null, 0, 1, scroll = true)))
        assertFalse(BookmarkPolicy.onPage(bookmark(2, "c1.xhtml", 0.3, position = null), PageLocation("c1.xhtml", 0.3, 12, 0, 1, scroll = true)))
    }

    @Test
    fun currentPicksTheFirstBookmarkOfThePage() {
        val bookmarks = listOf(
            bookmark(3, "c1.xhtml", progression = 0.41, createdAt = 30L),
            bookmark(1, "c1.xhtml", progression = 0.42, createdAt = 10L),
            bookmark(2, "c2.xhtml", progression = 0.42, createdAt = 20L),
        )
        val page = paginated("c1.xhtml", 0.4, pageIndex = 4, totalPages = 10)

        assertEquals(3L, BookmarkPolicy.current(bookmarks, page)?.id)
        assertNull(BookmarkPolicy.current(bookmarks, null))
        assertNull(BookmarkPolicy.current(emptyList(), page))
        assertNull(BookmarkPolicy.current(bookmarks, paginated("c3.xhtml", 0.4, 4, 10)))
    }

    @Test
    fun listedIsNewestFirstWithStableTies() {
        val bookmarks = listOf(
            bookmark(1, "a.xhtml", createdAt = 100L),
            bookmark(2, "b.xhtml", createdAt = 300L),
            bookmark(3, "c.xhtml", createdAt = 200L),
            bookmark(4, "d.xhtml", createdAt = 300L),
        )
        assertEquals(listOf(4L, 2L, 3L, 1L), BookmarkPolicy.listed(bookmarks).map { it.id })
    }

    @Test
    fun composeLocatorAddsTheAnchorAndTheQuoteWithoutTouchingTheOriginal() {
        val current = locator("c1.xhtml", 0.42, 7).put("title", "Chapter 1").put("text", JSONObject().put("before", "old"))

        val composed = BookmarkPolicy.composeLocator(current, "#p12 > span:nth-child(2)", "  A   long\n\tquote  ")

        assertEquals("c1.xhtml", composed.getString("href"))
        assertEquals("Chapter 1", composed.getString("title"))
        assertEquals(0.42, composed.getJSONObject("locations").getDouble("progression"), 0.0)
        assertEquals(7, composed.getJSONObject("locations").getInt("position"))
        assertEquals("#p12 > span:nth-child(2)", composed.getJSONObject("locations").getString("cssSelector"))
        assertEquals("A long quote", composed.getJSONObject("text").getString("highlight"))
        assertFalse(composed.getJSONObject("text").has("before"))
        assertFalse(current.getJSONObject("locations").has("cssSelector"))
        assertEquals("old", current.getJSONObject("text").getString("before"))
    }

    @Test
    fun composeLocatorWithoutAnElementKeepsThePageAndDropsTheText() {
        val current = locator("c1.xhtml", 0.42, 7).put("text", JSONObject().put("highlight", "stale"))

        val composed = BookmarkPolicy.composeLocator(current, null, "   ")

        assertFalse(composed.has("text"))
        assertFalse(composed.getJSONObject("locations").has("cssSelector"))
        assertEquals(0.42, composed.getJSONObject("locations").getDouble("progression"), 0.0)

        val long = BookmarkPolicy.composeLocator(current, "", "x".repeat(500))
        assertEquals(BookmarkPolicy.HIGHLIGHT_LENGTH, long.getJSONObject("text").getString("highlight").length)
        assertFalse(long.getJSONObject("locations").has("cssSelector"))

        val bare = BookmarkPolicy.composeLocator(JSONObject().put("href", "x.xhtml").put("type", "text/html"), "#a", "q")
        assertEquals("#a", bare.getJSONObject("locations").getString("cssSelector"))
    }

    @Test
    fun snippetsCollapseWhitespaceAndCutAtWordBoundaries() {
        assertNull(BookmarkPolicy.snippet(null))
        assertNull(BookmarkPolicy.snippet("  \n\t "))
        assertEquals("A short one", BookmarkPolicy.snippet("  A  short\n one "))
        val words = (1..40).joinToString(" ") { "word$it" }
        val cut = requireNotNull(BookmarkPolicy.snippet(words, limit = 30))
        assertTrue(cut, cut.endsWith("..."))
        assertTrue(cut, cut.length <= 30 + 3)
        assertEquals("word1 word2 word3 word4 word5...", cut)
        // No space near the limit: cut hard.
        assertEquals("x".repeat(30) + "...", BookmarkPolicy.snippet("x".repeat(80), limit = 30))
        assertEquals(BookmarkPolicy.SNIPPET_LENGTH + 3, BookmarkPolicy.snippet("y".repeat(400))?.length)
    }
}
