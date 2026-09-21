package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.app.SearchManager
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.SelectionActions
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.TapZones
import kotlinx.coroutines.launch
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P2.7 device evidence for the reading controls: the tap zones follow the setting (off
 * only toggles the chrome, top / bottom and left / right turn pages), a hardware keyboard's
 * arrow, page and space keys turn pages, and the text-selection actions use the selected text
 * (copy lands in the clipboard; share, web search and text processing build their intents).
 * Notes land in `files/p2-evidence/controls-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderControlsInstrumentationTest {

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
    fun tapZonesFollowTheSetting() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            val view = navigator(activity).publicationView
            assertEquals(TapZones.HORIZONTAL, activity.tapZones)

            // Off: a tap on the right edge changes nothing but the chrome. The taps stay in the
            // lower part of the page, where the short chapter has no text (and no links).
            main { activity.setTapZones(TapZones.OFF) }
            assertFalse(onMain { activity.isImmersive })
            var offTaps = 0
            while (!onMain { activity.isImmersive } && offTaps < 4) {
                tap(activity, view, 0.85f, 0.9f)
                offTaps++
                SystemClock.sleep(1000)
            }
            await("chrome hidden") { activity.isImmersive }
            SystemClock.sleep(800)
            assertTrue(currentHref(activity)?.endsWith("chapter1.xhtml") == true)
            tap(activity, view, 0.85f, 0.9f)
            await("chrome shown again") { !activity.isImmersive }

            // Top / bottom.
            main { activity.setTapZones(TapZones.VERTICAL) }
            var taps = 0
            while (currentHref(activity)?.endsWith("chapter1.xhtml") == true && taps < 6) {
                tap(activity, view, 0.5f, 0.9f)
                taps++
                SystemClock.sleep(600)
            }
            await("bottom tap turned the page") { currentHref(activity)?.endsWith("chapter2.xhtml") == true }
            val bottomTaps = taps
            taps = 0
            while (currentHref(activity)?.endsWith("chapter2.xhtml") == true && taps < 6) {
                tap(activity, view, 0.5f, 0.06f)
                taps++
                SystemClock.sleep(600)
            }
            await("top tap turned back") { currentHref(activity)?.endsWith("chapter1.xhtml") == true }

            // Left / right again, through the menu path.
            main { activity.setTapZones(TapZones.HORIZONTAL) }
            assertEquals(TapZones.HORIZONTAL, ReaderSettings(context).tapZones)
            taps = 0
            while (currentHref(activity)?.endsWith("chapter1.xhtml") == true && taps < 6) {
                tap(activity, view, 0.88f, 0.9f)
                taps++
                SystemClock.sleep(600)
            }
            await("right tap turned the page") { currentHref(activity)?.endsWith("chapter2.xhtml") == true }
            record(
                "controls-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "tapZones off=chrome-only($offTaps taps) vertical=bottom-next($bottomTaps taps)/top-previous horizontal=right-next($taps taps)",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    @Test
    fun keyboardKeysTurnPages() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            val lines = ArrayList<String>()
            lines += "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})"
            lines += "focusBefore=${onMain { navigator(activity).view?.hasFocus() }}"
            lines += "ArrowRight presses=${pressUntil(activity, KeyEvent.KEYCODE_DPAD_RIGHT, "chapter2.xhtml")}"
            lines += "ArrowLeft presses=${pressUntil(activity, KeyEvent.KEYCODE_DPAD_LEFT, "chapter1.xhtml")}"
            lines += "Space presses=${pressUntil(activity, KeyEvent.KEYCODE_SPACE, "chapter2.xhtml")}"
            lines += "PageUp presses=${pressUntil(activity, KeyEvent.KEYCODE_PAGE_UP, "chapter1.xhtml")}"
            lines += "PageDown presses=${pressUntil(activity, KeyEvent.KEYCODE_PAGE_DOWN, "chapter2.xhtml")}"

            // With the page focused the keys would otherwise be swallowed by the web view.
            main { navigator(activity).view?.requestFocus() }
            lines += "focusAfterRequest=${onMain { navigator(activity).view?.hasFocus() }}"
            lines += "ArrowLeft(focused) presses=${pressUntil(activity, KeyEvent.KEYCODE_DPAD_LEFT, "chapter1.xhtml")}"
            lines += "ArrowRight(focused) presses=${pressUntil(activity, KeyEvent.KEYCODE_DPAD_RIGHT, "chapter2.xhtml")}"
            record("controls-keys-api${Build.VERSION.SDK_INT}.txt", lines)
        } finally {
            finish(activity)
        }
    }

    @Test
    fun selectionActionsUseTheSelectedText() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            evaluate(
                activity,
                "(function(){var p=document.querySelector('p');var r=document.createRange();r.selectNodeContents(p);" +
                    "var s=window.getSelection();s.removeAllRanges();s.addRange(r);return s.toString();})()",
            )
            main { activity.rememberSelection() }
            await("selection remembered") { activity.rememberedSelection?.contains("lighthouse") == true }
            val selected = requireNotNull(activity.rememberedSelection)

            main { assertTrue(activity.performSelectionAction(R.id.selection_copy)) }
            SystemClock.sleep(500)
            val clip = onMain { context.getSystemService<ClipboardManager>()?.primaryClip }
            val copied = clip?.getItemAt(0)?.text?.toString()
            // Some vendors gate clipboard reads behind a prompt; the copy itself is then unverifiable here.
            if (copied != null) assertEquals(selected, copied)

            assertEquals(selected, SelectionActions.webSearchIntent(selected).getStringExtra(SearchManager.QUERY))
            val share = SelectionActions.shareIntent(selected)
            assertEquals(Intent.ACTION_CHOOSER, share.action)
            val targets = SelectionActions.processTextTargets(context.packageManager)
            assertTrue(targets.size <= SelectionActions.MAX_PROCESSORS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && targets.isNotEmpty()) {
                val intent = SelectionActions.processTextIntent(targets.first(), selected)
                assertEquals(Intent.ACTION_PROCESS_TEXT, intent.action)
                assertEquals(selected, intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT))
                assertTrue(intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false))
            }
            main { assertFalse(activity.performSelectionAction(R.id.action_search)) }
            assertNotNull(navigator(activity))
            record(
                "controls-selection-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "selected=${selected.take(60)}",
                    "clipboardReadable=${copied != null} processors=${targets.map { it.label }}",
                ),
            )
        } finally {
            main { context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText("", "")) }
            finish(activity)
        }
    }

    // ---- helpers ----

    /** Presses [keyCode] until the current resource ends with [suffix] (at most 6 times); returns the count. */
    private fun pressUntil(activity: EpubReaderActivity, keyCode: Int, suffix: String): Int {
        assumeReaderHasFocus(activity)
        var presses = 0
        while (currentHref(activity)?.endsWith(suffix) != true && presses < 6) {
            instrumentation.sendKeyDownUpSync(keyCode)
            presses++
            SystemClock.sleep(500)
            trace += "key=$keyCode press=$presses focus=${onMain { activity.currentFocus?.javaClass?.simpleName }} " +
                "navigatorFocus=${onMain { navigator(activity).view?.hasFocus() }} href=${currentHref(activity)} " +
                "progression=${onMain { navigator(activity).currentLocator.value.locations.progression }}"
        }
        if (currentHref(activity)?.endsWith(suffix) != true) record("controls-keys-trace-api${Build.VERSION.SDK_INT}.txt", trace)
        await("key $keyCode reached $suffix") { currentHref(activity)?.endsWith(suffix) == true }
        return presses
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

    private val trace = ArrayList<String>()

    /** A tap at the given fractions of [view], dispatched through the Activity like a real touch. */
    private fun tap(activity: EpubReaderActivity, view: View, fx: Float, fy: Float) {
        check(Looper.myLooper() != Looper.getMainLooper())
        val (x, y) = onMain {
            val location = IntArray(2).also { view.getLocationInWindow(it) }
            (location[0] + view.width * fx) to (location[1] + view.height * fy)
        }
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER })
        val coords = arrayOf(MotionEvent.PointerCoords().apply { this.x = x; this.y = y; pressure = 1f; size = 1f })
        val downTime = SystemClock.uptimeMillis()
        dispatch(activity, downTime, MotionEvent.ACTION_DOWN, properties, coords)
        SystemClock.sleep(60)
        dispatch(activity, downTime, MotionEvent.ACTION_UP, properties, coords)
        instrumentation.waitForIdleSync()
    }

    private fun dispatch(
        activity: EpubReaderActivity,
        downTime: Long,
        action: Int,
        properties: Array<MotionEvent.PointerProperties>,
        coords: Array<MotionEvent.PointerCoords>,
    ) {
        val event = MotionEvent.obtain(
            downTime, SystemClock.uptimeMillis(), action, 1, properties, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        try {
            main { activity.dispatchTouchEvent(event) }
        } finally {
            event.recycle()
        }
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
