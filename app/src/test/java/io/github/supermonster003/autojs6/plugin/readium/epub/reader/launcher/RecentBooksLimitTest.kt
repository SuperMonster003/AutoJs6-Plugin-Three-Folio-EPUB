package io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentBooksLimitTest {

    private fun book(index: Int, readAt: Long = index.toLong()) =
        RecentBook(uri = "content://a/$index", displayName = "$index.epub", addedAt = index.toLong(), lastReadAt = readAt)

    @Test
    fun newestReadingComesFirstAndUnreadBooksSortByWhenTheyWereAdded() {
        val sorted = RecentBooksPolicy.sorted(listOf(book(1, readAt = 50), book(2, readAt = 0), book(3, readAt = 0), book(4, readAt = 10)))
        assertEquals(listOf("content://a/1", "content://a/4", "content://a/3", "content://a/2"), sorted.map { it.uri })
    }

    @Test
    fun anUpsertReplacesTheSameUriKeepingWhatTheNewEntryLeavesNull() {
        val existing = book(1).copy(key = "k", title = "T", author = "A", coverFile = "c.webp", progression = 0.5, available = false)
        val update = RecentBooksPolicy.upsert(listOf(existing, book(2, readAt = 100)), book(1, readAt = 200).copy(title = "New"))
        assertTrue(update.evicted.isEmpty())
        assertEquals(2, update.books.size)
        val merged = update.books.first()
        assertEquals("content://a/1", merged.uri)
        assertEquals(existing.addedAt, merged.addedAt)
        assertEquals(200L, merged.lastReadAt)
        assertEquals("k", merged.key)
        assertEquals("New", merged.title)
        assertEquals("A", merged.author)
        assertEquals("c.webp", merged.coverFile)
        assertEquals(0.5, merged.progression!!, 0.0)
        assertTrue("an upsert marks the book available again", merged.available)
    }

    @Test
    fun theOldestEntriesFallOffBeyondTheLimit() {
        var books = emptyList<RecentBook>()
        for (index in 1..RecentBooksPolicy.LIMIT) books = RecentBooksPolicy.upsert(books, book(index)).books
        assertEquals(RecentBooksPolicy.LIMIT, books.size)
        val update = RecentBooksPolicy.upsert(books, book(RecentBooksPolicy.LIMIT + 1))
        assertEquals(RecentBooksPolicy.LIMIT, update.books.size)
        assertEquals(listOf("content://a/1"), update.evicted.map { it.uri })
        assertEquals("content://a/${RecentBooksPolicy.LIMIT + 1}", update.books.first().uri)
        // Reading an old book again moves it to the front instead of evicting anything.
        val reread = RecentBooksPolicy.upsert(update.books, book(2, readAt = 10_000))
        assertTrue(reread.evicted.isEmpty())
        assertEquals("content://a/2", reread.books.first().uri)
    }

    @Test
    fun removalAndPercentages() {
        assertEquals(listOf("content://a/2"), RecentBooksPolicy.remove(listOf(book(1), book(2)), "content://a/1").map { it.uri })
        assertNull(RecentBooksPolicy.progressPercent(null))
        assertNull(RecentBooksPolicy.progressPercent(1.5))
        assertEquals(0, RecentBooksPolicy.progressPercent(0.0))
        assertEquals(42, RecentBooksPolicy.progressPercent(0.429))
        assertEquals(100, RecentBooksPolicy.progressPercent(1.0))
    }
}
