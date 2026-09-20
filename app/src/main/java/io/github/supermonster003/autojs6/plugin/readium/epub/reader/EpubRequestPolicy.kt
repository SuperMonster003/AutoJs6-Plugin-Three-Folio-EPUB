package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import java.util.Locale

/**
 * Which door a request came through (roadmap P4): the host's Explorer Action envelope, the
 * plugin's own launcher (a document the user granted through the system picker, roadmap D4), or
 * another app's `ACTION_VIEW` (roadmap D27). The reader behaves the same behind every door; only
 * the recent list follows the door (the launcher's books are listed, the others are not unless
 * the user adds them).
 */
internal enum class ReaderEntry { EXPLORER, LAUNCHER, EXTERNAL }

/**
 * Which component received the request: the plugin-protected reader (the Explorer envelope and
 * the launcher's explicit action) or the exported viewer (`ACTION_VIEW` only, roadmap P4.2).
 */
internal enum class RequestReceiver { READER, EXTERNAL_VIEWER }

/**
 * The Android-free shape of an incoming request, extracted from the Intent by
 * [EpubReaderIntentPolicy] so the rules of the three doors can be tested on the JVM.
 */
internal data class RequestShape(
    val action: String?,
    /** The Intent names the receiving component's class (an explicit Intent). */
    val explicit: Boolean,
    val scheme: String?,
    val authority: String?,
    val pathSegments: List<String>,
    val hasQueryOrFragment: Boolean,
    val readGrant: Boolean,
    val prefixGrant: Boolean,
    /** ClipData item count, 0 without ClipData. */
    val clipItems: Int,
    val mimeType: String?,
    /** The display name the sender supplied (an extra, or the provider's `_display_name`). */
    val suppliedName: String?,
)

/** A request that passed the rules of its door: the door and the display name to show. */
internal data class RequestAdmission(val entry: ReaderEntry, val displayName: String)

/**
 * The rules shared by the three doors and what tells them apart (roadmap P4.2): every door takes
 * a plain `content://` document with a non-empty leaf and a read grant, and an EPUB by name or
 * type; the Explorer envelope also needs the prefix grant and the two-item ClipData of the v2
 * protocol, the launcher's action must be an explicit Intent without ClipData, and the external
 * door rejects a bare document tree. `file://` and any other scheme never pass. The receiver
 * decides which actions exist at all, so an Explorer envelope aimed at the exported viewer and an
 * `ACTION_VIEW` aimed at the protected reader are both refused before their contents are read.
 */
internal object EpubRequestPolicy {

    /** The launcher opens a recent or freshly picked book with this explicit action (roadmap P4.1). */
    const val ACTION_OPEN_RECENT = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.OPEN_RECENT"
    /** `Intent.ACTION_VIEW`, spelled out so the policy stays Android-free. */
    const val ACTION_VIEW = "android.intent.action.VIEW"

    private const val SCHEME_CONTENT = "content"
    private const val MAX_DISPLAY_NAME_LENGTH = 255
    private const val TREE_SEGMENT = "tree"

    private val epubMimeTypes = setOf(ReadiumEpubReaderPlugin.EPUB_MIME_TYPE)
    // application/zip is not listed: an EPUB is a ZIP container, so a host that sniffed the magic
    // number may legitimately report it for a `.epub` file (roadmap P1.1).
    private val conflictingMimeTypes = setOf(
        "application/x-cbz",
        "application/vnd.comicbook+zip",
        "application/pdf",
        "vnd.android.document/directory",
    )

    /** The door the receiver opens for this action, or null when the receiver has no such door. */
    fun entryFor(receiver: RequestReceiver, action: String?): ReaderEntry? = when (receiver) {
        RequestReceiver.READER -> when (action) {
            ExplorerActionPluginActions.EXECUTE -> ReaderEntry.EXPLORER
            ACTION_OPEN_RECENT -> ReaderEntry.LAUNCHER
            else -> null
        }
        RequestReceiver.EXTERNAL_VIEWER -> when (action) {
            ACTION_VIEW -> ReaderEntry.EXTERNAL
            else -> null
        }
    }

    fun admit(receiver: RequestReceiver, shape: RequestShape): RequestAdmission? {
        val entry = entryFor(receiver, shape.action) ?: return null
        if (!isContentDocument(shape)) return null
        if (!shape.readGrant) return null
        when (entry) {
            ReaderEntry.EXPLORER -> {
                if (!shape.prefixGrant) return null
                if (shape.clipItems != EpubReaderExplorerCompatibility.LEGACY_CLIP_ITEM_COUNT) return null
            }
            ReaderEntry.LAUNCHER -> {
                if (!shape.explicit) return null
                if (shape.clipItems != 0) return null
            }
            ReaderEntry.EXTERNAL -> {
                if (isDocumentTree(shape.pathSegments)) return null
            }
        }
        val displayName = sanitizeDisplayName(shape.suppliedName)
            ?: sanitizeDisplayName(shape.pathSegments.lastOrNull())
            ?: return null
        if (!isSupportedEpub(shape.mimeType, displayName)) return null
        return RequestAdmission(entry, displayName)
    }

    /** A `content://` URI with an authority, a non-empty leaf segment and neither query nor fragment. */
    private fun isContentDocument(shape: RequestShape): Boolean =
        shape.scheme.equals(SCHEME_CONTENT, ignoreCase = true) &&
            !shape.authority.isNullOrBlank() &&
            !shape.hasQueryOrFragment &&
            !shape.pathSegments.lastOrNull().isNullOrEmpty()

    /** `content://<authority>/tree/<id>` names a directory; a document inside a tree has more segments. */
    private fun isDocumentTree(segments: List<String>): Boolean =
        segments.size == 2 && segments[0] == TREE_SEGMENT

    /**
     * Accepts a `.epub` extension (whatever the MIME type, including the generic
     * `application/zip`), or an extensionless file the sender marked as `application/epub+zip`.
     * Any other extension, and any MIME type that names a different document format (comic
     * archive, PDF, a document directory), is rejected even when the extension looks right.
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
}
