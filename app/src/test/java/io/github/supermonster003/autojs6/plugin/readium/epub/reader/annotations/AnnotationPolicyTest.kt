package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P9: the Android-free rules of the highlights and notes. */
class AnnotationPolicyTest {

    private fun locator(href: String = "OEBPS/chapter1.xhtml", progression: Double? = 0.25, highlight: String? = "Once upon a time"): String =
        JSONObject().apply {
            put("href", href)
            put("type", "application/xhtml+xml")
            put("title", "Chapter 1")
            put("locations", JSONObject().apply {
                progression?.let { put("progression", it) }
                put("cssSelector", "#p12")
            })
            highlight?.let { put("text", JSONObject().put("highlight", it).put("before", "...").put("after", "...")) }
        }.toString()

    private fun draft(
        href: String = "OEBPS/chapter1.xhtml",
        progression: Double? = 0.25,
        createdAt: Long = 1000L,
        id: Long = 0L,
        locatorJson: String = locator(href, progression),
        note: String? = null,
        color: Int = AnnotationColors.DEFAULT,
    ) = BookAnnotation(id = id, bookKey = KEY, href = href, locator = locatorJson, color = color, note = note, createdAt = createdAt)

    @Test
    fun normalizeFillsTheHrefTheQuoteAndTheTimestampsAndTrimsTheTexts() {
        val raw = BookAnnotation(
            bookKey = KEY,
            href = "wrong",
            locator = locator(highlight = "  Once   upon\n a time  "),
            style = "bogus",
            color = 0x00FFD54F,
            note = "  a note \n",
            chapter = "  Chapter   1 ",
            createdAt = 0L,
        )
        val normalized = AnnotationPolicy.normalize(raw, now = 5000L)!!
        assertEquals("OEBPS/chapter1.xhtml", normalized.href)
        assertEquals(AnnotationStyle.HIGHLIGHT, normalized.style)
        assertEquals(AnnotationColors.YELLOW, normalized.color)
        assertEquals("a note", normalized.note)
        assertEquals("Once upon a time", normalized.quote)
        assertEquals("Chapter 1", normalized.chapter)
        assertEquals(5000L, normalized.createdAt)
        assertEquals(5000L, normalized.updatedAt)
        assertTrue(normalized.hasNote)
    }

    @Test
    fun normalizeKeepsAnExplicitQuoteAndCreationTimeAndCapsTheLengths() {
        val long = "x".repeat(AnnotationPolicy.MAX_NOTE_LENGTH + 10)
        val raw = draft(createdAt = 42L).copy(note = long, quote = "y".repeat(AnnotationPolicy.MAX_QUOTE_LENGTH + 5), chapter = "c".repeat(500))
        val normalized = AnnotationPolicy.normalize(raw, now = 99L)!!
        assertEquals(42L, normalized.createdAt)
        assertEquals(99L, normalized.updatedAt)
        assertEquals(AnnotationPolicy.MAX_NOTE_LENGTH, normalized.note!!.length)
        assertEquals(AnnotationPolicy.MAX_QUOTE_LENGTH, normalized.quote!!.length)
        assertEquals(AnnotationPolicy.MAX_CHAPTER_LENGTH, normalized.chapter!!.length)
        assertNull(AnnotationPolicy.normalize(raw.copy(note = "   "))!!.note)
    }

    @Test
    fun aLocatorWithoutHrefOrTypeIsNotOne() {
        assertNull(AnnotationPolicy.normalize(draft(locatorJson = "{}")))
        assertNull(AnnotationPolicy.normalize(draft(locatorJson = "not json")))
        assertNull(AnnotationPolicy.normalize(draft(locatorJson = JSONObject().put("href", "a.xhtml").toString())))
        assertNull(AnnotationPolicy.hrefOf("[]"))
        assertEquals("OEBPS/chapter1.xhtml", AnnotationPolicy.hrefOf(locator()))
        assertEquals(0.25, AnnotationPolicy.progressionOf(locator())!!, 0.0)
        assertNull(AnnotationPolicy.progressionOf(locator(progression = null)))
        assertEquals("Once upon a time", AnnotationPolicy.quoteOf(locator()))
        assertNull(AnnotationPolicy.quoteOf(locator(highlight = null)))
    }

    @Test
    fun samePlaceIgnoresIdsColoursAndNotes() {
        val a = draft(id = 1L, note = "one")
        val b = draft(id = 2L, note = "two", color = AnnotationColors.PINK, createdAt = 9L)
        assertTrue(AnnotationPolicy.samePlace(a, b))
        assertFalse(AnnotationPolicy.samePlace(a, draft(progression = 0.5)))
        assertFalse(AnnotationPolicy.samePlace(a, draft(href = "OEBPS/chapter2.xhtml", locatorJson = locator("OEBPS/chapter2.xhtml"))))
    }

