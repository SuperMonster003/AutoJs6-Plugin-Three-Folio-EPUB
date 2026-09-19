package io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts

/** The container format of an accepted font file; the extension names the stored copy. */
enum class FontFormat(val extension: String) {
    TRUE_TYPE("ttf"),
    OPEN_TYPE("otf"),
}

/** What [FontFileValidator.inspect] found in a candidate file. */
sealed class FontInspection {

    /** An SFNT font; [familyName] comes from the `name` table when it has a usable family record. */
    data class Font(val format: FontFormat, val familyName: String?) : FontInspection()

    sealed class Rejected : FontInspection() {
        /** The file does not start with a TrueType or OpenType signature. */
        object NotAFont : Rejected()

        /** A TrueType collection (`ttcf`): several fonts in one file, which the store does not support. */
        object Collection : Rejected()

        /** The table directory or one of the tables points past the end of the file. */
        object Truncated : Rejected()
    }
}

/**
 * Validates a candidate font file and reads its family name (roadmap P2.2). Pure JVM; its format
 * knowledge is limited to the SFNT wrapper shared by TrueType and OpenType: the signature, the
 * table directory and the `name` table (nameID 16 preferred over 1, Windows English before the
 * other platforms). Anything the WebView could not load is rejected before the file is stored.
 */
object FontFileValidator {

    /** Display names are cut to this length after sanitizing. */
    const val MAX_NAME_LENGTH = 60

    private const val SIGNATURE_TRUE_TYPE = 0x00010000
    private const val SIGNATURE_TRUE = 0x74727565 // 'true', legacy Apple TrueType
    private const val SIGNATURE_OPEN_TYPE = 0x4F54544F // 'OTTO', CFF outlines
    private const val SIGNATURE_COLLECTION = 0x74746366 // 'ttcf'
    private const val TAG_NAME = 0x6E616D65 // 'name'

    private const val HEADER_LENGTH = 12
    private const val TABLE_RECORD_LENGTH = 16
    private const val NAME_HEADER_LENGTH = 6
    private const val NAME_RECORD_LENGTH = 12

    private const val NAME_ID_FAMILY = 1
    private const val NAME_ID_TYPOGRAPHIC_FAMILY = 16
    private const val PLATFORM_UNICODE = 0
    private const val PLATFORM_MACINTOSH = 1
    private const val PLATFORM_WINDOWS = 3
    private const val ENCODING_MAC_ROMAN = 0
    private const val LANGUAGE_WINDOWS_ENGLISH_US = 0x0409

    /** Characters that would end a CSS string or confuse a quoted name; dropped from names. */
    private val FORBIDDEN = setOf('"', '\'', '\\', ';', '{', '}', '<', '>')
    private val WHITESPACE = Regex("\\s+")

    fun inspect(bytes: ByteArray): FontInspection {
        if (bytes.size < 4) return FontInspection.Rejected.NotAFont
        val format = when (readInt(bytes, 0)) {
            SIGNATURE_TRUE_TYPE, SIGNATURE_TRUE -> FontFormat.TRUE_TYPE
            SIGNATURE_OPEN_TYPE -> FontFormat.OPEN_TYPE
            SIGNATURE_COLLECTION -> return FontInspection.Rejected.Collection
            else -> return FontInspection.Rejected.NotAFont
        }
        if (bytes.size < HEADER_LENGTH) return FontInspection.Rejected.Truncated
        val tableCount = readUShort(bytes, 4)
        if (tableCount == 0) return FontInspection.Rejected.NotAFont
        if (HEADER_LENGTH + tableCount.toLong() * TABLE_RECORD_LENGTH > bytes.size) {
            return FontInspection.Rejected.Truncated
        }
        var familyName: String? = null
        for (index in 0 until tableCount) {
            val record = HEADER_LENGTH + index * TABLE_RECORD_LENGTH
            val offset = readUInt(bytes, record + 8)
            val length = readUInt(bytes, record + 12)
            if (offset + length > bytes.size) return FontInspection.Rejected.Truncated
            if (readInt(bytes, record) == TAG_NAME) {
                familyName = familyName(bytes, offset.toInt(), length.toInt())
            }
        }
        return FontInspection.Font(format, familyName)
    }

    /**
     * Turns every whitespace run into one space, drops control characters and CSS string
     * delimiters, and cuts to [maxLength]; null when nothing usable remains.
     */
    fun sanitize(raw: String?, maxLength: Int = MAX_NAME_LENGTH): String? {
        if (raw == null) return null
        val cleaned = raw.map { if (it.isWhitespace()) ' ' else it }
            .filter { it.code >= 0x20 && it.code != 0x7F && it !in FORBIDDEN }
            .joinToString("")
            .replace(WHITESPACE, " ")
            .trim()
            .take(maxLength)
            .trim()
        return cleaned.ifEmpty { null }
    }

    private fun familyName(bytes: ByteArray, tableOffset: Int, tableLength: Int): String? {
        if (tableLength < NAME_HEADER_LENGTH) return null
        val count = readUShort(bytes, tableOffset + 2)
        val stringsOffset = tableOffset + readUShort(bytes, tableOffset + 4)
        var bestRank = Int.MAX_VALUE
        var best: String? = null
        for (index in 0 until count) {
            val record = tableOffset + NAME_HEADER_LENGTH + index * NAME_RECORD_LENGTH
            if (record + NAME_RECORD_LENGTH > tableOffset + tableLength) break
            val platform = readUShort(bytes, record)
            val encoding = readUShort(bytes, record + 2)
            val language = readUShort(bytes, record + 4)
            val nameId = readUShort(bytes, record + 6)
            val length = readUShort(bytes, record + 8)
            val start = stringsOffset + readUShort(bytes, record + 10)
            val rank = rank(platform, encoding, language, nameId) ?: continue
            if (rank >= bestRank || start + length > bytes.size) continue
            val name = sanitize(decode(bytes, start, length, platform)) ?: continue
            bestRank = rank
            best = name
        }
        return best
    }

    /** Lower is better: the typographic family first, then Windows English, Windows, Unicode, Mac Roman. */
    private fun rank(platform: Int, encoding: Int, language: Int, nameId: Int): Int? {
        val nameRank = when (nameId) {
            NAME_ID_TYPOGRAPHIC_FAMILY -> 0
            NAME_ID_FAMILY -> 1
            else -> return null
        }
        val platformRank = when {
            platform == PLATFORM_WINDOWS && language == LANGUAGE_WINDOWS_ENGLISH_US -> 0
            platform == PLATFORM_WINDOWS -> 1
            platform == PLATFORM_UNICODE -> 2
            platform == PLATFORM_MACINTOSH && encoding == ENCODING_MAC_ROMAN -> 3
            else -> return null
        }
        return nameRank * 4 + platformRank
    }

    /** Windows and Unicode records are UTF-16BE; Mac Roman is kept to its ASCII subset. */
    private fun decode(bytes: ByteArray, start: Int, length: Int, platform: Int): String = when (platform) {
        PLATFORM_WINDOWS, PLATFORM_UNICODE -> String(bytes, start, length, Charsets.UTF_16BE)
        else -> String(bytes, start, length, Charsets.ISO_8859_1).filter { it.code in 0x20..0x7E }
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

    private fun readUInt(bytes: ByteArray, offset: Int): Long = readInt(bytes, offset).toLong() and 0xFFFFFFFFL

    private fun readUShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
}
