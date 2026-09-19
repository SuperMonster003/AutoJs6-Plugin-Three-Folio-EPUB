package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

/** The read-aloud sleep timer choices (roadmap P3): off, a fixed span, or the end of the current chapter. */
internal enum class SleepTimer(val key: String, val minutes: Int?) {
    OFF("off", null),
    MINUTES_15("15", 15),
    MINUTES_30("30", 30),
    MINUTES_60("60", 60),
    END_OF_CHAPTER("chapter", null);

    companion object {
        fun fromKey(key: String?): SleepTimer? = entries.firstOrNull { it.key == key }
    }
}

/** Android-free sleep-timer arithmetic, covered by `SleepTimerPolicyTest`. */
internal object SleepTimerPolicy {

    const val MILLIS_PER_MINUTE = 60_000L

    /** How long a fixed timer runs; null for [SleepTimer.OFF] and [SleepTimer.END_OF_CHAPTER]. */
    fun durationMillis(timer: SleepTimer): Long? = timer.minutes?.let { it * MILLIS_PER_MINUTE }

    /** Milliseconds left on a timer armed at [armedAtMillis] for [durationMillis], never negative. */
    fun remainingMillis(armedAtMillis: Long, durationMillis: Long, nowMillis: Long): Long =
        (armedAtMillis + durationMillis - nowMillis).coerceAtLeast(0L)

    /** Whole minutes to show for [remainingMillis], rounded up so the label reads "1" until the end. */
    fun remainingMinutes(remainingMillis: Long): Int =
        ((remainingMillis + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt()

    /** True when a timer armed at [armedAtMillis] for [durationMillis] has run out at [nowMillis]. */
    fun expired(armedAtMillis: Long, durationMillis: Long, nowMillis: Long): Boolean =
        remainingMillis(armedAtMillis, durationMillis, nowMillis) == 0L

    /**
     * True when [timer] is [SleepTimer.END_OF_CHAPTER], it was armed while the voice read
     * [armedHref], and the voice now reads [currentHref], a different resource. Unknown resources
     * (null) never end the chapter.
     */
    fun chapterEnded(timer: SleepTimer, armedHref: String?, currentHref: String?): Boolean =
        timer == SleepTimer.END_OF_CHAPTER && armedHref != null && currentHref != null && armedHref != currentHref
}
