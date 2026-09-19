package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.BundleCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.android.material.button.MaterialButtonToggleGroup
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.TocSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
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
import org.readium.r2.navigator.preferences.Spread
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Roadmap P2.4 device evidence with the six-plate fixed-layout sample: pages are counted as
 * `Page x of N`, portrait turns one page at a time, landscape shows spreads automatically (the
 * plugin resolves the automatic spread, Readium 3.4.0 has none), the spread preference overrides
 * the orientation both ways, the panel hides the text preferences and offers the spread, the
 * scroll mode menu entry disappears, and Readium's own pinch zoom and panning work on the page.
 * Screenshots land in `files/p2-evidence/` for the archive.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderFixedLayoutInstrumentationTest {

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
    fun portraitTurnsSinglePagesAndTheSpreadPreferenceOverridesIt() {
        val activity = instrumentation.startActivitySync(request()) as EpubReaderActivity
        try {
            rotate(activity, Configuration.ORIENTATION_PORTRAIT)
            awaitPage(activity, 1)
            assertTrue(activity.fixedLayout)
            await("six positions") { activity.readerModel.positionCount.value == PAGES }
            await("automatic spread resolves to single pages") { navigator(activity).settings.value.spread == Spread.NEVER }
            await("page label") { progressText(activity) == pageLabel(activity, 1) }
            SystemClock.sleep(1500)
            screenshot(activity, "fxl-portrait")

            main { navigator(activity).goForward(animated = false) }
            awaitPage(activity, 2)
            await("page label follows") { progressText(activity) == pageLabel(activity, 2) }
            jumpTo(activity, 4)
            main { navigator(activity).goBackward(animated = false) }
            awaitPage(activity, 3)

            // Two pages on request, even in portrait: the cover stays alone, then 2-3 and 4-5.
            main { activity.editPreferences { it.copy(spread = Spread.ALWAYS) } }
            await("spread forced on") { navigator(activity).settings.value.spread == Spread.ALWAYS }
            awaitPager(activity, double = true)
            jumpTo(activity, 1)
            main { navigator(activity).goForward(animated = false) }
            awaitPage(activity, 2)
            main { navigator(activity).goForward(animated = false) }
            awaitPage(activity, 4)
            val forced = progressText(activity)

            main { activity.editPreferences { it.copy(spread = null) } }
            await("back to automatic") { navigator(activity).settings.value.spread == Spread.NEVER }
            awaitPager(activity, double = false)
            awaitPage(activity, 4)
            main { navigator(activity).goForward(animated = false) }
            awaitPage(activity, 5)
            record(
                "fxl-portrait-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "orientation=${onMain { activity.resources.configuration.orientation }}",
                    "positions=${activity.readerModel.positionCount.value}",
                    "label=${pageLabel(activity, 1)}",
                    "forcedSpreadLabel=$forced",
                    "settings=${navigator(activity).settings.value}",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    @Test
    fun landscapeShowsSpreadsAutomatically() {
        val activity = instrumentation.startActivitySync(request()) as EpubReaderActivity
        try {
            awaitPage(activity, 1)
            rotate(activity, Configuration.ORIENTATION_LANDSCAPE)
            await("automatic spread resolves to two pages") { navigator(activity).settings.value.spread == Spread.ALWAYS }
            awaitPager(activity, double = true)
            jumpTo(activity, 1)
            main { navigator(activity).goForward(animated = false) }
            awaitPage(activity, 2)
            SystemClock.sleep(1500)
            screenshot(activity, "fxl-landscape")
            val webViews = onMain { countWebViews(navigator(activity).requireView()) }
            main { navigator(activity).goForward(animated = false) }
            awaitPage(activity, 4)
            main { navigator(activity).goBackward(animated = false) }
            awaitPage(activity, 2)

            main { activity.editPreferences { it.copy(spread = Spread.NEVER) } }
            await("spread forced off") { navigator(activity).settings.value.spread == Spread.NEVER }
            awaitPager(activity, double = false)
            awaitPage(activity, 2)
            main { navigator(activity).goForward(animated = false) }
            awaitPage(activity, 3)

            // Back to automatic from page 3: the rebuilt pager opens the 2-3 spread, and Readium's
            // locator names the left page of a spread, so the reader is on page 2 again.
            main { activity.editPreferences { it.copy(spread = null) } }
            await("automatic again") { navigator(activity).settings.value.spread == Spread.ALWAYS }
            awaitPager(activity, double = true)
            awaitPage(activity, 2)
            main { navigator(activity).goForward(animated = false) }
            awaitPage(activity, 4)
            record(
                "fxl-landscape-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "orientation=${onMain { activity.resources.configuration.orientation }}",
                    "attachedWebViewsOnSpread=$webViews",
                    "settings=${navigator(activity).settings.value}",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    @Test
    fun thePanelHidesTextPreferencesAndOffersTheSpread() {
        val activity = instrumentation.startActivitySync(request()) as EpubReaderActivity
        try {
            awaitPage(activity, 1)
            main { activity.showPreferences() }
            await("panel shown") { activity.preferencesSheet?.view != null }
            val sheet = requireNotNull(activity.preferencesSheet).requireView()
            val spreadGroup = sheet.findViewById<MaterialButtonToggleGroup>(R.id.spread_group)
            await("panel rendered") { spreadGroup.checkedButtonId == R.id.spread_auto }
            main {
                assertTrue(sheet.findViewById<View>(R.id.fixed_layout_group).isVisible())
                assertTrue(sheet.findViewById<View>(R.id.fixed_layout_hint).isVisible())
                assertFalse(sheet.findViewById<View>(R.id.reflowable_group).isVisible())
                assertFalse(sheet.findViewById<View>(R.id.overflow_group).isVisible())
                assertFalse(sheet.findViewById<View>(R.id.overflow_label).isVisible())
                assertTrue(sheet.findViewById<View>(R.id.theme_group).isVisible())
                val menu = activity.findViewById<Toolbar>(R.id.toolbar).menu
                assertFalse(menu.findItem(R.id.action_scroll_mode).isVisible)
                assertTrue(menu.findItem(R.id.action_preferences).isVisible)
                spreadGroup.check(R.id.spread_always)
            }
            await("preference set from the panel") { activity.readerModel.preferences.value.epub.spread == Spread.ALWAYS }
            await("navigator follows") { navigator(activity).settings.value.spread == Spread.ALWAYS }
            main { spreadGroup.check(R.id.spread_auto) }
            await("automatic again") { activity.readerModel.preferences.value.epub.spread == null }
            main { requireNotNull(activity.preferencesSheet).dismiss() }
            await("panel dismissed") { activity.preferencesSheet == null }
        } finally {
            finish(activity)
        }
    }

    @Test
    fun pinchZoomAndPanningAreReadiumsOwn() {
        val activity = instrumentation.startActivitySync(request()) as EpubReaderActivity
        try {
            rotate(activity, Configuration.ORIENTATION_PORTRAIT)
            awaitPage(activity, 1)
            SystemClock.sleep(1000)
            val page = onMain { navigator(activity).requireView() }
            val zoomLayout = onMain { requireNotNull(visibleZoomLayout(page)) { "no visible R2FXLLayout" } }
            val before = scaleOf(zoomLayout)
            pinch(activity, page, from = 60f, to = 400f)
            await("zoomed in") { scaleOf(zoomLayout) > 1.2f }
            val zoomed = scaleOf(zoomLayout)
            val panBefore = positionOf(zoomLayout)
            drag(activity, page, dx = -200f)
            SystemClock.sleep(500)
            val panAfter = positionOf(zoomLayout)
            SystemClock.sleep(1000)
            screenshot(activity, "fxl-zoomed")
            assertTrue("panned from $panBefore to $panAfter", abs(panAfter.first - panBefore.first) > 1f)
            record(
                "fxl-zoom-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "zoomLayout=${zoomLayout.javaClass.name}",
                    "scaleBefore=$before",
                    "scaleAfterPinch=$zoomed",
                    "positionBeforeDrag=$panBefore",
                    "positionAfterDrag=$panAfter",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    private fun rotate(activity: EpubReaderActivity, orientation: Int) {
        main {
            activity.requestedOrientation = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        await("orientation $orientation") { activity.resources.configuration.orientation == orientation }
        instrumentation.waitForIdleSync()
    }

    private fun finish(activity: EpubReaderActivity) {
        main {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity.finish()
        }
        instrumentation.waitForIdleSync()
    }

    private fun jumpTo(activity: EpubReaderActivity, page: Int) {
        val publication = requireNotNull(activity.readerModel.publication)
        val link = TocSheet.rows(publication).map { it.node }.firstOrNull { it.href.toString().endsWith("page$page.xhtml") }
            ?: publication.readingOrder[page - 1]
        main { activity.jumpTo(link) }
        awaitPage(activity, page)
    }

    /**
     * A spread change rebuilds the pager asynchronously (Readium's `InvalidateViewPager` event);
     * its page fragments carry a second URL only on spreads, which tells the two apart.
     */
    private fun awaitPager(activity: EpubReaderActivity, double: Boolean) {
        await(if (double) "two-page pager" else "single-page pager") {
            val pages = navigator(activity).childFragmentManager.fragments.filter { it.javaClass.simpleName == PAGE_FRAGMENT }
            val spreads = pages.count { page ->
                page.arguments?.let { BundleCompat.getParcelable(it, "secondUrl", Parcelable::class.java) } != null
            }
            pages.isNotEmpty() && if (double) spreads > 0 else spreads == 0
        }
        Log.i(TAG, "pager double=$double: ${pagerState(activity)}")
    }

    private fun awaitPage(activity: EpubReaderActivity, page: Int) {
        await("page $page", detail = { pagerState(activity) }) {
            activity.navigatorReady && currentHref(activity)?.endsWith("page$page.xhtml") == true
        }
        Log.i(TAG, "page $page: ${pagerState(activity)}")
    }

    /** The pager for failure messages: current item, adapter size, attachment and the page fragments' URLs. */
    private fun pagerState(activity: EpubReaderActivity): String = onMain {
        val navigator = navigator(activity)
        val pager = navigator.javaClass.getDeclaredField("resourcePager").apply { isAccessible = true }.get(navigator) as? View
        val item = pager?.javaClass?.getMethod("getCurrentItem")?.invoke(pager)
        val adapter = pager?.javaClass?.getMethod("getAdapter")?.invoke(pager)
        val count = adapter?.javaClass?.getMethod("getCount")?.invoke(adapter)
        val pages = navigator.childFragmentManager.fragments.filter { it.javaClass.simpleName == PAGE_FRAGMENT }.map { page ->
            listOf("firstUrl", "secondUrl").joinToString("+") { key ->
                page.arguments?.let { BundleCompat.getParcelable(it, key, Parcelable::class.java) }?.toString()?.substringAfterLast('/') ?: "-"
            }
        }
        "ready=${activity.navigatorReady} href=${currentHref(activity)?.substringAfterLast('/')} item=$item count=$count " +
            "attached=${pager?.isAttachedToWindow} size=${pager?.width}x${pager?.height} pages=$pages " +
            "settings=${navigator.settings.value.spread} prefs=${activity.readerModel.preferences.value.epub.spread} " +
            "orientation=${activity.resources.configuration.orientation}"
    }

    private fun pageLabel(activity: EpubReaderActivity, page: Int): String =
        activity.getString(R.string.text_progress_page_of, page, PAGES)

    private fun progressText(activity: EpubReaderActivity): String =
        onMain { activity.findViewById<TextView>(R.id.progress_text).text.toString() }

    private fun View.isVisible(): Boolean = visibility == View.VISIBLE && isShown

    private fun countWebViews(root: View): Int {
        var count = if (root is android.webkit.WebView && root.isShown) 1 else 0
        if (root is ViewGroup) for (index in 0 until root.childCount) count += countWebViews(root.getChildAt(index))
        return count
    }

    /** Readium's zoom layout is internal to its module, so it is found by class name and read by reflection. */
    private fun visibleZoomLayout(root: View): View? {
        if (root.javaClass.name == ZOOM_LAYOUT && root.isShown && root.getGlobalVisibleRect(Rect())) return root
        if (root is ViewGroup) for (index in 0 until root.childCount) visibleZoomLayout(root.getChildAt(index))?.let { return it }
        return null
    }

    private fun scaleOf(layout: View): Float = onMain { layout.javaClass.getMethod("getScale").invoke(layout) as Float }

    private fun positionOf(layout: View): Pair<Float, Float> = onMain {
        val x = layout.javaClass.getMethod("getPosX").invoke(layout) as Float
        val y = layout.javaClass.getMethod("getPosY").invoke(layout) as Float
        x to y
    }

    /**
     * Two fingers on the horizontal centre line moving apart from [from] to [to] pixels off centre.
     * The events go through the Activity's own dispatch (what the system's input pipeline calls
     * for a touch on its window) rather than `UiAutomation.injectInputEvent`: the input dispatcher
     * of the Xiaomi Pad (API 35) rejected injected streams both synchronously and asynchronously,
     * and the gesture handling under test is Readium's view layer anyway.
     */
    private fun pinch(activity: EpubReaderActivity, view: View, from: Float, to: Float) {
        check(Looper.myLooper() != Looper.getMainLooper())
        val (cx, cy) = centreOf(view)
        val properties = Array(2) { index -> MotionEvent.PointerProperties().apply { id = index; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        fun coords(offset: Float) = arrayOf(point(cx - offset, cy), point(cx + offset, cy))
        val downTime = SystemClock.uptimeMillis()
        dispatch(activity, downTime, MotionEvent.ACTION_DOWN, 1, properties, coords(from))
        dispatch(activity, downTime, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, properties, coords(from))
        val steps = 16
        for (step in 1..steps) {
            dispatch(activity, downTime, MotionEvent.ACTION_MOVE, 2, properties, coords(from + (to - from) * step / steps))
            SystemClock.sleep(16)
        }
        dispatch(activity, downTime, MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, properties, coords(to))
        SystemClock.sleep(16)
        dispatch(activity, downTime, MotionEvent.ACTION_UP, 1, properties, coords(to))
        instrumentation.waitForIdleSync()
    }

    /** One finger from the centre by [dx] pixels, slowly enough to be a scroll rather than a fling. */
    private fun drag(activity: EpubReaderActivity, view: View, dx: Float) {
        check(Looper.myLooper() != Looper.getMainLooper())
        val (cx, cy) = centreOf(view)
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER })
        val downTime = SystemClock.uptimeMillis()
        dispatch(activity, downTime, MotionEvent.ACTION_DOWN, 1, properties, arrayOf(point(cx, cy)))
        val steps = 20
        for (step in 1..steps) {
            dispatch(activity, downTime, MotionEvent.ACTION_MOVE, 1, properties, arrayOf(point(cx + dx * step / steps, cy)))
            SystemClock.sleep(25)
        }
        SystemClock.sleep(200)
        dispatch(activity, downTime, MotionEvent.ACTION_UP, 1, properties, arrayOf(point(cx + dx, cy)))
        instrumentation.waitForIdleSync()
    }

    /** The view's centre in window coordinates, which is what the Activity's dispatch expects. */
    private fun centreOf(view: View): Pair<Float, Float> = onMain {
        val location = IntArray(2).also { view.getLocationInWindow(it) }
        (location[0] + view.width / 2f) to (location[1] + view.height / 2f)
    }

    private fun point(x: Float, y: Float) = MotionEvent.PointerCoords().apply {
        this.x = x
        this.y = y
        pressure = 1f
        size = 1f
    }

    private fun dispatch(
        activity: EpubReaderActivity,
        downTime: Long,
        action: Int,
        count: Int,
        properties: Array<MotionEvent.PointerProperties>,
        coords: Array<MotionEvent.PointerCoords>,
    ) {
        val event = MotionEvent.obtain(
            downTime, SystemClock.uptimeMillis(), action, count, properties, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        try {
            main { activity.dispatchTouchEvent(event) }
        } finally {
            event.recycle()
        }
    }

    /**
     * The reader window's pixels. A display screenshot is not usable on the Xiaomi Pad: it ignores
     * orientation requests and letterboxes the Activity, and `UiAutomation.takeScreenshot` then
     * returns a crop of the display at the letterbox size taken from the display origin. API 24
     * has no `PixelCopy`, so the emulator keeps the display screenshot.
     */
    private fun screenshot(activity: EpubReaderActivity, name: String) {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decor = activity.window.decorView
            val target = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            val done = CountDownLatch(1)
            var result = PixelCopy.ERROR_UNKNOWN
            PixelCopy.request(activity.window, target, { result = it; done.countDown() }, Handler(Looper.getMainLooper()))
            assertTrue("window copy", done.await(10, TimeUnit.SECONDS) && result == PixelCopy.SUCCESS)
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

    private fun request(fixture: String = FIXTURE): Intent {
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

    private fun await(message: String, detail: (() -> String)? = null, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 20000
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = runCatching(condition).getOrDefault(false) }
            if (ready) return
            SystemClock.sleep(100)
        }
        val state = detail?.let { runCatching(it).getOrElse { error -> "detail failed: $error" } }
        fail("Timed out: $message" + (state?.let { " ($it)" } ?: ""))
    }

    private fun main(action: () -> Unit) = onMain(action)

    /** Runs [block] on the main thread; safe to nest because it never re-enters runOnMainSync from main. */
    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private companion object {
        const val TAG = "FxlTest"
        const val FIXTURE = "fixed-layout.epub"
        const val PAGES = 6
        const val ZOOM_LAYOUT = "org.readium.r2.navigator.epub.fxl.R2FXLLayout"
        const val PAGE_FRAGMENT = "R2FXLPageFragment"
    }
}
