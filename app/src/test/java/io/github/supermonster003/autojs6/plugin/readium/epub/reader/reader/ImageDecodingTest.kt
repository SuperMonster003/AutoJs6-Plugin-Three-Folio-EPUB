package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageDecodingTest {

    @Test
    fun imagesThatFitAreNotSampled() {
        assertEquals(1, ImageDecoding.sampleSize(800, 600, 2048))
        assertEquals(1, ImageDecoding.sampleSize(2048, 100, 2048))
    }

    @Test
    fun largerImagesHalveUntilTheLongerSideFits() {
        assertEquals(2, ImageDecoding.sampleSize(4000, 100, 2048))
        assertEquals(4, ImageDecoding.sampleSize(100, 8000, 2048))
        assertEquals(8, ImageDecoding.sampleSize(9000, 9000, 1200))
    }

    @Test
    fun unknownBoundsAreLeftAlone() {
        assertEquals(1, ImageDecoding.sampleSize(0, 0, 2048))
        assertEquals(1, ImageDecoding.sampleSize(-1, 500, 2048))
        assertEquals(1, ImageDecoding.sampleSize(500, 500, 0))
    }
}
