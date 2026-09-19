package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import org.json.JSONArray
import org.json.JSONObject

/**
 * One bookmark of one book (roadmap P2.6): the Readium `Locator` as JSON (href, type, title,
 * locations with progression / position / cssSelector, and the text of the first visible element
 * so a jump lands on the same element after a layout change), when it was created, and what the
 * list shows for it: the chapter title and a short excerpt, both captured at creation time.
 */
internal data class Bookmark(
    /** Unique within the book; never reused after a delete so the list can diff rows. */
    val id: Long,
    val locator: JSONObject,
    val createdAtMillis: Long,
    val chapter: String? = null,
    val snippet: String? = null,
) {
    val href: String get() = locator.optString(KEY_HREF)

    val progression: Double?
        get() = locator.optJSONObject(KEY_LOCATIONS)?.optDouble(KEY_PROGRESSION)?.takeIf { !it.isNaN() }

    val position: Int?
        get() = locator.optJSONObject(KEY_LOCATIONS)?.takeIf { it.has(KEY_POSITION) }?.optInt(KEY_POSITION)

    /** Structural comparison; [JSONObject] itself only compares identity. */
    fun sameAs(other: Bookmark?): Boolean =
        other != null &&
            id == other.id &&
            createdAtMillis == other.createdAtMillis &&
            chapter == other.chapter &&
            snippet == other.snippet &&
            locator.toString() == other.locator.toString()

    /** Two bookmarks of the same place: same locator, whatever their ids and labels. */
    fun samePlaceAs(other: Bookmark): Boolean = locator.toString() == other.locator.toString()

    private companion object {
        const val KEY_HREF = "href"
        const val KEY_LOCATIONS = "locations"
        const val KEY_PROGRESSION = "progression"
        const val KEY_POSITION = "position"
    }
}

/**
 * `bookmarks.json` codec. The file holds every bookmark of one book in creation order; a foreign
 * format or unreadable text decodes to null (the store treats it as empty), while a single corrupt
 * entry, a duplicate id or an entry beyond the cap is skipped so the rest survives.
 */
internal object BookmarkCodec {

    const val FORMAT = 1

    /** Per book (roadmap P2.6); the view model refuses to add beyond it. */
    const val MAX_BOOKMARKS = 500

    /** Labels are display text: anything longer is cut when decoding so a foreign file cannot bloat the list. */
    const val MAX_LABEL_LENGTH = 400

    private const val KEY_FORMAT = "format"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val KEY_ID = "id"
    private const val KEY_LOCATOR = "locator"
    private const val KEY_CREATED_AT = "createdAt"
    private const val KEY_CHAPTER = "chapter"
    private const val KEY_SNIPPET = "snippet"

    fun encode(bookmarks: List<Bookmark>): String = JSONObject().apply {
        put(KEY_FORMAT, FORMAT)
        put(
            KEY_BOOKMARKS,
            JSONArray().apply {
                for (bookmark in bookmarks) {
                    put(
                        JSONObject().apply {
                            put(KEY_ID, bookmark.id)
                            put(KEY_LOCATOR, JSONObject(bookmark.locator.toString()))
                            put(KEY_CREATED_AT, bookmark.createdAtMillis)
                            bookmark.chapter?.let { put(KEY_CHAPTER, it) }
                            bookmark.snippet?.let { put(KEY_SNIPPET, it) }
                        },
                    )
                }
            },
        )
    }.toString()

    fun decode(text: String): List<Bookmark>? = runCatching {
        val json = JSONObject(text)
        if (json.optInt(KEY_FORMAT, -1) != FORMAT) return null
        val array = json.optJSONArray(KEY_BOOKMARKS) ?: return null
        val seen = HashSet<Long>()
        val bookmarks = ArrayList<Bookmark>()
        for (index in 0 until array.length()) {
            val entry = array.optJSONObject(index) ?: continue
            val bookmark = decodeEntry(entry) ?: continue
            if (!seen.add(bookmark.id)) continue
            bookmarks.add(bookmark)
            if (bookmarks.size == MAX_BOOKMARKS) break
        }
        bookmarks
    }.getOrNull()

    private fun decodeEntry(entry: JSONObject): Bookmark? {
        val id = entry.optLong(KEY_ID, -1L).takeIf { it >= 0L } ?: return null
        val locator = entry.optJSONObject(KEY_LOCATOR) ?: return null
        // href and type are what Readium requires to rebuild a Locator.
        if (locator.optString("href").isEmpty() || locator.optString("type").isEmpty()) return null
        val createdAt = entry.optLong(KEY_CREATED_AT, -1L).takeIf { it >= 0L } ?: return null
        return Bookmark(
            id = id,
            locator = locator,
            createdAtMillis = createdAt,
            chapter = label(entry, KEY_CHAPTER),
            snippet = label(entry, KEY_SNIPPET),
        )
    }

    private fun label(entry: JSONObject, key: String): String? =
        entry.optString(key).takeIf { entry.has(key) && it.isNotBlank() }?.take(MAX_LABEL_LENGTH)

    /**
     * Unites two lists of the same book (a fingerprint migration, or memory against disk): every
     * entry of [primary] keeps its id; entries of [secondary] whose place is not in [primary] are
     * appended with fresh ids, and the whole is capped to the newest [MAX_BOOKMARKS]. When one
     * side is empty the other is taken as it is (a plain move keeps its ids).
     */
    fun merge(primary: List<Bookmark>, secondary: List<Bookmark>): List<Bookmark> {
        if (secondary.isEmpty()) return primary.take(MAX_BOOKMARKS)
        if (primary.isEmpty()) return secondary.take(MAX_BOOKMARKS)
        var nextId = (primary.maxOfOrNull { it.id } ?: -1L) + 1
        val merged = ArrayList(primary)
        for (candidate in secondary) {
            if (merged.any { it.samePlaceAs(candidate) }) continue
            merged.add(candidate.copy(id = nextId++))
        }
        if (merged.size <= MAX_BOOKMARKS) return merged
        val newest = merged.sortedByDescending { it.createdAtMillis }.take(MAX_BOOKMARKS).map { it.id }.toHashSet()
        return merged.filter { it.id in newest }
    }
}
