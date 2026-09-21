package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.TocSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import kotlinx.coroutines.launch
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.preferences.ReadingProgression
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P2.3 device evidence: Japanese and Chinese samples with a right-to-left page
 * progression render vertically (Readium forces scrolling), the Arabic sample reads right to
 * left, the reading progression the tap zones mirror follows the publication, table-of-contents
 * jumps land, the `Text direction` preference overrides the automatic choice, and the interface
 * keeps its own layout direction. Screenshots land in `files/p2-evidence/` for the archive.
 *
 * The interface language comes from the host when AutoJs6 answers, but the test-only per-app
 * locale interleaves differently per API level: below 33 AppCompat wraps the context after the
 * host wrap (the per-app locale wins), from 33 the framework applies it underneath the host wrap
 * (the host wins). The Arabic-interface test expects the chrome direction accordingly.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderDirectionInstrumentationTest {

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
    fun japaneseBooksRenderVerticallyAndReadRightToLeft() {
        val activity = instrumentation.startActivitySync(request("vertical-ja.epub")) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val settings = navigator(activity).settings.value
            assertTrue(settings.toString(), settings.verticalText)
            assertTrue(settings.toString(), settings.scroll)
            assertEquals(ReadingProgression.RTL, settings.readingProgression)
            main { assertEquals(ReadingProgression.RTL, navigator(activity).overflow.value.readingProgression) }
            val page = awaitPage(activity) { it.optString("writingMode").startsWith("vertical") }
            SystemClock.sleep(1500)
            screenshot("direction-ja")

            jumpToChapter(activity, 3)
            main { navigator(activity).goBackward(animated = false) }
            await("back into chapter 2") { currentHref(activity)?.endsWith("chapter2.xhtml") == true }

            main { activity.editPreferences { it.copy(verticalText = false) } }
            await("forced horizontal reaches the navigator") {
                val resolved = navigator(activity).settings.value
                !resolved.verticalText && !resolved.scroll
            }
            SystemClock.sleep(1500)
            val horizontal = awaitPage(activity) { true }
            screenshot("direction-ja-horizontal")
            record(
                "direction-ja-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "auto=$page",
                    "settings=$settings",
                    "forcedHorizontal=$horizontal",
                ),
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun chineseBooksTurnVerticalByMetadataAndHorizontalOnRequest() {
        val activity = instrumentation.startActivitySync(request("vertical-zh.epub")) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val settings = navigator(activity).settings.value
            assertTrue(settings.toString(), settings.verticalText)
            assertEquals(ReadingProgression.RTL, settings.readingProgression)
            val vertical = awaitPage(activity) { it.optString("writingMode") == "vertical-rl" }
            SystemClock.sleep(1500)
            screenshot("direction-zh")

            main { activity.editPreferences { it.copy(verticalText = false) } }
            await("horizontal settings") { !navigator(activity).settings.value.verticalText }
            val horizontal = awaitPage(activity) { it.optString("writingMode") == "horizontal-tb" }

            main { activity.editPreferences { it.copy(verticalText = null) } }
            await("automatic again") { navigator(activity).settings.value.verticalText }
            awaitPage(activity) { it.optString("writingMode") == "vertical-rl" }
            record(
                "direction-zh-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "auto=$vertical",
                    "forcedHorizontal=$horizontal",
                ),
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun arabicBooksReadRightToLeftWhileTheInterfaceKeepsItsDirection() {
        val activity = instrumentation.startActivitySync(request("rtl-ar.epub")) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val settings = navigator(activity).settings.value
            assertFalse(settings.toString(), settings.verticalText)
            assertEquals(ReadingProgression.RTL, settings.readingProgression)
            main { assertEquals(ReadingProgression.RTL, navigator(activity).overflow.value.readingProgression) }
            val page = awaitPage(activity) { it.optString("direction") == "rtl" }
            // The interface direction comes from the interface locale, not from the book.
            val uiDirection = onMain { activity.resources.configuration.layoutDirection }
            main {
                assertEquals(uiDirection, activity.window.decorView.layoutDirection)
                assertEquals(uiDirection, activity.findViewById<View>(R.id.toolbar).layoutDirection)
            }
            SystemClock.sleep(1500)
            screenshot("direction-ar")

            jumpToChapter(activity, 3)
            jumpToChapter(activity, 1)
            record(
                "direction-ar-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "page=$page",
                    "settings=$settings",
                    "uiLayoutDirection=$uiDirection",
                ),
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun theHostLanguageDrivesTheInterfaceDirection() {
        val arabic = HostAppearance("ar", darkMode = false).wrap(context).resources.configuration
        assertEquals("ar", arabic.locales[0].language)
        assertEquals(View.LAYOUT_DIRECTION_RTL, arabic.layoutDirection)
        val english = HostAppearance("en", darkMode = false).wrap(context).resources.configuration
        assertEquals("en", english.locales[0].language)
        assertEquals(View.LAYOUT_DIRECTION_LTR, english.layoutDirection)
    }

    @Test
    fun anArabicInterfaceMirrorsTheChromeWithoutTurningTheBookAround() {
        main { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar")) }
        val host = HostAppearance.read(context)
        val hostWins = host != null && Build.VERSION.SDK_INT >= 33
        val expected = if (hostWins) TextUtils.getLayoutDirectionFromLocale(Locale.forLanguageTag(requireNotNull(host).languageTag))
        else View.LAYOUT_DIRECTION_RTL
        // On API 33+ the per-app locale is the framework's and reaches the running process asynchronously; an
        // activity started before that resolves its configuration with the old locale list. The API 36 AVD never
        // delivers it to this already running process (the same locale set from the shell renders the launcher
        // in Arabic), so the check is skipped there rather than failed.
        if (!hostWins && Build.VERSION.SDK_INT >= 33) {
            val deadline = SystemClock.uptimeMillis() + 10000
            while (SystemClock.uptimeMillis() < deadline && context.resources.configuration.locales[0].language != "ar") SystemClock.sleep(100)
            assumeTrue(
                "the per-app locale did not reach the running process on API ${Build.VERSION.SDK_INT}",
                context.resources.configuration.locales[0].language == "ar",
            )
        }
        val activity = instrumentation.startActivitySync(request("minimal-epub3.epub")) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val language = onMain { activity.resources.configuration.locales[0].toLanguageTag() }
            main {
                if (!hostWins) assertEquals("ar", activity.resources.configuration.locales[0].language)
                assertEquals(expected, activity.resources.configuration.layoutDirection)
                assertEquals(expected, activity.window.decorView.layoutDirection)
                assertEquals(expected, activity.findViewById<View>(R.id.toolbar).layoutDirection)
                assertEquals(ReadingProgression.LTR, navigator(activity).overflow.value.readingProgression)
            }
            val page = awaitPage(activity) { it.optString("direction") == "ltr" }
            SystemClock.sleep(1500)
            screenshot("direction-ui-ar")
            record(
                "direction-ui-ar-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "host=$host",
                    "hostWins=$hostWins",
                    "interfaceLanguage=$language",
                    "interfaceLayoutDirection=$expected",
                    "page=$page",
                    "toolbarTitle=${onMain { activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).title }}",
                ),
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
            main { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
            instrumentation.waitForIdleSync()
        }
    }

    private fun jumpToChapter(activity: EpubReaderActivity, number: Int) {
        val publication = requireNotNull(activity.readerModel.publication)
        val row = TocSheet.rows(publication).first { it.node.href.toString().endsWith("chapter$number.xhtml") }
        main { activity.jumpTo(row.node) }
        await("table of contents jump to chapter $number") { currentHref(activity)?.endsWith("chapter$number.xhtml") == true }
    }

    /** Writing mode and direction as the page computes them; runs from the instrumentation thread only. */
    private fun pageState(activity: EpubReaderActivity): JSONObject? {
        check(Looper.myLooper() != Looper.getMainLooper())
        val script = """
            (function () {
                var root = getComputedStyle(document.documentElement);
                return {
                    writingMode: root.writingMode,
                    direction: root.direction,
                    dir: document.documentElement.getAttribute('dir'),
                    lang: document.documentElement.getAttribute('xml:lang') || document.documentElement.lang,
                    textOrientation: getComputedStyle(document.body).textOrientation,
                    title: document.title
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

    private fun awaitPage(activity: EpubReaderActivity, condition: (JSONObject) -> Boolean): JSONObject {
        val deadline = SystemClock.uptimeMillis() + 20000
        var latest: JSONObject? = null
        while (SystemClock.uptimeMillis() < deadline) {
            latest = pageState(activity)
            if (latest != null && condition(latest)) return latest
            SystemClock.sleep(250)
        }
        fail("Timed out: page state, last $latest")
        error("unreachable")
    }

    private fun screenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        outputFile("$name-api${Build.VERSION.SDK_INT}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun record(name: String, lines: List<String>) {
        outputFile(name).writeText(lines.joinToString("\n") + "\n")
    }

    private fun outputFile(name: String): File =
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name)

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
