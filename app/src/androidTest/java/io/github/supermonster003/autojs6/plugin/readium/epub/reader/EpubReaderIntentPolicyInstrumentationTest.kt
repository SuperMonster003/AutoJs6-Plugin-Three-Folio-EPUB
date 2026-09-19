package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpubReaderIntentPolicyInstrumentationTest {

    private val parent: Uri = "content://org.autojs.autojs6.explorer/tree/primary%3ABooks".toUri()
    private val document: Uri = "content://org.autojs.autojs6.explorer/tree/primary%3ABooks/novel.epub".toUri()

    private fun validIntent(
        actionId: String = ReadiumEpubReaderPlugin.PRIMARY_ACTION_ID,
        protocol: Int = ReadiumEpubReaderPlugin.PROTOCOL_VERSION,
        hostVersion: Long = ReadiumEpubReaderPlugin.REQUIRED_HOST_VERSION,
        surface: String = ExplorerActionIntentValues.SOURCE_SURFACE_MAIN,
        mimeType: String? = "application/epub+zip",
        displayName: String? = "novel.epub",
    ): Intent = Intent(ExplorerActionPluginActions.EXECUTE).apply {
        setDataAndType(document, mimeType)
        putExtra(ExplorerActionIntentExtras.ACTION_ID, actionId)
        putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, protocol)
        putExtra(ExplorerActionIntentExtras.HOST_VERSION_CODE, hostVersion)
        putExtra(ExplorerActionIntentExtras.SOURCE_SURFACE, surface)
        putExtra(ExplorerActionIntentExtras.PARENT_URI, parent)
        displayName?.let { putExtra(ExplorerActionIntentExtras.DISPLAY_NAME, it) }
        clipData = ClipData.newRawUri("target", document).apply { addItem(ClipData.Item(parent)) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
    }

    @Test
    fun aCompleteV2EnvelopeIsAccepted() {
        val request = EpubReaderIntentPolicy.resolve(validIntent())
        assertNotNull(request)
        assertEquals(document, request?.documentUri)
        assertEquals(parent, request?.parentUri)
        assertEquals("novel.epub", request?.displayName)

        assertNotNull(EpubReaderIntentPolicy.resolve(validIntent(actionId = ReadiumEpubReaderPlugin.ID)))
        assertNotNull(EpubReaderIntentPolicy.resolve(validIntent(displayName = null)))
        assertNotNull(EpubReaderIntentPolicy.resolve(validIntent(mimeType = null)))
        assertNotNull(EpubReaderIntentPolicy.resolve(validIntent(hostVersion = 9000L)))
    }

    @Test
    fun protocolIdentityAndHostBoundariesAreEnforced() {
        assertNull(EpubReaderIntentPolicy.resolve(validIntent(actionId = "html-previewer")))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent(protocol = 1)))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent(protocol = 22)))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent(hostVersion = 5268L)))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent(surface = "other")))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent().setAction(Intent.ACTION_VIEW)))
    }

    @Test
    fun grantsClipDataAndParentRelationshipAreEnforced() {
        assertNull(EpubReaderIntentPolicy.resolve(validIntent().apply { flags = Intent.FLAG_GRANT_READ_URI_PERMISSION }))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent().apply { clipData = null }))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent().apply { clipData = ClipData.newRawUri("target", document) }))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent().apply {
            clipData = ClipData.newRawUri("parent", parent).apply { addItem(ClipData.Item(document)) }
        }))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent().apply {
            putExtra(ExplorerActionIntentExtras.PARENT_URI, "content://org.autojs.autojs6.explorer/tree/primary%3AOther".toUri())
        }))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent().apply {
            putExtra(ExplorerActionIntentExtras.PARENT_URI, "content://org.autojs.autojs6.explorer/tree/primary%3ABooks?x=1".toUri())
        }))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent().apply { setDataAndType("file:///sdcard/novel.epub".toUri(), type) }))
    }

    @Test
    fun onlyEpubFilesPassTheFormatGate() {
        assertNull(EpubReaderIntentPolicy.resolve(validIntent(mimeType = "text/html", displayName = "page.html")))
        assertNotNull(EpubReaderIntentPolicy.resolve(validIntent(mimeType = "application/zip", displayName = "novel.epub")))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent(mimeType = "application/pdf", displayName = "novel.epub")))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent(mimeType = "application/zip", displayName = "novel.zip")))
        assertNull(EpubReaderIntentPolicy.resolve(validIntent(mimeType = null, displayName = "novel")))
        assertNotNull(EpubReaderIntentPolicy.resolve(validIntent(mimeType = "application/epub+zip", displayName = "novel")))
    }

    @Test
    fun parentRelationshipRejectsTraversalAndForeignAuthorities() {
        assertTrue(EpubReaderPathPolicy.isDescendant(parent, document))
        assertFalse(EpubReaderPathPolicy.isDescendant(parent, parent))
        assertFalse(EpubReaderPathPolicy.isDescendant(parent, "content://other.authority/tree/primary%3ABooks/novel.epub".toUri()))
        assertFalse(EpubReaderPathPolicy.isDescendant(parent, "content://org.autojs.autojs6.explorer/tree/primary%3ABooks/../x.epub".toUri()))
        assertFalse(EpubReaderPathPolicy.isDescendant(parent, "content://org.autojs.autojs6.explorer/tree/primary%3ABooks/novel.epub#frag".toUri()))
        assertFalse(EpubReaderPathPolicy.isDescendant("file:///Books".toUri(), "file:///Books/novel.epub".toUri()))
    }
}
