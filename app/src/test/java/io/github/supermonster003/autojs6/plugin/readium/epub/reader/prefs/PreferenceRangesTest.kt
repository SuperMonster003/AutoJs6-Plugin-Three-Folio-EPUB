package io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceRangesTest {

    private val fontSize = PreferenceRanges.FONT_SIZE
    private val step = PreferenceRanges.FONT_SIZE_STEP

    @Test
    fun clampKeepsValuesInsideTheRange() {
        assertEquals(0.5, PreferenceRanges.clamp(fontSize, 0.1), 0.0)
        assertEquals(3.0, PreferenceRanges.clamp(fontSize, 7.0), 0.0)
        assertEquals(1.7, PreferenceRanges.clamp(fontSize, 1.7), 0.0)
    }

    @Test
    fun snapRoundsToTheGridWithoutBinaryNoise() {
        assertEquals(1.2, PreferenceRanges.snap(fontSize, step, 1.23), 0.0)
        assertEquals(1.3, PreferenceRanges.snap(fontSize, step, 1.26), 0.0)
        assertEquals(3.0, PreferenceRanges.snap(fontSize, step, 3.7), 0.0)
        assertEquals(0.5, PreferenceRanges.snap(fontSize, step, 0.2), 0.0)
        val spacing = PreferenceRanges.PARAGRAPH_SPACING
        assertEquals(0.3, PreferenceRanges.snap(spacing, 0.1, 0.1 + 0.2), 0.0)
        assertEquals("0.3", PreferenceRanges.step(spacing, 0.1, 0.2, +1).toString())
    }

    @Test
    fun stepsMoveOneNotchAndStopAtTheEnds() {
        assertEquals(1.1, PreferenceRanges.step(fontSize, step, 1.0, +1), 0.0)
        assertEquals(0.9, PreferenceRanges.step(fontSize, step, 1.0, -1), 0.0)
        assertEquals(0.5, PreferenceRanges.step(fontSize, step, 0.5, -1), 0.0)
        assertEquals(3.0, PreferenceRanges.step(fontSize, step, 2.95, +1), 0.0)
        assertEquals(3.0, PreferenceRanges.step(fontSize, step, 3.0, +1), 0.0)
        assertEquals(1.25, PreferenceRanges.step(PreferenceRanges.PAGE_MARGINS, PreferenceRanges.PAGE_MARGINS_STEP, 1.0, +1), 0.0)
    }

    @Test
    fun rangesFollowReadiumExceptTheFontSizeWindow() {
        assertEquals(0.5..3.0, PreferenceRanges.FONT_SIZE)
        assertEquals(1.0..2.0, PreferenceRanges.LINE_HEIGHT)
        assertEquals(0.0..4.0, PreferenceRanges.PAGE_MARGINS)
        assertEquals(0.0..2.0, PreferenceRanges.PARAGRAPH_SPACING)
        assertEquals(0.0..2.5, PreferenceRanges.FONT_WEIGHT)
        assertEquals(1.0..2.0, PreferenceRanges.TYPE_SCALE)
    }
}
