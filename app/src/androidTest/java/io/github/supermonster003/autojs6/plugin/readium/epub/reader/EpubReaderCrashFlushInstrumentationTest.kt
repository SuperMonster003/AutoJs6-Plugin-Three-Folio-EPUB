package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.CrashFlush
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import java.security.MessageDigest

/**
 * Roadmap P7.7 (DEVICE evidence): while a book is open the reader keeps one crash-flush registration,
 * and running it writes the current position to disk synchronously, without waiting for the throttle
 * or the persistence scope; the registration goes away with the reader. The plugin also plants no
 * Timber tree, so Readium's logging is a no-op on the device. Notes land in
 * `files/p2-evidence/crash-flush-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderCrashFlushInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)
    private val notes = ArrayList<String>()

    @Before
    fun resetState() {
        booksDirectory.deleteRecursively()
    }

    @After
    fun cleanUp() {
        booksDirectory.deleteRecursively()
    }

    @Test
    fun theCrashGuardWritesTheCurrentPositionAtOnceAndLeavesWithTheReader() {
        val expectedKey = assets.open(FIXTURE).use { input ->
            MessageDigest.getInstance("SHA-256").digest(input.readBytes()).joinToString("") { "%02x".format(it) }
        }
        val timberTrees = runCatching { Class.forName("timber.log.Timber").getMethod("treeCount").invoke(null) as Int }
        note("Timber trees planted: ${timberTrees.getOrNull() ?: "Timber not on the classpath"}")
        assertEquals(0, timberTrees.getOrDefault(0))

        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            await("full fingerprint") { activity.readerModel.bookKey == expectedKey }
            note("registrations while the book is open: ${CrashFlush.registered}")
            assertEquals(1, CrashFlush.registered)

            // Two jumps inside the throttle window: the second position is at best parked as pending.
            main { navigator(activity).go(requireNotNull(activity.readerModel.publication).readingOrder[1], animated = false) }
            await("chapter 2") { currentHref(activity)?.endsWith("chapter2.xhtml") == true }
            main { navigator(activity).go(requireNotNull(activity.readerModel.publication).readingOrder[2], animated = false) }
            await("chapter 3 reached the model") { activity.readerModel.lastLocator?.href?.toString()?.endsWith("chapter3.xhtml") == true }
            val before = storedHref(expectedKey)
            note("stored before the guard ran: $before")

            val started = SystemClock.elapsedRealtime()
            val succeeded = CrashFlush.flushAll()
            val millis = SystemClock.elapsedRealtime() - started
            val after = storedHref(expectedKey)
            note("guard: $succeeded flush(es) succeeded in $millis ms; stored right after: $after")
            assertEquals(1, succeeded)
            assertTrue("the position was on disk the moment the guard returned: $after", after?.endsWith("chapter3.xhtml") == true)
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
        await("the registration left with the reader") { CrashFlush.registered == 0 }
        note("registrations after the reader closed: ${CrashFlush.registered}")
        writeNotes("crash-flush")
    }

    private fun storedHref(key: String): String? =
        BookDataStore(booksDirectory).readProgress(key)?.locator?.optString("href")

    private fun request(fixture: String): Intent {
        val target = EpubReaderTestContentProvider.fileFor(context, fixture)
        assets.open(fixture).use { input -> target.outputStream().use { input.copyTo(it) } }
        val documentUri = EpubReaderTestContentProvider.documentUri(fixture)
        val parentUri = EpubReaderTestContentProvider.parentUri()
        return Intent(context, EpubReaderActivity::class.java)
            .setAction(ExplorerActionPluginActions.EXECUTE)
            .setDataAndType(documentUri, ReadiumEpubReaderPlugin.EPUB_MIME_TYPE)
            .putExtra(ExplorerActionIntentExtras.ACTION_ID, ReadiumEpubReaderPlugin.PRIMARY_ACTION_ID)
            .putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, ReadiumEpubReaderPlugin.PROTOCOL_VERSION)
            .putExtra(ExplorerActionIntentExtras.HOST_PACKAGE_NAME, "org.autojs.autojs6")
            .putExtra(ExplorerActionIntentExtras.HOST_VERSION_CODE, EpubReaderExplorerCompatibility.maximumAuditedHostVersionCode)
            .putExtra(ExplorerActionIntentExtras.SOURCE_SURFACE, ExplorerActionIntentValues.SOURCE_SURFACE_MAIN)
            .putExtra(ExplorerActionIntentExtras.PARENT_URI, parentUri)
            .putExtra(ExplorerActionIntentExtras.DISPLAY_NAME, fixture)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            .apply {
                clipData = ClipData.newRawUri("Document", documentUri).apply { addItem(ClipData.Item(parentUri)) }
            }
    }

    private fun navigator(activity: EpubReaderActivity): EpubNavigatorFragment =
        requireNotNull(activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)

    private fun currentHref(activity: EpubReaderActivity): String? = onMain {
        (activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)
            ?.takeIf { it.view != null }
            ?.currentLocator?.value?.href?.toString()
    }

    private fun await(message: String, timeoutMillis: Long = 30_000L, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = runCatching(condition).getOrDefault(false) }
            if (ready) return
            SystemClock.sleep(200)
        }
        fail("Timed out: $message")
    }

    private fun main(action: () -> Unit) = onMain(action)

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private fun note(line: String) {
        notes += line
    }

    private fun writeNotes(name: String) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }
            .resolve("$name-api${Build.VERSION.SDK_INT}.txt")
            .writeText(notes.joinToString("\n") + "\n")
    }

    private companion object {
        const val FIXTURE = "minimal-epub3.epub"
    }
}
