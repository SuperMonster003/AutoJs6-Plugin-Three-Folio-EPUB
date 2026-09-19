package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.PreferencesCodec
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.StoredPreferences
import java.io.File

/**
 * The single global preferences file `files/reader-preferences.json` (roadmap D14 / P2.1),
 * written atomically like the book records. Pure JVM; callers serialize access.
 */
internal class ReaderPreferencesStore(private val file: File) {

    /** The stored preferences, or null when the file is missing or corrupt. */
    fun read(): StoredPreferences? =
        AtomicFiles.read(file)?.let { PreferencesCodec.decode(String(it, Charsets.UTF_8)) }

    fun exists(): Boolean = file.isFile

    fun write(stored: StoredPreferences) {
        AtomicFiles.write(file, PreferencesCodec.encode(stored).toByteArray(Charsets.UTF_8))
    }

    fun clear() {
        file.delete()
        File(file.parentFile, file.name + ".tmp").delete()
    }

    companion object {
        const val FILE_NAME = "reader-preferences.json"

        fun forFilesDirectory(filesDirectory: File): ReaderPreferencesStore =
            ReaderPreferencesStore(File(filesDirectory, FILE_NAME))
    }
}
