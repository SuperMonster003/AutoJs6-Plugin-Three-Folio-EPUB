package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import org.junit.Assert.assertEquals
import org.junit.Test

/** Roadmap P5.2 / D30: the XHTML block walk behind `getText`. */
class HtmlBlockExtractorTest {

    private fun blocks(html: String, href: String = "OEBPS/text/chapter.xhtml"): List<TextBlock> =
        HtmlBlockExtractor.blocks(html.toByteArray(Charsets.UTF_8), href)

    @Test
    fun headingsParagraphsQuotesNotesListsCodeAndImagesKeepTheirKind() {
        val html = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
            <head><title>Chapter</title><style>p { color: red }</style><script>alert(1)</script></head>
            <body>
            <h1 id="chapter-1">Chapter   1</h1>
            <p>The lighthouse keeper counted<br/>the ships.</p>
            <p>A note<a href="#note-1" epub:type="noteref">1</a> and <em>emphasis</em>.</p>
            <blockquote><p>First quoted.</p><p>Second quoted.</p></blockquote>
            <blockquote>Bare quote</blockquote>
            <ul><li>One</li><li>Two <ul><li>Nested</li></ul></li></ul>
            <pre>
            line one
              line two
            </pre>
            <figure><img src="../images/beacon.png" alt="A small beacon glyph"/><figcaption>The beacon</figcaption></figure>
            <p><img src="../images/plain.png" alt="Plain alt"/></p>
            <h2>Second   heading</h2>
            <aside epub:type="footnote" id="note-1" hidden="hidden"><p>The reef is named after nobody.</p></aside>
            <div role="doc-endnote">An endnote.</div>
            <span hidden="hidden">invisible</span>
            <table><tr><th>Ship</th><th>Cargo</th></tr><tr><td>Relief</td><td>Bread</td></tr></table>
            <section><div><p>Deep&nbsp;paragraph &amp; entity &eacute;.</p></div></section>
            <p>   </p>
            </body></html>
        """.trimIndent()

        assertEquals(
            listOf(
                TextBlock(TextBlockKind.HEADING, "Chapter 1", level = 1),
                TextBlock(TextBlockKind.BODY, "The lighthouse keeper counted\nthe ships."),
                TextBlock(TextBlockKind.BODY, "A note1 and emphasis."),
                TextBlock(TextBlockKind.QUOTE, "First quoted."),
                TextBlock(TextBlockKind.QUOTE, "Second quoted."),
                TextBlock(TextBlockKind.QUOTE, "Bare quote"),
                TextBlock(TextBlockKind.LIST_ITEM, "One"),
                TextBlock(TextBlockKind.LIST_ITEM, "Two"),
                TextBlock(TextBlockKind.LIST_ITEM, "Nested"),
                TextBlock(TextBlockKind.CODE, "line one\n  line two"),
                TextBlock(TextBlockKind.IMAGE, "The beacon", href = "OEBPS/images/beacon.png"),
                TextBlock(TextBlockKind.IMAGE, "Plain alt", href = "OEBPS/images/plain.png"),
                TextBlock(TextBlockKind.HEADING, "Second heading", level = 2),
                TextBlock(TextBlockKind.FOOTNOTE, "The reef is named after nobody."),
                TextBlock(TextBlockKind.FOOTNOTE, "An endnote."),
                TextBlock(TextBlockKind.BODY, "Ship | Cargo"),
                TextBlock(TextBlockKind.BODY, "Relief | Bread"),
                TextBlock(TextBlockKind.BODY, "Deep paragraph & entity é."),
            ),
            blocks(html),
        )
    }

    @Test
    fun bareBodyTextAndUnknownInlineMarkupStillProduceBlocks() {
        assertEquals(
            listOf(TextBlock(TextBlockKind.BODY, "Just text with bold words.")),
            blocks("<html><body>Just text with <b>bold</b> words.</body></html>"),
        )
        assertEquals(emptyList<TextBlock>(), blocks("<html><body><script>1</script></body></html>"))
        assertEquals(emptyList<TextBlock>(), blocks(""))
    }

    @Test
    fun imageHrefsResolveAgainstTheResource() {
        assertEquals("OEBPS/images/a.png", HtmlBlockExtractor.resolve("OEBPS/chapter1.xhtml", "images/a.png"))
        assertEquals("OEBPS/images/a.png", HtmlBlockExtractor.resolve("OEBPS/text/ch.xhtml", "../images/a.png"))
        assertEquals("images/a.png", HtmlBlockExtractor.resolve("chapter1.xhtml", "images/a.png"))
        assertEquals("/images/a.png", HtmlBlockExtractor.resolve("OEBPS/chapter1.xhtml", "/images/a.png"))
        assertEquals("OEBPS/a%20b.png", HtmlBlockExtractor.resolve("OEBPS/chapter1.xhtml", "a%20b.png"))
        assertEquals("OEBPS/a b.png", HtmlBlockExtractor.resolve("OEBPS/chapter1.xhtml", "a b.png"))
        assertEquals("https://example.com/a.png", HtmlBlockExtractor.resolve("OEBPS/chapter1.xhtml", "https://example.com/a.png"))
        assertEquals(
            listOf(TextBlock(TextBlockKind.IMAGE, "", href = "OEBPS/images/x.png")),
            blocks("<html><body><img src=\"images/x.png\"/><img src=\"\"/></body></html>", "OEBPS/ch.xhtml"),
        )
    }

    @Test
    fun theCharsetComesFromTheDocument() {
        val latin = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><html><body><p>café</p></body></html>"
        assertEquals(
            listOf(TextBlock(TextBlockKind.BODY, "café")),
            HtmlBlockExtractor.blocks(latin.toByteArray(Charsets.ISO_8859_1), "ch.xhtml"),
        )
    }
}
