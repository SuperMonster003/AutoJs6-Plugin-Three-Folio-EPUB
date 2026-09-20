package io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * One entry of the launcher's recent list (roadmap P4.1): the document URI the user granted through
 * the system document picker, what the picker said about it, and what the reader learned once the
 * book was open. Android-free so JUnit can lock the codec and the limit rules.
 */
internal data class RecentBook(
    /** The persisted `content://` document URI, as a string. */
    val uri: String,
    val displayName: String,
    val addedAt: Long,
    val lastReadAt: Long,
    /** The content fingerprint once the reader computed it (the key of the book's progress and bookmarks). */
    val key: String? = null,
    val title: String? = null,
    val author: String? = null,
    /** The file name of the cover thumbnail under `files/covers/`, once extracted. */
    val coverFile: String? = null,
    /** Total progression 0..1 of the last saved position, null before the first page turn. */
    val progression: Double? = null,
    /** False after an open failed (file moved, deleted or its grant revoked); the launcher offers to clean it up. */
    val available: Boolean = true,
)

/** JSON envelope of the recent list: `{"format": 1, "books": [...]}`, newest first. */
internal object RecentBooksCodec {

    const val FORMAT = 1

    fun encode(books: List<RecentBook>): String {
        val array = JSONArray()
        for (book in books) {
            array.put(
                JSONObject().apply {
                    put("uri", book.uri)
                    put("displayName", book.displayName)
                    put("addedAt", book.addedAt)
                    put("lastReadAt", book.lastReadAt)
                    book.key?.let { put("key", it) }
                    book.title?.let { put("title", it) }
                    book.author?.let { put("author", it) }
                    book.coverFile?.let { put("coverFile", it) }
                    book.progression?.let { put("progression", it) }
                    put("available", book.available)
                },
            )
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("books", array)
        }.toString()
    }

    /** Null for corrupt text or an unknown format; entries without a usable URI or name are dropped. */
    fun decode(text: String): List<RecentBook>? {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (root.optInt("format", -1) != FORMAT) return null
        val array = root.optJSONArray("books") ?: return null
        val books = ArrayList<RecentBook>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val uri = item.optString("uri").takeIf { it.isNotBlank() } ?: continue
            val displayName = item.optString("displayName").takeIf { it.isNotBlank() } ?: continue
            books += RecentBook(
                uri = uri,
                displayName = displayName,
                addedAt = item.optLong("addedAt", 0L),
                lastReadAt = item.optLong("lastReadAt", 0L),
                key = item.optStringOrNull("key"),
                title = item.optStringOrNull("title"),
                author = item.optStringOrNull("author"),
                coverFile = item.optStringOrNull("coverFile"),
                progression = if (item.has("progression") && !item.isNull("progression")) {
                    runCatching { item.getDouble("progression") }.getOrNull()?.takeIf { it in 0.0..1.0 }
                } else {
                    null
                },
                available = item.optBoolean("available", true),
            )
        }
        return books
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (has(name) && !isNull(name)) {
            try {
                getString(name).takeIf { it.isNotBlank() }
            } catch (_: JSONException) {
                null
            }
        } else {
            null
        }
}

/** The list rules: newest first, one entry per URI, at most [LIMIT] entries. */
internal object RecentBooksPolicy {

    const val LIMIT = 100

    /** The list after an upsert plus the entries that fell off the end (their covers and grants can be released). */
    data class Update(val books: List<RecentBook>, val evicted: List<RecentBook>)

    /** Newest reading first; entries never read sort by when they were added. */
    fun sorted(books: List<RecentBook>): List<RecentBook> =
        books.sortedWith(compareByDescending<RecentBook> { maxOf(it.lastReadAt, it.addedAt) }.thenByDescending { it.addedAt })

    /**
     * Replaces the entry with the same URI (keeping what the new one leaves null: fingerprint,
     * metadata, cover, progression), then trims the oldest entries beyond the limit.
     */
    fun upsert(books: List<RecentBook>, book: RecentBook): Update {
        val existing = books.firstOrNull { it.uri == book.uri }
        val merged = if (existing == null) {
            book
        } else {
            book.copy(
                addedAt = existing.addedAt,
                key = book.key ?: existing.key,
                title = book.title ?: existing.title,
                author = book.author ?: existing.author,
                coverFile = book.coverFile ?: existing.coverFile,
                progression = book.progression ?: existing.progression,
            )
        }
        val ordered = sorted(books.filterNot { it.uri == book.uri } + merged)
        return Update(ordered.take(LIMIT), ordered.drop(LIMIT))
    }

    fun remove(books: List<RecentBook>, uri: String): List<RecentBook> = books.filterNot { it.uri == uri }

    /** The whole-number percentage shown on the tile; null before the first saved position. */
    fun progressPercent(progression: Double?): Int? =
        progression?.takeIf { it in 0.0..1.0 }?.let { (it * 100).toInt().coerceIn(0, 100) }
}
