package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontEntry
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.FontStore
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.resource.Resource

/**
 * Serves the imported fonts (roadmap P2.2) to the navigator's WebView as if they were resources of
 * the book: `https://readium_package/fonts/<sha256>.<ext>`. Readium answers requests for that host
 * from the publication's container, and this container is composed after the book's own, so a
 * book resource of the same name always wins. Lookups go to disk on every request, which lets a
 * font imported while the book is open be served without reopening the book.
 */
internal class FontsContainer(private val store: FontStore) : Container<Resource> {

    override val entries: Set<Url> = store.read().fonts.mapNotNull { Url(DIRECTORY + it.fileName) }.toSet()

    override fun get(url: Url): Resource? {
        val path = url.path?.removePrefix("/") ?: return null
        if (!path.startsWith(DIRECTORY)) return null
        val file = store.fileNamed(path.removePrefix(DIRECTORY)) ?: return null
        return FileResource(file)
    }

    override fun close() = Unit

    companion object {
        private const val DIRECTORY = "fonts/"

        /** Readium's package host (`WebViewServer.PACKAGE_HOSTNAME`), under which the book's resources are served. */
        private const val PACKAGE_BASE = "https://readium_package/"

        /** Where the `@font-face` points; absolute, so Readium's asset URL normalizer leaves it alone. */
        fun urlFor(entry: FontEntry): AbsoluteUrl = requireNotNull(AbsoluteUrl(PACKAGE_BASE + DIRECTORY + entry.fileName))
    }
}
