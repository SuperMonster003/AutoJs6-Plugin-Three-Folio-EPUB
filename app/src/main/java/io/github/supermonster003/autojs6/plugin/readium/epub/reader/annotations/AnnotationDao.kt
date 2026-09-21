package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Row access of the `annotations` table (roadmap P9); the rules live in [AnnotationStore]. */
@Dao
internal interface AnnotationDao {

    @Query("SELECT * FROM annotations WHERE bookKey = :bookKey ORDER BY id")
    fun observe(bookKey: String): Flow<List<BookAnnotation>>

    @Query("SELECT * FROM annotations WHERE bookKey = :bookKey ORDER BY id")
    suspend fun list(bookKey: String): List<BookAnnotation>

    @Query("SELECT * FROM annotations WHERE id = :id")
    suspend fun get(id: Long): BookAnnotation?

    @Query("SELECT COUNT(*) FROM annotations WHERE bookKey = :bookKey")
    suspend fun count(bookKey: String): Int

    @Query("SELECT COUNT(*) FROM annotations")
    suspend fun countAll(): Int

    @Query("SELECT DISTINCT bookKey FROM annotations")
    suspend fun bookKeys(): List<String>

    @Insert
    suspend fun insert(annotation: BookAnnotation): Long

    @Update
    suspend fun update(annotation: BookAnnotation): Int

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM annotations WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<Long>): Int

    @Query("DELETE FROM annotations WHERE bookKey = :bookKey")
    suspend fun deleteBook(bookKey: String): Int

    @Query("DELETE FROM annotations WHERE bookKey NOT IN (:bookKeys)")
    suspend fun deleteBooksNotIn(bookKeys: List<String>): Int

    @Query("DELETE FROM annotations")
    suspend fun deleteEverything(): Int

    @Query("UPDATE annotations SET bookKey = :toKey WHERE bookKey = :fromKey")
    suspend fun rekey(fromKey: String, toKey: String): Int
}
