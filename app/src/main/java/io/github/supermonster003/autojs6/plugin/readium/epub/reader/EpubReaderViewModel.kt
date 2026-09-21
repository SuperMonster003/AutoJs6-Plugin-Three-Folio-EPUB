package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationAddResult
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationDatabase
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.BookAnnotation
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookFingerprint
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookOpenError
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookOpener
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.FontsContainer
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.PfdResource
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontCatalog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.CoverExtractor
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBook
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBooksStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontEntry
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontInspection
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ImageDecoding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.LinkHistory
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchSession
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchState
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.service.PreferencePatch
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.service.ReaderPreferencesJson
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.service.ReaderSession
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.service.ReaderSessionRegistry
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.service.SessionAdopter
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.Bookmark
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookmarkCodec
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.CrashFlush
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.autojs.plugin.epub.api.EpubContract
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
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
internal class EpubReaderViewModel(application: Application) : AndroidViewModel(application), SessionAdopter {

    private val store = BookDataStore.forFilesDirectory(application.filesDir)
    private val storeMutex = Mutex()

    /** The highlights and notes (roadmap P9 / D4): one Room table for every book, observed per key. */
    private val annotationStore = AnnotationStore(AnnotationDatabase.get(application))
    private val bookKeyState = MutableStateFlow<String?>(null)

    private val preferencesStore = ReaderPreferencesStore.forFilesDirectory(application.filesDir)
    private val preferencesMutex = Mutex()

    /**
     * Survives [onCleared] so the final flush and a late migration always complete; single-lane so
     * two writes of the same file can never land out of order.
     */
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val throttle = ProgressThrottle<ProgressRecord>()
    private var delayedFlush: Job? = null

    /** Roadmap P7.7: if the process is about to die from an uncaught exception, the position is written first. */
    private val crashFlush = CrashFlush.register(::flushProgressNow)

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

    /** The launcher's recent list (roadmap P4.1): only entries it already holds are updated from here. */
    private val recentStore = RecentBooksStore.forFilesDirectory(application.filesDir)

    /** The recent-list entry of the open book, when the list holds it (the launcher's books, or an added one); null otherwise. */
    @Volatile
    var recentUri: String? = null
        private set

    /** The request the book was opened with (roadmap P4.2: the door decides what the overflow offers). */
    @Volatile
    var request: EpubReaderRequest? = null
        private set

    /** The host reader session this book was adopted from (roadmap P5.3); null for the ordinary doors and once the host let go. */
    @Volatile
    var hostSession: ReaderSession? = null
        private set

    /** The adopted book's own descriptor copy for the fingerprints; closed with the book. */
    private var adoptedDescriptor: ParcelFileDescriptor? = null
    private var bookmarkAnnounceJob: Job? = null

    /** Roadmap P9.4: the host session hears the highlights of the adopted book as `highlight` events. */
    private var annotationAnnounceJob: Job? = null

    /**
     * Every key the adopted book has been stored under (the quick alias, then the full
     * fingerprint): the announcer filters the live table by this set, so the fingerprint
     * migration moving rows between the two keys is not a change.
     */
    private val announcedBookKeys = LinkedHashSet<String>()
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

