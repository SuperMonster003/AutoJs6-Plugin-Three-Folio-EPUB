package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions

/** One validated request: the book, its parent directory (Explorer envelope only), a display name and its door. */
internal data class EpubReaderRequest(
    val documentUri: Uri,
    val parentUri: Uri?,
    val displayName: String,
    val entry: ReaderEntry = ReaderEntry.EXPLORER,
)

/**
 * Turns an Intent into the Android-free [RequestShape], lets [EpubRequestPolicy] apply the rules
 * of the door the receiver opens, and completes what needs Android types: the Explorer Action v2
 * envelope (action ID, protocol version, host version, source surface, parent URI and the two
 * ClipData items, roadmap P1.1) is checked field by field before any content is opened; the
 * launcher's explicit action (roadmap P4.1) and another app's `ACTION_VIEW` (roadmap P4.2) carry
 * only the document. A grant the sender no longer holds fails at `startActivity`, not here.
 */
internal object EpubReaderIntentPolicy {

    const val ACTION_OPEN_RECENT = EpubRequestPolicy.ACTION_OPEN_RECENT
    const val EXTRA_DISPLAY_NAME = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.DISPLAY_NAME"

    private val acceptedActionIds = setOf(ReadiumEpubReaderPlugin.ID, ReadiumEpubReaderPlugin.PRIMARY_ACTION_ID)

    /**
     * [suppliedName] is the display name the receiver looked up itself (the viewer asks the
     * provider); the Explorer envelope and the launcher carry theirs in an extra.
     */
    fun resolve(
        intent: Intent,
        receiver: RequestReceiver = RequestReceiver.READER,
        suppliedName: String? = null,
    ): EpubReaderRequest? {
        val data = intent.data
        val shape = RequestShape(
            action = intent.action,
            explicit = intent.component?.className == receiverClassName(receiver),
            scheme = data?.scheme,
            authority = data?.authority,
            pathSegments = data?.pathSegments.orEmpty(),
            hasQueryOrFragment = data?.query != null || data?.fragment != null,
            readGrant = intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
            prefixGrant = intent.flags and Intent.FLAG_GRANT_PREFIX_URI_PERMISSION != 0,
            clipItems = intent.clipData?.itemCount ?: 0,
            mimeType = intent.type,
            suppliedName = suppliedName ?: when (intent.action) {
                ExplorerActionPluginActions.EXECUTE -> intent.getStringExtra(ExplorerActionIntentExtras.DISPLAY_NAME)
                ACTION_OPEN_RECENT -> intent.getStringExtra(EXTRA_DISPLAY_NAME)
                else -> null
            },
        )
        val admission = EpubRequestPolicy.admit(receiver, shape) ?: return null
        val documentUri = data ?: return null
        return when (admission.entry) {
            ReaderEntry.EXPLORER -> completeExplorer(intent, documentUri, admission.displayName)
            ReaderEntry.LAUNCHER -> EpubReaderRequest(documentUri, null, admission.displayName, ReaderEntry.LAUNCHER)
            ReaderEntry.EXTERNAL -> EpubReaderRequest(documentUri, null, admission.displayName, ReaderEntry.EXTERNAL)
        }
    }

    private fun receiverClassName(receiver: RequestReceiver): String = when (receiver) {
        RequestReceiver.READER -> ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME
        RequestReceiver.EXTERNAL_VIEWER -> ReadiumEpubReaderPlugin.EXTERNAL_VIEWER_CLASS_NAME
    }

    /** The rest of the v2 envelope: identity of the action, protocol and host, the parent directory and the ClipData contents. */
    private fun completeExplorer(intent: Intent, documentUri: Uri, displayName: String): EpubReaderRequest? {
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

        val parentUri = intent.parcelableUriExtra(ExplorerActionIntentExtras.PARENT_URI)
            ?.takeIf(::isPlainContentUri)
            ?: return null
        if (!EpubReaderPathPolicy.isDescendant(parentUri, documentUri)) return null

        val clipData = intent.clipData ?: return null
        if (clipData.getItemAt(ExplorerActionIntentValues.CLIP_ITEM_TARGET_INDEX).uri != documentUri) return null
        if (clipData.getItemAt(ExplorerActionIntentValues.CLIP_ITEM_PARENT_INDEX).uri != parentUri) return null

        return EpubReaderRequest(documentUri, parentUri, displayName, ReaderEntry.EXPLORER)
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
