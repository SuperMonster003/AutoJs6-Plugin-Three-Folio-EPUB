package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.view.KeyEvent
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.TapZones

/** What a tap or key press asks the reader to do (roadmap P1.2). */
internal enum class PageTurnAction {
    PREVIOUS,
    NEXT,
    TOGGLE_CHROME,
}

/**
 * Tap zones and keys as pure logic. Taps: the outer thirds of the navigator view turn pages
 * (left / right by default, top / bottom when so configured, never when the zones are off) and
 * the rest toggles the chrome; right-to-left books swap the horizontal edges. Keys: the volume
 * keys when enabled, and a hardware keyboard's arrow, page and space keys (roadmap P2.7).
 */
internal object PageTurnPolicy {

    // W3C `KeyboardEvent.code` values, which is how the navigator reports keys pressed in a page.
    const val KEY_ARROW_LEFT = "ArrowLeft"
    const val KEY_ARROW_RIGHT = "ArrowRight"
    const val KEY_ARROW_UP = "ArrowUp"
    const val KEY_ARROW_DOWN = "ArrowDown"
    const val KEY_PAGE_UP = "PageUp"
    const val KEY_PAGE_DOWN = "PageDown"
    const val KEY_SPACE = "Space"

    fun resolveTap(x: Float, width: Int, rightToLeft: Boolean): PageTurnAction =
        resolveTap(x, 0f, width, 1, TapZones.HORIZONTAL, rightToLeft)

    fun resolveTap(x: Float, y: Float, width: Int, height: Int, zones: TapZones, rightToLeft: Boolean): PageTurnAction =
        when (zones) {
            TapZones.OFF -> PageTurnAction.TOGGLE_CHROME
            TapZones.HORIZONTAL -> thirds(x, width).let { if (rightToLeft) it.mirrored() else it }
            TapZones.VERTICAL -> thirds(y, height)
        }

    private fun thirds(position: Float, extent: Int): PageTurnAction {
        if (extent <= 0) return PageTurnAction.TOGGLE_CHROME
        return when {
            position < extent / 3f -> PageTurnAction.PREVIOUS
            position >= extent * 2f / 3f -> PageTurnAction.NEXT
            else -> PageTurnAction.TOGGLE_CHROME
        }
    }

    /** Volume down goes forward and volume up goes back; null when the key is not a volume key or the feature is off. */
    fun resolveKey(keyCode: Int, volumeKeysTurnPages: Boolean): PageTurnAction? {
        if (!volumeKeysTurnPages) return null
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> PageTurnAction.NEXT
            KeyEvent.KEYCODE_VOLUME_UP -> PageTurnAction.PREVIOUS
            else -> null
        }
    }

    /**
     * A hardware keyboard key by its `KeyboardEvent.code`: left / right arrows follow the reading
     * direction, page up / down and space (shift + space backwards) always turn pages, and the
     * up / down arrows do so only in paginated mode, since in scroll mode the page scrolls itself.
     * Null leaves the key to the web view.
     */
    fun resolveKeyboard(code: String, shift: Boolean, rightToLeft: Boolean, scroll: Boolean): PageTurnAction? = when (code) {
        KEY_ARROW_LEFT -> PageTurnAction.PREVIOUS.let { if (rightToLeft) it.mirrored() else it }
        KEY_ARROW_RIGHT -> PageTurnAction.NEXT.let { if (rightToLeft) it.mirrored() else it }
        KEY_PAGE_UP -> PageTurnAction.PREVIOUS
        KEY_PAGE_DOWN -> PageTurnAction.NEXT
        KEY_SPACE -> if (shift) PageTurnAction.PREVIOUS else PageTurnAction.NEXT
        KEY_ARROW_UP -> if (scroll) null else PageTurnAction.PREVIOUS
        KEY_ARROW_DOWN -> if (scroll) null else PageTurnAction.NEXT
        else -> null
    }

    /** The same keys as Android key codes, for presses the navigator's view did not receive. */
    fun keyboardCode(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> KEY_ARROW_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> KEY_ARROW_RIGHT
        KeyEvent.KEYCODE_DPAD_UP -> KEY_ARROW_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> KEY_ARROW_DOWN
        KeyEvent.KEYCODE_PAGE_UP -> KEY_PAGE_UP
        KeyEvent.KEYCODE_PAGE_DOWN -> KEY_PAGE_DOWN
        KeyEvent.KEYCODE_SPACE -> KEY_SPACE
        else -> null
    }

    private fun PageTurnAction.mirrored(): PageTurnAction = when (this) {
        PageTurnAction.PREVIOUS -> PageTurnAction.NEXT
        PageTurnAction.NEXT -> PageTurnAction.PREVIOUS
        PageTurnAction.TOGGLE_CHROME -> PageTurnAction.TOGGLE_CHROME
    }
}
