package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.PopupMenu
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File

/**
 * Roadmap P0.2 device evidence: the reader Activity accepts a real Explorer Action v2 envelope,
 * opens the fixture through the granted descriptor, turns pages, jumps through the table of
 * contents, survives recreation with its locator, and fails gracefully on a malformed file.
 * Screenshots and timings are written to `files/p0-spike/` for `docs/dev/p0-readium-spike.md`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderUiInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets

    @Test
    fun opensTheFixtureTurnsPagesJumpsAndRestoresTheLocator() {
        val request = request("minimal-epub3.epub")
        assertNotNull(EpubReaderIntentPolicy.resolve(request))
        EpubReaderTestContentProvider.openCount.set(0)
        val started = SystemClock.uptimeMillis()
        var activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("reader did not show chapter 1") { currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val openMillis = SystemClock.uptimeMillis() - started
            await("toolbar title") { activity.supportActionBar?.title == "minimal-epub3.epub" }
            assertEquals(1, EpubReaderTestContentProvider.openCount.get())
            SystemClock.sleep(1500) // let the WebView paint before the first screenshot
            screenshot("reader-chapter1")

            // Page turn: the fixture chapters fit one page, so moving forward reaches chapter 2.
            var turns = 0
            while (currentHref(activity)?.endsWith("chapter1.xhtml") == true && turns < 6) {
                main { navigator(activity).goForward(animated = false) }
                turns++
                SystemClock.sleep(400)
            }
            await("goForward never left chapter 1") { currentHref(activity)?.endsWith("chapter2.xhtml") == true }

            // Table of contents: the dialog lists 4 entries (chapter 1, its child, chapter 2, chapter 3).
            main {
                val item = PopupMenu(activity, View(activity)).menu.add(0, R.id.action_table_of_contents, 0, "")
                assertTrue(activity.onOptionsItemSelected(item))
            }
            await("table of contents dialog") { activity.tableOfContentsDialog?.listView?.adapter != null }
            main {
                val list = requireNotNull(activity.tableOfContentsDialog?.listView)
                assertEquals(4, list.adapter.count)
                list.performItemClick(list.getChildAt(3), 3, list.adapter.getItemId(3))
            }
            await("TOC jump to chapter 3") { currentHref(activity)?.endsWith("chapter3.xhtml") == true }
            await("dialog dismissed") { activity.tableOfContentsDialog == null }
            SystemClock.sleep(800)
            screenshot("reader-chapter3")

            // Recreation keeps the publication in the ViewModel and restores the last locator.
            main { activity.recreate() }
            await("recreated Activity") { resumedReader()?.let { it !== activity } == true }
            activity = requireNotNull(resumedReader())
            await("locator restored after recreate") { currentHref(activity)?.endsWith("chapter3.xhtml") == true }
            assertEquals(1, EpubReaderTestContentProvider.openCount.get())
            assertFalse(activity.isFinishing)

            record(
                "reader-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "fixture=minimal-epub3.epub",
                    "openToFirstLocatorMillis=$openMillis",
                    "descriptorOpens=${EpubReaderTestContentProvider.openCount.get()}",
                    "pageTurnsToChapter2=$turns",
                ),
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun aMalformedFileShowsAnErrorInsteadOfCrashing() {
        val request = request("malformed-not-a-zip.epub")
        val activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("error panel") {
                val text = activity.findViewById<android.widget.TextView>(R.id.status_text)
                text.isShown && text.text.isNotBlank() && text.text.toString() != activity.getString(R.string.text_opening_book)
            }
            main {
                val text = activity.findViewById<android.widget.TextView>(R.id.status_text).text.toString()
                assertNotEquals(activity.getString(R.string.text_invalid_request), text)
                assertTrue(text, text.startsWith(activity.getString(R.string.text_open_failed, "").trimEnd()))
                assertFalse(activity.findViewById<View>(R.id.status_progress).isShown)
            }
            assertFalse(activity.isFinishing)
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
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
        requireNotNull(activity.supportFragmentManager.findFragmentByTag("readium-epub-navigator") as? EpubNavigatorFragment)

    private fun currentHref(activity: EpubReaderActivity): String? = onMain {
        (activity.supportFragmentManager.findFragmentByTag("readium-epub-navigator") as? EpubNavigatorFragment)
            ?.takeIf { it.view != null }
            ?.currentLocator?.value?.href?.toString()
    }

    private fun resumedReader(): EpubReaderActivity? = onMain {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            .filterIsInstance<EpubReaderActivity>()
            .firstOrNull()
    }

    private fun screenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        outputFile("$name-api${Build.VERSION.SDK_INT}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun record(name: String, lines: List<String>) {
        outputFile(name).writeText(lines.joinToString("\n") + "\n")
    }

    private fun outputFile(name: String): File =
        File(context.filesDir, "p0-spike").apply { mkdirs() }.resolve(name)

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 20000
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = runCatching(condition).getOrDefault(false) }
            if (ready) return
            SystemClock.sleep(100)
        }
        fail("Timed out: $message")
    }

    private fun main(action: () -> Unit) = onMain(action)

    /** Runs [block] on the main thread; safe to nest because it never re-enters runOnMainSync from main. */
    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }
}
