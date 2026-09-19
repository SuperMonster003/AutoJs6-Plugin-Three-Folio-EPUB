package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressThrottleTest {

    @Test
    fun theFirstUpdateIsWrittenImmediately() {
        val throttle = ProgressThrottle<String>(minIntervalMillis = 1000L)

        assertEquals("a", throttle.offer("a", nowMillis = 5_000L))
        assertFalse(throttle.hasPending)
        assertNull(throttle.flush(5_001L))
    }

    @Test
    fun updatesInsideTheIntervalAreParkedAndTheLatestOneWins() {
        val throttle = ProgressThrottle<String>(minIntervalMillis = 1000L)
        throttle.offer("a", 5_000L)

        assertNull(throttle.offer("b", 5_200L))
        assertNull(throttle.offer("c", 5_900L))
        assertTrue(throttle.hasPending)
        assertEquals("c", throttle.flush(5_950L))
        assertFalse(throttle.hasPending)
        assertNull(throttle.flush(5_960L))
    }

    @Test
    fun anUpdateAfterTheIntervalIsWrittenAndClearsThePendingOne() {
        val throttle = ProgressThrottle<String>(minIntervalMillis = 1000L)
        throttle.offer("a", 5_000L)
        throttle.offer("b", 5_500L)

        assertEquals("c", throttle.offer("c", 6_000L))
        assertFalse(throttle.hasPending)
    }

    @Test
    fun aClockGoingBackwardsNeverStallsWrites() {
        val throttle = ProgressThrottle<String>(minIntervalMillis = 1000L)
        throttle.offer("a", 10_000L)

        assertEquals("b", throttle.offer("b", 2_000L))
    }

    @Test
    fun flushingRestartsTheInterval() {
        val throttle = ProgressThrottle<String>(minIntervalMillis = 1000L)
        throttle.offer("a", 1_000L)
        throttle.offer("b", 1_500L)
        assertEquals("b", throttle.flush(1_600L))

        assertNull(throttle.offer("c", 2_000L))
        assertEquals("d", throttle.offer("d", 2_600L))
    }

    @Test
    fun theDefaultIntervalIsTwoSeconds() {
        assertEquals(2_000L, ProgressThrottle.DEFAULT_MIN_INTERVAL_MILLIS)
    }
}
