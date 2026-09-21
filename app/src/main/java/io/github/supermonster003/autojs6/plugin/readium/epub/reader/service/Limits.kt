package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchQueryPolicy
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes

/**
 * A request the contract refuses: the error code of appendix B.4 and a detail for the host. The
 * message is the exception form of the same error, so a call that cannot answer with a Bundle
 * rethrows it as an `IllegalArgumentException` or `IllegalStateException` unchanged.
 */
internal class ContractViolation(val code: String, val detail: String) : RuntimeException(EpubErrorCodes.encode(code, detail))

internal data class TextRequest(val offset: Int, val maxChars: Int, val markdown: Boolean)

internal data class SearchRequest(val query: String, val offset: Int, val limit: Int)

/**
 * Bounds of every Binder input (roadmap P5.2, appendix B.2 / B.5), Android-free so JUnit can
 * cover the edges. Missing integers arrive as 0 from a Bundle, so 0 selects the default where a
 * default exists and is refused where a value is required.
 */
internal object Limits {

    /**
     * Time one `search` page may take on the plugin side; below the host's `CALL_TIMEOUT_MS` so the
     * `TIMEOUT` answer reaches the host before it gives up on the Binder call (roadmap P7.1).
     */
    const val SEARCH_BUDGET_MS = EpubContract.CALL_TIMEOUT_MS - 10_000L

    fun href(raw: String?): String {
        val href = raw?.trim().orEmpty()
        if (href.isEmpty()) throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "href is missing")
        if (href.length > EpubContract.MAX_HREF_LENGTH) {
            throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "href exceeds ${EpubContract.MAX_HREF_LENGTH} characters")
        }
        if (href.any { it < ' ' }) throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "href contains control characters")
        return href
    }

    fun textRequest(offset: Int, maxChars: Int, format: String?): TextRequest {
        if (offset < 0) throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "offset must not be negative")
        if (maxChars < 0) throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "maxChars must not be negative")
        if (maxChars > EpubContract.MAX_TEXT_CHARS_PER_CALL) {
            throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "maxChars exceeds ${EpubContract.MAX_TEXT_CHARS_PER_CALL}")
        }
        val markdown = when (format?.trim()?.takeIf { it.isNotEmpty() }) {
            null, EpubContract.FORMAT_TEXT -> false
            EpubContract.FORMAT_MARKDOWN -> true
            else -> throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "unknown format: $format")
        }
        return TextRequest(offset, if (maxChars == 0) EpubContract.MAX_TEXT_CHARS_PER_CALL else maxChars, markdown)
    }

    fun searchRequest(rawQuery: String?, offset: Int, limit: Int): SearchRequest {
        val query = SearchQueryPolicy.normalize(rawQuery.orEmpty())
        if (query.length < EpubContract.MIN_QUERY_LENGTH) throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "query is empty")
        if (query.length > EpubContract.MAX_QUERY_LENGTH) {
            throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "query exceeds ${EpubContract.MAX_QUERY_LENGTH} characters")
        }
        if (offset < 0) throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "offset must not be negative")
        if (limit < 0) throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "limit must not be negative")
        if (limit > EpubContract.MAX_SEARCH_RESULTS) {
            throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "limit exceeds ${EpubContract.MAX_SEARCH_RESULTS}")
        }
        val pageSize = if (limit == 0) EpubContract.DEFAULT_SEARCH_LIMIT else limit
        if (offset.toLong() + pageSize > EpubContract.MAX_SEARCH_RESULTS) {
            throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "a search never yields more than ${EpubContract.MAX_SEARCH_RESULTS} results")
        }
        return SearchRequest(query, offset, pageSize)
    }

    fun readingOrderIndex(index: Int, size: Int): Int {
        if (index < 0) throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "index must not be negative")
        if (index >= size) throw ContractViolation(EpubErrorCodes.RESOURCE_NOT_FOUND, "reading order has $size entries")
        return index
    }

    fun openBooks(current: Int) {
        if (current >= EpubContract.MAX_OPEN_BOOKS) {
            throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "at most ${EpubContract.MAX_OPEN_BOOKS} books may be open")
        }
    }

    fun optionsBytes(size: Int) {
        if (size > EpubContract.MAX_OPTIONS_BYTES) {
            throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "options exceed ${EpubContract.MAX_OPTIONS_BYTES} bytes")
        }
    }

    fun resourceBytes(length: Long) {
        if (length > EpubContract.MAX_RESOURCE_BYTES) {
            throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "resource exceeds ${EpubContract.MAX_RESOURCE_BYTES} bytes")
        }
    }

    /** Cuts a detail to the contract's message budget, counted in UTF-8 bytes. */
    fun errorDetail(text: String?): String {
        val detail = text?.trim().orEmpty()
        if (detail.toByteArray(Charsets.UTF_8).size <= EpubContract.MAX_ERROR_MESSAGE_BYTES) return detail
        var end = detail.length
        while (end > 0 && detail.substring(0, end).toByteArray(Charsets.UTF_8).size > EpubContract.MAX_ERROR_MESSAGE_BYTES) {
            end -= maxOf(1, (end / 8))
        }
        return detail.substring(0, end)
    }
}
