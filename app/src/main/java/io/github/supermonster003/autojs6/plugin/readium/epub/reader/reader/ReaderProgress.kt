package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import kotlin.math.roundToInt

/**
 * What the progress bar shows (roadmap P1.2): the synthetic position `x / N` when Readium's
 * positions service has produced them, and the whole-book percentage. Fixed layouts (P2.4) have
 * exactly one position per page, so [pages] asks the chrome to say `Page x of N` instead.
 */
internal data class ProgressSnapshot(
    val position: Int?,
    val positionCount: Int,
    val percent: Int,
    val pages: Boolean = false,
) {
    val hasPosition: Boolean get() = position != null && positionCount > 0
}

internal object ReaderProgress {

    /**
     * @param position 1-based position from the current locator, if known.
     * @param positionCount total positions of the book; 0 while unknown.
     * @param totalProgression whole-book progression 0..1 from the locator, if known.
     * @param fixedLayout true when every position is a page of a fixed-layout book.
     */
    fun snapshot(position: Int?, positionCount: Int, totalProgression: Double?, fixedLayout: Boolean = false): ProgressSnapshot {
        val count = positionCount.coerceAtLeast(0)
        val safePosition = position?.takeIf { count > 0 && it in 1..count }
        val percent = when {
            totalProgression != null && !totalProgression.isNaN() ->
                (totalProgression.coerceIn(0.0, 1.0) * 100).roundToInt()
            safePosition != null -> ((safePosition - 1) * 100.0 / count).roundToInt()
            else -> 0
        }
        return ProgressSnapshot(safePosition, count, percent.coerceIn(0, 100), pages = fixedLayout)
    }
}
