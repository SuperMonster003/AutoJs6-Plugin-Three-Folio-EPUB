package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubReaderIntentPolicyTest {

    @Test
    fun epubExtensionIsAcceptedRegardlessOfMimeType() {
        assertTrue(EpubReaderIntentPolicy.isSupportedEpub(null, "book.epub"))
        assertTrue(EpubReaderIntentPolicy.isSupportedEpub("application/epub+zip", "Book.EPUB"))
        assertTrue(EpubReaderIntentPolicy.isSupportedEpub("application/octet-stream", "novel.epub"))
        assertTrue(EpubReaderIntentPolicy.isSupportedEpub("text/plain; charset=utf-8", "notes.epub"))
        assertTrue(EpubReaderIntentPolicy.isSupportedEpub("application/zip", "sniffed.epub"))
        assertTrue(EpubReaderIntentPolicy.isSupportedEpub("Application/ZIP", "sniffed.epub"))
    }

    @Test
    fun extensionlessFilesNeedTheEpubMimeType() {
        assertTrue(EpubReaderIntentPolicy.isSupportedEpub("application/epub+zip", "book"))
        assertTrue(EpubReaderIntentPolicy.isSupportedEpub("Application/EPUB+ZIP; charset=binary", "book"))
        assertFalse(EpubReaderIntentPolicy.isSupportedEpub(null, "book"))
        assertFalse(EpubReaderIntentPolicy.isSupportedEpub("application/octet-stream", "book"))
    }

    @Test
    fun otherExtensionsAndConflictingContainersAreRejected() {
        assertFalse(EpubReaderIntentPolicy.isSupportedEpub("application/epub+zip", "book.zip"))
        assertFalse(EpubReaderIntentPolicy.isSupportedEpub("application/epub+zip", "book.cbz"))
        assertFalse(EpubReaderIntentPolicy.isSupportedEpub(null, "book.pdf"))
        assertFalse(EpubReaderIntentPolicy.isSupportedEpub("application/zip", "book"))
        assertFalse(EpubReaderIntentPolicy.isSupportedEpub("application/pdf", "book.epub"))
        assertFalse(EpubReaderIntentPolicy.isSupportedEpub("application/vnd.comicbook+zip", "book.epub"))
    }

    @Test
    fun displayNamesAreReducedToASafeLeafName() {
        assertEquals("book.epub", EpubReaderIntentPolicy.sanitizeDisplayName("  book.epub  "))
        assertEquals("book.epub", EpubReaderIntentPolicy.sanitizeDisplayName("/sdcard/Books/book.epub"))
        assertEquals("book.epub", EpubReaderIntentPolicy.sanitizeDisplayName("C:\\Books\\book.epub"))
        assertEquals("ab.epub", EpubReaderIntentPolicy.sanitizeDisplayName("a\u0000b\u001f.epub"))
        assertNull(EpubReaderIntentPolicy.sanitizeDisplayName(null))
        assertNull(EpubReaderIntentPolicy.sanitizeDisplayName(""))
        assertNull(EpubReaderIntentPolicy.sanitizeDisplayName("   "))
        assertNull(EpubReaderIntentPolicy.sanitizeDisplayName("folder/"))
        assertEquals(255, EpubReaderIntentPolicy.sanitizeDisplayName("x".repeat(300))?.length)
    }
}
