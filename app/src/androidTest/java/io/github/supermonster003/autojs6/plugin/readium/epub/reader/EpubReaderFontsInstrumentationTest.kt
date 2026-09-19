package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Spinner
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontEntry
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontInspection
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PreferencesSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.FontImportResult
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.FontStore
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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P2.2 device evidence: a font imported through the reader lands in the catalog, is
 * declared to the rebuilt navigator, loads inside the WebView (`document.fonts`) and styles the
 * body; it survives a relaunch and its deletion clears the preference. Foreign files, duplicates
 * and missing documents leave the catalog untouched; the panel lists imported fonts and selects them.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderFontsInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)
    private val preferencesStore get() = ReaderPreferencesStore.forFilesDirectory(context.filesDir)
    private val fontStore get() = FontStore.forFilesDirectory(context.filesDir)
    private val fontsDirectory get() = File(context.filesDir, FontStore.DIRECTORY_NAME)

    @Before
    fun resetState() = cleanUp()

    @After
    fun cleanUp() {
        booksDirectory.deleteRecursively()
        preferencesStore.clear()
        fontStore.clear()
        context.getSharedPreferences(ReaderSettings.PREFERENCES_NAME, 0).edit().clear().commit()
    }

    @Test
    fun anImportedFontIsServedToTheBookAndSurvivesARelaunch() {
        val fontUri = stageBundledFont()
        val request = request("minimal-epub3.epub")
        var activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        lateinit var entry: FontEntry
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val before = navigator(activity)

            main { activity.importFont(fontUri) }
            await("catalogued") { activity.fontCatalog.value.fonts.size == 1 }
            entry = activity.fontCatalog.value.fonts.single()
            assertEquals("${entry.sha256}.ttf", entry.fileName)
            assertTrue(entry.family, entry.family.startsWith(entry.displayName))
            assertTrue(fontStore.fileFor(entry).isFile)
            await("selected") { activity.readerModel.preferences.value.epub.fontFamily?.name == entry.family }
            await("navigator rebuilt with the font") {
                val fragment = navigator(activity)
                fragment !== before && activity.navigatorReady && fragment.settings.value.fontFamily?.name == entry.family
            }
            val face = awaitFontFaceLoaded(activity, entry.family)
            assertTrue(face.toString(), face.optString("userFamily").contains(entry.family))
            assertTrue(face.toString(), face.optString("bodyFamily").contains(entry.family))
            record(
                "fonts-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "entry=$entry",
                    "face=$face",
                    "index=${File(fontsDirectory, FontStore.INDEX_FILE_NAME).readText().replace('\n', ' ')}",
                ),
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }

        activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("relaunched with the font") {
                activity.navigatorReady && navigator(activity).settings.value.fontFamily?.name == entry.family
            }
            awaitFontFaceLoaded(activity, entry.family)

            main { activity.deleteFont(entry) }
            await("catalog empty") { activity.fontCatalog.value.isEmpty }
            await("preference cleared") { activity.readerModel.preferences.value.epub.fontFamily == null }
            await("navigator rebuilt without the font") {
                activity.navigatorReady && navigator(activity).settings.value.fontFamily == null
            }
            assertFalse(fontStore.fileFor(entry).exists())
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun duplicatesAndForeignFilesLeaveTheCatalogUntouched() {
        val fontUri = stageBundledFont()
        val textUri = stage("not-a-font.ttf") { "This is not a font file".toByteArray() }
        val collectionUri = stage("collection.ttc") { "ttcf".toByteArray() + ByteArray(64) }
        val missingUri = "content://${EpubReaderTestContentProvider.AUTHORITY}/documents/missing.ttf".toUri()
        val activity = instrumentation.startActivitySync(request("minimal-epub3.epub")) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val model = activity.readerModel

            val first = runBlocking { model.importFont(context.contentResolver, fontUri) }
            assertTrue(first.toString(), first is FontImportResult.Imported)
            val entry = (first as FontImportResult.Imported).entry

            assertEquals(FontImportResult.AlreadyImported(entry), runBlocking { model.importFont(context.contentResolver, fontUri) })
            assertEquals(
                FontImportResult.Rejected(FontInspection.Rejected.NotAFont),
                runBlocking { model.importFont(context.contentResolver, textUri) },
            )
            assertEquals(
                FontImportResult.Rejected(FontInspection.Rejected.Collection),
                runBlocking { model.importFont(context.contentResolver, collectionUri) },
            )
            assertEquals(FontImportResult.Failed, runBlocking { model.importFont(context.contentResolver, missingUri) })

            assertEquals(listOf(entry), activity.fontCatalog.value.fonts)
            assertEquals(setOf(entry.fileName, FontStore.INDEX_FILE_NAME), fontsDirectory.list()?.toSet())
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun thePanelListsImportedFontsAndSelectsThem() {
        val fontUri = stageBundledFont()
        val activity = instrumentation.startActivitySync(request("minimal-epub3.epub")) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val imported = runBlocking { activity.readerModel.importFont(context.contentResolver, fontUri) }
            val entry = (imported as FontImportResult.Imported).entry
            await("catalogued") { activity.fontCatalog.value.fonts == listOf(entry) }

            main { activity.showPreferences() }
            await("panel shown") { activity.preferencesSheet?.view != null }
            val sheet = requireNotNull(activity.preferencesSheet)
            val spinner = requireNotNull(sheet.view).findViewById<Spinner>(R.id.font_family)
            await("font listed") { spinner.adapter?.count == PreferencesSheet.FONT_FAMILIES.size + 1 }
            main {
                assertEquals(entry.displayName, spinner.adapter.getItem(spinner.adapter.count - 1))
                assertEquals(0, spinner.selectedItemPosition)
                assertTrue(sheet.requireView().findViewById<View>(R.id.manage_fonts_button).isEnabled)
                spinner.setSelection(spinner.adapter.count - 1)
            }
            await("preference set from the panel") { activity.readerModel.preferences.value.epub.fontFamily?.name == entry.family }
            await("navigator follows") { navigator(activity).settings.value.fontFamily?.name == entry.family }
            main { sheet.dismiss() }
            await("panel dismissed") { activity.preferencesSheet == null }
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    /** Readium ships this TTF in its assets; its family collides with a bundled name, which exercises the suffix rule. */
    private fun stageBundledFont(): Uri =
        stage("duospace.ttf") { context.assets.open("readium/readium-css/fonts/iAWriterDuospace-Regular.ttf").use { it.readBytes() } }

    private fun stage(name: String, bytes: () -> ByteArray): Uri {
        EpubReaderTestContentProvider.fileFor(context, name).writeBytes(bytes())
        return EpubReaderTestContentProvider.documentUri(name)
    }

    /**
     * Asks the page for the FontFace declared under [family], the user font variable and the
     * body's computed family. Runs from the instrumentation thread only: the evaluation is a
     * main-thread coroutine, so waiting for it on the main thread would deadlock.
     */
    private fun fontFaceState(activity: EpubReaderActivity, family: String): JSONObject? {
        check(Looper.myLooper() != Looper.getMainLooper())
        val script = """
            (function () {
                var wanted = ${JSONObject.quote(family.lowercase())};
                var face = null;
                document.fonts.forEach(function (candidate) {
                    var name = candidate.family.replace(/^["']|["']$/g, '').toLowerCase();
                    if (name === wanted) face = candidate;
                });
                return {
                    status: face ? face.status : null,
                    userFamily: getComputedStyle(document.documentElement).getPropertyValue('--USER__fontFamily'),
                    bodyFamily: getComputedStyle(document.body).fontFamily
                };
            })()
        """.trimIndent()
        val latch = CountDownLatch(1)
        var result: String? = null
        instrumentation.runOnMainSync {
            activity.lifecycleScope.launch {
                result = runCatching { navigator(activity).evaluateJavascript(script) }.getOrNull()
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        return result?.let { runCatching { JSONObject(it) }.getOrNull() }
    }

    private fun awaitFontFaceLoaded(activity: EpubReaderActivity, family: String): JSONObject {
        val deadline = SystemClock.uptimeMillis() + 20000
        var latest: JSONObject? = null
        while (SystemClock.uptimeMillis() < deadline) {
            latest = fontFaceState(activity, family)
            if (latest != null && !latest.isNull("status") && latest.optString("status") == "loaded") return latest
            SystemClock.sleep(250)
        }
        fail("Timed out: font face '$family' loaded, last state $latest")
        error("unreachable")
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

    private fun record(name: String, lines: List<String>) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name).writeText(lines.joinToString("\n") + "\n")
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
