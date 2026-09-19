package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

/**
 * Decides when a stream of progress updates reaches disk (roadmap P1.3): the first update after
 * [minIntervalMillis] of silence is written immediately, updates that arrive sooner replace the
 * pending one, and [flush] hands back whatever is still pending (close, pause, process end).
 * Pure logic; the caller owns the clock and the delayed flush.
 */
internal class ProgressThrottle<T : Any>(private val minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MILLIS) {

    init {
        require(minIntervalMillis >= 0L)
    }

    private var lastWriteAtMillis: Long? = null
    private var pending: T? = null

    val hasPending: Boolean get() = pending != null

    /** Returns the value to write now, or null when it was parked as pending. */
    fun offer(value: T, nowMillis: Long): T? {
        val last = lastWriteAtMillis
        return if (last == null || nowMillis - last >= minIntervalMillis || nowMillis < last) {
            lastWriteAtMillis = nowMillis
            pending = null
            value
        } else {
            pending = value
            null
        }
    }

    /** Returns the pending value (and marks it written at [nowMillis]) or null when nothing waits. */
    fun flush(nowMillis: Long): T? {
        val value = pending ?: return null
        pending = null
        lastWriteAtMillis = nowMillis
        return value
    }

    companion object {
        const val DEFAULT_MIN_INTERVAL_MILLIS = 2_000L
    }
}
