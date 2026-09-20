package io.github.supermonster003.autojs6.plugin.readium.epub.reader.update

import org.json.JSONException
import org.json.JSONObject

/** One published GitHub release (roadmap P4.3): what the update dialog shows and links to. */
internal data class ReleaseInfo(
    val tagName: String,
    val name: String?,
    val htmlUrl: String,
    val publishedAt: String?,
    val preRelease: Boolean,
    val notes: String?,
)

/**
 * Reads the GitHub Releases API answer and the plugin's own cache of it. Only an `https` page on
 * `github.com` is accepted as the release page, drafts are never a release, and the notes are
 * capped so a huge body cannot bloat the cache.
 */
internal object ReleaseInfoCodec {

    const val MAX_NOTES_LENGTH = 4000
    const val MAX_TITLE_LENGTH = 200
    private const val RELEASE_PAGE_PREFIX = "https://github.com/"

    /** The release described by a GitHub `releases/latest` (or `releases/<id>`) answer, or null when it is unusable. */
    fun fromGitHub(json: String): ReleaseInfo? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        if (root.optBoolean("draft", false)) return null
        val tag = root.optString("tag_name").trim().takeIf { it.isNotEmpty() && it.length <= MAX_TITLE_LENGTH } ?: return null
        val url = root.optString("html_url").trim().takeIf(::isReleasePage) ?: return null
        return ReleaseInfo(
            tagName = tag,
            name = root.optString("name").trim().takeIf { it.isNotEmpty() }?.take(MAX_TITLE_LENGTH),
            htmlUrl = url,
            publishedAt = root.optString("published_at").trim().takeIf { it.isNotEmpty() }?.take(MAX_TITLE_LENGTH),
            preRelease = root.optBoolean("prerelease", false),
            notes = root.optString("body").trim().takeIf { it.isNotEmpty() }?.take(MAX_NOTES_LENGTH),
        )
    }

    fun isReleasePage(url: String): Boolean =
        url.startsWith(RELEASE_PAGE_PREFIX) && url.length > RELEASE_PAGE_PREFIX.length && url.none { it.isWhitespace() }

    /** The cache form: the same fields under the same names, so the GitHub reader decodes it too. */
    fun encode(release: ReleaseInfo): String = JSONObject().apply {
        put("tag_name", release.tagName)
        put("name", release.name ?: JSONObject.NULL)
        put("html_url", release.htmlUrl)
        put("published_at", release.publishedAt ?: JSONObject.NULL)
        put("prerelease", release.preRelease)
        put("body", release.notes ?: JSONObject.NULL)
    }.toString()

    fun decode(text: String?): ReleaseInfo? = try {
        text?.let(::fromGitHub)
    } catch (_: JSONException) {
        null
    }
}
