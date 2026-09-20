package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import java.io.IOException

internal fun releaseHistoryCandidates(language: String, country: String, script: String): List<String> {
    val code = when {
        language == "zh" && country == "HK" -> "zh-Hant-HK"
        language == "zh" && (country == "TW" || script == "Hant") -> "zh-Hant-TW"
        language == "zh" -> "zh-Hans"
        language in setOf("en", "ar", "es", "fr", "ja", "ko", "ru") -> language
        else -> "en"
    }
    return listOf("doc/CHANGELOG-$code.md", "doc/CHANGELOG-en.md").distinct()
}

internal fun loadReleaseHistory(candidates: List<String>, loader: (String) -> String): String? {
    for (path in candidates) {
        try { loader(path).takeIf { it.isNotBlank() }?.let { return it } } catch (_: IOException) { }
    }
    return null
}
