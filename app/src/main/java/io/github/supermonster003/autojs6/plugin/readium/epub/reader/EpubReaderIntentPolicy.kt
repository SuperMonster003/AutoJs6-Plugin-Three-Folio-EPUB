package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import java.util.Locale

/**
 * Which door a request came through (roadmap P4): the host's Explorer Action envelope, the
 * plugin's own launcher (a document the user granted through the system picker, roadmap D4), or
 * another app's `ACTION_VIEW` (roadmap D27). The reader behaves the same behind every door; only
 * the recent list follows the door (the launcher's books are listed, the others are not).
 */
internal enum class ReaderEntry { EXPLORER, LAUNCHER, EXTERNAL }

/** One validated request: the book, its parent directory (Explorer envelope only) and a display name. */
internal data class EpubReaderRequest(
    val documentUri: Uri,
    val parentUri: Uri?,
    val displayName: String,
    val entry: ReaderEntry = ReaderEntry.EXPLORER,
)

/**
 * Validates the complete, URI-only Explorer Action v2 envelope before any content is opened, and
 * the launcher's explicit open request (roadmap P4.1): the plugin's own component, a read grant,
 * a plain `content://` document and a display name; the launcher only sends documents it lists or
 * has just been granted, and a grant it no longer holds fails at `startActivity`, not here.
 */
internal object EpubReaderIntentPolicy {

    /** The launcher opens a recent or freshly picked book with this explicit action (roadmap P4.1). */
    const val ACTION_OPEN_RECENT = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.OPEN_RECENT"
    const val EXTRA_DISPLAY_NAME = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.DISPLAY_NAME"

    private const val MAX_DISPLAY_NAME_LENGTH = 255

    private val acceptedActionIds = setOf(ReadiumEpubReaderPlugin.ID, ReadiumEpubReaderPlugin.PRIMARY_ACTION_ID)
    private val epubMimeTypes = setOf(ReadiumEpubReaderPlugin.EPUB_MIME_TYPE)
    // application/zip is not listed: an EPUB is a ZIP container, so a host that sniffed the magic
    // number may legitimately report it for a `.epub` file (roadmap P1.1).
    private val conflictingMimeTypes = setOf(
        "application/x-cbz",
        "application/vnd.comicbook+zip",
        "application/pdf",
    )

    fun resolve(intent: Intent): EpubReaderRequest? = when (intent.action) {
        ExplorerActionPluginActions.EXECUTE -> resolveExplorer(intent)
        ACTION_OPEN_RECENT -> resolveRecent(intent)
        else -> null
    }

    private fun resolveExplorer(intent: Intent): EpubReaderRequest? {
        if (intent.getStringExtra(ExplorerActionIntentExtras.ACTION_ID) !in acceptedActionIds) return null
        if (
            intent.getIntExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, Int.MIN_VALUE) !=
            ReadiumEpubReaderPlugin.PROTOCOL_VERSION
        ) {
            return null
        }
        val hostVersionCode = intent.getLongExtra(
            ExplorerActionIntentExtras.HOST_VERSION_CODE,
            Long.MIN_VALUE,
        )
        if (!EpubReaderExplorerCompatibility.acceptsHostVersionCode(hostVersionCode)) return null
        if (
            intent.getStringExtra(ExplorerActionIntentExtras.SOURCE_SURFACE) !=
            ExplorerActionIntentValues.SOURCE_SURFACE_MAIN
        ) {
            return null
        }

        val requiredFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        if (intent.flags and requiredFlags != requiredFlags) return null

        val documentUri = intent.data?.takeIf(::isPlainContentUri) ?: return null
        val parentUri = intent.parcelableUriExtra(ExplorerActionIntentExtras.PARENT_URI)
            ?.takeIf(::isPlainContentUri)
            ?: return null
        if (!EpubReaderPathPolicy.isDescendant(parentUri, documentUri)) return null

        val clipData = intent.clipData ?: return null
        if (clipData.itemCount != EpubReaderExplorerCompatibility.LEGACY_CLIP_ITEM_COUNT) return null
        if (clipData.getItemAt(ExplorerActionIntentValues.CLIP_ITEM_TARGET_INDEX).uri != documentUri) return null
        if (clipData.getItemAt(ExplorerActionIntentValues.CLIP_ITEM_PARENT_INDEX).uri != parentUri) return null

