package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import org.autojs.plugin.epub.api.EpubContract
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression

/** A capped list and whether entries beyond the cap were left out. */
internal data class CappedArray(val array: JSONArray, val hasMore: Boolean)

/**
 * The JSON documents of the EPUB contract (roadmap P5.2, appendix A): metadata, the flattened
 * table of contents, the reading order and one search result, built from the open Readium
 * publication with the field names of `EpubContract`.
 */
internal object BookJson {

    /** Longest description carried; the metadata document stays far below `MAX_METADATA_BYTES`. */
    const val MAX_DESCRIPTION_CHARS = 65_536

    /** [positions] is the publication's position count, computed by the caller (a suspend call in Readium). */
    fun metadata(publication: Publication, positions: Int): JSONObject {
        val metadata = publication.metadata
        return JSONObject().apply {
            put(EpubContract.FIELD_TITLE, metadata.title.orEmpty())
            put(EpubContract.FIELD_AUTHORS, JSONArray(metadata.authors.map { it.name }))
            metadata.languages.firstOrNull()?.takeIf { it.isNotBlank() }?.let { put(EpubContract.FIELD_LANGUAGE, it) }
            metadata.identifier?.takeIf { it.isNotBlank() }?.let { put(EpubContract.FIELD_IDENTIFIER, it) }
            metadata.publishers.firstOrNull()?.name?.takeIf { it.isNotBlank() }?.let { put(EpubContract.FIELD_PUBLISHER, it) }
            metadata.published?.let { put(EpubContract.FIELD_PUBLISHED, it.toString()) }
            metadata.modified?.let { put(EpubContract.FIELD_MODIFIED, it.toString()) }
            metadata.description?.takeIf { it.isNotBlank() }?.let { put(EpubContract.FIELD_DESCRIPTION, it.take(MAX_DESCRIPTION_CHARS)) }
            put(EpubContract.FIELD_SUBJECTS, JSONArray(metadata.subjects.map { it.name }))
            put(
                EpubContract.FIELD_LAYOUT,
                if (metadata.layout == Layout.FIXED) EpubContract.LAYOUT_FIXED else EpubContract.LAYOUT_REFLOWABLE,
            )
            put(
                EpubContract.FIELD_READING_PROGRESSION,
                when (metadata.readingProgression) {
                    ReadingProgression.RTL -> EpubContract.READING_PROGRESSION_RTL
                    ReadingProgression.LTR -> EpubContract.READING_PROGRESSION_LTR
                    else -> EpubContract.READING_PROGRESSION_AUTO
                },
            )
            publication.linkWithRel("cover")?.let { put(EpubContract.FIELD_COVER, it.href.toString()) }
            put(EpubContract.FIELD_POSITIONS, positions)
        }
    }

    /** Flattened table of contents: `{title, href, depth}` rows in document order, at most [maxEntries]. */
    fun toc(publication: Publication, maxEntries: Int = EpubContract.MAX_TOC_ENTRIES): CappedArray {
        val rows = TocFlattener.flatten(publication.tableOfContents, Link::children, maxEntries = maxEntries + 1)
        val array = JSONArray()
        for (row in rows.take(maxEntries)) {
            array.put(
                JSONObject().apply {
                    put(EpubContract.FIELD_TITLE, row.node.title.orEmpty())
                    put(EpubContract.FIELD_HREF, row.node.href.toString())
                    put(EpubContract.FIELD_DEPTH, row.depth)
                },
            )
        }
        return CappedArray(array, hasMore = rows.size > maxEntries)
    }

    /** Reading order: `{href, type, title?}` per spine item, at most [maxEntries]. */
    fun readingOrder(publication: Publication, maxEntries: Int = EpubContract.MAX_READING_ORDER_ENTRIES): CappedArray {
        val links = publication.readingOrder
        val array = JSONArray()
        for (link in links.take(maxEntries)) {
            array.put(
                JSONObject().apply {
                    put(EpubContract.FIELD_HREF, link.href.toString())
                    put(EpubContract.FIELD_TYPE, link.mediaType?.toString().orEmpty())
                    link.title?.takeIf { it.isNotBlank() }?.let { put(EpubContract.FIELD_TITLE, it) }
                },
            )
        }
        return CappedArray(array, hasMore = links.size > maxEntries)
    }

    /** One search hit: `{href, title?, locator, text{before, highlight, after}}`. */
    fun searchResult(locator: Locator): JSONObject = JSONObject().apply {
        put(EpubContract.FIELD_HREF, locator.href.toString())
        locator.title?.takeIf { it.isNotBlank() }?.let { put(EpubContract.FIELD_TITLE, it) }
        put(EpubContract.FIELD_LOCATOR, locator.toJSON())
        put(
            EpubContract.FIELD_TEXT,
            JSONObject().apply {
                put(EpubContract.FIELD_BEFORE, locator.text.before.orEmpty())
                put(EpubContract.FIELD_HIGHLIGHT, locator.text.highlight.orEmpty())
                put(EpubContract.FIELD_AFTER, locator.text.after.orEmpty())
            },
        )
    }
}
