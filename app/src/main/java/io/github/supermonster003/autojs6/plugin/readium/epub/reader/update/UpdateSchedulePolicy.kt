package io.github.supermonster003.autojs6.plugin.readium.epub.reader.update

/**
 * When an update check may touch the network (roadmap D28): never on its own, and a manual check
 * fetches at most once a day (within the day the cached answer is shown again). Automatic checks
 * are disabled by design; were they ever enabled, a metered or unknown connection would still
 * block them, and the plugin does not request `ACCESS_NETWORK_STATE`, so the connection kind is
 * unknown (roadmap D19).
 */
internal object UpdateSchedulePolicy {

    const val AUTOMATIC_CHECKS_ENABLED = false
    const val MANUAL_FETCH_INTERVAL_MILLIS = 24L * 60 * 60 * 1000

    /** True when a manual check should hit the network: never fetched, a day passed, or the clock moved back. */
    fun manualFetchDue(lastFetchedAtMillis: Long?, nowMillis: Long): Boolean {
        if (lastFetchedAtMillis == null) return true
        if (nowMillis < lastFetchedAtMillis) return true
        return nowMillis - lastFetchedAtMillis >= MANUAL_FETCH_INTERVAL_MILLIS
    }

    /** Milliseconds until a manual check fetches again; zero when it is due now. */
    fun millisUntilManualFetch(lastFetchedAtMillis: Long?, nowMillis: Long): Long =
        if (manualFetchDue(lastFetchedAtMillis, nowMillis)) 0L
        else MANUAL_FETCH_INTERVAL_MILLIS - (nowMillis - requireNotNull(lastFetchedAtMillis))

    /** [metered] is null when the connection kind is unknown, which counts as metered. */
    fun automaticCheckAllowed(automaticEnabled: Boolean, metered: Boolean?): Boolean =
        automaticEnabled && metered == false
}
