package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubRequestPolicyTest {

    private fun shape(
        action: String? = EpubRequestPolicy.ACTION_VIEW,
        explicit: Boolean = false,
        scheme: String? = "content",
        authority: String? = "com.example.files",
        pathSegments: List<String> = listOf("document", "primary:Books/novel.epub"),
        hasQueryOrFragment: Boolean = false,
        readGrant: Boolean = true,
        prefixGrant: Boolean = false,
        clipItems: Int = 0,
        mimeType: String? = "application/epub+zip",
        suppliedName: String? = null,
    ) = RequestShape(action, explicit, scheme, authority, pathSegments, hasQueryOrFragment, readGrant, prefixGrant, clipItems, mimeType, suppliedName)

    private val explorerEnvelope = shape(
        action = ExplorerActionPluginActions.EXECUTE,
        authority = "org.autojs.autojs6.explorer",
        pathSegments = listOf("tree", "primary:Books", "novel.epub"),
        prefixGrant = true,
        clipItems = 2,
        suppliedName = "novel.epub",
    )
    private val launcherRequest = shape(action = EpubRequestPolicy.ACTION_OPEN_RECENT, explicit = true, suppliedName = "novel.epub")
    private val externalRequest = shape()

    private fun admit(receiver: RequestReceiver, shape: RequestShape) = EpubRequestPolicy.admit(receiver, shape)

    @Test
    fun eachReceiverOpensOnlyItsOwnDoors() {
        assertEquals(ReaderEntry.EXPLORER, admit(RequestReceiver.READER, explorerEnvelope)?.entry)
        assertEquals(ReaderEntry.LAUNCHER, admit(RequestReceiver.READER, launcherRequest)?.entry)
        assertEquals(ReaderEntry.EXTERNAL, admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest)?.entry)

        // An Explorer envelope must not walk in through the exported viewer, and ACTION_VIEW must not reach the protected reader.
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, explorerEnvelope))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, launcherRequest))
        assertNull(admit(RequestReceiver.READER, externalRequest))
        assertNull(admit(RequestReceiver.READER, externalRequest.copy(explicit = true)))
        assertNull(admit(RequestReceiver.READER, shape(action = null)))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, shape(action = "android.intent.action.SEND")))
    }

    @Test
    fun anExternalRequestDisguisedAsTheExplorerIsStillJudgedByTheExplorerRules() {
        // The action alone does not make an envelope: the prefix grant and the two ClipData items are required.
        assertNull(admit(RequestReceiver.READER, externalRequest.copy(action = ExplorerActionPluginActions.EXECUTE)))
        assertNull(admit(RequestReceiver.READER, explorerEnvelope.copy(prefixGrant = false)))
        assertNull(admit(RequestReceiver.READER, explorerEnvelope.copy(clipItems = 1)))
        assertNull(admit(RequestReceiver.READER, explorerEnvelope.copy(clipItems = 3)))
        assertNull(admit(RequestReceiver.READER, explorerEnvelope.copy(readGrant = false)))
    }

    @Test
    fun theLauncherActionNeedsAnExplicitIntentWithoutClipData() {
        assertNull(admit(RequestReceiver.READER, launcherRequest.copy(explicit = false)))
        assertNull(admit(RequestReceiver.READER, launcherRequest.copy(clipItems = 1)))
        assertNull(admit(RequestReceiver.READER, launcherRequest.copy(readGrant = false)))
        // The launcher never sends the prefix grant, but an extra flag does not hurt.
        assertNotNull(admit(RequestReceiver.READER, launcherRequest.copy(prefixGrant = true)))
    }

    @Test
    fun theExternalDoorTakesOnlyGrantedContentDocuments() {
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(readGrant = false)))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(scheme = "file", authority = null, pathSegments = listOf("sdcard", "novel.epub"))))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(scheme = "https", authority = "example.com")))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(scheme = null, authority = null)))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(authority = "")))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(hasQueryOrFragment = true)))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(pathSegments = emptyList())))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(pathSegments = listOf("document", ""))))
        // A bare document tree is a directory, and so is anything the provider types as one.
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(pathSegments = listOf("tree", "primary:Books"))))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(mimeType = "vnd.android.document/directory", suppliedName = "Books.epub")))
        // A document inside a tree is a document.
        assertNotNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(pathSegments = listOf("tree", "primary:Books", "document", "primary:Books/novel.epub"))))
        // The scheme is compared case-insensitively, an explicit Intent is welcome, a persistable flag is irrelevant here.
        assertNotNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(scheme = "Content", explicit = true)))
    }

    @Test
    fun theDisplayNameComesFromTheSenderOrTheLeafAndGatesTheFormat() {
        assertEquals("novel.epub", admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest)?.displayName)
        assertEquals("Moby Dick.epub", admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(suppliedName = " Moby Dick.epub "))?.displayName)
        // A media-style leaf without an extension is fine when the sender types the document as EPUB, and rejected otherwise.
        assertEquals("1234", admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(pathSegments = listOf("external", "file", "1234")))?.displayName)
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(pathSegments = listOf("external", "file", "1234"), mimeType = "application/octet-stream")))
        // A `.epub` name passes whatever the type, other formats never do.
        assertNotNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(mimeType = "application/octet-stream")))
        assertNotNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(mimeType = null)))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(suppliedName = "novel.pdf")))
        assertNull(admit(RequestReceiver.EXTERNAL_VIEWER, externalRequest.copy(mimeType = "application/pdf")))
        // A whitespace-only supplied name falls back to the leaf.
        assertEquals("novel.epub", admit(RequestReceiver.READER, launcherRequest.copy(suppliedName = "   "))?.displayName)
    }

    @Test
    fun epubExtensionIsAcceptedRegardlessOfMimeType() {
        assertTrue(EpubRequestPolicy.isSupportedEpub(null, "book.epub"))
        assertTrue(EpubRequestPolicy.isSupportedEpub("application/epub+zip", "Book.EPUB"))
        assertTrue(EpubRequestPolicy.isSupportedEpub("application/octet-stream", "novel.epub"))
        assertTrue(EpubRequestPolicy.isSupportedEpub("text/plain; charset=utf-8", "notes.epub"))
        assertTrue(EpubRequestPolicy.isSupportedEpub("application/zip", "sniffed.epub"))
        assertTrue(EpubRequestPolicy.isSupportedEpub("Application/ZIP", "sniffed.epub"))
    }

    @Test
    fun extensionlessFilesNeedTheEpubMimeType() {
        assertTrue(EpubRequestPolicy.isSupportedEpub("application/epub+zip", "book"))
        assertTrue(EpubRequestPolicy.isSupportedEpub("Application/EPUB+ZIP; charset=binary", "book"))
        assertFalse(EpubRequestPolicy.isSupportedEpub(null, "book"))
        assertFalse(EpubRequestPolicy.isSupportedEpub("application/octet-stream", "book"))
    }

    @Test
    fun otherExtensionsAndConflictingContainersAreRejected() {
        assertFalse(EpubRequestPolicy.isSupportedEpub("application/epub+zip", "book.zip"))
        assertFalse(EpubRequestPolicy.isSupportedEpub("application/epub+zip", "book.cbz"))
        assertFalse(EpubRequestPolicy.isSupportedEpub(null, "book.pdf"))
        assertFalse(EpubRequestPolicy.isSupportedEpub("application/zip", "book"))
        assertFalse(EpubRequestPolicy.isSupportedEpub("application/pdf", "book.epub"))
        assertFalse(EpubRequestPolicy.isSupportedEpub("application/vnd.comicbook+zip", "book.epub"))
        assertFalse(EpubRequestPolicy.isSupportedEpub("vnd.android.document/directory", "Books.epub"))
    }

    @Test
    fun displayNamesAreReducedToASafeLeafName() {
        assertEquals("book.epub", EpubRequestPolicy.sanitizeDisplayName("  book.epub  "))
        assertEquals("book.epub", EpubRequestPolicy.sanitizeDisplayName("/sdcard/Books/book.epub"))
        assertEquals("book.epub", EpubRequestPolicy.sanitizeDisplayName("C:\\Books\\book.epub"))
        assertEquals("ab.epub", EpubRequestPolicy.sanitizeDisplayName("a\u0000b\u001f.epub"))
        assertNull(EpubRequestPolicy.sanitizeDisplayName(null))
        assertNull(EpubRequestPolicy.sanitizeDisplayName(""))
        assertNull(EpubRequestPolicy.sanitizeDisplayName("   "))
        assertNull(EpubRequestPolicy.sanitizeDisplayName("folder/"))
        assertEquals(255, EpubRequestPolicy.sanitizeDisplayName("x".repeat(300))?.length)
    }
}
