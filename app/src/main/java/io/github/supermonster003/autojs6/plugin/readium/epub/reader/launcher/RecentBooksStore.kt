package io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.AtomicFiles
import java.io.File
import java.security.MessageDigest

/**
 * The recent list on disk (roadmap P4.1): `files/recent-books.json` written atomically, and the
 * cover thumbnails under `files/covers/<name>.webp`. Only the launcher path adds entries (roadmap
 * D4: a persisted document-picker grant); the reader only updates entries that already exist.
 * Every method takes the process-wide lock, because the launcher reads while a reader writes.
 */
internal class RecentBooksStore(private val file: File, private val coversDirectory: File) {

    fun read(): List<RecentBook> = synchronized(LOCK) { readLocked() }

    fun write(books: List<RecentBook>) = synchronized(LOCK) { writeLocked(books) }

    /** Adds or refreshes [book]; the evicted entries' covers are deleted here, their grants are the caller's. */
    fun upsert(book: RecentBook): RecentBooksPolicy.Update = synchronized(LOCK) {
        val update = RecentBooksPolicy.upsert(readLocked(), book)
        writeLocked(update.books)
        update.evicted.forEach { deleteCover(it.coverFile) }
        update
    }

    /** Applies [transform] to the entry with [uri]; a no-op (null) when the list does not know the URI. */
    fun update(uri: String, transform: (RecentBook) -> RecentBook): RecentBook? = synchronized(LOCK) {
        val books = readLocked()
        val index = books.indexOfFirst { it.uri == uri }
        if (index < 0) return null
        val updated = transform(books[index])
        writeLocked(books.toMutableList().apply { set(index, updated) })
        updated
    }

    fun contains(uri: String): Boolean = synchronized(LOCK) { readLocked().any { it.uri == uri } }

    fun remove(uri: String): RecentBook? = synchronized(LOCK) {
        val books = readLocked()
        val removed = books.firstOrNull { it.uri == uri } ?: return null
        writeLocked(RecentBooksPolicy.remove(books, uri))
        deleteCover(removed.coverFile)
        removed
    }

    /** Forgets every entry and every cover; the caller releases the grants of the returned entries. */
    fun clear(): List<RecentBook> = synchronized(LOCK) {
        val books = readLocked()
        file.delete()
        coversDirectory.listFiles()?.forEach { it.delete() }
        books
    }

    fun coverFile(name: String): File = File(coversDirectory, name)

    /** Stores [bytes] as the cover of the book at [uri] and returns the file name. */
    fun writeCover(uri: String, bytes: ByteArray): String {
        val name = coverName(uri)
        coversDirectory.mkdirs()
        AtomicFiles.write(File(coversDirectory, name), bytes)
        return name
    }

    /** Bytes used by the list and the covers, for the settings page. */
    fun usageBytes(): Long = synchronized(LOCK) {
        (if (file.isFile) file.length() else 0L) + (coversDirectory.listFiles()?.sumOf { it.length() } ?: 0L)
    }

    private fun readLocked(): List<RecentBook> =
        AtomicFiles.read(file)?.let { RecentBooksCodec.decode(String(it, Charsets.UTF_8)) }.orEmpty()

    private fun writeLocked(books: List<RecentBook>) {
        if (books.isEmpty()) {
            file.delete()
        } else {
            file.parentFile?.mkdirs()
            AtomicFiles.write(file, RecentBooksCodec.encode(books).toByteArray(Charsets.UTF_8))
        }
    }

    private fun deleteCover(name: String?) {
        if (name != null) File(coversDirectory, name).delete()
    }

    companion object {
        const val FILE_NAME = "recent-books.json"
        const val COVERS_DIRECTORY = "covers"
        private const val COVER_SUFFIX = ".webp"
        private val LOCK = Any()

        fun forFilesDirectory(filesDirectory: File): RecentBooksStore =
            RecentBooksStore(File(filesDirectory, FILE_NAME), File(filesDirectory, COVERS_DIRECTORY))

        /** A stable file name for the URI (its SHA-256, so no path or name of the document lands on disk). */
        fun coverName(uri: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }.take(32) + COVER_SUFFIX
        }
    }
}
