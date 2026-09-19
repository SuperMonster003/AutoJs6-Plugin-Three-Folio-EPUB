package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookFingerprint
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookOpenError
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookOpener
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.FontsContainer
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.PfdResource
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontCatalog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontEntry
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontInspection
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ImageDecoding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.LinkHistory
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchSession
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchState
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.Bookmark
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookmarkCodec
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.FontImportResult
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.FontStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ProgressRecord
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ProgressThrottle
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.OrphanBook
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsController
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.use

/** Why the book did not open; the Activity maps each case to a localized message. */
internal sealed class OpenFailure {
    object CannotRead : OpenFailure()
    object NotAnEpub : OpenFailure()
    object Protected : OpenFailure()
    object TimedOut : OpenFailure()
    data class Other(val detail: String) : OpenFailure()
}

/** Outcome of [EpubReaderViewModel.addBookmark]. */
internal sealed class BookmarkAddResult {
    data class Added(val bookmark: Bookmark) : BookmarkAddResult()
    data class Full(val limit: Int) : BookmarkAddResult()
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
 * It also owns the global reading preferences (roadmap P2.1 / D14): loaded from
 * `reader-preferences.json` before the book opens, edited in place by the panel and the menu, and
 * written back atomically after a short debounce (or from [flushPreferences] on pause), and the
 * imported fonts (roadmap P2.2): the catalog is read once, served to the book through
 * [FontsContainer], and updated by [importFont] / [deleteFont], and the full-text search
 * (roadmap P2.5): one [SearchSession] per book, whose results and active hit survive rotation and
 * the panel being closed, and which is dropped with the book, and the bookmarks (roadmap P2.6):
 * read with the progress under the book's key, edited in memory and written whole after each
 * change, and united with whatever the full fingerprint already held once the migration runs.
 *
 * The publication lives only in memory: after process death the Activity reopens the book from its
 * Intent and the saved locator instead of restoring fragments.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class EpubReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val store = BookDataStore.forFilesDirectory(application.filesDir)
    private val storeMutex = Mutex()

    private val preferencesStore = ReaderPreferencesStore.forFilesDirectory(application.filesDir)
    private val preferencesMutex = Mutex()

    /**
     * Survives [onCleared] so the final flush and a late migration always complete; single-lane so
     * two writes of the same file can never land out of order.
     */
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val throttle = ProgressThrottle<ProgressRecord>()
    private var delayedFlush: Job? = null

    private val _state = MutableStateFlow<OpenState>(OpenState.Idle)
    val state: StateFlow<OpenState> get() = _state

    private val _positionCount = MutableStateFlow(0)
    val positionCount: StateFlow<Int> get() = _positionCount

    /**
     * Read synchronously, once per process: the file is a few hundred bytes and reading it before
     * the first frame is what keeps a dark or sepia reader from flashing the light chrome on open.
     */
    private val _preferences = MutableStateFlow(
        preferencesStore.read()?.let(ReaderPreferencesState::fromStored) ?: ReaderPreferencesState.DEFAULT,
    )

    /** The global reading preferences (roadmap D14). */
    val preferences: StateFlow<ReaderPreferencesState> get() = _preferences

    private var preferencesDirty = false
    private var delayedPreferencesFlush: Job? = null

    private val fontStore = FontStore.forFilesDirectory(application.filesDir)
    private val fontsMutex = Mutex()
    private val _fonts = MutableStateFlow(fontStore.read())

    /** The imported fonts (roadmap P2.2); the Activity rebuilds the navigator when this changes. */
    val fonts: StateFlow<FontCatalog> get() = _fonts

    private val searchSession = SearchSession(viewModelScope)

