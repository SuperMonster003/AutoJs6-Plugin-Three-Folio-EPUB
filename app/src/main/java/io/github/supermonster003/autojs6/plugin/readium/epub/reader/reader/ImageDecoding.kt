package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Decodes book images for the viewer (roadmap P2.7), downsampled to what the screen can show. */
internal object ImageDecoding {

    /** The viewer never zooms, so a longer side beyond the screen's only costs memory. */
    const val MIN_MAX_SIDE = 1024

    /**
     * The power-of-two sample size that brings the longer of [width] and [height] down to at most
     * [maxSide] pixels (1 when the image already fits or the dimensions are unknown).
     */
    fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        if (width <= 0 || height <= 0 || maxSide <= 0) return 1
        var sample = 1
        while (maxOf(width, height) / sample > maxSide) sample *= 2
        return sample
    }

    fun decode(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        }
        return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }.getOrNull()
    }
}
