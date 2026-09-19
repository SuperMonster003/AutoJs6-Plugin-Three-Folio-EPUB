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
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.SearchSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchResultPager
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchRow
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchStatus
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import kotlinx.coroutines.launch
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Roadmap P2.5 device evidence: the search panel finds every occurrence of a word in the
 * fixtures (the expected count is taken from the fixture's own XHTML), groups the hits by
 * chapter, opens a hit in the reader with the hits decorated and the previous / next bar, pages
 * a large book 50 hits at a time up to the 500 cap, can be cancelled, rejects short queries,
 * reports empty results, and jumps to the page of a fixed-layout hit. Screenshots and timings
 * land in `files/p2-evidence/search-*`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderSearchInstrumentationTest {

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
    fun hitsAreCountedGroupedOpenedAndSteppedThrough() {
        val expected = expectedCount(FIXTURE, "lighthouse")
        assertTrue("fixture has several hits", expected >= 2)
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            main { assertTrue(activity.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.action_search).isVisible) }
            val sheet = openSheet(activity)
            main { sheet.submit("lighthouse") }
            await("search done") { activity.searchState.value.status == SearchStatus.DONE }
            val state = activity.searchState.value
            assertEquals(expected, state.results.size)
            assertTrue(state.exhausted)
            assertFalse(state.truncated)
            state.results.forEach { assertEquals("lighthouse", it.text.highlight?.lowercase()) }
            await("count in the status line") {
                statusText(sheet) == activity.getString(R.string.text_search_result_count, expected)
            }
            val rows = onMain { sheet.rows }
            assertEquals(listOf("Chapter 1", "Chapter 3"), rows.filterIsInstance<SearchRow.Header>().map { it.title })
            assertEquals(expected, rows.count { it is SearchRow.Hit })
            SystemClock.sleep(800)
            screenshot(requireNotNull(sheet.dialog?.window), "search-panel")

            // The last hit lives in chapter 3: opening it jumps there, decorates the page and shows the bar.
            main { sheet.openResult(expected - 1) }
            awaitHref(activity, "chapter3.xhtml")
            await("sheet dismissed") { activity.searchSheet == null }
            await("bar shows the position") {
                barVisible(activity) && positionText(activity) == activity.getString(R.string.text_search_position, expected, expected)
            }
            awaitDecorations(activity, atLeast = 1)
            SystemClock.sleep(800)
            screenshot(activity.window, "search-hit")
            main {
                assertTrue(activity.findViewById<View>(R.id.search_previous).isEnabled)
                assertFalse(activity.findViewById<View>(R.id.search_next).isEnabled)
            }

            main { activity.stepSearchResult(-1) }
            awaitHref(activity, "chapter1.xhtml")
            await("bar counts down") { positionText(activity) == activity.getString(R.string.text_search_position, expected - 1, expected) }
            awaitDecorations(activity, atLeast = 1)

            main { activity.closeSearch() }
            await("bar hidden") { !barVisible(activity) }
            awaitDecorations(activity, atLeast = 0, exactly = true)
            val closed = activity.searchState.value
            assertEquals(SearchStatus.IDLE, closed.status)
            assertEquals("lighthouse", closed.query)
            assertTrue(closed.results.isEmpty())
            record(
                "search-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "fixture=$FIXTURE query=lighthouse expected=$expected found=${state.results.size}",
                    "chapters=${rows.filterIsInstance<SearchRow.Header>().map { it.title }}",
                    "positionLabel=${activity.getString(R.string.text_search_position, expected, expected)}",
                    "snippet=${state.results.first().text.before?.takeLast(20)}[${state.results.first().text.highlight}]${state.results.first().text.after?.take(20)}",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    @Test
    fun shortQueriesEmptyResultsAndReopeningAreHandled() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            var sheet = openSheet(activity)
            main { sheet.submit(" a ") }
            await("too short") { activity.searchState.value.status == SearchStatus.TOO_SHORT }
            await("hint shown") { statusText(sheet) == activity.getString(R.string.text_search_too_short) }
            assertEquals("a", activity.searchState.value.query)

            main { sheet.submit("zeppelin") }
            await("no results") {
                activity.searchState.value.let { it.status == SearchStatus.DONE && it.results.isEmpty() && it.exhausted }
            }
            await("empty status") { statusText(sheet) == activity.getString(R.string.text_search_no_results, "zeppelin") }
            assertFalse(barVisible(activity))

            main { sheet.dismiss() }
            await("sheet gone") { activity.searchSheet == null }
            sheet = openSheet(activity)
            await("reopened sheet shows the last search") {
                statusText(sheet) == activity.getString(R.string.text_search_no_results, "zeppelin") &&
                    sheet.requireView().findViewById<TextView>(R.id.query).text.toString() == "zeppelin"
            }
        } finally {
            finish(activity)
        }
    }

    @Test
    fun largeBooksPageAndCapAndCancel() {
        val activity = instrumentation.startActivitySync(request(MANY)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            val sheet = openSheet(activity)
            val started = SystemClock.uptimeMillis()
            main { sheet.submit("filler") }
            await("first page") { activity.searchState.value.status == SearchStatus.DONE }
            val firstPageMillis = SystemClock.uptimeMillis() - started
            var state = activity.searchState.value
            assertEquals(SearchResultPager.DEFAULT_BATCH_SIZE, state.results.size)
            assertFalse(state.exhausted)
            assertTrue(state.canLoadMore)
            await("open-ended count") {
                statusText(sheet) == activity.getString(R.string.text_search_result_count_more, SearchResultPager.DEFAULT_BATCH_SIZE)
            }

            var pages = 1
            while (!activity.searchState.value.exhausted) {
                val before = activity.searchState.value.results.size
                main { activity.loadMoreSearchResults() }
                await("page ${pages + 1}") {
                    activity.searchState.value.let { it.status == SearchStatus.DONE && (it.results.size > before || it.exhausted) }
                }
                pages++
                assertTrue("runaway paging", pages <= SearchResultPager.DEFAULT_LIMIT / SearchResultPager.DEFAULT_BATCH_SIZE)
            }
            val capMillis = SystemClock.uptimeMillis() - started
            state = activity.searchState.value
            assertEquals(SearchResultPager.DEFAULT_LIMIT, state.results.size)
            assertTrue(state.truncated)
            await("capped count") {
                statusText(sheet) == activity.getString(R.string.text_search_result_count_truncated, SearchResultPager.DEFAULT_LIMIT)
            }
            assertEquals(SearchResultPager.DEFAULT_LIMIT / SearchResultPager.DEFAULT_BATCH_SIZE, pages)

            // Cancelling right after submitting stops the search before its first page arrives.
            main {
                sheet.submit("filler")
                activity.cancelSearch()
            }
            await("cancelled") { activity.searchState.value.status == SearchStatus.CANCELLED }
            val cancelled = activity.searchState.value.results.size
            assertTrue("cancelled early: $cancelled", cancelled < SearchResultPager.DEFAULT_LIMIT)
            await("cancelled status") { statusText(sheet) == activity.getString(R.string.text_search_cancelled, cancelled) }
            SystemClock.sleep(1000)
            assertEquals(SearchStatus.CANCELLED, activity.searchState.value.status)
            assertEquals(cancelled, activity.searchState.value.results.size)

            main { sheet.submit("filler") }
            await("searched again") { activity.searchState.value.status == SearchStatus.DONE }
            main { sheet.openResult(0) }
            awaitHref(activity, "filler/0.xhtml")
            await("bar") { positionText(activity) == activity.getString(R.string.text_search_position, 1, SearchResultPager.DEFAULT_BATCH_SIZE) }
            record(
                "search-paging-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "fixture=$MANY query=filler firstPageMillis=$firstPageMillis capMillis=$capMillis pages=$pages",
                    "capped=${state.results.size} truncated=${state.truncated} cancelledAt=$cancelled",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    @Test
    fun fixedLayoutHitsJumpToTheirPage() {
        val expected = expectedCount(FIXED, "reef")
        assertTrue(expected >= 1)
        val activity = instrumentation.startActivitySync(request(FIXED)) as EpubReaderActivity
        try {
            awaitHref(activity, "page1.xhtml")
            val sheet = openSheet(activity)
            main { sheet.submit("reef") }
            await("done") { activity.searchState.value.status == SearchStatus.DONE }
            assertEquals(expected, activity.searchState.value.results.size)
            main { sheet.openResult(0) }
            awaitHref(activity, "page2.xhtml")
            await("bar") { barVisible(activity) && positionText(activity) == activity.getString(R.string.text_search_position, 1, expected) }
            main { activity.closeSearch() }
            await("bar hidden") { !barVisible(activity) }
        } finally {
            finish(activity)
        }
    }

    // ---- helpers ----

    private fun openSheet(activity: EpubReaderActivity): SearchSheet {
        main { activity.showSearch() }
        await("search sheet") { activity.searchSheet?.view != null }
        return requireNotNull(activity.searchSheet)
    }

    private fun statusText(sheet: SearchSheet): String? =
        onMain { sheet.view?.findViewById<TextView>(R.id.search_status)?.text?.toString() }

    private fun barVisible(activity: EpubReaderActivity): Boolean = onMain { activity.findViewById<View>(R.id.search_bar).isShown }

    private fun positionText(activity: EpubReaderActivity): String =
        onMain { activity.findViewById<TextView>(R.id.search_position).text.toString() }

    /** Occurrences of [word] in the reading-order XHTML of the fixture, case-insensitively, tags stripped. */
    private fun expectedCount(fixture: String, word: String): Int {
        val regex = Regex(Regex.escape(word), RegexOption.IGNORE_CASE)
        var count = 0
        ZipInputStream(assets.open(fixture)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".xhtml") && !entry.name.endsWith("nav.xhtml")) {
                    val html = zip.readBytes().toString(Charsets.UTF_8)
                    val body = html.substringAfter("<body", "").substringAfter('>').substringBefore("</body>")
                    count += regex.findAll(body.replace(Regex("<[^>]+>"), " ")).count()
                }
                entry = zip.nextEntry
            }
        }
        return count
    }

    /** Decorated hits on the current page: the group container holds one element per decoration. */
    private fun decorationCount(activity: EpubReaderActivity): Int? =
        evaluate(activity, "document.querySelectorAll('[data-group=\"${EpubReaderActivity.SEARCH_DECORATIONS}\"] > div').length")?.toIntOrNull()

    private fun awaitDecorations(activity: EpubReaderActivity, atLeast: Int, exactly: Boolean = false) {
        val deadline = SystemClock.uptimeMillis() + 20000
        var latest: Int? = null
        while (SystemClock.uptimeMillis() < deadline) {
            latest = decorationCount(activity)
            if (latest != null && (if (exactly) latest == atLeast else latest >= atLeast)) return
            SystemClock.sleep(200)
        }
        fail("Timed out: decorations >= $atLeast (exactly=$exactly), last $latest")
    }

    /** Runs [script] in the current reflowable page from the instrumentation thread (the call suspends on main). */
    private fun evaluate(activity: EpubReaderActivity, script: String): String? {
        check(Looper.myLooper() != Looper.getMainLooper())
        val latch = CountDownLatch(1)
        var result: String? = null
        instrumentation.runOnMainSync {
            activity.lifecycleScope.launch {
                result = runCatching { navigator(activity).evaluateJavascript(script) }.getOrNull()
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        return result
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

    private fun finish(activity: EpubReaderActivity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
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

    private fun navigator(activity: EpubReaderActivity): EpubNavigatorFragment =
        requireNotNull(activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)

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
        const val MANY = "malformed-many-entries.epub"
        const val FIXED = "fixed-layout.epub"
    }
}
