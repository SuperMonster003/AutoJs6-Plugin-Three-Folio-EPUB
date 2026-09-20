package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookJson
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.HtmlBlockExtractor
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.PfdResource
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.TextBlock
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.TextExtractor
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.TocFlattener
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchResultPager
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.autojs.plugin.epub.api.IEpubBook
import org.json.JSONArray
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.search.SearchError
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse

/**
 * One open book on the Binder (roadmap P5.2): the `IEpubBook` methods over a Readium
 * [Publication] backed by the host's descriptor. Calls on one book are serialized by [mutex];
 * every Bundle answer either carries its result keys or an error code, and after [release]
 * every call answers `SESSION_CLOSED`. [lastUsedAt] feeds the idle sweep of [BookRegistry].
 */
@OptIn(ExperimentalReadiumApi::class)
internal class EpubBookBinder(
    private val publication: Publication,
    private val resource: PfdResource,
    private val guard: CallerGuard,
    private val onClosed: (EpubBookBinder) -> Unit,
) : IEpubBook.Stub() {

    @Volatile
    var lastUsedAt: Long = SystemClock.elapsedRealtime()
        private set

    @Volatile
    var isClosed: Boolean = false
        private set

    private val mutex = Mutex()
    private var cachedBlocks: Pair<String, List<TextBlock>>? = null

    override fun getMetadata(): Bundle = answer {
        val metadata = BookJson.metadata(publication, publication.positions().size).toString()
        Answers.ok { putString(EpubContract.KEY_METADATA, metadata) }
    }

    override fun getToc(): Bundle = answer {
        val toc = BookJson.toc(publication)
        Answers.ok {
            putString(EpubContract.KEY_TOC, toc.array.toString())
            putBoolean(EpubContract.KEY_HAS_MORE, toc.hasMore)
        }
    }

    override fun getReadingOrder(): Bundle = answer {
        val order = BookJson.readingOrder(publication)
        Answers.ok {
            putString(EpubContract.KEY_READING_ORDER, order.array.toString())
            putBoolean(EpubContract.KEY_HAS_MORE, order.hasMore)
        }
    }

    override fun getText(request: Bundle?): Bundle = answer {
        val req = request ?: Bundle.EMPTY
        val text = Limits.textRequest(
            req.getInt(EpubContract.KEY_OFFSET),
            req.getInt(EpubContract.KEY_MAX_CHARS),
            req.getString(EpubContract.KEY_FORMAT),
        )
        val (index, link) = resolveResource(req)
        val rendered = TextExtractor.render(blocksOf(link), text.markdown)
        val chunk = TextExtractor.slice(rendered, text.offset, text.maxChars)
        Answers.ok {
            putString(EpubContract.KEY_HREF, link.href.toString())
            putInt(EpubContract.KEY_INDEX, index)
            putString(EpubContract.KEY_FORMAT, if (text.markdown) EpubContract.FORMAT_MARKDOWN else EpubContract.FORMAT_TEXT)
            putInt(EpubContract.KEY_OFFSET, chunk.offset)
            putString(EpubContract.KEY_TEXT, chunk.text)
            putBoolean(EpubContract.KEY_HAS_MORE, chunk.hasMore)
        }
    }

    override fun openResource(href: String?): ParcelFileDescriptor {
        guard.check()
        touch()
        try {
            val clean = Limits.href(href)
            return runBlocking {
                mutex.withLock {
                    requireOpen()
                    val url = Url(clean) ?: throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "malformed href: $clean")
                    val link = publication.linkWithHref(url) ?: throw ContractViolation(EpubErrorCodes.RESOURCE_NOT_FOUND, clean)
                    val target = publication.get(link) ?: throw ContractViolation(EpubErrorCodes.RESOURCE_NOT_FOUND, clean)
                    val length = target.length().getOrElse { error ->
                        target.close()
                        throw ContractViolation(EpubErrorCodes.IO, error.message)
                    }
                    try {
                        Limits.resourceBytes(length)
                    } catch (e: ContractViolation) {
                        target.close()
                        throw e
                    }
                    DescriptorIo.stream(target, length)
                }
            }
        } catch (e: ContractViolation) {
            throw Answers.failure(e)
        }
    }

    override fun search(request: Bundle?): Bundle = answer {
        val req = request ?: Bundle.EMPTY
        val search = Limits.searchRequest(
            req.getString(EpubContract.KEY_QUERY),
            req.getInt(EpubContract.KEY_OFFSET),
            req.getInt(EpubContract.KEY_LIMIT),
        )
        val iterator = publication.search(search.query)
            ?: throw ContractViolation(EpubErrorCodes.INTERNAL, "the publication is not searchable")
        val wanted = search.offset + search.limit
        val pager = SearchResultPager(ReadiumSearchSource(iterator), batchSize = wanted, limit = wanted)
        val hits = try {
            pager.loadMore()
        } finally {
            pager.close()
        }
        val results = JSONArray()
        hits.drop(search.offset).forEach { results.put(BookJson.searchResult(it)) }
        Answers.ok {
            putString(EpubContract.KEY_QUERY, search.query)
            putInt(EpubContract.KEY_OFFSET, search.offset)
            putString(EpubContract.KEY_RESULTS, results.toString())
            putBoolean(EpubContract.KEY_HAS_MORE, pager.truncated && wanted < EpubContract.MAX_SEARCH_RESULTS)
        }
    }

    override fun getPositions(): Bundle = answer {
        val count = publication.positions().size
        Answers.ok { putInt(EpubContract.KEY_POSITIONS, count) }
    }

    override fun close() {
        guard.check()
        release()
    }

    /** Closes the book from either side of the Binder; idempotent. */
    fun release() {
        synchronized(this) {
            if (isClosed) return
            isClosed = true
        }
        onClosed(this)
        runBlocking {
            mutex.withLock {
                runCatching { publication.close() }
                runCatching { resource.close() }
                cachedBlocks = null
            }
        }
    }

    private fun answer(block: suspend () -> Bundle): Bundle {
        guard.check()
        touch()
        return try {
            runBlocking {
                mutex.withLock {
                    requireOpen()
                    block()
                }
            }
        } catch (e: ContractViolation) {
            Answers.error(e)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Throwable) {
            Answers.error(EpubErrorCodes.INTERNAL, e.toString())
        }
    }

    private fun requireOpen() {
        if (isClosed) throw ContractViolation(EpubErrorCodes.SESSION_CLOSED, "the book is closed")
    }

    private fun touch() {
        lastUsedAt = SystemClock.elapsedRealtime()
    }

    private fun resolveResource(request: Bundle): Pair<Int, Link> {
        val order = publication.readingOrder
        val href = request.getString(EpubContract.KEY_HREF)
        if (!href.isNullOrBlank()) {
            val clean = TocFlattener.resourceHref(Limits.href(href))
            val index = order.indexOfFirst { it.href.toString() == clean }
            if (index < 0) throw ContractViolation(EpubErrorCodes.RESOURCE_NOT_FOUND, clean)
            return index to order[index]
        }
        if (!request.containsKey(EpubContract.KEY_INDEX)) {
            throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "href or index is required")
        }
        val index = Limits.readingOrderIndex(request.getInt(EpubContract.KEY_INDEX), order.size)
        return index to order[index]
    }

    /** The blocks of one reading-order resource; non-HTML spine items (fixed-layout images, SVG) have no text. */
    private suspend fun blocksOf(link: Link): List<TextBlock> {
        val key = link.href.toString()
        cachedBlocks?.takeIf { it.first == key }?.let { return it.second }
        val blocks = if (link.mediaType?.isHtml == true) {
            val target = publication.get(link) ?: throw ContractViolation(EpubErrorCodes.RESOURCE_NOT_FOUND, key)
            val bytes = try {
                val length = target.length().getOrElse { error -> throw ContractViolation(EpubErrorCodes.IO, error.message) }
                Limits.resourceBytes(length)
                target.read().getOrElse { error -> throw ContractViolation(EpubErrorCodes.IO, error.message) }
            } finally {
                target.close()
            }
            HtmlBlockExtractor.blocks(bytes, key)
        } else {
            emptyList()
        }
        cachedBlocks = key to blocks
        return blocks
    }

    /** Readium's search iterator as a [SearchSource]; errors become contract violations. */
    private class ReadiumSearchSource(private val iterator: SearchIterator) : SearchSource<Locator> {
        override suspend fun next(): List<Locator>? = iterator.next().getOrElse { error ->
            val code = if (error is SearchError.Reading) EpubErrorCodes.IO else EpubErrorCodes.INTERNAL
            throw ContractViolation(code, error.message)
        }?.locators

        override fun close() = iterator.close()
    }
}
