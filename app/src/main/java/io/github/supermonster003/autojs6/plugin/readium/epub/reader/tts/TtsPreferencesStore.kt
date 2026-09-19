package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.AtomicFiles
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.navigator.media.tts.android.AndroidTtsPreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi
import java.io.File

/**
 * The global read-aloud preferences file `files/tts-preferences.json` (roadmap P3): speed, pitch,
 * language and the voice chosen per language, in Readium's own serialization so the file stays
 * readable by the toolkit that produced it. Written atomically like the reader preferences;
 * callers serialize access.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class TtsPreferencesStore(private val file: File) {

    private val serializer = AndroidTtsPreferencesSerializer()

    /** The stored preferences, or null when the file is missing or corrupt. */
    fun read(): AndroidTtsPreferences? =
        AtomicFiles.read(file)?.let { bytes ->
            runCatching { serializer.deserialize(String(bytes, Charsets.UTF_8)) }.getOrNull()
        }

    fun write(preferences: AndroidTtsPreferences) {
        AtomicFiles.write(file, serializer.serialize(preferences).toByteArray(Charsets.UTF_8))
    }

    fun clear() {
        file.delete()
        File(file.parentFile, file.name + ".tmp").delete()
    }

    companion object {
        const val FILE_NAME = "tts-preferences.json"

        fun forFilesDirectory(filesDirectory: File): TtsPreferencesStore =
            TtsPreferencesStore(File(filesDirectory, FILE_NAME))
    }
}
