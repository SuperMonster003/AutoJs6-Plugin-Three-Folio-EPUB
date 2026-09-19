package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BookDataStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val root: File get() = File(folder.root, "books")

    private fun key(seed: Int): String = seed.toString(16).padStart(64, '0')

    private fun record(href: String, updatedAt: Long) = ProgressRecord(
        JSONObject().put("href", href).put("type", "application/xhtml+xml"),
        updatedAt,
        totalProgression = 0.5,
    )

    @Test
    fun progressIsWrittenAtomicallyUnderTheFingerprintDirectory() {
        val store = BookDataStore(root)

        store.writeProgress(key(1), record("chapter1.xhtml", 10L))

        val file = File(root, key(1)).resolve(BookDataStore.PROGRESS_FILE)
        assertTrue(file.isFile)
        assertFalse(File(root, key(1)).resolve("progress.json.tmp").exists())
        assertEquals("chapter1.xhtml", store.readProgress(key(1))?.locator?.getString("href"))
        assertNull(store.readProgress(key(2)))
        assertEquals(listOf(key(1)), store.bookKeys())
    }

    @Test
    fun corruptProgressIsIgnoredAndOverwritten() {
        val store = BookDataStore(root)
        val directory = File(root, key(3)).apply { mkdirs() }
        directory.resolve(BookDataStore.PROGRESS_FILE).writeText("{ not json")

        assertNull(store.readProgress(key(3)))

        store.writeProgress(key(3), record("chapter2.xhtml", 20L))
        assertEquals(20L, store.readProgress(key(3))?.updatedAtMillis)
    }

    @Test
    fun clearingRemovesTheRecordAndTheEmptyDirectory() {
        val store = BookDataStore(root)
        store.writeProgress(key(4), record("chapter1.xhtml", 1L))

        store.clearProgress(key(4))

        assertNull(store.readProgress(key(4)))
        assertFalse(File(root, key(4)).exists())
        store.clearProgress(key(4)) // idempotent
    }

    @Test
    fun migrationRenamesWhenTheTargetIsNew() {
        val store = BookDataStore(root)
        store.writeProgress(key(5), record("chapter3.xhtml", 30L))

        assertTrue(store.migrate(key(5), key(6)))

        assertFalse(File(root, key(5)).exists())
        assertEquals("chapter3.xhtml", store.readProgress(key(6))?.locator?.getString("href"))
    }

    @Test
    fun migrationKeepsTheNewerProgressAndMovesOtherFilesOnlyWhenMissing() {
        val store = BookDataStore(root)
        store.writeProgress(key(7), record("quick.xhtml", 200L))
        store.writeProgress(key(8), record("full.xhtml", 100L))
        File(root, key(7)).resolve("bookmarks.json").writeText("from-quick")
        File(root, key(7)).resolve("notes.json").writeText("quick-notes")
        File(root, key(8)).resolve("bookmarks.json").writeText("from-full")

        assertTrue(store.migrate(key(7), key(8)))

        assertFalse(File(root, key(7)).exists())
        assertEquals("quick.xhtml", store.readProgress(key(8))?.locator?.getString("href"))
        assertEquals("from-full", File(root, key(8)).resolve("bookmarks.json").readText())
        assertEquals("quick-notes", File(root, key(8)).resolve("notes.json").readText())
    }

    @Test
    fun migrationKeepsTheTargetProgressWhenItIsNewer() {
        val store = BookDataStore(root)
        store.writeProgress(key(9), record("quick.xhtml", 100L))
        store.writeProgress(key(10), record("full.xhtml", 200L))

        assertTrue(store.migrate(key(9), key(10)))

        assertEquals("full.xhtml", store.readProgress(key(10))?.locator?.getString("href"))
        assertFalse(File(root, key(9)).exists())
    }

    @Test
    fun migrationOfAMissingSourceOrTheSameKeyIsANoOp() {
        val store = BookDataStore(root)
        store.writeProgress(key(11), record("a.xhtml", 1L))

        assertTrue(store.migrate(key(12), key(11)))
        assertTrue(store.migrate(key(11), key(11)))
        assertEquals("a.xhtml", store.readProgress(key(11))?.locator?.getString("href"))
    }

    @Test
    fun theLeastRecentlyUpdatedBooksAreEvictedBeyondTheLimit() {
        val store = BookDataStore(root, bookLimit = 3)
        (1..3).forEach { store.writeProgress(key(20 + it), record("c$it.xhtml", it * 10L)) }

        store.writeProgress(key(30), record("new.xhtml", 5L)) // oldest by timestamp, evicted first

        assertEquals(listOf(key(21), key(22), key(23)), store.bookKeys())

        store.writeProgress(key(31), record("newer.xhtml", 100L))
        assertEquals(listOf(key(22), key(23), key(31)), store.bookKeys())
    }

    @Test
    fun foreignDirectoriesAndInvalidKeysAreRejected() {
        val store = BookDataStore(root)
        File(root, "not-a-key").mkdirs()
        store.writeProgress(key(40), record("a.xhtml", 1L))

        assertEquals(listOf(key(40)), store.bookKeys())
        assertEquals(0, store.evictBeyondLimit())
        val failure = runCatching { store.readProgress("../escape") }
        assertTrue(failure.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun aliasesResolveTheQuickKeyToTheFullFingerprint() {
        val store = BookDataStore(root)
        store.writeProgress(key(60), record("a.xhtml", 1L))
        store.migrate(key(60), key(61))
        store.writeAlias(key(60), key(61))

        assertEquals(key(61), store.resolveKey(key(60)))
        assertEquals(key(61), store.resolveKey(key(61)))
        assertEquals(key(62), store.resolveKey(key(62)))
        assertEquals(listOf(key(61)), store.bookKeys())
        assertEquals("a.xhtml", store.readProgress(store.resolveKey(key(60)))?.locator?.getString("href"))
    }

    @Test
    fun aliasesToMissingOrInvalidTargetsAreIgnoredAndPruned() {
        val store = BookDataStore(root)
        store.writeAlias(key(70), key(71)) // target never written
        File(root, BookDataStore.ALIASES_DIRECTORY).resolve(key(72)).writeText("garbage")
        store.writeProgress(key(73), record("a.xhtml", 1L))
        store.writeAlias(key(74), key(73))

        assertEquals(key(70), store.resolveKey(key(70)))
        assertEquals(key(72), store.resolveKey(key(72)))
        assertEquals(key(73), store.resolveKey(key(74)))
        assertEquals(2, store.pruneDanglingAliases())
        assertEquals(key(73), store.resolveKey(key(74)))
        store.writeAlias(key(75), key(75)) // self alias is a no-op
        assertEquals(key(75), store.resolveKey(key(75)))
    }

    @Test
    fun evictionDropsTheAliasesOfEvictedBooks() {
        val store = BookDataStore(root, bookLimit = 1)
        store.writeProgress(key(80), record("old.xhtml", 1L))
        store.writeAlias(key(81), key(80))
        store.writeProgress(key(82), record("new.xhtml", 2L))

        assertEquals(listOf(key(82)), store.bookKeys())
        assertEquals(key(81), store.resolveKey(key(81)))
        assertFalse(File(root, BookDataStore.ALIASES_DIRECTORY).resolve(key(81)).exists())
    }

    @Test
    fun clearAllRemovesEveryBook() {
        val store = BookDataStore(root)
        store.writeProgress(key(50), record("a.xhtml", 1L))
        store.writeProgress(key(51), record("b.xhtml", 2L))

        store.writeAlias(key(52), key(50))

        store.clearAll()

        assertTrue(store.bookKeys().isEmpty())
        assertFalse(File(root, BookDataStore.ALIASES_DIRECTORY).exists())
    }
}
