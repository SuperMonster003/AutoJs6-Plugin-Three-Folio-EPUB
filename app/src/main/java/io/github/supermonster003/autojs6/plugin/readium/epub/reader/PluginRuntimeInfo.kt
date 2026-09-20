package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.Context
import android.os.Build
import android.os.Bundle
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.explorer.api.ExplorerActionCapabilityKeys
import org.autojs.plugin.explorer.api.ExplorerActionCatalogKeys
import org.autojs.plugin.explorer.api.ExplorerActionPluginIds
import org.autojs.plugin.explorer.api.ExplorerActionValues

/**
 * Plugin identity (roadmap D1 / D10). The Gradle `resValue` entries in app/build.gradle.kts and
 * `.readme/common.json` must carry the same values.
 */
internal object ReadiumEpubReaderPlugin {
    const val ID = "readium-epub-reader"
    const val PRIMARY_ACTION_ID = "$ID.primary"
    // Explorer Action v2 adds primary placement to the unchanged single-file envelope.
    const val PRIMARY_PLACEMENT = 2
    const val ACTION_PRIORITY = 100
    const val VARIANT = "default"
    const val LABEL_RESOURCE_NAME = "action_readium_epub_reader"
    const val LABEL_FALLBACK = "Readium EPUB Reader"
    const val ACTIVITY_CLASS_NAME =
        "io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity"
    /** The exported `ACTION_VIEW` door (roadmap P4.2); not part of the Explorer catalog. */
    const val EXTERNAL_VIEWER_CLASS_NAME =
        "io.github.supermonster003.autojs6.plugin.readium.epub.reader.ExternalViewerActivity"

    const val EPUB_MIME_TYPE = "application/epub+zip"
    const val EPUB_EXTENSION = "epub"

    val REQUIRED_HOST_VERSION = EpubReaderExplorerCompatibility.minimumHostVersionCode
    val PROTOCOL_VERSION = EpubReaderExplorerCompatibility.declaredProtocolVersion

    val MIME_TYPES = arrayOf(EPUB_MIME_TYPE)
    val EXTENSIONS = arrayOf(EPUB_EXTENSION)

    /**
     * The two catalog actions (roadmap P1.1): the primary button and the overflow entry share
     * every attribute except their ID and placement. Android-free so JUnit can lock the shape.
     */
    fun actionSpecs(): List<ExplorerActionSpec> = listOf(
        actionSpec(PRIMARY_ACTION_ID, PRIMARY_PLACEMENT),
        actionSpec(ID, ExplorerActionValues.PLACEMENT_OVERFLOW),
    )

    private fun actionSpec(id: String, placement: Int) = ExplorerActionSpec(
        id = id,
        placement = placement,
        priority = ACTION_PRIORITY,
        targetKind = ExplorerActionValues.TARGET_FILE,
        accessMode = ExplorerActionValues.ACCESS_READ_ONLY,
        mimeTypes = MIME_TYPES.asList(),
        extensions = EXTENSIONS.asList(),
        labelResourceName = LABEL_RESOURCE_NAME,
        labelFallback = LABEL_FALLBACK,
        activityClassName = ACTIVITY_CLASS_NAME,
    )
}

/** One catalog action as pure data; [readiumEpubReaderActionCatalog] turns it into the host Bundle. */
internal data class ExplorerActionSpec(
    val id: String,
    val placement: Int,
    val priority: Int,
    val targetKind: Int,
    val accessMode: Int,
    val mimeTypes: List<String>,
    val extensions: List<String>,
    val labelResourceName: String,
    val labelFallback: String,
    val activityClassName: String,
)

internal fun Context.readiumEpubReaderPluginInfo(): PluginInfo {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return PluginInfo().apply {
        name = getString(R.string.app_name)
        description = getString(R.string.plugin_description)
        instruction = resources.openRawResource(R.raw.plugin_instruction).use { input ->
            input.bufferedReader(Charsets.UTF_8).readText().trim()
        }
        author = getString(R.string.plugin_author)
        collaborators = null
        versionName = packageInfo.versionName.orEmpty()
        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        versionDate = getString(R.string.plugin_version_date)
        id = ReadiumEpubReaderPlugin.ID
        engine = ExplorerActionPluginIds.ENGINE
        variant = ReadiumEpubReaderPlugin.VARIANT
        supportedAbis = emptyArray()
        capabilities = Bundle().apply {
            putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ReadiumEpubReaderPlugin.REQUIRED_HOST_VERSION)
            putInt(ExplorerActionCapabilityKeys.PROTOCOL_VERSION, ReadiumEpubReaderPlugin.PROTOCOL_VERSION)
        }
    }
}

internal fun readiumEpubReaderActionCatalog(): Bundle {
    fun action(spec: ExplorerActionSpec) = Bundle().apply {
        putString(ExplorerActionCatalogKeys.ID, spec.id)
        putString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME, spec.labelResourceName)
        putString(ExplorerActionCatalogKeys.LABEL_FALLBACK, spec.labelFallback)
        putString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME, spec.activityClassName)
        putInt(ExplorerActionCatalogKeys.PRIORITY, spec.priority)
        putInt(ExplorerActionCatalogKeys.TARGET_KIND, spec.targetKind)
        putInt(ExplorerActionCatalogKeys.ACCESS_MODE, spec.accessMode)
        putInt(ExplorerActionCatalogKeys.PLACEMENT, spec.placement)
        putStringArrayList(ExplorerActionCatalogKeys.MIME_TYPES, ArrayList(spec.mimeTypes))
        putStringArrayList(ExplorerActionCatalogKeys.EXTENSIONS, ArrayList(spec.extensions))
    }
    return Bundle().apply {
        putInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION, ReadiumEpubReaderPlugin.PROTOCOL_VERSION)
        putParcelableArrayList(
            ExplorerActionCatalogKeys.ACTIONS,
            ArrayList(ReadiumEpubReaderPlugin.actionSpecs().map(::action)),
        )
    }
}
