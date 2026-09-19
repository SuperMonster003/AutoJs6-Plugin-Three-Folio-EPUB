package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.navigator.media.tts.TtsEngine
import org.readium.navigator.media.tts.TtsEngineProvider
import org.readium.navigator.media.tts.android.AndroidTtsDefaults
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsEngineProvider
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.navigator.media.tts.android.AndroidTtsPreferencesEditor
import org.readium.navigator.media.tts.android.AndroidTtsSettings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.Try
import java.util.Locale

/**
 * Readium's [TtsEngine] contract on Android's [TextToSpeech], derived from Readium's
 * `AndroidTtsEngine` (Copyright 2022 Readium Foundation, BSD 3-Clause License) with one change:
 * the engine to bind is chosen by [SystemTtsEngine.connect] instead of the framework's default
 * lookup alone. The no-argument `TextToSpeech` constructor only falls back to an installed engine
 * when that engine is a system app; on Xiaomi phones the XiaoAi engine lives in `/data/app` and
 * `tts_default_synth` is unset until the user picks it in the system settings, so the default
 * lookup fails while the very same engine works when its package is named. The engine name is
 * remembered for reconnections (some devices drop the service while the app is in the background).
 *
 * Errors and voices are Readium's own types, so the navigator, its preferences, settings and the
 * media3 adapter stay untouched.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class SystemTtsEngine private constructor(
    private val context: Context,
    private val engineName: String?,
    engine: TextToSpeech,
    private val settingsResolver: AndroidTtsEngine.SettingsResolver,
    private val voiceSelector: AndroidTtsEngine.VoiceSelector,
    override val voices: Set<AndroidTtsEngine.Voice>,
    initialPreferences: AndroidTtsPreferences,
) : TtsEngine<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsEngine.Error, AndroidTtsEngine.Voice> {

    private data class Request(val id: TtsEngine.RequestId, val text: String, val language: Language?)

    private sealed class State {
        data class EngineAvailable(val engine: TextToSpeech) : State()
        data class WaitingForService(val pendingRequests: MutableList<Request> = mutableListOf()) : State()
        data class Failure(val error: AndroidTtsEngine.Error) : State()
    }

    private val coroutineScope: CoroutineScope = MainScope()
    private var utteranceListener: TtsEngine.Listener<AndroidTtsEngine.Error>? = null
    private var state: State = State.EngineAvailable(engine)
    private var isClosed = false

    private val _settings = MutableStateFlow(settingsResolver.settings(initialPreferences)).also { engine.setupPitchAndSpeed(it.value) }
    override val settings: StateFlow<AndroidTtsSettings> = _settings.asStateFlow()

    override fun submitPreferences(preferences: AndroidTtsPreferences) {
        val newSettings = settingsResolver.settings(preferences)
        _settings.value = newSettings
        (state as? State.EngineAvailable)?.engine?.setupPitchAndSpeed(newSettings)
    }

    override fun setListener(listener: TtsEngine.Listener<AndroidTtsEngine.Error>?) {
        utteranceListener = listener
        (state as? State.EngineAvailable)?.let { setupListener(it.engine) }
    }

    override fun speak(requestId: TtsEngine.RequestId, text: String, language: Language?) {
        check(!isClosed) { "Engine is closed." }
        val request = Request(requestId, text, language)
        when (val stateNow = state) {
            is State.WaitingForService -> stateNow.pendingRequests.add(request)
            is State.Failure -> tryReconnect(request)
            is State.EngineAvailable -> if (!doSpeak(stateNow.engine, request)) {
                cleanEngine(stateNow.engine)
                tryReconnect(request)
            }
        }
    }

    override fun stop() {
        when (val stateNow = state) {
            is State.EngineAvailable -> stateNow.engine.stop()
            is State.Failure -> Unit
            is State.WaitingForService -> {
                for (request in stateNow.pendingRequests) utteranceListener?.onFlushed(request.id)
                stateNow.pendingRequests.clear()
            }
        }
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        coroutineScope.cancel()
        (state as? State.EngineAvailable)?.let { cleanEngine(it.engine) }
    }

    private fun doSpeak(engine: TextToSpeech, request: Request): Boolean =
        engine.setupVoice(settings.value, request.id, request.language) &&
            engine.speak(request.text, TextToSpeech.QUEUE_ADD, null, request.id.value) == TextToSpeech.SUCCESS

    private fun setupListener(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(utteranceListener?.let(::UtteranceListener))
    }

    private fun tryReconnect(request: Request) {
        state = State.WaitingForService(mutableListOf(request))
        coroutineScope.launch {
            val connection = connect(context, engineName)
            val previousState = state as State.WaitingForService
            if (connection == null) {
                val error = AndroidTtsEngine.Error.Service
                state = State.Failure(error)
                for (pending in previousState.pendingRequests) utteranceListener?.onError(pending.id, error)
                return@launch
            }
            val engine = connection.engine
            setupListener(engine)
            engine.setupPitchAndSpeed(settings.value)
            state = State.EngineAvailable(engine)
            if (isClosed) {
                engine.shutdown()
            } else {
                for (pending in previousState.pendingRequests) doSpeak(engine, pending)
            }
        }
    }

    private fun cleanEngine(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(null)
        engine.shutdown()
    }

    private fun TextToSpeech.setupPitchAndSpeed(settings: AndroidTtsSettings) {
        setSpeechRate(settings.speed.toFloat())
        setPitch(settings.pitch.toFloat())
    }

    /**
     * Picks the language and voice for one utterance: the utterance's own language unless the
     * settings override it, else the settings language, else the engine's default voice; the
     * language must be known to the engine and have its data installed.
     */
    private fun TextToSpeech.setupVoice(settings: AndroidTtsSettings, id: TtsEngine.RequestId, utteranceLanguage: Language?): Boolean {
        val language = utteranceLanguage
            .takeUnless { settings.overrideContentLanguage }
            ?.takeIf { isLanguageAvailable(it.locale) != TextToSpeech.LANG_NOT_SUPPORTED }
            ?: settings.language.takeIf { isLanguageAvailable(it.locale) != TextToSpeech.LANG_NOT_SUPPORTED }
            ?: defaultVoice?.locale?.let { Language(it) }
        if (language == null) {
            utteranceListener?.onError(id, AndroidTtsEngine.Error.Unknown)
            return false
        }
        if (isLanguageAvailable(language.locale) < TextToSpeech.LANG_AVAILABLE) {
            utteranceListener?.onError(id, AndroidTtsEngine.Error.LanguageMissingData(language))
            return false
        }
        val voice = settings.voices[language]?.let { voiceForName(it.value) }
            ?: settings.voices[language.removeRegion()]?.let { voiceForName(it.value) }
            ?: voiceSelector.voice(language, this@SystemTtsEngine.voices)?.let { voiceForName(it.id.value) }
        if (voice != null) this.voice = voice else this.language = language.locale
        return true
    }

    private fun TextToSpeech.voiceForName(name: String): Voice? = runCatching { voices }.getOrNull()?.firstOrNull { it.name == name }

    private class UtteranceListener(private val listener: TtsEngine.Listener<AndroidTtsEngine.Error>) : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) = listener.onStart(TtsEngine.RequestId(utteranceId))

        override fun onStop(utteranceId: String, interrupted: Boolean) {
            val requestId = TtsEngine.RequestId(utteranceId)
            if (interrupted) listener.onInterrupted(requestId) else listener.onFlushed(requestId)
        }

        override fun onDone(utteranceId: String) = listener.onDone(TtsEngine.RequestId(utteranceId))

        @Deprecated("Deprecated in the framework", ReplaceWith("onError(utteranceId, -1)"))
        override fun onError(utteranceId: String) = onError(utteranceId, -1)

        override fun onError(utteranceId: String, errorCode: Int) = listener.onError(TtsEngine.RequestId(utteranceId), nativeError(errorCode))

        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) =
            listener.onRange(TtsEngine.RequestId(utteranceId), start until end)
    }

    /** A bound engine and the package it was bound with (null = the framework's default). */
    internal class Connection(val engine: TextToSpeech, val engineName: String?)

    companion object {

        /** The package of the engine the last [open] bound (diagnostics and device evidence). */
        @Volatile
        var boundEngine: String? = null
            private set

        /**
         * Connects to [preferredEngine] (null = the framework's default engine). When that fails,
         * the engine [fallbackEngine] picks from the installed ones is named explicitly.
         */
        suspend fun connect(context: Context, preferredEngine: String?): Connection? {
            val (first, ready) = create(context, preferredEngine)
            if (ready) return Connection(first, preferredEngine)
            val fallback = fallbackEngine(
                defaultEngine = runCatching { first.defaultEngine }.getOrNull(),
                installed = runCatching { first.engines.map { it.name } }.getOrNull().orEmpty(),
            )
            first.shutdown()
            if (fallback == null || fallback == preferredEngine) return null
            val (second, secondReady) = create(context, fallback)
            if (secondReady) return Connection(second, fallback)
            second.shutdown()
            return null
        }

        /** The engine to name when the framework's default lookup found none: the declared default if installed, else the first installed. */
        fun fallbackEngine(defaultEngine: String?, installed: List<String>): String? =
            defaultEngine?.takeIf { it in installed } ?: installed.firstOrNull()

        /** Opens an engine for [initialPreferences], or returns null when no engine can be bound. */
        suspend fun open(
            context: Context,
            settingsResolver: AndroidTtsEngine.SettingsResolver,
            voiceSelector: AndroidTtsEngine.VoiceSelector,
            initialPreferences: AndroidTtsPreferences,
        ): SystemTtsEngine? {
            val connection = connect(context, null) ?: return null
            boundEngine = connection.engineName ?: runCatching { connection.engine.defaultEngine }.getOrNull()
            val voices = runCatching { connection.engine.voices }.getOrNull() // throws on some devices
                ?.mapNotNull { it.toReadiumVoice() }
                ?.toSet()
                .orEmpty()
            return SystemTtsEngine(context, connection.engineName, connection.engine, settingsResolver, voiceSelector, voices, initialPreferences)
        }

        private suspend fun create(context: Context, engineName: String?): Pair<TextToSpeech, Boolean> {
            val init = CompletableDeferred<Boolean>()
            val listener = TextToSpeech.OnInitListener { status -> init.complete(status == TextToSpeech.SUCCESS) }
            val engine = if (engineName == null) TextToSpeech(context, listener) else TextToSpeech(context, listener, engineName)
            return engine to init.await()
        }

        private fun Voice.toReadiumVoice(): AndroidTtsEngine.Voice? {
            val locale = locale ?: return null
            return AndroidTtsEngine.Voice(
                id = AndroidTtsEngine.Voice.Id(name),
                language = Language(locale),
                quality = when (quality) {
                    Voice.QUALITY_VERY_HIGH -> AndroidTtsEngine.Voice.Quality.Highest
                    Voice.QUALITY_HIGH -> AndroidTtsEngine.Voice.Quality.High
                    Voice.QUALITY_LOW -> AndroidTtsEngine.Voice.Quality.Low
                    Voice.QUALITY_VERY_LOW -> AndroidTtsEngine.Voice.Quality.Lowest
                    else -> AndroidTtsEngine.Voice.Quality.Normal
                },
                requiresNetwork = isNetworkConnectionRequired,
            )
        }

        private fun nativeError(code: Int): AndroidTtsEngine.Error = when (code) {
            TextToSpeech.ERROR_INVALID_REQUEST -> AndroidTtsEngine.Error.InvalidRequest
            TextToSpeech.ERROR_NETWORK -> AndroidTtsEngine.Error.Network
            TextToSpeech.ERROR_NETWORK_TIMEOUT -> AndroidTtsEngine.Error.NetworkTimeout
            TextToSpeech.ERROR_NOT_INSTALLED_YET -> AndroidTtsEngine.Error.NotInstalledYet
            TextToSpeech.ERROR_OUTPUT -> AndroidTtsEngine.Error.Output
            TextToSpeech.ERROR_SERVICE -> AndroidTtsEngine.Error.Service
            TextToSpeech.ERROR_SYNTHESIS -> AndroidTtsEngine.Error.Synthesis
            else -> AndroidTtsEngine.Error.Unknown
        }
    }
}

