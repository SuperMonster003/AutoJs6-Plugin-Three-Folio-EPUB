package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import kotlin.random.Random

/**
 * Roadmap P1.3 evidence (D23): how long the full-file SHA-256 takes through a descriptor on this
 * device, next to the quick key. The sample is synthesized in the cache (200 MB when the device
 * has room, 64 MB otherwise) and deleted afterwards; the numbers land in `files/p1-evidence/`.
 */
@RunWith(AndroidJUnit4::class)
class BookFingerprintInstrumentationTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun fullFingerprintTimingThroughADescriptor() {
        val free = StatFs(context.cacheDir.path).availableBytes
        val size = if (free > 1L shl 30) 200L shl 20 else 64L shl 20
        val sample = File(context.cacheDir, "fingerprint-sample.bin")
        try {
            RandomAccessFile(sample, "rw").use { file ->
                file.setLength(size)
                val block = Random(7).nextBytes(1 shl 20)
                var written = 0L
                while (written < size) {
                    val count = minOf(block.size.toLong(), size - written).toInt()
                    file.write(block, 0, count)
                    written += count
                }
            }
            val descriptor = ParcelFileDescriptor.open(sample, ParcelFileDescriptor.MODE_READ_ONLY)
            val (quick, quickMillis) = timed { ParcelFileDescriptor.AutoCloseInputStream(descriptor.dup()).use { BookFingerprint.quickKey(it.channel) } }
            val (full, fullMillis) = timed { ParcelFileDescriptor.AutoCloseInputStream(descriptor.dup()).use { BookFingerprint.fullKey(it.channel) } }
            val (fullAgain, _) = timed { ParcelFileDescriptor.AutoCloseInputStream(descriptor.dup()).use { BookFingerprint.fullKey(it.channel) } }
            descriptor.close()

            assertEquals(64, quick.length)
            assertEquals(full, fullAgain)
            assertNotEquals(quick, full)

            File(context.filesDir, "p1-evidence").apply { mkdirs() }
                .resolve("fingerprint-api${Build.VERSION.SDK_INT}.txt")
                .writeText(
                    listOf(
                        "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                        "sampleBytes=$size",
                        "quickKeyMillis=$quickMillis",
                        "fullKeyMillis=$fullMillis",
                        "fullKeyMiBPerSecond=${"%.1f".format((size / 1048576.0) / (fullMillis.coerceAtLeast(1) / 1000.0))}",
                    ).joinToString("\n") + "\n",
                )
        } finally {
            sample.delete()
        }
    }

    private inline fun <T> timed(block: () -> T): Pair<T, Long> {
        val started = SystemClock.elapsedRealtime()
        val value = block()
        return value to (SystemClock.elapsedRealtime() - started)
    }
}
