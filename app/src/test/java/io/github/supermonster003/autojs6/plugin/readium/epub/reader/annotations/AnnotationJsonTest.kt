package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import org.autojs.plugin.epub.api.EpubContract
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

    /** Roadmap P9.4: the document's field names are the contract's (`EpubContract.FIELD_*`, contract version 2). */
    @Test
    fun theFieldNamesAreTheContracts() {
        assertEquals(EpubContract.FIELD_ID, AnnotationJson.FIELD_ID)
        assertEquals(EpubContract.FIELD_STYLE, AnnotationJson.FIELD_STYLE)
        assertEquals(EpubContract.FIELD_COLOR, AnnotationJson.FIELD_COLOR)
        assertEquals(EpubContract.FIELD_NOTE, AnnotationJson.FIELD_NOTE)
        assertEquals(EpubContract.FIELD_QUOTE, AnnotationJson.FIELD_QUOTE)
        assertEquals(EpubContract.FIELD_TITLE, AnnotationJson.FIELD_TITLE)
        assertEquals(EpubContract.FIELD_LOCATOR, AnnotationJson.FIELD_LOCATOR)
        assertEquals(EpubContract.FIELD_CREATED_AT, AnnotationJson.FIELD_CREATED_AT)
        assertEquals(EpubContract.FIELD_UPDATED_AT, AnnotationJson.FIELD_UPDATED_AT)
        assertEquals(EpubContract.STYLE_HIGHLIGHT, AnnotationStyle.HIGHLIGHT)
        assertEquals(EpubContract.STYLE_UNDERLINE, AnnotationStyle.UNDERLINE)
        assertEquals(EpubContract.MAX_ANNOTATIONS, AnnotationPolicy.MAX_PER_BOOK)
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
