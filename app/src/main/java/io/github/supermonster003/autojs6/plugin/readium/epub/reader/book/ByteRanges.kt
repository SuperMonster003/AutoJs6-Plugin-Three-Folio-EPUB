package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

/** Pure range arithmetic shared by [PfdResource]; kept Android-free so JUnit covers every edge. */
internal object ByteRanges {

    /**
     * Clamps a requested inclusive [range] to the readable window `[0, length)` and returns the
     * `(offset, count)` pair to read, or `null` when nothing remains. A `null` range means the
     * whole resource. [length] must be non-negative.
     */
    fun clamp(range: LongRange?, length: Long): Pair<Long, Int>? {
        require(length >= 0) { "length must not be negative: $length" }
        if (length == 0L) return null
        val first = (range?.first ?: 0L).coerceAtLeast(0L)
        val last = (range?.last ?: (length - 1)).coerceAtMost(length - 1)
        if (first > last) return null
        val count = last - first + 1
        require(count <= Int.MAX_VALUE) { "range too large to fit in a byte array: $count bytes" }
        return first to count.toInt()
    }
}
