package io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs

import kotlin.math.roundToInt

/**
 * The value ranges the preferences panel offers and the codec clamps to (roadmap P2.1). Ranges
 * follow Readium's `EpubPreferencesEditor`, except the font size which the roadmap limits to
 * 50 % .. 300 %.
 */
internal object PreferenceRanges {

    val FONT_SIZE = 0.5..3.0
    const val FONT_SIZE_STEP = 0.1

    val LINE_HEIGHT = 1.0..2.0
    const val LINE_HEIGHT_STEP = 0.1

    val PAGE_MARGINS = 0.0..4.0
    const val PAGE_MARGINS_STEP = 0.25

    val PARAGRAPH_SPACING = 0.0..2.0
    const val PARAGRAPH_SPACING_STEP = 0.25

    val FONT_WEIGHT = 0.0..2.5
    val LETTER_SPACING = 0.0..1.0
    val PARAGRAPH_INDENT = 0.0..3.0
    val TYPE_SCALE = 1.0..2.0
    val WORD_SPACING = 0.0..1.0

    fun clamp(range: ClosedFloatingPointRange<Double>, value: Double): Double = value.coerceIn(range)

    /** Rounds [value] to the nearest multiple of [step] inside [range]. */
    fun snap(range: ClosedFloatingPointRange<Double>, step: Double, value: Double): Double {
        val steps = ((value - range.start) / step).roundToInt()
        val snapped = range.start + steps * step
        // Round to 4 decimals so 0.1 steps never accumulate binary noise (0.30000000000000004).
        return (clamp(range, snapped) * 10_000).roundToInt() / 10_000.0
    }

    /** One step up ([delta] = +1) or down ([delta] = -1) from [value], snapped to the grid. */
    fun step(range: ClosedFloatingPointRange<Double>, step: Double, value: Double, delta: Int): Double =
        snap(range, step, snap(range, step, value) + delta * step)
}
