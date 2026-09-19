package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderProgressTest {

    @Test
    fun positionsAndTotalProgressionProduceTheFullSnapshot() {
        val snapshot = ReaderProgress.snapshot(position = 3, positionCount = 12, totalProgression = 0.254)

        assertEquals(ProgressSnapshot(3, 12, 25), snapshot)
        assertTrue(snapshot.hasPosition)
    }

    @Test
    fun unknownPositionsFallBackToThePercentageOnly() {
        val snapshot = ReaderProgress.snapshot(position = null, positionCount = 0, totalProgression = 0.5)

        assertEquals(ProgressSnapshot(null, 0, 50), snapshot)
        assertFalse(snapshot.hasPosition)
    }

    @Test
    fun aPositionOutsideTheCountIsDropped() {
        assertNull(ReaderProgress.snapshot(position = 13, positionCount = 12, totalProgression = null).position)
        assertNull(ReaderProgress.snapshot(position = 0, positionCount = 12, totalProgression = null).position)
        assertNull(ReaderProgress.snapshot(position = 2, positionCount = 0, totalProgression = null).position)
    }

    @Test
    fun thePercentageDerivesFromThePositionWhenTheLocatorHasNoTotalProgression() {
        assertEquals(0, ReaderProgress.snapshot(position = 1, positionCount = 4, totalProgression = null).percent)
        assertEquals(50, ReaderProgress.snapshot(position = 3, positionCount = 4, totalProgression = null).percent)
        assertEquals(0, ReaderProgress.snapshot(position = null, positionCount = 0, totalProgression = null).percent)
    }

    @Test
    fun fixedLayoutsFlagTheirPositionsAsPages() {
        assertEquals(ProgressSnapshot(2, 6, 20, pages = true), ReaderProgress.snapshot(2, 6, 0.2, fixedLayout = true))
        assertFalse(ReaderProgress.snapshot(2, 6, 0.2).pages)
        assertFalse(ReaderProgress.snapshot(null, 0, 0.2, fixedLayout = true).hasPosition)
    }

    @Test
    fun valuesAreClampedIntoRange() {
        assertEquals(100, ReaderProgress.snapshot(null, 0, 1.7).percent)
        assertEquals(0, ReaderProgress.snapshot(null, 0, -0.2).percent)
        assertEquals(0, ReaderProgress.snapshot(null, -5, Double.NaN).percent)
        assertEquals(0, ReaderProgress.snapshot(null, -5, null).positionCount)
    }
}
