package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.SheetTtsBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The read-aloud settings panel (roadmap P3): speed and pitch sliders (committed when the finger
 * lifts, like the reading preferences), the language the engine should speak (automatic = the
 * sentence's own language, which is the book's unless a passage says otherwise) and the voice for
 * that language, plus shortcuts to the system text-to-speech settings and the voice-data installer;
 * the sleep timer (fixed spans or the end of the chapter, with the time left), keep-screen-on and
 * "continue in the background" (roadmap D26) sit below. Every change applies to the running
 * session at once; the preferences and the two switches are remembered across books, the timer is
 * per session.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class TtsSheet : BottomSheetDialogFragment() {

    private var _binding: SheetTtsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val host: EpubReaderActivity get() = requireActivity() as EpubReaderActivity

    /** True while the controls are being set from state, so their listeners stay quiet. */
    private var rendering = false

    /** The languages the picker rows stand for (row 0 = automatic) and the voices of the voice rows (row 0 = engine default). */
    private var languages: List<String?> = listOf(null)
    private var voices: List<VoiceOption?> = listOf(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SheetTtsBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindSlider(binding.speedSlider, binding.speedValue) { rate -> host.updateTtsPreferences { it.copy(speed = clampRate(rate)) } }
        bindSlider(binding.pitchSlider, binding.pitchValue) { rate -> host.updateTtsPreferences { it.copy(pitch = clampRate(rate)) } }
        binding.language.onItemSelectedListener = selection { position ->
            val tag = languages.getOrNull(position)
            host.updateTtsPreferences { it.copy(language = tag?.let(::Language)) }
        }
        binding.voice.onItemSelectedListener = selection { position ->
            val language = effectiveLanguage(host.ttsPreferences.value) ?: return@selection
            val voice = voices.getOrNull(position)
            host.updateTtsPreferences { preferences ->
                val key = Language(language)
                val current = preferences.voices.orEmpty()
                val next = if (voice == null) current - key else current + (key to AndroidTtsEngine.Voice.Id(voice.id))
                preferences.copy(voices = next.takeIf { it.isNotEmpty() })
            }
        }
        binding.systemSettings.setOnClickListener { host.openTtsSettings() }
        binding.installVoice.setOnClickListener { host.installTtsVoice() }
        fill(binding.sleepTimer, SleepTimer.entries.map(::sleepTimerLabel))
        binding.sleepTimer.onItemSelectedListener = selection { position -> host.setSleepTimer(SleepTimer.entries[position]) }
        binding.keepScreenOn.setOnCheckedChangeListener { _, checked -> if (!rendering) host.setReadAloudKeepScreenOn(checked) }
        binding.background.setOnCheckedChangeListener { _, checked -> if (!rendering) host.setReadAloudInBackground(checked) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(host.ttsPreferences, host.ttsStatus, host.ttsSleepTimer) { preferences, status, timer -> Triple(preferences, status, timer) }
                        .collect { (preferences, status, timer) ->
                            if (status == TtsStatus.IDLE) dismissAllowingStateLoss() else render(preferences, timer)
                        }
                }
                // The time left on a fixed timer ticks down while the sheet is open.
                while (isActive) {
                    delay(REMAINING_TICK_MILLIS)
                    if (_binding != null) renderRemaining()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun render(preferences: AndroidTtsPreferences, timer: SleepTimer) {
        rendering = true
        try {
            binding.sleepTimer.setSelection(SleepTimer.entries.indexOf(timer), false)
            renderRemaining()
            binding.keepScreenOn.isChecked = host.readAloudKeepScreenOn
            binding.background.isChecked = host.readAloudInBackground
            val speed = preferences.speed ?: DEFAULT_RATE
            val pitch = preferences.pitch ?: DEFAULT_RATE
            binding.speedSlider.value = sliderValue(speed, binding.speedSlider)
            binding.speedValue.text = percent(binding.speedSlider.value)
            binding.pitchSlider.value = sliderValue(pitch, binding.pitchSlider)
            binding.pitchValue.text = percent(binding.pitchSlider.value)

            val session = host.ttsSession
            val available = session?.voices.orEmpty()
            languages = listOf(null) + TtsVoicePolicy.languages(available)
            val chosenLanguage = preferences.language?.code?.let { TtsVoicePolicy.normalize(it) }
            fill(binding.language, languages.map { tag -> tag?.let(::displayLanguage) ?: getString(R.string.text_read_aloud_language_auto) })
            binding.language.setSelection(languages.indexOf(chosenLanguage).coerceAtLeast(0), false)

            val language = effectiveLanguage(preferences)
            voices = listOf(null) + (language?.let { TtsVoicePolicy.voicesFor(it, available) } ?: emptyList())
            val chosenVoice = language?.let { preferences.voices?.get(Language(it))?.value }
            fill(binding.voice, voices.map { voice -> voice?.let(::displayVoice) ?: getString(R.string.text_read_aloud_voice_default) })
            binding.voice.setSelection(voices.indexOfFirst { it != null && it.id == chosenVoice }.coerceAtLeast(0), false)
            binding.voice.isEnabled = voices.size > 1
        } finally {
            rendering = false
        }
    }

    private fun renderRemaining() {
        val remaining = host.ttsSession?.sleepTimerRemainingMillis()
        binding.sleepRemaining.text =
            remaining?.let { getString(R.string.text_read_aloud_sleep_remaining, SleepTimerPolicy.remainingMinutes(it)) } ?: ""
    }

    private fun sleepTimerLabel(timer: SleepTimer): String = when (timer) {
        SleepTimer.OFF -> getString(R.string.text_read_aloud_sleep_off)
        SleepTimer.END_OF_CHAPTER -> getString(R.string.text_read_aloud_sleep_chapter)
        else -> getString(R.string.text_read_aloud_sleep_minutes, timer.minutes ?: 0)
    }

    /** The language the voice picker is about: the chosen one, else the one the navigator resolved for the book. */
    private fun effectiveLanguage(preferences: AndroidTtsPreferences): String? =
        (preferences.language?.code ?: host.ttsSession?.settings?.value?.language?.code)?.let { TtsVoicePolicy.normalize(it) }

    private fun displayLanguage(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        val name = locale.getDisplayName(locale)
        return if (name.isBlank() || name == tag) tag else "$name ($tag)"
    }

    private fun displayVoice(voice: VoiceOption): String =
        if (voice.requiresNetwork) getString(R.string.text_read_aloud_voice_network, voice.id) else voice.id

    private fun fill(spinner: Spinner, labels: List<String>) {
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun selection(onSelected: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (!rendering) onSelected(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }

    private fun bindSlider(slider: Slider, label: TextView, commit: (Double) -> Unit) {
        slider.setLabelFormatter { percent(it) }
        slider.addOnChangeListener { _, value, fromUser -> if (fromUser) label.text = percent(value) }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit
            override fun onStopTrackingTouch(slider: Slider) = commit(slider.value / PERCENT)
        })
    }

    private fun sliderValue(rate: Double, slider: Slider): Float {
        val stepped = ((rate * PERCENT) / slider.stepSize).roundToInt() * slider.stepSize
        return stepped.coerceIn(slider.valueFrom, slider.valueTo)
    }

    private fun clampRate(rate: Double): Double = rate.coerceIn(MIN_RATE, MAX_RATE)

    private fun percent(value: Float): String = getString(R.string.text_percent_value, value.roundToInt())

    companion object {
        const val TAG = "reader-tts"

        private const val PERCENT = 100.0
        private const val DEFAULT_RATE = 1.0
        private const val MIN_RATE = 0.1
        private const val MAX_RATE = 4.0
        private const val REMAINING_TICK_MILLIS = 15_000L

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            TtsSheet().show(fragmentManager, TAG)
        }
    }
}
