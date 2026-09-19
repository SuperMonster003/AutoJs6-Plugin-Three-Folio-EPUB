package io.github.supermonster003.autojs6.plugin.readium.epub.reader.store

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Where taps turn pages (roadmap P2.7): the left / right thirds, the top / bottom thirds, or nowhere. */
internal enum class TapZones(val key: String) {
    OFF("off"),
    HORIZONTAL("horizontal"),
    VERTICAL("vertical"),
    ;

    companion object {
        val DEFAULT = HORIZONTAL

        fun fromKey(key: String?): TapZones? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Global reader toggles that already exist before the preferences panel (roadmap P1.2):
 * scroll versus paginated overflow, and whether the volume keys turn pages. Roadmap P2.1 adds the
 * serialized `EpubPreferences` next to these; both live in the plugin's private preferences.
 * Roadmap P2.7 adds the tap zones and whether web links open without asking; P3 remembers that
 * the notification permission was asked for read-aloud.
 */
internal class ReaderSettings(context: Context) {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var scrollMode: Boolean
        get() = preferences.getBoolean(KEY_SCROLL_MODE, DEFAULT_SCROLL_MODE)
        set(value) = preferences.edit { putBoolean(KEY_SCROLL_MODE, value) }

    var volumeKeysTurnPages: Boolean
        get() = preferences.getBoolean(KEY_VOLUME_KEYS_TURN_PAGES, DEFAULT_VOLUME_KEYS_TURN_PAGES)
        set(value) = preferences.edit { putBoolean(KEY_VOLUME_KEYS_TURN_PAGES, value) }

    var tapZones: TapZones
        get() = TapZones.fromKey(preferences.getString(KEY_TAP_ZONES, null)) ?: TapZones.DEFAULT
        set(value) = preferences.edit { putString(KEY_TAP_ZONES, value.key) }

    /** Roadmap decision D25: web links ask first by default; this opens them straight away. */
    var externalLinksDirect: Boolean
        get() = preferences.getBoolean(KEY_EXTERNAL_LINKS_DIRECT, DEFAULT_EXTERNAL_LINKS_DIRECT)
        set(value) = preferences.edit { putBoolean(KEY_EXTERNAL_LINKS_DIRECT, value) }

    /** Roadmap D15: Android 13+ asks for `POST_NOTIFICATIONS` once, before the first read-aloud. */
    var readAloudNotificationAsked: Boolean
        get() = preferences.getBoolean(KEY_READ_ALOUD_NOTIFICATION_ASKED, false)
        set(value) = preferences.edit { putBoolean(KEY_READ_ALOUD_NOTIFICATION_ASKED, value) }

    /** Roadmap P3: hold the screen on while a voice reads (off by default: the voice works with the screen off). */
    var readAloudKeepScreenOn: Boolean
        get() = preferences.getBoolean(KEY_READ_ALOUD_KEEP_SCREEN_ON, DEFAULT_READ_ALOUD_KEEP_SCREEN_ON)
        set(value) = preferences.edit { putBoolean(KEY_READ_ALOUD_KEEP_SCREEN_ON, value) }

    /** Roadmap D26: keep reading aloud after the reader closes (off by default: closing the reader stops the voice). */
    var readAloudInBackground: Boolean
        get() = preferences.getBoolean(KEY_READ_ALOUD_IN_BACKGROUND, DEFAULT_READ_ALOUD_IN_BACKGROUND)
        set(value) = preferences.edit { putBoolean(KEY_READ_ALOUD_IN_BACKGROUND, value) }

    companion object {
        internal const val PREFERENCES_NAME = "reader_settings"
        internal const val KEY_SCROLL_MODE = "scroll_mode"
        internal const val KEY_VOLUME_KEYS_TURN_PAGES = "volume_keys_turn_pages"
        internal const val KEY_TAP_ZONES = "tap_zones"
        internal const val KEY_EXTERNAL_LINKS_DIRECT = "external_links_direct"
        internal const val KEY_READ_ALOUD_NOTIFICATION_ASKED = "read_aloud_notification_asked"
        internal const val KEY_READ_ALOUD_KEEP_SCREEN_ON = "read_aloud_keep_screen_on"
        internal const val KEY_READ_ALOUD_IN_BACKGROUND = "read_aloud_in_background"
        const val DEFAULT_SCROLL_MODE = false
        const val DEFAULT_VOLUME_KEYS_TURN_PAGES = true
        const val DEFAULT_EXTERNAL_LINKS_DIRECT = false
        const val DEFAULT_READ_ALOUD_KEEP_SCREEN_ON = false
        const val DEFAULT_READ_ALOUD_IN_BACKGROUND = false
    }
}
