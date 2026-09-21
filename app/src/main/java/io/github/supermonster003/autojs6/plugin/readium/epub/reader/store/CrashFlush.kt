package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Work that must reach disk even when the process is about to die from an uncaught exception
 * (roadmap P7.7: the reading position is saved before the crash). An owner registers a synchronous
 * flush while it holds unsaved state and cancels it when that state is gone. The first registration
 * installs the guard as the default uncaught-exception handler; on a crash it runs every flush (each
 * on its own, so a failing flush cannot stop the others) and then hands the throwable to the handler
 * that was there before, so the system still reports the crash and restarts the app exactly as it
 * would without the guard. Nothing is logged: the plugin writes no logs at all (roadmap P7.7).
 */
object CrashFlush {

    class Registration internal constructor(internal val flush: () -> Unit) {
        fun cancel() {
            flushes.remove(this)
        }
    }

    private val flushes = CopyOnWriteArrayList<Registration>()
    private val installed = AtomicBoolean(false)
    private var previous: Thread.UncaughtExceptionHandler? = null

    val registered: Int get() = flushes.size

    fun register(flush: () -> Unit): Registration {
        install()
        return Registration(flush).also(flushes::add)
    }

    /** Runs every registered flush now (the crash path; tests call it too) and returns how many succeeded. */
    fun flushAll(): Int {
        var succeeded = 0
        for (registration in flushes) {
            if (runCatching(registration.flush).isSuccess) succeeded++
        }
        return succeeded
    }

    private fun install() {
        if (!installed.compareAndSet(false, true)) return
        previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(Guard())
    }

    private class Guard : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            runCatching { flushAll() }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Test seam: forgets every registration and hands the default handler back to what preceded the guard. */
    internal fun uninstallForTest() {
        flushes.clear()
        if (installed.compareAndSet(true, false)) {
            Thread.setDefaultUncaughtExceptionHandler(previous)
            previous = null
        }
    }
}
