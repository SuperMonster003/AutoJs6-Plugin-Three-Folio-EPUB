package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.random.Random

class BookFingerprintTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun file(name: String, bytes: ByteArray): File = folder.newFile(name).apply { writeBytes(bytes) }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun <T> withChannel(file: File, block: (java.nio.channels.FileChannel) -> T): T =
        RandomAccessFile(file, "r").use { block(it.channel) }

    @Test
    fun fullKeyIsThePlainSha256OfTheFile() {
        val bytes = Random(1).nextBytes(300_000)
        val book = file("book.epub", bytes)
        assertEquals(sha256(bytes), withChannel(book, BookFingerprint::fullKey))
        assertEquals(sha256(bytes), book.inputStream().use(BookFingerprint::fullKey))
        assertEquals(sha256(ByteArray(0)), withChannel(file("empty.epub", ByteArray(0)), BookFingerprint::fullKey))
    }

    @Test
    fun quickKeyIsDeterministicAndIndependentOfTheFileName() {
        val bytes = Random(2).nextBytes(2_000_000)
        val first = file("a.epub", bytes)
        val second = file("b.epub", bytes)
        val key = withChannel(first, BookFingerprint::quickKey)
        assertEquals(key, withChannel(second, BookFingerprint::quickKey))
        assertTrue(key.matches(Regex("[0-9a-f]{64}")))
        assertNotEquals(key, withChannel(first, BookFingerprint::fullKey))
    }

    @Test
    fun quickKeyCoversSizeHeadAndTailOnly() {
        val head = BookFingerprint.QUICK_HEAD_BYTES.toInt()
        val tail = BookFingerprint.QUICK_TAIL_BYTES.toInt()
        val bytes = Random(3).nextBytes(head + 4096 + tail)
        val baseline = withChannel(file("base.epub", bytes), BookFingerprint::quickKey)

        val middleChanged = bytes.copyOf().also { it[head + 100] = (it[head + 100] + 1).toByte() }
        assertEquals(baseline, withChannel(file("middle.epub", middleChanged), BookFingerprint::quickKey))

        val headChanged = bytes.copyOf().also { it[10] = (it[10] + 1).toByte() }
        assertNotEquals(baseline, withChannel(file("head.epub", headChanged), BookFingerprint::quickKey))

        val tailChanged = bytes.copyOf().also { it[it.size - 10] = (it[it.size - 10] + 1).toByte() }
        assertNotEquals(baseline, withChannel(file("tail.epub", tailChanged), BookFingerprint::quickKey))

        val longer = bytes + byteArrayOf(0)
        assertNotEquals(baseline, withChannel(file("longer.epub", longer), BookFingerprint::quickKey))
    }

    @Test
    fun smallFilesAreHashedCompletelyByTheQuickKeyWithoutDoubleCounting() {
        val bytes = Random(4).nextBytes(50_000)
        val expected = MessageDigest.getInstance("SHA-256").run {
            update(java.nio.ByteBuffer.allocate(8).putLong(bytes.size.toLong()).array())
            update(bytes)
            digest().joinToString("") { "%02x".format(it) }
        }
        assertEquals(expected, withChannel(file("small.epub", bytes), BookFingerprint::quickKey))
    }
}
