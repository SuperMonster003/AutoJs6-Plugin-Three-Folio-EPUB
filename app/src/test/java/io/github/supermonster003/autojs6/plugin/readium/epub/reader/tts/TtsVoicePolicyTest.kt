package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsVoicePolicyTest {

    private val voices = listOf(
        VoiceOption("en-us-x-local", "en-US", quality = 3),
        VoiceOption("en-us-x-network", "en-US", quality = 4, requiresNetwork = true),
        VoiceOption("en-gb-x-local", "en_GB", quality = 3),
        VoiceOption("zh-cn-x-local", "zh-CN", quality = 3),
        VoiceOption("ja-jp-x-low", "ja-JP", quality = 1),
        VoiceOption("ja-jp-x-high", "ja-JP", quality = 3),
    )

    @Test
    fun tagsAreNormalized() {
        assertEquals("en-US", TtsVoicePolicy.normalize("en_us"))
        assertEquals("en-US", TtsVoicePolicy.normalize("EN-us"))
        assertEquals("zh-Hant-TW", TtsVoicePolicy.normalize("zh-Hant-tw"))
        assertEquals("en", TtsVoicePolicy.normalize(" en "))
        assertEquals("", TtsVoicePolicy.normalize("-"))
        assertEquals("en", TtsVoicePolicy.base("en-US"))
        assertEquals(listOf("en-GB", "en-US", "ja-JP", "zh-CN"), TtsVoicePolicy.languages(voices))
    }

    @Test
    fun languagesMatchExactlyThenByBaseLanguage() {
        val available = TtsVoicePolicy.languages(voices)
        assertEquals("en-US", TtsVoicePolicy.match("en_US", available))
        assertEquals("en-GB", TtsVoicePolicy.match("en-GB", available))
        // No `en-AU`: the first voice of the same base language in sorted order.
        assertEquals("en-GB", TtsVoicePolicy.match("en-AU", available))
        assertEquals("en-GB", TtsVoicePolicy.match("en", available))
        // A bare base language is preferred over any regional variant when both exist.
        assertEquals("fr", TtsVoicePolicy.match("fr-CA", listOf("fr-FR", "fr", "fr-BE")))
        assertNull(TtsVoicePolicy.match("ar", available))
        assertNull(TtsVoicePolicy.match(null, available))
        assertNull(TtsVoicePolicy.match("", available))
    }

    @Test
    fun theDefaultLanguageFollowsTheBookWhenTheEngineHasIt() {
        val available = TtsVoicePolicy.languages(voices)
        assertEquals("ja-JP", TtsVoicePolicy.defaultLanguage(null, "ja", available))
        assertEquals("zh-CN", TtsVoicePolicy.defaultLanguage("zh", "en", available))
        assertEquals("en-US", TtsVoicePolicy.defaultLanguage("ar", "en-US", available))
        assertNull(TtsVoicePolicy.defaultLanguage("ar", "ko", available))
        assertNull(TtsVoicePolicy.defaultLanguage(null, null, available))
    }

    @Test
    fun voicesPreferExactRegionOfflineAndQuality() {
        // The exact region comes first even for a network voice; other regions follow, offline first.
        assertEquals(
            listOf("en-us-x-local", "en-us-x-network", "en-gb-x-local"),
            TtsVoicePolicy.voicesFor("en-US", voices).map { it.id },
        )
        assertEquals(
            listOf("en-gb-x-local", "en-us-x-local", "en-us-x-network"),
            TtsVoicePolicy.voicesFor("en-GB", voices).map { it.id },
        )
        assertEquals(listOf("ja-jp-x-high", "ja-jp-x-low"), TtsVoicePolicy.voicesFor("ja", voices).map { it.id })
        // No exact region: the offline voices tie, so the id order decides (stable across calls).
        assertEquals("en-gb-x-local", TtsVoicePolicy.preferredVoice("en-AU", voices)?.id)
        assertEquals("zh-cn-x-local", TtsVoicePolicy.preferredVoice("zh-TW", voices)?.id)
        assertNull(TtsVoicePolicy.preferredVoice("ko-KR", voices))
        assertNull(TtsVoicePolicy.preferredVoice(null, voices))
        assertEquals(emptyList<VoiceOption>(), TtsVoicePolicy.voicesFor("ko", voices))
    }

    @Test
    fun equalVoicesKeepAStableOrder() {
        val twins = listOf(VoiceOption("b", "en-US"), VoiceOption("a", "en-US"), VoiceOption("c", "en-US"))
        assertEquals(listOf("a", "b", "c"), TtsVoicePolicy.voicesFor("en-US", twins).map { it.id })
        assertEquals("a", TtsVoicePolicy.preferredVoice("en", twins)?.id)
    }
}
