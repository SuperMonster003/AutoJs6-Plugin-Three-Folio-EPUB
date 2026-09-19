package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import java.io.File

/**
 * Per-book data under `files/books/<fingerprint>/` (roadmap D4 / D13 / D23): `progress.json` and,
 * from roadmap P2.6, `bookmarks.json`. Keys are content fingerprints, never paths or names.
 *
 * - Writes are atomic ([AtomicFiles]) and the store keeps at most [bookLimit] books, evicting the
 *   least recently updated ones.
 * - [migrate] moves a book from its temporary quick key to the full-file fingerprint once the
 *   background hash completes; when both exist, the newer progress wins and the bookmarks are
 *   united. The quick key then stays known as an alias (`aliases/<quick key>` naming the full
 *   key) so the next open finds the book before the full hash is recomputed.
 *
 * Pure JVM and single-threaded by contract: callers serialize access (the view model uses one
 * mutex and one IO dispatcher).
 */
internal class BookDataStore(
    private val root: File,
    private val bookLimit: Int = MAX_BOOKS,
) {

    fun readProgress(key: String): ProgressRecord? {
        val bytes = AtomicFiles.read(progressFile(key)) ?: return null
        return ProgressCodec.decode(String(bytes, Charsets.UTF_8))
    }

    fun writeProgress(key: String, record: ProgressRecord) {
        AtomicFiles.write(progressFile(key), ProgressCodec.encode(record).toByteArray(Charsets.UTF_8))
        evictBeyondLimit()
    }

    fun clearProgress(key: String) {
        val directory = bookDirectory(key)
        File(directory, PROGRESS_FILE).delete()
        File(directory, "$PROGRESS_FILE.tmp").delete()
        if (directory.list().isNullOrEmpty()) directory.delete()
    }

    /** The bookmarks of one book in creation order; a missing or unreadable file is an empty list. */
    fun readBookmarks(key: String): List<Bookmark> =
        AtomicFiles.read(bookmarksFile(key))?.let { BookmarkCodec.decode(String(it, Charsets.UTF_8)) } ?: emptyList()

    /** Replaces the book's bookmarks; an empty list removes the file (the directory stays for the progress). */
    fun writeBookmarks(key: String, bookmarks: List<Bookmark>) {
        val file = bookmarksFile(key)
        if (bookmarks.isEmpty()) {
            file.delete()
            File(file.parentFile, "$BOOKMARKS_FILE.tmp").delete()
            return
        }
        AtomicFiles.write(file, BookmarkCodec.encode(bookmarks).toByteArray(Charsets.UTF_8))
        evictBeyondLimit()
    }

    /** Deletes everything stored for every book (settings page "clear all reading data", roadmap P4). */
    fun clearAll() {
        bookKeys().forEach { bookDirectory(it).deleteRecursively() }
        File(root, ALIASES_DIRECTORY).deleteRecursively()
    }

    /**
     * Moves the data stored under [fromKey] to [toKey]. Returns true when [toKey] now holds the
     * merged data (or nothing had to move). Progress keeps the newer of the two records; other
     * files are moved only when the target does not have them yet.
     */
    fun migrate(fromKey: String, toKey: String): Boolean {
        if (fromKey == toKey) return true
        val source = bookDirectory(fromKey)
        if (!source.isDirectory) return true
        val target = bookDirectory(toKey)
        if (!target.exists()) {
            if (source.renameTo(target)) return true
            if (!target.mkdirs() && !target.isDirectory) return false
        }
        val sourceProgress = readProgress(fromKey)
        val targetProgress = readProgress(toKey)
        val winner = listOfNotNull(sourceProgress, targetProgress).maxByOrNull { it.updatedAtMillis }
        if (winner != null && !winner.sameAs(targetProgress)) {
            AtomicFiles.write(progressFile(toKey), ProgressCodec.encode(winner).toByteArray(Charsets.UTF_8))
        }
        // Bookmarks are united (the target's keep their ids); a source file that does not decode
        // has nothing to contribute and leaves the target alone.
        val sourceBookmarks = readBookmarks(fromKey)
        if (sourceBookmarks.isNotEmpty()) {
            val merged = BookmarkCodec.merge(readBookmarks(toKey), sourceBookmarks)
            AtomicFiles.write(bookmarksFile(toKey), BookmarkCodec.encode(merged).toByteArray(Charsets.UTF_8))
        }
        source.listFiles().orEmpty()
            .filter { it.isFile && it.name != PROGRESS_FILE && it.name != BOOKMARKS_FILE && !it.name.endsWith(".tmp") }
            .forEach { file ->
                val destination = File(target, file.name)
                if (!destination.exists()) file.renameTo(destination)
            }
        source.deleteRecursively()
        return true
    }

    /**
     * The key that currently holds this book's data: the full fingerprint recorded as an alias of
     * [key] when one exists and still has a directory, otherwise [key] itself.
     */
    fun resolveKey(key: String): String {
        val alias = AtomicFiles.read(aliasFile(key))
            ?.let { String(it, Charsets.UTF_8).trim() }
            ?.takeIf { KEY_PATTERN.matches(it) && it != key }
            ?: return key
        return if (File(root, alias).isDirectory) alias else key
    }

    /** Records that [fromKey] (the quick key) stands for [toKey] (the full fingerprint). */
    fun writeAlias(fromKey: String, toKey: String) {
        require(KEY_PATTERN.matches(fromKey) && KEY_PATTERN.matches(toKey)) { "Not a book fingerprint" }
        if (fromKey == toKey) return
        AtomicFiles.write(aliasFile(fromKey), toKey.toByteArray(Charsets.UTF_8))
    }

    fun bookKeys(): List<String> =
        root.listFiles().orEmpty()
            .filter { it.isDirectory && KEY_PATTERN.matches(it.name) }
            .map { it.name }
            .sorted()

    /** Removes the least recently updated books beyond [bookLimit]; returns how many were removed. */
    fun evictBeyondLimit(): Int {
        val directories = root.listFiles().orEmpty().filter { it.isDirectory && KEY_PATTERN.matches(it.name) }
        val excess = directories.size - bookLimit
        if (excess <= 0) return 0
        val victims = directories
            .sortedWith(compareBy({ lastUpdated(it) }, { it.name }))
            .take(excess)
        victims.forEach { it.deleteRecursively() }
        pruneDanglingAliases()
        return victims.size
    }

    /** Drops aliases whose target book no longer exists; returns how many were removed. */
    fun pruneDanglingAliases(): Int {
        val stale = File(root, ALIASES_DIRECTORY).listFiles().orEmpty().filter { file ->
            val target = AtomicFiles.read(file)?.let { String(it, Charsets.UTF_8).trim() }
            target == null || !KEY_PATTERN.matches(target) || !File(root, target).isDirectory
        }
        stale.forEach { it.delete() }
        return stale.size
    }

    /** Timestamp of the most recent record for LRU ordering; falls back to the directory mtime. */
    fun lastUpdated(key: String): Long? = bookDirectory(key).takeIf { it.isDirectory }?.let(::lastUpdated)

    private fun lastUpdated(directory: File): Long =
        AtomicFiles.read(File(directory, PROGRESS_FILE))
            ?.let { ProgressCodec.decode(String(it, Charsets.UTF_8))?.updatedAtMillis }
            ?: directory.lastModified()

    private fun progressFile(key: String): File = File(bookDirectory(key), PROGRESS_FILE)

    private fun bookmarksFile(key: String): File = File(bookDirectory(key), BOOKMARKS_FILE)

    private fun aliasFile(key: String): File {
        require(KEY_PATTERN.matches(key)) { "Not a book fingerprint" }
        return File(File(root, ALIASES_DIRECTORY), key)
    }

    private fun bookDirectory(key: String): File {
        require(KEY_PATTERN.matches(key)) { "Not a book fingerprint" }
        return File(root, key)
    }

    companion object {
        const val MAX_BOOKS = 500
        const val PROGRESS_FILE = "progress.json"
        const val BOOKMARKS_FILE = "bookmarks.json"
        const val DIRECTORY_NAME = "books"
        const val ALIASES_DIRECTORY = "aliases"

        /** Lowercase hex SHA-256 (both the quick key and the full fingerprint). */
        val KEY_PATTERN = Regex("[0-9a-f]{64}")

        fun forFilesDirectory(filesDirectory: File): BookDataStore =
            BookDataStore(File(filesDirectory, DIRECTORY_NAME))
    }
}