/**
 * Readium's engine provider with [SystemTtsEngine] in place of `AndroidTtsEngine`; preferences,
 * playback parameters and error mapping are delegated to Readium's own provider so the navigator
 * behaves exactly as with the stock engine.
 */
@OptIn(ExperimentalReadiumApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
internal class SystemTtsEngineProvider(
    private val context: Context,
    private val voiceSelector: AndroidTtsEngine.VoiceSelector,
    private val defaults: AndroidTtsDefaults = AndroidTtsDefaults(),
) : TtsEngineProvider<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsPreferencesEditor, AndroidTtsEngine.Error, AndroidTtsEngine.Voice> {

    private val delegate = AndroidTtsEngineProvider(context, defaults, voiceSelector)

    override suspend fun createEngine(
        publication: Publication,
        initialPreferences: AndroidTtsPreferences,
    ): Try<TtsEngine<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsEngine.Error, AndroidTtsEngine.Voice>, Error> {
        val resolver = AndroidTtsEngine.SettingsResolver { preferences -> settings(publication.metadata, preferences) }
        val engine = SystemTtsEngine.open(context, resolver, voiceSelector, initialPreferences)
            ?: return Try.failure(DebugError("No text-to-speech engine could be bound."))
        return Try.success(engine)
    }

    /** Readium's settings resolution: the preference, else the book's language, else the defaults, else the device locale. */
    private fun settings(metadata: Metadata, preferences: AndroidTtsPreferences): AndroidTtsSettings {
        val language = preferences.language ?: metadata.language ?: defaults.language ?: Language(Locale.getDefault())
        return AndroidTtsSettings(
            language = language,
            voices = preferences.voices ?: emptyMap(),
            pitch = preferences.pitch ?: defaults.pitch ?: 1.0,
            speed = preferences.speed ?: defaults.speed ?: 1.0,
            overrideContentLanguage = preferences.language != null,
        )
    }

    override fun createPreferencesEditor(publication: Publication, initialPreferences: AndroidTtsPreferences): AndroidTtsPreferencesEditor =
        delegate.createPreferencesEditor(publication, initialPreferences)

    override fun createEmptyPreferences(): AndroidTtsPreferences = delegate.createEmptyPreferences()

    override fun getPlaybackParameters(settings: AndroidTtsSettings): PlaybackParameters = delegate.getPlaybackParameters(settings)

    override fun updatePlaybackParameters(previousPreferences: AndroidTtsPreferences, playbackParameters: PlaybackParameters): AndroidTtsPreferences =
        delegate.updatePlaybackParameters(previousPreferences, playbackParameters)

    override fun mapEngineError(error: AndroidTtsEngine.Error): PlaybackException = delegate.mapEngineError(error)
}
