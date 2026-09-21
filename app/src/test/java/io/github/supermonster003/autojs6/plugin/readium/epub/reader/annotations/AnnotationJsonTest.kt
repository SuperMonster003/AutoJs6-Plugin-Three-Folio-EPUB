package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P9: the contract JSON of an annotation (`book.annotations()` and the `highlight` event). */
class AnnotationJsonTest {

    private val locator = JSONObject()
        .put("href", "OEBPS/chapter1.xhtml")
        .put("type", "application/xhtml+xml")
        .put("locations", JSONObject().put("progression", 0.5))
        .put("text", JSONObject().put("highlight", "quoted"))

    @Test
    fun everyFieldOfAFullAnnotationIsPresent() {
        val annotation = BookAnnotation(
            id = 7L,
            bookKey = "k",
            href = "OEBPS/chapter1.xhtml",
            locator = locator.toString(),
            style = AnnotationStyle.UNDERLINE,
            color = AnnotationColors.BLUE,
            note = "my note",
            quote = "quoted",
            chapter = "Chapter 1",
            createdAt = 100L,
            updatedAt = 200L,
        )
        val json = AnnotationJson.toJson(annotation)
        assertEquals(7L, json.getLong("id"))
        assertEquals("underline", json.getString("style"))
        assertEquals("#64B5F6", json.getString("color"))
        assertEquals("my note", json.getString("note"))
        assertEquals("quoted", json.getString("quote"))
        assertEquals("Chapter 1", json.getString("title"))
        assertEquals("OEBPS/chapter1.xhtml", json.getJSONObject("locator").getString("href"))
        assertEquals(0.5, json.getJSONObject("locator").getJSONObject("locations").getDouble("progression"), 0.0)
        assertEquals(100L, json.getLong("createdAt"))
        assertEquals(200L, json.getLong("updatedAt"))
        assertEquals(setOf("id", "style", "color", "note", "quote", "title", "locator", "createdAt", "updatedAt"), json.keys().asSequence().toSet())
    }

    @Test
    fun optionalFieldsAreLeftOutAndABrokenLocatorBecomesAnEmptyObject() {
        val bare = BookAnnotation(id = 1L, bookKey = "k", href = "a", locator = "{broken", color = AnnotationColors.GREEN, createdAt = 1L)
        val json = AnnotationJson.toJson(bare)
        assertFalse(json.has("note"))
        assertFalse(json.has("quote"))
        assertFalse(json.has("title"))
        assertEquals(0, json.getJSONObject("locator").length())
        assertEquals("#81C784", json.getString("color"))
        val array = AnnotationJson.toJsonArray(listOf(bare, bare.copy(id = 2L)))
        assertEquals(2, array.length())
        assertTrue(array.getJSONObject(1).getLong("id") == 2L)
    }
}
