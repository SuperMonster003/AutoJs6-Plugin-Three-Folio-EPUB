package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.core.view.isVisible
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.LauncherActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBook
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBooksStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P4.1 device evidence: the launcher lists the recent books, opens them in the reader
 * through the plugin's own explicit action, the reader fills the entry (fingerprint, title,
 * author, cover, progress), an unreadable file is marked unavailable and can be removed (which
 * releases its grant), and a picked document whose grant cannot be persisted opens once without
 * being listed. The document picker itself is the system's; its result handler is called
 * directly. Notes land in `files/p2-evidence/launcher-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderLauncherInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val store get() = RecentBooksStore.forFilesDirectory(context.filesDir)
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)
    private val notes = ArrayList<String>()
    private var readerMonitor: Instrumentation.ActivityMonitor? = null

    @Before
    fun resetState() {
        cleanUp()
        notes += "api=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}"
        readerMonitor = instrumentation.addMonitor(EpubReaderActivity::class.java.name, null, false)
    }

    @After
    fun cleanUp() {
        readerMonitor?.let { instrumentation.removeMonitor(it) }
        readerMonitor = null
        store.clear()
        booksDirectory.deleteRecursively()
        ReaderPreferencesStore.forFilesDirectory(context.filesDir).clear()
    }

    @Test
    fun aListedBookOpensInTheReaderAndComesBackEnriched() {
        val uri = provide(FIXTURE)
        val seeded = RecentBook(uri.toString(), FIXTURE, addedAt = 1_700_000_000_000L, lastReadAt = 1_700_000_000_000L)
        store.upsert(seeded)

        val launcher = startLauncher()
        await("book listed") { launcher.shownBooks.size == 1 }
        assertEquals(FIXTURE, launcher.shownBooks.single().displayName)
        assertTrue(onMain { launcher.findViewById<View>(R.id.recent_books).isVisible })
        assertFalse(onMain { launcher.findViewById<View>(R.id.empty_view).isVisible })
        screenshot(launcher.window, "launcher-grid")

        main { launcher.openBook(launcher.shownBooks.single()) }
        val reader = awaitReader()
        try {
            awaitHref(reader, "chapter1.xhtml")
            assertEquals(uri.toString(), onMain { reader.readerModel.recentUri })
            await("entry enriched") { store.read().single().let { it.key != null && it.title != null } }
            val enriched = store.read().single()
            notes += "title=${enriched.title} author=${enriched.author} key=${enriched.key?.take(12)}"

            // Reading on updates the progress and the last read time of the tile.
            val link = requireNotNull(reader.readerModel.publication).readingOrder.first { it.href.toString().endsWith("chapter2.xhtml") }
            main { reader.jumpTo(link) }
            awaitHref(reader, "chapter2.xhtml")
            main { reader.readerModel.flushProgress() }
            await("progress on the tile") { store.read().single().progression != null }
            val read = store.read().single()
            assertTrue(read.lastReadAt > seeded.lastReadAt)
            // The fixture declares no cover image, so the tile keeps the placeholder; a cover file appears only when Readium finds one.
            notes += "progression=${read.progression} cover=${read.coverFile?.let { "present" } ?: "absent"}"
            SystemClock.sleep(1500)
            notes += "cover-file=${read.coverFile?.let { store.coverFile(it).isFile } ?: "none"}"
        } finally {
            finish(reader)
        }

        // Back in the launcher the tile reflects the reading.
        await("reader closed") { reader.isDestroyed }
        main { launcher.recreate() }
        SystemClock.sleep(500)
        val again = awaitLauncher()
        await("tile refreshed") { again.shownBooks.singleOrNull()?.progression != null }
        notes += "tile-progression=${again.shownBooks.single().progression}"
        record("launcher-api${Build.VERSION.SDK_INT}.txt", notes)
        finish(again)
    }

    @Test
    fun removingBooksReleasesThemAndShowsTheEmptyState() {
        val first = provide(FIXTURE)
        val second = provide(MANY)
        store.upsert(RecentBook(first.toString(), FIXTURE, 1L, 1L))
        store.upsert(RecentBook(second.toString(), MANY, 2L, 2L))
        val launcher = startLauncher()
        try {
            await("two listed") { launcher.shownBooks.size == 2 }
            assertEquals(MANY, launcher.shownBooks.first().displayName) // newest first
            main { launcher.removeBook(launcher.shownBooks.first()) }
            await("one left") { launcher.shownBooks.size == 1 }
            assertEquals(FIXTURE, launcher.shownBooks.single().displayName)
            main { launcher.removeBook(launcher.shownBooks.single()) }
            await("empty") { launcher.shownBooks.isEmpty() }
            await("empty state") { launcher.findViewById<View>(R.id.empty_view).isVisible }
            assertTrue(store.read().isEmpty())
            assertFalse(File(context.filesDir, RecentBooksStore.FILE_NAME).exists())
            assertTrue(context.contentResolver.persistedUriPermissions.none { it.uri == first || it.uri == second })
        } finally {
            finish(launcher)
        }
    }

    @Test
    fun anUnreadableBookIsMarkedUnavailableAndOffersRemoval() {
        val missing = EpubReaderTestContentProvider.documentUri("missing-book.epub")
        EpubReaderTestContentProvider.fileFor(context, "missing-book.epub").delete()
        store.upsert(RecentBook(missing.toString(), "missing-book.epub", 1L, 1L))
        val launcher = startLauncher()
        await("listed") { launcher.shownBooks.size == 1 }
        assertTrue(launcher.shownBooks.single().available)

        main { launcher.openBook(launcher.shownBooks.single()) }
        val reader = awaitReader()
        await("open failed") { reader.readerModel.state.value is OpenState.Failed }
        assertTrue((reader.readerModel.state.value as OpenState.Failed).failure is OpenFailure.CannotRead)
        await("marked unavailable") { store.read().single().available == false }
        finish(reader)
        await("reader closed") { reader.isDestroyed }

        main { launcher.recreate() }
        SystemClock.sleep(500)
        val again = awaitLauncher()
        try {
            await("shown unavailable") { again.shownBooks.singleOrNull()?.available == false }
            main { again.openBook(again.shownBooks.single()) }
            await("removal offered") { again.bookDialog != null }
            main { again.bookDialog?.dismiss() }
            main { again.removeBook(again.shownBooks.single()) }
            await("removed") { again.shownBooks.isEmpty() }
        } finally {
            finish(again)
        }
    }

    @Test
    fun aPickedDocumentWhoseGrantCannotPersistOpensOnceWithoutBeingListed() {
        val uri = provide(FIXTURE)
        val launcher = startLauncher()
        await("empty state") { launcher.findViewById<View>(R.id.empty_view).isVisible }
        // The test provider runs in this very process, so the system records no grant to persist.
        val persistable = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        assertFalse("a same-process provider offers no persistable grant", persistable)
        main { launcher.onDocumentPicked(uri) }
        val reader = awaitReader()
        try {
            awaitHref(reader, "chapter1.xhtml")
            assertNull("not listed, so not tracked", onMain { reader.readerModel.recentUri })
            assertTrue(store.read().isEmpty())
        } finally {
            finish(reader)
            finish(launcher)
        }
    }

    // ---- helpers ----

    private fun provide(fixture: String): Uri {
        val target = EpubReaderTestContentProvider.fileFor(context, fixture)
        assets.open(fixture).use { input -> target.outputStream().use { input.copyTo(it) } }
        return EpubReaderTestContentProvider.documentUri(fixture)
    }

    private fun startLauncher(): LauncherActivity =
        instrumentation.startActivitySync(
            Intent(context, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as LauncherActivity

    private fun awaitLauncher(): LauncherActivity {
        var found: LauncherActivity? = null
        await("launcher resumed") {
            found = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                .filterIsInstance<LauncherActivity>()
                .firstOrNull()
            found != null
        }
        return found!!
    }

    private fun awaitReader(): EpubReaderActivity {
        val activity = readerMonitor!!.waitForActivityWithTimeout(30_000) ?: fail("The reader did not start")
        return activity as EpubReaderActivity
    }

    private fun currentHref(activity: EpubReaderActivity): String? = onMain {
        (activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)
            ?.takeIf { it.view != null }
            ?.currentLocator?.value?.href?.toString()
    }

    private fun awaitHref(activity: EpubReaderActivity, suffix: String) {
        await("href $suffix") { activity.navigatorReady && currentHref(activity)?.endsWith(suffix) == true }
    }

    private fun finish(activity: Activity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
    }

    private fun screenshot(window: Window, name: String) {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decor = window.decorView
            if (decor.width == 0 || decor.height == 0) return
            val target = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            val done = CountDownLatch(1)
            var result = PixelCopy.ERROR_UNKNOWN
            PixelCopy.request(window, target, { result = it; done.countDown() }, Handler(Looper.getMainLooper()))
            if (!done.await(10, TimeUnit.SECONDS) || result != PixelCopy.SUCCESS) return
            target
        } else {
            instrumentation.uiAutomation.takeScreenshot() ?: return
        }
        outputFile("$name-api${Build.VERSION.SDK_INT}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun record(name: String, lines: List<String>) {
        outputFile(name).writeText(lines.joinToString("\n") + "\n")
    }

    private fun outputFile(name: String): File =
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name)

    private fun await(message: String, timeoutMillis: Long = 30000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = runCatching(condition).getOrDefault(false) }
            if (ready) return
            SystemClock.sleep(100)
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

    private companion object {
        const val FIXTURE = "minimal-epub3.epub"
        const val MANY = "malformed-many-entries.epub"
    }
}
