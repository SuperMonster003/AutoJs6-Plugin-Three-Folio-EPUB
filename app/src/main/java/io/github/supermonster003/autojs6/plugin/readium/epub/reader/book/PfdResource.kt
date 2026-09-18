package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.resource.mediaType
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * A Readium [Resource] backed by a read-only [ParcelFileDescriptor] (roadmap D11).
 *
 * Ranged reads are positional [FileChannel] reads, so the EPUB container is never copied and the
 * streaming ZIP provider can seek freely. A `null` [sourceUrl] steers Readium's archive opener to
 * `StreamingZipArchiveProvider` instead of the file-based path. Closing the resource closes the
 * descriptor; the caller must not close it separately.
 */
class PfdResource(
    private val descriptor: ParcelFileDescriptor,
    private val fileName: String?,
    private val mediaTypeHint: MediaType? = MediaType.EPUB,
) : Resource {

    private val stream = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
    private val channel: FileChannel = stream.channel

    @Volatile
    private var closed = false

    override val sourceUrl: AbsoluteUrl? = null

    private val properties = Resource.Properties {
        filename = fileName
        mediaType = mediaTypeHint
    }

    override suspend fun properties(): Try<Resource.Properties, ReadError> = Try.success(properties)

    override suspend fun length(): Try<Long, ReadError> = withContext(Dispatchers.IO) {
        catching { lengthSync() }
    }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> = withContext(Dispatchers.IO) {
        catching { readSync(range) }
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            stream.close()
        } catch (_: IOException) {
        }
    }

    private fun lengthSync(): Long {
        checkOpen()
        val statSize = descriptor.statSize
        return if (statSize >= 0) statSize else channel.size()
    }

    private fun readSync(range: LongRange?): ByteArray {
        checkOpen()
        val (offset, count) = ByteRanges.clamp(range, lengthSync()) ?: return ByteArray(0)
        val buffer = ByteBuffer.allocate(count)
        var position = offset
        while (buffer.hasRemaining()) {
            val read = channel.read(buffer, position)
            if (read < 0) break
            position += read
        }
        return if (buffer.hasRemaining()) buffer.array().copyOf(buffer.position()) else buffer.array()
    }

    private fun checkOpen() {
        if (closed) throw IOException("PfdResource is closed")
    }

    private inline fun <T> catching(block: () -> T): Try<T, ReadError> =
        try {
            Try.success(block())
        } catch (e: FileNotFoundException) {
            Try.failure(ReadError.Access(FileSystemError.FileNotFound(e)))
        } catch (e: SecurityException) {
            Try.failure(ReadError.Access(FileSystemError.Forbidden(e)))
        } catch (e: IOException) {
            Try.failure(ReadError.Access(FileSystemError.IO(e)))
        } catch (e: RuntimeException) {
            Try.failure(ReadError.Access(FileSystemError.IO(e)))
        } catch (e: OutOfMemoryError) {
            Try.failure(ReadError.OutOfMemory(e))
        }

    override fun toString(): String = "PfdResource(${fileName ?: "?"}, fd=${descriptor.fd})"
}
