package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

/**
 * What the reader does with a link that leaves the book (roadmap P2.7, decision D25): web links
 * go to the browser (after confirmation unless the setting says otherwise), mail and phone
 * links go to whatever the system offers, and every other scheme is refused.
 */
internal sealed class ExternalLink {
    abstract val url: String

    data class Web(override val url: String) : ExternalLink()
    data class System(override val url: String) : ExternalLink()
    data class Rejected(override val url: String) : ExternalLink()
}

internal object LinkPolicy {

    private val WEB_SCHEMES = setOf("http", "https")
    private val SYSTEM_SCHEMES = setOf("mailto", "tel")

    fun classifyExternal(url: String): ExternalLink {
        val scheme = url.substringBefore(':', "").trim().lowercase()
        return when (scheme) {
            in WEB_SCHEMES -> ExternalLink.Web(url)
            in SYSTEM_SCHEMES -> ExternalLink.System(url)
            else -> ExternalLink.Rejected(url)
        }
    }
}

/**
 * Where the reader was before each in-book link it followed, oldest first; the back key pops
 * the newest. Bounded so a book full of cross references cannot grow it without limit.
 */
internal class LinkHistory<T>(private val capacity: Int = DEFAULT_CAPACITY) {

    private val entries = ArrayDeque<T>()

    val size: Int get() = entries.size

    fun isEmpty(): Boolean = entries.isEmpty()

    fun push(entry: T) {
        entries.addLast(entry)
        while (entries.size > capacity) entries.removeFirst()
    }

    fun pop(): T? = entries.removeLastOrNull()

    fun clear() = entries.clear()

    companion object {
        const val DEFAULT_CAPACITY = 20
    }
}
