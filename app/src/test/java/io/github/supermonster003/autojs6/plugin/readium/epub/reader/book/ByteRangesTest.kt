package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ByteRangesTest {

    @Test
    fun nullRangeMeansTheWholeResource() {
        assertEquals(0L to 10, ByteRanges.clamp(null, 10L))
        assertNull(ByteRanges.clamp(null, 0L))
    }

    @Test
    fun rangesAreClampedToTheReadableWindow() {
        assertEquals(2L to 3, ByteRanges.clamp(2L..4L, 10L))
        assertEquals(0L to 5, ByteRanges.clamp(-5L..4L, 10L))
        assertEquals(8L to 2, ByteRanges.clamp(8L..100L, 10L))
        assertEquals(0L to 1, ByteRanges.clamp(0L..0L, 10L))
        assertEquals(9L to 1, ByteRanges.clamp(9L..9L, 10L))
    }

    @Test
    fun emptyOrOutOfWindowRangesReadNothing() {
        assertNull(ByteRanges.clamp(10L..20L, 10L))
        assertNull(ByteRanges.clamp(5L..4L, 10L))
        assertNull(ByteRanges.clamp(LongRange.EMPTY, 10L))
        assertNull(ByteRanges.clamp(0L..5L, 0L))
    }

    @Test
    fun oversizedRequestsAndNegativeLengthsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { ByteRanges.clamp(null, Int.MAX_VALUE.toLong() + 1) }
        assertThrows(IllegalArgumentException::class.java) { ByteRanges.clamp(0L..1L, -1L) }
        assertEquals(0L to Int.MAX_VALUE, ByteRanges.clamp(0L..Long.MAX_VALUE, Int.MAX_VALUE.toLong()))
    }
}
