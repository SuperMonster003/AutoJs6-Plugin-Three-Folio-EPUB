package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultPagerTest {

    /** Batches handed out in order; records how often it was pulled and whether it was closed. */
    private class FakeSource(private val batches: List<List<Int>>) : SearchSource<Int> {
        var pulls = 0
        var closed = false
        private var index = 0

        override suspend fun next(): List<Int>? {
            pulls++
            return batches.getOrNull(index++)
        }

        override fun close() {
            closed = true
        }
    }

    @Test
    fun pagesAreFilledFromSeveralSmallBatches() = runBlocking {
        val source = FakeSource((1..7).map { listOf(it) })
        val pager = SearchResultPager(source, batchSize = 3, limit = 100)

        assertEquals(listOf(1, 2, 3), pager.loadMore())
        assertEquals(3, source.pulls)
        assertFalse(pager.exhausted)
        assertEquals(listOf(4, 5, 6), pager.loadMore())
        assertEquals(listOf(7), pager.loadMore())
        assertTrue(pager.exhausted)
        assertFalse(pager.truncated)
        assertTrue(source.closed)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), pager.items)
        assertEquals(emptyList<Int>(), pager.loadMore())
        assertEquals(8, source.pulls)
    }

    @Test
    fun aLargeBatchIsSplitAcrossPagesWithoutPullingAgain() = runBlocking {
        val source = FakeSource(listOf((1..8).toList(), listOf(9)))
        val pager = SearchResultPager(source, batchSize = 3, limit = 100)

        assertEquals(listOf(1, 2, 3), pager.loadMore())
        assertEquals(listOf(4, 5, 6), pager.loadMore())
        assertEquals(1, source.pulls)
        assertEquals(listOf(7, 8, 9), pager.loadMore())
        assertEquals(2, source.pulls)
        assertFalse(pager.exhausted)
        assertEquals(emptyList<Int>(), pager.loadMore())
        assertTrue(pager.exhausted)
    }

    @Test
    fun theLimitEndsTheSearchAndMarksItTruncated() = runBlocking {
        val source = FakeSource(listOf((1..4).toList(), (5..8).toList(), (9..12).toList()))
        val pager = SearchResultPager(source, batchSize = 4, limit = 6)

        assertEquals(listOf(1, 2, 3, 4), pager.loadMore())
        assertEquals(listOf(5, 6), pager.loadMore())
        assertTrue(pager.exhausted)
        assertTrue(pager.truncated)
        assertTrue(source.closed)
        assertEquals(2, source.pulls)
        assertEquals(emptyList<Int>(), pager.loadMore())
    }

    @Test
    fun exactlyTheLimitWithNothingAfterIsStillReportedAsTruncated() = runBlocking {
        val source = FakeSource(listOf(listOf(1, 2), listOf(3, 4)))
        val pager = SearchResultPager(source, batchSize = 10, limit = 4)

        assertEquals(listOf(1, 2, 3, 4), pager.loadMore())
        // The source was never asked again, so the pager cannot know the book had no more hits.
        assertTrue(pager.truncated)
        assertTrue(pager.exhausted)
    }

    @Test
    fun anEmptySourceExhaustsImmediately() = runBlocking {
        val source = FakeSource(emptyList())
        val pager = SearchResultPager(source)

        assertEquals(emptyList<Int>(), pager.loadMore())
        assertTrue(pager.exhausted)
        assertFalse(pager.truncated)
        assertTrue(source.closed)
    }

    @Test
    fun cancellingALoadKeepsWhatArrivedAndCloseReleasesTheSource(): Unit = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val source = object : SearchSource<Int> {
            var closed = false
            private var pulls = 0

            override suspend fun next(): List<Int>? {
                pulls++
                if (pulls == 1) return listOf(1, 2)
                gate.await()
                return listOf(3)
            }

            override fun close() {
                closed = true
            }
        }
        val pager = SearchResultPager(source, batchSize = 5, limit = 100)
        val load = async { pager.loadMore() }
        while (pager.items.size < 2) yield()
        load.cancel()
        assertTrue(load.isCancelled)
        assertEquals(listOf(1, 2), pager.items)
        assertFalse(pager.exhausted)
        assertFalse(source.closed)

        pager.close()
        assertTrue(source.closed)
        assertTrue(pager.exhausted)
        assertTrue(pager.closed)
        assertEquals(emptyList<Int>(), pager.loadMore())
        gate.complete(Unit)
    }

    @Test
    fun theDefaultsMatchTheRoadmap() {
        assertEquals(50, SearchResultPager.DEFAULT_BATCH_SIZE)
        assertEquals(500, SearchResultPager.DEFAULT_LIMIT)
    }
}
