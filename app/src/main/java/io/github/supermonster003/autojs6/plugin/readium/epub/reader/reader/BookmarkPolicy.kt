package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.Bookmark
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Where the reader currently is, as far as bookmarks care: the resource, Readium's progression in
 * it, the position (about a thousand characters each), and for paginated reflowable resources
 * the page index and page count the navigator reported for the current layout.
 */
internal data class PageLocation(
    val href: String,
    val progression: Double?,
    val position: Int?,
    val pageIndex: Int?,
    val totalPages: Int?,
    val scroll: Boolean,
)

/**
 * Pure rules of roadmap P2.6: which bookmark belongs to the page on screen (the toolbar icon and
 * the panel's add button follow it), how the bookmark locator is composed, and what the list shows.
 */
internal object BookmarkPolicy {

    /** What the list shows of the first visible element; the locator keeps a longer quote for anchoring. */
    const val SNIPPET_LENGTH = 120

    /** The text quote stored for anchoring the jump ([Locator.text.highlight]); longer quotes match no better. */
    const val HIGHLIGHT_LENGTH = 200

    private const val ELLIPSIS = "..."
    private const val WORD_SLACK = 16

    /**
     * Readium's own mapping of a progression to a page of the current layout
     * (`R2EpubPageFragment.loadLocator`: `(progression * numPages).roundToInt()`).
     */
    fun pageOf(progression: Double, totalPages: Int): Int =
        (progression.coerceIn(0.0, 1.0) * totalPages).roundToInt().coerceIn(0, maxOf(totalPages - 1, 0))

    /**
     * True when [bookmark] marks the page at [page]:
     * - a fixed-layout page (no page count) is the resource itself;
     * - in scroll mode the "page" is the position, the slice of about a thousand characters the
     *   locator carries, because the viewport has no stable page grid;
     * - a paginated resource compares the pages the bookmark's and the current progression map
     *   to, so the match survives a font-size change the way Readium's own jump would.
     */
    fun onPage(bookmark: Bookmark, page: PageLocation): Boolean {
        if (bookmark.href != page.href) return false
        if (page.scroll) return bookmark.position != null && bookmark.position == page.position
        val total = page.totalPages ?: return true
        if (total <= 1) return true
        val current = page.pageIndex ?: pageOf(page.progression ?: 0.0, total)
        return pageOf(bookmark.progression ?: 0.0, total) == current
    }

    /** The bookmark of the page on screen (the oldest when several were added to it), or null. */
    fun current(bookmarks: List<Bookmark>, page: PageLocation?): Bookmark? =
        page?.let { bookmarks.firstOrNull { bookmark -> onPage(bookmark, it) } }

    /** Newest first, ties broken by id so the order is stable. */
    fun listed(bookmarks: List<Bookmark>): List<Bookmark> =
        bookmarks.sortedWith(compareByDescending<Bookmark> { it.createdAtMillis }.thenByDescending { it.id })

    /**
     * The bookmark locator: the navigator's current locator (page progression, position, total
     * progression, chapter title) plus the CSS selector and a text quote of the first visible
     * element when the navigator could tell, so `go()` anchors on that element (Readium tries the
     * text quote before the progression) and falls back to the page progression otherwise.
     */
    fun composeLocator(current: JSONObject, cssSelector: String?, visibleText: String?): JSONObject {
        val locator = JSONObject(current.toString())
        val locations = locator.optJSONObject("locations") ?: JSONObject().also { locator.put("locations", it) }
        if (!cssSelector.isNullOrBlank()) locations.put("cssSelector", cssSelector)
        val quote = visibleText?.let { collapse(it) }?.takeIf { it.isNotEmpty() }?.take(HIGHLIGHT_LENGTH)
        if (quote != null) {
            locator.put("text", JSONObject().put("highlight", quote))
        } else {
            locator.remove("text")
        }
        return locator
    }

    /** The excerpt shown in the list: whitespace collapsed, cut at a word boundary with an ellipsis. */
    fun snippet(text: String?, limit: Int = SNIPPET_LENGTH): String? {
        val collapsed = text?.let { collapse(it) }?.takeIf { it.isNotEmpty() } ?: return null
        if (collapsed.length <= limit) return collapsed
        val cut = collapsed.lastIndexOf(' ', limit).takeIf { it >= limit - WORD_SLACK } ?: limit
        return collapsed.substring(0, cut).trimEnd() + ELLIPSIS
    }

    private fun collapse(text: String): String = text.replace(WHITESPACE, " ").trim()

    private val WHITESPACE = Regex("\\s+")
}
