package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsSession.Companion.toOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.readium.navigator.media.tts.TtsNavigatorFactory
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.getOrElse
import kotlin.coroutines.coroutineContext

/**
 * Read-aloud for the reader (roadmap P3, `TtsController`): starts a [TtsSession] from a locator,
 * hands it to [TtsForegroundService] (the media session, its notification and the foreground
 * state that keeps speech going with the screen off), mirrors the session's status, position and
 * events for the Activity, and stops everything on request, at the end of the book, on an engine
 * failure or when the owning view model is cleared (roadmap D15: closing the reader stops
 * read-aloud). Preferences (speed, pitch, language, voices) persist through [TtsPreferencesStore].
 */
@OptIn(ExperimentalReadiumApi::class)
internal class TtsController(
    private val application: Application,
    private val scope: CoroutineScope,
    private val store: TtsPreferencesStore,
) {

    private val _session = MutableStateFlow<TtsSession?>(null)

    /** The running session, null while idle. */
    val session: StateFlow<TtsSession?> get() = _session

    private val _status = MutableStateFlow(TtsStatus.IDLE)
    val status: StateFlow<TtsStatus> get() = _status

    private val _location = MutableStateFlow<TtsLocation?>(null)
    val location: StateFlow<TtsLocation?> get() = _location

    private val _events = MutableSharedFlow<TtsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TtsEvent> get() = _events

    private val _sleepTimer = MutableStateFlow(SleepTimer.OFF)

    /** The running session's sleep timer, [SleepTimer.OFF] while idle. */
    val sleepTimer: StateFlow<SleepTimer> get() = _sleepTimer

    private val _preferences = MutableStateFlow(store.read() ?: AndroidTtsPreferences())

    /** The global read-aloud preferences; applied to the running session as they change. */
    val preferences: StateFlow<AndroidTtsPreferences> get() = _preferences

    /** Single-lane writer that outlives the view model scope so the last edit always lands. */
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    val isActive: Boolean get() = _status.value != TtsStatus.IDLE

    private var startJob: Job? = null
    private val sessionJobs = ArrayList<Job>()

    /** A book that came back with an adopted session and belongs to nobody else; closed with the session. */
    private var orphanBook: OrphanBook? = null

    private var binder: TtsForegroundService.Binder? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as? TtsForegroundService.Binder
            _session.value?.let { binder?.attach(it) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    /**
     * Starts speaking [publication] at [initialLocator] (the first visible element). No-op while a
     * session exists; failures surface as [TtsEvent.NoContent] or [TtsEvent.NoEngine].
     */
    fun start(publication: Publication, bookKey: String?, title: String?, initialLocator: Locator?) {
        if (_status.value != TtsStatus.IDLE) return
        _status.value = TtsStatus.STARTING
        startJob = scope.launch {
            val provider = SystemTtsEngineProvider(application, AndroidTtsEngine.VoiceSelector { language, voices -> selectVoice(language, voices) })
            val factory = TtsNavigatorFactory(application, publication, provider)
            if (factory == null) {
                fail(TtsEvent.NoContent)
                return@launch
            }
            val session = TtsSession.open(factory, bookKey, title, initialLocator, _preferences.value).getOrElse { error ->
                fail(if (error is TtsNavigatorFactory.Error.EngineInitialization) TtsEvent.NoEngine else TtsEvent.NoContent)
                return@launch
            }
            if (!coroutineContext.isActive || _status.value != TtsStatus.STARTING) {
                // Stopped while the engine was initializing.
                session.close()
                return@launch
            }
            adopt(session)
            session.play()
        }
    }

    /** Takes [session] as the running one: mirrors its flows, stops on its events, attaches it to the service. */
    fun adopt(session: TtsSession) {
        session.stopHandler = { stop() }
        _session.value = session
        _status.value = session.status.value
        sessionJobs += scope.launch { session.status.collect { _status.value = it } }
        sessionJobs += scope.launch { session.location.collect { _location.value = it } }
        sessionJobs += scope.launch { session.sleepTimer.collect { _sleepTimer.value = it } }
        sessionJobs += scope.launch {
            session.events.collect { event ->
                _events.emit(event)
                // The end of the book, the sleep timer and every failure release the engine at once (roadmap P3).
                stop()
            }
        }
        bind()
        binder?.attach(session)
    }

    /**
     * Roadmap D26: the reader is closing while the voice should go on. Hands the playing session and
     * [orphan] (the book it reads from, which the reader gives up) to the service; the controller ends
     * up idle. Anything not playing is stopped instead, and false comes back.
     */
    fun park(orphan: OrphanBook?): Boolean {
        val session = _session.value
        if (session == null || _status.value != TtsStatus.PLAYING) {
            orphan?.close()
            stop()
            return false
        }
        startJob?.cancel()
        startJob = null
        sessionJobs.forEach { it.cancel() }
        sessionJobs.clear()
        _session.value = null
        session.stopHandler = null
        val handle = ReadAloudHandle(session, orphan ?: orphanBook)
        orphanBook = null
        val parked = TtsForegroundService.park(handle)
        if (!parked) handle.close()
        unbind()
        _status.value = TtsStatus.IDLE
        _location.value = null
        _sleepTimer.value = SleepTimer.OFF
        return parked
    }

    /**
     * Roadmap D26: a voice parked in the background still reads the book with [bookKey]; takes it
     * back as the running session (its book stays with the controller until the session ends).
     */
    fun adoptSpeaking(bookKey: String?): TtsSession? {
        if (bookKey == null || _status.value != TtsStatus.IDLE) return null
        val handle = TtsForegroundService.take(bookKey) ?: return null
        orphanBook = handle.takeOrphan()
        adopt(handle.session)
        return handle.session
    }

    /** Roadmap D26: takes whatever is parked, for a reader reopened from the notification (which shows its book). */
    fun takeParked(): ReadAloudHandle? = if (_status.value == TtsStatus.IDLE) TtsForegroundService.take(null) else null

    /** Arms the sleep timer of the running session; [durationMillis] lets tests shorten a fixed span. */
    fun armSleepTimer(timer: SleepTimer, durationMillis: Long? = SleepTimerPolicy.durationMillis(timer)) {
        _session.value?.armSleepTimer(timer, durationMillis)
    }

    private suspend fun fail(event: TtsEvent) {
        _status.value = TtsStatus.IDLE
        _events.emit(event)
    }

    fun play() = _session.value?.play()

    fun pause() = _session.value?.pause()

    fun togglePlayPause() = _session.value?.togglePlayPause()

    fun previous() = _session.value?.previous()

    fun next() = _session.value?.next()

    /** Stops and releases the session (and the service with it); safe to call while idle. */
    fun stop() {
        startJob?.cancel()
        startJob = null
        sessionJobs.forEach { it.cancel() }
        sessionJobs.clear()
        val session = _session.value
        _session.value = null
        if (session != null) {
            binder?.detach(session)
            session.close()
        }
        orphanBook?.close()
        orphanBook = null
        unbind()
        _status.value = TtsStatus.IDLE
        _location.value = null
        _sleepTimer.value = SleepTimer.OFF
    }

    /** The owning view model is going away: the reader closes, so read-aloud stops (roadmap D15). */
    fun shutdown() = stop()

    /** Changes the preferences, applies them to the running session and persists them. */
    fun updatePreferences(transform: (AndroidTtsPreferences) -> AndroidTtsPreferences) {
        val next = transform(_preferences.value)
        if (next == _preferences.value) return
        _preferences.value = next
        _session.value?.submitPreferences(next)
        persistScope.launch { runCatching { store.write(next) } }
    }

    /** Readium's voice selector: the policy's best offline voice for the sentence's language. */
    private fun selectVoice(language: Language?, voices: Set<AndroidTtsEngine.Voice>): AndroidTtsEngine.Voice? {
        val option = TtsVoicePolicy.preferredVoice(language?.code, voices.map { it.toOption() }) ?: return null
        return voices.firstOrNull { it.id.value == option.id }
    }

    private fun bind() {
        if (bound) return
        val intent = Intent(application, TtsForegroundService::class.java).setAction(TtsForegroundService.ACTION_BIND)
        bound = runCatching { application.bindService(intent, connection, Context.BIND_AUTO_CREATE) }.getOrDefault(false)
    }

    private fun unbind() {
        if (!bound) return
        bound = false
        binder = null
        runCatching { application.unbindService(connection) }
    }
}