    /** The full-text search over the open book (roadmap P2.5). */
    val search: StateFlow<SearchState> get() = searchSession.state

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())

    /** Where in-book links were followed from, for the back key (roadmap P2.7). */
    val linkHistory = LinkHistory<Locator>()

    /** Read-aloud (roadmap P3): stops with the book and with this view model (roadmap D15). */
    val tts = TtsController(application, viewModelScope, TtsPreferencesStore.forFilesDirectory(application.filesDir))

    /** The bookmarks of the open book in creation order (roadmap P2.6). */
    val bookmarks: StateFlow<List<Bookmark>> get() = _bookmarks

    init {
        // The first run after the pre-P2.1 builds adopts the old scroll toggle so an update keeps
        // the user's choice; a corrupt file (exists but unreadable) falls back to the defaults.
        if (!preferencesStore.exists() && ReaderSettings(application).scrollMode) {
            _preferences.value = ReaderPreferencesState.DEFAULT.copy(epub = EpubPreferences(scroll = true))
            preferencesDirty = true
            flushPreferences()
        }
    }

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
        val storedBookmarks = initialKey?.let { key ->
            withContext(Dispatchers.IO) { storeMutex.withLock { store.readBookmarks(key) } }
        }.orEmpty()

        val resource = PfdResource(descriptor, request.displayName)
        val opened = withTimeoutOrNull(OPEN_TIMEOUT_MILLIS) {
            BookOpener(getApplication()).open(resource, FontsContainer(fontStore))
        }
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
        _bookmarks.value = storedBookmarks
        searchSession.attach(publication)
        navigatorFactory = EpubNavigatorFactory(publication)
        bookKey = initialKey
        // Roadmap D26: a voice still reading this book in the background comes back to the reader, which opens where it speaks.
        val speaking = tts.adoptSpeaking(initialKey)
        lastLocator = speaking?.location?.value?.locator ?: savedLocator ?: storedProgress?.let { Locator.fromJSON(it.locator) }

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
            // Store writes never take the reader down (see persist): a failed alias write only
            // means the next open fingerprints the file again.
            runCatching {
                if (current == fullKey || store.migrate(current, fullKey)) {
                    store.writeAlias(quickKey, fullKey)
                    bookKey = fullKey
                    // The full fingerprint may already hold bookmarks this open did not see (its
                    // alias was missing): unite them with the ones in memory, memory first.
                    val onDisk = store.readBookmarks(fullKey)
                    if (onDisk.isNotEmpty()) {
                        val united = BookmarkCodec.merge(_bookmarks.value, onDisk)
                        _bookmarks.value = united
                        store.writeBookmarks(fullKey, united)
                    }
                }
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

    /**
     * A write can fail underneath the reader (book directory removed meanwhile, storage not
     * writable); that loses this one record and must never crash the process, as an uncaught
     * exception on [persistScope] would.
     */
    private fun persist(record: ProgressRecord) {
        persistScope.launch {
            storeMutex.withLock { bookKey?.let { key -> runCatching { store.writeProgress(key, record) } } }
        }
    }

    // ---- Reading preferences (roadmap P2.1) ----

    /** Applies [transform] to the stored Readium preferences; the theme stays derived from the mode. */
    fun editPreferences(transform: (EpubPreferences) -> EpubPreferences) {
        _preferences.update { it.copy(epub = transform(it.epub).copy(theme = null)) }
        schedulePreferencesFlush()
    }

    fun setThemeMode(mode: ThemeMode) {
        _preferences.update { it.copy(themeMode = mode) }
        schedulePreferencesFlush()
    }

    /** "Restore defaults": every preference back to Readium's defaults and the theme back to the host's. */
    fun resetPreferences() {
        _preferences.value = ReaderPreferencesState.DEFAULT
        schedulePreferencesFlush()
    }

    private fun schedulePreferencesFlush() {
        preferencesDirty = true
        delayedPreferencesFlush?.cancel()
        delayedPreferencesFlush = viewModelScope.launch {
            delay(PREFERENCES_FLUSH_DELAY_MILLIS)
            flushPreferences()
        }
    }

    /** Writes the preferences now when an edit is pending (pause, close, process end). */
    fun flushPreferences() {
        delayedPreferencesFlush?.cancel()
        if (!preferencesDirty) return
        preferencesDirty = false
        val stored = _preferences.value.toStored()
        persistScope.launch {
            preferencesMutex.withLock { runCatching { preferencesStore.write(stored) } }
        }
    }

    // ---- Imported fonts (roadmap P2.2) ----

    /**
     * Copies the document behind [uri] into the font store and refreshes [fonts]. The document's
     * display name only serves as an extension pre-check and as the fallback display name; the
     * store decides on the file's own signature.
     */
    suspend fun importFont(contentResolver: ContentResolver, uri: Uri): FontImportResult = withContext(Dispatchers.IO) {
        fontsMutex.withLock {
            val hint = runCatching { displayName(contentResolver, uri) }.getOrNull()
            val extension = hint?.substringAfterLast('.', "")?.lowercase()
            val result = if (!extension.isNullOrEmpty() && extension !in FONT_EXTENSIONS) {
                FontImportResult.Rejected(FontInspection.Rejected.NotAFont)
            } else {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { fontStore.import(it, hint) } ?: FontImportResult.Failed
                }.getOrDefault(FontImportResult.Failed)
            }
            _fonts.value = fontStore.read()
            result
        }
    }

    /** Deletes [entry]; a preference pointing at it goes back to the publisher's font. */
    suspend fun deleteFont(entry: FontEntry): Boolean {
        val deleted = withContext(Dispatchers.IO) {
            fontsMutex.withLock { fontStore.delete(entry.sha256).also { _fonts.value = fontStore.read() } }
        }
        if (deleted && _preferences.value.epub.fontFamily?.name == entry.family) {
            editPreferences { it.copy(fontFamily = null) }
        }
        return deleted
    }

    // ---- Bookmarks (roadmap P2.6) ----

    /**
     * Adds a bookmark for [locator] unless the book already has [BookmarkCodec.MAX_BOOKMARKS];
     * ids grow monotonically so a deleted bookmark's id is never reused.
     */
    fun addBookmark(locator: JSONObject, chapter: String?, snippet: String?): BookmarkAddResult {
        val current = _bookmarks.value
        if (current.size >= BookmarkCodec.MAX_BOOKMARKS) return BookmarkAddResult.Full(BookmarkCodec.MAX_BOOKMARKS)
        val bookmark = Bookmark(
            id = (current.maxOfOrNull { it.id } ?: -1L) + 1,
            locator = locator,
            createdAtMillis = System.currentTimeMillis(),
            chapter = chapter,
            snippet = snippet,
        )
        _bookmarks.value = current + bookmark
        persistBookmarks()
        return BookmarkAddResult.Added(bookmark)
    }

    /** Removes the bookmark with [id]; returns false when there is none. */
    fun removeBookmark(id: Long): Boolean {
        val current = _bookmarks.value
        val remaining = current.filterNot { it.id == id }
        if (remaining.size == current.size) return false
        _bookmarks.value = remaining
        persistBookmarks()
        return true
    }

    fun clearBookmarks() {
        if (_bookmarks.value.isEmpty()) return
        _bookmarks.value = emptyList()
        persistBookmarks()
    }

    /** Whole-file write of the current list; like [persist], a failure loses this write, not the reader. */
    private fun persistBookmarks() {
        val snapshot = _bookmarks.value
        persistScope.launch {
            storeMutex.withLock { bookKey?.let { key -> runCatching { store.writeBookmarks(key, snapshot) } } }
        }
    }

    // ---- Full-text search (roadmap P2.5) ----

    fun search(query: String) = searchSession.search(query)

    fun loadMoreSearchResults() = searchSession.loadMore()

    fun cancelSearch() = searchSession.cancel()

    fun selectSearchResult(index: Int?) = searchSession.select(index)

    fun closeSearch() = searchSession.close()

    private fun displayName(contentResolver: ContentResolver, uri: Uri): String? =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    /**
     * The image at [href] inside the open book, downsampled so its longer side is at most
     * [maxSide] pixels; null when the book has no such resource or it does not decode.
     */
    suspend fun decodeImage(href: String, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
        val publication = publication ?: return@withContext null
        val url = Url(href) ?: return@withContext null
        val bytes = publication.get(url)?.use { resource -> resource.read().getOrNull() } ?: return@withContext null
        ImageDecoding.decode(bytes, maxSide)
    }

    /**
     * Roadmap D26: the read-aloud notification reopened the reader. Takes the parked session and the
     * book it reads from, shows that book at the spoken sentence and keeps the voice going. False when
     * nothing is parked (the caller closes the reader).
     */
    fun resumeBackground(): Boolean {
        if (_state.value !is OpenState.Idle) return _state.value is OpenState.Ready
        val handle = tts.takeParked() ?: return false
        val orphan = handle.takeOrphan()
        if (orphan == null) {
            handle.close()
            return false
        }
        _state.value = OpenState.Opening
        release()
        resource = orphan.resource
        publication = orphan.publication
        searchSession.attach(orphan.publication)
        navigatorFactory = EpubNavigatorFactory(orphan.publication)
        bookKey = handle.session.bookKey
        lastLocator = handle.session.location.value?.locator
        viewModelScope.launch {
            _positionCount.value = runCatching { orphan.publication.positions().size }.getOrDefault(0)
        }
        bookKey?.let { key ->
            viewModelScope.launch {
                _bookmarks.value = withContext(Dispatchers.IO) { storeMutex.withLock { store.readBookmarks(key) } }
            }
        }
        tts.adopt(handle.session)
        _state.value = OpenState.Ready
        return true
    }

    private fun release() {
        tts.stop()
        searchSession.detach()
        _bookmarks.value = emptyList()
        linkHistory.clear()
        navigatorFactory = null
        publication?.close()
        publication = null
        resource?.close()
        resource = null
        _positionCount.value = 0
    }

    override fun onCleared() {
        flushProgress()
        flushPreferences()
        if (ReaderSettings(getApplication()).readAloudInBackground && tts.status.value == TtsStatus.PLAYING) {
            // Roadmap D26: the voice goes on without the reader and takes the book with it.
            val orphan = publication?.let { OrphanBook(it, resource) }
            publication = null
            resource = null
            tts.park(orphan)
        } else {
            tts.shutdown()
        }
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
        const val PREFERENCES_FLUSH_DELAY_MILLIS = 400L

        /** A picked document with another extension is refused before it is read. */
        private val FONT_EXTENSIONS = setOf("ttf", "otf")
    }
}
