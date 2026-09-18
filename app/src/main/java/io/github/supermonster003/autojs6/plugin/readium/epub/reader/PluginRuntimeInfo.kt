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
    const val VARIANT = "default"
    const val LABEL_RESOURCE_NAME = "action_readium_epub_reader"
    const val LABEL_FALLBACK = "Readium EPUB Reader"
    const val ACTIVITY_CLASS_NAME =
        "io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity"

    const val EPUB_MIME_TYPE = "application/epub+zip"
    const val EPUB_EXTENSION = "epub"

    val REQUIRED_HOST_VERSION = EpubReaderExplorerCompatibility.minimumHostVersionCode
    val PROTOCOL_VERSION = EpubReaderExplorerCompatibility.declaredProtocolVersion

    val MIME_TYPES = arrayOf(EPUB_MIME_TYPE)
    val EXTENSIONS = arrayOf(EPUB_EXTENSION)
}

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
    fun action(id: String, placement: Int) = Bundle().apply {
        putString(ExplorerActionCatalogKeys.ID, id)
        putString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME, ReadiumEpubReaderPlugin.LABEL_RESOURCE_NAME)
        putString(ExplorerActionCatalogKeys.LABEL_FALLBACK, ReadiumEpubReaderPlugin.LABEL_FALLBACK)
        putString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME, ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME)
        putInt(ExplorerActionCatalogKeys.PRIORITY, 100)
        putInt(ExplorerActionCatalogKeys.TARGET_KIND, ExplorerActionValues.TARGET_FILE)
        putInt(ExplorerActionCatalogKeys.ACCESS_MODE, ExplorerActionValues.ACCESS_READ_ONLY)
        putInt(ExplorerActionCatalogKeys.PLACEMENT, placement)
        putStringArrayList(
            ExplorerActionCatalogKeys.MIME_TYPES,
            ArrayList(ReadiumEpubReaderPlugin.MIME_TYPES.asList()),
        )
        putStringArrayList(
            ExplorerActionCatalogKeys.EXTENSIONS,
            ArrayList(ReadiumEpubReaderPlugin.EXTENSIONS.asList()),
        )
    }
    return Bundle().apply {
        putInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION, ReadiumEpubReaderPlugin.PROTOCOL_VERSION)
        putParcelableArrayList(
            ExplorerActionCatalogKeys.ACTIONS,
            arrayListOf(
                action(ReadiumEpubReaderPlugin.PRIMARY_ACTION_ID, ReadiumEpubReaderPlugin.PRIMARY_PLACEMENT),
                action(ReadiumEpubReaderPlugin.ID, ExplorerActionValues.PLACEMENT_OVERFLOW),
            ),
        )
    }
}
