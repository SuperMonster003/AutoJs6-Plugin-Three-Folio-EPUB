package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontCatalog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontFormat
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontInspection
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.SyntheticFonts
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.SyntheticFonts.windows
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest

class FontStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val directory: File get() = File(folder.root, FontStore.DIRECTORY_NAME)

    private fun store(maxBytes: Long = 4096, maxFonts: Int = 2, clock: () -> Long = { 1_700_000_000_000 }) =
        FontStore(directory, maxBytes, maxFonts, clock)

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun files(): List<String> = directory.list()?.sorted() ?: emptyList()

    @Test
    fun anImportStoresTheFileUnderItsHashAndCataloguesIt() {
        val store = store()
        val bytes = SyntheticFonts.build(names = listOf(windows(16, "Test Sans")))

        val result = store.import(ByteArrayInputStream(bytes), "whatever.ttf") as FontImportResult.Imported

        val entry = result.entry
        assertEquals("Test Sans", entry.displayName)
        assertEquals("Test Sans", entry.family)
        assertEquals(sha256(bytes), entry.sha256)
        assertEquals("${entry.sha256}.ttf", entry.fileName)
        assertEquals(bytes.size.toLong(), entry.bytes)
        assertEquals(1_700_000_000_000, entry.importedAt)
        assertArrayEquals(bytes, store.fileFor(entry).readBytes())
        assertEquals(listOf(entry.fileName, FontStore.INDEX_FILE_NAME), files())
        assertEquals(FontCatalog(listOf(entry)), store.read())
        assertEquals(FontCatalog(listOf(entry)), FontStore.forFilesDirectory(folder.root).read())
        assertNotNull(store.fileNamed(entry.fileName))
        assertNull(store.fileNamed("../" + entry.fileName))
        assertNull(store.fileNamed(FontStore.INDEX_FILE_NAME))
        assertNull(store.fileNamed("a".repeat(64) + ".ttf"))
    }

    @Test
    fun theSameFileIsImportedOnce() {
        val store = store()
        val bytes = SyntheticFonts.build(names = listOf(windows(1, "Test Sans")))
        val first = store.import(ByteArrayInputStream(bytes), null) as FontImportResult.Imported

        val second = store.import(ByteArrayInputStream(bytes), "renamed.otf")

        assertEquals(FontImportResult.AlreadyImported(first.entry), second)
        assertEquals(1, store.read().fonts.size)
        assertEquals(listOf(first.entry.fileName, FontStore.INDEX_FILE_NAME), files())
    }

    @Test
    fun openTypeFilesKeepTheOtfExtension() {
        val bytes = SyntheticFonts.build(signature = SyntheticFonts.OPEN_TYPE, names = listOf(windows(1, "Test Serif")))

        val result = store().import(ByteArrayInputStream(bytes), null) as FontImportResult.Imported

        assertTrue(result.entry.fileName.endsWith(".otf"))
        assertEquals(FontFormat.OPEN_TYPE, result.entry.format)
    }

    @Test
    fun theDisplayNameFallsBackToTheFileNameThenToTheHash() {
        val store = store(maxFonts = 5)

        val named = store.import(ByteArrayInputStream(SyntheticFonts.build(names = null, seed = 1)), "My_Cool-Font.TTF") as FontImportResult.Imported
        val anonymous = store.import(ByteArrayInputStream(SyntheticFonts.build(names = null, seed = 2)), null) as FontImportResult.Imported
        val blankHint = store.import(ByteArrayInputStream(SyntheticFonts.build(names = null, seed = 3)), "\"\".ttf") as FontImportResult.Imported

        assertEquals("My Cool Font", named.entry.displayName)
        assertEquals("Font " + anonymous.entry.sha256.take(8), anonymous.entry.displayName)
        assertEquals("Font " + blankHint.entry.sha256.take(8), blankHint.entry.displayName)
    }

    @Test
    fun rejectedAndOversizedFilesLeaveNothingBehind() {
        val store = store(maxBytes = 300)

        assertEquals(
            FontImportResult.Rejected(FontInspection.Rejected.NotAFont),
            store.import(ByteArrayInputStream("plain text".toByteArray()), "font.ttf"),
        )
        assertEquals(
            FontImportResult.Rejected(FontInspection.Rejected.Collection),
            store.import(ByteArrayInputStream(SyntheticFonts.build(signature = SyntheticFonts.COLLECTION)), "font.ttc"),
        )
        val truncated = SyntheticFonts.build(names = listOf(windows(1, "Cut")))
        assertEquals(
            FontImportResult.Rejected(FontInspection.Rejected.Truncated),
            store.import(ByteArrayInputStream(truncated.copyOf(truncated.size - 4)), "font.ttf"),
        )
        assertEquals(FontImportResult.TooLarge(300), store.import(ByteArrayInputStream(ByteArray(301)), "font.ttf"))

        assertEquals(emptyList<String>(), files())
        assertEquals(FontCatalog.EMPTY, store.read())
    }

    @Test
    fun theCatalogIsCapped() {
        val store = store(maxFonts = 2)
        store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "One")), seed = 1)), null)
        store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "Two")), seed = 2)), null)

        val third = store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "Three")), seed = 3)), null)

        assertEquals(FontImportResult.TooMany(2), third)
        assertEquals(listOf("One", "Two"), store.read().fonts.map { it.displayName })
    }

    @Test
    fun familyNamesNeverCollide() {
        val store = store(maxFonts = 5)

        val first = store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "Dup")), seed = 1)), null) as FontImportResult.Imported
        val second = store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "dup")), seed = 2)), null) as FontImportResult.Imported
        val generic = store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "serif")), seed = 3)), null) as FontImportResult.Imported

        assertEquals("Dup", first.entry.family)
        assertEquals("dup (${second.entry.sha256.take(8)})", second.entry.family)
        assertEquals("dup", second.entry.displayName)
        assertEquals("serif (${generic.entry.sha256.take(8)})", generic.entry.family)
        assertEquals(3, store.read().fonts.map { it.family.lowercase() }.toSet().size)
    }

    @Test
    fun deleteRemovesTheFileAndTheEntry() {
        val store = store()
        val entry = (store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "Gone")))), null) as FontImportResult.Imported).entry

        assertTrue(store.delete(entry.sha256))

        assertFalse(store.fileFor(entry).exists())
        assertEquals(FontCatalog.EMPTY, store.read())
        assertEquals(listOf(FontStore.INDEX_FILE_NAME), files())
        assertFalse(store.delete(entry.sha256))
    }

    @Test
    fun entriesWhoseFileVanishedAreLeftOut() {
        val store = store()
        val entry = (store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "Lost")))), null) as FontImportResult.Imported).entry

        assertTrue(store.fileFor(entry).delete())

        assertEquals(FontCatalog.EMPTY, store.read())
        assertNull(store.fileNamed(entry.fileName))
    }

    @Test
    fun aCorruptIndexReadsAsEmptyAndIsReplacedByTheNextImport() {
        val store = store()
        directory.mkdirs()
        File(directory, FontStore.INDEX_FILE_NAME).writeText("{broken")

        assertEquals(FontCatalog.EMPTY, store.read())

        val imported = store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "Fresh")))), null)

        assertTrue(imported is FontImportResult.Imported)
        assertEquals(listOf("Fresh"), store.read().fonts.map { it.displayName })
    }

    @Test
    fun clearRemovesTheDirectory() {
        val store = store()
        store.import(ByteArrayInputStream(SyntheticFonts.build(names = listOf(windows(1, "Bye")))), null)

        store.clear()

        assertFalse(directory.exists())
        assertEquals(FontCatalog.EMPTY, store.read())
    }
}
