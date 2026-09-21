package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap P9 device evidence for the highlights database: add / restyle / cap, update, delete,
 * clear, the fingerprint migration (plain move and union), the LRU retention, and the live list.
 * Runs against a private in-memory Room database.
 */
@RunWith(AndroidJUnit4::class)
class AnnotationStoreInstrumentationTest {

    private lateinit var database: AnnotationDatabase
    private lateinit var store: AnnotationStore

    @Before
    fun openDatabase() {
        database = AnnotationDatabase.inMemory(InstrumentationRegistry.getInstrumentation().targetContext)
        store = AnnotationStore(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun locator(href: String = "OEBPS/chapter1.xhtml", progression: Double = 0.25, highlight: String = "Once upon a time"): String =
        JSONObject().apply {
            put("href", href)
            put("type", "application/xhtml+xml")
            put("locations", JSONObject().put("progression", progression).put("cssSelector", "#p1"))
            put("text", JSONObject().put("highlight", highlight))
        }.toString()

    private fun draft(key: String = KEY_A, href: String = "OEBPS/chapter1.xhtml", progression: Double = 0.25, note: String? = null, color: Int = AnnotationColors.DEFAULT, createdAt: Long = 0L) =
        BookAnnotation(bookKey = key, href = "", locator = locator(href, progression), color = color, note = note, chapter = "Chapter 1", createdAt = createdAt)

    @Test
    fun addRestylesTheSamePlaceAndRefusesBeyondTheCap() = runBlocking {
        val added = store.add(draft(note = "first"), now = 1000L) as AnnotationAddResult.Added
        assertTrue(added.annotation.id > 0L)
        assertEquals("OEBPS/chapter1.xhtml", added.annotation.href)
        assertEquals("Once upon a time", added.annotation.quote)
        assertEquals(1000L, added.annotation.createdAt)
        assertEquals(AnnotationAddResult.Invalid, store.add(draft().copy(locator = "{}")))

        val restyled = store.add(draft(color = AnnotationColors.PINK), now = 2000L) as AnnotationAddResult.Restyled
        assertEquals(added.annotation.id, restyled.annotation.id)
        assertEquals(AnnotationColors.PINK, restyled.annotation.color)
        assertEquals("first", restyled.annotation.note)
        assertEquals(2000L, restyled.annotation.updatedAt)
        assertEquals(1000L, restyled.annotation.createdAt)
        assertEquals(1, store.count(KEY_A))
        val withNote = store.add(draft(note = "second"), now = 3000L) as AnnotationAddResult.Restyled
        assertEquals("second", withNote.annotation.note)

        // The cap: fill the book through the DAO (fast), then one more add is refused while another book is not.
        val dao = database.annotations()
        for (index in 2..AnnotationPolicy.MAX_PER_BOOK) {
            dao.insert(draft(progression = index / 10000.0).copy(href = "OEBPS/chapter1.xhtml", createdAt = index.toLong()))
        }
        assertEquals(AnnotationPolicy.MAX_PER_BOOK, store.count(KEY_A))
        assertEquals(AnnotationAddResult.Full(AnnotationPolicy.MAX_PER_BOOK), store.add(draft(progression = 0.99999)))
        assertTrue(store.add(draft(key = KEY_B)) is AnnotationAddResult.Added)
        assertEquals(AnnotationPolicy.MAX_PER_BOOK + 1, store.countAll())
        assertEquals(setOf(KEY_A, KEY_B), store.bookKeys().toSet())
    }

    @Test
    fun updateDeleteAndClearKeepTheInvariants() = runBlocking {
        val a = (store.add(draft(note = "n"), now = 10L) as AnnotationAddResult.Added).annotation
        val b = (store.add(draft(progression = 0.5), now = 11L) as AnnotationAddResult.Added).annotation
        val edited = store.update(
            a.copy(bookKey = "other", locator = "{}", style = AnnotationStyle.UNDERLINE, color = 0x0064B5F6, note = "  edited \n", quote = null, createdAt = 999L),
            now = 20L,
        )
        assertNotNull(edited)
        assertEquals(KEY_A, edited!!.bookKey)
        assertEquals(a.locator, edited.locator)
        assertEquals(10L, edited.createdAt)
        assertEquals(20L, edited.updatedAt)
        assertEquals(AnnotationStyle.UNDERLINE, edited.style)
        assertEquals(AnnotationColors.BLUE, edited.color)
        assertEquals("edited", edited.note)
        assertEquals("Once upon a time", edited.quote)
        assertEquals(edited, store.get(a.id))
        assertNull(store.update(a.copy(id = 12345L)))

        assertTrue(store.delete(b.id))
        assertFalse(store.delete(b.id))
        assertEquals(listOf(a.id), store.list(KEY_A).map { it.id })
        store.add(draft(key = KEY_B))
        assertEquals(1, store.clearBook(KEY_A))
        assertEquals(1, store.countAll())
        assertEquals(1, store.clearAll())
        assertEquals(0, store.countAll())
    }

    @Test
    fun migrationMovesOrUnitesAndRetentionDropsEvictedBooks() = runBlocking {
        val quick = KEY_A
        val full = KEY_B
        store.add(draft(key = quick, progression = 0.1), now = 1L)
        store.add(draft(key = quick, progression = 0.2), now = 2L)
        assertEquals(2, store.migrate(quick, full))
        assertEquals(0, store.count(quick))
        assertEquals(2, store.count(full))
        assertEquals(2, store.migrate(full, full))

        // A second copy of the book annotated under a fresh quick key: the union keeps the target's rows on the same place.
        val other = KEY_C
        store.add(draft(key = other, progression = 0.2, note = "dup"), now = 3L)
        store.add(draft(key = other, progression = 0.3), now = 4L)
        assertEquals(3, store.migrate(other, full))
        val united = store.list(full)
        assertEquals(listOf(0.1, 0.2, 0.3), united.map { AnnotationPolicy.progressionOf(it.locator)!! }.sorted())
        assertNull(united.first { AnnotationPolicy.progressionOf(it.locator) == 0.2 }.note)
        assertEquals(0, store.count(other))
        assertEquals(0, store.migrate("0".repeat(64), full).let { it - 3 })

        store.add(draft(key = other))
        assertEquals(1, store.retainOnly(listOf(full, "f".repeat(64))))
        assertEquals(setOf(full), store.bookKeys().toSet())
        assertEquals(3, store.retainOnly(emptyList()))
        assertEquals(0, store.countAll())
    }

    @Test
    fun theLiveListFollowsEveryChange() = runBlocking {
        val flow = store.observe(KEY_A)
        assertTrue(withTimeout(10_000) { flow.first() }.isEmpty())
        val added = (store.add(draft()) as AnnotationAddResult.Added).annotation
        assertEquals(listOf(added.id), withTimeout(10_000) { flow.first { it.isNotEmpty() } }.map { it.id })
        store.update(added.copy(note = "note"))
        assertEquals("note", withTimeout(10_000) { flow.first { it.firstOrNull()?.note != null } }.first().note)
        store.delete(added.id)
        assertTrue(withTimeout(10_000) { flow.first { it.isEmpty() } }.isEmpty())
    }

    private companion object {
        val KEY_A = "a".repeat(64)
        val KEY_B = "b".repeat(64)
        val KEY_C = "c".repeat(64)
    }
}
