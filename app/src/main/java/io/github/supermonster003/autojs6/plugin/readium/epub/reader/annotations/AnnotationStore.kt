package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/** Outcome of [AnnotationStore.add]. */
internal sealed class AnnotationAddResult {
    /** A new row. */
    data class Added(val annotation: BookAnnotation) : AnnotationAddResult()

    /** The same selection was already annotated: that row took the draft's style and colour instead. */
    data class Restyled(val annotation: BookAnnotation) : AnnotationAddResult()

    data class Full(val limit: Int) : AnnotationAddResult()

    /** The draft's locator is not a locator. */
    data object Invalid : AnnotationAddResult()
}

/**
 * The highlights and notes of every book (roadmap P9 / D4): the rules of [AnnotationPolicy] over
 * the `annotations` table. Suspend calls run on Room's own executor, so any coroutine may call
 * them; a book's list is observed as a Flow that re-emits after each change.
 */
internal class AnnotationStore(private val database: AnnotationDatabase) {

    private val dao: AnnotationDao get() = database.annotations()

    fun observe(bookKey: String): Flow<List<BookAnnotation>> = dao.observe(bookKey)

    /** Every row of every book, live; the host session announcer filters it by the keys its book has had (roadmap P9.4). */
    fun observeAll(): Flow<List<BookAnnotation>> = dao.observeAll()

    suspend fun list(bookKey: String): List<BookAnnotation> = dao.list(bookKey)

    suspend fun get(id: Long): BookAnnotation? = dao.get(id)

    suspend fun count(bookKey: String): Int = dao.count(bookKey)

    suspend fun countAll(): Int = dao.countAll()

    suspend fun bookKeys(): List<String> = dao.bookKeys()

    /**
     * Stores [draft] (normalized first) unless the book is at [AnnotationPolicy.MAX_PER_BOOK]; a
     * draft of a place the book already annotated restyles that row (style, colour, and the note
     * when the draft brings one) instead of adding a second one.
     */
    suspend fun add(draft: BookAnnotation, now: Long = System.currentTimeMillis()): AnnotationAddResult {
        val normalized = AnnotationPolicy.normalize(draft.copy(id = 0L, createdAt = draft.createdAt.takeIf { it > 0L } ?: now), now)
            ?: return AnnotationAddResult.Invalid
        return database.withTransaction {
            val existing = dao.list(normalized.bookKey)
            val same = existing.firstOrNull { AnnotationPolicy.samePlace(it, normalized) }
            if (same != null) {
                val restyled = same.copy(
                    style = normalized.style,
                    color = normalized.color,
                    note = normalized.note ?: same.note,
                    updatedAt = now,
                )
                dao.update(restyled)
                AnnotationAddResult.Restyled(restyled)
            } else if (existing.size >= AnnotationPolicy.MAX_PER_BOOK) {
                AnnotationAddResult.Full(AnnotationPolicy.MAX_PER_BOOK)
            } else {
                val id = dao.insert(normalized)
                AnnotationAddResult.Added(normalized.copy(id = id))
            }
        }
    }

    /**
     * Replaces the row with [annotation]'s id by [annotation] (style, colour, note and quote
     * normalized, `updatedAt` set to [now]); false when there is no such row. The book key, the
     * locator and the creation time of the stored row are kept.
     */
    suspend fun update(annotation: BookAnnotation, now: Long = System.currentTimeMillis()): BookAnnotation? =
        database.withTransaction {
            val stored = dao.get(annotation.id) ?: return@withTransaction null
            val next = stored.copy(
                style = AnnotationStyle.normalize(annotation.style),
                color = AnnotationColors.normalize(annotation.color),
                note = AnnotationPolicy.trimNote(annotation.note),
                quote = AnnotationPolicy.trimQuote(annotation.quote) ?: stored.quote,
                chapter = AnnotationPolicy.trimChapter(annotation.chapter) ?: stored.chapter,
                updatedAt = now,
            )
            dao.update(next)
            next
        }

    /** Removes the row with [id]; false when there is none. */
    suspend fun delete(id: Long): Boolean = dao.delete(id) > 0

    /** Removes every annotation of [bookKey]; returns how many. */
    suspend fun clearBook(bookKey: String): Int = dao.deleteBook(bookKey)

    /** Removes everything (settings page "clear"); returns how many rows went. */
    suspend fun clearAll(): Int = dao.deleteEverything()

    /**
     * Moves the annotations of [fromKey] to [toKey] (the fingerprint migration of D23): when
     * [toKey] already has some, the two lists are united by [AnnotationPolicy.mergeable] and the
     * surplus of [fromKey] is dropped. Returns how many rows [toKey] holds afterwards.
     */
    suspend fun migrate(fromKey: String, toKey: String): Int {
        if (fromKey == toKey) return dao.count(toKey)
        return database.withTransaction {
            val source = dao.list(fromKey)
            if (source.isEmpty()) return@withTransaction dao.count(toKey)
            val target = dao.list(toKey)
            if (target.isEmpty()) {
                dao.rekey(fromKey, toKey)
                return@withTransaction source.size
            }
            val keep = AnnotationPolicy.mergeable(target, source).map { it.id }.toHashSet()
            val drop = source.filter { it.id !in keep }.map { it.id }
            if (drop.isNotEmpty()) dao.deleteAll(drop)
            dao.rekey(fromKey, toKey)
            target.size + keep.size
        }
    }

    /** Drops the annotations of every book not in [bookKeys] (the store's LRU eviction); returns how many rows went. */
    suspend fun retainOnly(bookKeys: Collection<String>): Int {
        val keys = bookKeys.toList()
        return if (keys.isEmpty()) dao.deleteEverything() else dao.deleteBooksNotIn(keys)
    }
}
