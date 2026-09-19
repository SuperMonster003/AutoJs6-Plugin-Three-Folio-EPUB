package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.StoredPreferences
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ReaderPreferencesStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val filesDirectory: File get() = folder.root
    private val file: File get() = File(filesDirectory, ReaderPreferencesStore.FILE_NAME)

    @Test
    fun aMissingFileReadsAsNull() {
        val store = ReaderPreferencesStore.forFilesDirectory(filesDirectory)

        assertNull(store.read())
        assertFalse(store.exists())
    }

    @Test
    fun preferencesRoundTripThroughTheFile() {
        val store = ReaderPreferencesStore.forFilesDirectory(filesDirectory)
        val stored = StoredPreferences(ThemeMode.DARK, JSONObject().put("fontSize", 1.4).put("scroll", true))

        store.write(stored)

        assertTrue(store.exists())
        assertTrue(stored.sameAs(store.read()))
        assertFalse(File(filesDirectory, ReaderPreferencesStore.FILE_NAME + ".tmp").exists())
        assertEquals("reader-preferences.json", file.name)
    }

    @Test
    fun aLaterWriteReplacesTheEarlierOne() {
        val store = ReaderPreferencesStore.forFilesDirectory(filesDirectory)
        store.write(StoredPreferences(ThemeMode.SEPIA, JSONObject().put("fontSize", 2.0)))

        store.write(StoredPreferences.defaults())

        assertTrue(store.read()?.isDefault == true)
    }

    @Test
    fun aCorruptFileReadsAsNullButStillExists() {
        val store = ReaderPreferencesStore.forFilesDirectory(filesDirectory)
        file.writeText("{not json")

        assertNull(store.read())
        assertTrue(store.exists())
    }

    @Test
    fun clearRemovesTheFileAndItsTemporary() {
        val store = ReaderPreferencesStore.forFilesDirectory(filesDirectory)
        store.write(StoredPreferences.defaults())
        File(filesDirectory, ReaderPreferencesStore.FILE_NAME + ".tmp").writeText("half")

        store.clear()

        assertFalse(store.exists())
        assertFalse(File(filesDirectory, ReaderPreferencesStore.FILE_NAME + ".tmp").exists())
        assertNull(store.read())
    }
}
