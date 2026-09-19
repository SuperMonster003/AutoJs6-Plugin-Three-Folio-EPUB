package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontCatalog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontCatalogCodec
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontEntry
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontFamilyNames
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontFileValidator
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontInspection
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontLimits
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/** The outcome of [FontStore.import]; the Activity maps each case to a localized message. */
sealed class FontImportResult {
    data class Imported(val entry: FontEntry) : FontImportResult()
    data class AlreadyImported(val entry: FontEntry) : FontImportResult()
    data class Rejected(val reason: FontInspection.Rejected) : FontImportResult()
    data class TooLarge(val maxBytes: Long) : FontImportResult()
    data class TooMany(val maxFonts: Int) : FontImportResult()

    /** The source could not be read or the copy could not be written. */
    object Failed : FontImportResult()
}

/**
 * The imported fonts under `files/fonts/` (roadmap P2.2 / D13): one file per content hash next to
 * an `index.json` catalog written through [AtomicFiles]. A candidate is copied to a temporary file
 * while it is hashed and size-capped, validated by [FontFileValidator], then renamed to its final
 * name; nothing partial survives a failure. Pure JVM; callers serialize access.
 */
internal class FontStore(
    private val directory: File,
    private val maxBytes: Long = FontLimits.MAX_BYTES,
    private val maxFonts: Int = FontLimits.MAX_FONTS,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val indexFile = File(directory, INDEX_FILE_NAME)

    /** The catalog on disk; entries whose file went missing are left out. */
    fun read(): FontCatalog {
        val decoded = AtomicFiles.read(indexFile)
            ?.let { FontCatalogCodec.decode(String(it, Charsets.UTF_8)) }
            ?: return FontCatalog.EMPTY
        return FontCatalog(decoded.fonts.filter { File(directory, it.fileName).isFile })
    }

    fun fileFor(entry: FontEntry): File = File(directory, entry.fileName)

    /** The stored file called [fileName], or null unless it is a hash-named font that exists. */
    fun fileNamed(fileName: String): File? =
        fileName.takeIf { FontCatalogCodec.FILE_NAME_PATTERN.matches(it) }
            ?.let { File(directory, it) }
            ?.takeIf { it.isFile }

    /**
     * Copies [source] into the store. [fileNameHint] (the picked document's display name) only
     * names the font when its `name` table has no usable family.
     */
    fun import(source: InputStream, fileNameHint: String?): FontImportResult {
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) return FontImportResult.Failed
        val catalog = read()
        if (catalog.fonts.size >= maxFonts) return FontImportResult.TooMany(maxFonts)
        val temporary = File(directory, "$TEMPORARY_PREFIX${System.nanoTime()}$TEMPORARY_SUFFIX")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) return FontImportResult.TooLarge(maxBytes)
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
                output.flush()
                output.fd.sync()
            }
            val sha256 = digest.digest().toHex()
            catalog.bySha256(sha256)?.let { return FontImportResult.AlreadyImported(it) }
            val font = when (val inspection = FontFileValidator.inspect(temporary.readBytes())) {
                is FontInspection.Font -> inspection
                is FontInspection.Rejected -> return FontImportResult.Rejected(inspection)
            }
            val fileName = "$sha256.${font.format.extension}"
            val target = File(directory, fileName)
            if (!temporary.renameTo(target) && (!target.delete() || !temporary.renameTo(target))) {
                return FontImportResult.Failed
            }
            val displayName = font.familyName
                ?: FontFileValidator.sanitize(fileNameHint?.substringBeforeLast('.')?.replace('_', ' ')?.replace('-', ' '))
                ?: "Font ${sha256.take(FontFamilyNames.HASH_SUFFIX_LENGTH)}"
            val family = FontFamilyNames.resolve(displayName, sha256, catalog.fonts.map { it.family })
            val entry = FontEntry(sha256, fileName, displayName, family, total, clock())
            write(FontCatalog(catalog.fonts + entry))
            return FontImportResult.Imported(entry)
        } catch (_: IOException) {
            return FontImportResult.Failed
        } finally {
            temporary.delete()
        }
    }

    /** Removes the entry and its file; false when the hash is unknown. */
    fun delete(sha256: String): Boolean {
        val catalog = read()
        val entry = catalog.bySha256(sha256) ?: return false
        write(FontCatalog(catalog.fonts - entry))
        File(directory, entry.fileName).delete()
        return true
    }

    fun clear() {
        directory.deleteRecursively()
    }

    private fun write(catalog: FontCatalog) {
        AtomicFiles.write(indexFile, FontCatalogCodec.encode(catalog).toByteArray(Charsets.UTF_8))
    }

    private fun ByteArray.toHex(): String {
        val out = CharArray(size * 2)
        for (index in indices) {
            val value = this[index].toInt() and 0xFF
            out[index * 2] = HEX[value ushr 4]
            out[index * 2 + 1] = HEX[value and 0x0F]
        }
        return String(out)
    }

    companion object {
        const val DIRECTORY_NAME = "fonts"
        const val INDEX_FILE_NAME = "index.json"

        private const val TEMPORARY_PREFIX = "import-"
        private const val TEMPORARY_SUFFIX = ".tmp"
        private const val COPY_BUFFER_SIZE = 64 * 1024
        private val HEX = "0123456789abcdef".toCharArray()

        fun forFilesDirectory(filesDirectory: File): FontStore = FontStore(File(filesDirectory, DIRECTORY_NAME))
    }
}
