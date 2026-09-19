package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.ReaderPreferencesState
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.SheetPreferencesBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.PreferenceRanges
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import kotlin.math.roundToInt

/**
 * The reading preferences panel (roadmap P2.1): a bottom sheet whose controls show the settings
 * the navigator resolved and write back Readium preferences through the [EpubReaderActivity]. Every change applies
 * immediately; sliders commit when the finger lifts so a drag does not reflow the book ten times.
 *
 * Readium CSS ignores line height, paragraph spacing, alignment and hyphenation while publisher
 * styles are on, so touching one of them turns publisher styles off instead of silently doing
 * nothing; the hint under the switch explains that rule.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class PreferencesSheet : BottomSheetDialogFragment() {

    private var _binding: SheetPreferencesBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val host: EpubReaderActivity get() = requireActivity() as EpubReaderActivity

    /** True while the controls are being set from state, so their listeners stay quiet. */
    private var rendering = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SheetPreferencesBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setUpControls()
        val settings = host.navigatorSettings
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val flow = if (settings != null) {
                    combine(host.preferencesState, settings) { state, resolved -> state to resolved }
                } else {
                    host.preferencesState.map { it to null }
                }
                flow.collect { (state, resolved) -> render(state, resolved) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setUpControls() = with(binding) {
        themeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !rendering) host.setThemeMode(themeModeFor(checkedId))
        }
        overflowGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !rendering) host.editPreferences { it.copy(scroll = checkedId == R.id.overflow_scrolled) }
        }

        fontSizeDecrease.setOnClickListener { stepFontSize(-1) }
        fontSizeIncrease.setOnClickListener { stepFontSize(+1) }
        bindSlider(fontSizeSlider, fontSizeValue) { value -> host.editPreferences { it.copy(fontSize = value) } }

        fontFamily.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            FONT_FAMILY_LABELS.map { getString(it) },
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        fontFamily.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (rendering) return
                val family = FONT_FAMILIES[position]
                if (family != currentFontFamily()) host.editPreferences { it.copy(fontFamily = family) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        bindSlider(lineHeightSlider, lineHeightValue) { value -> editAdvanced { it.copy(lineHeight = value) } }
        bindSlider(pageMarginsSlider, pageMarginsValue) { value -> host.editPreferences { it.copy(pageMargins = value) } }
        bindSlider(paragraphSpacingSlider, paragraphSpacingValue) { value -> editAdvanced { it.copy(paragraphSpacing = value) } }

        textAlignGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || rendering) return@addOnButtonCheckedListener
            val align = when (checkedId) {
                R.id.text_align_start -> TextAlign.START
                R.id.text_align_justify -> TextAlign.JUSTIFY
                else -> null
            }
            editAdvanced { it.copy(textAlign = align) }
        }
        hyphensSwitch.setOnCheckedChangeListener { _, checked ->
            if (!rendering) editAdvanced { it.copy(hyphens = checked) }
        }
        publisherStylesSwitch.setOnCheckedChangeListener { _, checked ->
            if (!rendering) host.editPreferences { it.copy(publisherStyles = checked) }
        }
        columnCountGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || rendering) return@addOnButtonCheckedListener
            val columns = when (checkedId) {
                R.id.column_count_one -> ColumnCount.ONE
                R.id.column_count_two -> ColumnCount.TWO
                else -> ColumnCount.AUTO
            }
            host.editPreferences { it.copy(columnCount = columns) }
        }
        resetButton.setOnClickListener { host.resetPreferences() }
    }

    /** Sliders show their value live and commit once the drag ends. */
    private fun bindSlider(slider: Slider, label: TextView, commit: (Double) -> Unit) {
        slider.setLabelFormatter { percent(it) }
        slider.addOnChangeListener { _, value, fromUser -> if (fromUser) label.text = percent(value) }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit
            override fun onStopTrackingTouch(slider: Slider) = commit(slider.value / PERCENT)
        })
    }

    /** Preferences Readium CSS only honours with publisher styles off: switch them off along the way. */
    private fun editAdvanced(transform: (EpubPreferences) -> EpubPreferences) {
        host.editPreferences { transform(it).copy(publisherStyles = false) }
    }

    private fun stepFontSize(delta: Int) {
        val current = host.navigatorSettings?.value?.fontSize ?: host.preferencesState.value.epub.fontSize ?: 1.0
        val next = PreferenceRanges.step(PreferenceRanges.FONT_SIZE, PreferenceRanges.FONT_SIZE_STEP, current, delta)
        host.editPreferences { it.copy(fontSize = next) }
    }

    private fun currentFontFamily(): FontFamily? =
        host.navigatorSettings?.value?.fontFamily ?: host.preferencesState.value.epub.fontFamily

    private fun render(state: ReaderPreferencesState, settings: EpubSettings?) = with(binding) {
        rendering = true
        try {
            val fixed = host.fixedLayout
            themeGroup.check(themeButtonFor(state.themeMode))

            val scroll = settings?.scroll ?: state.epub.scroll ?: false
            overflowGroup.check(if (scroll) R.id.overflow_scrolled else R.id.overflow_paged)
            setGroupEnabled(overflowGroup, !fixed)

            reflowableGroup.isVisible = !fixed
            fixedLayoutHint.isVisible = fixed

            setSlider(fontSizeSlider, fontSizeValue, settings?.fontSize ?: state.epub.fontSize ?: 1.0)
            val family = settings?.fontFamily ?: state.epub.fontFamily
            fontFamily.setSelection(FONT_FAMILIES.indexOf(family).coerceAtLeast(0), false)

            setSlider(lineHeightSlider, lineHeightValue, settings?.lineHeight ?: state.epub.lineHeight ?: DEFAULT_LINE_HEIGHT)
            setSlider(pageMarginsSlider, pageMarginsValue, settings?.pageMargins ?: state.epub.pageMargins ?: DEFAULT_PAGE_MARGINS)
            setSlider(
                paragraphSpacingSlider,
                paragraphSpacingValue,
                settings?.paragraphSpacing ?: state.epub.paragraphSpacing ?: DEFAULT_PARAGRAPH_SPACING,
            )

            val align = settings?.textAlign ?: state.epub.textAlign
            textAlignGroup.check(
                when (align) {
                    null -> R.id.text_align_publisher
                    TextAlign.JUSTIFY -> R.id.text_align_justify
                    else -> R.id.text_align_start
                },
            )
            hyphensSwitch.isChecked = settings?.hyphens ?: state.epub.hyphens ?: false

            val publisherStyles = settings?.publisherStyles ?: state.epub.publisherStyles ?: true
            publisherStylesSwitch.isChecked = publisherStyles
            publisherStylesHint.isVisible = publisherStyles
            advancedGroup.alpha = if (publisherStyles) INEFFECTIVE_ALPHA else 1f

            val columns = settings?.columnCount ?: state.epub.columnCount ?: ColumnCount.AUTO
            columnCountGroup.check(
                when (columns) {
                    ColumnCount.ONE -> R.id.column_count_one
                    ColumnCount.TWO -> R.id.column_count_two
                    ColumnCount.AUTO -> R.id.column_count_auto
                },
            )
            setGroupEnabled(columnCountGroup, !scroll)
        } finally {
            rendering = false
        }
    }

    private fun setSlider(slider: Slider, label: TextView, value: Double) {
        val steps = ((value * PERCENT - slider.valueFrom) / slider.stepSize).roundToInt()
        val snapped = (slider.valueFrom + steps * slider.stepSize).coerceIn(slider.valueFrom, slider.valueTo)
        slider.value = snapped
        label.text = percent(snapped)
    }

    private fun setGroupEnabled(group: ViewGroup, enabled: Boolean) {
        group.isEnabled = enabled
        for (index in 0 until group.childCount) group.getChildAt(index).isEnabled = enabled
        group.alpha = if (enabled) 1f else INEFFECTIVE_ALPHA
    }

    private fun percent(value: Float): String = getString(R.string.text_percent_value, value.roundToInt())

    private fun themeModeFor(buttonId: Int): ThemeMode = when (buttonId) {
        R.id.theme_light -> ThemeMode.LIGHT
        R.id.theme_sepia -> ThemeMode.SEPIA
        R.id.theme_dark -> ThemeMode.DARK
        else -> ThemeMode.HOST
    }

    private fun themeButtonFor(mode: ThemeMode): Int = when (mode) {
        ThemeMode.LIGHT -> R.id.theme_light
        ThemeMode.SEPIA -> R.id.theme_sepia
        ThemeMode.DARK -> R.id.theme_dark
        ThemeMode.HOST -> R.id.theme_host
    }

    companion object {
        const val TAG = "reader-preferences"

        private const val PERCENT = 100.0
        private const val INEFFECTIVE_ALPHA = 0.5f

        /** Shown while the preference is unset; Readium CSS then leaves the publisher's values. */
        private const val DEFAULT_LINE_HEIGHT = 1.5
        private const val DEFAULT_PAGE_MARGINS = 1.0
        private const val DEFAULT_PARAGRAPH_SPACING = 0.0

        /** Generic CSS families plus the accessibility fonts Readium bundles; index 0 is the publisher's. */
        val FONT_FAMILIES: List<FontFamily?> = listOf(
            null,
            FontFamily.SERIF,
            FontFamily.SANS_SERIF,
            FontFamily.MONOSPACE,
            FontFamily.OPEN_DYSLEXIC,
            FontFamily.ACCESSIBLE_DFA,
            FontFamily.IA_WRITER_DUOSPACE,
        )

        private val FONT_FAMILY_LABELS = listOf(
            R.string.text_preferences_publisher_default,
            R.string.text_font_family_serif,
            R.string.text_font_family_sans_serif,
            R.string.text_font_family_monospace,
            R.string.text_font_family_open_dyslexic,
            R.string.text_font_family_accessible_dfa,
            R.string.text_font_family_ia_writer_duospace,
        )

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            PreferencesSheet().show(fragmentManager, TAG)
        }
    }
}
