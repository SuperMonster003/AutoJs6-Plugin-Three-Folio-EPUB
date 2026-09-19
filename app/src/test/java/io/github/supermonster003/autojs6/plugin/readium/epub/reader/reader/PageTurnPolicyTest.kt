package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.view.KeyEvent
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
}
