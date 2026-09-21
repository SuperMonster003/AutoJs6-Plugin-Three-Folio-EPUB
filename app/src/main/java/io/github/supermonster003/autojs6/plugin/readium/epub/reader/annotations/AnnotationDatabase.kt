package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * The highlights and notes database (roadmap P9 / D4 / D33): one table, `annotations`, in the
 * plugin's private `databases/` directory. Schema version 1 is exported to `app/schemas/` so a
 * later version can be migrated and the migration tested against the recorded schema.
 */
@Database(entities = [BookAnnotation::class], version = 1, exportSchema = true)
internal abstract class AnnotationDatabase : RoomDatabase() {

    abstract fun annotations(): AnnotationDao

    companion object {
        const val NAME = "annotations.db"

        @Volatile
        private var instance: AnnotationDatabase? = null

        /** The process-wide database; opened lazily on first access. */
        fun get(context: Context): AnnotationDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AnnotationDatabase::class.java, NAME)
                .build()
                .also { instance = it }
        }

        /** A private in-memory database for tests. */
        fun inMemory(context: Context): AnnotationDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, AnnotationDatabase::class.java).build()

        /** Bytes the database and its journal files take on disk (the settings page's usage line). */
        fun sizeOnDisk(context: Context): Long {
            val file = context.getDatabasePath(NAME)
            return listOf(file, File(file.path + "-wal"), File(file.path + "-shm"), File(file.path + "-journal"))
                .filter { it.isFile }
                .sumOf { it.length() }
        }
    }
}
