package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.PreferencesCodec
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ReaderTheme
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ReaderThemeColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.StoredPreferences
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMapping
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PreferencesSheet
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
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P2.1 device evidence: preference edits reach the navigator (resolved settings and the
 * Readium CSS `--USER__fontSize` variable), recolour the chrome, persist to
 * `reader-preferences.json` and come back after a relaunch; the panel's controls edit through the
 * same path; the pre-P2.1 scroll toggle migrates into the file.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderPreferencesInstrumentationTest {

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
    fun fontSizeAndThemeReachTheNavigatorAndSurviveARelaunch() {
        val request = request("minimal-epub3.epub")
        var activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            val hostTheme = ThemeMapping.resolve(ThemeMode.HOST, activity.hostDarkMode)
            main {
                assertEquals(1.0, navigator(activity).settings.value.fontSize, 0.0)
                assertEquals(hostTheme, activity.resolvedTheme)
                assertEquals(ReaderThemeColors.forTheme(hostTheme), activity.chromeColors)
            }
            assertNull(preferencesStore.read())

            main {
                activity.readerModel.editPreferences { it.copy(fontSize = 1.5) }
                activity.readerModel.setThemeMode(ThemeMode.SEPIA)
            }
            await("settings resolved") {
                val settings = navigator(activity).settings.value
                settings.fontSize == 1.5 && settings.theme == Theme.SEPIA
            }
            await("chrome recoloured") { activity.chromeColors == ReaderThemeColors.forTheme(ReaderTheme.SEPIA) }
            main { assertEquals(ReaderTheme.SEPIA, activity.resolvedTheme) }
            val fontSizeVariable = awaitFontSizeVariable(activity, "150")
            await("preferences written") { stored()?.let { it.themeMode == ThemeMode.SEPIA && it.readium.optDouble("fontSize") == 1.5 } == true }
            record(
                "preferences-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "fontSizeVariable=$fontSizeVariable",
                    "chrome=${activity.chromeColors}",
                    "file=${preferencesStore.read()?.let(PreferencesCodec::encode)?.replace('\n', ' ')}",
                ),
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }

        activity = instrumentation.startActivitySync(request) as EpubReaderActivity
        try {
            await("relaunched with the stored preferences") {
                val settings = navigator(activity).settings.value
                activity.navigatorReady && settings.fontSize == 1.5 && settings.theme == Theme.SEPIA
            }
            main { assertEquals(ReaderThemeColors.forTheme(ReaderTheme.SEPIA), activity.chromeColors) }

            main { activity.readerModel.resetPreferences() }
            val hostTheme = ThemeMapping.resolve(ThemeMode.HOST, activity.hostDarkMode)
            await("defaults restored") {
                val settings = navigator(activity).settings.value
                settings.fontSize == 1.0 && settings.theme == hostTheme.toReadium()
            }
            await("defaults written") { stored()?.isDefault == true }
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun thePanelControlsEditThroughTheSamePath() {
        val activity = instrumentation.startActivitySync(request("minimal-epub3.epub")) as EpubReaderActivity
        try {
            await("chapter 1") { activity.navigatorReady && currentHref(activity)?.endsWith("chapter1.xhtml") == true }
            main { activity.showPreferences() }
            await("panel shown") { activity.preferencesSheet?.view != null }
            val sheet = requireNotNull(activity.preferencesSheet)
            fun control(id: Int): View = requireNotNull(sheet.view).findViewById(id)

            main { control(R.id.font_size_increase).performClick() }
            await("one step larger") { navigator(activity).settings.value.fontSize == 1.1 }
            main { control(R.id.font_size_decrease).performClick() }
            await("back to 100 %") { navigator(activity).settings.value.fontSize == 1.0 }

            main { control(R.id.theme_dark).performClick() }
            await("dark theme") { navigator(activity).settings.value.theme == Theme.DARK && activity.resolvedTheme == ReaderTheme.DARK }
            main { assertEquals(ReaderThemeColors.forTheme(ReaderTheme.DARK), activity.chromeColors) }

            main { control(R.id.overflow_scrolled).performClick() }
            await("scrolled") { navigator(activity).settings.value.scroll }
            main { assertTrue(activity.readerModel.preferences.value.epub.scroll == true) }

            main { control(R.id.text_align_justify).performClick() }
            await("justified with publisher styles off") {
                val settings = navigator(activity).settings.value
                settings.textAlign == TextAlign.JUSTIFY && !settings.publisherStyles
            }

            main { control(R.id.reset_button).performClick() }
            await("reset") {
                val settings = navigator(activity).settings.value
                settings.fontSize == 1.0 && !settings.scroll && settings.publisherStyles && settings.textAlign == null &&
                    activity.resolvedTheme == ThemeMapping.resolve(ThemeMode.HOST, activity.hostDarkMode)
            }
            await("defaults persisted", detail = { "file=${stored()?.let(PreferencesCodec::encode)} state=${activity.readerModel.preferences.value}" }) {
                stored()?.isDefault == true
            }

            main { sheet.dismiss() }
            await("panel dismissed") { activity.preferencesSheet == null }
            assertEquals("reader-preferences", PreferencesSheet.TAG)
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun theLegacyScrollModeToggleMigratesIntoThePreferencesFile() {
        ReaderSettings(context).scrollMode = true
        assertFalse(preferencesStore.exists())

        val activity = instrumentation.startActivitySync(request("minimal-epub3.epub")) as EpubReaderActivity
        try {
            await("scrolled from the legacy toggle") { activity.navigatorReady && navigator(activity).settings.value.scroll }
            await("migrated to the file") { stored()?.readium?.optBoolean("scroll") == true }
            main { activity.setScrollMode(false) }
            await("paginated again") { !navigator(activity).settings.value.scroll }
            await("file updated") { stored()?.readium?.optBoolean("scroll", true) == false }
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    private fun stored(): StoredPreferences? = preferencesStore.read()

    /**
     * Readium CSS keeps `--USER__fontSize` on the root element. Runs from the instrumentation thread
     * only: the evaluation is a main-thread coroutine, so waiting for it on the main thread would deadlock.
     */
    private fun fontSizeVariable(activity: EpubReaderActivity): String? {
        check(Looper.myLooper() != Looper.getMainLooper())
        val latch = CountDownLatch(1)
        var result: String? = null
        instrumentation.runOnMainSync {
            activity.lifecycleScope.launch {
                result = runCatching {
                    navigator(activity).evaluateJavascript(
                        "getComputedStyle(document.documentElement).getPropertyValue('--USER__fontSize')",
                    )
                }.getOrNull()
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        return result
    }

    /** Polls [fontSizeVariable] from the instrumentation thread until it reports [expected]. */
    private fun awaitFontSizeVariable(activity: EpubReaderActivity, expected: String): String {
        val deadline = SystemClock.uptimeMillis() + 20000
        var latest: String? = null
        while (SystemClock.uptimeMillis() < deadline) {
            latest = fontSizeVariable(activity)
            if (latest?.contains(expected) == true) return latest
            SystemClock.sleep(200)
        }
        fail("Timed out: Readium CSS font size variable, last value $latest")
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

    private fun await(message: String, detail: () -> String? = { null }, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 60000 // a cold open takes longer than 20 s on the GitHub-hosted API 35 emulator
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = runCatching(condition).getOrDefault(false) }
            if (ready) return
            SystemClock.sleep(100)
        }
        fail("Timed out: $message" + (runCatching(detail).getOrNull()?.let { " ($it)" } ?: ""))
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
