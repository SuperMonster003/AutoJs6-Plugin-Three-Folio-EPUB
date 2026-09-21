package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/** Roadmap P7.7: the crash guard flushes what was registered and then lets the crash proceed as before. */
class CrashFlushTest {

    private var original: Thread.UncaughtExceptionHandler? = null

    @Before
    fun remember() {
        CrashFlush.uninstallForTest()
        original = Thread.getDefaultUncaughtExceptionHandler()
    }

    @After
    fun restore() {
        CrashFlush.uninstallForTest()
        Thread.setDefaultUncaughtExceptionHandler(original)
    }

    @Test
    fun theGuardRunsEveryFlushAndThenHandsTheCrashToThePreviousHandler() {
        val delegated = ArrayList<Pair<Thread, Throwable>>()
        val previous = Thread.UncaughtExceptionHandler { thread, throwable -> delegated += thread to throwable }
        Thread.setDefaultUncaughtExceptionHandler(previous)
        val ran = ArrayList<String>()

        CrashFlush.register { ran += "progress" }
        CrashFlush.register { throw IllegalStateException("storage gone") }
        CrashFlush.register { ran += "bookmarks" }
        val guard = Thread.getDefaultUncaughtExceptionHandler()
        assertNotSame(previous, guard)

        val crash = RuntimeException("boom")
        guard!!.uncaughtException(Thread.currentThread(), crash)

        assertEquals(listOf("progress", "bookmarks"), ran)
        assertEquals(listOf(Thread.currentThread() to crash), delegated)
    }

    @Test
    fun aCancelledRegistrationNoLongerRunsAndTheCountFollows() {
        var runs = 0
        val kept = CrashFlush.register { runs++ }
        val dropped = CrashFlush.register { runs += 100 }
        assertEquals(2, CrashFlush.registered)

        dropped.cancel()
        assertEquals(1, CrashFlush.registered)
        assertEquals(1, CrashFlush.flushAll())
        assertEquals(1, runs)

        kept.cancel()
        assertEquals(0, CrashFlush.flushAll())
        assertEquals(1, runs)
    }

    @Test
    fun theGuardIsInstalledOnceAndUninstallingRestoresThePreviousHandler() {
        val previous = Thread.UncaughtExceptionHandler { _, _ -> }
        Thread.setDefaultUncaughtExceptionHandler(previous)

        CrashFlush.register { }
        val guard = Thread.getDefaultUncaughtExceptionHandler()
        CrashFlush.register { }
        assertSame(guard, Thread.getDefaultUncaughtExceptionHandler())

        CrashFlush.uninstallForTest()
        assertSame(previous, Thread.getDefaultUncaughtExceptionHandler())
        assertEquals(0, CrashFlush.registered)
    }
}
