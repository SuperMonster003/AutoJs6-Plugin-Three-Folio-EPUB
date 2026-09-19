package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.BookmarkSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.Bookmark
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookmarkCodec
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Link
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P2.6 device evidence: the toolbar icon adds a bookmark for the page on screen (with
 * the chapter title and an excerpt of the first visible element) and removes it again, the
 * panel lists the bookmarks newest first, jumps to one, deletes one and clears them all, the
 * bookmarks survive a relaunch (and the restored page shows the filled icon), the per-book cap
 * of 500 is enforced from a stored file, and fixed-layout pages are bookmarked by resource.
 * Screenshots and notes land in `files/p2-evidence/bookmarks-*`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderBookmarksInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)
    private val preferencesStore get() = ReaderPreferencesStore.forFilesDirectory(context.filesDir)

    @Before
    fun resetState() = cleanUp()

    @After
    fun cleanUp() {
        booksDirectory.deleteRecursively()
        preferencesStore.clear()
        context.getSharedPreferences(ReaderSettings.PREFERENCES_NAME, 0).edit().clear().commit()
    }

    @Test
    fun bookmarksAreAddedListedOpenedDeletedAndKeptAcrossRelaunch() {
        val lines = ArrayList<String>()
        var activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            await("toolbar offers to add") { menuTitle(activity) == activity.getString(R.string.text_bookmark_add) }
            assertTrue(activity.bookmarks.value.isEmpty())

            val started = SystemClock.uptimeMillis()
            main { activity.toggleBookmark() }
            await("bookmark added") { activity.bookmarks.value.size == 1 && activity.currentBookmark.value != null }
            val addMillis = SystemClock.uptimeMillis() - started
            val first = activity.bookmarks.value.single()
            assertTrue(first.href, first.href.endsWith("chapter1.xhtml"))
            assertEquals("Chapter 1", first.chapter)
            assertFalse("snippet: ${first.snippet}", first.snippet.isNullOrBlank())
            assertTrue(first.locator.getJSONObject("text").getString("highlight").isNotBlank())
            await("toolbar offers to remove") { menuTitle(activity) == activity.getString(R.string.text_bookmark_remove) }
            SystemClock.sleep(500)
            screenshot(activity.window, "bookmarks-page")

            // Toggling again removes it and the icon follows.
            main { activity.toggleBookmark() }
            await("bookmark removed") { activity.bookmarks.value.isEmpty() && activity.currentBookmark.value == null }
            await("toolbar offers to add again") { menuTitle(activity) == activity.getString(R.string.text_bookmark_add) }

            // One bookmark in chapter 1, a newer one in chapter 3.
            main { activity.toggleBookmark() }
            await("chapter 1 bookmarked") { activity.bookmarks.value.size == 1 }
            val chapter1 = activity.bookmarks.value.single()
            main { activity.jumpTo(link(activity, "chapter3.xhtml")) }
            awaitHref(activity, "chapter3.xhtml")
            await("chapter 3 has no bookmark") { activity.currentBookmark.value == null }
            SystemClock.sleep(300) // distinct timestamps keep the newest-first order unambiguous
            main { activity.toggleBookmark() }
            await("chapter 3 bookmarked") { activity.bookmarks.value.size == 2 && activity.currentBookmark.value != null }
            val chapter3 = requireNotNull(activity.currentBookmark.value)
            assertEquals("Chapter 3", chapter3.chapter)

            var sheet = openSheet(activity)
            await("two rows newest first") { sheet.rows.map { it.id } == listOf(chapter3.id, chapter1.id) }
            main {
                assertFalse(sheet.requireView().findViewById<View>(R.id.bookmark_add).isEnabled)
                assertTrue(sheet.requireView().findViewById<View>(R.id.bookmark_clear).isEnabled)
                assertFalse(sheet.requireView().findViewById<View>(R.id.bookmark_empty).isVisible)
            }
            SystemClock.sleep(800)
            screenshot(requireNotNull(sheet.dialog?.window), "bookmarks-panel")

            // Tapping the older row jumps back to chapter 1, where the icon fills again.
            main { sheet.open(1) }
            awaitHref(activity, "chapter1.xhtml")
            await("sheet dismissed") { activity.bookmarkSheet == null }
            await("chapter 1 page is bookmarked") { activity.currentBookmark.value?.id == chapter1.id }

            // Deleting from the panel.
            sheet = openSheet(activity)
            await("rows") { sheet.rows.size == 2 }
            main { activity.removeBookmark(chapter3.id) }
            await("one row left") { sheet.rows.map { it.id } == listOf(chapter1.id) && activity.bookmarks.value.size == 1 }
            main { sheet.dismiss() }
            await("sheet closed") { activity.bookmarkSheet == null }
            await("bookmarks file written") { bookmarkFiles().isNotEmpty() }
            lines += "addMillis=$addMillis chapter=${first.chapter} snippet=${first.snippet}"
            lines += "cssSelector=${first.locator.getJSONObject("locations").optString("cssSelector")}"
            lines += "file=${bookmarkFiles().first().parentFile?.name?.take(12)}... bytes=${bookmarkFiles().first().length()}"
        } finally {
            finish(activity)
        }

        // Relaunch: the bookmark is back and the restored page shows the filled icon.
        activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            await("bookmark survives a relaunch") { activity.bookmarks.value.size == 1 }
            val restored = activity.bookmarks.value.single()
            assertEquals("Chapter 1", restored.chapter)
            await("restored page is bookmarked") { activity.currentBookmark.value?.id == restored.id }
            await("toolbar offers to remove after relaunch") { menuTitle(activity) == activity.getString(R.string.text_bookmark_remove) }

            val sheet = openSheet(activity)
            await("row") { sheet.rows.size == 1 }
            main { activity.clearBookmarks() }
            await("cleared") { activity.bookmarks.value.isEmpty() && sheet.rows.isEmpty() }
            main {
                assertTrue(sheet.requireView().findViewById<View>(R.id.bookmark_empty).isVisible)
                assertFalse(sheet.requireView().findViewById<View>(R.id.bookmark_clear).isEnabled)
                assertTrue(sheet.requireView().findViewById<View>(R.id.bookmark_add).isEnabled)
            }
            await("file removed") { bookmarkFiles().isEmpty() }
            main { sheet.dismiss() }
            lines += "relaunch=kept clear=file-removed"
            record("bookmarks-api${Build.VERSION.SDK_INT}.txt", listOf("device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})") + lines)
        } finally {
            finish(activity)
        }
    }

    @Test
    fun theLimitIsEnforcedFromAStoredFile() {
        var activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            main { activity.toggleBookmark() }
            await("stored") { bookmarkFiles().isNotEmpty() }
        } finally {
            finish(activity)
        }
        // Fill the file to the cap: ids 0..499 in chapter 2, none on the page that opens.
        val file = bookmarkFiles().single()
        val full = (0 until BookmarkCodec.MAX_BOOKMARKS).map { index ->
            Bookmark(
                id = index.toLong(),
                locator = JSONObject()
                    .put("href", "OEBPS/chapter2.xhtml")
                    .put("type", "application/xhtml+xml")
                    .put("locations", JSONObject().put("progression", index / 1000.0)),
                createdAtMillis = 1_700_000_000_000L + index,
                chapter = "Chapter 2",
                snippet = "Filler $index",
            )
        }
        file.writeText(BookmarkCodec.encode(full))

        activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            await("all loaded") { activity.bookmarks.value.size == BookmarkCodec.MAX_BOOKMARKS }
            assertNull(activity.currentBookmark.value)
            val result = onMain { activity.readerModel.addBookmark(JSONObject().put("href", "x.xhtml").put("type", "text/html"), null, null) }
            assertTrue("$result", result is BookmarkAddResult.Full)
            main { activity.toggleBookmark() }
            SystemClock.sleep(1500)
            assertEquals(BookmarkCodec.MAX_BOOKMARKS, activity.bookmarks.value.size)
            assertNull(activity.currentBookmark.value)

            val sheet = openSheet(activity)
            await("all rows") { sheet.rows.size == BookmarkCodec.MAX_BOOKMARKS }
            assertEquals(BookmarkCodec.MAX_BOOKMARKS - 1L, sheet.rows.first().id) // newest first
            main { activity.removeBookmark(sheet.rows.first().id) }
            await("one freed") { activity.bookmarks.value.size == BookmarkCodec.MAX_BOOKMARKS - 1 }
            main { sheet.dismiss() }
            await("sheet closed") { activity.bookmarkSheet == null }
            main { activity.toggleBookmark() }
            await("added again") { activity.bookmarks.value.size == BookmarkCodec.MAX_BOOKMARKS && activity.currentBookmark.value != null }
            record(
                "bookmarks-limit-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "loaded=${BookmarkCodec.MAX_BOOKMARKS} addAtCap=$result fileBytes=${file.length()}",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    @Test
    fun fixedLayoutPagesAreBookmarkedByResource() {
        val activity = instrumentation.startActivitySync(request(FIXED)) as EpubReaderActivity
        try {
            awaitHref(activity, "page1.xhtml")
            await("toolbar offers to add") { menuTitle(activity) == activity.getString(R.string.text_bookmark_add) }
            main { activity.toggleBookmark() }
            await("page 1 bookmarked") { activity.bookmarks.value.size == 1 && activity.currentBookmark.value != null }
            val bookmark = activity.bookmarks.value.single()
            assertTrue(bookmark.href, bookmark.href.endsWith("page1.xhtml"))
            assertFalse(bookmark.chapter.isNullOrBlank())
            assertNull(bookmark.snippet) // fixed layouts have no first visible element

            main { activity.jumpTo(link(activity, "page2.xhtml")) }
            awaitHref(activity, "page2.xhtml")
            await("page 2 has no bookmark") { activity.currentBookmark.value == null }
            await("toolbar offers to add on page 2") { menuTitle(activity) == activity.getString(R.string.text_bookmark_add) }

            val sheet = openSheet(activity)
            await("row") { sheet.rows.size == 1 }
            main { sheet.open(0) }
            awaitHref(activity, "page1.xhtml")
            await("page 1 bookmarked again") { activity.currentBookmark.value?.id == bookmark.id }
            record(
                "bookmarks-fixed-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "href=${bookmark.href} chapter=${bookmark.chapter}",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    // ---- helpers ----

    private fun openSheet(activity: EpubReaderActivity): BookmarkSheet {
        main { activity.showBookmarks() }
        await("bookmark sheet") { activity.bookmarkSheet?.view != null }
        return requireNotNull(activity.bookmarkSheet)
    }

    private fun menuTitle(activity: EpubReaderActivity): String? =
        onMain { activity.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.action_bookmark)?.takeIf { it.isVisible }?.title?.toString() }

    private fun link(activity: EpubReaderActivity, suffix: String): Link =
        requireNotNull(activity.readerModel.publication).readingOrder.first { it.href.toString().endsWith(suffix) }

    private fun bookmarkFiles(): List<File> =
        booksDirectory.listFiles().orEmpty().mapNotNull { File(it, BookDataStore.BOOKMARKS_FILE).takeIf(File::isFile) }

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

    private fun finish(activity: EpubReaderActivity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(500) // let the pending bookmark / progress writes land before the next launch
    }

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

    private fun currentHref(activity: EpubReaderActivity): String? = onMain {
        (activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)
            ?.takeIf { it.view != null }
            ?.currentLocator?.value?.href?.toString()
    }

    private fun awaitHref(activity: EpubReaderActivity, suffix: String) {
        await("href $suffix") { activity.navigatorReady && currentHref(activity)?.endsWith(suffix) == true }
    }

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
        const val FIXED = "fixed-layout.epub"
    }
}
