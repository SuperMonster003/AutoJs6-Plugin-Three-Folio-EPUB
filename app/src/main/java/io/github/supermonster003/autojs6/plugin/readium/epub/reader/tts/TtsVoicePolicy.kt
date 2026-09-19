package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

/**
 * One voice of the system text-to-speech engine as the selection rules see it (roadmap P3):
 * Android-free so the rules run in JVM tests. [quality] grows with the engine's quality level
 * and [language] is the BCP 47 tag the engine reported (`en-US`, `zh-CN`, `en`).
 */
internal data class VoiceOption(
    val id: String,
    val language: String,
    val quality: Int = 0,
    val requiresNetwork: Boolean = false,
)

/**
 * Language matching and voice fallback for read-aloud. The engine speaks each sentence in the
 * language Readium detected for it (the book's language, or a `lang` attribute), so these rules
 * decide which of the installed voices stands for such a language: an exact tag first, then any
 * voice of the same base language (`en` for `en-GB`), offline voices before network ones, higher
 * quality before lower, and a stable order after that so the choice never flickers.
 */
internal object TtsVoicePolicy {

    /** `en_us` and `EN-us` both become `en-US`; a bare language stays bare. */
    fun normalize(tag: String): String {
        val parts = tag.trim().replace('_', '-').split('-').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return ""
        return buildString {
            append(parts[0].lowercase())
            for (part in parts.drop(1)) {
                append('-')
                append(if (part.length == 2) part.uppercase() else part)
            }
        }
    }

    /** The base language of [tag]: `en` for `en-US`. */
    fun base(tag: String): String = normalize(tag).substringBefore('-')

    /** The distinct languages the [voices] cover, normalized and sorted; what the language picker lists. */
    fun languages(voices: Collection<VoiceOption>): List<String> =
        voices.map { normalize(it.language) }.filter { it.isNotEmpty() }.distinct().sorted()

    /**
     * The tag among [available] that stands for [wanted]: the same tag, else the bare base
     * language, else the first tag of that base language in sorted order, else null.
     */
    fun match(wanted: String?, available: Collection<String>): String? {
        val target = wanted?.let(::normalize)?.takeIf { it.isNotEmpty() } ?: return null
        val candidates = available.map(::normalize)
        if (target in candidates) return target
        val targetBase = target.substringBefore('-')
        return candidates.filter { it.substringBefore('-') == targetBase }.minWithOrNull(compareBy({ it.contains('-') }, { it }))
    }

    /**
     * The language read-aloud should start with when the user picked none: the book's language
     * when the engine covers it, else null so the engine speaks in its own default language.
     */
    fun defaultLanguage(preferred: String?, publicationLanguage: String?, available: Collection<String>): String? =
        match(preferred, available) ?: match(publicationLanguage, available)

    /** The voices usable for [language] (same base language), best first. */
    fun voicesFor(language: String, voices: Collection<VoiceOption>): List<VoiceOption> {
        val target = normalize(language)
        val targetBase = target.substringBefore('-')
        return voices
            .filter { base(it.language) == targetBase }
            .sortedWith(
                compareBy<VoiceOption>({ normalize(it.language) != target }, { it.requiresNetwork }, { -it.quality }, { it.id }),
            )
    }

    /** The voice the engine should use for [language] when the user chose none; null when it has no such voice. */
    fun preferredVoice(language: String?, voices: Collection<VoiceOption>): VoiceOption? {
        val target = language?.takeIf { normalize(it).isNotEmpty() } ?: return null
        return voicesFor(target, voices).firstOrNull()
    }
}