        val suppliedName = intent.getStringExtra(ExplorerActionIntentExtras.DISPLAY_NAME)
        val displayName = sanitizeDisplayName(suppliedName)
            ?: sanitizeDisplayName(documentUri.lastPathSegment)
            ?: return null
        if (!isSupportedEpub(intent.type, displayName)) return null

        return EpubReaderRequest(documentUri, parentUri, displayName, ReaderEntry.EXPLORER)
    }

    /** The launcher's request (roadmap P4.1): explicit component, read grant, plain content document, EPUB by name or type. */
    private fun resolveRecent(intent: Intent): EpubReaderRequest? {
        if (intent.component?.className != ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME) return null
        if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION == 0) return null
        if (intent.clipData != null) return null
        val documentUri = intent.data?.takeIf(::isPlainContentUri) ?: return null
        if (documentUri.lastPathSegment.isNullOrEmpty()) return null
        val displayName = sanitizeDisplayName(intent.getStringExtra(EXTRA_DISPLAY_NAME))
            ?: sanitizeDisplayName(documentUri.lastPathSegment)
            ?: return null
        if (!isSupportedEpub(intent.type, displayName)) return null
        return EpubReaderRequest(documentUri, null, displayName, ReaderEntry.LAUNCHER)
    }

    /**
     * Accepts a `.epub` extension (whatever the MIME type, including the generic
     * `application/zip`), or an extensionless file the host marked as `application/epub+zip`.
     * Any other extension, and any MIME type that names a different document format (comic
     * archive, PDF), is rejected even when the extension looks right.
     */
    fun isSupportedEpub(mimeType: String?, displayName: String): Boolean {
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
        val hasEpubExtension = extension == ReadiumEpubReaderPlugin.EPUB_EXTENSION
        val hasUnsupportedExtension = extension.isNotEmpty() && !hasEpubExtension
        val normalizedMimeType = mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)

        if (hasUnsupportedExtension) return false
        if (normalizedMimeType in conflictingMimeTypes) return false
        return hasEpubExtension || normalizedMimeType in epubMimeTypes
    }

    fun sanitizeDisplayName(value: String?): String? {
        val leaf = value
            ?.replace('\\', '/')
            ?.substringAfterLast('/')
            ?.filterNot { it.code < 0x20 || it.code == 0x7f }
            ?.trim()
            ?.take(MAX_DISPLAY_NAME_LENGTH)
            .orEmpty()
        return leaf.takeIf(String::isNotEmpty)
    }

    fun isPlainContentUri(uri: Uri): Boolean =
        uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true) &&
            !uri.authority.isNullOrBlank() &&
            uri.query == null &&
            uri.fragment == null

    @Suppress("DEPRECATION")
    private fun Intent.parcelableUriExtra(name: String): Uri? = getParcelableExtra(name)
}

/** Parent / child relationship of the two content URIs in the v2 envelope; the parent is never read. */
internal object EpubReaderPathPolicy {

    fun normalizeRoot(rootUri: Uri): Uri? {
        if (!rootUri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true)) return null
        if (rootUri.authority.isNullOrBlank() || rootUri.query != null || rootUri.fragment != null) return null
        return rootUri.buildUpon().clearQuery().fragment(null).build()
    }

    fun isDescendant(rootUri: Uri, candidateUri: Uri): Boolean {
        val root = normalizeRoot(rootUri) ?: return false
        if (!candidateUri.scheme.equals(root.scheme, ignoreCase = true)) return false
        if (!candidateUri.authority.equals(root.authority, ignoreCase = true)) return false
        if (candidateUri.query != null || candidateUri.fragment != null) return false
        val rootSegments = root.pathSegments
        val candidateSegments = candidateUri.pathSegments
        return candidateSegments.size > rootSegments.size &&
            candidateSegments.take(rootSegments.size) == rootSegments &&
            candidateSegments.drop(rootSegments.size).all(::isSafeUriSegment)
    }

    private fun isSafeUriSegment(segment: String): Boolean =
        segment.isNotEmpty() &&
            segment != "." &&
            segment != ".." &&
            segment.none { it == '/' || it == '\\' || it.code < 0x20 || it.code == 0x7f }
}
