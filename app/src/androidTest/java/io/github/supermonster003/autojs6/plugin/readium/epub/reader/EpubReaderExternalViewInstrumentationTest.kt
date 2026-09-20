package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.widget.TextView
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.LauncherActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher.RecentBooksStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File

/**
 * Roadmap P4.2 device evidence: another app's `ACTION_VIEW` (played by the shell, `am start`)
 * opens a `content://` EPUB in the exported viewer without listing it, "add to recent books"
 * refuses when the sender's grant cannot persist, the viewer rejects what the external rules
 * forbid (no grant, `file://`, an Explorer envelope) and the protected reader refuses `ACTION_VIEW`
 * altogether; where the shell can grant a documents-provider URI persistably, the book joins the
 * recent list and reopens from the launcher. Notes land in `files/p2-evidence/view-*-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderExternalViewInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val store get() = RecentBooksStore.forFilesDirectory(context.filesDir)
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)
    private val notes = ArrayList<String>()
    private var readerMonitor: Instrumentation.ActivityMonitor? = null
    private var lastShellOutput = ""
    private var openCountBefore = 0

    @Before
    fun resetState() {
        cleanUp()
        notes += "api=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}"
        readerMonitor = instrumentation.addMonitor(EpubReaderActivity::class.java.name, null, false)
    }

    @After
    fun cleanUp() {
        readerMonitor?.let { instrumentation.removeMonitor(it) }
        readerMonitor = null
        store.clear()
        booksDirectory.deleteRecursively()
        context.contentResolver.persistedUriPermissions.forEach {
            runCatching { context.contentResolver.releasePersistableUriPermission(it.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    @Test
    fun aViewIntentOpensTheBookWithoutListingItAndCannotKeepAnUnpersistableGrant() {
        val uri = provide(FIXTURE)
        val viewer = startViewer(viewCommand(uri, persistable = false))
        try {
            awaitHref(viewer, "chapter1.xhtml")
            val request = requireNotNull(onMain { viewer.readerModel.request })
            assertEquals(ReaderEntry.EXTERNAL, request.entry)
            assertEquals("the provider's _display_name", FIXTURE, request.displayName)
            assertNull("an ACTION_VIEW book is not listed", onMain { viewer.readerModel.recentUri })
            assertTrue(store.read().isEmpty())
            assertEquals(1, EpubReaderTestContentProvider.openCount.get() - openCountBefore)

            // The shell granted read access only for this launch, and a same-process provider offers nothing to persist.
            assertFalse("add to recent refuses", onMain { viewer.addToRecent() })
            assertTrue(store.read().isEmpty())
            assertNull(onMain { viewer.readerModel.recentUri })
            notes += "entry=${request.entry} displayName=${request.displayName} listed=false addToRecent=refused"
        } finally {
            finish(viewer)
        }
        record("view-api${Build.VERSION.SDK_INT}.txt", notes)
    }

    @Test
    fun theViewerRejectsWhatTheExternalRulesForbid() {
        val uri = provide(FIXTURE)
        val invalid = context.getString(R.string.text_invalid_request)

        // No read grant: the request is refused before anything is opened.
        startViewer("am start -a android.intent.action.VIEW -d $uri -t application/epub+zip -n ${component(ExternalViewerActivity::class.java)}").also { viewer ->
            await("refused without a grant") { statusText(viewer) == invalid }
            assertTrue(onMain { viewer.readerModel.state.value is OpenState.Idle })
            finish(viewer)
            await("viewer gone") { viewer.isDestroyed }
        }

        // file:// never passes (roadmap D27).
        startViewer("am start -a android.intent.action.VIEW -d file:///sdcard/novel.epub -t application/epub+zip --grant-read-uri-permission -n ${component(ExternalViewerActivity::class.java)}").also { viewer ->
            await("refused file URI") { statusText(viewer) == invalid }
            finish(viewer)
            await("viewer gone") { viewer.isDestroyed }
        }

        // An Explorer envelope aimed at the exported viewer is not one of its doors.
        startViewer("am start -a org.autojs.plugin.EXPLORER_ACTION_EXECUTE -d $uri -t application/epub+zip --grant-read-uri-permission --grant-prefix-uri-permission -n ${component(ExternalViewerActivity::class.java)}").also { viewer ->
            await("refused Explorer envelope") { statusText(viewer) == invalid }
            finish(viewer)
            await("viewer gone") { viewer.isDestroyed }
        }

        // ACTION_VIEW at the protected reader is refused by the system before the activity exists: the
        // shell holds no PLUGIN permission (stderr is merged into the output through eval).
        val reader = startFromShell(withStderr(viewCommand(uri, persistable = false, target = EpubReaderActivity::class.java)), EpubReaderActivity::class.java, 3_000)
        assertNull("the reader stays closed to ACTION_VIEW: $lastShellOutput", reader)
        assertTrue(lastShellOutput, lastShellOutput.contains("requires org.autojs.permission.PLUGIN"))
        notes += "no-grant=refused file-uri=refused explorer-envelope-at-viewer=refused view-at-reader=denied-by-system"
        record("view-rules-api${Build.VERSION.SDK_INT}.txt", notes)
    }

    @Test
    fun aPersistableGrantLetsTheBookJoinTheRecentListAndReopenFromTheLauncher() {
        provide(FIXTURE)
        // Only a provider outside this process can hand out a persistable grant: the shell copies the
        // fixture to Download (through a cat of its own: a redirect written by the app's uid stays
        // empty on FUSE) and hands over the documents-provider URI the way a file manager would.
        val copied = shell("sh -c run-as\${IFS}${context.packageName}\${IFS}cat\${IFS}cache/epub-reader-test-documents/$FIXTURE|cat>$PUBLIC_COPY")
        val size = shell("sh -c wc\${IFS}-c<$PUBLIC_COPY").trim().toLongOrNull() ?: 0L
        notes += "copy: ${copied.trim()} size=$size"
        assumeTrue("the shell could not place the fixture in Download: $copied size=$size", size > 0)
        try {
            val uri = "content://com.android.externalstorage.documents/document/primary%3ADownload%2F$PUBLIC_NAME".toUri()
            val viewer = startViewer(viewCommand(uri, persistable = true), timeoutMillis = 10_000)
            val refused = lastShellOutput.contains("exception", ignoreCase = true) || lastShellOutput.contains("Security")
            notes += "shell-grant=${if (viewer == null || refused) "denied" else "granted"}"
            if (viewer == null || refused) record("view-persistable-api${Build.VERSION.SDK_INT}.txt", notes)
            assumeTrue("the shell cannot grant documents-provider URIs on this device: $lastShellOutput", viewer != null && !refused)
            viewer!!
            try {
                awaitHref(viewer, "chapter1.xhtml")
                assertEquals(PUBLIC_NAME, onMain { viewer.readerModel.request?.displayName })
                assertTrue("add to recent keeps the grant", onMain { viewer.addToRecent() })
                await("listed") { store.read().singleOrNull()?.uri == uri.toString() }
                await("tracked") { onMain { viewer.readerModel.recentUri } == uri.toString() }
                assertTrue(context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission })
                await("enriched") { store.read().single().title != null }
                val entry = store.read().single()
                notes += "added=${entry.displayName} title=${entry.title} persisted=true"
            } finally {
                finish(viewer)
            }
            await("viewer closed") { viewer.isDestroyed }

            // The launcher lists the book and opens it through its own door.
            val launcher = instrumentation.startActivitySync(
                Intent(context, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            ) as LauncherActivity
            await("launcher lists it") { launcher.shownBooks.singleOrNull()?.uri == uri.toString() }
            main { launcher.openBook(launcher.shownBooks.single()) }
            val reader = (readerMonitor!!.waitForActivityWithTimeout(30_000) ?: fail("The launcher did not open the book")) as EpubReaderActivity
            try {
                awaitHref(reader, "chapter1.xhtml")
                assertEquals(ReaderEntry.LAUNCHER, onMain { reader.readerModel.request?.entry })
                notes += "reopened-from-launcher=true"
            } finally {
                finish(reader)
                finish(launcher)
            }
            record("view-persistable-api${Build.VERSION.SDK_INT}.txt", notes)
        } finally {
            shell("rm $PUBLIC_COPY")
        }
    }

    // ---- helpers ----

    private fun provide(fixture: String): Uri {
        val target = EpubReaderTestContentProvider.fileFor(context, fixture)
        assets.open(fixture).use { input -> target.outputStream().use { input.copyTo(it) } }
        openCountBefore = EpubReaderTestContentProvider.openCount.get()
        return EpubReaderTestContentProvider.documentUri(fixture)
    }

    private fun component(target: Class<out Activity>): String = "${context.packageName}/${target.name}"

    private fun viewCommand(uri: Uri, persistable: Boolean, target: Class<out Activity> = ExternalViewerActivity::class.java): String =
        "am start -a android.intent.action.VIEW -d $uri -t application/epub+zip --grant-read-uri-permission" +
            (if (persistable) " --grant-persistable-uri-permission" else "") +
            " -n ${component(target)}"

    /**
     * UiAutomation returns stdout only; `am` reports refusals on stderr. This wraps a space-free
     * command so the shell re-parses it with `2>&1` (Runtime.exec splits on whitespace, hence IFS).
     */
    private fun withStderr(command: String): String =
        "sh -c eval" + command.split(' ').joinToString("") { "\${IFS}$it" } + "\${IFS}2\\>\\&1"

    /** Runs a shell command through UiAutomation (the shell user plays the other app) and returns its output. */
    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use {
            it.readBytes().toString(Charsets.UTF_8)
        }

    /**
     * Runs an `am start` and returns the activity it produced, skipping instances already on their
     * way out (the host-appearance sync may recreate an activity once); null when none appears.
     */
    private fun <T : Activity> startFromShell(command: String, target: Class<T>, timeoutMillis: Long): T? {
        val monitor = instrumentation.addMonitor(target.name, null, false)
        try {
            lastShellOutput = shell(command)
            notes += "am-start: ${lastShellOutput.trim().replace('\n', ' ').take(300)}"
            val deadline = SystemClock.uptimeMillis() + timeoutMillis
            while (SystemClock.uptimeMillis() < deadline) {
                val activity = monitor.waitForActivityWithTimeout(deadline - SystemClock.uptimeMillis()) ?: break
                if (target.isInstance(activity) && !onMain { activity.isFinishing || activity.isDestroyed }) return target.cast(activity)
            }
            return null
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun startViewer(command: String, timeoutMillis: Long): ExternalViewerActivity? =
        startFromShell(command, ExternalViewerActivity::class.java, timeoutMillis)

    private fun startViewer(command: String): ExternalViewerActivity =
        startViewer(command, 30_000) ?: failWith("The viewer did not start: $lastShellOutput")

    private fun statusText(activity: EpubReaderActivity): String? = onMain {
        activity.findViewById<TextView>(R.id.status_text)?.text?.toString()
    }

    private fun currentHref(activity: EpubReaderActivity): String? = onMain {
        (activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)
            ?.takeIf { it.view != null }
            ?.currentLocator?.value?.href?.toString()
    }

    private fun awaitHref(activity: EpubReaderActivity, suffix: String) {
        await("href $suffix") { activity.navigatorReady && currentHref(activity)?.endsWith(suffix) == true }
    }

    private fun finish(activity: Activity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
    }

    private fun record(name: String, lines: List<String>) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name).writeText(lines.joinToString("\n") + "\n")
    }

    private fun await(message: String, timeoutMillis: Long = 30000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = runCatching(condition).getOrDefault(false) }
            if (ready) return
            SystemClock.sleep(100)
        }
        fail("Timed out: $message")
    }

    private fun <T> failWith(message: String): T = throw AssertionError(message)

    private fun main(action: () -> Unit) = onMain(action)

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private companion object {
        const val FIXTURE = "minimal-epub3.epub"
        const val PUBLIC_NAME = "epub-reader-view-test.epub"
        const val PUBLIC_COPY = "/sdcard/Download/$PUBLIC_NAME"
    }
}
