package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest

/**
 * Content fingerprints of a book (roadmap D23), independent of its path or display name.
 *
 * - [quickKey] hashes the size plus the first 1 MiB and the last 64 KiB; it is cheap enough to
 *   compute before the reader opens and serves as the temporary progress key.
 * - [fullKey] hashes the complete file and is the canonical key the progress store migrates to.
 *
 * Both are lowercase hex SHA-256 digests. Pure JVM so JUnit covers them with temp files.
 */
internal object BookFingerprint {

    const val QUICK_HEAD_BYTES = 1L shl 20
    const val QUICK_TAIL_BYTES = 64L shl 10
    private const val CHUNK = 1 shl 16

    fun quickKey(channel: FileChannel): String {
        val length = channel.size()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(length).array())
        val head = minOf(QUICK_HEAD_BYTES, length)
        digest.updateRange(channel, 0L, head)
        val tailStart = maxOf(head, length - QUICK_TAIL_BYTES)
        if (tailStart < length) digest.updateRange(channel, tailStart, length - tailStart)
        return digest.digest().toHex()
    }

    fun fullKey(channel: FileChannel): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.updateRange(channel, 0L, channel.size())
        return digest.digest().toHex()
    }

    fun fullKey(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(CHUNK)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().toHex()
    }

    private fun MessageDigest.updateRange(channel: FileChannel, offset: Long, count: Long) {
        var position = offset
        val end = offset + count
        val buffer = ByteBuffer.allocate(CHUNK)
        while (position < end) {
            buffer.clear()
            buffer.limit(minOf(CHUNK.toLong(), end - position).toInt())
            val read = channel.read(buffer, position)
            if (read <= 0) break
            buffer.flip()
            update(buffer)
            position += read
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
