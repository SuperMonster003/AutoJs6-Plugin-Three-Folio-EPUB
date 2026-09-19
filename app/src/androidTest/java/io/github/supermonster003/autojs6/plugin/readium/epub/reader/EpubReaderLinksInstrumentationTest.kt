package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P2.7 device evidence for links: an in-book link followed from the page jumps within
 * the reader and the back key returns to where it was followed from (a second back press then
 * leaves the reader), a `noteref` link opens the note in a dialog instead of jumping, and
 * links that leave the book follow decision D25 (web links ask first unless set to open
 * directly, mail and phone links go to the system, other schemes are refused).
 * Notes land in `files/p2-evidence/links-api<N>.txt`.
 */
@OptIn(ExperimentalReadiumApi::class)
@RunWith(AndroidJUnit4::class)
class EpubReaderLinksInstrumentationTest {

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
    fun inBookLinksKeepABackStack() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            assertEquals(0, activity.readerModel.linkHistory.size)

            val clickedAt = SystemClock.uptimeMillis()
            evaluate(activity, "document.querySelector('a[href^=\"chapter3\"]').click(); 'clicked'")
            await("link jumped to chapter 3") { currentHref(activity)?.endsWith("chapter3.xhtml") == true }
            val jumpMillis = SystemClock.uptimeMillis() - clickedAt
            assertEquals(1, onMain { activity.readerModel.linkHistory.size })

            val backAt = SystemClock.uptimeMillis()
            main { activity.onBackPressedDispatcher.onBackPressed() }
            await("back returned to chapter 1") { currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val backMillis = SystemClock.uptimeMillis() - backAt
            assertEquals(0, onMain { activity.readerModel.linkHistory.size })
            assertFalse(onMain { activity.isFinishing })

            // With the history empty the back key leaves the reader as before.
            main { activity.onBackPressedDispatcher.onBackPressed() }
            await("second back finishes") { activity.isFinishing }
            record(
                "links-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "inBookLink jump=${jumpMillis}ms back=${backMillis}ms secondBackFinishes=true",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    @Test
    fun noteLinksOpenADialog() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            val clickedAt = SystemClock.uptimeMillis()
            evaluate(activity, "document.getElementById('ref-1').click(); 'clicked'")
            await("note dialog") { activity.noteText != null }
            val noteMillis = SystemClock.uptimeMillis() - clickedAt
            val note = onMain { activity.noteText.toString() }
            assertTrue(note, note.contains("nobody in particular"))
            SystemClock.sleep(500)
            assertTrue(currentHref(activity)?.endsWith("chapter1.xhtml") == true)
            assertEquals(0, onMain { activity.readerModel.linkHistory.size })
            main { activity.dismissLinkDialogs() }
            assertNull(onMain { activity.noteText })
            record(
                "links-note-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "noteref dialog=${noteMillis}ms text=${note.take(60)}",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    @Test
    fun externalLinksFollowThePolicy() {
        val filter = IntentFilter(Intent.ACTION_VIEW).apply {
            addDataScheme("http")
            addDataScheme("https")
            addDataScheme("mailto")
            addDataScheme("tel")
        }
        val monitor = instrumentation.addMonitor(filter, Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            val web = requireNotNull(AbsoluteUrl("https://example.com/page"))
            assertFalse(onMain { activity.externalLinksDirect })

            // Default: ask first; cancelling opens nothing, confirming opens the browser.
            main { activity.onExternalLinkActivated(web) }
            await("confirmation dialog") { activity.externalLinkDialog?.isShowing == true }
            assertEquals(0, monitor.hits)
            main { activity.dismissLinkDialogs() }
            main { activity.onExternalLinkActivated(web) }
            await("confirmation dialog again") { activity.externalLinkDialog?.isShowing == true }
            main { requireNotNull(activity.externalLinkDialog).getButton(AlertDialog.BUTTON_POSITIVE).performClick() }
            await("browser intent after confirming") { monitor.hits == 1 }

            // Direct: no dialog.
            main { activity.setExternalLinksDirect(true) }
            assertTrue(ReaderSettings(context).externalLinksDirect)
            main { activity.onExternalLinkActivated(web) }
            await("browser intent without asking") { monitor.hits == 2 }
            assertNull(onMain { activity.externalLinkDialog })

            // Mail and phone links go to the system without asking, in either mode. Readium's
            // AbsoluteUrl only holds hierarchical URLs, so these reach the policy as plain strings.
            main { activity.setExternalLinksDirect(false) }
            assertNull(AbsoluteUrl("mailto:someone@example.com"))
            main { activity.openExternalLink("mailto:someone@example.com") }
            await("mail intent") { monitor.hits == 3 }
            main { activity.openExternalLink("tel:+1234567890") }
            await("phone intent") { monitor.hits == 4 }
            assertNull(onMain { activity.externalLinkDialog })

            // Other schemes are refused: nothing launches and nothing asks.
            for (refused in listOf("intent://scan/#Intent;scheme=zxing;end", "file:///sdcard/x")) {
                main { activity.onExternalLinkActivated(requireNotNull(AbsoluteUrl(refused))) }
            }
            main { activity.openExternalLink("javascript:alert(1)") }
            SystemClock.sleep(800)
            assertEquals(4, monitor.hits)
            assertNull(onMain { activity.externalLinkDialog })
            record(
                "links-external-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "external confirm=dialog-then-intent direct=intent mailto=intent tel=intent refused=javascript/intent/file (hits=${monitor.hits})",
                ),
            )
        } finally {
            instrumentation.removeMonitor(monitor)
            finish(activity)
        }
    }

    // ---- helpers ----

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

    private fun record(name: String, lines: List<String>) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name).writeText(lines.joinToString("\n") + "\n")
    }

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
    }
}
