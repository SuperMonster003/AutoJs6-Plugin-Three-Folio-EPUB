package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AbsSeekBar
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.LauncherActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.BookmarkSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PreferencesSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.SearchSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings.SettingsActivity
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import kotlin.math.roundToInt

/**
 * Roadmap P7.6 (DEVICE evidence): every interactive view of the reader chrome, its four panels, the
 * table of contents, the launcher and the settings screen carries a label a screen reader can speak
 * (content description, own text, or a labelled child) and a touch target of at least 48 x 48 dp;
 * visible chrome text is neither cut vertically nor pushed out of its parent; the right arrow turns
 * the page and Tab walks the focus. The host script runs the class once per mode (default, a 1.3 font
 * scale, night, forced RTL), passed as the `mode` instrumentation argument; the reader's effective
 * configuration is recorded, and under RTL the toolbar's navigation button must sit on the right.
 * Notes land in `files/p2-evidence/a11y-<mode>-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderAccessibilityInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val mode: String get() = InstrumentationRegistry.getArguments().getString("mode") ?: "default"
    private val notes = ArrayList<String>()
    private val problems = ArrayList<String>()

    @Test
    fun everyControlIsLabelledAndLargeEnoughAndTheChromeSurvivesTheMode() {
        note("mode=$mode API ${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL}")
        note("system configuration: " + describe(context.resources.configuration))

        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            await("the navigator is ready", 60_000) { activity.navigatorReady }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(500)
            val configuration = onMain { Configuration(activity.resources.configuration) }
            note("reader configuration: " + describe(configuration))
            val rtl = configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            if (mode == "rtl") assertTrue("forced RTL did not reach the reader: ${describe(configuration)}", rtl)

            audit("reader chrome", activity, onMain { activity.window.decorView })
            checkToolbarMirroring(activity, rtl)
            checkKeyboard(activity)

            openTableOfContents(activity)
            for ((name, tag, id) in listOf(
                Triple("bookmarks", BookmarkSheet.TAG, R.id.action_bookmarks),
                Triple("preferences", PreferencesSheet.TAG, R.id.action_preferences),
                Triple("search", SearchSheet.TAG, R.id.action_search),
            )) {
                main { menu(activity, id) }
                val decor = pollFor("$name sheet", 10_000) {
                    onMain {
                        (activity.supportFragmentManager.findFragmentByTag(tag) as? DialogFragment)
                            ?.dialog?.takeIf { it.isShowing }?.window?.decorView?.takeIf { it.width > 0 }
                    }
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(400)
                audit(name, activity, decor)
                main { (activity.supportFragmentManager.findFragmentByTag(tag) as? DialogFragment)?.dismissAllowingStateLoss() }
                instrumentation.waitForIdleSync()
                awaitOrNot(5_000) { activity.supportFragmentManager.findFragmentByTag(tag) == null }
            }
            auditDeclared("search bar", activity, R.id.search_bar)
            auditDeclared("read-aloud bar", activity, R.id.tts_bar)
            checkLandscape(activity)
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
            awaitOrNot(5_000) { activity.isDestroyed }
        }

        val launcher = instrumentation.startActivitySync(Intent(context, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            instrumentation.waitForIdleSync()
            SystemClock.sleep(800)
            audit("launcher", launcher, onMain { launcher.window.decorView })
        } finally {
            main { launcher.finish() }
            instrumentation.waitForIdleSync()
        }
        val settings = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            instrumentation.waitForIdleSync()
            SystemClock.sleep(800)
            audit("settings", settings, onMain { settings.window.decorView })
        } finally {
            main { settings.finish() }
            instrumentation.waitForIdleSync()
        }

        note("problems: ${problems.size}")
        problems.forEach { note("  $it") }
        writeNotes("a11y-$mode")
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun openTableOfContents(activity: EpubReaderActivity) {
        main { menu(activity, R.id.action_table_of_contents) }
        val decor = pollFor("table of contents dialog", 10_000) {
            onMain { activity.tableOfContentsDialog?.takeIf { it.isShowing }?.window?.decorView?.takeIf { it.width > 0 } }
        }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(400)
        audit("table of contents", activity, decor)
        main { activity.tableOfContentsDialog?.dismiss() }
        instrumentation.waitForIdleSync()
    }

    private fun menu(activity: EpubReaderActivity, id: Int) {
        val item = PopupMenu(activity, View(activity)).menu.add(0, id, 0, "")
        assertTrue("menu action $id not handled", activity.onOptionsItemSelected(item))
    }

    /** Walks every shown view under [root]: labels and 48 dp targets for controls, clipping for text. */
    private fun audit(screen: String, owner: android.app.Activity, root: View) {
        val density = owner.resources.displayMetrics.density
        var controls = 0
        var texts = 0
        val found = ArrayList<String>()
        onMain {
            fun walk(view: View, insideScroller: Boolean) {
                if (!view.isShown) return
                val scroller = insideScroller || view is RecyclerView || view is ScrollView || view is NestedScrollView
                if (view.importantForAccessibility != View.IMPORTANT_FOR_ACCESSIBILITY_NO) {
                    if (isControl(view)) {
                        controls++
                        val label = labelOf(view)
                        val widthDp = view.width / density
                        val heightDp = view.height / density
                        val id = name(owner, view)
                        if (label.isBlank()) found += "$screen: $id has no label a screen reader can speak"
                        if (widthDp < MIN_TARGET_DP - 0.5f || heightDp < MIN_TARGET_DP - 0.5f) {
                            found += "$screen: $id is ${widthDp.roundToInt()} x ${heightDp.roundToInt()} dp (label [$label])"
                        }
                    }
                    if (view is TextView && view.text.isNotBlank() && view !is EditText) {
                        texts++
                        val layout = view.layout
                        if (layout != null) {
                            val inner = view.height - view.paddingTop - view.paddingBottom
                            if (layout.height > inner + 1) {
                                found += "$screen: ${name(owner, view)} [${view.text.take(30)}] in ${(view.parent as? View)?.let { name(owner, it) }} " +
                                    "cuts its text vertically (${layout.lineCount} lines need ${layout.height} px, has $inner px)"
                            }
                        }
                        if (!scroller) {
                            val visible = Rect()
                            if (!view.getGlobalVisibleRect(visible) || visible.height() < view.height - 1 || visible.width() < view.width - 1) {
                                found += "$screen: ${name(owner, view)} is pushed out of its parent (visible ${visible.width()} x ${visible.height()} of ${view.width} x ${view.height})"
                            }
                        }
                    }
                }
                if (view is ViewGroup) for (index in 0 until view.childCount) walk(view.getChildAt(index), scroller)
            }
            walk(root, false)
        }
        note("$screen: $controls controls, $texts texts, ${found.size} problems")
        problems += found
    }

    /** Bars that only show while a search or a voice is active: sizes and labels as declared in the layout. */
    private fun auditDeclared(screen: String, activity: EpubReaderActivity, barId: Int) {
        val density = activity.resources.displayMetrics.density
        var controls = 0
        onMain {
            fun walk(view: View) {
                if (view is ImageButton) {
                    controls++
                    val params = view.layoutParams
                    val widthDp = params.width / density
                    val heightDp = params.height / density
                    if (widthDp < MIN_TARGET_DP - 0.5f || heightDp < MIN_TARGET_DP - 0.5f) {
                        problems += "$screen: ${name(activity, view)} declares ${widthDp.roundToInt()} x ${heightDp.roundToInt()} dp"
                    }
                    if (view.contentDescription.isNullOrBlank()) problems += "$screen: ${name(activity, view)} has no content description"
                }
                if (view is ViewGroup) for (index in 0 until view.childCount) walk(view.getChildAt(index))
            }
            walk(activity.findViewById(barId))
        }
        note("$screen: $controls declared controls checked")
    }

    private fun checkToolbarMirroring(activity: EpubReaderActivity, rtl: Boolean) {
        val toolbar = onMain { activity.findViewById<Toolbar>(R.id.toolbar) }
        val navigation = onMain {
            (0 until toolbar.childCount).map { toolbar.getChildAt(it) }.firstOrNull { it is ImageButton && it.isShown }
        } ?: run {
            note("toolbar: no navigation button shown")
            return
        }
        val rect = Rect()
        val centre = onMain { navigation.getGlobalVisibleRect(rect); rect.centerX() }
        val width = onMain { toolbar.width }
        val onTheRight = centre > width / 2
        note("toolbar: navigation button centre x=$centre of $width (${if (onTheRight) "right" else "left"} side), layout ${if (rtl) "rtl" else "ltr"}, label [${onMain { navigation.contentDescription }}]")
        if (rtl != onTheRight) problems += "toolbar: navigation button on the ${if (onTheRight) "right" else "left"} under ${if (rtl) "RTL" else "LTR"}"
    }

    private fun checkKeyboard(activity: EpubReaderActivity) {
        // Progress restored from an earlier mode may sit on the last chapter, where the right arrow has nothing left to turn to.
        main { navigator(activity).go(requireNotNull(activity.readerModel.publication).readingOrder[0], animated = false) }
        awaitOrNot(10_000) { locator(activity)?.contains("chapter1.xhtml") == true }
        SystemClock.sleep(1_000)
        val before = locator(activity)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
        val turned = awaitOrNot(10_000) { locator(activity) != before }
        note("keyboard: DPAD_RIGHT $before -> ${locator(activity)} (${if (turned) "turned" else "did not turn"})")
        if (!turned) problems += "keyboard: the right arrow did not turn the page"
        val trail = ArrayList<String>()
        repeat(6) {
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
            instrumentation.waitForIdleSync()
            trail += onMain { activity.currentFocus?.let { name(activity, it) } ?: "none" }
        }
        note("keyboard: Tab focus trail " + trail.joinToString(" > "))
    }

    /** The reader handles orientation itself (configChanges): the page and the chrome must survive a turn to landscape. */
    private fun checkLandscape(activity: EpubReaderActivity) {
        val before = locator(activity)
        main { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        val landscape = awaitOrNot(10_000) { activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(1_500)
        val configuration = onMain { Configuration(activity.resources.configuration) }
        note("landscape: ${if (landscape) "reached" else "not reached"}; " + describe(configuration) + "; navigatorReady=${onMain { activity.navigatorReady }}; locator $before -> ${locator(activity)}")
        if (!landscape) problems += "landscape: the orientation did not change within 10 s"
        if (landscape) {
            if (!onMain { activity.navigatorReady }) problems += "landscape: the navigator is not ready after the turn"
            audit("reader chrome (landscape)", activity, onMain { activity.window.decorView })
        }
        main { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        awaitOrNot(10_000) { activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
        instrumentation.waitForIdleSync()
    }

    private fun navigator(activity: EpubReaderActivity): EpubNavigatorFragment =
        requireNotNull(activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)

    private fun locator(activity: EpubReaderActivity): String? = onMain {
        (activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)
            ?.takeIf { it.view != null }?.currentLocator?.value?.let { "${it.href} @ ${it.locations.progression}" }
    }

    /**
     * A control is anything a user operates directly. A switch that is neither clickable nor focusable is a
     * decoration of its row (the row carries the label and the click); a WebView, and a container whose only
     * content is a WebView, expose the page through the WebView's own accessibility tree and carry no label.
     */
    private fun isControl(view: View): Boolean {
        if (view is WebView || (view is ViewGroup && hostsWebView(view))) return false
        return view.isClickable || view.isLongClickable || view is AbsSeekBar || view is EditText ||
            (view is CompoundButton && (view.isClickable || view.isFocusable))
    }

    private fun hostsWebView(group: ViewGroup): Boolean {
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            if (child is WebView || (child is ViewGroup && hostsWebView(child))) return true
        }
        return false
    }

    private fun labelOf(view: View): String {
        view.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        if (view is TextView) {
            view.text?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            view.hint?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                val child = view.getChildAt(index)
                if (child.isShown && child.importantForAccessibility != View.IMPORTANT_FOR_ACCESSIBILITY_NO) {
                    labelOf(child).takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return ""
    }

    private fun name(owner: android.app.Activity, view: View): String {
        val id = view.id.takeIf { it != View.NO_ID }?.let { runCatching { owner.resources.getResourceEntryName(it) }.getOrNull() }
        return view.javaClass.simpleName + (id?.let { "#$it" } ?: "")
    }

    private fun describe(configuration: Configuration): String {
        val night = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val rtl = configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        return "fontScale=${configuration.fontScale} night=$night layout=${if (rtl) "rtl" else "ltr"} locale=${configuration.locales[0]}" +
            " ${configuration.screenWidthDp}x${configuration.screenHeightDp}dp ${configuration.densityDpi}dpi orientation=${configuration.orientation}"
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

    private fun <T : Any> pollFor(message: String, timeoutMillis: Long, supplier: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            runCatching(supplier).getOrNull()?.let { return it }
            SystemClock.sleep(250)
        }
        throw AssertionError("Timed out: $message")
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
        if (!awaitOrNot(timeoutMillis, condition)) fail("Timed out: $message")
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
        const val MIN_TARGET_DP = 48f
    }
}
