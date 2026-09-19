package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.view.KeyEvent
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.TapZones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageTurnPolicyTest {

    @Test
    fun tapZonesSplitTheWidthInThirds() {
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveTap(0f, 900, rightToLeft = false))
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveTap(299f, 900, rightToLeft = false))
        assertEquals(PageTurnAction.TOGGLE_CHROME, PageTurnPolicy.resolveTap(300f, 900, rightToLeft = false))
        assertEquals(PageTurnAction.TOGGLE_CHROME, PageTurnPolicy.resolveTap(599f, 900, rightToLeft = false))
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveTap(600f, 900, rightToLeft = false))
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveTap(899f, 900, rightToLeft = false))
    }

    @Test
    fun rightToLeftBooksMirrorTheEdges() {
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveTap(10f, 900, rightToLeft = true))
        assertEquals(PageTurnAction.TOGGLE_CHROME, PageTurnPolicy.resolveTap(450f, 900, rightToLeft = true))
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveTap(890f, 900, rightToLeft = true))
    }

    @Test
    fun anUnmeasuredViewOnlyTogglesTheChrome() {
        assertEquals(PageTurnAction.TOGGLE_CHROME, PageTurnPolicy.resolveTap(10f, 0, rightToLeft = false))
    }

    @Test
    fun volumeKeysTurnPagesOnlyWhenEnabled() {
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveKey(KeyEvent.KEYCODE_VOLUME_DOWN, volumeKeysTurnPages = true))
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveKey(KeyEvent.KEYCODE_VOLUME_UP, volumeKeysTurnPages = true))
        assertNull(PageTurnPolicy.resolveKey(KeyEvent.KEYCODE_VOLUME_DOWN, volumeKeysTurnPages = false))
        assertNull(PageTurnPolicy.resolveKey(KeyEvent.KEYCODE_VOLUME_MUTE, volumeKeysTurnPages = true))
        assertNull(PageTurnPolicy.resolveKey(KeyEvent.KEYCODE_BACK, volumeKeysTurnPages = true))
    }

    @Test
    fun tapZonesCanBeSwitchedOffOrTurnedVertical() {
        assertEquals(PageTurnAction.TOGGLE_CHROME, PageTurnPolicy.resolveTap(10f, 10f, 900, 1600, TapZones.OFF, rightToLeft = false))
        assertEquals(PageTurnAction.TOGGLE_CHROME, PageTurnPolicy.resolveTap(890f, 1590f, 900, 1600, TapZones.OFF, rightToLeft = true))

        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveTap(890f, 10f, 900, 1600, TapZones.VERTICAL, rightToLeft = false))
        assertEquals(PageTurnAction.TOGGLE_CHROME, PageTurnPolicy.resolveTap(10f, 800f, 900, 1600, TapZones.VERTICAL, rightToLeft = false))
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveTap(10f, 1590f, 900, 1600, TapZones.VERTICAL, rightToLeft = false))
        // Vertical zones do not mirror for right-to-left books.
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveTap(10f, 1590f, 900, 1600, TapZones.VERTICAL, rightToLeft = true))
        assertEquals(PageTurnAction.TOGGLE_CHROME, PageTurnPolicy.resolveTap(10f, 10f, 900, 0, TapZones.VERTICAL, rightToLeft = false))

        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveTap(890f, 10f, 900, 1600, TapZones.HORIZONTAL, rightToLeft = false))
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveTap(890f, 10f, 900, 1600, TapZones.HORIZONTAL, rightToLeft = true))
    }

    @Test
    fun keyboardKeysFollowTheReadingDirectionAndTheOverflow() {
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveKeyboard("ArrowLeft", shift = false, rightToLeft = false, scroll = false))
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveKeyboard("ArrowRight", shift = false, rightToLeft = false, scroll = false))
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveKeyboard("ArrowLeft", shift = false, rightToLeft = true, scroll = false))
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveKeyboard("ArrowRight", shift = false, rightToLeft = true, scroll = false))
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveKeyboard("PageUp", shift = false, rightToLeft = true, scroll = true))
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveKeyboard("PageDown", shift = false, rightToLeft = false, scroll = true))
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveKeyboard("Space", shift = false, rightToLeft = false, scroll = false))
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveKeyboard("Space", shift = true, rightToLeft = false, scroll = false))
        assertEquals(PageTurnAction.PREVIOUS, PageTurnPolicy.resolveKeyboard("ArrowUp", shift = false, rightToLeft = false, scroll = false))
        assertEquals(PageTurnAction.NEXT, PageTurnPolicy.resolveKeyboard("ArrowDown", shift = false, rightToLeft = false, scroll = false))
        // In scroll mode the page scrolls itself on the vertical arrows.
        assertNull(PageTurnPolicy.resolveKeyboard("ArrowUp", shift = false, rightToLeft = false, scroll = true))
        assertNull(PageTurnPolicy.resolveKeyboard("ArrowDown", shift = false, rightToLeft = false, scroll = true))
        assertNull(PageTurnPolicy.resolveKeyboard("Enter", shift = false, rightToLeft = false, scroll = false))
        assertNull(PageTurnPolicy.resolveKeyboard("KeyA", shift = true, rightToLeft = false, scroll = false))
    }

    @Test
    fun androidKeyCodesMapToTheSameKeys() {
        assertEquals("ArrowLeft", PageTurnPolicy.keyboardCode(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals("ArrowRight", PageTurnPolicy.keyboardCode(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals("ArrowUp", PageTurnPolicy.keyboardCode(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals("ArrowDown", PageTurnPolicy.keyboardCode(KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals("PageUp", PageTurnPolicy.keyboardCode(KeyEvent.KEYCODE_PAGE_UP))
        assertEquals("PageDown", PageTurnPolicy.keyboardCode(KeyEvent.KEYCODE_PAGE_DOWN))
        assertEquals("Space", PageTurnPolicy.keyboardCode(KeyEvent.KEYCODE_SPACE))
        assertNull(PageTurnPolicy.keyboardCode(KeyEvent.KEYCODE_ENTER))
        assertNull(PageTurnPolicy.keyboardCode(KeyEvent.KEYCODE_VOLUME_DOWN))
    }
}
