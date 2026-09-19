package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.app.Application
import android.content.ContentResolver
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookFingerprint
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookOpenError
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookOpener
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.PfdResource
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ProgressRecord
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ProgressThrottle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.getOrElse

/** Why the book did not open; the Activity maps each case to a localized message. */
internal sealed class OpenFailure {
    object CannotRead : OpenFailure()
    object NotAnEpub : OpenFailure()
    object Protected : OpenFailure()
    object TimedOut : OpenFailure()
    data class Other(val detail: String) : OpenFailure()
}

internal sealed class OpenState {
    object Idle : OpenState()
    object Opening : OpenState()
    object Ready : OpenState()
    data class Failed(val failure: OpenFailure) : OpenState()
}

/**
 * Owns the opened book across configuration changes and drives progress memory (roadmap P1.3):
 *
 * 1. the quick fingerprint (size + head + tail) keys the stored position before the reader shows;
 * 2. the full-file SHA-256 is computed in the background and the store migrates to it (D23);
 * 3. every locator change is throttled to disk, and [flushProgress] writes the pending one.
 *
 * The publication lives only in memory: after process death the Activity reopens the book from its
 * Intent and the saved locator instead of restoring fragments.
 */
internal class EpubReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val store = BookDataStore.forFilesDirectory(application.filesDir)
    private val storeMutex = Mutex()

    /** Survives [onCleared] so the final flush and a late migration always complete. */
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val throttle = ProgressThrottle<ProgressRecord>()
    private var delayedFlush: Job? = null

    private val _state = MutableStateFlow<OpenState>(OpenState.Idle)
    val state: StateFlow<OpenState> get() = _state

    private val _positionCount = MutableStateFlow(0)
    val positionCount: StateFlow<Int> get() = _positionCount

    var resource: PfdResource? = null
        private set
    var publication: Publication? = null
        private set
    var navigatorFactory: EpubNavigatorFactory? = null
        private set

    /** Where the navigator should start; updated on every locator change. */
    var lastLocator: Locator? = null
        private set

    /** The quick key until the full fingerprint replaces it. */
    @Volatile
    var bookKey: String? = null
        private set

    /** Evidence for roadmap P1.3: how long the full-file hash took and over how many bytes. */
    @Volatile
    var fullFingerprintMillis: Long? = null
        private set

    @Volatile
    var fullFingerprintBytes: Long? = null
        private set

    val positionsKnown: Boolean get() = _positionCount.value > 0

    /**
     * Opens the book once; later calls while opening or ready are ignored. [savedLocator] (from
     * `savedInstanceState`) wins over the stored progress because it is the freshest position.
     */
    fun open(request: EpubReaderRequest, contentResolver: ContentResolver, savedLocator: Locator?) {
        if (_state.value !is OpenState.Idle) return
        _state.value = OpenState.Opening
        viewModelScope.launch {
            _state.value = openBook(request, contentResolver, savedLocator)
        }
    }

    private suspend fun openBook(
        request: EpubReaderRequest,
        contentResolver: ContentResolver,
        savedLocator: Locator?,
    ): OpenState {
        val descriptor = withContext(Dispatchers.IO) {
            runCatching { contentResolver.openFileDescriptor(request.documentUri, "r") }.getOrNull()
        } ?: return OpenState.Failed(OpenFailure.CannotRead)

        val quickKey = withContext(Dispatchers.IO) {
            runCatching { descriptor.withDuplicate { BookFingerprint.quickKey(it.channel) } }.getOrNull()
        }
        // A book opened before is already filed under its full fingerprint; the quick key alias
        // finds it without hashing the whole file again before the reader shows.
        val initialKey = quickKey?.let { key ->
            withContext(Dispatchers.IO) { storeMutex.withLock { store.resolveKey(key) } }
        }
        val storedProgress = initialKey?.let { key ->
            withContext(Dispatchers.IO) { storeMutex.withLock { store.readProgress(key) } }
        }

        val resource = PfdResource(descriptor, request.displayName)
        val opened = withTimeoutOrNull(OPEN_TIMEOUT_MILLIS) { BookOpener(getApplication()).open(resource) }
        if (opened == null) {
            resource.close()
            return OpenState.Failed(OpenFailure.TimedOut)
        }
        val publication = opened.getOrElse { error ->
            resource.close()
            return OpenState.Failed(error.toFailure())
        }

        release()
        this.resource = resource
        this.publication = publication
        navigatorFactory = EpubNavigatorFactory(publication)
        bookKey = initialKey
        lastLocator = savedLocator ?: storedProgress?.let { Locator.fromJSON(it.locator) }

        viewModelScope.launch {
            _positionCount.value = runCatching { publication.positions().size }.getOrDefault(0)
        }
        if (quickKey != null) {
            viewModelScope.launch(Dispatchers.IO) { migrateToFullFingerprint(descriptor, quickKey) }
        }
        return OpenState.Ready
    }

    /**
     * Hashes the whole file, moves whatever the book accumulated under its current key to the
     * full fingerprint (a no-op when the alias already pointed there), and records the alias.
     */
    private suspend fun migrateToFullFingerprint(descriptor: ParcelFileDescriptor, quickKey: String) {
        val started = SystemClock.elapsedRealtime()
        val fullKey = runCatching {
            descriptor.withDuplicate { stream ->
                fullFingerprintBytes = stream.channel.size()
                BookFingerprint.fullKey(stream.channel)
            }
        }.getOrNull() ?: return
        fullFingerprintMillis = SystemClock.elapsedRealtime() - started
        storeMutex.withLock {
            val current = bookKey ?: return
            if (current == fullKey || store.migrate(current, fullKey)) {
                store.writeAlias(quickKey, fullKey)
                bookKey = fullKey
            }
        }
    }

    /** Called for every navigator locator; persists it through the throttle. */
    fun onLocatorChanged(locator: Locator) {
        lastLocator = locator
        val now = System.currentTimeMillis()
        val record = ProgressRecord(locator.toJSON(), now, locator.locations.totalProgression)
        val immediate = throttle.offer(record, now)
        if (immediate != null) {
            delayedFlush?.cancel()
            persist(immediate)
        } else if (delayedFlush?.isActive != true) {
            delayedFlush = viewModelScope.launch {
                delay(ProgressThrottle.DEFAULT_MIN_INTERVAL_MILLIS)
                flushProgress()
            }
        }
    }

    /** Writes whatever is still pending (pause, close, process end). */
    fun flushProgress() {
        delayedFlush?.cancel()
        throttle.flush(System.currentTimeMillis())?.let(::persist)
    }

    /** "Start from the beginning": forgets the stored position of this book. */
    fun clearProgress() {
        delayedFlush?.cancel()
        throttle.flush(System.currentTimeMillis())
        val key = bookKey ?: return
        persistScope.launch { storeMutex.withLock { store.clearProgress(key) } }
    }

    private fun persist(record: ProgressRecord) {
        persistScope.launch {
            storeMutex.withLock { bookKey?.let { store.writeProgress(it, record) } }
        }
    }

    private fun release() {
        navigatorFactory = null
        publication?.close()
        publication = null
        resource?.close()
        resource = null
        _positionCount.value = 0
    }

    override fun onCleared() {
        flushProgress()
        release()
    }

    private fun BookOpenError.toFailure(): OpenFailure = when (this) {
        is BookOpenError.NotAnEpub -> OpenFailure.NotAnEpub
        is BookOpenError.Protected -> OpenFailure.Protected
        is BookOpenError.Retrieve, is BookOpenError.Open -> OpenFailure.Other(message)
    }

    private inline fun <T> ParcelFileDescriptor.withDuplicate(
        block: (ParcelFileDescriptor.AutoCloseInputStream) -> T,
    ): T = ParcelFileDescriptor.AutoCloseInputStream(dup()).use(block)

    companion object {
        const val OPEN_TIMEOUT_MILLIS = 60_000L
    }
}
