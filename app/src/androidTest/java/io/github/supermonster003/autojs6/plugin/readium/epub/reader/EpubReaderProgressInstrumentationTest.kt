package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File

/**
 * Roadmap P1.2 / P1.3 device evidence: the reader chrome (progress bar, immersive mode, scroll
 * mode, volume keys) works on a real navigator, the reading position is stored under the book's
 * content fingerprint and restored on the next open, and "start from the beginning" clears it.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderProgressInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)

    @Before
    fun resetState() {
        booksDirectory.deleteRecursively()
        ReaderPreferencesStore.forFilesDirectory(context.filesDir).clear()
        context.getSharedPreferences(ReaderSettings.PREFERENCES_NAME, 0).edit().clear().commit()
    }

    @After
    fun cleanUp() {
        booksDirectory.deleteRecursively()
        ReaderPreferencesStore.forFilesDirectory(context.filesDir).clear()
        context.getSharedPreferences(ReaderSettings.PREFERENCES_NAME, 0).edit().clear().commit()
    }

    @Test
    fun thePositionIsStoredUnderTheFullFingerprintAndRestoredOnReopen() {
        val request = request("minimal-epub3.epub")
        val expectedKey = assets.open("minimal-epub3.epub").use { input ->
            java.security.MessageDigest.getInstance("SHA-256").digest(input.readBytes()).joinToString("") { "%02x".format(it) }
        }

        var activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            await("full fingerprint") { activity.readerModel.bookKey == expectedKey }
            main {
                val chapter3 = requireNotNull(activity.readerModel.publication).readingOrder[2]
                navigator(activity).go(chapter3, animated = false)
            }
            await("chapter 3") { currentHref(activity)?.endsWith("chapter3.xhtml") == true }
            SystemClock.sleep(300)
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
        await("progress written to disk") { storedHref(expectedKey)?.endsWith("chapter3.xhtml") == true }
        assertEquals(listOf(expectedKey), BookDataStore(booksDirectory).bookKeys())

        activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("restored to chapter 3") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter3.xhtml") == true }
            await("progress panel") { activity.findViewById<View>(R.id.progress_panel).isShown }
            main {
                val text = activity.findViewById<android.widget.TextView>(R.id.progress_text).text.toString()
                assertTrue(text, text.contains('%'))
                assertEquals("Minimal EPUB 3", activity.supportActionBar?.title?.toString())
            }

            main { activity.restartFromBeginning() }
            await("back to chapter 1") { currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            SystemClock.sleep(300)
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
        await("restart persisted") { storedHref(expectedKey)?.endsWith("chapter1.xhtml") == true }
    }

    @Test
    fun aJumpRequestedBeforeTheFirstPageLoadsIsReplayedOnceTheNavigatorIsReady() {
        val activity = instrumentation.startActivitySync(request("minimal-epub3.epub")) as EpubReaderActivity
        try {
            await("publication") { activity.readerModel.publication != null && !activity.navigatorReady }
            main { activity.jumpTo(requireNotNull(activity.readerModel.publication).readingOrder[2]) }
            await("navigator ready") { activity.navigatorReady }
            await("jump replayed to chapter 3") { currentHref(activity)?.endsWith("chapter3.xhtml") == true }
            await("chapter 3 located") { navigator(activity).currentLocator.value.locations.position == 3 }
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun aSavedLocatorInTheInstanceStateWinsOverTheStoredProgress() {
        val request = request("minimal-epub3.epub")
        var activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            main {
                val chapter2 = requireNotNull(activity.readerModel.publication).readingOrder[1]
                navigator(activity).go(chapter2, animated = false)
            }
            await("chapter 2") { currentHref(activity)?.endsWith("chapter2.xhtml") == true }
            main { activity.toggleImmersive() }
            main { assertTrue(activity.isImmersive) }
            await("toolbar hidden") { !activity.findViewById<View>(R.id.toolbar).isShown }

            main { activity.recreate() }
            await("recreated Activity") { resumedReader()?.let { it !== activity } == true }
            activity = requireNotNull(resumedReader())
            await("locator kept after recreate") { currentHref(activity)?.endsWith("chapter2.xhtml") == true }
            main { assertTrue(activity.isImmersive) }
            main { activity.toggleImmersive() }
            await("toolbar shown") { activity.findViewById<View>(R.id.toolbar).isShown }
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun volumeKeysAndScrollModeDriveTheNavigator() {
        val request = request("minimal-epub3.epub")
        val activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            SystemClock.sleep(500)
            assumeReaderHasFocus(activity)
            var presses = 0
            while (currentHref(activity)?.endsWith("chapter1.xhtml") == true && presses < 6) {
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_DOWN)
                presses++
                SystemClock.sleep(400)
            }
            await("volume down turned the page") { currentHref(activity)?.endsWith("chapter2.xhtml") == true }
            presses = 0
            while (currentHref(activity)?.endsWith("chapter2.xhtml") == true && presses < 6) {
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP)
                presses++
                SystemClock.sleep(400)
            }
            await("volume up turned back") { currentHref(activity)?.endsWith("chapter1.xhtml") == true }

            main { assertFalse(navigator(activity).settings.value.scroll) }
            main { activity.setScrollMode(true) }
            await("scroll mode applied") { navigator(activity).settings.value.scroll }
            main { assertTrue(activity.readerModel.preferences.value.epub.scroll == true) }
            main { activity.setScrollMode(false) }
            await("paginated again") { !navigator(activity).settings.value.scroll }

            record(
                "reader-chrome-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "positionCount=${activity.readerModel.positionCount.value}",
                    "fullFingerprintMillis=${activity.readerModel.fullFingerprintMillis}",
                    "fullFingerprintBytes=${activity.readerModel.fullFingerprintBytes}",
                ),
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun aBookWithoutAFingerprintableDescriptorStillOpens() {
        // Sanity: the not-a-zip fixture must not leave any book directory behind.
        val request = request("malformed-not-a-zip.epub")
        val activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("error shown") {
                val text = activity.findViewById<android.widget.TextView>(R.id.status_text)
                text.isShown && text.text.toString() != activity.getString(R.string.text_opening_book)
            }
            assertNull(activity.readerModel.publication)
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
        assertTrue(BookDataStore(booksDirectory).bookKeys().isEmpty())
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

    private fun resumedReader(): EpubReaderActivity? = onMain {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            .filterIsInstance<EpubReaderActivity>()
            .firstOrNull()
    }

    private fun record(name: String, lines: List<String>) {
        File(context.filesDir, "p1-evidence").apply { mkdirs() }.resolve(name).writeText(lines.joinToString("\n") + "\n")
    }

    /**
     * Injected keys go to the focused window. When another window holds it (a focusable overlay, the shell of a
     * CI emulator; roadmap P7 finding 1) the key paths cannot be exercised, so the check is skipped rather than failed.
     */
    private fun assumeReaderHasFocus(activity: EpubReaderActivity) {
        assumeTrue("the reader window does not hold input focus, injected keys would go elsewhere", readerHasFocus(activity))
    }

    private fun readerHasFocus(activity: EpubReaderActivity): Boolean {
        val deadline = SystemClock.uptimeMillis() + 5_000
        var focused = false
        while (!focused && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync { focused = activity.hasWindowFocus() }
            if (!focused) SystemClock.sleep(200)
        }
        return focused
    }

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
