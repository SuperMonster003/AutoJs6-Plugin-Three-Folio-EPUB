package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

/** One row of the flattened table of contents: its nesting depth and the original node. */
internal data class TocRow<T>(val depth: Int, val node: T)

/**
 * Turns a table-of-contents tree into the rows a list can show (roadmap P1.2). Generic over the
 * node type so the logic stays Android-free: Readium `Link` trees are passed with
 * `Link::children`. Depth is capped at [MAX_DEPTH] and the total at [MAX_ENTRIES] so a hostile
 * NCX cannot exhaust memory.
 */
internal object TocFlattener {

    const val MAX_DEPTH = 8
    const val MAX_ENTRIES = 5000

    fun <T> flatten(
        roots: List<T>,
        children: (T) -> List<T>,
        maxDepth: Int = MAX_DEPTH,
        maxEntries: Int = MAX_ENTRIES,
    ): List<TocRow<T>> {
        val rows = ArrayList<TocRow<T>>()
        fun visit(nodes: List<T>, depth: Int) {
            for (node in nodes) {
                if (rows.size >= maxEntries) return
                rows += TocRow(depth, node)
                if (depth + 1 < maxDepth) visit(children(node), depth + 1)
            }
        }
        visit(roots, 0)
        return rows
    }

    /** The resource part of an href: everything before the first `#`. */
    fun resourceHref(href: String): String = href.substringBefore('#')

    /**
     * Index of the row to highlight for [currentHref]: the first row whose resource matches the
     * locator's resource (fragments are ignored because the visible fragment is unknown), or -1.
     */
    fun <T> indexOfHref(rows: List<TocRow<T>>, hrefOf: (T) -> String?, currentHref: String?): Int {
        val current = currentHref?.let(::resourceHref)?.takeIf { it.isNotEmpty() } ?: return -1
        return rows.indexOfFirst { row -> hrefOf(row.node)?.let(::resourceHref) == current }
    }

    /** Title shown for the resource at [currentHref], if any row names it. */
    fun <T> titleForHref(
        rows: List<TocRow<T>>,
        hrefOf: (T) -> String?,
        titleOf: (T) -> String?,
        currentHref: String?,
    ): String? {
        val index = indexOfHref(rows, hrefOf, currentHref)
        return rows.getOrNull(index)?.let { titleOf(it.node) }?.takeIf { it.isNotBlank() }
    }
}
