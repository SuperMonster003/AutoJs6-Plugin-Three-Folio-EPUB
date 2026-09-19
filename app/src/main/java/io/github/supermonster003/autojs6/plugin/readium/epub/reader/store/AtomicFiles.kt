package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Atomic replace-on-write for the small JSON files of the book store (roadmap D13): the payload is
 * written to a sibling temporary file, synced, then renamed over the target so a crash never leaves
 * a half-written record. Pure JVM (no `android.util.AtomicFile`) so JUnit exercises the real code.
 */
internal object AtomicFiles {

    private const val TEMPORARY_SUFFIX = ".tmp"

    fun write(target: File, bytes: ByteArray) {
        val directory = target.absoluteFile.parentFile ?: throw IOException("No parent directory for $target")
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Cannot create $directory")
        }
        val temporary = File(directory, target.name + TEMPORARY_SUFFIX)
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
        if (!temporary.renameTo(target)) {
            // Windows (JUnit) cannot rename over an existing file; Linux replaces it atomically.
            if (!target.delete() || !temporary.renameTo(target)) {
                temporary.delete()
                throw IOException("Cannot replace $target")
            }
        }
    }

    /** Returns the file content, or null when it is missing or unreadable. A leftover temp file is discarded. */
    fun read(target: File): ByteArray? {
        File(target.parentFile, target.name + TEMPORARY_SUFFIX).delete()
        return runCatching { if (target.isFile) target.readBytes() else null }.getOrNull()
    }
}
