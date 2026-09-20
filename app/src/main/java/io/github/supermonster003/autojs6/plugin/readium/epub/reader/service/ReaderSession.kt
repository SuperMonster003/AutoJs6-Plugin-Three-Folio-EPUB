package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.PfdResource
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.TocFlattener
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.Bookmark
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ProgressThrottle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.autojs.plugin.epub.api.IEpubReaderCallback
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.util.Url
import java.util.concurrent.atomic.AtomicLong

/** What the reader's view model takes over from a claimed session (roadmap P5.3). */
internal class SessionHandover(
    val resource: PfdResource,
    val publication: Publication,
    val target: SessionTarget?,
    val preferences: PreferencePatch,
)

/** The view model that adopted a session: preferences arrive here, and it learns when the host let go. */
internal interface SessionAdopter {
    fun applyPreferencePatch(patch: PreferencePatch)
    fun onSessionClosed()
}

/** The reader Activity showing an adopted session; every call arrives on the main thread. */
internal interface ReaderSessionController {
    fun goTo(locator: Locator)
    fun navigate(direction: Int)
    fun finishReader()
}

/**
 * One host reader session (roadmap P5.3, decisions D12 and D32): minted by `openReader` with a
 * random token, owning the opened book until the reader Activity claims the token and its view
 * model adopts the book, then the bridge between that reader and the host's callback.
 *
 * Events go out as `onEvent(generation, seq, bundle)` with one generation per session and a
 * strictly increasing `seq`; `open` once the navigator shows the book, `progress` throttled to
 * [EpubContract.PROGRESS_THROTTLE_MS], `bookmark` per added or removed bookmark, `error` for a
 * refused preference key or an unreachable `goTo` target, and exactly one `close` with its
 * reason. A dead host callback ends the session silently and the reader stays open for the user.
 */
