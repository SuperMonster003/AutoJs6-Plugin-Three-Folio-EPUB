package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import java.io.File
import java.io.FileNotFoundException

/**
 * Debug-only read-only provider backing the instrumentation tests: documents live under the
 * cache directory and are served as `content://<authority>/documents/<name>`.
 */
class EpubReaderTestContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("The test provider is read-only")
        val name = uri.lastPathSegment
            ?.takeIf { it.matches(SAFE_FILE_NAME) }
            ?: throw FileNotFoundException("Invalid test document URI")
        val file = fileFor(requireNotNull(context), name)
        if (!file.isFile) throw FileNotFoundException("Missing test document: $name")
        openCount.incrementAndGet()
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = ReadiumEpubReaderPlugin.EPUB_MIME_TYPE

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        // Like a document provider: the display name and size of one document, nothing for a missing one.
        val name = uri.lastPathSegment?.takeIf { it.matches(SAFE_FILE_NAME) } ?: return null
        val file = fileFor(requireNotNull(context), name)
        if (!file.isFile) return null
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns, 1).apply {
            addRow(columns.map { column ->
                when (column) {
                    OpenableColumns.DISPLAY_NAME -> name
                    OpenableColumns.SIZE -> file.length()
                    else -> null
                }
            })
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("The test provider is read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("The test provider is read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("The test provider is read-only")

    companion object {
        const val AUTHORITY = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.test.documents"

        private val SAFE_FILE_NAME = Regex("[A-Za-z0-9._-]+")

        /** Number of descriptors handed out; tests use it to prove the book is opened exactly once. */
        val openCount = java.util.concurrent.atomic.AtomicInteger()

        fun documentUri(name: String): Uri = "content://$AUTHORITY/documents/$name".toUri()

        fun parentUri(): Uri = "content://$AUTHORITY/documents".toUri()

        fun fileFor(context: Context, name: String): File {
            require(name.matches(SAFE_FILE_NAME)) { "Unsafe test file name" }
            val directory = File(context.cacheDir, "epub-reader-test-documents").apply {
                check(mkdirs() || isDirectory) { "Cannot create the test document directory" }
            }
            return File(directory, name)
        }
    }
}
