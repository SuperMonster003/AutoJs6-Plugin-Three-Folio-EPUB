package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.LauncherActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBook
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBooksStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.BookmarkSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PreferencesSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.SearchSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsStatus
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File

/**
 * Roadmap P8: captures the README screenshots on a real device from the repository's own fixtures
 * (`docs/fixtures`, roadmap D22: no third-party book is ever shown). Runs only with
 * `-e screenshots 1`; the PNG files land in `files/p2-evidence/screenshots/` and are pulled with
 * `run-as`, then scaled by `.python/generate_screenshots.py` into `docs/images/screenshots/`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderScreenshotCaptureTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val notes = ArrayList<String>()

    @Test
    fun captureTheReadmeScreenshots() {
        assumeTrue("README screenshots are captured on demand only: -e screenshots 1", InstrumentationRegistry.getArguments().getString("screenshots") == "1")
        note("device ${Build.MANUFACTURER} ${Build.MODEL} API ${Build.VERSION.SDK_INT} ${context.resources.displayMetrics.let { "${it.widthPixels}x${it.heightPixels} @ ${it.densityDpi} dpi" }}")
        outputDirectory().listFiles()?.forEach { it.delete() }

        val store = RecentBooksStore.forFilesDirectory(context.filesDir)
        val previousBooks = store.read()
        try {
            seedRecentBooks(store)
            captureReader()
            captureBook(FIXTURE_VERTICAL, "vertical-ja")
            captureBook(FIXTURE_FIXED, "fixed-layout")
            captureLauncher()
            captureSettings()
        } finally {
            store.write(previousBooks)
        }
        File(context.filesDir, "p2-evidence").resolve("screenshots-api${Build.VERSION.SDK_INT}.txt").writeText(notes.joinToString("\n") + "\n")
    }

    /** The launcher shows the three fixtures as recent books; the titles and authors are the fixtures' own metadata. */
    private fun seedRecentBooks(store: RecentBooksStore) {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val day = 24 * hour
        store.write(
            listOf(
                RecentBook(uri(FIXTURE_MAIN), FIXTURE_MAIN, addedAt = now - 3 * day, lastReadAt = now - hour, title = "Minimal EPUB 3", author = AUTHOR, progression = 0.34),
                RecentBook(uri(FIXTURE_VERTICAL), FIXTURE_VERTICAL, addedAt = now - 2 * day, lastReadAt = now - day, title = "灯台守 (縦書き)", author = AUTHOR, progression = 0.67),
                RecentBook(uri(FIXTURE_FIXED), FIXTURE_FIXED, addedAt = now - day, lastReadAt = now - 2 * day, title = "The Lighthouse Plates", author = AUTHOR, progression = 0.5),
            ),
        )
    }

    private fun captureReader() {
        val activity = open(FIXTURE_MAIN)
        try {
            settle(1_500)
            screenshot("reader")

            main { menu(activity, R.id.action_table_of_contents) }
            pollFor("table of contents dialog", 10_000) { onMain { activity.tableOfContentsDialog?.takeIf { it.isShowing }?.window?.decorView?.takeIf { it.width > 0 } } }
            settle(600)
            screenshot("table-of-contents")
            main { activity.tableOfContentsDialog?.dismiss() }
            settle(400)

            main { menu(activity, R.id.action_bookmark) }
            settle(600)
            sheet(activity, BookmarkSheet.TAG, R.id.action_bookmarks, "bookmarks")

            sheet(activity, PreferencesSheet.TAG, R.id.action_preferences, "preferences")

            main { activity.showSearch() }
            val search = pollFor("search sheet", 10_000) { activity.searchSheet?.takeIf { onMain { it.view != null } } }
            main { search.submit("fog") }
            await("search results", 30_000) { activity.searchState.value.results.isNotEmpty() }
            settle(800)
            screenshot("search")
            dismiss(activity, SearchSheet.TAG)

            main { activity.readerModel.setThemeMode(ThemeMode.DARK) }
            settle(1_500)
            screenshot("dark-theme")
            main { activity.readerModel.setThemeMode(ThemeMode.SEPIA) }
            settle(1_500)
            screenshot("sepia-theme")
            main { activity.readerModel.setThemeMode(ThemeMode.LIGHT) }
            settle(1_000)

            main { activity.startReadAloud() }
            val speaking = awaitOrNot(60_000) { activity.ttsStatus.value == TtsStatus.PLAYING && activity.ttsLocation.value != null }
            note("read-aloud: ${if (speaking) "playing" else "unavailable (${onMain { activity.ttsStatus.value }})"}")
            if (speaking) {
                settle(1_500)
                screenshot("read-aloud")
                main { activity.stopReadAloud() }
                awaitOrNot(10_000) { activity.ttsStatus.value == TtsStatus.IDLE }
            }
        } finally {
            main { activity.readerModel.setThemeMode(ThemeMode.LIGHT) }
            finish(activity)
        }
    }

    private fun captureBook(fixture: String, name: String) {
        val activity = open(fixture)
        try {
            settle(1_500)
            screenshot(name)
        } finally {
            finish(activity)
        }
    }

    private fun captureLauncher() {
        val launcher = instrumentation.startActivitySync(Intent(context, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as LauncherActivity
        try {
            await("recent books shown", 10_000) { launcher.shownBooks.size == 3 }
            settle(1_000)
            screenshot("launcher")
        } finally {
            main { launcher.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    private fun captureSettings() {
        val settings = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            settle(1_000)
            screenshot("settings")
        } finally {
            main { settings.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    private fun sheet(activity: EpubReaderActivity, tag: String, action: Int, name: String) {
        main { menu(activity, action) }
        pollFor("$name sheet", 10_000) {
            onMain { (activity.supportFragmentManager.findFragmentByTag(tag) as? DialogFragment)?.dialog?.takeIf { it.isShowing }?.window?.decorView?.takeIf { it.width > 0 } }
        }
        settle(800)
        screenshot(name)
        dismiss(activity, tag)
    }

    private fun dismiss(activity: EpubReaderActivity, tag: String) {
        main { (activity.supportFragmentManager.findFragmentByTag(tag) as? DialogFragment)?.dismissAllowingStateLoss() }
        instrumentation.waitForIdleSync()
        awaitOrNot(5_000) { activity.supportFragmentManager.findFragmentByTag(tag) == null }
        settle(300)
    }

    private fun open(fixture: String): EpubReaderActivity {
        val activity = instrumentation.startActivitySync(request(fixture)) as EpubReaderActivity
        await("the navigator is ready for $fixture", 60_000) { activity.navigatorReady && navigator(activity)?.currentLocator?.value != null }
        instrumentation.waitForIdleSync()
        return activity
    }

    private fun finish(activity: EpubReaderActivity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
        awaitOrNot(5_000) { activity.isDestroyed }
        settle(300)
    }

    private fun settle(millis: Long) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(millis)
    }

    private fun screenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: throw AssertionError("no screenshot for $name")
        outputDirectory().resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        note("$name: ${bitmap.width}x${bitmap.height}")
    }

    private fun outputDirectory(): File = File(context.filesDir, "p2-evidence/screenshots").apply { mkdirs() }

    private fun navigator(activity: EpubReaderActivity): EpubNavigatorFragment? =
        activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment

    private fun menu(activity: EpubReaderActivity, id: Int) {
        val item = PopupMenu(activity, View(activity)).menu.add(0, id, 0, "")
        assertTrue("menu action $id not handled", activity.onOptionsItemSelected(item))
    }

    private fun uri(fixture: String): String = EpubReaderTestContentProvider.documentUri(fixture).toString()

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

    private companion object {
        const val FIXTURE_MAIN = "minimal-epub3.epub"
        const val FIXTURE_VERTICAL = "vertical-ja.epub"
        const val FIXTURE_FIXED = "fixed-layout.epub"
        const val AUTHOR = "AutoJs6 Readium EPUB Reader fixtures"
    }
}
