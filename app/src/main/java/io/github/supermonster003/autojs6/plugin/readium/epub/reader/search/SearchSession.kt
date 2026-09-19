package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchError
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.getOrElse

/**
 * Drives one search at a time over the attached publication (roadmap P2.5): [search] starts a
 * new query (cancelling the previous one), [loadMore] appends the next page, [cancel] stops the
 * running load and keeps what arrived, [close] leaves search mode, and [detach] forgets everything
 * when the book closes. All calls come from the main thread; the loads run on [scope], which is
 * the view model's scope so a search survives rotation but not the book.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class SearchSession(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> get() = _state

    private var publication: Publication? = null
    private var pager: SearchResultPager<Locator>? = null
    private var job: Job? = null

    fun attach(publication: Publication) {
        detach()
        this.publication = publication
    }

    /** The book is closing: cancel, release the iterator and reset the state. */
    fun detach() {
        cancelJob()
        releasePager()
        publication = null
        _state.value = SearchState()
    }

    fun search(rawQuery: String) {
        val query = SearchQueryPolicy.normalize(rawQuery)
        cancelJob()
        releasePager()
        if (SearchQueryPolicy.isTooShort(query)) {
            _state.value = SearchState(query = query, status = SearchStatus.TOO_SHORT)
            return
        }
        val publication = publication
        if (publication == null) {
            _state.value = SearchState(query = query, status = SearchStatus.NOT_SEARCHABLE)
            return
        }
        _state.value = SearchState(query = query, status = SearchStatus.SEARCHING)
        job = scope.launch {
            val iterator = publication.search(query)
            if (iterator == null) {
                _state.update { it.copy(status = SearchStatus.NOT_SEARCHABLE, exhausted = true) }
                return@launch
            }
            val pager = SearchResultPager(ReadiumSource(iterator))
            this@SearchSession.pager = pager
            load(pager)
        }
    }

    /** Requests the next page; ignored while loading, after the end, or without a search. */
    fun loadMore() {
        val pager = pager ?: return
        if (!_state.value.canLoadMore) return
        _state.update { it.copy(status = SearchStatus.SEARCHING) }
        job = scope.launch { load(pager) }
    }

    private suspend fun load(pager: SearchResultPager<Locator>) {
        try {
            pager.loadMore()
            _state.update { it.copy(status = SearchStatus.DONE, results = pager.items, exhausted = pager.exhausted, truncated = pager.truncated) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: SearchFailure) {
            pager.close()
            _state.update { it.copy(status = SearchStatus.FAILED, results = pager.items, exhausted = true) }
        }
    }

    /** Stops a running load; the results that already arrived stay listed. */
    fun cancel() {
        val running = job?.isActive == true
        cancelJob()
        val pager = pager
        pager?.close()
        if (running) {
            _state.update { it.copy(status = SearchStatus.CANCELLED, results = pager?.items ?: it.results, exhausted = true) }
        }
    }

    /** Marks the hit the reader shows; null leaves search mode without dropping the results. */
    fun select(index: Int?) {
        _state.update { state -> state.copy(activeIndex = index?.takeIf { it in state.results.indices }) }
    }

    /** Leaves search mode and drops the results; the query text stays for the next time. */
    fun close() {
        cancelJob()
        releasePager()
        _state.update { SearchState(query = it.query) }
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }

    private fun releasePager() {
        pager?.close()
        pager = null
    }

    /** Readium's iterator as a [SearchSource]: one batch per resource, errors as [SearchFailure]. */
    private class ReadiumSource(private val iterator: SearchIterator) : SearchSource<Locator> {
        override suspend fun next(): List<Locator>? =
            iterator.next().getOrElse { throw SearchFailure(it) }?.locators

        override fun close() = iterator.close()
    }
}

internal class SearchFailure(val error: SearchError) : Exception(error.message)
