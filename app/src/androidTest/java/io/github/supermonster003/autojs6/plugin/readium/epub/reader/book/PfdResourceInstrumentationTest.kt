package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.content.content
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.getOrElse
import java.io.File

/**
 * Roadmap P0.2 evidence: a read-only descriptor is enough for Readium to sniff, parse and expose
 * an EPUB without any copy on disk. Fixtures come from docs/fixtures (androidTest assets).
 */
@RunWith(AndroidJUnit4::class)
class PfdResourceInstrumentationTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val assets get() = InstrumentationRegistry.getInstrumentation().context.assets

    private fun fixture(name: String): File {
        val file = File(context.cacheDir, "fixture-$name")
        assets.open(name).use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }

    private fun openDescriptor(file: File): ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

    @Test
    fun rangedReadsAreExactAndThreadSafe() = runBlocking {
        val file = fixture("minimal-epub3.epub")
        val bytes = file.readBytes()
        val resource = PfdResource(openDescriptor(file), file.name)
        try {
            assertEquals(bytes.size.toLong(), resource.length().getOrElse { error(it) })
            assertArrayEquals(bytes, resource.read().getOrElse { error(it) })
            assertArrayEquals(bytes.copyOfRange(0, 4), resource.read(0L..3L).getOrElse { error(it) })
            assertArrayEquals(
                bytes.copyOfRange(bytes.size - 22, bytes.size),
                resource.read((bytes.size - 22L)..(bytes.size + 100L)).getOrElse { error(it) },
            )
            assertEquals(0, resource.read((bytes.size + 1L)..(bytes.size + 5L)).getOrElse { error(it) }.size)
            // The EPUB OCF signature: "mimetype" entry stored first, right after the local header.
            assertEquals("application/epub+zip", String(resource.read(38L..57L).getOrElse { error(it) }, Charsets.US_ASCII))

            val slices = (0 until 32).map { index ->
                async {
                    val start = (index * 97L) % bytes.size
                    val end = minOf(bytes.size - 1L, start + 511L)
                    start to resource.read(start..end).getOrElse { error(it) }
                }
            }.awaitAll()
            for ((start, slice) in slices) {
                assertArrayEquals(bytes.copyOfRange(start.toInt(), start.toInt() + slice.size), slice)
            }
        } finally {
            resource.close()
        }
        assertNull(resource.read(0L..1L).getOrNull())
    }

    @Test
    fun readiumOpensAnEpub2AndAnEpub3ThroughTheDescriptor() = runBlocking {
        val opener = BookOpener(context)
        for ((name, expectedTitle) in listOf("minimal-epub2.epub" to "Minimal EPUB 2", "minimal-epub3.epub" to "Minimal EPUB 3")) {
            val file = fixture(name)
            val resource = PfdResource(openDescriptor(file), name)
            val publication = opener.open(resource).getOrElse { error("$name: ${it.message}") }
            try {
                assertEquals(name, expectedTitle, publication.metadata.title)
                assertEquals(name, 3, publication.readingOrder.size)
                assertTrue(name, publication.tableOfContents.isNotEmpty())
                val chapter = publication.get(publication.readingOrder.first())
                assertNotNull(name, chapter)
                val html = chapter!!.read().getOrElse { error(it) }.toString(Charsets.UTF_8)
                assertTrue(name, html.contains("Chapter 1"))
            } finally {
                publication.close()
                resource.close()
            }
        }
    }

    @OptIn(ExperimentalReadiumApi::class)
    @Test
    fun positionsSearchAndContentServicesWorkThroughTheDescriptor() = runBlocking {
        val file = fixture("minimal-epub3.epub")
        val resource = PfdResource(openDescriptor(file), file.name)
        val publication = BookOpener(context).open(resource).getOrElse { error(it.message) }
        try {
            val positions = publication.positions()
            assertTrue("positions=${positions.size}", positions.size >= 3)
            assertEquals(3, positions.map { it.href.toString() }.distinct().size)

            val iterator = requireNotNull(publication.search("reef")) { "SearchService missing" }
            val hits = mutableListOf<String>()
            iterator.forEach { collection -> collection.locators.forEach { hits += it.href.toString() } }
                .getOrElse { error(it.message) }
            iterator.close()
            assertTrue(hits.toString(), hits.isNotEmpty())
            assertTrue(hits.toString(), hits.all { it.endsWith("chapter1.xhtml") })

            val text = requireNotNull(publication.content()?.text()) { "ContentService missing" }
            assertTrue(text.contains("Chapter 1") && text.contains("Chapter 2") && text.contains("Chapter 3"))
            assertTrue(text.contains("reef"))
        } finally {
            publication.close()
            resource.close()
        }
    }

    @Test
    fun malformedContainersFailWithAnErrorInsteadOfCrashing() = runBlocking {
        val opener = BookOpener(context)
        for (name in listOf(
            "malformed-not-a-zip.epub",
            "malformed-missing-container.epub",
            "malformed-missing-opf.epub",
            "malformed-encrypted-lcp.epub",
        )) {
            val file = fixture(name)
            val resource = PfdResource(openDescriptor(file), name)
            val result = opener.open(resource)
            result.getOrNull()?.close()
            resource.close()
            assertNull(name, result.getOrNull())
            assertTrue(name, result.failureOrNull()?.message?.isNotBlank() == true)
        }
    }
}
