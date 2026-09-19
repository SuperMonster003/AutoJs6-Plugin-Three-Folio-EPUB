package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ProgressRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The read-aloud foreground service (roadmap P3 / D15): a media3 [MediaSessionService] that wraps
 * the running [TtsSession]'s player in a media session, so Android shows a media notification with
 * play / pause, previous / next sentence and stop, routes headset and media buttons, manages audio
 * focus (inside Readium's adapter) and keeps the process in the foreground while speech plays,
 * with the screen off too. The service is not exported: only the reader binds it (through
 * [ACTION_BIND]) and media3 connects to it in-process. It exists only while a session is attached;
 * detaching stops the foreground state, removes the notification and stops the service.
 *
 * Roadmap D26: with "continue in the background" on, a closing reader parks its playing session
 * here together with the book it reads from ([park]); the service then owns both until the book
 * ends, the sleep timer fires, the notification stops it, or a reader takes the session back
 * ([take]). Tapping the notification meanwhile reopens the reader on that book
 * ([EpubReaderActivity.ACTION_RESUME_READ_ALOUD]); the reading position is saved when a parked voice stops.
 */
@OptIn(UnstableApi::class)
internal class TtsForegroundService : MediaSessionService() {

    /** What the reader binds: attach the session to speak, detach it to stop. */
    inner class Binder : android.os.Binder() {
        fun attach(session: TtsSession) = this@TtsForegroundService.attach(session)

        fun detach(session: TtsSession) = this@TtsForegroundService.detach(session)

        val attached: TtsSession? get() = session
    }

    private val binder = Binder()
    private var session: TtsSession? = null
    private var mediaSession: MediaSession? = null

    /** The session no reader owns (roadmap D26), with its book. */
    private var background: ReadAloudHandle? = null
    private var backgroundJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val previousCommand = SessionCommand(ACTION_PREVIOUS, Bundle.EMPTY)
    private val nextCommand = SessionCommand(ACTION_NEXT, Bundle.EMPTY)
    private val stopCommand = SessionCommand(ACTION_STOP, Bundle.EMPTY)

    private val callback = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(previousCommand)
                        .add(nextCommand)
                        .add(stopCommand)
                        .build(),
                )
                .setMediaButtonPreferences(mediaButtons())
                .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_PREVIOUS -> this@TtsForegroundService.session?.previous()
                ACTION_NEXT -> this@TtsForegroundService.session?.next()
                ACTION_STOP -> this@TtsForegroundService.session?.requestStop()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        /** Headset next / previous skip one sentence; everything else keeps media3's handling. */
        override fun onMediaButtonEvent(session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent): Boolean {
            val event = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return false
            if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
            return when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    this@TtsForegroundService.session?.next()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    this@TtsForegroundService.session?.previous()
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        running = true
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.text_read_aloud)
                .setNotificationId(NOTIFICATION_ID)
                .build()
                .apply { setSmallIcon(R.drawable.ic_volume_up_24) },
        )
    }

    override fun onBind(intent: Intent?): IBinder? =
        if (intent?.action == ACTION_BIND) binder else super.onBind(intent)

    /**
     * media3 starts the service itself to go foreground (its own intent), and a sticky restart after
     * a process death arrives with no intent: without a session there is nothing to keep alive.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (session == null) stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        stopBackground()
        session?.let { detach(it) }
        serviceScope.cancel()
        running = false
        instance = null
        super.onDestroy()
    }

    private fun parkSession(handle: ReadAloudHandle): Boolean {
        if (handle.session.closed) return false
        attach(handle.session)
        background = handle
        handle.session.stopHandler = { stopBackground() }
        backgroundJob = serviceScope.launch { handle.session.events.collect { stopBackground() } }
        mediaSession?.setSessionActivity(resumeIntent())
        return true
    }

    private fun takeParked(bookKey: String?): ReadAloudHandle? {
        val handle = background ?: return null
        if (bookKey != null && handle.session.bookKey != bookKey) return null
        background = null
        backgroundJob?.cancel()
        backgroundJob = null
        handle.session.stopHandler = null
        mediaSession?.setSessionActivity(null)
        return handle
    }

    /** Ends a parked voice: remembers where it got to, releases the session, the book and the service. */
    private fun stopBackground() {
        val handle = background ?: return
        background = null
        backgroundJob?.cancel()
        backgroundJob = null
        saveProgress(handle.session)
        detach(handle.session)
        handle.close()
    }

    private fun saveProgress(session: TtsSession) {
        val key = session.bookKey ?: return
        val locator = session.location.value?.locator ?: return
        val record = ProgressRecord(locator.toJSON(), System.currentTimeMillis(), locator.locations.totalProgression)
        val store = BookDataStore.forFilesDirectory(filesDir)
        CoroutineScope(Dispatchers.IO).launch { runCatching { store.writeProgress(key, record) } }
    }

    private fun resumeIntent(): PendingIntent {
        val intent = Intent(this, EpubReaderActivity::class.java)
            .setAction(EpubReaderActivity.ACTION_RESUME_READ_ALOUD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun attach(session: TtsSession) {
        if (this.session === session) return
        this.session?.let { detach(it) }
        this.session = session
        mediaSession = MediaSession.Builder(this, session.media3Player())
            .setId(SESSION_ID)
            .setCallback(callback)
            .setMediaButtonPreferences(mediaButtons())
            .build()
            .also { addSession(it) }
    }

    private fun detach(session: TtsSession) {
        if (this.session !== session) return
        this.session = null
        mediaSession?.let { active ->
            removeSession(active)
            active.release()
        }
        mediaSession = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun mediaButtons(): List<CommandButton> = listOf(
        CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setSessionCommand(previousCommand)
            .setDisplayName(getString(R.string.text_read_aloud_previous))
            .setSlots(CommandButton.SLOT_BACK)
            .build(),
        CommandButton.Builder(CommandButton.ICON_NEXT)
            .setSessionCommand(nextCommand)
            .setDisplayName(getString(R.string.text_read_aloud_next))
            .setSlots(CommandButton.SLOT_FORWARD)
            .build(),
        CommandButton.Builder(CommandButton.ICON_STOP)
            .setSessionCommand(stopCommand)
            .setDisplayName(getString(R.string.text_read_aloud_stop))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build(),
    )

    companion object {
        const val ACTION_BIND = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.BIND"
        private const val ACTION_PREVIOUS = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.PREVIOUS"
        private const val ACTION_NEXT = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.NEXT"
        private const val ACTION_STOP = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.STOP"
        const val CHANNEL_ID = "read_aloud"
        const val NOTIFICATION_ID = 2001
        private const val SESSION_ID = "read-aloud"

        /** Test hook: true between onCreate and onDestroy of the (single) service instance. */
        @Volatile
        var running: Boolean = false
            private set

        @Volatile
        private var instance: TtsForegroundService? = null

        /** Roadmap D26: parks [handle] with the running service; false (and nothing owned) when no service runs. */
        fun park(handle: ReadAloudHandle): Boolean = instance?.parkSession(handle) ?: false

        /** Roadmap D26: takes the parked session back when it reads the book with [bookKey] (null = whichever is parked). */
        fun take(bookKey: String?): ReadAloudHandle? = instance?.takeParked(bookKey)

        /** Test hook: the session parked in the background, if any. */
        val parked: TtsSession? get() = instance?.background?.session

        /** Test hook: ends a parked voice as the notification's stop would. */
        fun stopParked() {
            instance?.stopBackground()
        }
    }
}
