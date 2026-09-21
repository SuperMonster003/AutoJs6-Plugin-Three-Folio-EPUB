package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import org.json.JSONObject

/**
 * Pure rules of the highlights and notes (roadmap P9), Android-free so JUnit covers them: the
 * caps, what counts as the same place, how a draft is normalized before it is stored, the order
 * the panel and the export use, and how two lists of the same book are united by the fingerprint
 * migration.
 */
internal object AnnotationPolicy {

    /** Per book; the store refuses to add beyond it. */
    const val MAX_PER_BOOK = 2000

    /** A note is display text and export text: anything longer is cut when stored. */
    const val MAX_NOTE_LENGTH = 4000

    /** The quote kept for the list and the export; the locator carries Readium's own (longer) text context. */
    const val MAX_QUOTE_LENGTH = 1000

    const val MAX_CHAPTER_LENGTH = 400

    private const val KEY_HREF = "href"
    private const val KEY_TYPE = "type"
    private const val KEY_LOCATIONS = "locations"
    private const val KEY_PROGRESSION = "progression"
    private const val KEY_TEXT = "text"
    private const val KEY_HIGHLIGHT = "highlight"

    private val WHITESPACE = Regex("\\s+")

    /** The `href` of a locator JSON, or null when the text is not a locator with an href and a type. */
    fun hrefOf(locatorJson: String): String? = runCatching {
        val json = JSONObject(locatorJson)
        json.optString(KEY_HREF).takeIf { it.isNotEmpty() && json.optString(KEY_TYPE).isNotEmpty() }
    }.getOrNull()

    /** `locations.progression` of a locator JSON, when it has one. */
    fun progressionOf(locatorJson: String): Double? = runCatching {
        JSONObject(locatorJson).optJSONObject(KEY_LOCATIONS)?.optDouble(KEY_PROGRESSION)?.takeIf { !it.isNaN() }
    }.getOrNull()

    /** `text.highlight` of a locator JSON: the selected text as Readium reported it. */
    fun quoteOf(locatorJson: String): String? = runCatching {
        JSONObject(locatorJson).optJSONObject(KEY_TEXT)?.optString(KEY_HIGHLIGHT)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /** The note trimmed and capped; null for nothing but whitespace. */
    fun trimNote(note: String?): String? = note?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_NOTE_LENGTH)

    /** The quote with its whitespace collapsed and capped; null for nothing but whitespace. */
    fun trimQuote(quote: String?): String? = quote?.replace(WHITESPACE, " ")?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_QUOTE_LENGTH)

    fun trimChapter(chapter: String?): String? = chapter?.replace(WHITESPACE, " ")?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_CHAPTER_LENGTH)

    /**
     * A draft made storable: the href taken from the locator, the style and colour normalized,
     * the texts trimmed, the quote falling back to the locator's own selected text, and both
     * timestamps set. Null when the locator is not one (no href or type).
     */
    fun normalize(draft: BookAnnotation, now: Long = draft.createdAt): BookAnnotation? {
        val href = hrefOf(draft.locator) ?: return null
        return draft.copy(
            href = href,
            style = AnnotationStyle.normalize(draft.style),
            color = AnnotationColors.normalize(draft.color),
            note = trimNote(draft.note),
            quote = trimQuote(draft.quote ?: quoteOf(draft.locator)),
            chapter = trimChapter(draft.chapter),
            createdAt = if (draft.createdAt > 0L) draft.createdAt else now,
            updatedAt = now,
        )
    }

    /** Two annotations of the same selection: same resource and the same locator text, whatever their ids, colours and notes. */
    fun samePlace(a: BookAnnotation, b: BookAnnotation): Boolean = a.href == b.href && a.locator == b.locator

    /**
     * Book order: by the resource's index in [readingOrder] (resources the book no longer has go
     * last, in href order), then by progression within it, then by creation time and id, so two
     * highlights of one paragraph list in the order they were made.
     */
    fun ordered(annotations: List<BookAnnotation>, readingOrder: List<String>): List<BookAnnotation> {
        val index = HashMap<String, Int>(readingOrder.size * 2)
        readingOrder.forEachIndexed { i, href -> index.putIfAbsent(href, i) }
        val progression = HashMap<Long, Double>()
        return annotations.sortedWith(
            compareBy<BookAnnotation> { index[it.href] ?: Int.MAX_VALUE }
                .thenBy { if (index.containsKey(it.href)) "" else it.href }
                .thenBy { progression.getOrPut(it.id) { progressionOf(it.locator) ?: 0.0 } }
                .thenBy { it.createdAt }
                .thenBy { it.id },
        )
    }

    /** Newest change first (the panel's "recent" order), ties broken by id. */
    fun newestFirst(annotations: List<BookAnnotation>): List<BookAnnotation> =
        annotations.sortedWith(compareByDescending<BookAnnotation> { it.updatedAt }.thenByDescending { it.id })

    /**
     * Unites two lists of the same book (a fingerprint migration): every entry of [primary]
     * stays; entries of [secondary] whose place is not in [primary] are added; the whole is
     * capped to the newest [MAX_PER_BOOK]. Returns the entries of [secondary] to keep.
     */
    fun mergeable(primary: List<BookAnnotation>, secondary: List<BookAnnotation>): List<BookAnnotation> {
        if (secondary.isEmpty()) return emptyList()
        val kept = ArrayList<BookAnnotation>()
        for (candidate in secondary) {
            if (primary.any { samePlace(it, candidate) } || kept.any { samePlace(it, candidate) }) continue
            kept.add(candidate)
        }
        val room = MAX_PER_BOOK - primary.size
        if (room <= 0) return emptyList()
        if (kept.size <= room) return kept
        return kept.sortedByDescending { it.createdAt }.take(room)
    }

    /** A single-line preview of [text] for a row: whitespace collapsed, cut at a word boundary. */
    fun preview(text: String?, limit: Int = PREVIEW_LENGTH): String? {
        val collapsed = text?.replace(WHITESPACE, " ")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (collapsed.length <= limit) return collapsed
        val cut = collapsed.lastIndexOf(' ', limit).takeIf { it >= limit - WORD_SLACK } ?: limit
        return collapsed.substring(0, cut).trimEnd() + ELLIPSIS
    }

    const val PREVIEW_LENGTH = 160
    private const val WORD_SLACK = 16
    private const val ELLIPSIS = "..."
}
