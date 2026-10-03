package io.github.supermonster003.autojs6.plugin.three.folio.epub

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.ColorStateList
import io.github.supermonster003.autojs6.plugin.three.folio.epub.settings.LauncherIcons
import android.os.Bundle
import android.os.LocaleList
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import android.view.View
import android.view.ViewGroup
import io.github.supermonster003.autojs6.plugin.three.folio.epub.settings.AppLanguageMode
import io.github.supermonster003.autojs6.plugin.three.folio.epub.settings.AppNightMode
import io.github.supermonster003.autojs6.plugin.three.folio.epub.settings.AppPreferenceStore
import io.github.supermonster003.autojs6.plugin.three.folio.epub.theme.AppThemePalette
import io.github.supermonster003.autojs6.plugin.three.folio.epub.theme.AppThemePaletteGenerator
import io.github.supermonster003.autojs6.plugin.three.folio.epub.theme.AppThemeDialogStyler
import io.github.supermonster003.autojs6.plugin.three.folio.epub.theme.ThemePreferenceStore
import io.github.supermonster003.autojs6.plugin.three.folio.epub.theme.ThemeSourcePolicy
import java.util.Locale

/** The host's resolved language and night mode, read through the AutoJs6 settings provider. */
internal data class HostAppearance(
    val languageTag: String,
    val darkMode: Boolean,
    val themeColorPrimary: Int = AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE,
    val themeColorAccent: Int = AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE,
) {
    fun wrap(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        val locale = Locale.forLanguageTag(languageTag)
        configuration.setLocales(LocaleList(locale))
        configuration.setLayoutDirection(locale)
        configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (darkMode) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return context.createConfigurationContext(configuration)
    }

    companion object {
        // AutoJs6HostSettingsContract v1; independent of the Explorer Action protocol. The client
        // is unstable on purpose: a stable provider connection makes Android kill the reader
        // together with the host when the host is stopped or updated during the call (seen on API
        // 28 while another session reinstalled the host); an unstable one only fails the call.
        @Volatile private var cached: HostAppearance? = null
        @Volatile private var loaded = false
        internal val hasSnapshot: Boolean get() = loaded
        private val refreshing = java.util.concurrent.atomic.AtomicBoolean(false)
        private val listeners = java.util.concurrent.CopyOnWriteArraySet<() -> Unit>()
        private val worker by lazy { java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "host-appearance") } }
        private val main by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

        fun read(context: Context): HostAppearance? {
            if (!loaded) refresh(context)
            return cached
        }

        fun addListener(listener: () -> Unit) { listeners += listener }
        fun removeListener(listener: () -> Unit) { listeners -= listener }

        fun refresh(context: Context) {
            if (!refreshing.compareAndSet(false, true)) return
            val app = context.applicationContext
            worker.execute {
                val latest = readBlocking(app)
                val changed = latest != cached
                cached = latest
                loaded = true
                refreshing.set(false)
                if (changed) main.post { listeners.forEach { it() } }
            }
        }

        private fun readBlocking(context: Context): HostAppearance? = runCatching {
            val client = context.contentResolver.acquireUnstableContentProviderClient(
                "content://org.autojs.autojs6.plugin.settings".toUri(),
            ) ?: return null
            val result = client.use { it.call("getSettings", null, null) } ?: return null
            fromBundle(result)
        }.getOrNull()

        fun fromBundle(result: Bundle): HostAppearance? {
            if (result.getInt("protocolVersion", 0) != 1 ||
                result.getString("hostPackageName") != "org.autojs.autojs6" ||
                !result.containsKey("darkModeActive")
            ) return null
            val language = result.getString("resolvedLanguageTag")?.takeIf {
                it.length in 2..80 && it.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")) &&
                    Locale.forLanguageTag(it).language.isNotBlank()
            } ?: return null
            return HostAppearance(
                language,
                result.getBoolean("darkModeActive"),
                AppThemePaletteGenerator.opaque(result.getInt("themeColorPrimary", AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE)),
                AppThemePaletteGenerator.opaque(result.getInt("themeColorAccent", AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE)),
            )
        }
    }
}

/** Recreates the plugin's own resource context using the host's resolved language and night mode. */
abstract class HostAppearanceActivity : AppCompatActivity() {
    private var hostAppearance: HostAppearance? = null
    private lateinit var appAppearance: HostAppearance
    private var preferenceRevision = Long.MIN_VALUE
    private var themeRevision = Long.MIN_VALUE
    private var recreationRequested = false
    private var userInteracted = false
    private val appearanceDialogs = java.util.WeakHashMap<android.app.Dialog, Unit>()
    internal fun trackAppearanceDialog(dialog: android.app.Dialog) { appearanceDialogs[dialog] = Unit }
    override fun onUserInteraction() {
        super.onUserInteraction()
        userInteracted = true
    }
    private val hostAppearanceListener: () -> Unit = {
        if (!userInteracted && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) refreshEffectiveAppearance()
    }

    internal lateinit var appPalette: AppThemePalette
        private set