internal class ReaderSession(
    val token: String,
    val generation: Long,
    val displayName: String?,
    resource: PfdResource,
    val publication: Publication,
    initialTarget: SessionTarget?,
    initialPreferences: PreferencePatch,
    private val callback: IEpubReaderCallback,
) {

    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val seq = AtomicLong(0L)

    private var ownedResource: PfdResource? = resource
    private var ownedPublication: Publication? = publication

    @Volatile
    var claimed: Boolean = false
        private set

    @Volatile
    var closed: Boolean = false
        private set

    @Volatile
    var visible: Boolean = false
        private set

    /** The place the host named before the reader adopted the book; consumed by [adopt]. */
    @Volatile
    private var target: SessionTarget? = initialTarget

    @Volatile
    private var pendingPreferences: PreferencePatch = initialPreferences

    @Volatile
    var lastLocator: Locator? = null
        private set

    @Volatile
    var chapterTitle: String? = null
        private set

    @Volatile
    var positions: Int = 0
        private set

    @Volatile
    private var bookmarksJson: JSONArray = JSONArray()
    private var knownBookmarks: Map<Long, Bookmark>? = null

    private var opened = false
    private val progressThrottle = ProgressThrottle<Locator>(EpubContract.PROGRESS_THROTTLE_MS)
    private var progressFlush: Runnable? = null

    @Volatile
    private var adopter: SessionAdopter? = null

    @Volatile
    private var controller: ReaderSessionController? = null

    @Volatile
    private var finishRequested = false

    /** Set by the registry so it can forget a session that closed on its own. */
    @Volatile
    var onClosed: ((ReaderSession) -> Unit)? = null

    val lastSeq: Long get() = seq.get()

    private val deathRecipient = IBinder.DeathRecipient { close(EpubContract.REASON_ERROR, finish = false, notify = false) }

    init {
        try {
            callback.asBinder().linkToDeath(deathRecipient, 0)
        } catch (e: RemoteException) {
            // The host went away between the call and now; the registry closes the session right after.
            handler.post { close(EpubContract.REASON_ERROR, finish = false, notify = false) }
        }
    }

    // ---- Registry ----

    fun markClaimed() {
        claimed = true
    }

    // ---- Reader side (main thread) ----

    /**
     * The view model takes the book over: from here on it closes the publication and the
     * descriptor, the session only looks the publication up.
     */
    fun adopt(adopter: SessionAdopter): SessionHandover {
        val resource: PfdResource
        val publication: Publication
        val target: SessionTarget?
        val preferences: PreferencePatch
        synchronized(lock) {
            resource = checkNotNull(ownedResource) { "the session was adopted already" }
            publication = checkNotNull(ownedPublication)
            ownedResource = null
            ownedPublication = null
            target = this.target
            this.target = null
            preferences = pendingPreferences
            pendingPreferences = PreferencePatch.EMPTY
            this.adopter = adopter
        }
        return SessionHandover(resource, publication, target, preferences)
    }

    fun attachController(controller: ReaderSessionController) {
        this.controller = controller
        if (finishRequested) controller.finishReader()
    }

    fun detachController(controller: ReaderSessionController) {
        if (this.controller === controller) this.controller = null
    }

    fun setVisible(visible: Boolean) {
        this.visible = visible
    }

    /** Every navigator locator: the first one opens the session, the later ones report progress through the throttle. */
    fun onLocator(locator: Locator, positionCount: Int, chapterTitle: String?) {
        lastLocator = locator
        positions = positionCount
        this.chapterTitle = chapterTitle
        if (!opened) {
            opened = true
            emit(EpubContract.EVENT_OPEN) {
                putBoolean(EpubContract.KEY_VISIBLE, visible)
                publication.metadata.title?.let { putString(EpubContract.KEY_TITLE, it) }
                putString(EpubContract.KEY_LOCATOR, locator.toJSON().toString())
                putString(EpubContract.KEY_HREF, locator.href.toString())
                putInt(EpubContract.KEY_POSITIONS, positionCount)
            }
            return
        }
        val now = SystemClock.uptimeMillis()
        val immediate = progressThrottle.offer(locator, now)
        if (immediate != null) {
            progressFlush?.let(handler::removeCallbacks)
            progressFlush = null
            emitProgress(immediate)
        } else if (progressFlush == null) {
            val flush = Runnable {
                progressFlush = null
                progressThrottle.flush(SystemClock.uptimeMillis())?.let(::emitProgress)
            }
            progressFlush = flush
            handler.postDelayed(flush, EpubContract.PROGRESS_THROTTLE_MS)
        }
    }

    private fun emitProgress(locator: Locator) {
        emit(EpubContract.EVENT_PROGRESS) {
            putString(EpubContract.KEY_LOCATOR, locator.toJSON().toString())
            locator.locations.totalProgression?.let { putDouble(EpubContract.KEY_PROGRESSION, it) }
            chapterTitle?.let { putString(EpubContract.KEY_TITLE, it) }
            putString(EpubContract.KEY_HREF, locator.href.toString())
        }
    }

    /** The bookmarks of the book as the view model holds them; the first list is the baseline, later ones are diffed into events. */
    fun updateBookmarks(bookmarks: List<Bookmark>) {
        val next = bookmarks.associateBy { it.id }
        val previous = knownBookmarks
        knownBookmarks = next
        bookmarksJson = JSONArray().also { array -> bookmarks.forEach { array.put(bookmarkJson(it)) } }
        if (previous == null) return
        previous.values.filter { it.id !in next }.forEach { emitBookmark(EpubContract.CHANGE_REMOVED, it) }
        bookmarks.filter { it.id !in previous }.forEach { emitBookmark(EpubContract.CHANGE_ADDED, it) }
    }

    private fun emitBookmark(change: String, bookmark: Bookmark) {
        emit(EpubContract.EVENT_BOOKMARK) {
            putString(EpubContract.KEY_CHANGE, change)
            putString(EpubContract.KEY_BOOKMARKS, JSONArray().put(bookmarkJson(bookmark)).toString())
        }
    }

    // ---- Host side (binder threads) ----

    /** Checks that [target] names a place of this book; the shape was checked by [SessionTarget.parse]. */
    fun validateTarget(target: SessionTarget) {
        target.locator?.let { json ->
            val locator = Locator.fromJSON(json) ?: throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "locator is not a Readium locator")
            if (publication.linkWithHref(locator.href) == null) {
                throw ContractViolation(EpubErrorCodes.RESOURCE_NOT_FOUND, locator.href.toString())
            }
        }
        target.href?.let { href -> resolveHref(href) }
    }

    /** Before the reader adopted the book the target becomes its start position; afterwards the reader jumps there. */
    fun requestGoTo(target: SessionTarget) {
        synchronized(lock) {
            if (ownedPublication != null) {
                this.target = target
                return
            }
        }
        scope.launch {
            val locator = try {
                resolve(target)
            } catch (e: ContractViolation) {
                emitError(e.code, e.detail)
                return@launch
            }
            if (locator == null) {
                emitError(EpubErrorCodes.RESOURCE_NOT_FOUND, "the target has no position in this book")
                return@launch
            }
            controller?.goTo(locator)
        }
    }

    fun requestNavigate(direction: Int) {
        handler.post { controller?.navigate(direction) }
    }

    /** Before adoption the patch waits with the others; afterwards the view model applies it. */
    fun requestPreferences(parse: PreferenceParse) {
        if (parse.unsupported.isNotEmpty()) {
            emitError(EpubErrorCodes.UNSUPPORTED_PREFERENCE, "unsupported preferences: " + parse.unsupported.joinToString(", "))
        }
        if (parse.patch.isEmpty) return
        val adopter = synchronized(lock) {
            this.adopter ?: run {
                pendingPreferences = pendingPreferences.merge(parse.patch)
                null
            }
        } ?: return
        handler.post { adopter.applyPreferencePatch(parse.patch) }
    }

    fun stateBundle(): Bundle = Answers.ok {
        putString(EpubContract.KEY_SESSION_TOKEN, token)
        putBoolean(EpubContract.KEY_VISIBLE, visible)
        putInt(EpubContract.KEY_POSITIONS, positions)
        lastLocator?.let { locator ->
            putString(EpubContract.KEY_LOCATOR, locator.toJSON().toString())
            locator.locations.totalProgression?.let { putDouble(EpubContract.KEY_PROGRESSION, it) }
            val href = locator.href.toString()
            putString(EpubContract.KEY_HREF, href)
            val index = publication.readingOrder.indexOfFirst { it.href.toString() == TocFlattener.resourceHref(href) }
            if (index >= 0) putInt(EpubContract.KEY_INDEX, index)
            chapterTitle?.let { putString(EpubContract.KEY_TITLE, it) }
        }
    }

    fun bookmarksBundle(): Bundle = Answers.ok { putString(EpubContract.KEY_BOOKMARKS, bookmarksJson.toString()) }

    /**
     * Ends the session exactly once: the `close` event (unless the host is gone), the callback
     * link, the pending progress flush, the book when nobody adopted it, and with [finish] the
     * reader Activity; without it the reader lives on as an ordinary reader (D32).
     */
    fun close(reason: String, finish: Boolean, notify: Boolean = true) {
        val resource: PfdResource?
        val publication: Publication?
        val adopter: SessionAdopter?
        synchronized(lock) {
            if (closed) return
            if (notify) send(EpubContract.EVENT_CLOSE) { putString(EpubContract.KEY_REASON, reason) }
            closed = true
            resource = ownedResource
            publication = ownedPublication
            ownedResource = null
            ownedPublication = null
            adopter = this.adopter
            this.adopter = null
        }
        runCatching { callback.asBinder().unlinkToDeath(deathRecipient, 0) }
        scope.cancel()
        handler.post {
            progressFlush?.let(handler::removeCallbacks)
            progressFlush = null
            adopter?.onSessionClosed()
            if (finish) {
                val controller = this.controller
                if (controller != null) controller.finishReader() else finishRequested = true
            }
        }
        publication?.close()
        resource?.close()
        onClosed?.invoke(this)
    }

    /** The start position of an adopted book for the view model: the target resolved against the publication. */
    suspend fun resolveStart(target: SessionTarget): Locator? = resolve(target)

    // ---- Internals ----

    private suspend fun resolve(target: SessionTarget): Locator? {
        target.locator?.let { json ->
            return Locator.fromJSON(json) ?: throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "locator is not a Readium locator")
        }
        target.href?.let { return resolveHref(it) }
        target.progression?.let { return publication.locateProgression(it) }
        return null
    }

    /** A resource href with an optional fragment resolved against the reading order, or `RESOURCE_NOT_FOUND`. */
    private fun resolveHref(href: String): Locator {
        val resourceHref = TocFlattener.resourceHref(href)
        val fragment = href.substringAfter('#', "").takeIf { it.isNotEmpty() }
        val url = Url(resourceHref) ?: throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "href is not a valid URL")
        val link = publication.linkWithHref(url) ?: throw ContractViolation(EpubErrorCodes.RESOURCE_NOT_FOUND, href)
        val locator = publication.locatorFromLink(link) ?: throw ContractViolation(EpubErrorCodes.RESOURCE_NOT_FOUND, href)
        return if (fragment == null) locator else locator.copy(locations = locator.locations.copy(fragments = listOf(fragment)))
    }

    private fun emitError(code: String, detail: String) {
        emit(EpubContract.EVENT_ERROR) {
            putString(EpubContract.KEY_ERROR_CODE, code)
            putString(EpubContract.KEY_ERROR_MESSAGE, Limits.errorDetail(detail))
        }
    }

    private fun emit(type: String, build: Bundle.() -> Unit) {
        val dead: Boolean
        synchronized(lock) {
            if (closed) return
            dead = !send(type, build)
        }
        if (dead) close(EpubContract.REASON_ERROR, finish = false, notify = false)
    }

    /** Under [lock]: numbers and sends one event; false when the host callback is gone. */
    private fun send(type: String, build: Bundle.() -> Unit): Boolean {
        val bundle = Answers.ok {
            putString(EpubContract.KEY_EVENT, type)
            build()
        }
        if (DescriptorIo.parcelSize(bundle) > EpubContract.MAX_EVENT_BYTES) {
            bundle.remove(EpubContract.KEY_LOCATOR)
            bundle.remove(EpubContract.KEY_BOOKMARKS)
            if (DescriptorIo.parcelSize(bundle) > EpubContract.MAX_EVENT_BYTES) return true
        }
        return try {
            callback.onEvent(generation, seq.incrementAndGet(), bundle)
            true
        } catch (e: RemoteException) {
            false
        }
    }

    private fun bookmarkJson(bookmark: Bookmark): JSONObject = JSONObject().apply {
        put(EpubContract.FIELD_LOCATOR, bookmark.locator)
        put(EpubContract.FIELD_CREATED_AT, bookmark.createdAtMillis)
        bookmark.chapter?.let { put(EpubContract.FIELD_TITLE, it) }
        bookmark.snippet?.let { put(EpubContract.FIELD_TEXT, it) }
    }
}

