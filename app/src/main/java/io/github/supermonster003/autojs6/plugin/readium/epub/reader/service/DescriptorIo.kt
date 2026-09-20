package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.os.Bundle
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import kotlinx.coroutines.runBlocking
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.Resource
import java.io.IOException
import java.util.concurrent.Executors

/** Descriptor and parcel plumbing of the EPUB service (roadmap P5.2, appendix B.3). */
internal object DescriptorIo {

    /** Bytes per pipe write while streaming a resource to the host. */
    const val CHUNK_BYTES = 256 * 1024

    private val streamers = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "epub-resource-stream").apply { isDaemon = true }
    }

    /** The host must hand over a regular file: pipes and sockets have no length and cannot be seeked. */
    fun requireRegularFile(descriptor: ParcelFileDescriptor) {
        val stat = try {
            Os.fstat(descriptor.fileDescriptor)
        } catch (e: ErrnoException) {
            throw ContractViolation(EpubErrorCodes.FILE_UNREADABLE, "fstat failed: ${e.message}")
        }
        if (!OsConstants.S_ISREG(stat.st_mode)) {
            throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "source is not a regular file")
        }
    }

    /** Size of a bundle once parcelled, the measure the contract's byte ceilings use. */
    fun parcelSize(bundle: Bundle?): Int {
        if (bundle == null) return 0
        val parcel = Parcel.obtain()
        try {
            bundle.writeToParcel(parcel, 0)
            return parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    /**
     * Streams [resource] into a reliable pipe and returns its read end for the host. The write
     * end is closed with an error when a read fails, so the host's `checkError()` distinguishes
     * a truncated stream from a complete one. The resource is closed once the stream ends.
     */
    fun stream(resource: Resource, length: Long): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createReliablePipe()
        val read = pipe[0]
        val write = pipe[1]
        streamers.execute { copy(resource, length, write) }
        return read
    }

    private fun copy(resource: Resource, length: Long, write: ParcelFileDescriptor) {
        val output = ParcelFileDescriptor.AutoCloseOutputStream(write)
        var failure: String? = null
        try {
            var position = 0L
            while (position < length) {
                val end = minOf(length, position + CHUNK_BYTES) - 1
                val bytes = runBlocking {
                    resource.read(position..end).getOrElse { error -> throw IOException(error.message) }
                }
                if (bytes.isEmpty()) break
                output.write(bytes)
                position += bytes.size
            }
            output.flush()
        } catch (e: IOException) {
            failure = e.message ?: e.javaClass.simpleName
        } catch (e: RuntimeException) {
            failure = e.message ?: e.javaClass.simpleName
        } finally {
            try {
                if (failure != null) write.closeWithError(Limits.errorDetail(failure)) else output.close()
            } catch (_: IOException) {
            }
            runCatching { output.close() }
            runCatching { resource.close() }
        }
    }
}
