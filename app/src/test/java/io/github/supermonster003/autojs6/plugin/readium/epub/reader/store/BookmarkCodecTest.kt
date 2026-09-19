package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P2.6: the `bookmarks.json` codec (the roadmap calls this `BookmarkStoreCodecTest`). */
class BookmarkCodecTest {

    private fun locator(href: String, progression: Double = 0.25, position: Int? = 3, highlight: String? = null): JSONObject =
        JSONObject().apply {
            put("href", href)
            put("type", "application/xhtml+xml")
            put("title", "Chapter")
            put("locations", JSONObject().apply {
                put("progression", progression)
                position?.let { put("position", it) }
                put("totalProgression", 0.1)
                put("cssSelector", "#p12")
            })
            highlight?.let { put("text", JSONObject().put("highlight", it)) }
        }

    private fun bookmark(id: Long, href: String = "OEBPS/chapter1.xhtml", createdAt: Long = 1000L + id, chapter: String? = "Chapter 1", snippet: String? = "Once upon") =
        Bookmark(id, locator(href, highlight = snippet), createdAt, chapter, snippet)

    @Test
    fun roundTripKeepsEveryFieldAndTheOrder() {
        val original = listOf(bookmark(0), bookmark(1, "OEBPS/chapter2.xhtml", chapter = null, snippet = null), bookmark(7, snippet = "Later on"))

        val decoded = requireNotNull(BookmarkCodec.decode(BookmarkCodec.encode(original)))

        assertEquals(original.size, decoded.size)
        original.zip(decoded).forEach { (expected, actual) -> assertTrue("$expected vs $actual", actual.sameAs(expected)) }
        assertEquals(listOf(0L, 1L, 7L), decoded.map { it.id })
    }

    @Test
    fun theFileFormatIsVersionedAndExplicit() {
        val json = JSONObject(BookmarkCodec.encode(listOf(bookmark(3))))

        assertEquals(BookmarkCodec.FORMAT, json.getInt("format"))
        val entry = json.getJSONArray("bookmarks").getJSONObject(0)
        assertEquals(setOf("id", "locator", "createdAt", "chapter", "snippet"), entry.keys().asSequence().toSet())
        assertEquals("OEBPS/chapter1.xhtml", entry.getJSONObject("locator").getString("href"))
        assertEquals(1003L, entry.getLong("createdAt"))

        val bare = JSONObject(BookmarkCodec.encode(listOf(bookmark(4, chapter = null, snippet = null))))
        assertEquals(setOf("id", "locator", "createdAt"), bare.getJSONArray("bookmarks").getJSONObject(0).keys().asSequence().toSet())
    }

    @Test
    fun anEmptyListEncodesToAnEmptyArray() {
        val decoded = BookmarkCodec.decode(BookmarkCodec.encode(emptyList()))
        assertEquals(emptyList<Bookmark>(), decoded)
    }

    @Test
    fun foreignOrUnreadableTextDecodesToNull() {
        assertNull(BookmarkCodec.decode("{ not json"))
        assertNull(BookmarkCodec.decode(""))
        assertNull(BookmarkCodec.decode("""{"format":2,"bookmarks":[]}"""))
        assertNull(BookmarkCodec.decode("""{"bookmarks":[]}"""))
        assertNull(BookmarkCodec.decode("""{"format":1}"""))
        assertNull(BookmarkCodec.decode("[]"))
    }

