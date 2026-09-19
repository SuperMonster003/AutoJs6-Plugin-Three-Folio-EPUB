package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

import org.readium.r2.shared.publication.Locator

internal enum class SearchStatus {
    /** No search, or the last one was closed. */
    IDLE,

    /** The query was rejected by [SearchQueryPolicy]. */
    TOO_SHORT,

    /** A page of results is being loaded (the first one or a further one). */
    SEARCHING,

    /** The last page loaded; [SearchState.exhausted] tells whether more can be requested. */
    DONE,

    /** The user stopped the search; the results loaded so far stay. */
    CANCELLED,

    /** Readium reported an error; the results loaded so far stay. */
    FAILED,

    /** The publication has no search service. */
    NOT_SEARCHABLE,
}

/**
 * The search as the panel and the reader see it (roadmap P2.5): the normalized query, the hits
 * loaded so far in reading order, whether more can be loaded, and which hit the reader shows.
 */
internal data class SearchState(
    val query: String = "",
    val status: SearchStatus = SearchStatus.IDLE,
    val results: List<Locator> = emptyList(),
    val exhausted: Boolean = false,
    val truncated: Boolean = false,
    val activeIndex: Int? = null,
) {
    val loading: Boolean get() = status == SearchStatus.SEARCHING

    /** More results can be requested with the next scroll to the end of the list. */
    val canLoadMore: Boolean get() = status == SearchStatus.DONE && !exhausted

    /** The reader is in search mode: a result was opened and the hits are decorated. */
    val active: Boolean get() = activeIndex != null && results.isNotEmpty()
}
