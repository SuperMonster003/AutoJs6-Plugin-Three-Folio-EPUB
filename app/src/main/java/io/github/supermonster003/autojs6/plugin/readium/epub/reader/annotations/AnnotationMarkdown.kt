package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

/**
 * The Markdown export of a book's highlights and notes (roadmap P9.3): the book title as the
 * heading, the authors, then one section per chapter in reading order holding each highlight as
 * a block quote, its note as the paragraph after it and the time as an italic line. The text is
 * language-neutral (no labels), so the same file reads the same in every locale; the time
 * formatting is the caller's. Android-free so JVM tests cover it.
 */
internal object AnnotationMarkdown {

    const val MIME_TYPE = "text/markdown"
    const val FILE_EXTENSION = ".md"
    const val FALLBACK_FILE_NAME = "highlights"
    const val MAX_FILE_NAME_LENGTH = 80

    private val LINE_BREAKS = Regex("\\r\\n|\\r|\\n")
    private val FILE_NAME_FORBIDDEN = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]+")
    private val WHITESPACE = Regex("\\s+")

    fun render(
        title: String?,
        authors: List<String>,
        annotations: List<BookAnnotation>,
        readingOrder: List<String>,
        formatTime: (Long) -> String,
    ): String {
        val out = StringBuilder()
        out.append("# ").append(line(title).ifEmpty { FALLBACK_FILE_NAME }).append("\n")
        val byline = authors.map(::line).filter { it.isNotEmpty() }.joinToString(", ")
        if (byline.isNotEmpty()) out.append("\n").append(byline).append("\n")
        for (row in AnnotationListing.rows(annotations, readingOrder)) {
            when (row) {
                is AnnotationRow.Header -> out.append("\n## ").append(line(row.title)).append("\n")
                is AnnotationRow.Item -> appendEntry(out, row.annotation, formatTime)
            }
        }
        return out.toString()
    }

    private fun appendEntry(out: StringBuilder, annotation: BookAnnotation, formatTime: (Long) -> String) {
        val quote = annotation.quote?.trim().orEmpty()
        if (quote.isNotEmpty()) {
            out.append("\n")
            quote.split(LINE_BREAKS).forEach { out.append("> ").append(it).append("\n") }
        }
        val note = annotation.note?.trim().orEmpty()
        if (note.isNotEmpty()) {
            out.append("\n")
            note.split(LINE_BREAKS).forEach { out.append(paragraphLine(it)).append("\n") }
        }
        out.append("\n*").append(line(formatTime(annotation.updatedAt))).append("*\n")
    }

    /** One line of heading or byline text: no line breaks, no leading Markdown marker. */
    private fun line(text: String?): String = paragraphLine(text.orEmpty().replace(LINE_BREAKS, " ").trim())

    /** A line inside a paragraph: a leading `#`, `>`, `-`, `+` or `*` would turn it into structure. */
    private fun paragraphLine(text: String): String =
        if (text.isNotEmpty() && text[0] in MARKERS) "\\" + text else text

    private const val MARKERS = "#>-+*"

    /** A file name for the export: the title with the characters no file system takes replaced, bounded, plus `.md`. */
    fun fileName(title: String?): String {
        val base = title.orEmpty()
            .replace(FILE_NAME_FORBIDDEN, " ")
            .replace(WHITESPACE, " ")
            .trim()
            .trimEnd('.')
            .take(MAX_FILE_NAME_LENGTH)
            .trim()
        return (base.ifEmpty { FALLBACK_FILE_NAME }) + FILE_EXTENSION
    }
}
