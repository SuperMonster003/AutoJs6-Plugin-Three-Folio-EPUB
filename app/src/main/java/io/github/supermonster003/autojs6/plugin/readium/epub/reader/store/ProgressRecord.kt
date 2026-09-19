package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import org.json.JSONObject

/**
 * The last reading position of one book (roadmap D4 / D13): the Readium `Locator` as JSON, when
 * it was written, and the whole-book progression when the navigator knew it.
 */
internal data class ProgressRecord(
    val locator: JSONObject,
    val updatedAtMillis: Long,
    val totalProgression: Double? = null,
) {
    /** Structural comparison; [JSONObject] itself only compares identity. */
    fun sameAs(other: ProgressRecord?): Boolean =
        other != null &&
            updatedAtMillis == other.updatedAtMillis &&
            totalProgression == other.totalProgression &&
            locator.toString() == other.locator.toString()
}

/** `progress.json` codec. Unknown formats and corrupt text decode to null and are ignored by the store. */
internal object ProgressCodec {

    const val FORMAT = 1

    private const val KEY_FORMAT = "format"
    private const val KEY_LOCATOR = "locator"
    private const val KEY_UPDATED_AT = "updatedAt"
    private const val KEY_TOTAL_PROGRESSION = "totalProgression"

    fun encode(record: ProgressRecord): String = JSONObject().apply {
        put(KEY_FORMAT, FORMAT)
        put(KEY_LOCATOR, JSONObject(record.locator.toString()))
        put(KEY_UPDATED_AT, record.updatedAtMillis)
        record.totalProgression?.let { put(KEY_TOTAL_PROGRESSION, it) }
    }.toString()

    fun decode(text: String): ProgressRecord? = runCatching {
        val json = JSONObject(text)
        if (json.optInt(KEY_FORMAT, -1) != FORMAT) return null
        val locator = json.optJSONObject(KEY_LOCATOR) ?: return null
        // href and type are what Readium requires to rebuild a Locator.
        if (locator.optString("href").isEmpty() || locator.optString("type").isEmpty()) return null
        val updatedAt = json.optLong(KEY_UPDATED_AT, -1L).takeIf { it >= 0L } ?: return null
        val totalProgression = if (json.has(KEY_TOTAL_PROGRESSION)) {
            json.optDouble(KEY_TOTAL_PROGRESSION).takeIf { !it.isNaN() && it in 0.0..1.0 }
        } else {
            null
        }
        ProgressRecord(locator, updatedAt, totalProgression)
    }.getOrNull()
}
