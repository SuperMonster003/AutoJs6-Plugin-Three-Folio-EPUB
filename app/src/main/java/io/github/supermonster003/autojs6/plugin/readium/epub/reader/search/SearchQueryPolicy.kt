package io.github.supermonster003.autojs6.plugin.readium.epub.reader.search

/**
 * What counts as a query worth running (roadmap P2.5): whitespace is trimmed and collapsed, and a
 * query needs [MIN_LENGTH] characters unless it is a single CJK ideograph, kana or hangul
 * syllable, which is a complete word on its own. Shorter queries get a hint instead of a search
 * that would list every other line of the book.
 */
internal object SearchQueryPolicy {

    const val MIN_LENGTH = 2

    private val WHITESPACE = Regex("\\s+")

    fun normalize(raw: String): String = raw.trim().replace(WHITESPACE, " ")

    /** True for the empty query and for one-character queries outside the CJK scripts. */
    fun isTooShort(query: String): Boolean {
        val length = query.codePointCount(0, query.length)
        if (length >= MIN_LENGTH) return false
        if (length == 0) return true
        return !isCjkWord(query.codePointAt(0))
    }

    private fun isCjkWord(codePoint: Int): Boolean = when (Character.UnicodeBlock.of(codePoint)) {
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        -> true
        else -> false
    }
}
