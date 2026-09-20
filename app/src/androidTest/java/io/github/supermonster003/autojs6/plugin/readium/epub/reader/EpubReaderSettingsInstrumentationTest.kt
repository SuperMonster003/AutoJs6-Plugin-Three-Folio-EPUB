package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.app.Activity
import android.app.Instrumentation
import android.content.DialogInterface
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
import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBook
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBooksStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.StoredPreferences
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ProgressRecord
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.TapZones
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.SleepTimer
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.update.AppUpdateCoordinator
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.update.ReleaseInfo
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.update.ReleaseInfoCodec
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.update.UpdateFailure
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.update.UpdateFetchResult
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.update.UpdateSource
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P4.3 device evidence: the settings page persists the toggles and choices where the
 * reader reads them, the release history opens with the bundled changelog, the data rows empty
 * the stores, and the manual update check (D28) shows a newer release from a substituted source,
 * remembers "ignore", reuses today's answer instead of fetching again, and leaves the stored
 * state alone on failure. Notes land in `files/p2-evidence/settings-api<N>.txt`.
 */
@OptIn(ExperimentalReadiumApi::class)
@RunWith(AndroidJUnit4::class)
class EpubReaderSettingsInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val settings get() = ReaderSettings(context)
    private val preferencesStore get() = ReaderPreferencesStore.forFilesDirectory(context.filesDir)
    private val ttsStore get() = TtsPreferencesStore.forFilesDirectory(context.filesDir)
    private val recentBooksStore get() = RecentBooksStore.forFilesDirectory(context.filesDir)
    private val bookDataStore get() = BookDataStore.forFilesDirectory(context.filesDir)
    private val notes = ArrayList<String>()
    private var historyMonitor: Instrumentation.ActivityMonitor? = null

    @Before
    fun resetState() {
        cleanUp()
        notes += "api=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}"
    }

    @After
    fun cleanUp() {
        historyMonitor?.let { instrumentation.removeMonitor(it) }
        historyMonitor = null
        AppUpdateCoordinator.sourceOverride = null
        settings.clearAll()
        preferencesStore.clear()
        ttsStore.clear()
        recentBooksStore.clear()
        bookDataStore.clearAll()
    }

    @Test
    fun togglesAndChoicesLandWhereTheReaderReadsThem() {
        val page = startSettings()
        val volumeBefore = settings.volumeKeysTurnPages
        click(page, R.string.text_volume_keys_turn_pages)
        await("volume keys flipped") { settings.volumeKeysTurnPages != volumeBefore }
        val screenBefore = settings.readAloudKeepScreenOn
        click(page, R.string.text_read_aloud_keep_screen_on)
        await("keep screen on flipped") { settings.readAloudKeepScreenOn != screenBefore }
        val linksBefore = settings.externalLinksDirect
        click(page, R.string.text_external_links_direct)
        await("external links flipped") { settings.externalLinksDirect != linksBefore }

        main { page.setTapZones(TapZones.VERTICAL) }
        assertEquals(TapZones.VERTICAL, settings.tapZones)
        main { page.setDefaultSleepTimer(SleepTimer.MINUTES_30) }
        assertEquals(SleepTimer.MINUTES_30, settings.readAloudSleepTimer)
        main { page.setThemeMode(ThemeMode.SEPIA) }
        await("theme mode stored") { preferencesStore.read()?.themeMode == ThemeMode.SEPIA }
        main { page.setReadAloudSpeed(150) }
        await("speed stored") { ttsStore.read()?.speed == 1.5 }
        main { page.setReadAloudPitch(80) }
        await("pitch stored") { ttsStore.read()?.pitch == 0.8 }
        assertEquals(1.5, ttsStore.read()?.speed)
        instrumentation.waitForIdleSync()
        screenshot(page.window, "settings")

        notes += "toggles: volume ${settings.volumeKeysTurnPages} screen ${settings.readAloudKeepScreenOn} links ${settings.externalLinksDirect}"
        notes += "choices: tapZones=${settings.tapZones} sleep=${settings.readAloudSleepTimer} theme=${preferencesStore.read()?.themeMode} speed=${ttsStore.read()?.speed} pitch=${ttsStore.read()?.pitch}"
        record("settings-api${Build.VERSION.SDK_INT}.txt", notes)
        finish(page)
    }

    @Test
    fun theReleaseHistoryOpensWithTheBundledChangelog() {
        historyMonitor = instrumentation.addMonitor(ReleaseHistoryActivity::class.java.name, null, false)
        val page = startSettings()
        click(page, R.string.release_history)
        val history = historyMonitor!!.waitForActivityWithTimeout(30_000) as? ReleaseHistoryActivity
            ?: failWith("The release history did not open")
        val unavailable = context.getString(R.string.release_history_unavailable)
        await("history rendered") { historyText(history).length > 100 }
        val text = historyText(history)
        assertNotEquals(unavailable, text)
        assertTrue(text, text.contains("1.0.0"))

        notes += "history: ${text.length} chars, first line '${text.lineSequence().first()}'"
        record("settings-history-api${Build.VERSION.SDK_INT}.txt", notes)
        finish(history)
        finish(page)
    }

    @Test
    fun theDataRowsEmptyTheStores() {
        recentBooksStore.upsert(RecentBook("content://example/1", "One", addedAt = 1L, lastReadAt = 1L))
        bookDataStore.writeProgress("1".padStart(64, '0'), ProgressRecord(JSONObject().put("href", "/a.xhtml"), 1L))
        preferencesStore.write(StoredPreferences(ThemeMode.DARK, JSONObject()))
        settings.externalLinksDirect = true
        settings.ignoredUpdateVersion = "v9.9.9"
        assertEquals(1, recentBooksStore.read().size)
        assertEquals(1, bookDataStore.bookKeys().size)

        val page = startSettings()
        main { page.clearProgress() }
        await("progress cleared") { bookDataStore.bookKeys().isEmpty() }
        main { page.clearRecentBooks() }
        await("recent cleared") { recentBooksStore.read().isEmpty() }
        main { page.clearFonts() }
        main { page.clearPreferences() }
        await("preferences cleared") { !preferencesStore.exists() && !settings.externalLinksDirect && settings.ignoredUpdateVersion == null }

        notes += "data: books=${bookDataStore.bookKeys().size} recent=${recentBooksStore.read().size} preferencesFile=${preferencesStore.exists()} externalLinksDirect=${settings.externalLinksDirect}"
        record("settings-data-api${Build.VERSION.SDK_INT}.txt", notes)
        finish(page)
    }

    @Test
    fun theUpdateCheckShowsANewerReleaseRemembersIgnoreAndReusesTodaysAnswer() {
        val release = ReleaseInfo(
            tagName = "v9.9.9",
            name = "Nine",
            htmlUrl = "https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases/tag/v9.9.9",
            publishedAt = "2026-09-20T00:00:00Z",
            preRelease = false,
            notes = "* `Feature` Something new",
        )
        var fetches = 0
        AppUpdateCoordinator.sourceOverride = UpdateSource { fetches++; UpdateFetchResult.Success(release) }
        val page = startSettings()

        main { page.checkForUpdates() }
        await("release dialog") { page.updates.dialog?.isShowing == true }
        assertEquals(1, fetches)
        assertEquals("v9.9.9", ReleaseInfoCodec.decode(settings.cachedRelease)?.tagName)
        assertNotNull(settings.lastUpdateCheckAt)
        val ignoreLabel = onMain { page.updates.dialog!!.getButton(DialogInterface.BUTTON_NEGATIVE).text.toString() }
        assertEquals(context.getString(R.string.text_update_ignore), ignoreLabel)
        main { page.updates.dialog!!.getButton(DialogInterface.BUTTON_NEGATIVE).performClick() }
        await("ignored") { settings.ignoredUpdateVersion == "v9.9.9" && page.updates.dialog == null }

        // Within the day the cached answer is shown again, no fetch, and the button undoes the ignore.
        main { page.checkForUpdates() }
        await("dialog again") { page.updates.dialog?.isShowing == true }
        assertEquals(1, fetches)
        val unignoreLabel = onMain { page.updates.dialog!!.getButton(DialogInterface.BUTTON_NEGATIVE).text.toString() }
        assertEquals(context.getString(R.string.text_update_unignore), unignoreLabel)
        main { page.updates.dialog!!.getButton(DialogInterface.BUTTON_NEGATIVE).performClick() }
        await("unignored") { settings.ignoredUpdateVersion == null && page.updates.dialog == null }

        // A failure leaves the stored state alone and shows no dialog.
        settings.lastUpdateCheckAt = null
        settings.cachedRelease = null
        AppUpdateCoordinator.sourceOverride = UpdateSource { fetches++; UpdateFetchResult.Failure(UpdateFailure.NETWORK) }
        main { page.checkForUpdates() }
        await("failure handled") { fetches == 2 && !page.updates.isChecking }
        assertNull(page.updates.dialog)
        assertNull(settings.lastUpdateCheckAt)
        assertNull(settings.cachedRelease)

        // An older release means "up to date": stored, but no dialog.
        AppUpdateCoordinator.sourceOverride = UpdateSource { fetches++; UpdateFetchResult.Success(release.copy(tagName = "v0.0.1")) }
        main { page.checkForUpdates() }
        await("up to date handled") { fetches == 3 && !page.updates.isChecking }
        assertNull(page.updates.dialog)
        assertEquals("v0.0.1", ReleaseInfoCodec.decode(settings.cachedRelease)?.tagName)
        assertNotNull(settings.lastUpdateCheckAt)

        notes += "update: fake v9.9.9 shown once, ignored then unignored through the dialog, second check reused the cache (fetches=$fetches after failure and v0.0.1)"
        record("settings-update-api${Build.VERSION.SDK_INT}.txt", notes)
        finish(page)
    }

    // ---- helpers ----

    private fun startSettings(): SettingsActivity =
        instrumentation.startActivitySync(
            Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as SettingsActivity

    private fun click(page: SettingsActivity, @StringRes title: Int) {
        main { row(page, title).performClick() }
        instrumentation.waitForIdleSync()
    }

    private fun row(page: SettingsActivity, @StringRes title: Int): View {
        val content = page.findViewById<ViewGroup>(R.id.content)
        return (0 until content.childCount).map(content::getChildAt).firstOrNull { it.tag == title }
            ?: failWith("No row titled ${context.resources.getResourceEntryName(title)}")
    }

    private fun historyText(history: ReleaseHistoryActivity): String = onMain {
        history.findViewById<TextView>(R.id.history).text.toString()
    }

    private fun finish(activity: Activity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
    }

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
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name)

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

    private fun <T> failWith(message: String): T {
        fail(message)
        throw AssertionError(message)
    }

    private fun main(action: () -> Unit) = onMain(action)

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }
}
