package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import java.io.IOException

/**
 * The two ways out for the Markdown export of a book's highlights (roadmap P9.3): the system
 * share sheet with the text, or a file the user places through the document picker. The
 * intents and the write are built here so the Activity only launches and reports.
 */
internal object AnnotationExport {

    /** `ACTION_SEND` chooser carrying the Markdown as plain text with the book title as subject. */
    fun shareIntent(markdown: String, subject: String?): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType(SelectionActions.MIME_TEXT)
                .putExtra(Intent.EXTRA_TEXT, markdown)
                .apply { if (!subject.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, subject) },
            null,
        )

    /** Writes [markdown] as UTF-8 to the document at [uri], truncating what was there; throws on failure. */
    @Throws(IOException::class)
    fun write(resolver: ContentResolver, uri: Uri, markdown: String) {
        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("no output stream for $uri")
        stream.use { it.write(markdown.toByteArray(Charsets.UTF_8)) }
    }
}
