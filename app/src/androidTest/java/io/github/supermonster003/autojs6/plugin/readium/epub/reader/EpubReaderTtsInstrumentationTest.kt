package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.SystemTtsEngine
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsEvent
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsForegroundService
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsStatus
import kotlinx.coroutines.launch
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P3 device evidence for read-aloud: the system engine speaks the English fixture from the
 * current page, the spoken sentence is highlighted (decoration group `tts`) and advances, the bar
 * and the media notification appear, previous / next / pause / play work, the page follows the
 * voice into the next chapter, a manual jump stops the voice, and closing the reader stops the
 * service; the voice keeps going for three minutes with the screen off, another app taking the
 * audio focus pauses it and play resumes it, and the notification's own actions control it.
 * Devices without an engine or without English voice data record that and skip the speaking
 * cases. Notes land in `files/p2-evidence/tts-*.txt`.
 *
 * The screen-off case really sleeps the device only when its lock screen is not secure (a PIN,
 * pattern or fingerprint lock could not be dismissed again and every later test would fail);
 * otherwise the reader is sent behind the launcher with the screen on. Runner argument
 * `screenOff=force` sleeps a secure device anyway (unlock it by hand afterwards).
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderTtsInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)
    private val preferencesStore get() = ReaderPreferencesStore.forFilesDirectory(context.filesDir)
    private val ttsPreferencesStore get() = TtsPreferencesStore.forFilesDirectory(context.filesDir)
    private val notes = ArrayList<String>()

    @Before
    fun resetState() {
        cleanUp()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        notes += "api=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}"
        notes += "engine=${Settings.Secure.getString(context.contentResolver, "tts_default_synth")}"
    }

    @After
    fun cleanUp() {
        booksDirectory.deleteRecursively()
        preferencesStore.clear()
        ttsPreferencesStore.clear()
        context.getSharedPreferences(ReaderSettings.PREFERENCES_NAME, 0).edit().clear().commit()
    }

    @Test
    fun readAloudSpeaksHighlightsAndControls() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            // A fast voice keeps the test short; the preference also proves the store round trip.
            main { activity.updateTtsPreferences { it.copy(speed = 2.0) } }

            val started = SystemClock.uptimeMillis()
            main { activity.startReadAloud() }
            awaitSpeaking(activity)
            notes += "start-to-playing=${SystemClock.uptimeMillis() - started}ms"
            val session = onMain { activity.ttsSession }
            assertNotNull(session)
            notes += "bound-engine=${SystemTtsEngine.boundEngine}"
            notes += "voices=${session!!.voices.size} languages=${session.voices.map { it.language }.distinct().sorted()}"
            notes += "resolved-language=${session.settings.value.language.code} speed=${session.settings.value.speed}"

            // The spoken sentence is on the first chapter, highlighted, and advances on its own.
            val first = onMain { activity.ttsLocation.value }
            assertNotNull(first)
            assertTrue(first!!.href, first.href.endsWith("chapter1.xhtml"))
            assertEquals(first.locator, onMain { activity.ttsHighlighted })
            // One highlight, drawn as one box per line the sentence spans.
            awaitDecorations(activity, "highlight decoration in the page") { it >= 1 }
            await("the next sentence", 60000) { activity.ttsLocation.value?.utterance != first.utterance }
            notes += "first-utterance=${first.utterance.take(60)}"
            notes += "second-utterance=${onMain { activity.ttsLocation.value?.utterance }?.take(60)}"

            // Pause / play through the bar's actions.
            main { activity.readAloudTogglePlayPause() }
            await("paused") { activity.ttsStatus.value == TtsStatus.PAUSED }
            val paused = onMain { activity.ttsLocation.value?.utterance }
            SystemClock.sleep(1500)
            assertEquals(paused, onMain { activity.ttsLocation.value?.utterance })
            main { activity.readAloudTogglePlayPause() }
            await("playing again") { activity.ttsStatus.value == TtsStatus.PLAYING }

            // Previous / next move by one sentence.
            main { activity.readAloudNext() }
            await("next sentence") { activity.ttsLocation.value?.utterance != paused }
            val afterNext = onMain { activity.ttsLocation.value?.utterance }
            main { activity.readAloudPrevious() }
            await("previous sentence") { activity.ttsLocation.value?.utterance != afterNext }

            // The foreground service and its media notification exist while the voice reads.
            assertTrue(TtsForegroundService.running)
            if (notificationsEnabled()) {
                await("media notification") { hasNotification() }
                notes += "notification=shown"
            } else {
                notes += "notification=not-permitted"
            }
            assertTrue(onMain { activity.chromeShowsReadAloud })

            // Stop: idle, highlight gone, notification gone, service stopped.
            main { activity.stopReadAloud() }
            await("idle") { activity.ttsStatus.value == TtsStatus.IDLE }
            assertNull(onMain { activity.ttsHighlighted })
            awaitDecorations(activity, "decoration removed") { it == 0 }
            await("notification removed") { !hasNotification() }
            await("service stopped") { !TtsForegroundService.running }
            assertFalse(onMain { activity.chromeShowsReadAloud })
            assertEquals(2.0, ttsPreferencesStore.read()?.speed)
            record("tts-api${Build.VERSION.SDK_INT}.txt", notes)
        } finally {
            finish(activity)
        }
    }

    @Test
    fun readAloudFollowsIntoTheNextChapterAndStopsOnManualNavigation() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            main { activity.updateTtsPreferences { it.copy(speed = 2.0) } }
            main { activity.startReadAloud() }
            awaitSpeaking(activity)

            // Skip sentence by sentence until the voice enters chapter 2; the page must follow.
            var skips = 0
            while (onMain { activity.ttsLocation.value?.href }?.endsWith("chapter2.xhtml") != true && skips < 40) {
                main { activity.readAloudNext() }
                skips++
                SystemClock.sleep(300)
            }
            await("voice in chapter 2") { activity.ttsLocation.value?.href?.endsWith("chapter2.xhtml") == true }
            val followStarted = SystemClock.uptimeMillis()
            await("page followed the voice") { currentHref(activity)?.endsWith("chapter2.xhtml") == true }
            notes += "skips-to-chapter2=$skips follow-latency=${SystemClock.uptimeMillis() - followStarted}ms"
            assertEquals(TtsStatus.PLAYING, onMain { activity.ttsStatus.value })

            // A manual jump (table of contents, bookmark, link) ends read-aloud.
            val target = onMain { activity.readerModel.publication!!.readingOrder.first() }
            main { activity.jumpTo(target) }
            await("read-aloud stopped by the jump") { activity.ttsStatus.value == TtsStatus.IDLE }
            await("service stopped") { !TtsForegroundService.running }
            awaitHref(activity, "chapter1.xhtml")
            record("tts-follow-api${Build.VERSION.SDK_INT}.txt", notes)
        } finally {
            finish(activity)
        }
    }

    @Test
    fun closingTheReaderStopsReadAloud() {
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        var speaking = false
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            main { activity.startReadAloud() }
            awaitSpeaking(activity)
            speaking = true
            assertTrue(TtsForegroundService.running)
        } finally {
            finish(activity)
        }
        if (speaking) {
            await("service stopped with the reader") { !TtsForegroundService.running }
            await("notification removed") { !hasNotification() }
            notes += "closing-stopped-service=true"
            record("tts-close-api${Build.VERSION.SDK_INT}.txt", notes)
        }
    }

    @Test
    fun readAloudContinuesWithTheScreenOff() {
        val activity = instrumentation.startActivitySync(request(MANY)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            main { activity.startReadAloud() }
            awaitSpeaking(activity)
            notes += "bound-engine=${SystemTtsEngine.boundEngine}"
            val sleeps = screenOffForced() || !context.getSystemService<KeyguardManager>()!!.isDeviceSecure
            if (sleeps) {
                shell("input keyevent KEYCODE_SLEEP")
                notes += "mode=screen-off"
            } else {
                shell("input keyevent KEYCODE_HOME")
                notes += "mode=background-screen-on (secure lock screen; pass screenOff=force to sleep the device)"
            }
            SystemClock.sleep(3000)
            notes += "wakefulness-after-sleep=${wakefulness()}"

            // Three minutes with the screen off (or behind the launcher): the sentence must keep advancing every minute.
            var previous = onMain { activity.ttsLocation.value?.utterance }
            var advances = 0
            val started = SystemClock.uptimeMillis()
            for (minute in 1..3) {
                val minuteStart = SystemClock.uptimeMillis()
                var advancedThisMinute = 0
                while (SystemClock.uptimeMillis() - minuteStart < 60000) {
                    SystemClock.sleep(2000)
                    val current = onMain { activity.ttsLocation.value?.utterance }
                    if (current != previous) {
                        advancedThisMinute++
                        previous = current
                    }
                }
                advances += advancedThisMinute
                notes += "minute-$minute advanced=$advancedThisMinute status=${onMain { activity.ttsStatus.value }} wakefulness=${wakefulness()} href=${onMain { activity.ttsLocation.value?.href }}"
                assertTrue("no sentence advanced in minute $minute", advancedThisMinute > 0)
            }
            notes += "screen-off-total=${SystemClock.uptimeMillis() - started}ms advances=$advances"
            assertEquals(TtsStatus.PLAYING, onMain { activity.ttsStatus.value })
            assertTrue(TtsForegroundService.running)

            wakeAndUnlock()
            notes += "wakefulness-after-wakeup=${wakefulness()} keyguard-showing=${keyguardShowing()}"
            main { activity.stopReadAloud() }
            await("idle") { activity.ttsStatus.value == TtsStatus.IDLE }
            record("tts-screen-off-api${Build.VERSION.SDK_INT}.txt", notes)
        } finally {
            wakeAndUnlock()
            finish(activity)
        }
    }

    @Test
    fun readAloudPausesOnAudioFocusLossAndResumes() {
        assumeTrue("audio focus requests need API 26", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        val focus = AudioFocus(context.getSystemService<AudioManager>()!!)
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            main { activity.updateTtsPreferences { it.copy(speed = 0.7) } }
            main { activity.startReadAloud() }
            awaitSpeaking(activity)

            // A transient interruption (a ringtone, a navigation prompt): Readium keeps speaking through it.
            assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, focus.request(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT))
            SystemClock.sleep(2500)
            notes += "transient-loss-status=${onMain { activity.ttsStatus.value }}"
            focus.abandon()
            SystemClock.sleep(1500)
            notes += "transient-regain-status=${onMain { activity.ttsStatus.value }}"

            // Another app takes the audio for good (music, a video): read-aloud pauses and stays paused.
            assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, focus.request(AudioManager.AUDIOFOCUS_GAIN))
            await("paused by the audio focus loss") { activity.ttsStatus.value == TtsStatus.PAUSED }
            val pausedAt = onMain { activity.ttsLocation.value?.utterance }
            focus.abandon()
            SystemClock.sleep(2500)
            assertEquals(TtsStatus.PAUSED, onMain { activity.ttsStatus.value })
            assertEquals(pausedAt, onMain { activity.ttsLocation.value?.utterance })
            notes += "permanent-loss=paused-at=${pausedAt?.take(40)}"

            // Play resumes from the paused sentence and the voice advances again.
            main { activity.readAloudTogglePlayPause() }
            await("playing after the interruption") { activity.ttsStatus.value == TtsStatus.PLAYING }
            await("advancing after the interruption", 60000) { activity.ttsLocation.value?.utterance != pausedAt }
            notes += "resumed=true"
            main { activity.stopReadAloud() }
            await("idle") { activity.ttsStatus.value == TtsStatus.IDLE }
            record("tts-audio-focus-api${Build.VERSION.SDK_INT}.txt", notes)
        } finally {
            focus.abandon()
            finish(activity)
        }
    }

    /**
     * Holds the API 26 focus request behind a nested class: the JUnit runner reflects over this test
     * class's methods on every device, and a signature mentioning [AudioFocusRequest] would fail to
     * load on API 24 / 25 before the assumption above can skip the test.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private class AudioFocus(private val audioManager: AudioManager) {
        private var request: AudioFocusRequest? = null

        fun request(gain: Int): Int {
            abandon()
            val built = AudioFocusRequest.Builder(gain)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setOnAudioFocusChangeListener {}
                .build()
            request = built
            return audioManager.requestAudioFocus(built)
        }

        fun abandon() {
            request?.let { audioManager.abandonAudioFocusRequest(it) }
            request = null
        }
    }

    @Test
    fun notificationActionsControlReadAloud() {
        assumeTrue("notifications are not permitted on this device", notificationsEnabled())
        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            main { activity.updateTtsPreferences { it.copy(speed = 0.7) } }
            main { activity.startReadAloud() }
            awaitSpeaking(activity)
            await("media notification") { hasNotification() }
            notes += "actions=${notificationActions().map { it.title }}"

            sendAction(androidx.media3.session.R.string.media3_controls_pause_description)
            await("paused from the notification") { activity.ttsStatus.value == TtsStatus.PAUSED }
            val paused = onMain { activity.ttsLocation.value?.utterance }
            sendAction(androidx.media3.session.R.string.media3_controls_play_description)
            await("playing from the notification") { activity.ttsStatus.value == TtsStatus.PLAYING }
            sendAction(R.string.text_read_aloud_next)
            await("next sentence from the notification") { activity.ttsLocation.value?.utterance != paused }
            val afterNext = onMain { activity.ttsLocation.value?.utterance }
            sendAction(R.string.text_read_aloud_previous)
            await("previous sentence from the notification") { activity.ttsLocation.value?.utterance != afterNext }
            sendAction(R.string.text_read_aloud_stop)
            await("stopped from the notification") { activity.ttsStatus.value == TtsStatus.IDLE }
            await("notification removed") { !hasNotification() }
            await("service stopped") { !TtsForegroundService.running }
            notes += "notification-controls=pause,play,next,previous,stop"
            record("tts-notification-api${Build.VERSION.SDK_INT}.txt", notes)
        } finally {
            finish(activity)
        }
    }

    // ---- helpers ----

    /** Waits for playback; a device without an engine or English voice data records that and skips. */
    private fun awaitSpeaking(activity: EpubReaderActivity) {
        val deadline = SystemClock.uptimeMillis() + 60000
        while (SystemClock.uptimeMillis() < deadline) {
            val status = onMain { activity.ttsStatus.value }
            val event = onMain { activity.lastTtsEvent }
            if (status == TtsStatus.PLAYING && onMain { activity.ttsLocation.value } != null) return
            if (event != null && status == TtsStatus.IDLE) {
                notes += "unavailable=$event"
                record("tts-unavailable-api${Build.VERSION.SDK_INT}.txt", notes)
                assumeTrue("read-aloud unavailable on this device: $event", false)
            }
            SystemClock.sleep(100)
        }
        fail("Timed out waiting for read-aloud to speak (status ${onMain { activity.ttsStatus.value }})")
    }

    private fun decorationCount(activity: EpubReaderActivity): Int? =
        evaluate(activity, "document.querySelectorAll('[data-group=\"${EpubReaderActivity.TTS_DECORATIONS}\"] > div').length")?.toIntOrNull()

    /** Polls the page from the instrumentation thread (the evaluator suspends on main, so [await] cannot host it). */
    private fun awaitDecorations(activity: EpubReaderActivity, message: String, condition: (Int) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition(decorationCount(activity) ?: 0)) return
            SystemClock.sleep(200)
        }
        fail("Timed out: $message (${decorationCount(activity)} boxes)")
    }

    private fun notificationsEnabled(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun hasNotification(): Boolean =
        context.getSystemService<NotificationManager>()!!.activeNotifications.any { it.id == TtsForegroundService.NOTIFICATION_ID }

    private fun notificationActions(): List<Notification.Action> =
        context.getSystemService<NotificationManager>()!!.activeNotifications
            .firstOrNull { it.id == TtsForegroundService.NOTIFICATION_ID }?.notification?.actions.orEmpty().toList()

    /** Taps the notification action titled with [titleRes], as the user would in the shade. */
    private fun sendAction(titleRes: Int) {
        val title = context.getString(titleRes)
        val deadline = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < deadline) {
            val action = notificationActions().firstOrNull { it.title?.toString() == title }
            if (action != null) {
                action.actionIntent.send()
                return
            }
            SystemClock.sleep(200)
        }
        fail("No notification action titled $title among ${notificationActions().map { it.title }}")
    }

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).bufferedReader().readText()
        }

    private fun wakefulness(): String =
        shell("dumpsys power").lineSequence().firstOrNull { "mWakefulness=" in it }?.substringAfter("mWakefulness=")?.trim() ?: "unknown"

    private fun screenOffForced(): Boolean = InstrumentationRegistry.getArguments().getString("screenOff") == "force"

    /** The window policy's keyguard line (`showing=true|false` under KeyguardServiceDelegate), "unknown" when absent. */
    private fun keyguardShowing(): String =
        shell("dumpsys window policy").lineSequence().map { it.trim() }.firstOrNull { it.startsWith("showing=") }?.substringAfter("showing=") ?: "unknown"

    /** Wakes the device and dismisses a non-secure keyguard, retrying a few times; a secure one needs the user. */
    private fun wakeAndUnlock() {
        repeat(3) { attempt ->
            shell("input keyevent KEYCODE_WAKEUP")
            SystemClock.sleep(1000)
            shell("wm dismiss-keyguard")
            SystemClock.sleep(1000)
            if (attempt > 0) shell("input keyevent 82")
            SystemClock.sleep(500)
            if (keyguardShowing() != "true") return
        }
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

    private fun record(name: String, lines: List<String>) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name).writeText(lines.joinToString("\n") + "\n")
    }

    private fun finish(activity: EpubReaderActivity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
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
