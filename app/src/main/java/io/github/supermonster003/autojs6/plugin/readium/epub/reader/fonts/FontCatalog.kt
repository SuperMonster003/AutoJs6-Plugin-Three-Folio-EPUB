package io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts

import org.json.JSONArray
import org.json.JSONObject

/** One imported font file (roadmap P2.2 / D13), stored as `files/fonts/<sha256>.<ttf|otf>`. */
data class FontEntry(
    val sha256: String,
    val fileName: String,
    /** Shown in the panel: the `name` table's family, else derived from the picked file's name. */
    val displayName: String,
    /** The CSS family the `@font-face` declares and the preference stores; unique in the catalog. */
    val family: String,
    val bytes: Long,
    val importedAt: Long,
) {
    val format: FontFormat
        get() = if (fileName.endsWith(".otf")) FontFormat.OPEN_TYPE else FontFormat.TRUE_TYPE
}

data class FontCatalog(val fonts: List<FontEntry>) {

    val isEmpty: Boolean get() = fonts.isEmpty()

    fun bySha256(sha256: String): FontEntry? = fonts.firstOrNull { it.sha256 == sha256 }

    fun byFamily(family: String): FontEntry? = fonts.firstOrNull { it.family.equals(family, ignoreCase = true) }

    companion object {
        val EMPTY = FontCatalog(emptyList())
    }
}

/** Limits of the font store (roadmap P2.2): they bound disk use and the length of the font list. */
object FontLimits {
    const val MAX_FONTS = 10
    const val MAX_BYTES_MEGABYTES = 20
    const val MAX_BYTES: Long = MAX_BYTES_MEGABYTES * 1024L * 1024L
}

/** CSS family names an imported font must not shadow; collisions get a hash suffix. */
object FontFamilyNames {

    /** Generic CSS families, CSS-wide keywords and the fonts Readium bundles (compared lowercase). */
    val RESERVED: Set<String> = setOf(
        "serif", "sans-serif", "monospace", "cursive", "fantasy", "system-ui", "ui-serif", "ui-sans-serif",
        "ui-monospace", "ui-rounded", "math", "emoji", "fangsong", "inherit", "initial", "unset", "revert",
        "default", "opendyslexic", "accessibledfa", "ia writer duospace",
    )

    /** How many hash digits disambiguate a colliding name. */
    const val HASH_SUFFIX_LENGTH = 8

    /**
     * [preferred] when it is free, else `preferred (first 8 hex digits of the hash)`. The result is
     * stored with the entry and never recomputed, so a stored preference keeps matching.
     */
    fun resolve(preferred: String, sha256: String, taken: Collection<String>): String {
        val free = preferred.lowercase() !in RESERVED && taken.none { it.equals(preferred, ignoreCase = true) }
        return if (free) preferred else "$preferred (${sha256.take(HASH_SUFFIX_LENGTH)})"
    }
}

/**
 * The `files/fonts/index.json` envelope: `{format, fonts: [{sha256, fileName, displayName, family,
 * bytes, importedAt}]}`. Decoding drops entries that fail validation and keeps the first of two
 * entries sharing a hash or a family; a corrupt envelope reads as null.
 */
object FontCatalogCodec {

    const val FORMAT = 1

    val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    val FILE_NAME_PATTERN = Regex("[0-9a-f]{64}\\.(ttf|otf)")

    /** A family may carry the hash suffix on top of a full-length display name. */
    const val MAX_FAMILY_LENGTH = FontFileValidator.MAX_NAME_LENGTH + FontFamilyNames.HASH_SUFFIX_LENGTH + 3

    fun encode(catalog: FontCatalog): String {
        val fonts = JSONArray()
        for (entry in catalog.fonts) {
            fonts.put(
                JSONObject()
                    .put("sha256", entry.sha256)
                    .put("fileName", entry.fileName)
                    .put("displayName", entry.displayName)
                    .put("family", entry.family)
                    .put("bytes", entry.bytes)
                    .put("importedAt", entry.importedAt),
            )
        }
        return JSONObject().put("format", FORMAT).put("fonts", fonts).toString(2)
    }

    fun decode(text: String): FontCatalog? {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (root.optInt("format", -1) != FORMAT) return null
        val array = root.optJSONArray("fonts") ?: return FontCatalog.EMPTY
        val fonts = mutableListOf<FontEntry>()
        for (index in 0 until array.length()) {
            val entry = array.optJSONObject(index)?.let(::decodeEntry) ?: continue
            if (fonts.any { it.sha256 == entry.sha256 || it.family.equals(entry.family, ignoreCase = true) }) continue
            fonts += entry
        }
        return FontCatalog(fonts)
    }

    private fun decodeEntry(json: JSONObject): FontEntry? {
        val sha256 = json.optString("sha256").takeIf { SHA256_PATTERN.matches(it) } ?: return null
        val fileName = json.optString("fileName")
            .takeIf { FILE_NAME_PATTERN.matches(it) && it.startsWith(sha256) } ?: return null
        val displayName = json.optString("displayName").takeIf { it.isClean(FontFileValidator.MAX_NAME_LENGTH) } ?: return null
        val family = json.optString("family").takeIf { it.isClean(MAX_FAMILY_LENGTH) } ?: return null
        val bytes = json.optLong("bytes", -1).takeIf { it > 0 } ?: return null
        val importedAt = json.optLong("importedAt", 0).coerceAtLeast(0)
        return FontEntry(sha256, fileName, displayName, family, bytes, importedAt)
    }

    /** Names are stored already sanitized; anything else is a corrupt or tampered index. */
    private fun String.isClean(maxLength: Int): Boolean =
        isNotEmpty() && FontFileValidator.sanitize(this, maxLength) == this
}
