package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

/**
 * Where search hits come from: Readium's iterator yields one batch per resource of the reading
 * order (every hit of that resource), and null once the publication is exhausted. Reading a batch
 * is a suspending call that honours coroutine cancellation.
 */
internal interface SearchSource<T> {
    suspend fun next(): List<T>?
    fun close()
}

/**
 * Paginates a [SearchSource] for the results panel (roadmap P2.5): every [loadMore] appends up
 * to [batchSize] hits, pulling as many source batches as needed and keeping the leftover of a large
 * batch for the next page, and the whole search stops at [limit] hits. A loading call that gets
 * cancelled keeps the hits already appended; [close] ends the search early and releases the source.
 *
 * The pager is single-threaded by contract: it is driven from one coroutine at a time on the
 * caller's dispatcher, which the session guarantees by cancelling the previous load first.
 */
internal class SearchResultPager<T>(
    private val source: SearchSource<T>,
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    val limit: Int = DEFAULT_LIMIT,
) {

    private val loaded = mutableListOf<T>()
    private val pending = ArrayDeque<T>()

    /** Everything loaded so far, in reading order. */
    val items: List<T> get() = loaded.toList()

    /** True once nothing more will come: the source ended, the limit was reached or the pager was closed. */
    var exhausted: Boolean = false
        private set

    /** True when the search stopped at [limit] before the source reported its end. */
    var truncated: Boolean = false
        private set

    var closed: Boolean = false
        private set

    /** Appends the next page and returns the hits it added; empty once [exhausted]. */
    suspend fun loadMore(): List<T> {
        if (exhausted) return emptyList()
        val added = mutableListOf<T>()
        while (added.size < batchSize && loaded.size < limit) {
            if (pending.isEmpty()) {
                val batch = source.next()
                if (batch == null) {
                    finish()
                    break
                }
                pending.addAll(batch)
                continue
            }
            val item = pending.removeFirst()
            loaded.add(item)
            added.add(item)
        }
        if (!exhausted && loaded.size >= limit) {
            truncated = true
            finish()
        }
        return added
    }

    /** Ends the search: nothing more is loaded and the source is released. */
    fun close() {
        if (!closed) finish()
    }

    private fun finish() {
        exhausted = true
        if (!closed) {
            closed = true
            source.close()
        }
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 50
        const val DEFAULT_LIMIT = 500
    }
}
