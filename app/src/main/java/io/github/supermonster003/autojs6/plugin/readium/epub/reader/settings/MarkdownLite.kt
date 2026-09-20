package io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings

/** The inline pieces the release history uses: plain text, `code`, **bold** and [links](https://...). */
internal sealed class MarkdownInline {
    abstract val text: String

    data class Text(override val text: String) : MarkdownInline()
    data class Code(override val text: String) : MarkdownInline()
    data class Bold(override val text: String) : MarkdownInline()
    data class Link(override val text: String, val url: String) : MarkdownInline()
}

/** The block pieces: `#` headings, `-` / `*` bullets (indented ones nest), horizontal rules and paragraphs of joined lines. */
internal sealed class MarkdownBlock {
    abstract val inlines: List<MarkdownInline>

    data class Heading(val level: Int, override val inlines: List<MarkdownInline>) : MarkdownBlock()
    data class Bullet(val depth: Int, override val inlines: List<MarkdownInline>) : MarkdownBlock()
    data class Paragraph(override val inlines: List<MarkdownInline>) : MarkdownBlock()
    data object Rule : MarkdownBlock() {
        override val inlines: List<MarkdownInline> get() = emptyList()
    }
}

/**
 * The small, Android-free markdown reader behind the release history screen (roadmap P4.3): just
 * the constructs the generated CHANGELOG files use. Anything else stays literal text, and every
 * block keeps its order, so the screen shows the file faithfully even when it cannot style a line.
 */
internal object MarkdownLite {

    private val HEADING = Regex("""^(#{1,6})\s+(.*?)\s*#*\s*$""")
    private val BULLET = Regex("""^(\s*)[-*+]\s+(.*)$""")
    private val RULE = Regex("""^\s*([-*_])(?:\s*\1){2,}\s*$""")
    private val LINK = Regex("""\[([^\]]+)]\((https?://[^\s)]+)\)""")
    private const val INDENT_PER_LEVEL = 2

    fun parse(markdown: String): List<MarkdownBlock> {
        val blocks = ArrayList<MarkdownBlock>()
        val paragraph = ArrayList<String>()
        fun flush() {
            if (paragraph.isNotEmpty()) {
                blocks += MarkdownBlock.Paragraph(inlines(paragraph.joinToString(" ")))
                paragraph.clear()
            }
        }
        for (raw in markdown.lineSequence()) {
            val line = raw.trimEnd()
            if (line.isBlank()) {
                flush()
                continue
            }
            val heading = HEADING.matchEntire(line)
            val rule = if (heading == null) RULE.matchEntire(line) else null
            val bullet = if (heading == null && rule == null) BULLET.matchEntire(line) else null
            when {
                heading != null -> {
                    flush()
                    blocks += MarkdownBlock.Heading(heading.groupValues[1].length, inlines(heading.groupValues[2]))
                }
                rule != null -> {
                    flush()
                    blocks += MarkdownBlock.Rule
                }
                bullet != null -> {
                    flush()
                    val depth = bullet.groupValues[1].replace("\t", "    ").length / INDENT_PER_LEVEL
                    blocks += MarkdownBlock.Bullet(depth, inlines(bullet.groupValues[2]))
                }
                else -> paragraph += line.trim()
            }
        }
        flush()
        return blocks
    }

    /** Splits one line into text, code spans, bold runs and links; unmatched markers stay literal. */
    fun inlines(text: String): List<MarkdownInline> {
        val out = ArrayList<MarkdownInline>()
        val plain = StringBuilder()
        fun flushPlain() {
            if (plain.isNotEmpty()) {
                out += MarkdownInline.Text(plain.toString())
                plain.setLength(0)
            }
        }
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '`' -> {
                    val end = text.indexOf('`', index + 1)
                    if (end > index + 1) {
                        flushPlain()
                        out += MarkdownInline.Code(text.substring(index + 1, end))
                        index = end + 1
                        continue
                    }
                }
                char == '*' && text.startsWith("**", index) -> {
                    val end = text.indexOf("**", index + 2)
                    if (end > index + 2) {
                        flushPlain()
                        out += MarkdownInline.Bold(text.substring(index + 2, end))
                        index = end + 2
                        continue
                    }
                }
                char == '[' -> {
                    val match = LINK.matchAt(text, index)
                    if (match != null) {
                        flushPlain()
                        out += MarkdownInline.Link(match.groupValues[1], match.groupValues[2])
                        index = match.range.last + 1
                        continue
                    }
                }
            }
            plain.append(char)
            index++
        }
        flushPlain()
        return out
    }

    /** The text of the blocks without any styling, one line per block (what a screen reader gets). */
    fun plainText(blocks: List<MarkdownBlock>): String = blocks.joinToString("\n") { block ->
        val body = block.inlines.joinToString("") { it.text }
        when (block) {
            is MarkdownBlock.Bullet -> "  ".repeat(block.depth) + "- " + body
            MarkdownBlock.Rule -> "---"
            else -> body
        }
    }
}
