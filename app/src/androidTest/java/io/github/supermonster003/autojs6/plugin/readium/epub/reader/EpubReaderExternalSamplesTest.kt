package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchStatus
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Real-book measurements for the roadmap's evidence (never part of the gate): the test runs only
 * with the runner argument `external=true`, uses every sample whose file sits in the test document
 * directory (pushed with `adb` before the run), and is skipped otherwise; `samples=a,b` narrows
 * the run to the named samples. Nothing of the books is recorded, only open,
 * positions and search timings plus counts, in `files/p2-evidence/external-<name>-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderExternalSamplesTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)

    private data class Sample(val file: String, val query: String, val name: String)

    private val samples = listOf(
        Sample("mega-5000.epub", "chap", "mega-5000"),
        Sample("mega-1000.epub", "chap", "mega-1000"),
        Sample("sherlock-zh.epub", "福尔摩斯", "sherlock-zh"),
        Sample("communication-zh.epub", "沟通", "communication-zh"),
        Sample("javascript-zh.epub", "函数", "javascript-zh"),
        Sample("gutenberg.epub", "the", "gutenberg"),
        Sample("howtolive.epub", "life", "howtolive"),
        Sample("galahit-fxl.epub", "the", "galahit-fxl"),
    )

    @After
    fun cleanUp() {
        booksDirectory.deleteRecursively()
    }

    @Test
    fun searchesTheExternalSamplesWhenPresent() {
        assumeTrue(
            "pass -e external true to measure the external samples",
            InstrumentationRegistry.getArguments().getString("external") == "true",
        )
        val wanted = InstrumentationRegistry.getArguments().getString("samples")?.split(',')?.map { it.trim() }
        val present = samples
            .filter { wanted == null || it.name in wanted }
            .filter { EpubReaderTestContentProvider.fileFor(context, it.file).isFile }
        assumeTrue("no external samples pushed to the test document directory", present.isNotEmpty())
        for (sample in present) measure(sample)
    }

    private fun measure(sample: Sample) {
        val bytes = EpubReaderTestContentProvider.fileFor(context, sample.file).length()
        val started = SystemClock.uptimeMillis()
        val activity = instrumentation.startActivitySync(request(sample.file)) as EpubReaderActivity
        try {
            val opened = awaitOrNot(OPEN_TIMEOUT) {
                activity.readerModel.state.value is OpenState.Failed || (activity.navigatorReady && currentHref(activity) != null)
            }
            val openMillis = SystemClock.uptimeMillis() - started
            val failure = (activity.readerModel.state.value as? OpenState.Failed)?.failure
            if (!opened || failure != null) {
                record(
                    "external-${sample.name}-api${Build.VERSION.SDK_INT}.txt",
                    listOf(
                        "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                        "sample=${sample.name} bytes=$bytes",
                        "openFailed=true afterMillis=$openMillis state=${activity.readerModel.state.value} failure=$failure navigatorReady=${activity.navigatorReady} href=${currentHref(activity)}",
                    ),
                )
                fail("${sample.name}: not opened after $openMillis ms, state=${activity.readerModel.state.value}, failure=$failure")
            }
            await("${sample.name}: positions", OPEN_TIMEOUT) { activity.readerModel.positionCount.value > 0 }
            val positionsMillis = SystemClock.uptimeMillis() - started
            val positions = activity.readerModel.positionCount.value
            val readingOrder = requireNotNull(activity.readerModel.publication).readingOrder.size

            val searchStarted = SystemClock.uptimeMillis()
            main { activity.submitSearch(sample.query) }
            await("${sample.name}: first page", SEARCH_TIMEOUT) { activity.searchState.value.status != SearchStatus.SEARCHING }
            val firstPageMillis = SystemClock.uptimeMillis() - searchStarted
            val firstPage = activity.searchState.value.results.size
            var pages = 1
            while (activity.searchState.value.canLoadMore) {
                val before = activity.searchState.value.results.size
                main { activity.loadMoreSearchResults() }
                await("${sample.name}: page ${pages + 1}", SEARCH_TIMEOUT) {
                    activity.searchState.value.let { it.status != SearchStatus.SEARCHING && (it.results.size > before || it.exhausted) }
                }
                pages++
            }
            val allMillis = SystemClock.uptimeMillis() - searchStarted
            val state = activity.searchState.value

            var jumpMillis = -1L
            var decorationMillis = -1L
            var hitsInTarget = 0
            var jumpedTo: String? = null
            if (state.results.isNotEmpty()) {
                val target = state.results.last()
                hitsInTarget = state.results.count { it.href == target.href }
                val publication = requireNotNull(activity.readerModel.publication)
                val fixed = activity.fixedLayout
                val hrefs = publication.readingOrder.map { it.url().toString() }
                val targetIndex = hrefs.indexOf(target.href.toString())
                val jumpStarted = SystemClock.uptimeMillis()
                main { activity.openSearchResult(state.results.lastIndex) }
                // A fixed-layout spread reports its left page, so the neighbour of the target counts there.
                await("${sample.name}: jump", SEARCH_TIMEOUT) {
                    val current = hrefs.indexOf(currentHref(activity))
                    current == targetIndex || (fixed && current >= 0 && kotlin.math.abs(current - targetIndex) == 1)
                }
                jumpMillis = SystemClock.uptimeMillis() - jumpStarted
                jumpedTo = currentHref(activity)
                if (!fixed) {
                    val deadline = SystemClock.uptimeMillis() + SEARCH_TIMEOUT
                    while (SystemClock.uptimeMillis() < deadline && decorationMillis < 0) {
                        if ((decorationCount(activity) ?: 0) >= 1) decorationMillis = SystemClock.uptimeMillis() - jumpStarted
                        else SystemClock.sleep(200)
                    }
                }
            }
            assertTrue(state.status == SearchStatus.DONE)
            record(
                "external-${sample.name}-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "sample=${sample.name} bytes=$bytes readingOrder=$readingOrder layout=${activity.readerModel.publication?.metadata?.layout}",
                    "openMillis=$openMillis positions=$positions positionsMillis=$positionsMillis",
                    "query=${sample.query} firstPage=$firstPage firstPageMillis=$firstPageMillis",
                    "loaded=${state.results.size} pages=$pages truncated=${state.truncated} allMillis=$allMillis",
                    "jumpMillis=$jumpMillis decorationMillis=$decorationMillis hitsInTarget=$hitsInTarget jumpedTo=${jumpedTo?.substringAfterLast('/')}",
                ),
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(1000)
        }
    }

    private fun record(name: String, lines: List<String>) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name).writeText(lines.joinToString("\n") + "\n")
    }

    private fun request(fixture: String): Intent {
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

    /** Decorated hits on the current page; null when the page is not reflowable or not ready. */
    private fun decorationCount(activity: EpubReaderActivity): Int? {
        val latch = CountDownLatch(1)
        var result: String? = null
        instrumentation.runOnMainSync {
            activity.lifecycleScope.launch {
                result = runCatching {
                    (activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)
                        ?.evaluateJavascript("document.querySelectorAll('[data-group=\"${EpubReaderActivity.SEARCH_DECORATIONS}\"] > div').length")
                }.getOrNull()
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        return result?.toIntOrNull()
    }

    private fun currentHref(activity: EpubReaderActivity): String? = onMain {
        (activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)
            ?.takeIf { it.view != null }
            ?.currentLocator?.value?.href?.toString()
    }

    private fun awaitOrNot(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = runCatching(condition).getOrDefault(false) }
            if (ready) return true
            SystemClock.sleep(200)
        }
        return false
    }

    private fun await(message: String, timeoutMillis: Long, condition: () -> Boolean) {
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

    private companion object {
        const val OPEN_TIMEOUT = 180_000L
        const val SEARCH_TIMEOUT = 300_000L
    }
}
