package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import org.json.JSONArray
import org.json.JSONObject

/**
 * The JSON shape of an annotation as the host contract and the script API see it (roadmap P9,
 * contract version 2): `{ id, style, color: "#RRGGBB", note?, quote?, title?, locator,
 * createdAt, updatedAt }`. The locator is the Readium locator object, the title is the chapter
 * title captured at creation. Android-free.
 */
internal object AnnotationJson {

    const val FIELD_ID = "id"
    const val FIELD_STYLE = "style"
    const val FIELD_COLOR = "color"
    const val FIELD_NOTE = "note"
    const val FIELD_QUOTE = "quote"
    const val FIELD_TITLE = "title"
    const val FIELD_LOCATOR = "locator"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_UPDATED_AT = "updatedAt"

    fun toJson(annotation: BookAnnotation): JSONObject = JSONObject().apply {
        put(FIELD_ID, annotation.id)
        put(FIELD_STYLE, annotation.style)
        put(FIELD_COLOR, AnnotationColors.hex(annotation.color))
        annotation.note?.let { put(FIELD_NOTE, it) }
        annotation.quote?.let { put(FIELD_QUOTE, it) }
        annotation.chapter?.let { put(FIELD_TITLE, it) }
        put(FIELD_LOCATOR, runCatching { JSONObject(annotation.locator) }.getOrDefault(JSONObject()))
        put(FIELD_CREATED_AT, annotation.createdAt)
        put(FIELD_UPDATED_AT, annotation.updatedAt)
    }

    fun toJsonArray(annotations: List<BookAnnotation>): JSONArray =
        JSONArray().also { array -> annotations.forEach { array.put(toJson(it)) } }
}
