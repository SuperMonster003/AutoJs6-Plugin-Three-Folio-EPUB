package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P3: the sleep timer's spans, remaining-time label, expiry and end-of-chapter detection. */
class SleepTimerPolicyTest {

    @Test
    fun fixedSpansAreWholeMinutesAndTheOthersHaveNone() {
        assertEquals(15 * 60_000L, SleepTimerPolicy.durationMillis(SleepTimer.MINUTES_15))
        assertEquals(30 * 60_000L, SleepTimerPolicy.durationMillis(SleepTimer.MINUTES_30))
        assertEquals(60 * 60_000L, SleepTimerPolicy.durationMillis(SleepTimer.MINUTES_60))
        assertNull(SleepTimerPolicy.durationMillis(SleepTimer.OFF))
        assertNull(SleepTimerPolicy.durationMillis(SleepTimer.END_OF_CHAPTER))
    }

    @Test
    fun remainingTimeCountsDownAndNeverGoesNegative() {
        val armed = 1_000L
        val duration = 15 * 60_000L
        assertEquals(duration, SleepTimerPolicy.remainingMillis(armed, duration, armed))
        assertEquals(duration - 90_000L, SleepTimerPolicy.remainingMillis(armed, duration, armed + 90_000L))
        assertEquals(0L, SleepTimerPolicy.remainingMillis(armed, duration, armed + duration + 5L))
        assertFalse(SleepTimerPolicy.expired(armed, duration, armed + duration - 1L))
        assertTrue(SleepTimerPolicy.expired(armed, duration, armed + duration))
    }

    @Test
    fun remainingMinutesRoundUpSoTheLabelReadsOneUntilTheEnd() {
        assertEquals(15, SleepTimerPolicy.remainingMinutes(15 * 60_000L))
        assertEquals(15, SleepTimerPolicy.remainingMinutes(14 * 60_000L + 1L))
        assertEquals(1, SleepTimerPolicy.remainingMinutes(1L))
        assertEquals(0, SleepTimerPolicy.remainingMinutes(0L))
    }

    @Test
    fun theChapterEndsWhenTheVoiceMovesToAnotherResource() {
        assertFalse(SleepTimerPolicy.chapterEnded(SleepTimer.END_OF_CHAPTER, "chapter1.xhtml", "chapter1.xhtml"))
        assertTrue(SleepTimerPolicy.chapterEnded(SleepTimer.END_OF_CHAPTER, "chapter1.xhtml", "chapter2.xhtml"))
        assertFalse(SleepTimerPolicy.chapterEnded(SleepTimer.MINUTES_15, "chapter1.xhtml", "chapter2.xhtml"))
        assertFalse(SleepTimerPolicy.chapterEnded(SleepTimer.END_OF_CHAPTER, null, "chapter2.xhtml"))
        assertFalse(SleepTimerPolicy.chapterEnded(SleepTimer.END_OF_CHAPTER, "chapter1.xhtml", null))
    }

    @Test
    fun keysRoundTripAndUnknownKeysAreRejected() {
        for (timer in SleepTimer.entries) assertEquals(timer, SleepTimer.fromKey(timer.key))
        assertNull(SleepTimer.fromKey("45"))
        assertNull(SleepTimer.fromKey(null))
    }
}
