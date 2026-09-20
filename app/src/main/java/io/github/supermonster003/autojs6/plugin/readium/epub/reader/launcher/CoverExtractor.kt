package io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher

import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import java.io.ByteArrayOutputStream
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.coverFitting

/** The cover thumbnail of a book (roadmap P4.1): Readium's cover scaled to fit [MAX_SIDE], encoded as WebP. */
internal object CoverExtractor {

    const val MAX_SIDE = 512
    private const val QUALITY = 80

    /** Null when the book declares no cover or the image cannot be decoded. */
    suspend fun extract(publication: Publication): ByteArray? {
        val bitmap = runCatching { publication.coverFitting(Size(MAX_SIDE, MAX_SIDE)) }.getOrNull() ?: return null
        return try {
            encode(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun encode(bitmap: Bitmap): ByteArray? {
        val output = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        return if (bitmap.compress(format, QUALITY, output)) output.toByteArray() else null
    }
}
