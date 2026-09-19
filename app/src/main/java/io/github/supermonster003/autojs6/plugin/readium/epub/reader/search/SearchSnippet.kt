package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

/** A result row's text: the context before the hit, the hit itself and the context after it. */
internal data class Snippet(val before: String, val highlight: String, val after: String)

/**
 * Trims Readium's search snippets for a list row (roadmap P2.5). Readium hands out up to 200
 * characters on either side of a hit; a row keeps [DEFAULT_CONTEXT] characters per side, cut at a
 * word boundary when one is close, and marks the cut with an ellipsis. Line breaks and runs of
 * whitespace collapse to single spaces so a hit spanning paragraphs still reads as one line.
 */
internal object SearchSnippet {

    const val DEFAULT_CONTEXT = 48
    const val ELLIPSIS = "..."

    /** How far a cut may move towards the hit to land on a word boundary. */
    private const val WORD_SLACK = 16

    private val WHITESPACE = Regex("\\s+")

    fun trim(before: String?, highlight: String?, after: String?, context: Int = DEFAULT_CONTEXT): Snippet =
        Snippet(
            before = head(collapse(before.orEmpty()), context),
            highlight = collapse(highlight.orEmpty()),
            after = tail(collapse(after.orEmpty()), context),
        )

    private fun collapse(text: String): String = text.replace(WHITESPACE, " ")

    /** The last [limit] characters of [text], moved forward to the next space when one is near. */
    private fun head(text: String, limit: Int): String {
        if (text.length <= limit) return text
        var start = text.length - limit
        val space = text.indexOf(' ', start)
        if (space in start until text.length - 1 && space - start <= WORD_SLACK) start = space + 1
        return ELLIPSIS + text.substring(start)
    }

    /** The first [limit] characters of [text], moved back to the previous space when one is near. */
    private fun tail(text: String, limit: Int): String {
        if (text.length <= limit) return text
        var end = limit
        val space = text.lastIndexOf(' ', end)
        if (space > 0 && limit - space <= WORD_SLACK) end = space
        return text.substring(0, end) + ELLIPSIS
    }
}
