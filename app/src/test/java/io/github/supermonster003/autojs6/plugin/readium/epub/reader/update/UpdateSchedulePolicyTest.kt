package io.github.supermonster003.autojs6.plugin.readium.epub.reader.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSchedulePolicyTest {

    private val now = 1_800_000_000_000L
    private val hour = 60L * 60 * 1000

    @Test
    fun aManualCheckFetchesOnceADay() {
        assertTrue(UpdateSchedulePolicy.manualFetchDue(null, now))
        assertFalse(UpdateSchedulePolicy.manualFetchDue(now - hour, now))
        assertFalse(UpdateSchedulePolicy.manualFetchDue(now - 23 * hour, now))
        assertTrue(UpdateSchedulePolicy.manualFetchDue(now - 24 * hour, now))
        assertTrue(UpdateSchedulePolicy.manualFetchDue(now - 30 * hour, now))
        assertEquals(0L, UpdateSchedulePolicy.millisUntilManualFetch(null, now))
        assertEquals(23 * hour, UpdateSchedulePolicy.millisUntilManualFetch(now - hour, now))
        assertEquals(0L, UpdateSchedulePolicy.millisUntilManualFetch(now - 24 * hour, now))
    }

    @Test
    fun aClockThatMovedBackDoesNotLockTheCheckOut() {
        assertTrue(UpdateSchedulePolicy.manualFetchDue(now + 5 * hour, now))
        assertEquals(0L, UpdateSchedulePolicy.millisUntilManualFetch(now + 5 * hour, now))
    }

    @Test
    fun automaticChecksAreOffAndWouldNeedAnUnmeteredKnownConnection() {
        assertFalse(UpdateSchedulePolicy.AUTOMATIC_CHECKS_ENABLED)
        assertFalse(UpdateSchedulePolicy.automaticCheckAllowed(UpdateSchedulePolicy.AUTOMATIC_CHECKS_ENABLED, metered = false))
        assertTrue(UpdateSchedulePolicy.automaticCheckAllowed(automaticEnabled = true, metered = false))
        assertFalse(UpdateSchedulePolicy.automaticCheckAllowed(automaticEnabled = true, metered = true))
        assertFalse("an unknown connection counts as metered", UpdateSchedulePolicy.automaticCheckAllowed(automaticEnabled = true, metered = null))
    }
}
