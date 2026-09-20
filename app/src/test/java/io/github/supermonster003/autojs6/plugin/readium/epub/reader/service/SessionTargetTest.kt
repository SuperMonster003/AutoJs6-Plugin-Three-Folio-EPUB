package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class SessionTargetTest {

    private fun codeOf(block: () -> Unit): String {
        try {
            block()
        } catch (e: ContractViolation) {
            return e.code
        }
        fail("expected a ContractViolation")
        throw AssertionError()
    }

    @Test
    fun nothingNamedIsNoTarget() {
        assertNull(SessionTarget.parse(null, null, null))
    }

    @Test
    fun eachShapeParsesOnItsOwnAndTogether() {
        val locator = """{"href":"OEBPS/chapter2.xhtml","type":"application/xhtml+xml","locations":{"progression":0.5}}"""
        val byLocator = SessionTarget.parse(locator, null, null)!!
        assertEquals("OEBPS/chapter2.xhtml", byLocator.locator?.getString("href"))
        assertNull(byLocator.href)
        assertNull(byLocator.progression)
        val byHref = SessionTarget.parse(null, " OEBPS/chapter1.xhtml#ref-1 ", null)!!
        assertEquals("OEBPS/chapter1.xhtml#ref-1", byHref.href)
        val byProgression = SessionTarget.parse(null, null, 0.25)!!
        assertEquals(0.25, byProgression.progression!!, 0.0)
        val all = SessionTarget.parse(locator, "OEBPS/chapter3.xhtml", 1.0)!!
        assertEquals("OEBPS/chapter3.xhtml", all.href)
        assertEquals(1.0, all.progression!!, 0.0)
        assertEquals(0.0, SessionTarget.parse(null, null, 0.0)!!.progression!!, 0.0)
    }

    @Test
    fun malformedShapesCarryTheContractCodes() {
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { SessionTarget.parse("not json", null, null) })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { SessionTarget.parse("""{"type":"x"}""", null, null) })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { SessionTarget.parse("""{"href":""}""", null, null) })
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, codeOf { SessionTarget.parse("""{"href":"${"a".repeat(EpubContract.MAX_LOCATOR_BYTES)}"}""", null, null) })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { SessionTarget.parse(null, "", null) })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { SessionTarget.parse(null, "a\u0000b", null) })
        assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, codeOf { SessionTarget.parse(null, "x".repeat(EpubContract.MAX_HREF_LENGTH + 1), null) })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { SessionTarget.parse(null, null, -0.1) })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { SessionTarget.parse(null, null, 1.5) })
        assertEquals(EpubErrorCodes.INVALID_ARGUMENT, codeOf { SessionTarget.parse(null, null, Double.NaN) })
    }
}