    /** The host's night mode, or the system's when the host settings are unavailable. */
    internal val hostDarkMode: Boolean
        get() = hostAppearance?.darkMode
            ?: (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    override fun attachBaseContext(newBase: Context) {
        hostAppearance = HostAppearance.read(newBase)
        appAppearance = resolveAppearance(newBase, hostAppearance)
        super.attachBaseContext(appAppearance.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        HostAppearance.addListener(hostAppearanceListener)
        delegate.localNightMode = if (appAppearance.darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        super.onCreate(savedInstanceState)
        LauncherIcons.normalizeAsync(this)
        preferenceRevision = AppPreferenceStore(this).appearanceRevision()
        val themeStore = ThemePreferenceStore(this)
        themeRevision = themeStore.revision()
        appPalette = AppThemePaletteGenerator.generate(
            ThemeSourcePolicy.resolveColor(themeStore.load(), hostAppearance?.themeColorPrimary),
            appAppearance.darkMode,
            if (themeStore.load().mode == io.github.supermonster003.autojs6.plugin.three.folio.epub.theme.ThemeSourceMode.AUTOJS6)
                hostAppearance?.themeColorAccent ?: AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE
            else ThemeSourcePolicy.resolveColor(themeStore.load(), null),
        )
        @Suppress("DEPRECATION")
        window.apply {
            statusBarColor = appPalette.appBar
            navigationBarColor = if (android.os.Build.VERSION.SDK_INT < 26) 0xFF121212.toInt() else appPalette.background
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = AppThemePaletteGenerator.bestMonochromeForeground(appPalette.appBar) == android.graphics.Color.BLACK
            isAppearanceLightNavigationBars = !appAppearance.darkMode
        }
    }

    override fun onResume() {
        super.onResume()
        userInteracted = false
        HostAppearance.refresh(this)
        refreshEffectiveAppearance()
    }

    override fun onDestroy() {
        HostAppearance.removeListener(hostAppearanceListener)
        super.onDestroy()
    }

    private fun refreshEffectiveAppearance() {
        if (recreationRequested || isFinishing || isDestroyed || appearanceDialogs.keys.any { it.isShowing }) return
        val latest = HostAppearance.read(this)
        val systemDark = Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val readerNightChanged = this is EpubReaderActivity && (latest?.darkMode ?: systemDark) != hostDarkMode
        if (readerNightChanged || resolveAppearance(this, latest) != appAppearance ||
            AppPreferenceStore(this).appearanceRevision() != preferenceRevision ||
            ThemePreferenceStore(this).revision() != themeRevision
        ) {
            recreationRequested = true
            recreate()
        } else {
            hostAppearance = latest
        }
    }

    override fun onContentChanged() {
        super.onContentChanged()
        // Book-page colors remain controlled by the separate reader preferences and chrome.
        if (!::appPalette.isInitialized || this is EpubReaderActivity) return
        findViewById<View>(android.R.id.content)?.let { content ->
            content.setBackgroundColor(appPalette.background)
            AppThemeDialogStyler.styleTree(content, appPalette)
            fun toolbar(view: View) {
                if (view is Toolbar) {
                    view.setBackgroundColor(appPalette.appBar)
                    view.setTitleTextColor(appPalette.onAppBar)
                    view.setSubtitleTextColor(appPalette.onAppBar)
                    view.navigationIcon?.mutate()?.setTint(appPalette.onAppBar)
                    view.overflowIcon?.mutate()?.setTint(appPalette.onAppBar)
                }
                if (view is ViewGroup) for (index in 0 until view.childCount) toolbar(view.getChildAt(index))
            }
            toolbar(content)
        }
    }

    internal fun showAppDialog(dialog: AlertDialog, onShown: (AlertDialog) -> Unit = {}) =
        AppThemeDialogStyler.show(dialog, appPalette, onShown)

    internal fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun resolveAppearance(context: Context, host: HostAppearance?): HostAppearance {
        val store = AppPreferenceStore(context)
        val system = Resources.getSystem().configuration
        val language = store.language().let { preference ->
            when (preference.mode) {
                AppLanguageMode.AUTOJS6 -> host?.languageTag
                AppLanguageMode.SYSTEM -> null
                AppLanguageMode.SPECIFIC -> preference.languageTag
            }
        } ?: system.locales[0].toLanguageTag()
        val dark = when (store.nightMode()) {
            AppNightMode.AUTOJS6 -> host?.darkMode
            AppNightMode.SYSTEM -> null
            AppNightMode.LIGHT -> false
            AppNightMode.DARK -> true
        } ?: (system.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        val theme = ThemePreferenceStore(context).load()
        val source = ThemeSourcePolicy.resolveColor(theme, host?.themeColorPrimary)
        val accent = if (theme.mode == io.github.supermonster003.autojs6.plugin.three.folio.epub.theme.ThemeSourceMode.AUTOJS6)
            host?.themeColorAccent ?: AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE else source
        return HostAppearance(language, dark, source, accent)
    }
}
