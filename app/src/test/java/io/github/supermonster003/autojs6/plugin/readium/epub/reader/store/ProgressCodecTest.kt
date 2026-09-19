package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCodecTest {

    private fun locator(href: String = "chapter2.xhtml", progression: Double = 0.25): JSONObject = JSONObject()
        .put("href", href)
        .put("type", "application/xhtml+xml")
        .put("title", "Chapter 2")
        .put("locations", JSONObject().put("progression", progression).put("position", 3).put("totalProgression", 0.4))

    @Test
    fun recordsRoundTripThroughJson() {
        val record = ProgressRecord(locator(), updatedAtMillis = 1_726_700_000_000L, totalProgression = 0.4)

        val decoded = ProgressCodec.decode(ProgressCodec.encode(record))

        assertTrue(record.sameAs(decoded))
        assertEquals("chapter2.xhtml", decoded?.locator?.getString("href"))
        assertEquals(3, decoded?.locator?.getJSONObject("locations")?.getInt("position"))
        assertEquals(0.4, decoded?.totalProgression)
    }

    @Test
    fun aMissingTotalProgressionStaysNull() {
        val decoded = ProgressCodec.decode(ProgressCodec.encode(ProgressRecord(locator(), 5L)))

        assertEquals(5L, decoded?.updatedAtMillis)
        assertNull(decoded?.totalProgression)
    }

    @Test
    fun theEnvelopeCarriesTheFormatVersion() {
        val json = JSONObject(ProgressCodec.encode(ProgressRecord(locator(), 1L, 0.5)))

        assertEquals(ProgressCodec.FORMAT, json.getInt("format"))
        assertEquals(setOf("format", "locator", "updatedAt", "totalProgression"), json.keys().asSequence().toSet())
    }

    @Test
    fun corruptOrForeignRecordsDecodeToNull() {
        assertNull(ProgressCodec.decode(""))
        assertNull(ProgressCodec.decode("not json"))
        assertNull(ProgressCodec.decode("[]"))
        assertNull(ProgressCodec.decode("""{"format":2,"locator":{"href":"a","type":"b"},"updatedAt":1}"""))
        assertNull(ProgressCodec.decode("""{"format":1,"updatedAt":1}"""))
        assertNull(ProgressCodec.decode("""{"format":1,"locator":{"href":"a"},"updatedAt":1}"""))
        assertNull(ProgressCodec.decode("""{"format":1,"locator":{"href":"a","type":"b"}}"""))
        assertNull(ProgressCodec.decode("""{"format":1,"locator":{"href":"a","type":"b"},"updatedAt":-1}"""))
    }

    @Test
    fun outOfRangeTotalProgressionIsDropped() {
        val decoded = ProgressCodec.decode(
            """{"format":1,"locator":{"href":"a","type":"b"},"updatedAt":7,"totalProgression":1.5}""",
        )

        assertEquals(7L, decoded?.updatedAtMillis)
        assertNull(decoded?.totalProgression)
    }
}