    @Test
    fun corruptDuplicateAndIncompleteEntriesAreSkipped() {
        val entries = JSONArray().apply {
            put("not an object")
            put(JSONObject().put("id", 1).put("locator", locator("a.xhtml")).put("createdAt", 10L))
            put(JSONObject().put("id", 1).put("locator", locator("dup.xhtml")).put("createdAt", 11L)) // duplicate id
            put(JSONObject().put("id", -2).put("locator", locator("neg.xhtml")).put("createdAt", 12L)) // negative id
            put(JSONObject().put("id", 3).put("createdAt", 13L)) // no locator
            put(JSONObject().put("id", 4).put("locator", JSONObject().put("type", "text/html")).put("createdAt", 14L)) // no href
            put(JSONObject().put("id", 5).put("locator", JSONObject().put("href", "x.xhtml")).put("createdAt", 15L)) // no type
            put(JSONObject().put("id", 6).put("locator", locator("b.xhtml"))) // no timestamp
            put(JSONObject().put("id", 7).put("locator", locator("c.xhtml")).put("createdAt", 17L).put("chapter", "   ").put("snippet", "x".repeat(1000)))
        }
        val text = JSONObject().put("format", 1).put("bookmarks", entries).toString()

        val decoded = requireNotNull(BookmarkCodec.decode(text))

        assertEquals(listOf(1L, 7L), decoded.map { it.id })
        assertEquals("a.xhtml", decoded[0].href)
        assertNull(decoded[1].chapter)
        assertEquals(BookmarkCodec.MAX_LABEL_LENGTH, decoded[1].snippet?.length)
    }

    @Test
    fun decodingCapsAtTheLimit() {
        val many = (0 until BookmarkCodec.MAX_BOOKMARKS + 25).map { bookmark(it.toLong()) }

        val decoded = requireNotNull(BookmarkCodec.decode(BookmarkCodec.encode(many)))

        assertEquals(BookmarkCodec.MAX_BOOKMARKS, decoded.size)
        assertEquals(0L, decoded.first().id)
        assertEquals(BookmarkCodec.MAX_BOOKMARKS - 1L, decoded.last().id)
    }

    @Test
    fun accessorsReadTheLocator() {
        val bookmark = bookmark(0)
        assertEquals("OEBPS/chapter1.xhtml", bookmark.href)
        assertEquals(0.25, requireNotNull(bookmark.progression), 0.0)
        assertEquals(3, bookmark.position)

        val bare = Bookmark(1, JSONObject().put("href", "x.xhtml").put("type", "text/html"), 5L)
        assertNull(bare.progression)
        assertNull(bare.position)
        assertTrue(bookmark.samePlaceAs(bookmark.copy(id = 99, chapter = "Other", createdAtMillis = 1L)))
        assertTrue(!bookmark.samePlaceAs(bare))
    }

    @Test
    fun mergeKeepsThePrimaryIdsAppendsNewPlacesWithFreshIdsAndSkipsKnownPlaces() {
        val primary = listOf(bookmark(0, "a.xhtml"), bookmark(5, "b.xhtml"))
        val secondary = listOf(
            bookmark(0, "b.xhtml", createdAt = 50L, chapter = "Copy"), // same place as primary id 5
            bookmark(1, "c.xhtml", createdAt = 60L),
            bookmark(2, "d.xhtml", createdAt = 70L),
        )

        val merged = BookmarkCodec.merge(primary, secondary)

        assertEquals(listOf(0L, 5L, 6L, 7L), merged.map { it.id })
        assertEquals(listOf("a.xhtml", "b.xhtml", "c.xhtml", "d.xhtml"), merged.map { it.href })
        assertEquals("Chapter 1", merged[1].chapter)
        assertEquals(60L, merged[2].createdAtMillis)
        assertEquals(primary, BookmarkCodec.merge(primary, emptyList()))
        assertEquals(listOf(0L, 1L, 2L), BookmarkCodec.merge(emptyList(), secondary).map { it.id })
    }

    @Test
    fun mergeKeepsTheNewestWithinTheLimit() {
        val primary = (0 until BookmarkCodec.MAX_BOOKMARKS - 10).map { bookmark(it.toLong(), "p$it.xhtml", createdAt = 1000L + it) }
        val secondary = (0 until 30).map { bookmark(it.toLong(), "s$it.xhtml", createdAt = if (it < 15) 1L else 5000L + it) }

        val merged = BookmarkCodec.merge(primary, secondary)

        assertEquals(BookmarkCodec.MAX_BOOKMARKS, merged.size)
        // The 15 old secondary entries and the 5 oldest primary entries fall away; the rest keep their order.
        assertEquals(primary.drop(5).map { it.href } + (15 until 30).map { "s$it.xhtml" }, merged.map { it.href })
        assertEquals(merged.map { it.id }.toSet().size, merged.size)
    }
}
