package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.PreferenceRanges
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.json.JSONException
import org.json.JSONObject

/**
 * The validated subset of the contract's `PREFERENCE_*` keys one `setPreferences` call (or the
 * `openReader` options) carries: a key that is present with `null` resets that preference to
 * the reader's default, an absent key leaves it alone.
 */
internal class PreferencePatch(private val values: Map<String, Any?>) {

    val keys: Set<String> get() = values.keys
    val isEmpty: Boolean get() = values.isEmpty()

    fun has(key: String): Boolean = values.containsKey(key)
    fun double(key: String): Double? = values[key] as? Double
    fun string(key: String): String? = values[key] as? String
    fun boolean(key: String): Boolean? = values[key] as? Boolean

    /** [other] wins where both set a key. */
    fun merge(other: PreferencePatch): PreferencePatch = PreferencePatch(values + other.values)

    override fun toString(): String = "PreferencePatch$values"

    companion object {
        val EMPTY = PreferencePatch(emptyMap())
    }
}

/** A parsed preferences document: what applies, and the keys the contract does not know (reported, never applied). */
internal data class PreferenceParse(val patch: PreferencePatch, val unsupported: List<String>)

/**
 * Parses the `KEY_PREFERENCES` JSON of the reader session (roadmap P5.3). Values are checked
 * strictly (a wrong type or an out-of-range number is `INVALID_ARGUMENT`, the size ceiling is
 * `LIMIT_EXCEEDED`) because a script wrote them on purpose; keys outside the contract are
 * collected for an `UNSUPPORTED_PREFERENCE` error event as the AIDL contract prescribes. The
 * ranges are the reader panel's own ([PreferenceRanges]).
 */
internal object ReaderPreferencesJson {

    const val MAX_FONT_FAMILY_LENGTH = 120

    val COLUMN_COUNTS: Set<String> = setOf("auto", "1", "2")
    val TEXT_ALIGNS: Set<String> = setOf("start", "end", "left", "right", "justify", "center")

    /** Keys the reader applies only without the publisher's styles, as the preferences panel does. */
    val ADVANCED_KEYS: Set<String> = setOf(
        EpubContract.PREFERENCE_LINE_HEIGHT,
        EpubContract.PREFERENCE_TEXT_ALIGN,
        EpubContract.PREFERENCE_HYPHENS,
    )

    private val numberRanges: Map<String, ClosedFloatingPointRange<Double>> = mapOf(
        EpubContract.PREFERENCE_FONT_SIZE to PreferenceRanges.FONT_SIZE,
        EpubContract.PREFERENCE_LINE_HEIGHT to PreferenceRanges.LINE_HEIGHT,
        EpubContract.PREFERENCE_PAGE_MARGINS to PreferenceRanges.PAGE_MARGINS,
    )

    private val booleanKeys: Set<String> = setOf(
        EpubContract.PREFERENCE_SCROLL,
        EpubContract.PREFERENCE_VERTICAL_TEXT,
        EpubContract.PREFERENCE_HYPHENS,
        EpubContract.PREFERENCE_PUBLISHER_STYLES,
    )

    private val enumValues: Map<String, Set<String>> = mapOf(
        EpubContract.PREFERENCE_THEME to EpubContract.THEMES,
        EpubContract.PREFERENCE_COLUMN_COUNT to COLUMN_COUNTS,
        EpubContract.PREFERENCE_TEXT_ALIGN to TEXT_ALIGNS,
    )

    fun parse(text: String?): PreferenceParse {
        val raw = text ?: throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "preferences are missing")
        if (raw.toByteArray(Charsets.UTF_8).size > EpubContract.MAX_PREFERENCES_BYTES) {
            throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "preferences exceed ${EpubContract.MAX_PREFERENCES_BYTES} bytes")
        }
        val json = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "preferences are not a JSON object")
        }
        val values = LinkedHashMap<String, Any?>()
        val unsupported = ArrayList<String>()
        for (key in json.keys()) {
            if (key !in EpubContract.PREFERENCES) {
                unsupported += key
                continue
            }
            val value = json.opt(key)
            if (value == null || value === JSONObject.NULL) {
                values[key] = null
                continue
            }
            values[key] = when {
                key in numberRanges -> number(key, value, numberRanges.getValue(key))
                key in booleanKeys -> value as? Boolean ?: throw invalid("$key must be a boolean")
                key in enumValues -> (value as? String)?.takeIf { it in enumValues.getValue(key) }
                    ?: throw invalid("$key must be one of ${enumValues.getValue(key).joinToString(", ")}")
                key == EpubContract.PREFERENCE_FONT_FAMILY -> (value as? String)?.trim()
                    ?.takeIf { it.isNotEmpty() && it.length <= MAX_FONT_FAMILY_LENGTH && it.none { c -> c < ' ' } }
                    ?: throw invalid("fontFamily must be a name of at most $MAX_FONT_FAMILY_LENGTH characters")
                else -> throw invalid("$key is not a reader preference")
            }
        }
        return PreferenceParse(PreferencePatch(values), unsupported)
    }

    private fun number(key: String, value: Any, range: ClosedFloatingPointRange<Double>): Double {
        val number = (value as? Number)?.toDouble()?.takeIf { it.isFinite() } ?: throw invalid("$key must be a number")
        if (number !in range) throw invalid("$key must be within ${range.start}..${range.endInclusive}")
        return number
    }

    private fun invalid(detail: String) = ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, detail)
}
