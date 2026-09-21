package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationAddResult
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationDatabase
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationMarkdown
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationStyle
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.AnnotationExport
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.AnnotationSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import kotlinx.coroutines.runBlocking
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.json.JSONObject
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
import java.io.File

/**
 * Roadmap P9.3 device evidence: the Markdown export of a book's highlights carries the title,
 * the author, the chapters in reading order, the quotes, the notes and local times; the share
 * intent wraps it as plain text with the title as subject; the save path writes it to the
 * document the user picked (here a file URI) and truncates what was there; the panel's export
 * button follows the list; an empty book only gets a toast. Notes land in `files/p9-evidence/`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderAnnotationExportInstrumentationTest {

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
    fun theExportCarriesTheBookAndIsSharedOrSaved() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        val target = File(context.cacheDir, "p9-export.md").apply { writeText("stale content that is longer than the export will be\n".repeat(50)) }
        try {
            awaitHref(activity, "chapter1.xhtml")
            val sheet = openSheet(activity)
            await("export disabled while empty") { !sheet.requireView().findViewById<View>(R.id.annotation_export).isEnabled }
            main { activity.exportAnnotations() }
            SystemClock.sleep(300)
            assertNull(activity.annotationExportDialog)
            main { sheet.dismiss() }
            await("sheet closed") { activity.annotationSheet == null }

            val chapter3 = runBlocking { activity.readerModel.addAnnotation(locator("OEBPS/chapter3.xhtml", 0.5, "the last chapter"), AnnotationStyle.UNDERLINE, AnnotationColors.BLUE, "second note", "Chapter 3") }
            val chapter1 = runBlocking { activity.readerModel.addAnnotation(locator("OEBPS/chapter1.xhtml", 0.1, "The lighthouse keeper"), AnnotationStyle.HIGHLIGHT, AnnotationColors.YELLOW, "first\nnote", "Chapter 1") }
            assertTrue("$chapter3", chapter3 is AnnotationAddResult.Added)
            assertTrue("$chapter1", chapter1 is AnnotationAddResult.Added)
            await("two highlights") { activity.annotations.value.size == 2 }

            val markdown = onMain { activity.annotationMarkdown() }
            val lines = markdown.lines()
            assertEquals("# Minimal EPUB 3", lines[0])
            assertEquals("AutoJs6 Readium EPUB Reader fixtures", lines[2])
            val headings = lines.filter { it.startsWith("## ") }
            assertEquals(listOf("## Chapter 1", "## Chapter 3"), headings)
            assertTrue(markdown, markdown.indexOf("> The lighthouse keeper") < markdown.indexOf("> the last chapter"))
            assertTrue(markdown, markdown.contains("\nfirst\nnote\n"))
            assertTrue(markdown, markdown.contains("\nsecond note\n"))
            assertEquals(2, lines.count { it.startsWith("*") && it.endsWith("*") })
            assertEquals("Minimal EPUB 3.md", AnnotationMarkdown.fileName(activity.readerModel.publication?.metadata?.title))

            // The share sheet gets the text and the title as subject.
            val chooser = AnnotationExport.shareIntent(markdown, "Minimal EPUB 3")
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            val send = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals("text/plain", send.type)
            assertEquals(markdown, send.getStringExtra(Intent.EXTRA_TEXT))
            assertEquals("Minimal EPUB 3", send.getStringExtra(Intent.EXTRA_SUBJECT))

            // The save path writes the document the picker returned, replacing what was there.
            main { activity.writeAnnotationExport(Uri.fromFile(target)) }
            await("file written") { target.length() == markdown.toByteArray().size.toLong() }
            assertEquals(markdown, target.readText())

            // The dialog offering the two ways out.
            val sheetAgain = openSheet(activity)
            await("export enabled") { sheetAgain.requireView().findViewById<View>(R.id.annotation_export).isEnabled }
            main { activity.exportAnnotations() }
            await("export dialog") { activity.annotationExportDialog?.isShowing == true }
            main { activity.annotationExportDialog?.dismiss() }
            await("export dialog gone") { activity.annotationExportDialog == null }
            main { sheetAgain.dismiss() }

            // A broken target only reports a failure.
            main { activity.writeAnnotationExport(Uri.fromFile(File(context.cacheDir, "missing-directory/p9-export.md"))) }
            SystemClock.sleep(800)
            assertFalse(File(context.cacheDir, "missing-directory").exists())

            record(
                "annotations-export-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "bytes=${target.length()} lines=${lines.size} headings=${headings.size}",
                    "fileName=${AnnotationMarkdown.fileName(activity.readerModel.publication?.metadata?.title)}",
                ),
            )
            outputFile("annotations-export-api${Build.VERSION.SDK_INT}.md").writeText(markdown)
        } finally {
            target.delete()
            finish(activity)
        }
    }

    // ---- helpers ----

    private fun locator(href: String, progression: Double, highlight: String): JSONObject =
        JSONObject()
            .put("href", href)
            .put("type", "application/xhtml+xml")
            .put("locations", JSONObject().put("progression", progression))
            .put("text", JSONObject().put("highlight", highlight))

    private fun openSheet(activity: EpubReaderActivity): AnnotationSheet {
        main { activity.showAnnotations() }
        await("annotation sheet") { activity.annotationSheet?.view != null }
        return requireNotNull(activity.annotationSheet)
    }

    private fun record(name: String, lines: List<String>) {
        outputFile(name).writeText(lines.joinToString("\n") + "\n")
    }

    private fun outputFile(name: String): File =
        File(context.filesDir, "p9-evidence").apply { mkdirs() }.resolve(name)

    private fun finish(activity: EpubReaderActivity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(500)
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