    @Test
    fun orderedFollowsTheReadingOrderThenTheProgressionThenTheCreation() {
        val order = listOf("OEBPS/chapter1.xhtml", "OEBPS/chapter2.xhtml", "OEBPS/chapter3.xhtml")
        val c2 = draft(href = "OEBPS/chapter2.xhtml", progression = 0.1, createdAt = 1L, id = 1L)
        val c1Late = draft(progression = 0.9, createdAt = 2L, id = 2L)
        val c1EarlyOld = draft(progression = 0.2, createdAt = 3L, id = 3L)
        val c1EarlyNew = draft(progression = 0.2, createdAt = 4L, id = 4L)
        val gone = draft(href = "OEBPS/zz.xhtml", locatorJson = locator("OEBPS/zz.xhtml"), createdAt = 0L, id = 5L)
        val goneToo = draft(href = "OEBPS/aa.xhtml", locatorJson = locator("OEBPS/aa.xhtml"), createdAt = 0L, id = 6L)
        val ordered = AnnotationPolicy.ordered(listOf(gone, c2, c1Late, goneToo, c1EarlyNew, c1EarlyOld), order)
        assertEquals(listOf(3L, 4L, 2L, 1L, 6L, 5L), ordered.map { it.id })
        assertEquals(listOf(4L, 3L, 2L, 1L), AnnotationPolicy.newestFirst(listOf(c2, c1Late, c1EarlyOld, c1EarlyNew).map { it.copy(updatedAt = it.createdAt) }).map { it.id })
    }

    @Test
    fun mergeableSkipsPlacesThePrimaryHasAndRespectsTheCap() {
        val primary = listOf(draft(id = 1L), draft(id = 2L, progression = 0.5))
        val secondary = listOf(
            draft(id = 10L, note = "dup of 1"),
            draft(id = 11L, progression = 0.75, createdAt = 50L),
            draft(id = 12L, progression = 0.75, createdAt = 60L),
            draft(id = 13L, href = "OEBPS/chapter2.xhtml", locatorJson = locator("OEBPS/chapter2.xhtml"), createdAt = 70L),
        )
        assertEquals(listOf(11L, 13L), AnnotationPolicy.mergeable(primary, secondary).map { it.id })
        assertTrue(AnnotationPolicy.mergeable(primary, emptyList()).isEmpty())
        val full = (1..AnnotationPolicy.MAX_PER_BOOK).map { draft(id = it.toLong(), progression = it / 10000.0) }
        assertTrue(AnnotationPolicy.mergeable(full, listOf(draft(id = 9999L, progression = 0.99999))).isEmpty())
        val nearlyFull = full.dropLast(1)
        val newest = AnnotationPolicy.mergeable(nearlyFull, listOf(draft(id = 8L, progression = 0.9991, createdAt = 1L), draft(id = 9L, progression = 0.9992, createdAt = 2L)))
        assertEquals(listOf(9L), newest.map { it.id })
    }

    @Test
    fun previewCollapsesWhitespaceAndCutsAtAWordBoundary() {
        assertNull(AnnotationPolicy.preview("  \n "))
        assertEquals("a b c", AnnotationPolicy.preview(" a \n b\tc "))
        val words = (1..60).joinToString(" ") { "word$it" }
        val preview = AnnotationPolicy.preview(words, limit = 40)!!
        assertTrue(preview, preview.endsWith("..."))
        assertTrue(preview, preview.length <= 43)
        assertFalse(preview, preview.removeSuffix("...").endsWith(" "))
    }

    @Test
    fun coloursPrintAsHexAndParseBack() {
        assertEquals("#FFD54F", AnnotationColors.hex(AnnotationColors.YELLOW))
        assertEquals(AnnotationColors.YELLOW, AnnotationColors.parseHex("#ffd54f"))
        assertEquals(AnnotationColors.YELLOW, AnnotationColors.parseHex(" FFD54F "))
        assertEquals(0xFF112233.toInt(), AnnotationColors.parseHex("#123"))
        assertNull(AnnotationColors.parseHex("#12345"))
        assertNull(AnnotationColors.parseHex("blue"))
        assertNull(AnnotationColors.parseHex(null))
        assertEquals(0xFF123456.toInt(), AnnotationColors.normalize(0x00123456))
        assertEquals(3, AnnotationColors.indexOf(0x00F48FB1))
        assertEquals(-1, AnnotationColors.indexOf(0xFF000000.toInt()))
        assertEquals(5, AnnotationColors.PALETTE.distinct().size)
        assertEquals(AnnotationStyle.UNDERLINE, AnnotationStyle.normalize("underline"))
        assertEquals(AnnotationStyle.HIGHLIGHT, AnnotationStyle.normalize(null))
        assertEquals(AnnotationStyle.HIGHLIGHT, AnnotationStyle.normalize("Underline"))
    }

    private companion object {
        const val KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
}
