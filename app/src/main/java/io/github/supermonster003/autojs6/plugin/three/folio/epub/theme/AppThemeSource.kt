package io.github.supermonster003.autojs6.plugin.three.folio.epub.theme

import android.content.Context
import androidx.core.content.edit

internal enum class ThemeSourceMode {
    AUTOJS6,
    PRESET,
    CUSTOM,
}

internal data class ThemeSourcePreference(
    val mode: ThemeSourceMode = ThemeSourceMode.AUTOJS6,
    val presetKey: String? = null,
    val customColor: Int = AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE,
)

internal data class ThemePreset(
    val key: String,
    val color: Int,
)

/** Material Design 500 colors used by the classic Material Dialog color selector. */
internal object ThemePresetCatalog {
    val colors: List<ThemePreset> = listOf(
        ThemePreset("red", 0xFFF44336.toInt()),
        ThemePreset("pink", 0xFFE91E63.toInt()),
        ThemePreset("purple", 0xFF9C27B0.toInt()),
        ThemePreset("deep_purple", 0xFF673AB7.toInt()),
        ThemePreset("indigo", 0xFF3F51B5.toInt()),
        ThemePreset("blue", 0xFF2196F3.toInt()),
        ThemePreset("light_blue", 0xFF03A9F4.toInt()),
        ThemePreset("cyan", 0xFF00BCD4.toInt()),
        ThemePreset("teal", 0xFF009688.toInt()),
        ThemePreset("green", 0xFF4CAF50.toInt()),
        ThemePreset("light_green", 0xFF8BC34A.toInt()),
        ThemePreset("lime", 0xFFCDDC39.toInt()),
        ThemePreset("yellow", 0xFFFFEB3B.toInt()),
        ThemePreset("amber", 0xFFFFC107.toInt()),
        ThemePreset("orange", 0xFFFF9800.toInt()),
        ThemePreset("deep_orange", 0xFFFF5722.toInt()),
        ThemePreset("brown", 0xFF795548.toInt()),
        ThemePreset("gray", 0xFF9E9E9E.toInt()),
        ThemePreset("blue_gray", 0xFF607D8B.toInt()),
    )

    fun find(key: String?): ThemePreset? = colors.firstOrNull { it.key == key }
}

internal object ThemeSourcePolicy {
    fun normalize(preference: ThemeSourcePreference): ThemeSourcePreference = when (preference.mode) {
        ThemeSourceMode.AUTOJS6 -> ThemeSourcePreference(
            customColor = AppThemePaletteGenerator.opaque(preference.customColor),
        )
        ThemeSourceMode.PRESET -> ThemePresetCatalog.find(preference.presetKey)?.let { preset ->
            ThemeSourcePreference(
                mode = ThemeSourceMode.PRESET,
                presetKey = preset.key,
                customColor = AppThemePaletteGenerator.opaque(preference.customColor),
            )
        } ?: ThemeSourcePreference(
            customColor = AppThemePaletteGenerator.opaque(preference.customColor),
        )
        ThemeSourceMode.CUSTOM -> ThemeSourcePreference(
            mode = ThemeSourceMode.CUSTOM,
            customColor = AppThemePaletteGenerator.opaque(preference.customColor),
        )
    }

    fun resolveColor(preference: ThemeSourcePreference, hostColor: Int?): Int {
        val normalized = normalize(preference)
        return when (normalized.mode) {
            ThemeSourceMode.AUTOJS6 -> hostColor
                ?.let(AppThemePaletteGenerator::opaque)
                ?: AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE
            ThemeSourceMode.PRESET -> ThemePresetCatalog.find(normalized.presetKey)?.color
                ?: AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE
            ThemeSourceMode.CUSTOM -> normalized.customColor
        }
    }
}

internal class ThemePreferenceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): ThemeSourcePreference = ThemeSourcePolicy.normalize(
        ThemeSourcePreference(
            mode = preferences.getString(KEY_MODE, null)
                ?.let { value -> runCatching { ThemeSourceMode.valueOf(value) }.getOrNull() }
                ?: ThemeSourceMode.AUTOJS6,
            presetKey = preferences.getString(KEY_PRESET, null),
            customColor = preferences.getInt(
                KEY_CUSTOM_COLOR,
                AppThemePaletteGenerator.AUTOJS6_FALLBACK_SOURCE,
            ),
        ),
    )

    fun save(preference: ThemeSourcePreference): Boolean {
        val normalized = ThemeSourcePolicy.normalize(preference)
        if (normalized == load()) return false
        preferences.edit {
            putString(KEY_MODE, normalized.mode.name)
            putString(KEY_PRESET, normalized.presetKey)
            putInt(KEY_CUSTOM_COLOR, normalized.customColor)
            putLong(KEY_REVISION, revision() + 1L)
        }
        return true
    }

    fun revision(): Long = preferences.getLong(KEY_REVISION, 0L)

    private companion object {
        const val PREFERENCES_NAME = "app_theme"
        const val KEY_MODE = "source_mode"
        const val KEY_PRESET = "preset_key"
        const val KEY_CUSTOM_COLOR = "custom_color"
        const val KEY_REVISION = "revision"
    }
}

internal data class AppHostSnapshot(val themeColorPrimary: Int, val themeColorAccent: Int)
internal data class AppHostResult(val snapshot: AppHostSnapshot?) {
    val available: Boolean get() = snapshot != null
}
internal object AutoJs6AppearanceClient {
    fun query(context: Context): AppHostResult = AppHostResult(
        io.github.supermonster003.autojs6.plugin.three.folio.epub.HostAppearance.read(context)?.let {
            AppHostSnapshot(it.themeColorPrimary, it.themeColorAccent)
        },
    )
}
