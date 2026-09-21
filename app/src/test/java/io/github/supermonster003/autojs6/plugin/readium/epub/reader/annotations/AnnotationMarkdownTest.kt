package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** Roadmap P9.3: the Markdown export of a book's highlights. */
class AnnotationMarkdownTest {

    private fun locator(href: String, progression: Double): String =
        JSONObject().put("href", href).put("type", "application/xhtml+xml")
            .put("locations", JSONObject().put("progression", progression)).toString()

    private fun annotation(id: Long, href: String, progression: Double, chapter: String?, quote: String?, note: String? = null) =
        BookAnnotation(id = id, bookKey = "k", href = href, locator = locator(href, progression), quote = quote, note = note, chapter = chapter, createdAt = id, updatedAt = id * 10)

    @Test
    fun theDocumentListsChaptersInReadingOrderWithQuotesNotesAndTimes() {
        val text = AnnotationMarkdown.render(
            title = "The\nLighthouse ",
            authors = listOf("Ann Author", "", "# Bob"),
            annotations = listOf(
                annotation(2L, "OEBPS/chapter3.xhtml", 0.5, "Chapter 3", quote = "Second\nline", note = "- a note\nwith two lines"),
                annotation(1L, "OEBPS/chapter1.xhtml", 0.1, "Chapter 1", quote = "  First  ", note = "  "),
                annotation(3L, "OEBPS/chapter1.xhtml", 0.9, "Chapter 1", quote = null, note = "Only a note"),
            ),
            readingOrder = listOf("OEBPS/chapter1.xhtml", "OEBPS/chapter3.xhtml"),
            formatTime = { "t$it" },
        )
        assertEquals(
            """
            |# The Lighthouse
            |
            |Ann Author, \# Bob
            |
            |## Chapter 1
            |
            |> First
            |
            |*t10*
            |
            |Only a note
            |
            |*t30*
            |
            |## Chapter 3
            |
            |> Second
            |> line
            |
            |\- a note
            |with two lines
            |
            |*t20*
            |""".trimMargin(),
            text,
        )
    }

    @Test
    fun anEmptyBookStillHasItsHeading() {
        assertEquals("# highlights\n", AnnotationMarkdown.render(null, emptyList(), emptyList(), emptyList()) { "" })
        assertEquals("# \\#1\n", AnnotationMarkdown.render("#1", emptyList(), emptyList(), emptyList()) { "" })
    }

    @Test
    fun theFileNameIsTheSanitizedTitle() {
        assertEquals("The Lighthouse.md", AnnotationMarkdown.fileName("The Lighthouse"))
        assertEquals("A B C.md", AnnotationMarkdown.fileName(" A/B:C*?\"<>|\u0007 "))
        assertEquals("highlights.md", AnnotationMarkdown.fileName(null))
        assertEquals("highlights.md", AnnotationMarkdown.fileName("..."))
        assertEquals("x".repeat(AnnotationMarkdown.MAX_FILE_NAME_LENGTH) + ".md", AnnotationMarkdown.fileName("x".repeat(200)))
    }
}
