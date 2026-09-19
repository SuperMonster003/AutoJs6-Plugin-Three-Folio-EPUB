package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.view.KeyEvent

/** What a tap or key press asks the reader to do (roadmap P1.2). */
internal enum class PageTurnAction {
    PREVIOUS,
    NEXT,
    TOGGLE_CHROME,
}

/**
 * Tap zones and volume keys as pure logic: the left third of the navigator view goes back, the
 * right third goes forward, the middle toggles the chrome. Right-to-left books swap the edges.
 */
internal object PageTurnPolicy {

    fun resolveTap(x: Float, width: Int, rightToLeft: Boolean): PageTurnAction {
        if (width <= 0) return PageTurnAction.TOGGLE_CHROME
        val zone = when {
            x < width / 3f -> PageTurnAction.PREVIOUS
            x >= width * 2f / 3f -> PageTurnAction.NEXT
            else -> PageTurnAction.TOGGLE_CHROME
        }
        return if (rightToLeft) zone.mirrored() else zone
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

    private fun PageTurnAction.mirrored(): PageTurnAction = when (this) {
        PageTurnAction.PREVIOUS -> PageTurnAction.NEXT
        PageTurnAction.NEXT -> PageTurnAction.PREVIOUS
        PageTurnAction.TOGGLE_CHROME -> PageTurnAction.TOGGLE_CHROME
    }
}