    /** The highlights and notes of the open book (roadmap P9), live from the database; empty without a book. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val annotations: StateFlow<List<BookAnnotation>> = bookKeyState
        .flatMapLatest { key -> if (key == null) flowOf(emptyList()) else annotationStore.observe(key) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    /** The quick key until the full fingerprint replaces it; the highlights list follows it (roadmap P9). */
    @Volatile
    var bookKey: String? = null
        private set(value) {
            field = value
            bookKeyState.value = value
        }

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
        this.request = request
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
        }
        if (descriptor == null) {
            if (request.entry != ReaderEntry.EXPLORER) markRecentUnavailable(request.documentUri.toString())
            return OpenState.Failed(OpenFailure.CannotRead)
        }

        val stored = loadStored(descriptor)
        val quickKey = stored.quickKey
        val initialKey = stored.key
        val storedProgress = stored.progress
        val storedBookmarks = stored.bookmarks

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
        pruneEvictedAnnotations(initialKey)
        // Roadmap D26: a voice still reading this book in the background comes back to the reader, which opens where it speaks.
        val speaking = tts.adoptSpeaking(initialKey)
        lastLocator = speaking?.location?.value?.locator ?: savedLocator ?: storedProgress?.let { Locator.fromJSON(it.locator) }

        viewModelScope.launch {
            _positionCount.value = runCatching { publication.positions().size }.getOrDefault(0)
        }
        if (quickKey != null) {
            viewModelScope.launch(Dispatchers.IO) { migrateToFullFingerprint(descriptor, quickKey) }
        }
        // The Explorer door never lists (roadmap D4); the others update an entry the list already holds.
        if (request.entry != ReaderEntry.EXPLORER) trackRecent(request.documentUri.toString(), publication)
        return OpenState.Ready
    }

    private class StoredBook(val quickKey: String?, val key: String?, val progress: ProgressRecord?, val bookmarks: List<Bookmark>)

    /**
     * What the store holds for the book behind [descriptor]: a book opened before is already
     * filed under its full fingerprint; the quick key alias finds it without hashing the whole
     * file again before the reader shows.
     */
    private suspend fun loadStored(descriptor: ParcelFileDescriptor): StoredBook {
        val quickKey = withContext(Dispatchers.IO) {
            runCatching { descriptor.withDuplicate { BookFingerprint.quickKey(it.channel) } }.getOrNull()
        }
        val initialKey = quickKey?.let { key ->
            withContext(Dispatchers.IO) { storeMutex.withLock { store.resolveKey(key) } }
        }
        val storedProgress = initialKey?.let { key ->
            withContext(Dispatchers.IO) { storeMutex.withLock { store.readProgress(key) } }
        }
        val storedBookmarks = initialKey?.let { key ->
            withContext(Dispatchers.IO) { storeMutex.withLock { store.readBookmarks(key) } }
        }.orEmpty()
        return StoredBook(quickKey, initialKey, storedProgress, storedBookmarks)
    }

    // ---- Host reader session (roadmap P5.3) ----

    /**
     * Claims the host session behind [token] and adopts its book (the two-step launch of D12);
     * false when no session waits for that token. Like [open], a later call while opening or
     * ready is ignored, so a recreated Activity keeps the adopted book.
     */
    fun adoptSession(token: String): Boolean {
        if (_state.value !is OpenState.Idle) return _state.value is OpenState.Ready
        val session = ReaderSessionRegistry.claim(token) ?: return false
        _state.value = OpenState.Opening
        viewModelScope.launch {
            _state.value = adoptBook(session)
        }
        return true
    }

    private suspend fun adoptBook(session: ReaderSession): OpenState {
        val handover = session.adopt(this)
        release()
        hostSession = session
        val descriptor = handover.resource.duplicateDescriptor()
        adoptedDescriptor = descriptor
        val stored = loadStored(descriptor)
        val publication = handover.publication
        resource = handover.resource
        this.publication = publication
        _bookmarks.value = stored.bookmarks
        searchSession.attach(publication)
        navigatorFactory = EpubNavigatorFactory(publication)
        bookKey = stored.key
        pruneEvictedAnnotations(stored.key)
        // The place the host named wins over the stored position; a target that no longer resolves falls back to it.
        val start = handover.target?.let { target -> runCatching { session.resolveStart(target) }.getOrNull() }
        lastLocator = start ?: stored.progress?.let { Locator.fromJSON(it.locator) }
        viewModelScope.launch {
            _positionCount.value = runCatching { publication.positions().size }.getOrDefault(0)
        }
        stored.quickKey?.let { key ->
            viewModelScope.launch(Dispatchers.IO) { migrateToFullFingerprint(descriptor, key) }
        }
        if (!handover.preferences.isEmpty) applyPreferencePatch(handover.preferences)
        // The stored bookmarks are the baseline; every later change of the list becomes a bookmark event.
        bookmarkAnnounceJob = viewModelScope.launch {
            _bookmarks.collect { session.updateBookmarks(it) }
        }
        // The stored highlights are the baseline too (contract version 2, roadmap P9.4); the live
        // table is filtered by every key this book has had, so the migration below moves nothing.
        synchronized(announcedBookKeys) {
            announcedBookKeys.clear()
            stored.key?.let { announcedBookKeys.add(it) }
        }
        annotationAnnounceJob = viewModelScope.launch {
            annotationStore.observeAll()
                .map { rows -> synchronized(announcedBookKeys) { rows.filter { it.bookKey in announcedBookKeys } } }
                .collect { session.updateAnnotations(it) }
        }
        return OpenState.Ready
    }

    /** A validated preference patch of the host (`setPreferences` or the `openReader` options); the advanced keys drop the publisher's styles as the panel does. */
    override fun applyPreferencePatch(patch: PreferencePatch) {
        if (patch.has(EpubContract.PREFERENCE_THEME)) {
            setThemeMode(patch.string(EpubContract.PREFERENCE_THEME)?.let { ThemeMode.fromKey(it) } ?: ThemeMode.DEFAULT)
        }
        val advanced = ReaderPreferencesJson.ADVANCED_KEYS.any { patch.has(it) && patch.string(it) != null || patch.has(it) && patch.double(it) != null || patch.has(it) && patch.boolean(it) != null }
        editPreferences { epub ->
            var next = epub
            if (patch.has(EpubContract.PREFERENCE_FONT_SIZE)) next = next.copy(fontSize = patch.double(EpubContract.PREFERENCE_FONT_SIZE))
            if (patch.has(EpubContract.PREFERENCE_FONT_FAMILY)) next = next.copy(fontFamily = patch.string(EpubContract.PREFERENCE_FONT_FAMILY)?.let { FontFamily(it) })
            if (patch.has(EpubContract.PREFERENCE_LINE_HEIGHT)) next = next.copy(lineHeight = patch.double(EpubContract.PREFERENCE_LINE_HEIGHT))
            if (patch.has(EpubContract.PREFERENCE_PAGE_MARGINS)) next = next.copy(pageMargins = patch.double(EpubContract.PREFERENCE_PAGE_MARGINS))
            if (patch.has(EpubContract.PREFERENCE_SCROLL)) next = next.copy(scroll = patch.boolean(EpubContract.PREFERENCE_SCROLL))
            if (patch.has(EpubContract.PREFERENCE_COLUMN_COUNT)) {
                next = next.copy(
                    columnCount = when (patch.string(EpubContract.PREFERENCE_COLUMN_COUNT)) {
                        "1" -> ColumnCount.ONE
                        "2" -> ColumnCount.TWO
                        "auto" -> ColumnCount.AUTO
                        else -> null
                    },
                )
            }
            if (patch.has(EpubContract.PREFERENCE_VERTICAL_TEXT)) next = next.copy(verticalText = patch.boolean(EpubContract.PREFERENCE_VERTICAL_TEXT))
            if (patch.has(EpubContract.PREFERENCE_TEXT_ALIGN)) {
                next = next.copy(
                    textAlign = when (patch.string(EpubContract.PREFERENCE_TEXT_ALIGN)) {
                        "start" -> TextAlign.START
                        "end" -> TextAlign.END
                        "left" -> TextAlign.LEFT
                        "right" -> TextAlign.RIGHT
                        "justify" -> TextAlign.JUSTIFY
                        "center" -> TextAlign.CENTER
                        else -> null
                    },
                )
            }
            if (patch.has(EpubContract.PREFERENCE_HYPHENS)) next = next.copy(hyphens = patch.boolean(EpubContract.PREFERENCE_HYPHENS))
            if (patch.has(EpubContract.PREFERENCE_PUBLISHER_STYLES)) {
                next = next.copy(publisherStyles = patch.boolean(EpubContract.PREFERENCE_PUBLISHER_STYLES))
            } else if (advanced) {
                next = next.copy(publisherStyles = false)
            }
            next
        }
    }

    /** The host closed the session (or its process died): the reader lives on as an ordinary reader (D32). */
    override fun onSessionClosed() {
        hostSession = null
        bookmarkAnnounceJob?.cancel()
        bookmarkAnnounceJob = null
        annotationAnnounceJob?.cancel()
        annotationAnnounceJob = null
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
        synchronized(announcedBookKeys) { if (announcedBookKeys.isNotEmpty()) announcedBookKeys.add(fullKey) }
        storeMutex.withLock {
            val current = bookKey ?: return
            // Store writes never take the reader down (see persist): a failed alias write only
            // means the next open fingerprints the file again.
            runCatching {
                if (current == fullKey || store.migrate(current, fullKey)) {
                    store.writeAlias(quickKey, fullKey)
                    bookKey = fullKey
                    recentUri?.let { uri -> runCatching { recentStore.update(uri) { it.copy(key = fullKey) } } }
                    // The highlights move with the key (roadmap P9); rows the full key already has win on the same place.
                    if (current != fullKey) runCatching { annotationStore.migrate(current, fullKey) }
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

    /**
     * The crash path (roadmap P7.7) cannot wait for [persistScope]: the current locator is written on the
     * dying thread, straight through the atomic store write. The recent list catches up on the next open.
     */
    private fun flushProgressNow() {
        val key = bookKey ?: return
        val locator = lastLocator ?: return
        delayedFlush?.cancel()
        val now = System.currentTimeMillis()
        throttle.flush(now)
        store.writeProgress(key, ProgressRecord(locator.toJSON(), now, locator.locations.totalProgression))
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
            recentUri?.let { uri ->
                runCatching { recentStore.update(uri) { it.copy(progression = record.totalProgression ?: it.progression, lastReadAt = record.updatedAtMillis) } }
            }
        }
    }

    // ---- The launcher's recent list (roadmap P4.1) ----

    /**
     * Fills the recent entry of a book the launcher opened: fingerprint, title, authors and, once,
     * the cover thumbnail. Entries the list does not hold (a grant that could not be persisted)
     * are never created here.
     */
    private fun trackRecent(uri: String, publication: Publication) {
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = publication.metadata
            val updated = runCatching {
                recentStore.update(uri) {
                    it.copy(
                        key = bookKey ?: it.key,
                        title = metadata.title?.takeIf(String::isNotBlank) ?: it.title,
                        author = metadata.authors.joinToString(", ") { author -> author.name }.takeIf(String::isNotBlank) ?: it.author,
                        available = true,
                    )
                }
            }.getOrNull() ?: return@launch
            recentUri = uri
            if (updated.coverFile != null) return@launch
            val cover = CoverExtractor.extract(publication) ?: return@launch
            runCatching {
                val name = recentStore.writeCover(uri, cover)
                recentStore.update(uri) { it.copy(coverFile = name) }
            }
        }
    }

    /**
     * Roadmap P4.2: lists a book that came through `ACTION_VIEW` after the activity took the
     * persistable grant; entries evicted by the limit hand their URI to [releaseEvicted].
     */
    fun addToRecent(request: EpubReaderRequest, releaseEvicted: (Uri) -> Unit) {
        val publication = publication ?: return
        val uri = request.documentUri.toString()
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val update = runCatching {
                recentStore.upsert(RecentBook(uri, request.displayName, addedAt = now, lastReadAt = now, key = bookKey))
            }.getOrNull() ?: return@launch
            update.evicted.forEach { evicted -> releaseEvicted(evicted.uri.toUri()) }
            trackRecent(uri, publication)
        }
    }

    private fun markRecentUnavailable(uri: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { recentStore.update(uri) { it.copy(available = false) } } }
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

    /**
     * Roadmap P4.3: the settings page edits the same file (theme mode, restore). Re-read it when
     * the reader comes back, unless an edit of this reader is still waiting to be flushed.
     */
    fun reloadPreferences() {
        if (preferencesDirty) return
        val stored = preferencesStore.read()?.let(ReaderPreferencesState::fromStored) ?: ReaderPreferencesState.DEFAULT
        if (stored != _preferences.value) _preferences.value = stored
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

    // ---- Highlights and notes (roadmap P9) ----

    /**
     * Highlights [locator] (the navigator's selection locator as JSON) in [style] and [color], with
     * an optional [note]; a place the book already annotated is restyled instead. The store applies
     * the caps. Taken under the store mutex so the key cannot migrate underneath the insert.
     */
    suspend fun addAnnotation(locator: JSONObject, style: String, color: Int, note: String?, chapter: String?): AnnotationAddResult =
        storeMutex.withLock {
            val key = bookKey ?: return@withLock AnnotationAddResult.Invalid
            annotationStore.add(
                BookAnnotation(bookKey = key, href = "", locator = locator.toString(), style = style, color = color, note = note, chapter = chapter, createdAt = System.currentTimeMillis()),
            )
        }

    /** Replaces the style, colour, note and quote of the stored row with [annotation]'s; null when the row is gone. */
    suspend fun updateAnnotation(annotation: BookAnnotation): BookAnnotation? = annotationStore.update(annotation)

    /** Removes the annotation with [id]; false when there is none. */
    suspend fun deleteAnnotation(id: Long): Boolean = annotationStore.delete(id)

    /** Removes every highlight and note of the open book; returns how many. */
    suspend fun clearAnnotations(): Int = storeMutex.withLock { bookKey?.let { annotationStore.clearBook(it) } ?: 0 }

    /**
     * Books the store evicted (LRU, D13) lose their highlights too. Runs once per open, in the
     * background; the book opening now is kept whatever the state of its directory.
     */
    private fun pruneEvictedAnnotations(current: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val kept = storeMutex.withLock { store.bookKeys() } + listOfNotNull(current)
            runCatching { annotationStore.retainOnly(kept) }
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
        recentUri = null
        bookmarkAnnounceJob?.cancel()
        bookmarkAnnounceJob = null
        annotationAnnounceJob?.cancel()
        annotationAnnounceJob = null
        synchronized(announcedBookKeys) { announcedBookKeys.clear() }
        adoptedDescriptor?.let { runCatching { it.close() } }
        adoptedDescriptor = null
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
        crashFlush.cancel()
        flushPreferences()
        // The user left the reader: the host hears `close(user)` before the book goes.
        hostSession?.close(EpubContract.REASON_USER, finish = false)
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
        is BookOpenError.Retrieve, is BookOpenError.Open, is BookOpenError.Malformed -> OpenFailure.Other(message)
    }

    private inline fun <T> ParcelFileDescriptor.withDuplicate(
        block: (ParcelFileDescriptor.AutoCloseInputStream) -> T,
    ): T = ParcelFileDescriptor.AutoCloseInputStream(dup()).use(block)

    companion object {
        const val OPEN_TIMEOUT_MILLIS = 60_000L
        const val PREFERENCES_FLUSH_DELAY_MILLIS = 400L

        /** A picked document with another extension is refused before it is read. */
        /**
         * Extensions the picker may hand over. Collections (`ttc` / `otc`) pass this gate so that the
         * header check can name the real reason for refusing them instead of "not a font".
         */
        private val FONT_EXTENSIONS = setOf("ttf", "otf", "ttc", "otc")
    }
}
