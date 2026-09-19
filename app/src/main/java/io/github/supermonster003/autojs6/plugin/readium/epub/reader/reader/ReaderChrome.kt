package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.app.Activity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ActivityEpubReaderBinding

/**
 * Toolbar, progress bar and immersive mode of the reader (roadmap P1.2). Immersive mode hides the
 * chrome and the system bars, exactly like the sibling previewers' fullscreen mode; the reader
 * itself never moves, so toggling is instant and keeps the locator.
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

    fun showProgress(snapshot: ProgressSnapshot) {
        binding.progressBar.max = PROGRESS_SCALE
        binding.progressBar.progress = snapshot.percent * PROGRESS_SCALE / 100
        val resources = activity.resources
        binding.progressText.text = if (snapshot.hasPosition) {
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

    private fun applyVisibility() {
        binding.toolbar.isVisible = !immersive
        binding.progressPanel.isVisible = readerVisible && !immersive
    }

    private fun applySystemBars() {
        val window = activity.window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (immersive) {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            } else {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    companion object {
        private const val PROGRESS_SCALE = 1000
    }
}
