package io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLiteTest {

    private val sample = """
        ******

        ### Release History

        ******

        # v1.0.0

        ###### 2026/09/19

        * `Feature` A **primary** button for `.epub` files
          - nested detail, see [the docs](https://docs.autojs6.com) now
        * `Fix` plain
        A paragraph line
        continued here.

        Another paragraph.
    """.trimIndent()

    @Test
    fun theChangelogShapeParsesIntoOrderedBlocks() {
        val blocks = MarkdownLite.parse(sample)
        assertEquals(MarkdownBlock.Rule, blocks[0])
        assertEquals(MarkdownBlock.Heading(3, listOf(MarkdownInline.Text("Release History"))), blocks[1])
        assertEquals(MarkdownBlock.Rule, blocks[2])
        assertEquals(MarkdownBlock.Heading(1, listOf(MarkdownInline.Text("v1.0.0"))), blocks[3])
        assertEquals(MarkdownBlock.Heading(6, listOf(MarkdownInline.Text("2026/09/19"))), blocks[4])
        assertEquals(
            MarkdownBlock.Bullet(
                0,
                listOf(
                    MarkdownInline.Code("Feature"), MarkdownInline.Text(" A "), MarkdownInline.Bold("primary"),
                    MarkdownInline.Text(" button for "), MarkdownInline.Code(".epub"), MarkdownInline.Text(" files"),
                ),
            ),
            blocks[5],
        )
        assertEquals(
            MarkdownBlock.Bullet(
                1,
                listOf(MarkdownInline.Text("nested detail, see "), MarkdownInline.Link("the docs", "https://docs.autojs6.com"), MarkdownInline.Text(" now")),
            ),
            blocks[6],
        )
        assertEquals(MarkdownBlock.Bullet(0, listOf(MarkdownInline.Code("Fix"), MarkdownInline.Text(" plain"))), blocks[7])
        assertEquals(MarkdownBlock.Paragraph(listOf(MarkdownInline.Text("A paragraph line continued here."))), blocks[8])
        assertEquals(MarkdownBlock.Paragraph(listOf(MarkdownInline.Text("Another paragraph."))), blocks[9])
        assertEquals(10, blocks.size)
    }

    @Test
    fun unmatchedMarkersAndForeignLinksStayLiteral() {
        assertEquals(listOf(MarkdownInline.Text("a ` b ** c [x](ftp://host) [y](nope)")), MarkdownLite.inlines("a ` b ** c [x](ftp://host) [y](nope)"))
        assertEquals(listOf(MarkdownInline.Text("**")), MarkdownLite.inlines("**"))
        assertEquals(listOf(MarkdownInline.Text("``")), MarkdownLite.inlines("``"))
        assertEquals(listOf(MarkdownInline.Text("2 * 3 * 4")), MarkdownLite.inlines("2 * 3 * 4"))
        assertEquals(listOf(MarkdownBlock.Paragraph(listOf(MarkdownInline.Text("*not a bullet")))), MarkdownLite.parse("*not a bullet"))
        assertEquals(listOf(MarkdownBlock.Paragraph(listOf(MarkdownInline.Text("#hashtag")))), MarkdownLite.parse("#hashtag"))
    }

    @Test
    fun headingsDropTrailingHashesAndRulesComeInThreeFlavours() {
        assertEquals(listOf(MarkdownBlock.Heading(2, listOf(MarkdownInline.Text("Title")))), MarkdownLite.parse("## Title ##"))
        assertEquals(listOf(MarkdownBlock.Rule, MarkdownBlock.Rule, MarkdownBlock.Rule), MarkdownLite.parse("---\n* * *\n___"))
        assertTrue(MarkdownLite.parse("   \n\n").isEmpty())
    }

    @Test
    fun plainTextKeepsTheOrderAndTheNesting() {
        val text = MarkdownLite.plainText(MarkdownLite.parse(sample))
        assertEquals(
            listOf("---", "Release History", "---", "v1.0.0", "2026/09/19", "- Feature A primary button for .epub files",
                "  - nested detail, see the docs now", "- Fix plain", "A paragraph line continued here.", "Another paragraph."),
            text.lines(),
        )
    }
}
