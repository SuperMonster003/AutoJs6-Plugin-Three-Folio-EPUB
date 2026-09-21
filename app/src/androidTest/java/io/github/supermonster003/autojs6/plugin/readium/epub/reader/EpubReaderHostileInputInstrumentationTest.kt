package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Roadmap P7.1 hostile-input matrix through the reader Activity (DEVICE evidence): every malformed
 * fixture of docs/fixtures, opened the way the AutoJs6 file manager opens a book, ends either in a
 * working navigator or on the error panel with one of the localized open-failure messages; the
 * Activity never finishes on its own, the process never dies, and a refused book leaves no book
 * directory behind. Notes land in `files/p2-evidence/hostile-reader-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderHostileInputInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val notes = ArrayList<String>()

    @Test
    fun everyHostileFixtureEndsInTheReaderOrOnTheErrorPanel() {
        val booksDirectory = File(context.filesDir, "books")
        for (fixture in HOSTILE_FIXTURES) {
            val booksBefore = booksSnapshot(booksDirectory)
            val started = SystemClock.uptimeMillis()
            val activity = instrumentation.startActivitySync(request(fixture)) as EpubReaderActivity
            var opened = false
            try {
                await("$fixture settles", SETTLE_TIMEOUT_MILLIS) { activity.navigatorReady || errorShown(activity) }
                val elapsed = SystemClock.uptimeMillis() - started
                opened = onMain { activity.navigatorReady }
                val outcome = if (opened) {
                    "reader (${activity.readerModel.publication?.readingOrder?.size} reading-order entries)"
                } else {
                    val text = onMain { statusText(activity) }
                    main { assertLocalizedFailure(activity, text) }
                    "error panel [$text]"
                }
                main {
                    assertFalse("$fixture: the Activity finished on its own", activity.isFinishing)
                    assertFalse(activity.findViewById<View>(R.id.status_progress).isShown)
                }
                note("$fixture -> $outcome in $elapsed ms")
            } finally {
                main { activity.finish() }
                instrumentation.waitForIdleSync()
                awaitOrNot(5_000) { activity.isDestroyed }
            }
            if (!opened) {
                assertEquals("$fixture: a refused book wrote into files/books", booksBefore, booksSnapshot(booksDirectory))
            }
        }
        writeNotes("hostile-reader")
    }

    private fun assertLocalizedFailure(activity: EpubReaderActivity, text: String) {
        val exact = listOf(
            R.string.text_open_failed_not_epub,
            R.string.text_open_failed_protected,
            R.string.text_open_failed_timeout,
            R.string.text_cannot_read_file,
        ).map { activity.getString(it) }
        val prefixed = activity.getString(R.string.text_open_failed, "").trimEnd()
        assertTrue("unexpected error text [$text]", text in exact || (text.startsWith(prefixed) && text.length > prefixed.length))
        assertFalse(text == activity.getString(R.string.text_invalid_request))
        assertFalse(text == activity.getString(R.string.text_opening_book))
    }

    private fun errorShown(activity: EpubReaderActivity): Boolean {
        val text = activity.findViewById<TextView>(R.id.status_text)
        return text.isShown && text.text.isNotBlank() && text.text.toString() != activity.getString(R.string.text_opening_book) &&
            !activity.findViewById<View>(R.id.status_progress).isShown
    }

    private fun statusText(activity: EpubReaderActivity): String = activity.findViewById<TextView>(R.id.status_text).text.toString()

    private fun booksSnapshot(directory: File): List<String> =
        directory.walkTopDown().filter { it.isFile }.map { it.relativeTo(directory).path.replace('\\', '/') + ":" + it.length() }.toList().sorted()

    private fun request(fixture: String): Intent {
        val target = EpubReaderTestContentProvider.fileFor(context, fixture)
        assets.open(fixture).use { input -> target.outputStream().use { input.copyTo(it) } }
        val documentUri = EpubReaderTestContentProvider.documentUri(fixture)
        val parentUri = EpubReaderTestContentProvider.parentUri()
        return Intent(context, EpubReaderActivity::class.java)
            .setAction(ExplorerActionPluginActions.EXECUTE)
            .setDataAndType(documentUri, ReadiumEpubReaderPlugin.EPUB_MIME_TYPE)
            .putExtra(ExplorerActionIntentExtras.ACTION_ID, ReadiumEpubReaderPlugin.PRIMARY_ACTION_ID)
            .putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, ReadiumEpubReaderPlugin.PROTOCOL_VERSION)
            .putExtra(ExplorerActionIntentExtras.HOST_PACKAGE_NAME, "org.autojs.autojs6")
            .putExtra(ExplorerActionIntentExtras.HOST_VERSION_CODE, EpubReaderExplorerCompatibility.maximumAuditedHostVersionCode)
            .putExtra(ExplorerActionIntentExtras.SOURCE_SURFACE, ExplorerActionIntentValues.SOURCE_SURFACE_MAIN)
            .putExtra(ExplorerActionIntentExtras.PARENT_URI, parentUri)
            .putExtra(ExplorerActionIntentExtras.DISPLAY_NAME, fixture)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            .apply {
                clipData = ClipData.newRawUri("Document", documentUri).apply { addItem(ClipData.Item(parentUri)) }
            }
    }

    private fun awaitOrNot(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = runCatching(condition).getOrDefault(false) }
            if (ready) return true
            SystemClock.sleep(200)
        }
        return false
    }

    private fun await(message: String, timeoutMillis: Long, condition: () -> Boolean) {
        if (!awaitOrNot(timeoutMillis, condition)) fail("Timed out: $message")
    }

    private fun main(action: () -> Unit) = onMain(action)

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private fun note(line: String) {
        notes += line
    }

    private fun writeNotes(name: String) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }
            .resolve("$name-api${Build.VERSION.SDK_INT}.txt")
            .writeText(notes.joinToString("\n") + "\n")
    }

    private companion object {
        /** The reader's own open timeout is 60 s (EpubReaderViewModel); the panel must settle before that plus a margin. */
        const val SETTLE_TIMEOUT_MILLIS = 75_000L

        val HOSTILE_FIXTURES = listOf(
            "malformed-not-a-zip.epub",
            "malformed-empty-zip.epub",
            "malformed-missing-mimetype.epub",
            "malformed-missing-container.epub",
            "malformed-missing-opf.epub",
            "malformed-bad-opf.epub",
            "malformed-bad-ncx.epub",
            "malformed-xxe.epub",
            "malformed-path-traversal.epub",
            "malformed-traversal-encoded.epub",
            "malformed-long-names.epub",
            "malformed-duplicate-entries.epub",
            "malformed-many-entries.epub",
            "malformed-high-ratio.epub",
            "malformed-encrypted-lcp.epub",
            "malformed-lcp-license-only.epub",
            "malformed-encrypted-adept.epub",
        )
    }
}
