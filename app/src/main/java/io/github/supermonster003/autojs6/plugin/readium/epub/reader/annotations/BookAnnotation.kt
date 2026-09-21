package io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One highlight of one book (roadmap P9 / D4), with an optional note: the Readium `Locator` of
 * the selected text as JSON (href, type, title, `locations` with progression / position /
 * cssSelector / domRange, `text` with the quote and its context), the decoration style and
 * colour, the note, and what the list and the export show: the chapter title and the quote
 * captured at creation time. Rows are keyed by the book's content fingerprint like
 * `progress.json` (D13 / D23); the key moves with the fingerprint migration.
 *
 * The class doubles as the Room entity of the `annotations` table (schema version 1, exported to
 * `app/schemas/`); everything else about it is plain Kotlin so the policies stay Android-free.
 */
@Entity(tableName = "annotations", indices = [Index("bookKey"), Index("bookKey", "href")])
data class BookAnnotation(
    /** Room's row id; 0 for a draft that has not been inserted yet. Never reused after a delete. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** The book's fingerprint (quick key while opening, full SHA-256 after the migration). */
    val bookKey: String,
    /** The resource of the locator, denormalized for grouping and ordering. */
    val href: String,
    /** The Readium locator as JSON text, exactly as `Locator.toJSON()` wrote it. */
    val locator: String,
    /** [AnnotationStyle.HIGHLIGHT] or [AnnotationStyle.UNDERLINE]. */
    val style: String = AnnotationStyle.HIGHLIGHT,
    /** Opaque ARGB colour; see [AnnotationColors]. */
    val color: Int = AnnotationColors.DEFAULT,
    /** The note, or null for a bare highlight. */
    val note: String? = null,
    /** The selected text at creation, whitespace collapsed and capped. */
    val quote: String? = null,
    /** The chapter title at creation, for the list and the export. */
    val chapter: String? = null,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
) {
    val hasNote: Boolean get() = !note.isNullOrEmpty()
}

/** The two decoration styles Readium's navigator renders. */
internal object AnnotationStyle {
    const val HIGHLIGHT = "highlight"
    const val UNDERLINE = "underline"

    val ALL: List<String> = listOf(HIGHLIGHT, UNDERLINE)

    /** A stored or foreign style name normalized to one of [ALL]. */
    fun normalize(style: String?): String = if (style == UNDERLINE) UNDERLINE else HIGHLIGHT
}

/**
 * The highlight palette (roadmap P9): five opaque colours that read on the light, sepia and dark
 * themes once Readium applies its own 30% alpha to a highlight tint. Stored as opaque ARGB so the
 * export and the contract can print them as `#RRGGBB`.
 */
internal object AnnotationColors {
    const val YELLOW = 0xFFFFD54F.toInt()
    const val GREEN = 0xFF81C784.toInt()
    const val BLUE = 0xFF64B5F6.toInt()
    const val PINK = 0xFFF48FB1.toInt()
    const val PURPLE = 0xFFB39DDB.toInt()

    val PALETTE: List<Int> = listOf(YELLOW, GREEN, BLUE, PINK, PURPLE)

    const val DEFAULT = YELLOW

    private const val RGB_MASK = 0xFFFFFF
    private const val OPAQUE = 0xFF000000.toInt()

    /** Any ARGB value forced opaque, so a foreign or translucent value still renders and prints. */
    fun normalize(color: Int): Int = color or OPAQUE

    /** `#RRGGBB`, uppercase, alpha dropped. */
    fun hex(color: Int): String = "#%06X".format(color and RGB_MASK)

    /** The opaque colour of a `#RRGGBB` (or `#RGB`) string, or null for anything else. */
    fun parseHex(text: String?): Int? {
        val hex = text?.trim()?.removePrefix("#") ?: return null
        val digits = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> return null
        }
        val rgb = digits.toIntOrNull(16) ?: return null
        return normalize(rgb)
    }

    /** Index of [color] in the palette, or -1 for a colour the palette does not have. */
    fun indexOf(color: Int): Int = PALETTE.indexOf(normalize(color))
}
