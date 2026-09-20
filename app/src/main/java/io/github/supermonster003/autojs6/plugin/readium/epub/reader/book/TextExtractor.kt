package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

/** What a block of extracted text is: the XHTML block structure reduced to what the two output formats distinguish. */
internal enum class TextBlockKind { HEADING, BODY, QUOTE, FOOTNOTE, LIST_ITEM, CODE, IMAGE }

/**
 * One block of a resource's content (roadmap P5.2): a paragraph, a heading with its level, a
 * quotation, a footnote, a list item, a preformatted block, or an image with its caption and
 * href. Android-free so JUnit can lock the two renderings.
 */
internal data class TextBlock(
    val kind: TextBlockKind,
    val text: String,
    val level: Int = 0,
    val href: String? = null,
)

/** A window of the rendered text: the characters from [offset], and whether more follow them. */
internal data class TextChunk(val text: String, val offset: Int, val hasMore: Boolean)

/**
 * Renders extracted blocks as plain text or light Markdown (roadmap D30) and cuts the result into
 * the chunks one Binder call carries (appendix B.2). Plain text keeps one block per paragraph,
 * separated by a single line break, with headings unmarked, list items and code bare, and images
 * dropped; Markdown marks headings with `#`, quotations with `> `, list items with `- `, code
 * with fences, images as `![caption](href)`, and separates blocks by a blank line. Emphasis and
 * links are not carried, so the Markdown stays at the block level.
 */
internal object TextExtractor {

    const val MAX_HEADING_LEVEL = 6

    fun render(blocks: List<TextBlock>, markdown: Boolean): String {
        val parts = ArrayList<String>(blocks.size)
        for (block in blocks) {
            val text = block.text.trim()
            when (block.kind) {
                TextBlockKind.IMAGE -> if (markdown) {
                    val href = block.href.orEmpty()
                    if (href.isNotEmpty() || text.isNotEmpty()) parts += "![$text]($href)"
                }
                TextBlockKind.HEADING -> if (text.isNotEmpty()) {
                    parts += if (markdown) "#".repeat(block.level.coerceIn(1, MAX_HEADING_LEVEL)) + " " + text else text
                }
                TextBlockKind.QUOTE -> if (text.isNotEmpty()) {
                    parts += if (markdown) text.lineSequence().joinToString("\n") { "> $it" } else text
                }
                TextBlockKind.LIST_ITEM -> if (text.isNotEmpty()) {
                    parts += if (markdown) "- " + text.lineSequence().joinToString("\n  ") else text
                }
                TextBlockKind.CODE -> {
                    val code = block.text.lines().dropWhile { it.isBlank() }.joinToString("\n").trimEnd()
                    if (code.isNotBlank()) parts += if (markdown) "```\n$code\n```" else code
                }
                TextBlockKind.BODY, TextBlockKind.FOOTNOTE -> if (text.isNotEmpty()) parts += text
            }
        }
        return parts.joinToString(if (markdown) "\n\n" else "\n")
    }

    /**
     * The characters of [text] from [offset], at most [maxChars] of them, never cut inside a
     * surrogate pair. An offset at or beyond the end yields an empty chunk without more.
     */
    fun slice(text: String, offset: Int, maxChars: Int): TextChunk {
        require(offset >= 0) { "offset must not be negative" }
        require(maxChars > 0) { "maxChars must be positive" }
        if (offset >= text.length) return TextChunk("", offset, hasMore = false)
        var end = minOf(text.length.toLong(), offset.toLong() + maxChars).toInt()
        if (end < text.length && text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) {
            end = if (end - 1 > offset) end - 1 else end + 1
        }
        return TextChunk(text.substring(offset, end), offset, hasMore = end < text.length)
    }
}
