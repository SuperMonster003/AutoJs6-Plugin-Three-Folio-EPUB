package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** Roadmap P9.2: the highlights panel groups the book's annotations by chapter in reading order. */
class AnnotationListingTest {

    private fun locator(href: String, progression: Double): String =
        JSONObject().put("href", href).put("type", "application/xhtml+xml")
            .put("locations", JSONObject().put("progression", progression)).toString()

    private fun annotation(id: Long, href: String, progression: Double, chapter: String?, createdAt: Long = id) =
        BookAnnotation(id = id, bookKey = "k", href = href, locator = locator(href, progression), chapter = chapter, createdAt = createdAt)

    @Test
    fun rowsFollowTheReadingOrderWithOneHeaderPerChapterRun() {
        val readingOrder = listOf("OEBPS/cover.xhtml", "OEBPS/chapter1.xhtml", "OEBPS/chapter2.xhtml", "OEBPS/chapter3.xhtml")
        val rows = AnnotationListing.rows(
            listOf(
                annotation(1L, "OEBPS/chapter3.xhtml", 0.5, "Chapter 3"),
                annotation(2L, "OEBPS/chapter1.xhtml", 0.9, "Chapter 1"),
                annotation(3L, "OEBPS/chapter1.xhtml", 0.1, "Chapter 1"),
                annotation(4L, "OEBPS/gone.xhtml", 0.0, null),
                annotation(5L, "OEBPS/chapter1.xhtml", 0.1, "Chapter 1", createdAt = 0L),
            ),
            readingOrder,
        )
        assertEquals(
            listOf(
                AnnotationRow.Header("Chapter 1"),
                AnnotationRow.Item(annotation(5L, "OEBPS/chapter1.xhtml", 0.1, "Chapter 1", createdAt = 0L)),
                AnnotationRow.Item(annotation(3L, "OEBPS/chapter1.xhtml", 0.1, "Chapter 1")),
                AnnotationRow.Item(annotation(2L, "OEBPS/chapter1.xhtml", 0.9, "Chapter 1")),
                AnnotationRow.Header("Chapter 3"),
                AnnotationRow.Item(annotation(1L, "OEBPS/chapter3.xhtml", 0.5, "Chapter 3")),
                AnnotationRow.Header("gone.xhtml"),
                AnnotationRow.Item(annotation(4L, "OEBPS/gone.xhtml", 0.0, null)),
            ),
            rows,
        )
        assertEquals(listOf(5L, 3L, 2L, 1L, 4L), AnnotationListing.items(rows).map { it.id })
        assertEquals(emptyList<AnnotationRow>(), AnnotationListing.rows(emptyList(), readingOrder))
    }

    @Test
    fun theTitleFallsBackToTheFileNameAndThenTheHref() {
        assertEquals("Intro", AnnotationListing.titleOf(annotation(1L, "a/b.xhtml", 0.0, "Intro")))
        assertEquals("b.xhtml", AnnotationListing.titleOf(annotation(1L, "a/b.xhtml", 0.0, "  ")))
        assertEquals("plain", AnnotationListing.titleOf(annotation(1L, "plain", 0.0, null)))
        assertEquals("trailing/", AnnotationListing.titleOf(annotation(1L, "trailing/", 0.0, null)))
    }
}
