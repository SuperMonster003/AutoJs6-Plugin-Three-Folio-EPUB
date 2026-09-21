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
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationDatabase
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationRow
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationStyle
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.BookAnnotation
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.AnnotationDialog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.AnnotationSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchStatus
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P9.2 device evidence: a passage is highlighted with the last chosen colour, the
 * highlight renders as a decoration of the `annotations` group, highlighting the same place
 * again restyles it, the editor adds a note (and remembers the colour and style for the next
 * quick highlight), the panel lists the book's highlights by chapter in reading order, jumps,
 * edits and deletes, everything survives a relaunch, clear all empties the book, and the
 * settings page counts the stored highlights and clears them. Screenshots and notes land in
 * `files/p9-evidence/annotations-*`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderAnnotationsInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)
    private val preferencesStore get() = ReaderPreferencesStore.forFilesDirectory(context.filesDir)
    private val dao get() = AnnotationDatabase.get(context).annotations()

    @Before
    fun resetState() = cleanUp()

    @After
    fun cleanUp() {
        runBlocking { dao.deleteEverything() }
        booksDirectory.deleteRecursively()
        preferencesStore.clear()
        context.getSharedPreferences(ReaderSettings.PREFERENCES_NAME, 0).edit().clear().commit()
    }

    @Test
    fun highlightsAreAddedRestyledAnnotatedListedEditedDeletedAndKeptAcrossRelaunch() {
        val lines = ArrayList<String>()
        var activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            main { assertTrue(activity.findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.action_annotations).isVisible) }
            assertTrue(activity.annotations.value.isEmpty())

            // The places to highlight: the search hits stand in for text selections (same locator shape: page + text context).
            main { activity.submitSearch("lighthouse") }
            await("search done") { activity.searchState.value.status == SearchStatus.DONE }
            val hits = activity.searchState.value.results
            val first = hits.first { it.href.toString().endsWith("chapter1.xhtml") }
            val last = hits.last { it.href.toString().endsWith("chapter3.xhtml") }
            main { activity.closeSearch() }

            val started = SystemClock.uptimeMillis()
            main { activity.addAnnotation(first, AnnotationStyle.HIGHLIGHT, AnnotationColors.YELLOW, note = null) }
            await("highlight added") { activity.annotations.value.size == 1 }
            val addMillis = SystemClock.uptimeMillis() - started
            val added = activity.annotations.value.single()
            assertTrue(added.href, added.href.endsWith("chapter1.xhtml"))
            assertEquals("lighthouse", added.quote)
            assertEquals("Chapter 1", added.chapter)
            assertEquals(AnnotationStyle.HIGHLIGHT, added.style)
            assertEquals(AnnotationColors.YELLOW, added.color)
            assertNull(added.note)
            assertEquals(first.href.toString(), Locator.fromJSON(JSONObject(added.locator))?.href?.toString())
            awaitDecorations(activity, 1)
            SystemClock.sleep(500)
            screenshot(activity.window, "annotations-page")

            // The same place again only changes the colour.
            main { activity.addAnnotation(first, AnnotationStyle.HIGHLIGHT, AnnotationColors.PINK, note = null) }
            await("restyled") { activity.annotations.value.singleOrNull()?.color == AnnotationColors.PINK }
            assertEquals(added.id, activity.annotations.value.single().id)
            assertEquals(AnnotationColors.PINK, ReaderSettings(context).annotationColor)

            // "Add note" on the chapter 3 hit: the editor's choices are stored and become the next defaults.
            main { AnnotationDialog.showNew(activity.supportFragmentManager, last, AnnotationStyle.HIGHLIGHT, AnnotationColors.PINK) }
            var dialog = awaitDialog(activity)
            assertEquals(0L, dialog.annotationId)
            main {
                dialog.selectStyle(AnnotationStyle.UNDERLINE)
                dialog.selectColor(AnnotationColors.GREEN)
                dialog.setNote("my note")
            }
            SystemClock.sleep(500)
            screenshot(requireNotNull(dialog.dialog?.window), "annotations-editor")
            main { dialog.save() }
            await("note added") { activity.annotations.value.size == 2 && activity.annotationDialog == null }
            val noted = activity.annotations.value.first { it.id != added.id }
            assertTrue(noted.href, noted.href.endsWith("chapter3.xhtml"))
            assertEquals("Chapter 3", noted.chapter)
            assertEquals(AnnotationStyle.UNDERLINE, noted.style)
            assertEquals(AnnotationColors.GREEN, noted.color)
            assertEquals("my note", noted.note)
            assertEquals(AnnotationColors.GREEN, ReaderSettings(context).annotationColor)
            assertEquals(AnnotationStyle.UNDERLINE, ReaderSettings(context).annotationStyle)

            // The panel: reading order, one header per chapter.
            var sheet = openSheet(activity)
            await("two items under two headers") { sheet.items.map { it.id } == listOf(added.id, noted.id) }
            assertEquals(listOf("Chapter 1", "Chapter 3"), onMain { sheet.rows.filterIsInstance<AnnotationRow.Header>().map { it.title } })
            main {
                assertTrue(sheet.requireView().findViewById<View>(R.id.annotation_clear).isEnabled)
                assertFalse(sheet.requireView().findViewById<View>(R.id.annotation_empty).isVisible)
            }
            SystemClock.sleep(800)
            screenshot(requireNotNull(sheet.dialog?.window), "annotations-panel")

            // Tapping the second row jumps to chapter 3, where the underline renders.
            main { sheet.open(1) }
            awaitHref(activity, "chapter3.xhtml")
            await("sheet dismissed") { activity.annotationSheet == null }
            awaitDecorations(activity, 1)

            // A tap on the decoration opens the editor of that highlight.
            main { assertTrue(activity.onAnnotationDecorationActivated("${EpubReaderActivity.ANNOTATION_DECORATIONS}-${noted.id}")) }
            dialog = awaitDialog(activity)
            assertEquals(noted.id, dialog.annotationId)
            assertEquals(AnnotationStyle.UNDERLINE, dialog.selectedStyle)
            assertEquals(AnnotationColors.GREEN, dialog.selectedColor)
            assertEquals("my note", onMain { dialog.noteText })
            main {
                dialog.setNote("edited")
                dialog.selectColor(AnnotationColors.BLUE)
                dialog.save()
            }
            await("edited") { activity.annotations.value.firstOrNull { it.id == noted.id }?.let { it.note == "edited" && it.color == AnnotationColors.BLUE } == true }
            assertEquals(noted.createdAt, activity.annotations.value.first { it.id == noted.id }.createdAt)
            main { assertFalse(activity.onAnnotationDecorationActivated("${EpubReaderActivity.ANNOTATION_DECORATIONS}-999999")) }

            // Deleting from the panel's editor.
            sheet = openSheet(activity)
            await("rows") { sheet.items.size == 2 }
            main { activity.editAnnotation(sheet.items[0]) }
            dialog = awaitDialog(activity)
            main { dialog.delete() }
            await("one left") { activity.annotations.value.map { it.id } == listOf(noted.id) && sheet.items.size == 1 }
            main { sheet.dismiss() }
            await("sheet closed") { activity.annotationSheet == null }
            lines += "addMillis=$addMillis quote=${added.quote} chapter=${added.chapter}"
            lines += "cssSelector=${JSONObject(added.locator).getJSONObject("locations").optString("cssSelector")}"
        } finally {
            finish(activity)
        }

        // Relaunch: the highlight is back on the restored page, and clear all empties the book.
        activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter3.xhtml")
            await("highlight survives a relaunch") { activity.annotations.value.size == 1 }
            val restored = activity.annotations.value.single()
            assertEquals("edited", restored.note)
            awaitDecorations(activity, 1)

            val sheet = openSheet(activity)
            await("row") { sheet.items.size == 1 }
            main { activity.clearAnnotations() }
            await("cleared") { activity.annotations.value.isEmpty() && sheet.items.isEmpty() }
            main {
                assertTrue(sheet.requireView().findViewById<View>(R.id.annotation_empty).isVisible)
                assertFalse(sheet.requireView().findViewById<View>(R.id.annotation_clear).isEnabled)
            }
            awaitDecorations(activity, 0, exactly = true)
            main { sheet.dismiss() }
            assertEquals(0, runBlocking { dao.countAll() })
            lines += "relaunch=kept clear=empty database=${AnnotationDatabase.sizeOnDisk(context)}B"
            record("annotations-api${Build.VERSION.SDK_INT}.txt", listOf("device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})") + lines)
        } finally {
            finish(activity)
        }
    }

    @Test
    fun theSettingsPageCountsTheHighlightsAndClearsThem() {
        runBlocking {
            dao.insert(BookAnnotation(bookKey = "k".repeat(64), href = "a.xhtml", locator = "{}", createdAt = 1L))
            dao.insert(BookAnnotation(bookKey = "k".repeat(64), href = "b.xhtml", locator = "{}", note = "n", createdAt = 2L))
        }
        val page = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        try {
            await("two highlights counted") { summary(page).startsWith("2 ") }
            main { page.clearAnnotations() }
            await("nothing stored") { summary(page) == context.getString(R.string.text_settings_nothing_stored) }
            assertEquals(0, runBlocking { dao.countAll() })
        } finally {
            main { page.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    // ---- helpers ----

    private fun summary(page: SettingsActivity): String = onMain {
        val content = page.findViewById<ViewGroup>(R.id.content)
        val row = (0 until content.childCount).map(content::getChildAt).first { it.tag == R.string.text_settings_clear_annotations }
        row.findViewById<TextView>(R.id.row_summary).text.toString()
    }

    private fun openSheet(activity: EpubReaderActivity): AnnotationSheet {
        main { activity.showAnnotations() }
        await("annotation sheet") { activity.annotationSheet?.view != null }
        return requireNotNull(activity.annotationSheet)
    }

    private fun awaitDialog(activity: EpubReaderActivity): AnnotationDialog {
        await("annotation editor") { activity.annotationDialog?.dialog?.isShowing == true }
        return requireNotNull(activity.annotationDialog)
    }

    private fun decorationCount(activity: EpubReaderActivity): Int? =
        evaluate(activity, "document.querySelectorAll('[data-group=\"${EpubReaderActivity.ANNOTATION_DECORATIONS}\"] > div').length")?.toIntOrNull()

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

    private fun navigator(activity: EpubReaderActivity): EpubNavigatorFragment =
        requireNotNull(activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)

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
        File(context.filesDir, "p9-evidence").apply { mkdirs() }.resolve(name)

    private fun finish(activity: EpubReaderActivity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(500) // let the pending progress writes land before the next launch
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
    }
}
