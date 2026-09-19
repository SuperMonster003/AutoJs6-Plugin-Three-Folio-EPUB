package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.navigator.media.common.MediaNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.TtsNavigatorFactory
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.navigator.media.tts.android.AndroidTtsSettings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse

/** Where read-aloud stands, as the reader chrome and the tests see it. */
internal enum class TtsStatus { IDLE, STARTING, PLAYING, PAUSED }

/** The sentence being spoken: its resource, its text and the locator the reader highlights and follows. */
internal data class TtsLocation(val href: String, val utterance: String, val locator: Locator)

/** One-off outcomes the reader turns into dialogs or toasts. */
internal sealed class TtsEvent {
    /** The book has no text content service, so there is nothing to speak. */
    object NoContent : TtsEvent()

    /** No text-to-speech engine could be initialized (none installed or enabled). */
    object NoEngine : TtsEvent()

    /** The last sentence of the book was spoken. */
    object Ended : TtsEvent()

    /** The engine knows [language] but its voice data is not installed. */
    data class MissingVoiceData(val language: String) : TtsEvent()

    data class Failed(val kind: Kind) : TtsEvent() {
        enum class Kind { NETWORK, ENGINE, CONTENT }
    }
}

/**
 * One read-aloud run over a book (roadmap P3): owns Readium's TTS navigator (the system engine
 * plus the sentence tokenizer over the publication's content) and republishes its playback and
 * position on the main thread as [status], [location] and [events]. The session outlives no book:
 * the controller closes it when the user stops, the book ends, the engine fails or the reader goes
 * away; [close] is idempotent and releases the engine.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class TtsSession private constructor(
    val bookKey: String?,
    val title: String?,
) : TtsNavigator.Listener {

    private lateinit var navigator: AndroidTtsNavigator
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _status = MutableStateFlow(TtsStatus.STARTING)
    val status: StateFlow<TtsStatus> get() = _status

    private val _location = MutableStateFlow<TtsLocation?>(null)
    val location: StateFlow<TtsLocation?> get() = _location

    private val _events = MutableSharedFlow<TtsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TtsEvent> get() = _events

    /** Set by the owner: a stop requested through the media session (notification, headset, system). */
    var stopHandler: (() -> Unit)? = null

    /** True once [play] was called: before that the navigator idles and its paused state is not shown. */
    private var started = false

    /** True after the end of the book or a failure was reported; later playback updates are ignored. */
    private var terminal = false

    var closed: Boolean = false
        private set

    /** The voices of the engine (roadmap P3 language and voice picker). */
    val voices: List<VoiceOption> get() = navigator.voices.map { it.toOption() }

    /** The settings the navigator resolved from the preferences and the book. */
    val settings: StateFlow<AndroidTtsSettings> get() = navigator.settings

    private fun attach(navigator: AndroidTtsNavigator) {
        this.navigator = navigator
        scope.launch { navigator.playback.collect { onPlayback(it) } }
        scope.launch {
            navigator.location.collect { location ->
                _location.value = TtsLocation(location.href.toString(), location.utterance, location.utteranceLocator)
            }
        }
    }

    private fun onPlayback(playback: TtsNavigator.Playback) {
        if (terminal || closed) return
        when (val state = playback.state) {
            is MediaNavigator.State.Ended -> {
                terminal = true
                _status.value = TtsStatus.PAUSED
                _events.tryEmit(TtsEvent.Ended)
            }
            is MediaNavigator.State.Failure -> {
                terminal = true
                _status.value = TtsStatus.PAUSED
                _events.tryEmit(eventFor(state))
            }
            else -> if (started) _status.value = if (playback.playWhenReady) TtsStatus.PLAYING else TtsStatus.PAUSED
        }
    }

    private fun eventFor(state: MediaNavigator.State.Failure): TtsEvent {
        val error = (state as? TtsNavigator.State.Failure)?.error
        val engineError = (error as? TtsNavigator.Error.EngineError<*>)?.cause as? AndroidTtsEngine.Error
        return when (engineError) {
            is AndroidTtsEngine.Error.LanguageMissingData -> TtsEvent.MissingVoiceData(engineError.language.code)
            AndroidTtsEngine.Error.Network, AndroidTtsEngine.Error.NetworkTimeout -> TtsEvent.Failed(TtsEvent.Failed.Kind.NETWORK)
            null -> TtsEvent.Failed(TtsEvent.Failed.Kind.CONTENT)
            else -> TtsEvent.Failed(TtsEvent.Failed.Kind.ENGINE)
        }
    }

    fun play() {
        if (closed) return
        started = true
        navigator.play()
    }

    fun pause() {
        if (closed) return
        navigator.pause()
    }

    fun togglePlayPause() {
        if (_status.value == TtsStatus.PLAYING) pause() else play()
    }

    fun previous() {
        if (!closed) navigator.skipToPreviousUtterance()
    }

    fun next() {
        if (!closed) navigator.skipToNextUtterance()
    }

    /** Continues from [locator] (a page the user turned to); playback keeps its play or pause state. */
    fun go(locator: Locator) {
        if (!closed) navigator.go(locator)
    }

    fun submitPreferences(preferences: AndroidTtsPreferences) {
        if (!closed) navigator.submitPreferences(preferences)
    }

    /** The media3 player the foreground service wraps in its media session. */
    fun media3Player(): Player = navigator.asMedia3Player()

    /** TtsNavigator.Listener: media3 asked the player to stop (notification dismissed, system stop). */
    override fun onStopRequested() {
        requestStop()
    }

    fun requestStop() {
        stopHandler?.invoke()
    }

    /** Releases the engine; safe to call twice. The media session must be released first. */
    fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        _status.value = TtsStatus.IDLE
        _location.value = null
        runCatching { navigator.close() }
    }

    companion object {
        /**
         * Creates the navigator (initializing the system engine) from [initialLocator]; fails with
         * [TtsNavigatorFactory.Error.EngineInitialization] when no engine is available.
         */
        suspend fun open(
            factory: AndroidTtsNavigatorFactory,
            bookKey: String?,
            title: String?,
            initialLocator: Locator?,
            preferences: AndroidTtsPreferences,
        ): Try<TtsSession, TtsNavigatorFactory.Error> {
            val session = TtsSession(bookKey, title)
            val navigator = factory.createNavigator(session, initialLocator, preferences)
                .getOrElse { error -> return Try.failure(error) }
            session.attach(navigator)
            return Try.success(session)
        }

        fun AndroidTtsEngine.Voice.toOption(): VoiceOption =
            VoiceOption(id.value, language.code, quality.ordinal, requiresNetwork)
    }
}
