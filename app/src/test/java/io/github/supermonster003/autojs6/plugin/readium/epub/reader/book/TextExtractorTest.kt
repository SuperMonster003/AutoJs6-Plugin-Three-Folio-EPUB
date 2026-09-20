package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P5.2 / D30: the two renderings of extracted blocks and the chunking of one call. */
class TextExtractorTest {

    private val blocks = listOf(
        TextBlock(TextBlockKind.HEADING, "Chapter One", level = 1),
        TextBlock(TextBlockKind.BODY, "The keeper climbed the stairs."),
        TextBlock(TextBlockKind.IMAGE, "A small beacon glyph", href = "images/beacon.png"),
        TextBlock(TextBlockKind.QUOTE, "First line\nsecond line"),
        TextBlock(TextBlockKind.HEADING, "  ", level = 2),
        TextBlock(TextBlockKind.FOOTNOTE, "1. The reef lies east."),
        TextBlock(TextBlockKind.HEADING, "Deep", level = 9),
        TextBlock(TextBlockKind.HEADING, "Shallow", level = 0),
        TextBlock(TextBlockKind.LIST_ITEM, "First item\nwrapped"),
        TextBlock(TextBlockKind.CODE, "\n  let x = 1;\n  x++;\n\n"),
        TextBlock(TextBlockKind.CODE, "   "),
    )

    @Test
    fun plainTextKeepsOneParagraphPerBlockAndDropsImages() {
        val text = TextExtractor.render(blocks, markdown = false)

        assertEquals(
            listOf(
                "Chapter One",
                "The keeper climbed the stairs.",
                "First line\nsecond line",
                "1. The reef lies east.",
                "Deep",
                "Shallow",
                "First item\nwrapped",
                "  let x = 1;\n  x++;",
            ).joinToString("\n"),
            text,
        )
        assertFalse(text.contains("beacon"))
        assertFalse(text.contains("#"))
    }

    @Test
    fun markdownMarksHeadingsQuotesListsCodeAndImagesAndClampsHeadingLevels() {
        val markdown = TextExtractor.render(blocks, markdown = true)

        assertEquals(
            listOf(
                "# Chapter One",
                "The keeper climbed the stairs.",
                "![A small beacon glyph](images/beacon.png)",
                "> First line\n> second line",
                "1. The reef lies east.",
                "###### Deep",
                "# Shallow",
                "- First item\n  wrapped",
                "```\n  let x = 1;\n  x++;\n```",
            ).joinToString("\n\n"),
            markdown,
        )
    }

    @Test
    fun imagesWithoutCaptionOrHrefAreDroppedFromMarkdownToo() {
        assertEquals("", TextExtractor.render(listOf(TextBlock(TextBlockKind.IMAGE, "")), markdown = true))
        assertEquals("![](a.png)", TextExtractor.render(listOf(TextBlock(TextBlockKind.IMAGE, "", href = "a.png")), markdown = true))
        assertEquals("", TextExtractor.render(emptyList(), markdown = false))
    }

    @Test
    fun slicesWalkTheTextWithoutLosingOrRepeatingCharacters() {
        val text = "The lighthouse keeper counted the ships."
        val chunks = ArrayList<TextChunk>()
        var offset = 0
        do {
            val chunk = TextExtractor.slice(text, offset, 7)
            chunks += chunk
            assertEquals(offset, chunk.offset)
            offset += chunk.text.length
        } while (chunk.hasMore)

        assertEquals(text, chunks.joinToString("") { it.text })
        assertTrue(chunks.dropLast(1).all { it.text.length == 7 && it.hasMore })
        assertFalse(chunks.last().hasMore)
    }

    @Test
    fun aSliceAtOrBeyondTheEndIsEmptyWithoutMore() {
        assertEquals(TextChunk("", 5, hasMore = false), TextExtractor.slice("hello", 5, 10))
        assertEquals(TextChunk("", 99, hasMore = false), TextExtractor.slice("hello", 99, 10))
        assertEquals(TextChunk("hello", 0, hasMore = false), TextExtractor.slice("hello", 0, 5))
        assertEquals(TextChunk("hell", 0, hasMore = true), TextExtractor.slice("hello", 0, 4))
        assertEquals(TextChunk("", 0, hasMore = false), TextExtractor.slice("", 0, 4))
    }

    @Test
    fun slicesNeverSplitASurrogatePair() {
        val text = "ab😀cd"

        val short = TextExtractor.slice(text, 0, 3)
        assertEquals("ab", short.text)
        assertTrue(short.hasMore)

        val pair = TextExtractor.slice(text, 2, 1)
        assertEquals("😀", pair.text)
        assertTrue(pair.hasMore)

        val rest = TextExtractor.slice(text, 4, 10)
        assertEquals("cd", rest.text)
        assertFalse(rest.hasMore)
    }

    @Test
    fun sliceRefusesNegativeOffsetsAndEmptyWindows() {
        assertThrows(IllegalArgumentException::class.java) { TextExtractor.slice("hello", -1, 1) }
        assertThrows(IllegalArgumentException::class.java) { TextExtractor.slice("hello", 0, 0) }
    }
}
