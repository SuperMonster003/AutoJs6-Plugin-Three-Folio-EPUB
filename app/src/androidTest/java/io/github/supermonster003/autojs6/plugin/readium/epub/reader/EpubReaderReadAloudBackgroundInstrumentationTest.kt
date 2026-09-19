package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.Manifest
import android.app.NotificationManager
import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.getSystemService
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.SleepTimer
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsEvent
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsForegroundService
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsSession
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsStatus
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File

/**
 * Roadmap P3 stage 2 / D26 device evidence: the sleep timer stops the voice after its span or at
 * the end of the chapter, the keep-screen-on switch holds the window flag only while a voice reads,
 * and with "continue in the background" on the voice outlives the reader, keeps its notification,
 * is adopted by a reader reopening the same book (which opens at the spoken sentence) and by the
 * reader the notification launches; with the switch off (the default) closing the reader stops it.
 * Notes land in `files/p2-evidence/tts-background-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderReadAloudBackgroundInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)
    private val preferencesStore get() = ReaderPreferencesStore.forFilesDirectory(context.filesDir)
    private val ttsPreferencesStore get() = TtsPreferencesStore.forFilesDirectory(context.filesDir)
    private val settings get() = ReaderSettings(context)
    private val notes = ArrayList<String>()

    @Before
    fun resetState() {
        cleanUp()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        notes += "api=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}"
    }

    @After
    fun cleanUp() {
        TtsForegroundService.stopParked()
        booksDirectory.deleteRecursively()
        preferencesStore.clear()
        ttsPreferencesStore.clear()
        context.getSharedPreferences(ReaderSettings.PREFERENCES_NAME, 0).edit().clear().commit()
    }

    @Test
    fun sleepTimerStopsAfterItsSpanAndAtTheEndOfTheChapter() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            main { activity.updateTtsPreferences { it.copy(speed = 0.7) } }
            main { activity.startReadAloud() }
            awaitSpeaking(activity)

            // A fixed span (shortened to 4 s here) ends read-aloud and reports the timer.
            main { activity.setSleepTimer(SleepTimer.MINUTES_15, durationMillis = 4000L) }
            assertEquals(SleepTimer.MINUTES_15, onMain { activity.ttsSleepTimer.value })
            val armed = SystemClock.uptimeMillis()
            await("stopped by the sleep timer", 20000) { activity.ttsStatus.value == TtsStatus.IDLE }
            notes += "fixed-span-stopped-after=${SystemClock.uptimeMillis() - armed}ms event=${onMain { activity.lastTtsEvent }}"
            assertEquals(TtsEvent.SleepTimerEnded, onMain { activity.lastTtsEvent })
            await("service stopped") { !TtsForegroundService.running }

            // The end of the chapter: skipping into chapter 2 ends read-aloud right there (the timer
            // event of the first run is still the last event, so the speaking wait ignores it).
            main { activity.startReadAloud() }
            awaitSpeaking(activity, stale = TtsEvent.SleepTimerEnded)
            main { activity.setSleepTimer(SleepTimer.END_OF_CHAPTER) }
            var skips = 0
            while (onMain { activity.ttsStatus.value } == TtsStatus.PLAYING && skips < 40) {
                main { activity.readAloudNext() }
                skips++
                SystemClock.sleep(300)
            }
            await("stopped at the end of the chapter", 20000) { activity.ttsStatus.value == TtsStatus.IDLE }
            notes += "end-of-chapter-skips=$skips event=${onMain { activity.lastTtsEvent }}"
            assertEquals(TtsEvent.SleepTimerEnded, onMain { activity.lastTtsEvent })
            record("tts-sleep-timer-api${Build.VERSION.SDK_INT}.txt", notes)
        } finally {
            finish(activity)
        }
    }

    @Test
    fun keepScreenOnHoldsTheWindowFlagOnlyWhileReading() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            assertFalse(onMain { activity.keepsScreenOn })
            main { activity.setReadAloudKeepScreenOn(true) }
            assertFalse("idle: no flag even with the switch on", onMain { activity.keepsScreenOn })
            main { activity.startReadAloud() }
            awaitSpeaking(activity)
            await("flag while reading") { activity.keepsScreenOn }
            main { activity.setReadAloudKeepScreenOn(false) }
            assertFalse("switch off clears the flag at once", onMain { activity.keepsScreenOn })
            main { activity.setReadAloudKeepScreenOn(true) }
            assertTrue(onMain { activity.keepsScreenOn })
            assertTrue(settings.readAloudKeepScreenOn)
            main { activity.stopReadAloud() }
            await("idle") { activity.ttsStatus.value == TtsStatus.IDLE }
            assertFalse("stopped: flag cleared", onMain { activity.keepsScreenOn })
            notes += "keep-screen-on=flag-follows-reading"
            record("tts-keep-screen-on-api${Build.VERSION.SDK_INT}.txt", notes)
        } finally {
            finish(activity)
        }
    }

    @Test
    fun backgroundReadAloudOutlivesTheReaderAndComesBackOnReopen() {
        settings.readAloudInBackground = true
        val first = instrumentation.startActivitySync(request(MANY)) as EpubReaderActivity
        awaitHref(first, "chapter1.xhtml")
        SystemClock.sleep(500)
        main { first.startReadAloud() }
        awaitSpeaking(first)
        val session = onMain { first.ttsSession }!!
        finish(first)
        await("reader destroyed") { first.isDestroyed }

        // The voice, its notification and the service survive the reader; the sentence keeps advancing.
        await("session parked") { TtsForegroundService.parked === session }
        assertTrue(TtsForegroundService.running)
        await("notification kept") { hasNotification() }
        val before = session.location.value?.utterance
        await("voice advancing in the background", 60000) { session.location.value?.utterance != before }
        assertEquals(TtsStatus.PLAYING, session.status.value)
        notes += "parked=true advancing=true href=${session.location.value?.href}"

        // Reopening the same book adopts the voice and opens where it speaks.
        val second = instrumentation.startActivitySync(request(MANY)) as EpubReaderActivity
        try {
            await("adopted") { second.ttsSession === session }
            assertNull(TtsForegroundService.parked)
            await("reader ready") { second.navigatorReady }
            val spoken = onMain { second.ttsLocation.value?.href }
            await("reader opened at the spoken sentence") { currentHref(second) == spoken }
            assertEquals(TtsStatus.PLAYING, onMain { second.ttsStatus.value })
            assertTrue(onMain { second.chromeShowsReadAloud })
            notes += "adopted=true reader-href=${currentHref(second)} spoken-href=$spoken"
            main { second.stopReadAloud() }
            await("idle") { second.ttsStatus.value == TtsStatus.IDLE }
            await("service stopped") { !TtsForegroundService.running }
            record("tts-background-api${Build.VERSION.SDK_INT}.txt", notes)
        } finally {
            finish(second)
        }
    }

    @Test
    fun theNotificationReopensTheReaderOnTheParkedBook() {
        settings.readAloudInBackground = true
        val first = instrumentation.startActivitySync(request(MANY)) as EpubReaderActivity
        awaitHref(first, "chapter1.xhtml")
        SystemClock.sleep(500)
        main { first.startReadAloud() }
        awaitSpeaking(first)
        val session = onMain { first.ttsSession }!!
        val publication = onMain { first.readerModel.publication }
        finish(first)
        await("session parked") { TtsForegroundService.parked === session }

        // What the notification's tap sends: the resume action, no document at all.
        val resumed = instrumentation.startActivitySync(resumeIntent()) as EpubReaderActivity
        try {
            await("adopted") { resumed.ttsSession === session }
            assertSame("the parked book is shown again", publication, onMain { resumed.readerModel.publication })
            await("reader ready") { resumed.navigatorReady }
            val spoken = onMain { resumed.ttsLocation.value?.href }
            await("reader opened at the spoken sentence") { currentHref(resumed) == spoken }
            assertEquals(TtsStatus.PLAYING, onMain { resumed.ttsStatus.value })
            notes += "resumed-from-notification=true reader-href=${currentHref(resumed)}"

            // Closing this reader with the switch on parks the voice again; stopping from the notification ends it.
            finish(resumed)
            await("parked again") { TtsForegroundService.parked === session }
            TtsForegroundService.stopParked()
            await("service stopped") { !TtsForegroundService.running }
            await("notification removed") { !hasNotification() }
            assertTrue(session.closed)
            notes += "stop-parked=released"
        } finally {
            if (!resumed.isFinishing && !resumed.isDestroyed) finish(resumed)
        }

        // With nothing parked, the resume action just closes.
        val empty = instrumentation.startActivitySync(resumeIntent()) as EpubReaderActivity
        await("nothing to resume: reader closes") { empty.isFinishing || empty.isDestroyed }
        notes += "resume-without-session=closes"
        record("tts-notification-resume-api${Build.VERSION.SDK_INT}.txt", notes)
    }

    @Test
    fun withTheSwitchOffClosingTheReaderStopsTheVoice() {
        assertFalse(settings.readAloudInBackground)
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        awaitHref(activity, "chapter1.xhtml")
        SystemClock.sleep(500)
        main { activity.startReadAloud() }
        awaitSpeaking(activity)
        val session = onMain { activity.ttsSession }!!
        finish(activity)
        await("service stopped with the reader") { !TtsForegroundService.running }
        assertNull(TtsForegroundService.parked)
        assertTrue(session.closed)
        await("notification removed") { !hasNotification() }
    }

    // ---- helpers ----

    private fun awaitSpeaking(activity: EpubReaderActivity, stale: TtsEvent? = null) {
        val deadline = SystemClock.uptimeMillis() + 60000
        while (SystemClock.uptimeMillis() < deadline) {
            val status = onMain { activity.ttsStatus.value }
            val event = onMain { activity.lastTtsEvent }
            if (status == TtsStatus.PLAYING && onMain { activity.ttsLocation.value } != null) return
            if (event != null && event !== stale && status == TtsStatus.IDLE) {
                assumeTrue("read-aloud unavailable on this device: $event", false)
            }
            SystemClock.sleep(100)
        }
        fail("Timed out waiting for read-aloud to speak (status ${onMain { activity.ttsStatus.value }})")
    }

    private fun hasNotification(): Boolean =
        context.getSystemService<NotificationManager>()!!.activeNotifications.any { it.id == TtsForegroundService.NOTIFICATION_ID }

    private fun record(name: String, lines: List<String>) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name).writeText(lines.joinToString("\n") + "\n")
    }

    private fun finish(activity: EpubReaderActivity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
    }

    private fun resumeIntent(): Intent =
        Intent(context, EpubReaderActivity::class.java)
            .setAction(EpubReaderActivity.ACTION_RESUME_READ_ALOUD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

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
        const val MANY = "malformed-many-entries.epub"
    }
}
