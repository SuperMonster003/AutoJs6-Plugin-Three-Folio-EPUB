package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import org.autojs.plugin.epub.api.EpubActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostSessionPolicyTest {

    @Test
    fun tokensAreThirtyTwoLowercaseHexDigitsAndNeverRepeat() {
        val tokens = List(200) { HostSessionPolicy.newToken() }
        tokens.forEach { assertTrue(it, HostSessionPolicy.isWellFormed(it)) }
        assertEquals(tokens.size, tokens.toSet().size)
        assertNotEquals(tokens[0], tokens[1])
    }

    @Test
    fun onlyTheReaderActionWithAnExplicitComponentAndAWellFormedTokenIsAccepted() {
        val token = HostSessionPolicy.newToken()
        assertEquals(token, HostSessionPolicy.tokenOf(EpubActions.READER_ACTIVITY_ACTION, explicit = true, token = token))
        assertNull(HostSessionPolicy.tokenOf(EpubActions.READER_ACTIVITY_ACTION, explicit = false, token = token))
        assertNull(HostSessionPolicy.tokenOf("android.intent.action.VIEW", explicit = true, token = token))
        assertNull(HostSessionPolicy.tokenOf(null, explicit = true, token = token))
        assertNull(HostSessionPolicy.tokenOf(EpubActions.READER_ACTIVITY_ACTION, explicit = true, token = null))
        assertNull(HostSessionPolicy.tokenOf(EpubActions.READER_ACTIVITY_ACTION, explicit = true, token = token.uppercase()))
        assertNull(HostSessionPolicy.tokenOf(EpubActions.READER_ACTIVITY_ACTION, explicit = true, token = token.dropLast(1)))
        assertNull(HostSessionPolicy.tokenOf(EpubActions.READER_ACTIVITY_ACTION, explicit = true, token = "$token "))
        assertNull(HostSessionPolicy.tokenOf(EpubActions.READER_ACTIVITY_ACTION, explicit = true, token = ""))
    }

    @Test
    fun tokenComparisonIsExact() {
        val token = HostSessionPolicy.newToken()
        assertTrue(HostSessionPolicy.sameToken(token, token))
        assertFalse(HostSessionPolicy.sameToken(token, HostSessionPolicy.newToken()))
        assertFalse(HostSessionPolicy.sameToken(token.dropLast(1), token))
        assertFalse(HostSessionPolicy.sameToken("", token))
    }
}
