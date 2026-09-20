package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

/**
 * The `ACTION_VIEW` door (roadmap P4.2 / D27): exported without a permission so that file
 * managers, browsers and mail apps can hand over a `content://` EPUB document. It is the same
 * reader behind the rules of the external entry ([EpubRequestPolicy]): a plain content document
 * with a read grant, never `file://`, never a document tree. The book is not listed in the
 * launcher unless the user picks "add to recent books" in the overflow, which only succeeds when
 * the sender allowed the grant to persist (roadmap D4).
 */
class ExternalViewerActivity : EpubReaderActivity() {

    override fun resolveRequest(): EpubReaderRequest? =
        EpubReaderIntentPolicy.resolve(intent, RequestReceiver.EXTERNAL_VIEWER, queryDisplayName(intent.data))

    /**
     * The name the provider gives the document (`_display_name`), which is what the file manager
     * showed; a media or cloud URI has no meaningful leaf segment. Any failure falls back to the
     * URI's leaf in the policy.
     */
    private fun queryDisplayName(uri: Uri?): String? {
        if (uri == null || !uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true)) return null
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst() && !cursor.isNull(column)) cursor.getString(column) else null
            }
        }.getOrNull()
    }
}
