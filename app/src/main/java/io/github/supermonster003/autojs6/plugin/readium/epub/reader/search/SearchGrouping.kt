package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

/** A row of the results list: a chapter header or a hit with its index among all hits. */
internal sealed class SearchRow<out T> {
    data class Header(val title: String) : SearchRow<Nothing>()
    data class Hit<T>(val index: Int, val item: T) : SearchRow<T>()
}

/**
 * Groups search hits by chapter for the results panel (roadmap P2.5). Hits arrive in reading
 * order, so a header is inserted whenever the chapter key changes; [titleOf] names the header and
 * falls back to the key (the resource href) when the chapter has no title.
 */
internal object SearchGrouping {

    fun <T> rows(items: List<T>, keyOf: (T) -> String?, titleOf: (T) -> String?): List<SearchRow<T>> {
        val rows = ArrayList<SearchRow<T>>(items.size + 8)
        var lastKey: String? = null
        items.forEachIndexed { index, item ->
            val key = keyOf(item)
            if (index == 0 || key != lastKey) {
                rows.add(SearchRow.Header(titleOf(item)?.takeIf { it.isNotBlank() } ?: key.orEmpty()))
                lastKey = key
            }
            rows.add(SearchRow.Hit(index, item))
        }
        return rows
    }
}
