package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.json.JSONException
import org.json.JSONObject

/**
 * A position the host names for `openReader` or `goTo` (roadmap P5.3): a Readium locator
 * document, a resource href with an optional fragment, or a total progression in `0..1`; the
 * first present one wins in that order. Only the shape is checked here; whether the place exists
 * in the book is decided against the publication by the session.
 */
internal data class SessionTarget(
    val locator: JSONObject? = null,
    val href: String? = null,
    val progression: Double? = null,
) {

    companion object {

        /** Null when nothing was named; the shape checks throw [ContractViolation]. */
        fun parse(locatorJson: String?, href: String?, progression: Double?): SessionTarget? {
            val locator = locatorJson?.let { text ->
                if (text.toByteArray(Charsets.UTF_8).size > EpubContract.MAX_LOCATOR_BYTES) {
                    throw ContractViolation(EpubErrorCodes.LIMIT_EXCEEDED, "locator exceeds ${EpubContract.MAX_LOCATOR_BYTES} bytes")
                }
                val json = try {
                    JSONObject(text)
                } catch (e: JSONException) {
                    throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "locator is not a JSON object")
                }
                if (json.optString(EpubContract.FIELD_HREF).isBlank()) {
                    throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "locator has no href")
                }
                json
            }
            val cleanHref = href?.let { Limits.href(it) }
            if (progression != null && (!progression.isFinite() || progression < 0.0 || progression > 1.0)) {
                throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "progression must be within 0..1")
            }
            if (locator == null && cleanHref == null && progression == null) return null
            return SessionTarget(locator, cleanHref, progression)
        }
    }
}