/**
 * The process-wide table of host reader sessions (roadmap P5.3): one session at a time, a new
 * `openReader` replaces the previous one (its reader is finished, its host hears `replaced`), and
 * a session nobody claims within [claimTimeoutMs] closes with `timeout` and releases its book.
 */
internal object ReaderSessionRegistry {

    private val handler = Handler(Looper.getMainLooper())
    private val generations = AtomicLong(System.currentTimeMillis())
    private val timeouts = HashMap<ReaderSession, Runnable>()
    private var current: ReaderSession? = null

    /** Instrumentation tests shorten the claim window; production keeps the contract's value. */
    @Volatile
    internal var claimTimeoutMs: Long = EpubContract.READER_CLAIM_TIMEOUT_MS

    fun open(
        displayName: String?,
        resource: PfdResource,
        publication: Publication,
        target: SessionTarget?,
        preferences: PreferencePatch,
        callback: IEpubReaderCallback,
    ): ReaderSession {
        val session = ReaderSession(HostSessionPolicy.newToken(), generations.incrementAndGet(), displayName, resource, publication, target, preferences, callback)
        session.onClosed = ::forget
        val previous: ReaderSession?
        synchronized(this) {
            previous = current
            current = session
            val timeout = Runnable { session.close(EpubContract.REASON_TIMEOUT, finish = false) }
            timeouts[session] = timeout
            handler.postDelayed(timeout, claimTimeoutMs)
        }
        previous?.close(EpubContract.REASON_REPLACED, finish = true)
        return session
    }

    /** The session behind [token] when it is still waiting for its reader; null otherwise. */
    fun claim(token: String): ReaderSession? = synchronized(this) {
        val session = current ?: return null
        if (session.closed || session.claimed || !HostSessionPolicy.sameToken(token, session.token)) return null
        timeouts.remove(session)?.let(handler::removeCallbacks)
        session.markClaimed()
        session
    }

    val currentSession: ReaderSession? get() = synchronized(this) { current }

    fun closeAll(reason: String) {
        currentSession?.close(reason, finish = false)
    }

    private fun forget(session: ReaderSession) {
        synchronized(this) {
            timeouts.remove(session)?.let(handler::removeCallbacks)
            if (current === session) current = null
        }
    }
}
