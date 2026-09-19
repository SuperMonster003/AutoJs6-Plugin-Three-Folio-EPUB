package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The engine named explicitly when the framework's default lookup finds none (roadmap P3, Xiaomi engines in `/data/app`). */
class SystemTtsEngineTest {

    @Test
    fun theDeclaredDefaultWinsWhenInstalled() {
        assertEquals("com.google.android.tts", SystemTtsEngine.fallbackEngine("com.google.android.tts", listOf("com.xiaomi.mibrain.speech", "com.google.android.tts")))
    }

    @Test
    fun theFirstInstalledEngineIsNamedWhenNoDefaultIsUsable() {
        assertEquals("com.xiaomi.mibrain.speech", SystemTtsEngine.fallbackEngine(null, listOf("com.xiaomi.mibrain.speech")))
        assertEquals("com.xiaomi.mibrain.speech", SystemTtsEngine.fallbackEngine("com.removed.tts", listOf("com.xiaomi.mibrain.speech")))
    }

    @Test
    fun noInstalledEngineMeansNoFallback() {
        assertNull(SystemTtsEngine.fallbackEngine(null, emptyList()))
        assertNull(SystemTtsEngine.fallbackEngine("com.google.android.tts", emptyList()))
    }
}
