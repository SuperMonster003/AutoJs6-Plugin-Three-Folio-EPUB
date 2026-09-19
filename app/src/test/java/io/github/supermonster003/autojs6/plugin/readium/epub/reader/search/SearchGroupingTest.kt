package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchGroupingTest {

    private data class Hit(val href: String, val title: String?)

    @Test
    fun aHeaderPrecedesEachRunOfHitsFromTheSameChapter() {
        val hits = listOf(
            Hit("ch1.xhtml", "Chapter 1"),
            Hit("ch1.xhtml", "Chapter 1"),
            Hit("ch3.xhtml", "Chapter 3"),
            Hit("ch1.xhtml", "Chapter 1"),
        )

        val rows = SearchGrouping.rows(hits, keyOf = { it.href }, titleOf = { it.title })

        assertEquals(
            listOf(
                SearchRow.Header("Chapter 1"),
                SearchRow.Hit(0, hits[0]),
                SearchRow.Hit(1, hits[1]),
                SearchRow.Header("Chapter 3"),
                SearchRow.Hit(2, hits[2]),
                SearchRow.Header("Chapter 1"),
                SearchRow.Hit(3, hits[3]),
            ),
            rows,
        )
    }

    @Test
    fun untitledChaptersAreHeadedByTheirHref() {
        val hits = listOf(Hit("part.xhtml", null), Hit("part.xhtml", " "))

        val rows = SearchGrouping.rows(hits, keyOf = { it.href }, titleOf = { it.title })

        assertEquals(listOf(SearchRow.Header("part.xhtml"), SearchRow.Hit(0, hits[0]), SearchRow.Hit(1, hits[1])), rows)
    }

    @Test
    fun noHitsMeansNoRows() {
        assertEquals(emptyList<SearchRow<Hit>>(), SearchGrouping.rows(emptyList<Hit>(), { it.href }, { it.title }))
    }
}
