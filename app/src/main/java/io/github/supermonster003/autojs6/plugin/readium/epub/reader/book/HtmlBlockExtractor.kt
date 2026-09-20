package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.io.ByteArrayInputStream
import java.net.URI

/**
 * Walks the DOM of one XHTML resource (roadmap P5.2 / D30) and reduces it to [TextBlock]s in
 * document order: headings keep their level, `blockquote` content becomes quotations, notes
 * (`epub:type` footnote / endnote / rearnote or the matching `role`) become footnotes, list
 * items and `pre` blocks keep their kind, images keep their `figcaption` (or `alt`) and their
 * href resolved against the resource, table rows join their cells. Scripts, styles, media and
 * hidden elements are dropped. Readium's own content iterator tags every text element as
 * `Body`, which is why the Markdown output needs this walk. Android-free so JUnit can lock it.
 */
internal object HtmlBlockExtractor {

    fun blocks(bytes: ByteArray, resourceHref: String): List<TextBlock> {
        val document = Jsoup.parse(ByteArrayInputStream(bytes), null, "")
        val body = document.body() ?: return emptyList()
        val walker = Walker(resourceHref)
        walker.container(body, Kind.BODY, null)
        walker.flush(Kind.BODY)
        return walker.blocks
    }

    /** Resolves an image `src` against the resource it appears in; unparsable values are kept as written. */
    fun resolve(resourceHref: String, src: String): String = try {
        URI(resourceHref).resolve(URI(src)).toString()
    } catch (_: Exception) {
        val directory = resourceHref.substringBeforeLast('/', "")
        if (directory.isEmpty() || src.startsWith("/")) src else "$directory/$src"
    }

    private enum class Kind { BODY, QUOTE, FOOTNOTE, LIST }

    private val HEADINGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
    private val LISTS = setOf("ul", "ol", "menu")
    private val SKIPPED = setOf("script", "style", "template", "head", "title", "noscript", "svg", "math", "iframe", "object", "embed", "video", "audio", "canvas", "map")
    private val NOTE_TYPES = setOf("footnote", "endnote", "rearnote", "note", "footnotes", "endnotes", "rearnotes")
    private val NOTE_ROLES = setOf("doc-footnote", "doc-endnote", "doc-footnotes", "doc-endnotes", "note")

    private class Walker(private val resourceHref: String) {
        val blocks = ArrayList<TextBlock>()
        private val inline = StringBuilder()

        fun container(element: Element, kind: Kind, caption: String?) {
            for (node in element.childNodes()) {
                when (node) {
                    is TextNode -> inline.append(node.wholeText)
                    is Element -> child(node, kind, caption)
                    else -> {}
                }
            }
        }

        private fun child(element: Element, kind: Kind, caption: String?) {
            val tag = element.normalName()
            if (tag in SKIPPED) return
            val note = isNote(element)
            if (!note && element.hasAttr("hidden")) return
            when {
                tag == "br" -> inline.append('\n')
                tag == "img" -> {
                    flush(kind)
                    image(element, caption)
                }
                tag in HEADINGS -> {
                    flush(kind)
                    val text = normalize(element.text())
                    if (text.isNotEmpty()) blocks += TextBlock(TextBlockKind.HEADING, text, level = tag[1] - '0')
                }
                tag == "pre" -> {
                    flush(kind)
                    val code = element.wholeText().lines().dropWhile { it.isBlank() }.joinToString("\n").trimEnd()
                    if (code.isNotBlank()) blocks += TextBlock(TextBlockKind.CODE, code)
                }
                tag == "figure" -> {
                    flush(kind)
                    val figcaption = element.children().firstOrNull { it.normalName() == "figcaption" }
                    val hasImage = element.selectFirst("img") != null
                    val ownCaption = figcaption?.text()?.let(::normalize)?.takeIf { it.isNotEmpty() }
                    for (node in element.childNodes()) {
                        when (node) {
                            is TextNode -> inline.append(node.wholeText)
                            is Element -> if (!(hasImage && node === figcaption)) child(node, kind, ownCaption ?: caption)
                            else -> {}
                        }
                    }
                    flush(kind)
                }
                tag == "blockquote" -> nested(element, Kind.QUOTE, caption, kind)
                note -> nested(element, Kind.FOOTNOTE, caption, kind)
                tag in LISTS || tag == "li" -> nested(element, Kind.LIST, caption, kind)
                tag == "tr" -> {
                    flush(kind)
                    val cells = element.children()
                        .filter { it.normalName() == "td" || it.normalName() == "th" }
                        .map { normalize(it.text()) }
                        .filter { it.isNotEmpty() }
                    if (cells.isNotEmpty()) blocks += TextBlock(blockKind(kind), cells.joinToString(" | "))
                }
                element.isBlock -> nested(element, kind, caption, kind)
                else -> container(element, kind, caption)
            }
        }

        private fun nested(element: Element, inner: Kind, caption: String?, outer: Kind) {
            flush(outer)
            container(element, inner, caption)
            flush(inner)
        }

        private fun image(element: Element, caption: String?) {
            val src = element.attr("src").trim()
            if (src.isEmpty()) return
            val alt = normalize(element.attr("alt"))
            blocks += TextBlock(TextBlockKind.IMAGE, caption ?: alt, href = resolve(resourceHref, src))
        }

        fun flush(kind: Kind) {
            if (inline.isEmpty()) return
            val text = normalize(inline.toString())
            inline.setLength(0)
            if (text.isNotEmpty()) blocks += TextBlock(blockKind(kind), text)
        }

        private fun blockKind(kind: Kind): TextBlockKind = when (kind) {
            Kind.BODY -> TextBlockKind.BODY
            Kind.QUOTE -> TextBlockKind.QUOTE
            Kind.FOOTNOTE -> TextBlockKind.FOOTNOTE
            Kind.LIST -> TextBlockKind.LIST_ITEM
        }

        private fun isNote(element: Element): Boolean {
            val types = element.attr("epub:type").split(WHITESPACE).map { it.lowercase() }
            if (types.any { it in NOTE_TYPES }) return true
            val roles = element.attr("role").split(WHITESPACE).map { it.lowercase() }
            return roles.any { it in NOTE_ROLES }
        }
    }

    private val WHITESPACE = Regex("\\s+")
    private val LINE_WHITESPACE = Regex("[ \\t\\u000B\\u000C\\r\\u00A0\\u2000-\\u200B\\u202F\\u205F\\u3000]+")

    /** Collapses whitespace inside lines, trims them and drops the blank ones; line breaks come from `br` and block ends. */
    private fun normalize(text: String): String = text.split('\n')
        .map { it.replace(LINE_WHITESPACE, " ").trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
}
