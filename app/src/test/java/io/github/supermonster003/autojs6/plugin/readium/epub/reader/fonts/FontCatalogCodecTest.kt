package io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FontCatalogCodecTest {

    private val shaA = "a".repeat(64)
    private val shaB = "b".repeat(64)
    private val entryA = FontEntry(shaA, "$shaA.ttf", "Test Sans", "Test Sans", 1234, 1_700_000_000_000)
    private val entryB = FontEntry(shaB, "$shaB.otf", "Test Serif", "Test Serif (bbbbbbbb)", 99, 0)

    @Test
    fun aCatalogRoundTripsThroughTheEnvelope() {
        val encoded = FontCatalogCodec.encode(FontCatalog(listOf(entryA, entryB)))

        val json = JSONObject(encoded)
        assertEquals(FontCatalogCodec.FORMAT, json.getInt("format"))
        assertEquals(setOf("format", "fonts"), json.keys().asSequence().toSet())
        assertEquals(FontCatalog(listOf(entryA, entryB)), FontCatalogCodec.decode(encoded))
        assertEquals(FontFormat.OPEN_TYPE, entryB.format)
        assertEquals(FontFormat.TRUE_TYPE, entryA.format)
    }

    @Test
    fun corruptEnvelopesReadAsNullAndAMissingListAsEmpty() {
        assertNull(FontCatalogCodec.decode("{not json"))
        assertNull(FontCatalogCodec.decode("[]"))
        assertNull(FontCatalogCodec.decode("""{"format": 2, "fonts": []}"""))
        assertEquals(FontCatalog.EMPTY, FontCatalogCodec.decode("""{"format": 1}"""))
        assertEquals(FontCatalog.EMPTY, FontCatalogCodec.decode("""{"format": 1, "fonts": [1, "x", null]}"""))
    }

    @Test
    fun invalidAndDuplicateEntriesAreDropped() {
        val valid = entry(entryA)
        val badSha = entry(entryA).put("sha256", "xyz")
        val fileNameMismatch = entry(entryA).put("sha256", shaB)
        val badExtension = entry(entryA).put("fileName", "$shaA.woff")
        val emptyName = entry(entryA).put("displayName", "  ")
        val dirtyFamily = entry(entryA).put("family", "Bad\"Family")
        val zeroBytes = entry(entryA).put("bytes", 0)
        val duplicateSha = entry(entryA).put("family", "Other")
        val duplicateFamily = entry(entryB).put("family", "test sans")
        val fonts = JSONArray().put(badSha).put(fileNameMismatch).put(badExtension).put(emptyName).put(dirtyFamily)
            .put(zeroBytes).put(valid).put(duplicateSha).put(duplicateFamily)

        val decoded = FontCatalogCodec.decode(JSONObject().put("format", 1).put("fonts", fonts).toString())

        assertEquals(FontCatalog(listOf(entryA)), decoded)
    }

    @Test
    fun lookupsIgnoreCaseForFamiliesOnly() {
        val catalog = FontCatalog(listOf(entryA, entryB))

        assertEquals(entryA, catalog.byFamily("TEST SANS"))
        assertEquals(entryB, catalog.bySha256(shaB))
        assertNull(catalog.bySha256(shaB.uppercase()))
        assertNull(catalog.byFamily("Nope"))
        assertTrue(FontCatalog.EMPTY.isEmpty)
    }

    @Test
    fun familyNamesAvoidReservedAndTakenNames() {
        assertEquals("Test Sans", FontFamilyNames.resolve("Test Sans", shaA, emptyList()))
        assertEquals("Serif (aaaaaaaa)", FontFamilyNames.resolve("Serif", shaA, emptyList()))
        assertEquals("OpenDyslexic (aaaaaaaa)", FontFamilyNames.resolve("OpenDyslexic", shaA, emptyList()))
        assertEquals("ia writer duospace (aaaaaaaa)", FontFamilyNames.resolve("ia writer duospace", shaA, emptyList()))
        assertEquals("Test Sans (bbbbbbbb)", FontFamilyNames.resolve("Test Sans", shaB, listOf("test SANS")))
        assertEquals("Test Sans", FontFamilyNames.resolve("Test Sans", shaB, listOf("Test Sans (aaaaaaaa)")))
    }

    private fun entry(source: FontEntry): JSONObject = JSONObject(FontCatalogCodec.encode(FontCatalog(listOf(source))))
        .getJSONArray("fonts").getJSONObject(0)
}
