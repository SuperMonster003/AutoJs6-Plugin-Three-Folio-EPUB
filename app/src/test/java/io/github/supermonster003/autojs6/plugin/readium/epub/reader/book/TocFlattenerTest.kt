package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TocFlattenerTest {

    private data class Node(val title: String?, val href: String?, val children: List<Node> = emptyList())

    private fun flatten(roots: List<Node>, maxDepth: Int = TocFlattener.MAX_DEPTH, maxEntries: Int = TocFlattener.MAX_ENTRIES) =
        TocFlattener.flatten(roots, Node::children, maxDepth, maxEntries)

    @Test
    fun nestedEntriesKeepDocumentOrderAndDepth() {
        val tree = listOf(
            Node("Chapter 1", "c1.xhtml", listOf(Node("Section 1.1", "c1.xhtml#s1"), Node("Section 1.2", "c1.xhtml#s2"))),
            Node("Chapter 2", "c2.xhtml"),
        )

        val rows = flatten(tree)

        assertEquals(
            listOf(0 to "Chapter 1", 1 to "Section 1.1", 1 to "Section 1.2", 0 to "Chapter 2"),
            rows.map { it.depth to it.node.title },
        )
    }

    @Test
    fun depthAndEntryLimitsBoundHostileTrees() {
        var deep = Node("leaf", "leaf.xhtml")
        repeat(20) { level -> deep = Node("level $level", "l$level.xhtml", listOf(deep)) }
        val wide = List(50) { Node("entry $it", "e$it.xhtml") }

        assertEquals(TocFlattener.MAX_DEPTH, flatten(listOf(deep)).size)
        assertEquals(TocFlattener.MAX_DEPTH - 1, flatten(listOf(deep)).last().depth)
        assertEquals(10, flatten(wide, maxEntries = 10).size)
        assertEquals(2, flatten(listOf(deep), maxDepth = 2).size)
    }

    @Test
    fun theCurrentRowMatchesTheResourceIgnoringFragments() {
        val rows = flatten(
            listOf(
                Node("Chapter 1", "c1.xhtml", listOf(Node("Section", "c1.xhtml#s1"))),
                Node("Chapter 2", "c2.xhtml#top"),
                Node("Untitled", null),
            ),
        )

        assertEquals(0, TocFlattener.indexOfHref(rows, Node::href, "c1.xhtml#anything"))
        assertEquals(2, TocFlattener.indexOfHref(rows, Node::href, "c2.xhtml"))
        assertEquals(-1, TocFlattener.indexOfHref(rows, Node::href, "c9.xhtml"))
        assertEquals(-1, TocFlattener.indexOfHref(rows, Node::href, null))
        assertEquals(-1, TocFlattener.indexOfHref(rows, Node::href, ""))
        assertEquals("Chapter 2", TocFlattener.titleForHref(rows, Node::href, Node::title, "c2.xhtml"))
        assertNull(TocFlattener.titleForHref(rows, Node::href, Node::title, "missing.xhtml"))
    }

    @Test
    fun blankTitlesDoNotNameAChapter() {
        val rows = flatten(listOf(Node("   ", "c1.xhtml")))

        assertNull(TocFlattener.titleForHref(rows, Node::href, Node::title, "c1.xhtml"))
        assertEquals("c1.xhtml", TocFlattener.resourceHref("c1.xhtml#s1"))
        assertEquals("c1.xhtml", TocFlattener.resourceHref("c1.xhtml"))
    }
}
