package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

/** One row of the highlights panel (roadmap P9.2): a chapter header or one annotation. */
internal sealed class AnnotationRow {
    data class Header(val title: String) : AnnotationRow()
    data class Item(val annotation: BookAnnotation) : AnnotationRow()
}

/**
 * Builds the highlights panel's rows: the book's annotations in reading order, with a header in
 * front of every run of the same chapter. Android-free so the grouping is covered by JVM tests.
 */
internal object AnnotationListing {

    /** The title a row shows for [annotation]: its chapter, else the resource's file name. */
    fun titleOf(annotation: BookAnnotation): String =
        annotation.chapter?.takeIf { it.isNotBlank() }
            ?: annotation.href.substringAfterLast('/').ifEmpty { annotation.href }

    fun rows(annotations: List<BookAnnotation>, readingOrder: List<String>): List<AnnotationRow> {
        val rows = ArrayList<AnnotationRow>(annotations.size + 8)
        var last: String? = null
        for (annotation in AnnotationPolicy.ordered(annotations, readingOrder)) {
            val title = titleOf(annotation)
            if (title != last) {
                rows += AnnotationRow.Header(title)
                last = title
            }
            rows += AnnotationRow.Item(annotation)
        }
        return rows
    }

    /** The annotations of [rows] in listed order: what the panel's open / edit / delete act on. */
    fun items(rows: List<AnnotationRow>): List<BookAnnotation> =
        rows.mapNotNull { (it as? AnnotationRow.Item)?.annotation }
}
