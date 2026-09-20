package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.BuildConfig
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookOpenError
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookOpener
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.PfdResource
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.epubCapabilities
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.readiumEpubPluginInfo
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.autojs.plugin.epub.api.IEpubBook
import org.autojs.plugin.epub.api.IEpubPlugin
import org.autojs.plugin.epub.api.IEpubReaderCallback
import org.autojs.plugin.epub.api.IEpubReaderSession
import org.readium.r2.shared.util.getOrElse

/**
 * `org.autojs.plugin.EPUB` capability service (roadmap P5.2 / D10): the `IEpubPlugin` entry of
 * the host, sharing the package identity of the Explorer Action service with engine `epub`.
 * Books are opened from the host's read-only descriptor, capped and idle-swept by
 * [BookRegistry], and all closed when the host unbinds. Reader sessions arrive with roadmap P5.3.
 */
class ReadiumEpubReaderPluginService : Service() {

    private val guard by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { CallerGuard(this, allowOwnUid = BuildConfig.DEBUG) }
    private val opener by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { BookOpener(this) }
    private val registry = BookRegistry()

    private val binder = object : IEpubPlugin.Stub() {
        override fun getInfo(): PluginInfo {
            guard.check()
            return readiumEpubPluginInfo()
        }

        override fun getCapabilities(): Bundle {
            guard.check()
            return epubCapabilities()
        }

        override fun openBook(source: ParcelFileDescriptor?, options: Bundle?): IEpubBook {
            guard.check()
            return open(source, options)
        }

        override fun openReader(source: ParcelFileDescriptor?, options: Bundle?, callback: IEpubReaderCallback?): IEpubReaderSession {
            guard.check()
            source?.let { runCatching { it.close() } }
            throw IllegalStateException(EpubErrorCodes.encode(EpubErrorCodes.INTERNAL, "reader sessions arrive with roadmap P5.3"))
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    /** The last client left: the host's leases ended, so nothing may keep a book open. */
    override fun onUnbind(intent: Intent?): Boolean {
        registry.closeAll()
        return false
    }

    override fun onDestroy() {
        registry.shutdown()
        super.onDestroy()
    }

    private fun open(source: ParcelFileDescriptor?, options: Bundle?): IEpubBook {
        val descriptor = source
            ?: throw IllegalArgumentException(EpubErrorCodes.encode(EpubErrorCodes.INVALID_ARGUMENT, "source descriptor is missing"))
        var resource: PfdResource? = null
        try {
            Limits.optionsBytes(DescriptorIo.parcelSize(options))
            DescriptorIo.requireRegularFile(descriptor)
            registry.checkCapacity()
            val displayName = options?.getString(EpubContract.KEY_DISPLAY_NAME)
                ?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_DISPLAY_NAME_CHARS)
            val pfdResource = PfdResource(descriptor, displayName)
            resource = pfdResource
            val opened = runBlocking { withTimeoutOrNull(EpubContract.OPEN_TIMEOUT_MS) { opener.open(pfdResource) } }
                ?: throw ContractViolation(EpubErrorCodes.TIMEOUT, "opening took longer than ${EpubContract.OPEN_TIMEOUT_MS} ms")
            val publication = opened.getOrElse { error -> throw ContractViolation(codeOf(error), error.message) }
            val book = EpubBookBinder(publication, pfdResource, guard, registry::remove)
            try {
                registry.register(book)
            } catch (e: ContractViolation) {
                book.release()
                throw e
            }
            return book
        } catch (e: ContractViolation) {
            closeQuietly(resource, descriptor)
            throw Answers.failure(e)
        } catch (e: SecurityException) {
            closeQuietly(resource, descriptor)
            throw e
        } catch (e: Throwable) {
            closeQuietly(resource, descriptor)
            throw IllegalStateException(EpubErrorCodes.encode(EpubErrorCodes.INTERNAL, e.toString()))
        }
    }

    private fun closeQuietly(resource: PfdResource?, descriptor: ParcelFileDescriptor) {
        if (resource != null) resource.close() else runCatching { descriptor.close() }
    }

    private fun codeOf(error: BookOpenError): String = when (error) {
        is BookOpenError.NotAnEpub, is BookOpenError.Retrieve -> EpubErrorCodes.NOT_EPUB
        is BookOpenError.Protected -> EpubErrorCodes.ENCRYPTED
        is BookOpenError.Open -> EpubErrorCodes.PARSE_FAILED
    }

    private companion object {
        const val MAX_DISPLAY_NAME_CHARS = 255
    }
}
