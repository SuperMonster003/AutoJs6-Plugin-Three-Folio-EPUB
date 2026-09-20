package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P5.2, appendix B.2 / B.5: every Binder input is bounds-checked before it reaches Readium. */
class LimitsTest {

    private inline fun violation(block: () -> Unit): ContractViolation {
        try {
            block()
        } catch (e: ContractViolation) {
            return e
        }
        throw AssertionError("expected a contract violation")
    }

    @Test
    fun violationsCarryTheEncodedCodeAsTheirMessage() {
        val violation = ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "too much")

        assertEquals("LIMIT_EXCEEDED: too much", violation.message)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED to "too much", EpubErrorCodes.decode(violation.message.orEmpty()))
    }

    @Test
    fun hrefsAreTrimmedBoundedAndFreeOfControlCharacters() {
        assertEquals("OEBPS/ch1.xhtml", Limits.href("  OEBPS/ch1.xhtml\n"))
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.href(null) }.code)
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.href("   ") }.code)
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.href("a\u0000b") }.code)
        assertEquals("a".repeat(EpubContract.MAX_HREF_LENGTH), Limits.href("a".repeat(EpubContract.MAX_HREF_LENGTH)))
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.href("a".repeat(EpubContract.MAX_HREF_LENGTH + 1)) }.code)
    }

    @Test
    fun textRequestsDefaultToTheCallCeilingAndKnowTheTwoFormats() {
        assertEquals(TextRequest(0, EpubContract.MAX_TEXT_CHARS_PER_CALL, markdown = false), Limits.textRequest(0, 0, null))
        assertEquals(TextRequest(10, 5, markdown = false), Limits.textRequest(10, 5, EpubContract.FORMAT_TEXT))
        assertEquals(TextRequest(10, 5, markdown = true), Limits.textRequest(10, 5, " markdown "))
        assertEquals(TextRequest(0, 5, markdown = false), Limits.textRequest(0, 5, "  "))
        assertEquals(
            TextRequest(0, EpubContract.MAX_TEXT_CHARS_PER_CALL, markdown = false),
            Limits.textRequest(0, EpubContract.MAX_TEXT_CHARS_PER_CALL, null),
        )
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.textRequest(-1, 0, null) }.code)
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.textRequest(0, -1, null) }.code)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.textRequest(0, EpubContract.MAX_TEXT_CHARS_PER_CALL + 1, null) }.code)
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.textRequest(0, 0, "html") }.code)
    }

    @Test
    fun searchRequestsNormalizeTheQueryAndKeepPagesInsideTheResultCeiling() {
        assertEquals(SearchRequest("light house", 0, EpubContract.DEFAULT_SEARCH_LIMIT), Limits.searchRequest("  light \t house ", 0, 0))
        assertEquals(SearchRequest("reef", 20, 10), Limits.searchRequest("reef", 20, 10))
        assertEquals(
            SearchRequest("reef", EpubContract.MAX_SEARCH_RESULTS - 1, 1),
            Limits.searchRequest("reef", EpubContract.MAX_SEARCH_RESULTS - 1, 1),
        )
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.searchRequest(null, 0, 0) }.code)
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.searchRequest(" \n ", 0, 0) }.code)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.searchRequest("q".repeat(EpubContract.MAX_QUERY_LENGTH + 1), 0, 0) }.code)
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.searchRequest("reef", -1, 0) }.code)
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.searchRequest("reef", 0, -1) }.code)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.searchRequest("reef", 0, EpubContract.MAX_SEARCH_RESULTS + 1) }.code)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.searchRequest("reef", EpubContract.MAX_SEARCH_RESULTS, 1) }.code)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.searchRequest("reef", EpubContract.MAX_SEARCH_RESULTS - 10, 0) }.code)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.searchRequest("reef", Int.MAX_VALUE, 1) }.code)
    }

    @Test
    fun readingOrderIndexesMustExist() {
        assertEquals(0, Limits.readingOrderIndex(0, 3))
        assertEquals(2, Limits.readingOrderIndex(2, 3))
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, violation { Limits.readingOrderIndex(-1, 3) }.code)
        assertEquals(EpubErrorCodes.RESOURCE_NOT_FOUND, violation { Limits.readingOrderIndex(3, 3) }.code)
        assertEquals(EpubErrorCodes.RESOURCE_NOT_FOUND, violation { Limits.readingOrderIndex(0, 0) }.code)
    }

    @Test
    fun byteAndCountCeilingsFollowTheContract() {
        Limits.openBooks(EpubContract.MAX_OPEN_BOOKS - 1)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.openBooks(EpubContract.MAX_OPEN_BOOKS) }.code)

        Limits.optionsBytes(EpubContract.MAX_OPTIONS_BYTES)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.optionsBytes(EpubContract.MAX_OPTIONS_BYTES + 1) }.code)

        Limits.resourceBytes(EpubContract.MAX_RESOURCE_BYTES)
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, violation { Limits.resourceBytes(EpubContract.MAX_RESOURCE_BYTES + 1) }.code)
    }

    @Test
    fun errorDetailsAreCutToTheMessageBudgetInUtf8Bytes() {
        assertEquals("", Limits.errorDetail(null))
        assertEquals("boom", Limits.errorDetail("  boom \n"))

        val ascii = "x".repeat(EpubContract.MAX_ERROR_MESSAGE_BYTES * 3)
        assertTrue(Limits.errorDetail(ascii).toByteArray(Charsets.UTF_8).size <= EpubContract.MAX_ERROR_MESSAGE_BYTES)

        val wide = "书".repeat(EpubContract.MAX_ERROR_MESSAGE_BYTES)
        val cut = Limits.errorDetail(wide)
        assertTrue(cut.toByteArray(Charsets.UTF_8).size <= EpubContract.MAX_ERROR_MESSAGE_BYTES)
        assertTrue(cut.isNotEmpty())
        assertTrue(cut.all { it == '书' })
    }
}
