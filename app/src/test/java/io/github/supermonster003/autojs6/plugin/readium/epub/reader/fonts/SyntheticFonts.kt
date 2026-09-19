package io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Builds minimal SFNT files for the validator and store tests: a table directory, an optional
 * `name` table and a `head` filler whose bytes depend on [build]'s seed so two fonts never hash alike.
 */
internal object SyntheticFonts {

    const val TRUE_TYPE = 0x00010000
    const val OPEN_TYPE = 0x4F54544F
    const val COLLECTION = 0x74746366

    data class NameRecord(val platform: Int, val encoding: Int, val language: Int, val nameId: Int, val text: String)

    fun windows(nameId: Int, text: String, language: Int = 0x0409) = NameRecord(3, 1, language, nameId, text)

    fun unicode(nameId: Int, text: String) = NameRecord(0, 3, 0, nameId, text)

    fun macRoman(nameId: Int, text: String) = NameRecord(1, 0, 0, nameId, text)

    /** [names] null leaves the `name` table out entirely. */
    fun build(signature: Int = TRUE_TYPE, names: List<NameRecord>? = emptyList(), seed: Int = 0): ByteArray {
        val tables = mutableListOf<Pair<String, ByteArray>>()
        if (names != null) tables += "name" to nameTable(names)
        tables += "head" to ByteArray(54) { (it + seed).toByte() }

        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)
        data.writeInt(signature)
        data.writeShort(tables.size)
        data.writeShort(0)
        data.writeShort(0)
        data.writeShort(0)
        var offset = 12 + tables.size * 16
        val bodies = mutableListOf<ByteArray>()
        for ((tag, body) in tables) {
            data.writeBytes(tag)
            data.writeInt(0)
            data.writeInt(offset)
            data.writeInt(body.size)
            val padded = (body.size + 3) / 4 * 4
            bodies += body.copyOf(padded)
            offset += padded
        }
        bodies.forEach(data::write)
        return out.toByteArray()
    }

    private fun nameTable(records: List<NameRecord>): ByteArray {
        val strings = ByteArrayOutputStream()
        val placed = records.map { record ->
            val bytes = if (record.platform == 1) {
                record.text.toByteArray(Charsets.ISO_8859_1)
            } else {
                record.text.toByteArray(Charsets.UTF_16BE)
            }
            val position = strings.size()
            strings.write(bytes)
            Triple(record, position, bytes.size)
        }
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)
        data.writeShort(0)
        data.writeShort(records.size)
        data.writeShort(6 + records.size * 12)
        for ((record, position, length) in placed) {
            data.writeShort(record.platform)
            data.writeShort(record.encoding)
            data.writeShort(record.language)
            data.writeShort(record.nameId)
            data.writeShort(length)
            data.writeShort(position)
        }
        data.write(strings.toByteArray())
        return out.toByteArray()
    }
}
