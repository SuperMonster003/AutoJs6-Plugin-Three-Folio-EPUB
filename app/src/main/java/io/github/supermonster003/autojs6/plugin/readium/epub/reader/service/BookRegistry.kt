package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.os.SystemClock
import org.autojs.plugin.epub.api.EpubContract
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * The books one service instance keeps open (roadmap P5.2, appendix B.2): at most
 * `MAX_OPEN_BOOKS` at a time, each closed after `BOOK_IDLE_TIMEOUT_MS` without a call, all of
 * them when the host unbinds or the service dies.
 */
internal class BookRegistry(
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val idleTimeoutMillis: Long = EpubContract.BOOK_IDLE_TIMEOUT_MS,
    sweepIntervalMillis: Long = SWEEP_INTERVAL_MILLIS,
) {

    private val lock = Any()
    private val books = LinkedHashSet<EpubBookBinder>()
    private val sweeper: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "epub-book-sweeper").apply { isDaemon = true }
    }

    init {
        sweeper.scheduleWithFixedDelay({ runCatching { sweep(clock()) } }, sweepIntervalMillis, sweepIntervalMillis, TimeUnit.MILLISECONDS)
    }

    val size: Int get() = synchronized(lock) { books.size }

    /** Refuses the ninth book before the caller spends time parsing it. */
    fun checkCapacity() = synchronized(lock) { Limits.openBooks(books.size) }

    fun register(book: EpubBookBinder) = synchronized(lock) {
        Limits.openBooks(books.size)
        books += book
    }

    fun remove(book: EpubBookBinder) = synchronized(lock) {
        books -= book
    }

    /** Closes the books nobody touched for the idle timeout; returns how many were closed. */
    fun sweep(now: Long): Int {
        val idle = synchronized(lock) { books.filter { now - it.lastUsedAt >= idleTimeoutMillis } }
        idle.forEach { it.release() }
        return idle.size
    }

    fun closeAll() {
        val open = synchronized(lock) { books.toList() }
        open.forEach { it.release() }
    }

    fun shutdown() {
        sweeper.shutdownNow()
        closeAll()
    }

    companion object {
        const val SWEEP_INTERVAL_MILLIS = 30_000L
    }
}
