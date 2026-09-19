package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkPolicyTest {

    @Test
    fun webLinksGoToTheBrowser() {
        assertEquals(ExternalLink.Web("https://example.com/a?b=c#d"), LinkPolicy.classifyExternal("https://example.com/a?b=c#d"))
        assertEquals(ExternalLink.Web("http://example.com"), LinkPolicy.classifyExternal("http://example.com"))
        assertEquals(ExternalLink.Web("HTTPS://EXAMPLE.COM"), LinkPolicy.classifyExternal("HTTPS://EXAMPLE.COM"))
    }

    @Test
    fun mailAndPhoneLinksGoToTheSystem() {
        assertEquals(ExternalLink.System("mailto:someone@example.com"), LinkPolicy.classifyExternal("mailto:someone@example.com"))
        assertEquals(ExternalLink.System("tel:+1234567890"), LinkPolicy.classifyExternal("tel:+1234567890"))
        assertEquals(ExternalLink.System("MailTo:x@y.z"), LinkPolicy.classifyExternal("MailTo:x@y.z"))
    }

    @Test
    fun everyOtherSchemeIsRefused() {
        for (url in listOf(
            "javascript:alert(1)",
            "file:///sdcard/secret.txt",
            "content://media/external/images/1",
            "intent://scan/#Intent;scheme=zxing;end",
            "data:text/html,<script>1</script>",
            "ftp://example.com/file",
            "sms:12345",
            "chapter2.xhtml",
            "",
        )) {
            assertEquals(url, ExternalLink.Rejected(url), LinkPolicy.classifyExternal(url))
        }
    }

    @Test
    fun historyPopsNewestFirstAndForgetsTheOldestPastItsCapacity() {
        val history = LinkHistory<Int>(capacity = 3)
        assertTrue(history.isEmpty())
        assertNull(history.pop())

        history.push(1)
        history.push(2)
        history.push(3)
        history.push(4)
        assertEquals(3, history.size)
        assertEquals(4, history.pop())
        assertEquals(3, history.pop())
        assertEquals(2, history.pop())
        assertNull(history.pop())
        assertTrue(history.isEmpty())

        history.push(5)
        history.clear()
        assertTrue(history.isEmpty())
        assertEquals(LinkHistory.DEFAULT_CAPACITY, 20)
    }
}
