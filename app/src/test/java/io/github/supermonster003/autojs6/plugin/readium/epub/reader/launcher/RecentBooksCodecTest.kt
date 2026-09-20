package io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentBooksCodecTest {

    private val full = RecentBook(
        uri = "content://com.android.externalstorage.documents/document/primary%3ABooks%2Fnovel.epub",
        displayName = "novel.epub",
        addedAt = 1_700_000_000_000L,
        lastReadAt = 1_700_000_100_000L,
        key = "sha256:abc",
        title = "A Novel",
        author = "Someone",
        coverFile = "0123.webp",
        progression = 0.42,
        available = false,
    )

    private val minimal = RecentBook(uri = "content://x/doc/1", displayName = "book.epub", addedAt = 1L, lastReadAt = 1L)

    @Test
    fun roundTripsEveryFieldAndTheOptionalOnes() {
        val decoded = RecentBooksCodec.decode(RecentBooksCodec.encode(listOf(full, minimal)))
        assertEquals(listOf(full, minimal), decoded)
    }

    @Test
    fun theEnvelopeCarriesTheFormatAndOmitsAbsentFields() {
        val root = JSONObject(RecentBooksCodec.encode(listOf(minimal)))
        assertEquals(RecentBooksCodec.FORMAT, root.getInt("format"))
        val item = root.getJSONArray("books").getJSONObject(0)
        assertEquals(setOf("uri", "displayName", "addedAt", "lastReadAt", "available"), item.keys().asSequence().toSet())
        assertTrue(item.getBoolean("available"))
    }

    @Test
    fun corruptTextAndUnknownFormatsDecodeToNull() {
        assertNull(RecentBooksCodec.decode("not json"))
        assertNull(RecentBooksCodec.decode("""{"format": 2, "books": []}"""))
        assertNull(RecentBooksCodec.decode("""{"format": 1}"""))
    }

    @Test
    fun entriesWithoutAUriOrNameAreDroppedAndBadProgressionsIgnored() {
        val text = """{"format": 1, "books": [
            {"uri": "", "displayName": "x.epub", "addedAt": 1, "lastReadAt": 1},
            {"uri": "content://a/1", "displayName": "", "addedAt": 1, "lastReadAt": 1},
            {"uri": "content://a/2", "displayName": "ok.epub", "addedAt": 5, "lastReadAt": 6, "progression": 1.5, "title": null},
            "junk"
        ]}"""
        val decoded = RecentBooksCodec.decode(text)!!
        assertEquals(1, decoded.size)
        assertEquals(RecentBook("content://a/2", "ok.epub", 5L, 6L), decoded.single())
    }
}
