package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import java.util.Locale

/** The host's resolved language and night mode, read through the AutoJs6 settings provider. */
internal data class HostAppearance(val languageTag: String, val darkMode: Boolean) {
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
        // AutoJs6HostSettingsContract v1; independent of the Explorer Action protocol.
        fun read(context: Context): HostAppearance? = runCatching {
            val result = context.contentResolver.call(
                "content://org.autojs.autojs6.plugin.settings".toUri(),
                "getSettings", null, null,
            ) ?: return null
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
            return HostAppearance(language, result.getBoolean("darkModeActive"))
        }
    }
}

/** Recreates the plugin's own resource context using the host's resolved language and night mode. */
abstract class HostAppearanceActivity : AppCompatActivity() {
    private var hostAppearance: HostAppearance? = null

    /** The host's night mode, or the system's when the host settings are unavailable. */
    internal val hostDarkMode: Boolean
        get() = hostAppearance?.darkMode
            ?: (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    override fun attachBaseContext(newBase: Context) {
        hostAppearance = HostAppearance.read(newBase)
        super.attachBaseContext(hostAppearance?.wrap(newBase) ?: newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.localNightMode = hostAppearance?.let {
            if (it.darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        } ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val latest = HostAppearance.read(this)
        if (latest != null && latest != hostAppearance) {
            hostAppearance = latest
            recreate()
        }
    }
}
