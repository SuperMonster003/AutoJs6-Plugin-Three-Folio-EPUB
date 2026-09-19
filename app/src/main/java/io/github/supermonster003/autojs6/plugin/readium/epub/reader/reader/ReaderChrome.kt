package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.Menu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ActivityEpubReaderBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ChromeColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ReaderTheme
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ReaderThemeColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsStatus

/**
 * Toolbar, progress bar, immersive mode and colours of the reader (roadmap P1.2 / P2.1).
 *
 * The window draws edge to edge on every API level: the toolbar extends under the status bar and
 * the progress panel under the navigation bar, both painted in the chrome colour of the current
 * [ReaderTheme], so the bars never flash a foreign colour when immersive mode toggles or the theme
 * changes. Immersive mode hides the chrome and the system bars, exactly like the sibling
 * previewers' fullscreen mode; the reader itself never moves, so toggling keeps the locator.
 * The search bar (P2.5) sits above the progress panel while a search hit is open and follows the
 * same colours and immersive rule; the read-aloud bar (P3) does the same while a voice reads.
 */
internal class ReaderChrome(
    private val activity: Activity,
    private val binding: ActivityEpubReaderBinding,
) {

    var immersive: Boolean = false
        private set

    /** The progress bar is only meaningful once the book renders. */
    var readerVisible: Boolean = false
        set(value) {
            field = value
            applyVisibility()
        }

    /** True while a search hit is open; the bar shows only together with the rest of the chrome. */
    var searchBarVisible: Boolean = false
        private set

    /** True while read-aloud runs; its bar shows only together with the rest of the chrome. */
    var readAloudVisible: Boolean = false
        private set

    var theme: ReaderTheme = ReaderTheme.LIGHT
        private set

    var colors: ChromeColors = ReaderThemeColors.forTheme(theme)
        private set

    private val actionBarSize: Int = TypedValue().let { value ->
        activity.theme.resolveAttribute(android.R.attr.actionBarSize, value, true)
        TypedValue.complexToDimensionPixelSize(value.data, activity.resources.displayMetrics)
    }
    private val statusPanelPaddingBottom = binding.statusPanel.paddingBottom

    init {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            root.updatePadding(left = bars.left, right = bars.right)
            binding.toolbar.updatePadding(top = bars.top)
            binding.toolbar.updateLayoutParams { height = actionBarSize + bars.top }
            binding.progressPanel.updatePadding(bottom = bars.bottom)
            binding.statusPanel.updatePadding(bottom = statusPanelPaddingBottom + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        applyTheme(theme)
    }

    fun setImmersive(enabled: Boolean) {
        immersive = enabled
        applyVisibility()
        applySystemBars()
    }

    fun toggleImmersive() = setImmersive(!immersive)

    /** Re-hides the system bars when the window regains focus (dialogs, app switches). */
    fun onWindowFocusGained() {
        if (immersive) applySystemBars()
    }

    fun setBookTitle(title: CharSequence?) {
        binding.toolbar.title = title
    }

    fun setChapterTitle(title: CharSequence?) {
        binding.toolbar.subtitle = title
    }

    fun setSearchBarListeners(onPrevious: () -> Unit, onNext: () -> Unit, onClose: () -> Unit) {
        binding.searchPrevious.setOnClickListener { onPrevious() }
        binding.searchNext.setOnClickListener { onNext() }
        binding.searchClose.setOnClickListener { onClose() }
    }

    /** Shows `x of N` for the open hit ([index] is zero-based), or hides the bar when [index] is null. */
    fun showSearchPosition(index: Int?, count: Int) {
        searchBarVisible = index != null && count > 0
        if (index != null) {
            binding.searchPosition.text = activity.getString(R.string.text_search_position, index + 1, count)
            setEnabled(binding.searchPrevious, index > 0)
            setEnabled(binding.searchNext, index < count - 1)
        }
        applyVisibility()
    }

    fun setReadAloudListeners(
        onPrevious: () -> Unit,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onSettings: () -> Unit,
        onStop: () -> Unit,
    ) {
        binding.ttsPrevious.setOnClickListener { onPrevious() }
        binding.ttsPlayPause.setOnClickListener { onPlayPause() }
        binding.ttsNext.setOnClickListener { onNext() }
        binding.ttsSettings.setOnClickListener { onSettings() }
        binding.ttsStop.setOnClickListener { onStop() }
    }

    /** Shows the read-aloud bar for every status but idle; the controls wait while the engine starts. */
    fun showReadAloud(status: TtsStatus) {
        readAloudVisible = status != TtsStatus.IDLE
        val playing = status == TtsStatus.PLAYING
        binding.ttsPlayPause.setImageResource(if (playing) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24)
        binding.ttsPlayPause.contentDescription =
            activity.getString(if (playing) R.string.text_read_aloud_pause else R.string.text_read_aloud_play)
        binding.ttsStatus.setText(
            when (status) {
                TtsStatus.STARTING -> R.string.text_read_aloud_starting
                TtsStatus.PLAYING -> R.string.text_read_aloud_playing
                else -> R.string.text_read_aloud_paused
            },
        )
        val controls = status != TtsStatus.STARTING
        setEnabled(binding.ttsPrevious, controls)
        setEnabled(binding.ttsPlayPause, controls)
        setEnabled(binding.ttsNext, controls)
        setEnabled(binding.ttsSettings, controls)
        applyVisibility()
    }

    private fun setEnabled(button: android.view.View, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    fun showProgress(snapshot: ProgressSnapshot) {
        binding.progressBar.max = PROGRESS_SCALE
        binding.progressBar.progress = snapshot.percent * PROGRESS_SCALE / 100
        val resources = activity.resources
        binding.progressText.text = if (snapshot.hasPosition && snapshot.pages) {
            resources.getString(R.string.text_progress_page_of, snapshot.position, snapshot.positionCount)
        } else if (snapshot.hasPosition) {
            resources.getString(
                R.string.text_progress_position_and_percent,
                snapshot.position,
                snapshot.positionCount,
                snapshot.percent,
            )
        } else {
            resources.getString(R.string.text_progress_percent, snapshot.percent)
        }
    }

    /** Recolours the chrome, the loading panel, the window and the system bars for [theme]. */
    fun applyTheme(theme: ReaderTheme) {
        this.theme = theme
        colors = ReaderThemeColors.forTheme(theme)
        activity.window.setBackgroundDrawable(theme.backgroundColor.toDrawable())
        binding.root.setBackgroundColor(theme.backgroundColor)
        binding.readerContainer.setBackgroundColor(theme.backgroundColor)
        binding.statusText.setTextColor(theme.contentColor)
        binding.statusProgress.indeterminateTintList = ColorStateList.valueOf(colors.accent)
        binding.toolbar.setBackgroundColor(colors.background)
        binding.toolbar.setTitleTextColor(colors.foreground)
        binding.toolbar.setSubtitleTextColor(colors.secondaryForeground)
        binding.progressPanel.setBackgroundColor(colors.background)
        binding.progressBar.progressTintList = ColorStateList.valueOf(colors.accent)
        binding.progressBar.progressBackgroundTintList =
            ColorStateList.valueOf(ReaderThemeColors.withAlpha(colors.foreground, TRACK_ALPHA))
        binding.progressText.setTextColor(colors.secondaryForeground)
        binding.searchBar.setBackgroundColor(colors.background)
        binding.searchPosition.setTextColor(colors.foreground)
        val iconTint = ColorStateList.valueOf(colors.foreground)
        binding.searchPrevious.imageTintList = iconTint
        binding.searchNext.imageTintList = iconTint
        binding.searchClose.imageTintList = iconTint
        binding.ttsBar.setBackgroundColor(colors.background)
        binding.ttsStatus.setTextColor(colors.foreground)
        binding.ttsPrevious.imageTintList = iconTint
        binding.ttsPlayPause.imageTintList = iconTint
        binding.ttsNext.imageTintList = iconTint
        binding.ttsSettings.imageTintList = iconTint
        binding.ttsStop.imageTintList = iconTint
        tintToolbarIcons(binding.toolbar.menu)
        applySystemBars()
    }

    /** Menu icons are inflated after the theme: the Activity calls this again from `onPrepareOptionsMenu`. */
    fun tintToolbarIcons(menu: Menu?) {
        val tint = colors.foreground
        binding.toolbar.navigationIcon = binding.toolbar.navigationIcon?.mutate()?.apply { setTint(tint) }
        binding.toolbar.overflowIcon = binding.toolbar.overflowIcon?.mutate()?.apply { setTint(tint) }
        if (menu == null) return
        for (index in 0 until menu.size) {
            val item = menu[index]
            item.icon = item.icon?.mutate()?.apply { setTint(tint) }
        }
    }

    private fun applyVisibility() {
        binding.toolbar.isVisible = !immersive
        binding.searchBar.isVisible = readerVisible && searchBarVisible && !immersive
        binding.ttsBar.isVisible = readerVisible && readAloudVisible && !immersive
        binding.progressPanel.isVisible = readerVisible && !immersive
    }

    private fun applySystemBars() {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = colors.lightBars
        controller.isAppearanceLightNavigationBars = colors.lightBars
        // API 24 and 25 cannot draw dark navigation bar icons: keep the classic dark bar there.
        val legacyLightBars = colors.lightBars && Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        setBarColors(if (legacyLightBars) LEGACY_NAVIGATION_BAR else Color.TRANSPARENT)
        if (immersive) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    // Both setters are no-ops from API 35 on, where edge-to-edge is enforced and the bars are transparent.
    @Suppress("DEPRECATION")
    private fun setBarColors(navigationBar: Int) {
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = navigationBar
    }

    companion object {
        private const val PROGRESS_SCALE = 1000
        private const val DISABLED_ALPHA = 0.38f
        private const val TRACK_ALPHA = 0.12
        private const val LEGACY_NAVIGATION_BAR = 0xFF000000.toInt()
    }
}
