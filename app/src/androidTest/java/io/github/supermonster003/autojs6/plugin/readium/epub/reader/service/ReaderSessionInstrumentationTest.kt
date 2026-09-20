package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.ReadiumEpubReaderPlugin
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import org.autojs.plugin.epub.api.EpubActions
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.autojs.plugin.epub.api.IEpubPlugin
import org.autojs.plugin.epub.api.IEpubReaderCallback
import org.autojs.plugin.epub.api.IEpubReaderSession
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Roadmap P5.3 device evidence: `openReader` through the generated AIDL Proxy / Parcel path, the
 * two-step launch of the reader Activity with the session token (decision D12), the event
 * stream (`open`, `progress`, `bookmark`, `error`, `close` with one generation and a strictly
 * increasing `seq`), `goTo` / `navigate` / `setPreferences` / `getBookmarks` / `getState`,
 * replacement, the claim timeout, a wrong token, and the contract codes of every refusal.
 * Notes land in `files/p2-evidence/reader-session-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class ReaderSessionInstrumentationTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val notes = ArrayList<String>()
    private val activities = ArrayList<EpubReaderActivity>()

    private class Event(val generation: Long, val seq: Long, val bundle: Bundle) {
        val type: String? get() = bundle.getString(EpubContract.KEY_EVENT)
        val reason: String? get() = bundle.getString(EpubContract.KEY_REASON)
        val href: String? get() = bundle.getString(EpubContract.KEY_HREF)
        override fun toString(): String = "#$seq $type ${bundle.keySet().joinToString { "$it=${bundle.get(it)}" }}"
    }

    private class RecordingCallback : IEpubReaderCallback.Stub() {
        private val events = ArrayList<Event>()

        override fun onEvent(generation: Long, seq: Long, event: Bundle?) {
            synchronized(events) { events += Event(generation, seq, Bundle(event ?: Bundle())) }
        }

        fun snapshot(): List<Event> = synchronized(events) { events.toList() }

        /** The first event after index [after] matching [predicate], waiting up to [timeoutMillis]. */
        fun await(what: String, after: Int = -1, timeoutMillis: Long = 30_000, predicate: (Event) -> Boolean): Event {
            val deadline = SystemClock.uptimeMillis() + timeoutMillis
            while (SystemClock.uptimeMillis() < deadline) {
                snapshot().withIndex().firstOrNull { (index, event) -> index > after && predicate(event) }?.let { return it.value }
                SystemClock.sleep(100)
            }
            fail("Timed out waiting for $what; events: ${snapshot()}")
            throw AssertionError()
        }

        fun indexOf(event: Event): Int = snapshot().indexOfFirst { it === event }
    }

    @After
    fun tearDown() {
        ReaderSessionRegistry.claimTimeoutMs = EpubContract.READER_CLAIM_TIMEOUT_MS
        activities.forEach { activity -> instrumentation.runOnMainSync { if (!activity.isFinishing) activity.finish() } }
        activities.clear()
        instrumentation.waitForIdleSync()
    }

    // ---- Helpers ----

    private fun fixture(name: String): File {
        val file = File(context.cacheDir, "reader-session-$name")
        assets.open(name).use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }

    private fun descriptor(name: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(fixture(name), ParcelFileDescriptor.MODE_READ_ONLY)

    private fun bind(): IEpubPlugin {
        val binder = serviceRule.bindService(Intent(context, ReadiumEpubReaderPluginService::class.java))
        return IEpubPlugin.Stub.asInterface(RemoteOnlySessionBinder(binder))
    }

    private fun IEpubReaderSession.remote(): IEpubReaderSession = IEpubReaderSession.Stub.asInterface(RemoteOnlySessionBinder(asBinder()))

    private fun options(build: Bundle.() -> Unit = {}): Bundle = Bundle().apply {
        putInt(EpubContract.KEY_CONTRACT_VERSION, EpubContract.CONTRACT_VERSION)
        build()
    }

    private fun openReader(plugin: IEpubPlugin, callback: IEpubReaderCallback?, options: Bundle = options()): IEpubReaderSession =
        descriptor(FIXTURE).use { plugin.openReader(it, options, callback).remote() }

    private fun Bundle.errorCode(): String? = getString(EpubContract.KEY_ERROR_CODE)

    private fun Bundle.requireOk(what: String): Bundle {
        assertEquals(EpubContract.CONTRACT_VERSION, getInt(EpubContract.KEY_CONTRACT_VERSION))
        assertNull("$what: ${getString(EpubContract.KEY_ERROR_MESSAGE)}", errorCode())
        return this
    }

    private fun Bundle.requireError(what: String, code: String): Bundle {
        assertEquals(EpubContract.CONTRACT_VERSION, getInt(EpubContract.KEY_CONTRACT_VERSION))
        assertEquals(what, code, errorCode())
        return this
    }

    private inline fun <reified T : RuntimeException> failure(code: String, block: () -> Unit): String {
        try {
            block()
        } catch (e: RuntimeException) {
            assertTrue("expected ${T::class.java.simpleName}, got $e", e is T)
            val decoded = EpubErrorCodes.decode(e.message.orEmpty())
            assertNotNull("undecodable message: ${e.message}", decoded)
            assertEquals(e.message, code, decoded?.first)
            return e.message.orEmpty()
        }
        fail("expected a $code failure")
        throw AssertionError()
    }

    private fun launchReader(token: String): EpubReaderActivity {
        val intent = Intent(EpubActions.READER_ACTIVITY_ACTION)
            .setClassName(context.packageName, ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME)
            .putExtra(EpubActions.EXTRA_SESSION_TOKEN, token)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent) as EpubReaderActivity
        activities += activity
        return activity
    }

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private fun await(message: String, timeoutMillis: Long = 30_000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            if (onMain { runCatching(condition).getOrDefault(false) }) return
            SystemClock.sleep(100)
        }
        fail("Timed out: $message")
    }

    private fun openDescriptors(): Int = File("/proc/self/fd").list()?.size ?: -1

    private fun awaitDescriptors(baseline: Int): Int {
        var count = openDescriptors()
        val deadline = SystemClock.elapsedRealtime() + 5_000
        while (count > baseline && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(50)
            count = openDescriptors()
        }
        return count
    }

    private fun note(line: String) {
        notes += line
    }

    private fun writeNotes(name: String) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }
            .resolve("$name-api${Build.VERSION.SDK_INT}.txt")
            .writeText(notes.joinToString("\n") + "\n")
    }

    // ---- Tests ----

    @Test
    fun sessionRoundTripThroughTheReader() {
        note("device api=${Build.VERSION.SDK_INT}")
        val plugin = bind()
        val callback = RecordingCallback()
        val session = openReader(
            plugin, callback,
            options {
                putString(EpubContract.KEY_DISPLAY_NAME, FIXTURE)
                putString(EpubContract.KEY_HREF, CHAPTER2)
                putString(EpubContract.KEY_PREFERENCES, """{"fontSize":1.3,"bogus":1}""")
            },
        )
        val before = session.state.requireOk("state before the claim")
        val token = requireNotNull(before.getString(EpubContract.KEY_SESSION_TOKEN))
        assertTrue(token, HostSessionPolicy.isWellFormed(token))
        assertFalse(before.getBoolean(EpubContract.KEY_VISIBLE))
        assertNull(before.getString(EpubContract.KEY_LOCATOR))
        note("state before claim: visible=false token=${token.length} hex chars")

        val unsupported = callback.await("unsupported preference error") { it.type == EpubContract.EVENT_ERROR }
        assertEquals(EpubErrorCodes.UNSUPPORTED_PREFERENCE, unsupported.bundle.getString(EpubContract.KEY_ERROR_CODE))
        assertTrue(unsupported.bundle.getString(EpubContract.KEY_ERROR_MESSAGE).orEmpty().contains("bogus"))
        note("unsupported preference: ${unsupported.bundle.getString(EpubContract.KEY_ERROR_MESSAGE)}")
        note("navigate before claim: " + failure<IllegalStateException>(EpubErrorCodes.READER_NOT_VISIBLE) { session.navigate(EpubContract.DIRECTION_NEXT_PAGE) })

        val activity = launchReader(token)
        val open = callback.await("open event") { it.type == EpubContract.EVENT_OPEN }
        note("open: $open")
        assertTrue(open.bundle.getBoolean(EpubContract.KEY_VISIBLE))
        assertEquals("Minimal EPUB 3", open.bundle.getString(EpubContract.KEY_TITLE))
        assertEquals(CHAPTER2, open.href)
        assertTrue(open.bundle.getInt(EpubContract.KEY_POSITIONS) >= 3)
        assertNotNull(open.bundle.getString(EpubContract.KEY_LOCATOR))
        val claimed = session.state.requireOk("state after the claim")
        assertEquals(token, claimed.getString(EpubContract.KEY_SESSION_TOKEN))
        assertTrue(claimed.getBoolean(EpubContract.KEY_VISIBLE))
        assertEquals(CHAPTER2, claimed.getString(EpubContract.KEY_HREF))
        assertEquals(1, claimed.getInt(EpubContract.KEY_INDEX))
        assertTrue(claimed.getInt(EpubContract.KEY_POSITIONS) >= 3)
        note("state after claim: href=${claimed.getString(EpubContract.KEY_HREF)} index=${claimed.getInt(EpubContract.KEY_INDEX)} title=${claimed.getString(EpubContract.KEY_TITLE)}")
        onMain { assertEquals(1.3, requireNotNull(activity.preferencesState.value.epub.fontSize), 1e-9) }
        await("navigator ready") { activity.navigatorReady }

        session.navigate(EpubContract.DIRECTION_NEXT_CHAPTER)
        val chapter3 = callback.await("progress to chapter 3", callback.indexOf(open)) { it.type == EpubContract.EVENT_PROGRESS && it.href == CHAPTER3 }
        note("next chapter: $chapter3")
        assertTrue(chapter3.bundle.containsKey(EpubContract.KEY_PROGRESSION))
        assertNotNull(chapter3.bundle.getString(EpubContract.KEY_LOCATOR))

        session.goTo(options { putString(EpubContract.KEY_HREF, CHAPTER1) })
        val chapter1 = callback.await("progress to chapter 1", callback.indexOf(chapter3)) { it.type == EpubContract.EVENT_PROGRESS && it.href == CHAPTER1 }
        note("goTo href: $chapter1")

        session.goTo(options { putDouble(EpubContract.KEY_PROGRESSION, 1.0) })
        val end = callback.await("progress to the end", callback.indexOf(chapter1)) { it.type == EpubContract.EVENT_PROGRESS && it.href == CHAPTER3 }
        note("goTo progression 1.0: $end")

        session.goTo(options { putString(EpubContract.KEY_LOCATOR, chapter1.bundle.getString(EpubContract.KEY_LOCATOR)) })
        val back = callback.await("progress back to chapter 1", callback.indexOf(end)) { it.type == EpubContract.EVENT_PROGRESS && it.href == CHAPTER1 }
        note("goTo locator: $back")
        session.navigate(EpubContract.DIRECTION_PREVIOUS_CHAPTER)
        SystemClock.sleep(EpubContract.PROGRESS_THROTTLE_MS * 2)
        assertEquals(CHAPTER1, session.state.requireOk("state at the first chapter").getString(EpubContract.KEY_HREF))

        session.setPreferences(options { putString(EpubContract.KEY_PREFERENCES, """{"fontSize":2.0,"theme":"dark","lineHeight":1.5,"nope":true}""") })
        val refused = callback.await("unsupported key after claim", callback.indexOf(back)) { it.type == EpubContract.EVENT_ERROR }
        assertEquals(EpubErrorCodes.UNSUPPORTED_PREFERENCE, refused.bundle.getString(EpubContract.KEY_ERROR_CODE))
        await("preferences applied") {
            val state = activity.preferencesState.value
            state.epub.fontSize == 2.0 && state.themeMode == ThemeMode.DARK && state.epub.lineHeight == 1.5 && state.epub.publisherStyles == false
        }
        note("setPreferences: fontSize=2.0 theme=dark lineHeight=1.5 publisherStyles=false, refused=${refused.bundle.getString(EpubContract.KEY_ERROR_MESSAGE)}")

        assertEquals(0, JSONArray(session.bookmarks.requireOk("bookmarks").getString(EpubContract.KEY_BOOKMARKS)).length())
        onMain { activity.addBookmark() }
        val added = callback.await("bookmark added", callback.indexOf(refused)) { it.type == EpubContract.EVENT_BOOKMARK }
        assertEquals(EpubContract.CHANGE_ADDED, added.bundle.getString(EpubContract.KEY_CHANGE))
        val addedRows = JSONArray(added.bundle.getString(EpubContract.KEY_BOOKMARKS))
        assertEquals(1, addedRows.length())
        assertEquals(CHAPTER1, addedRows.getJSONObject(0).getJSONObject(EpubContract.FIELD_LOCATOR).getString(EpubContract.FIELD_HREF))
        assertTrue(addedRows.getJSONObject(0).getLong(EpubContract.FIELD_CREATED_AT) > 0L)
        note("bookmark added: $addedRows")
        val listed = JSONArray(session.bookmarks.requireOk("bookmarks").getString(EpubContract.KEY_BOOKMARKS))
        assertEquals(1, listed.length())
        val id = onMain { activity.bookmarks.value.single().id }
        onMain { activity.removeBookmark(id) }
        val removed = callback.await("bookmark removed", callback.indexOf(added)) { it.type == EpubContract.EVENT_BOOKMARK }
        assertEquals(EpubContract.CHANGE_REMOVED, removed.bundle.getString(EpubContract.KEY_CHANGE))
        assertEquals(0, JSONArray(session.bookmarks.requireOk("bookmarks").getString(EpubContract.KEY_BOOKMARKS)).length())
        note("bookmark removed: ${removed.bundle.getString(EpubContract.KEY_BOOKMARKS)}")

        session.close(options { putBoolean(EpubContract.KEY_FINISH, true) })
        val close = callback.await("close event", callback.indexOf(removed)) { it.type == EpubContract.EVENT_CLOSE }
        assertEquals(EpubContract.REASON_HOST, close.reason)
        await("reader finished") { activity.isFinishing || activity.isDestroyed }
        session.state.requireError("state after close", EpubErrorCodes.SESSION_CLOSED)
        session.bookmarks.requireError("bookmarks after close", EpubErrorCodes.SESSION_CLOSED)
        failure<IllegalStateException>(EpubErrorCodes.SESSION_CLOSED) { session.navigate(EpubContract.DIRECTION_NEXT_PAGE) }
        session.close(null)

        val all = callback.snapshot()
        assertEquals(close, all.last())
        assertTrue(all.all { it.generation == all[0].generation })
        assertTrue(all.zipWithNext().all { (a, b) -> b.seq > a.seq })
        assertEquals(listOf(EpubContract.EVENT_ERROR, EpubContract.EVENT_OPEN), all.take(2).map { it.type })
        note("events: ${all.size}, generation=${all[0].generation}, seq ${all.first().seq}..${all.last().seq}, types=${all.map { it.type }.distinct()}")
        writeNotes("reader-session")
    }

    @Test
    fun replacementWrongTokenAndClaimTimeout() {
        val plugin = bind()
        val first = RecordingCallback()
        val sessionA = openReader(plugin, first)
        val tokenA = requireNotNull(sessionA.state.requireOk("A").getString(EpubContract.KEY_SESSION_TOKEN))
        val second = RecordingCallback()
        val sessionB = openReader(plugin, second)
        val tokenB = requireNotNull(sessionB.state.requireOk("B").getString(EpubContract.KEY_SESSION_TOKEN))
        assertTrue(tokenA != tokenB)
        val replaced = first.await("A replaced") { it.type == EpubContract.EVENT_CLOSE }
        assertEquals(EpubContract.REASON_REPLACED, replaced.reason)
        sessionA.state.requireError("A after replacement", EpubErrorCodes.SESSION_CLOSED)
        note("replacement: A closed with ${replaced.reason}")

        // A wrong token (or the replaced one) opens nothing: the reader shows the invalid-request panel.
        for (token in listOf(tokenA, "0".repeat(32), "not-a-token")) {
            val stale = launchReader(token)
            await("invalid request shown for $token") {
                stale.findViewById<TextView>(R.id.status_text)?.text?.toString() == stale.getString(R.string.text_invalid_request)
            }
            assertFalse(onMain { stale.navigatorReady })
            onMain { stale.finish() }
        }
        note("wrong tokens: invalid request panel, nothing adopted")
        assertTrue(second.snapshot().none { it.type == EpubContract.EVENT_OPEN })

        val activityB = launchReader(tokenB)
        val open = second.await("B open") { it.type == EpubContract.EVENT_OPEN }
        assertTrue(open.bundle.getBoolean(EpubContract.KEY_VISIBLE))
        // Closing without `finish` leaves the reader on screen as an ordinary reader (D32).
        sessionB.close(options { putBoolean(EpubContract.KEY_FINISH, false) })
        val closeB = second.await("B closed", second.indexOf(open)) { it.type == EpubContract.EVENT_CLOSE }
        assertEquals(EpubContract.REASON_HOST, closeB.reason)
        SystemClock.sleep(500)
        assertFalse(onMain { activityB.isFinishing })
        note("close without finish: reader stays, host heard ${closeB.reason}")

        ReaderSessionRegistry.claimTimeoutMs = 1_500L
        val third = RecordingCallback()
        val sessionC = openReader(plugin, third)
        val started = SystemClock.uptimeMillis()
        val timeout = third.await("C timeout", timeoutMillis = 15_000) { it.type == EpubContract.EVENT_CLOSE }
        assertEquals(EpubContract.REASON_TIMEOUT, timeout.reason)
        assertTrue(SystemClock.uptimeMillis() - started >= 1_000L)
        sessionC.state.requireError("C after timeout", EpubErrorCodes.SESSION_CLOSED)
        note("claim timeout (1500 ms): closed with ${timeout.reason} after ${SystemClock.uptimeMillis() - started} ms")
        writeNotes("reader-session-lifecycle")
    }

    /**
     * Roadmap P6.2: a host launch that the activity manager delivers to the reader already on top
     * of its task (single-top delivery) opens the new session in a fresh reader; the old one ends.
     */
    @Test
    fun hostLaunchOnTopOfAnOpenReaderOpensAFreshInstance() {
        val plugin = bind()
        val first = RecordingCallback()
        val sessionA = openReader(plugin, first)
        val tokenA = requireNotNull(sessionA.state.requireOk("A").getString(EpubContract.KEY_SESSION_TOKEN))
        val activityA = launchReader(tokenA)
        first.await("A open") { it.type == EpubContract.EVENT_OPEN }
        // The host ends A without finishing the reader (D32): it stays on screen as an ordinary reader.
        sessionA.close(options { putBoolean(EpubContract.KEY_FINISH, false) })
        first.await("A closed") { it.type == EpubContract.EVENT_CLOSE }
        SystemClock.sleep(300)
        assertFalse(onMain { activityA.isFinishing })

        val second = RecordingCallback()
        val sessionB = openReader(plugin, second)
        val tokenB = requireNotNull(sessionB.state.requireOk("B").getString(EpubContract.KEY_SESSION_TOKEN))
        val launch = Intent(EpubActions.READER_ACTIVITY_ACTION)
            .setClassName(context.packageName, ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME)
            .putExtra(EpubActions.EXTRA_SESSION_TOKEN, tokenB)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val monitor = instrumentation.addMonitor(EpubReaderActivity::class.java.name, null, false)
        try {
            // The host's launch flags: with A on top of its task the activity manager delivers the intent
            // to A (onNewIntent) instead of creating an instance; A relaunches the session in a fresh one.
            context.startActivity(launch)
            // The monitor also fires when A resumes after onNewIntent (Instrumentation matches on resume,
            // observed on API 28), so wait until a different instance shows up.
            val deadline = SystemClock.uptimeMillis() + 15_000
            var activityB: EpubReaderActivity? = null
            while (activityB == null && SystemClock.uptimeMillis() < deadline) {
                val candidate = monitor.waitForActivityWithTimeout(deadline - SystemClock.uptimeMillis()) as? EpubReaderActivity
                if (candidate != null && candidate !== activityA) activityB = candidate
            }
            activityB ?: throw AssertionError("no fresh reader instance for B")
            activities += activityB
            val open = second.await("B open") { it.type == EpubContract.EVENT_OPEN }
            assertTrue(open.bundle.getBoolean(EpubContract.KEY_VISIBLE))
            await("A finished") { onMain { activityA.isFinishing || activityA.isDestroyed } }
            assertTrue(first.snapshot().count { it.type == EpubContract.EVENT_CLOSE } == 1)
            note("single-top host launch: A finished, B opened in a fresh instance")
        } finally {
            instrumentation.removeMonitor(monitor)
        }
        sessionB.close(options { putBoolean(EpubContract.KEY_FINISH, true) })
        second.await("B closed") { it.type == EpubContract.EVENT_CLOSE }
        writeNotes("reader-session-single-top")
    }

    @Test
    fun refusalsCarryTheContractCodesAndLeakNothing() {
        val plugin = bind()
        val callback = RecordingCallback()
        val baseline = openDescriptors()
        note("no callback: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) { openReader(plugin, null) })
        note("bad locator: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) {
            openReader(plugin, callback, options { putString(EpubContract.KEY_LOCATOR, "not json") })
        })
        note("unknown href: " + failure<IllegalArgumentException>(EpubErrorCodes.RESOURCE_NOT_FOUND) {
            openReader(plugin, callback, options { putString(EpubContract.KEY_HREF, "OEBPS/nope.xhtml") })
        })
        note("progression 2.0: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) {
            openReader(plugin, callback, options { putDouble(EpubContract.KEY_PROGRESSION, 2.0) })
        })
        note("bad preferences: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) {
            openReader(plugin, callback, options { putString(EpubContract.KEY_PREFERENCES, "[]") })
        })
        note("fontSize 9: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) {
            openReader(plugin, callback, options { putString(EpubContract.KEY_PREFERENCES, """{"fontSize":9}""") })
        })
        note("not a zip: " + failure<IllegalArgumentException>(EpubErrorCodes.NOT_EPUB) {
            descriptor("malformed-not-a-zip.epub").use { plugin.openReader(it, options(), callback) }
        })
        assertTrue(callback.snapshot().isEmpty())
        val afterRefusals = awaitDescriptors(baseline)
        note("descriptors after refusals: $afterRefusals (baseline $baseline)")
        assertTrue("descriptor leak: $afterRefusals > $baseline", afterRefusals <= baseline)

        val session = openReader(plugin, callback)
        note("navigate 9: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) { session.navigate(9) })
        note("goTo nothing: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) { session.goTo(options()) })
        note("goTo unknown href: " + failure<IllegalArgumentException>(EpubErrorCodes.RESOURCE_NOT_FOUND) {
            session.goTo(options { putString(EpubContract.KEY_HREF, "OEBPS/nope.xhtml") })
        })
        note("goTo foreign locator: " + failure<IllegalArgumentException>(EpubErrorCodes.RESOURCE_NOT_FOUND) {
            session.goTo(options { putString(EpubContract.KEY_LOCATOR, """{"href":"OEBPS/nope.xhtml","type":"application/xhtml+xml"}""") })
        })
        note("theme blue: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) {
            session.setPreferences(options { putString(EpubContract.KEY_PREFERENCES, """{"theme":"blue"}""") })
        })
        note("missing preferences: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) { session.setPreferences(options()) })
        // A target named before the claim becomes the start position; nothing is emitted for it.
        session.goTo(options { putString(EpubContract.KEY_HREF, CHAPTER3) })
        assertTrue(callback.snapshot().isEmpty())
        session.close(null)
        val close = callback.await("close") { it.type == EpubContract.EVENT_CLOSE }
        assertEquals(EpubContract.REASON_HOST, close.reason)
        assertEquals(1, callback.snapshot().size)
        val afterClose = awaitDescriptors(baseline)
        note("descriptors after close: $afterClose (baseline $baseline)")
        assertTrue("descriptor leak: $afterClose > $baseline", afterClose <= baseline)
        writeNotes("reader-session-failures")
    }

    private companion object {
        const val FIXTURE = "minimal-epub3.epub"
        const val CHAPTER1 = "OEBPS/chapter1.xhtml"
        const val CHAPTER2 = "OEBPS/chapter2.xhtml"
        const val CHAPTER3 = "OEBPS/chapter3.xhtml"
    }
}

/** Hides the local interface so the generated Proxy marshals every call like the host's process would. */
private class RemoteOnlySessionBinder(private val target: IBinder) : IBinder by target {
    override fun queryLocalInterface(descriptor: String): android.os.IInterface? = null
    override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = target.transact(code, data, reply, flags)
}
