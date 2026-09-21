package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import androidx.webkit.WebViewCompat
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.WebViewBoundary
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P7.2 WebView boundary (DEVICE evidence): every page WebView Readium creates for a book carries
 * the plugin's boundary (no file or content-provider access, the file-URL cross-origin switches off)
 * while JavaScript stays on (D6); the page lives on Readium's in-memory `https://readium_package` host,
 * and a page script that asks for a `content://` document or a `file://` path gets nothing (the debug
 * provider is not even opened), while the same script reaches the page's own host. Notes land in
 * `files/p2-evidence/webview-boundary-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderWebViewBoundaryInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val notes = ArrayList<String>()

    @Test
    fun everyPageWebViewCarriesTheBoundaryAndStaysOnReadiumsHost() {
        val provider = WebViewCompat.getCurrentWebViewPackage(context)
        note("WebView provider: " + (provider?.let { "${it.packageName} ${it.versionName}" } ?: "unknown") + " on API ${Build.VERSION.SDK_INT}")
        EpubReaderTestContentProvider.fileFor(context, PROBE_DOCUMENT).writeText("probe")
        val probeUri = EpubReaderTestContentProvider.documentUri(PROBE_DOCUMENT)

        val activity = instrumentation.startActivitySync(request(FIXTURE)) as EpubReaderActivity
        try {
            await("the navigator is ready", NAVIGATOR_TIMEOUT_MILLIS) { activity.navigatorReady }
            val page = pollFor("a page WebView on Readium's package host", PAGE_TIMEOUT_MILLIS) {
                onMain { webViews(activity) }.firstOrNull { js(it, "location.origin") == PACKAGE_ORIGIN }
            }
            val views = onMain { webViews(activity) }
            note("page WebViews: ${views.size}")
            for ((index, view) in views.withIndex()) {
                val settings = onMain { view.settings }
                val origin = js(view, "location.origin")
                @Suppress("DEPRECATION")
                val line = onMain {
                    "webView[$index] ${view.javaClass.simpleName}: allowFileAccess=${settings.allowFileAccess}" +
                        " allowContentAccess=${settings.allowContentAccess}" +
                        " allowFileAccessFromFileURLs=${settings.allowFileAccessFromFileURLs}" +
                        " allowUniversalAccessFromFileURLs=${settings.allowUniversalAccessFromFileURLs}" +
                        " javaScriptEnabled=${settings.javaScriptEnabled} domStorageEnabled=${settings.domStorageEnabled}" +
                        " origin=$origin"
                }
                note(line)
                assertTrue(line, onMain { WebViewBoundary.holds(settings) })
                assertTrue("JavaScript stays on for Readium (D6)", onMain { settings.javaScriptEnabled })
            }

            note("typeof Android (Readium's own interface) = " + js(page, "typeof Android"))
            note("page href = " + js(page, "location.href"))

            val self = probe(page, "fetch(location.href).then(function(r){ return 'ok ' + r.status; })")
            note("fetch(location.href) -> $self")
            assertEquals("the probe reaches the page's own host", "ok 200", self)

            val opensBefore = EpubReaderTestContentProvider.openCount.get()
            val contentFetch = probe(page, "fetch('$probeUri').then(function(r){ return 'ok ' + r.status; })")
            note("fetch($probeUri) -> $contentFetch")
            val contentImage = probe(
                page,
                "new Promise(function(resolve){ var i = new Image(); i.onload = function(){ resolve('loaded'); };" +
                    " i.onerror = function(){ resolve('error'); }; i.src = '$probeUri'; })",
            )
            note("<img src=$probeUri> -> $contentImage")
            val fileFetch = probe(page, "fetch('file:///etc/hosts').then(function(r){ return 'ok ' + r.status; })")
            note("fetch(file:///etc/hosts) -> $fileFetch")
            val opensDuring = EpubReaderTestContentProvider.openCount.get() - opensBefore
            note("test provider opened by the page: $opensDuring times")
            assertTrue(contentFetch, contentFetch.startsWith("error"))
            assertEquals("error", contentImage)
            assertTrue(fileFetch, fileFetch.startsWith("error"))
            assertEquals("the page never reached the content provider", 0, opensDuring)

            // Control: the same document is served to the test process itself, so the counter is live.
            context.contentResolver.openInputStream(probeUri)!!.use { it.readBytes() }
            assertEquals(1, EpubReaderTestContentProvider.openCount.get() - opensBefore - opensDuring)
            note("control: the test process opened the same document once")
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
            awaitOrNot(5_000) { activity.isDestroyed }
        }
        writeNotes("webview-boundary")
    }

    private fun webViews(root: EpubReaderActivity): List<WebView> {
        val found = ArrayList<WebView>()
        fun walk(view: View) {
            if (view is WebView) found += view
            if (view is ViewGroup) for (index in 0 until view.childCount) walk(view.getChildAt(index))
        }
        walk(root.findViewById(R.id.reader_container))
        return found
    }

    /** Evaluates [script] on the page and waits for its (JSON-encoded) answer from the instrumentation thread. */
    private fun js(view: WebView, script: String): String {
        val answer = AtomicReference<String?>()
        instrumentation.runOnMainSync { view.evaluateJavascript(script) { answer.set(it) } }
        val deadline = SystemClock.uptimeMillis() + JS_TIMEOUT_MILLIS
        while (answer.get() == null && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        val raw = answer.get() ?: return "<no answer within $JS_TIMEOUT_MILLIS ms>"
        return if (raw.startsWith("\"")) JSONArray("[$raw]").getString(0) else raw
    }

    /** Runs an asynchronous [expression] (a promise) on the page and polls its settled value. */
    private fun probe(view: WebView, expression: String): String {
        val started = js(
            view,
            "(function(){ window.__boundaryProbe = 'pending'; Promise.resolve().then(function(){ return $expression; })" +
                ".then(function(v){ window.__boundaryProbe = String(v); }, function(e){ window.__boundaryProbe = 'error ' + e; });" +
                " return 'started'; })()",
        )
        assertEquals("started", started)
        val deadline = SystemClock.uptimeMillis() + PROBE_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            val value = js(view, "window.__boundaryProbe")
            if (value != "pending") return value
            SystemClock.sleep(200)
        }
        return "pending after $PROBE_TIMEOUT_MILLIS ms"
    }

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

    private fun <T : Any> pollFor(message: String, timeoutMillis: Long, supplier: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            runCatching(supplier).getOrNull()?.let { return it }
            SystemClock.sleep(250)
        }
        throw AssertionError("Timed out: $message")
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
        const val FIXTURE = "minimal-epub3.epub"
        const val PROBE_DOCUMENT = "boundary-probe.txt"
        const val PACKAGE_ORIGIN = "https://readium_package"
        const val NAVIGATOR_TIMEOUT_MILLIS = 60_000L
        const val PAGE_TIMEOUT_MILLIS = 30_000L
        const val JS_TIMEOUT_MILLIS = 10_000L
        const val PROBE_TIMEOUT_MILLIS = 15_000L
    }
}
