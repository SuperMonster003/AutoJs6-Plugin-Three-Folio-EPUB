package io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.BuildConfig
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationDatabase
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ActivitySettingsBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.DialogRateBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ItemSettingHeaderBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ItemSettingRowBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBooksStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.StoredPreferences
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.FontStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.TapZones
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.SleepTimer
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.update.AppUpdateCoordinator
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.update.AppUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import java.io.File
import kotlin.math.roundToInt

/**
 * The standalone settings page (roadmap P4.3), reached from the launcher menu and the reader
 * overflow. It edits what the reader reads back on its next start: the theme mode of the shared
 * preferences file (the rest of the Readium preferences stay with the panel inside the reader,
 * D14), the [ReaderSettings] toggles, the read-aloud speed and pitch of [TtsPreferencesStore]
 * and the sleep timer every session starts with. The data section empties the plugin's own
 * stores (progress and bookmarks, the recent list with its covers and grants, imported fonts,
 * preferences) after a confirmation; the about section links to the project pages, the built-in
 * release history and the manual update check (D28). Not exported and takes no data.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class SettingsActivity : HostAppearanceActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: ReaderSettings
    private val preferencesStore by lazy { ReaderPreferencesStore.forFilesDirectory(filesDir) }
    private val ttsStore by lazy { TtsPreferencesStore.forFilesDirectory(filesDir) }
    private val bookDataStore by lazy { BookDataStore.forFilesDirectory(filesDir) }
    private val recentBooksStore by lazy { RecentBooksStore.forFilesDirectory(filesDir) }
    private val fontStore by lazy { FontStore.forFilesDirectory(filesDir) }
    private val annotationDatabase by lazy { AnnotationDatabase.get(this) }
    private var annotationsRow: ItemSettingRowBinding? = null
    private var annotationUsage: String? = null
    private val refreshers = ArrayList<() -> Unit>()
    private var dialog: AlertDialog? = null

    /** The update flow; the instrumentation test drives and observes it. */
    internal lateinit var updates: AppUpdateCoordinator
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = ReaderSettings(this)
        updates = AppUpdateCoordinator(this, settings, packageInfo().versionName).also { it.onChanged = ::refresh }
        binding.toolbar.setNavigationOnClickListener { finish() }
        fitToolbarAndContent(binding.root, binding.toolbar, binding.content)
        buildRows()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        updates.dismiss()
        super.onDestroy()
    }

    // ---- rows ----

    private fun buildRows() {
        header(R.string.text_settings_reading)
        row(R.string.text_settings_theme, summary = { getString(themeLabel(currentThemeMode())) }) { chooseThemeMode() }
        row(R.string.text_settings_reset_reading, summary = { getString(R.string.text_settings_reading_hint) }) {
            confirm(R.string.text_settings_reset_reading, R.string.text_settings_reset_reading_message) { resetReadingPreferences() }
        }

        header(R.string.text_settings_page_turning)
        row(R.string.text_tap_zones, summary = { getString(tapZonesLabel(settings.tapZones)) }) { chooseTapZones() }
        switchRow(R.string.text_volume_keys_turn_pages, read = { settings.volumeKeysTurnPages }) { settings.volumeKeysTurnPages = it }

        header(R.string.text_settings_read_aloud)
        row(R.string.text_read_aloud_speed, summary = { percent(ttsPreferences().speed) }) {
            chooseRate(R.string.text_read_aloud_speed, MAX_SPEED_PERCENT, { it.speed }, ::setReadAloudSpeed)
        }
        row(R.string.text_read_aloud_pitch, summary = { percent(ttsPreferences().pitch) }) {
            chooseRate(R.string.text_read_aloud_pitch, MAX_PITCH_PERCENT, { it.pitch }, ::setReadAloudPitch)
        }
        row(R.string.text_settings_default_sleep_timer, summary = { sleepTimerLabel(settings.readAloudSleepTimer) }) { chooseSleepTimer() }
        switchRow(R.string.text_read_aloud_keep_screen_on, read = { settings.readAloudKeepScreenOn }) { settings.readAloudKeepScreenOn = it }
        switchRow(
            R.string.text_read_aloud_background,
            R.string.text_read_aloud_background_hint,
            read = { settings.readAloudInBackground },
        ) { settings.readAloudInBackground = it }

        header(R.string.text_settings_links)
        switchRow(R.string.text_external_links_direct, read = { settings.externalLinksDirect }) { settings.externalLinksDirect = it }

        header(R.string.text_settings_data)
        row(R.string.text_settings_clear_progress, summary = {
            usage(bookDataStore.bookKeys().size, directorySize(File(filesDir, BookDataStore.DIRECTORY_NAME)), R.string.text_settings_usage_books)
        }) {
            confirm(R.string.text_settings_clear_progress, R.string.text_settings_clear_progress_message) { clearProgress() }
        }
        annotationsRow = row(R.string.text_settings_clear_annotations, summary = { annotationUsage }) {
            confirm(R.string.text_settings_clear_annotations, R.string.text_settings_clear_annotations_message) { clearAnnotations() }
        }
        row(R.string.text_settings_clear_recent, summary = {
            usage(recentBooksStore.read().size, recentBooksStore.usageBytes(), R.string.text_settings_usage_books)
        }) {
            confirm(R.string.text_settings_clear_recent, R.string.text_settings_clear_recent_message) { clearRecentBooks() }
        }
        row(R.string.text_settings_clear_fonts, summary = {
            val fonts = fontStore.read().fonts
            usage(fonts.size, fonts.sumOf { it.bytes }, R.string.text_settings_usage_fonts)
        }) {
            confirm(R.string.text_settings_clear_fonts, R.string.text_settings_clear_fonts_message) { clearFonts() }
        }
        row(R.string.text_settings_clear_preferences) {
            confirm(R.string.text_settings_clear_preferences, R.string.text_settings_clear_preferences_message) { clearPreferences() }
        }

        header(R.string.text_settings_about)
        row(R.string.text_settings_version, summary = { versionSummary() })
        row(R.string.text_settings_readium, summary = { BuildConfig.READIUM_VERSION })
        row(R.string.text_settings_author, summary = { getString(R.string.plugin_author) }) { openUrl(AUTHOR_PAGE) }
        row(R.string.text_settings_license, summary = { getString(R.string.text_settings_license_summary) }) { openUrl(LICENSE_PAGE) }
        row(R.string.text_settings_third_party) { openUrl(THIRD_PARTY_PAGE) }
        row(R.string.text_settings_source, summary = { getString(R.string.text_settings_source_summary) }) { openUrl(SOURCE_PAGE) }
        row(R.string.release_history) { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) }
        row(R.string.text_settings_check_update, summary = { updateSummary() }) { checkForUpdates() }
    }

    private fun header(@StringRes title: Int) {
        ItemSettingHeaderBinding.inflate(layoutInflater, binding.content, true).headerTitle.setText(title)
    }

    /** One row; its root carries the title resource as tag so tests can find it. */
    private fun row(@StringRes title: Int, summary: () -> CharSequence? = { null }, onClick: (() -> Unit)? = null): ItemSettingRowBinding {
        val row = ItemSettingRowBinding.inflate(layoutInflater, binding.content, true)
        row.rowTitle.setText(title)
        row.root.tag = title
        if (onClick != null) row.root.setOnClickListener { onClick() } else row.root.isClickable = false
        refreshers += {
            val text = summary()
            row.rowSummary.text = text
            row.rowSummary.isVisible = !text.isNullOrEmpty()
        }
        return row
    }

    private fun switchRow(@StringRes title: Int, @StringRes summary: Int? = null, read: () -> Boolean, write: (Boolean) -> Unit) {
        val row = row(title, summary = { summary?.let(::getString) }) {}
        row.rowSwitch.isVisible = true
        row.root.setOnClickListener {
            write(!read())
            refresh()
        }
        refreshers += { row.rowSwitch.isChecked = read() }
    }

    private fun refresh() {
        if (isFinishing || isDestroyed) return
        refreshers.forEach { it() }
        refreshAnnotationUsage()
    }

    /** The highlights live in Room (roadmap P9), so their summary arrives a moment after the others. */
    private fun refreshAnnotationUsage() {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                usage(annotationDatabase.annotations().countAll(), AnnotationDatabase.sizeOnDisk(this@SettingsActivity), R.string.text_settings_usage_annotations)
            }
            annotationUsage = text
            annotationsRow?.rowSummary?.apply {
                this.text = text
                isVisible = text.isNotEmpty()
            }
        }
    }

    // ---- reading ----

    private fun currentThemeMode(): ThemeMode = preferencesStore.read()?.themeMode ?: ThemeMode.DEFAULT

    private fun chooseThemeMode() {
        val modes = ThemeMode.entries
        choose(R.string.text_settings_theme, modes.map { getString(themeLabel(it)) }, modes.indexOf(currentThemeMode())) { setThemeMode(modes[it]) }
    }

    /** Writes the mode next to whatever Readium preferences the file holds; the reader reloads it on its next start. */
    internal fun setThemeMode(mode: ThemeMode) = io {
        val current = preferencesStore.read()
        if (current == null && mode == ThemeMode.DEFAULT) return@io
        preferencesStore.write(StoredPreferences(mode, current?.readium ?: JSONObject()))
    }

    private fun resetReadingPreferences() = io(cleared = true) { preferencesStore.clear() }

    // ---- page turning and read-aloud ----

    private fun chooseTapZones() {
        val zones = TapZones.entries
        choose(R.string.text_tap_zones, zones.map { getString(tapZonesLabel(it)) }, zones.indexOf(settings.tapZones)) { setTapZones(zones[it]) }
    }

    internal fun setTapZones(zones: TapZones) {
        settings.tapZones = zones
        refresh()
    }

    private fun chooseSleepTimer() {
        val timers = SleepTimer.entries
        choose(R.string.text_settings_default_sleep_timer, timers.map(::sleepTimerLabel), timers.indexOf(settings.readAloudSleepTimer)) {
            setDefaultSleepTimer(timers[it])
        }
    }

    internal fun setDefaultSleepTimer(timer: SleepTimer) {
        settings.readAloudSleepTimer = timer
        refresh()
    }

    private fun chooseRate(@StringRes title: Int, maxPercent: Int, read: (AndroidTtsPreferences) -> Double?, commit: (Int) -> Unit) {
        val view = DialogRateBinding.inflate(layoutInflater)
        view.slider.valueFrom = MIN_RATE_PERCENT.toFloat()
        view.slider.valueTo = maxPercent.toFloat()
        view.slider.stepSize = RATE_STEP.toFloat()
        val current = ((read(ttsPreferences()) ?: DEFAULT_RATE) * PERCENT).roundToInt()
        view.slider.value = ((current.toDouble() / RATE_STEP).roundToInt() * RATE_STEP).coerceIn(MIN_RATE_PERCENT, maxPercent).toFloat()
        view.value.text = getString(R.string.text_percent_value, view.slider.value.roundToInt())
        view.slider.addOnChangeListener { _, value, _ -> view.value.text = getString(R.string.text_percent_value, value.roundToInt()) }
        dialog?.dismiss()
        dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view.root)
            .setPositiveButton(android.R.string.ok) { _, _ -> commit(view.slider.value.roundToInt()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    internal fun setReadAloudSpeed(percent: Int) = io { ttsStore.write(ttsPreferences().copy(speed = rate(percent, MAX_SPEED_PERCENT))) }

    internal fun setReadAloudPitch(percent: Int) = io { ttsStore.write(ttsPreferences().copy(pitch = rate(percent, MAX_PITCH_PERCENT))) }

    private fun rate(percent: Int, maxPercent: Int): Double = percent.coerceIn(MIN_RATE_PERCENT, maxPercent) / PERCENT

    private fun ttsPreferences(): AndroidTtsPreferences = ttsStore.read() ?: AndroidTtsPreferences()

    private fun percent(rate: Double?): String = getString(R.string.text_percent_value, ((rate ?: DEFAULT_RATE) * PERCENT).roundToInt())

    // ---- data ----

    internal fun clearProgress() = io(cleared = true) { bookDataStore.clearAll() }

    /** Every highlight and note of every book (roadmap P9); the reader's live list follows through Room. */
    internal fun clearAnnotations() = io(cleared = true) { annotationDatabase.annotations().deleteEverything() }

    /** Forgets the list and the covers, and gives the persistable grants back (roadmap D4). */
    internal fun clearRecentBooks() = io(cleared = true) {
        recentBooksStore.clear().forEach { book ->
            runCatching { contentResolver.releasePersistableUriPermission(book.uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    internal fun clearFonts() = io(cleared = true) { fontStore.clear() }

    internal fun clearPreferences() = io(cleared = true) {
        preferencesStore.clear()
        ttsStore.clear()
        settings.clearAll()
    }

    private fun usage(count: Int, bytes: Long, @StringRes format: Int): String =
        if (count == 0) getString(R.string.text_settings_nothing_stored)
        else getString(format, count, Formatter.formatShortFileSize(this, bytes))

    private fun directorySize(directory: File): Long =
        directory.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    // ---- about ----

    private fun versionSummary(): String {
        val info = packageInfo()
        return getString(
            R.string.text_settings_version_summary,
            info.versionName ?: "?",
            PackageInfoCompat.getLongVersionCode(info),
            getString(R.string.plugin_version_date),
        )
    }

    private fun packageInfo(): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }

    private fun updateSummary(): String {
        val checked = settings.lastUpdateCheckAt
        val base = if (checked == null) {
            getString(R.string.text_settings_update_never_checked)
        } else {
            val relative = DateUtils.getRelativeTimeSpanString(checked, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            getString(R.string.text_settings_update_last_checked, relative)
        }
        val ignored = settings.ignoredUpdateVersion ?: return base
        return base + "\n" + getString(R.string.text_settings_update_ignoring, ignored)
    }

    internal fun checkForUpdates() = updates.check()

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            toast(R.string.text_no_app_for_action)
        }
    }

    // ---- helpers ----

    private fun choose(@StringRes title: Int, labels: List<String>, checked: Int, onChosen: (Int) -> Unit) {
        dialog?.dismiss()
        dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { shown, which ->
                shown.dismiss()
                onChosen(which)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirm(@StringRes title: Int, @StringRes message: Int, action: () -> Unit) {
        dialog?.dismiss()
        dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> action() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Runs the store work off the main thread, then refreshes the summaries (and says so when [cleared]). */
    private fun io(cleared: Boolean = false, block: suspend () -> Unit) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { block() }
            if (cleared) toast(R.string.text_settings_cleared)
            refresh()
        }
    }

    private fun toast(@StringRes text: Int) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    @StringRes
    private fun themeLabel(mode: ThemeMode): Int = when (mode) {
        ThemeMode.LIGHT -> R.string.text_theme_light
        ThemeMode.SEPIA -> R.string.text_theme_sepia
        ThemeMode.DARK -> R.string.text_theme_dark
        ThemeMode.HOST -> R.string.text_theme_host
    }

    @StringRes
    private fun tapZonesLabel(zones: TapZones): Int = when (zones) {
        TapZones.OFF -> R.string.text_tap_zones_off
        TapZones.HORIZONTAL -> R.string.text_tap_zones_horizontal
        TapZones.VERTICAL -> R.string.text_tap_zones_vertical
    }

    private fun sleepTimerLabel(timer: SleepTimer): String = when (timer) {
        SleepTimer.OFF -> getString(R.string.text_read_aloud_sleep_off)
        SleepTimer.END_OF_CHAPTER -> getString(R.string.text_read_aloud_sleep_chapter)
        else -> getString(R.string.text_read_aloud_sleep_minutes, timer.minutes ?: 0)
    }

    companion object {
        private const val PERCENT = 100.0
        private const val DEFAULT_RATE = 1.0
        private const val MIN_RATE_PERCENT = 50
        private const val MAX_SPEED_PERCENT = 300
        private const val MAX_PITCH_PERCENT = 200
        private const val RATE_STEP = 10
        private const val AUTHOR_PAGE = "https://github.com/SuperMonster003"
        private const val SOURCE_PAGE = "https://github.com/${AppUpdateRepository.REPOSITORY}"
        private const val LICENSE_PAGE = "$SOURCE_PAGE/blob/master/LICENSE"
        private const val THIRD_PARTY_PAGE = "$SOURCE_PAGE/blob/master/THIRD_PARTY_NOTICES.md"
    }
}
