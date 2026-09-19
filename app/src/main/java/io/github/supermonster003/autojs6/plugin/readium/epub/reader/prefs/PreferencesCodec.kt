package io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs

import org.json.JSONException
import org.json.JSONObject

/**
 * What the reader stores globally (roadmap D14): the theme mode and Readium's own
 * `EpubPreferences` JSON. [readium] is the sanitized object as Readium serializes it, so the
 * Android side hands it straight to `EpubPreferencesSerializer`.
 */
internal data class StoredPreferences(
    val themeMode: ThemeMode,
    val readium: JSONObject,
) {
    /** True when nothing deviates from the defaults. */
    val isDefault: Boolean get() = themeMode == ThemeMode.DEFAULT && readium.length() == 0

    fun sameAs(other: StoredPreferences?): Boolean =
        other != null && themeMode == other.themeMode && readium.toString() == other.readium.toString()

    companion object {
        fun defaults() = StoredPreferences(ThemeMode.DEFAULT, JSONObject())
    }
}

/**
 * `reader-preferences.json` (roadmap P2.1):
 *
 * ```json
 * { "format": 1, "themeMode": "sepia", "preferences": { "fontSize": 1.2, "theme": "sepia", ... } }
 * ```
 *
 * `preferences` is the Readium `EpubPreferences` JSON. Decoding is strict about the envelope and
 * forgiving about the payload: unknown keys are dropped, values of the wrong type are dropped, and
 * numbers are clamped into [PreferenceRanges] so a hand-edited or older file can never make
 * `EpubPreferences` throw or push the layout out of the supported window. A corrupt file yields
 * null and the caller falls back to [StoredPreferences.defaults].
 */
internal object PreferencesCodec {

    const val FORMAT = 1
    private const val KEY_FORMAT = "format"
    private const val KEY_THEME_MODE = "themeMode"
    private const val KEY_PREFERENCES = "preferences"

    private val numberRanges: Map<String, ClosedFloatingPointRange<Double>> = mapOf(
        "fontSize" to PreferenceRanges.FONT_SIZE,
        "fontWeight" to PreferenceRanges.FONT_WEIGHT,
        "letterSpacing" to PreferenceRanges.LETTER_SPACING,
        "lineHeight" to PreferenceRanges.LINE_HEIGHT,
        "pageMargins" to PreferenceRanges.PAGE_MARGINS,
        "paragraphIndent" to PreferenceRanges.PARAGRAPH_INDENT,
        "paragraphSpacing" to PreferenceRanges.PARAGRAPH_SPACING,
        "typeScale" to PreferenceRanges.TYPE_SCALE,
        "wordSpacing" to PreferenceRanges.WORD_SPACING,
    )

    private val booleanKeys = setOf(
        "hyphens", "ligatures", "publisherStyles", "scroll", "textNormalization", "verticalText",
    )

    private val enumValues: Map<String, Set<String>> = mapOf(
        "columnCount" to setOf("auto", "1", "2"),
        "imageFilter" to setOf("darken", "invert"),
        "readingProgression" to setOf("ltr", "rtl"),
        // `EpubPreferences` rejects `auto` for the spread preference.
        "spread" to setOf("never", "always"),
        "textAlign" to setOf("center", "justify", "start", "end", "left", "right"),
    )

    /** The theme is derived from [StoredPreferences.themeMode] at display time, never stored. */
    private val derivedKeys = setOf("theme")

    private val colorKeys = setOf("backgroundColor", "textColor")

    private const val KEY_FONT_FAMILY = "fontFamily"
    private const val KEY_LANGUAGE = "language"
    private const val MAX_FONT_FAMILY_LENGTH = 120
    private val languagePattern = Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*")

    fun encode(stored: StoredPreferences): String =
        JSONObject()
            .put(KEY_FORMAT, FORMAT)
            .put(KEY_THEME_MODE, stored.themeMode.key)
            .put(KEY_PREFERENCES, sanitize(stored.readium))
            .toString(2)

    fun decode(text: String): StoredPreferences? {
        val root = try {
            JSONObject(text)
        } catch (_: JSONException) {
            return null
        }
        if (root.optInt(KEY_FORMAT, -1) != FORMAT) return null
        val themeMode = ThemeMode.fromKey(root.optString(KEY_THEME_MODE, null)) ?: ThemeMode.DEFAULT
        val readium = root.optJSONObject(KEY_PREFERENCES) ?: JSONObject()
        return StoredPreferences(themeMode, sanitize(readium))
    }

    /** Keeps only known keys with values of the right type, clamped into the supported ranges. */
    fun sanitize(raw: JSONObject): JSONObject {
        val clean = JSONObject()
        for (key in raw.keys()) {
            val value = raw.opt(key) ?: continue
            when {
                key in derivedKeys -> Unit
                key in numberRanges -> (value as? Number)?.toDouble()
                    ?.takeIf { it.isFinite() }
                    ?.let { clean.put(key, PreferenceRanges.clamp(numberRanges.getValue(key), it)) }
                key in booleanKeys -> (value as? Boolean)?.let { clean.put(key, it) }
                key in enumValues -> (value as? String)
                    ?.takeIf { it in enumValues.getValue(key) }
                    ?.let { clean.put(key, it) }
                key in colorKeys -> (value as? Number)
                    ?.takeIf { it is Int || it is Long && it.toInt().toLong() == it }
                    ?.let { clean.put(key, it.toInt()) }
                key == KEY_FONT_FAMILY -> (value as? String)?.trim()
                    ?.takeIf { it.isNotEmpty() && it.length <= MAX_FONT_FAMILY_LENGTH }
                    ?.let { clean.put(key, it) }
                key == KEY_LANGUAGE -> (value as? String)?.trim()
                    ?.takeIf { languagePattern.matches(it) }
                    ?.let { clean.put(key, it) }
            }
        }
        return clean
    }
}
