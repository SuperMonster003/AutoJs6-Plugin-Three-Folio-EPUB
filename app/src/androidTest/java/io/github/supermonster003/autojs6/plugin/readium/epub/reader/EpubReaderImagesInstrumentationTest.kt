package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.Window
import androidx.lifecycle.lifecycleScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.BookDataStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderPreferencesStore
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import kotlinx.coroutines.launch
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P2.7 device evidence for images: tapping an image in a reflowable page opens the
 * viewer with the decoded image and its alt text as the caption, dismissing it leaves the page
 * where it was, and a fixed-layout page (whose taps carry no element) does nothing.
 * Notes land in `files/p2-evidence/images-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderImagesInstrumentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val assets get() = instrumentation.context.assets
    private val booksDirectory get() = File(context.filesDir, BookDataStore.DIRECTORY_NAME)
    private val preferencesStore get() = ReaderPreferencesStore.forFilesDirectory(context.filesDir)

    @Before
    fun resetState() = cleanUp()

    @After
    fun cleanUp() {
        booksDirectory.deleteRecursively()
        preferencesStore.clear()
        context.getSharedPreferences(ReaderSettings.PREFERENCES_NAME, 0).edit().clear().commit()
    }

    @Test
    fun tappedImageOpensTheViewer() {
        val activity = instrumentation.startActivitySync(request(REFLOWABLE)) as EpubReaderActivity
        try {
            awaitHref(activity, "chapter1.xhtml")
            SystemClock.sleep(500)
            assertNull(onMain { activity.imageViewer })
            val clickedAt = SystemClock.uptimeMillis()
            evaluate(activity, "document.querySelector('img').click(); 'clicked'")
            await("image viewer shows the image") { activity.imageViewer?.hasImage == true }
            val shownMillis = SystemClock.uptimeMillis() - clickedAt
            val viewer = requireNotNull(onMain { activity.imageViewer })
            val href = onMain { viewer.href }
            val caption = onMain { viewer.captionText?.toString() }
            assertTrue(href, href?.endsWith("images/beacon.png") == true)
            assertEquals("A small beacon glyph", caption)
            SystemClock.sleep(300)
            onMain { viewer.dialog?.window }?.let { screenshot(it, "images-viewer") }

            main { viewer.dismiss() }
            await("viewer dismissed") { activity.imageViewer == null }
            assertTrue(currentHref(activity)?.endsWith("chapter1.xhtml") == true)
            record(
                "images-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "reflowable image viewer=${shownMillis}ms href=$href caption=$caption",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    @Test
    fun fixedLayoutTapsDoNotOpenTheViewer() {
        val activity = instrumentation.startActivitySync(request(FIXED)) as EpubReaderActivity
        try {
            awaitHref(activity, "page1.xhtml")
            SystemClock.sleep(1000)
            evaluate(activity, "(function(){var i=document.querySelector('img'); if(i){i.click(); return 'clicked'} return 'no-img'})()")
            SystemClock.sleep(1500)
            assertNull(onMain { activity.imageViewer })
            record(
                "images-fixed-api${Build.VERSION.SDK_INT}.txt",
                listOf(
                    "device=${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "fixed-layout image tap opens nothing (no target element)",
                ),
            )
        } finally {
            finish(activity)
        }
    }

    // ---- helpers ----

    /** Runs [script] in the current page from the instrumentation thread (the call suspends on main). */
    private fun evaluate(activity: EpubReaderActivity, script: String): String? {
        check(Looper.myLooper() != Looper.getMainLooper())
        val latch = CountDownLatch(1)
        var result: String? = null
        instrumentation.runOnMainSync {
            activity.lifecycleScope.launch {
                result = runCatching { navigator(activity).evaluateJavascript(script) }.getOrNull()
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        return result
    }


    /** A PixelCopy of [window] (the dialog's own window, which the Activity's copy would not include). */
    private fun screenshot(window: Window, name: String) {
        check(Looper.myLooper() != Looper.getMainLooper())
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decor = window.decorView
            if (decor.width == 0 || decor.height == 0) return
            val target = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            val done = CountDownLatch(1)
            var result = PixelCopy.ERROR_UNKNOWN
            PixelCopy.request(window, target, { result = it; done.countDown() }, Handler(Looper.getMainLooper()))
            if (!done.await(10, TimeUnit.SECONDS) || result != PixelCopy.SUCCESS) return
            target
        } else {
            instrumentation.uiAutomation.takeScreenshot() ?: return
        }
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve("$name-api${Build.VERSION.SDK_INT}.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun record(name: String, lines: List<String>) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }.resolve(name).writeText(lines.joinToString("\n") + "\n")
    }

    private fun finish(activity: EpubReaderActivity) {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
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

    private fun navigator(activity: EpubReaderActivity): EpubNavigatorFragment =
        requireNotNull(activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)

    private fun currentHref(activity: EpubReaderActivity): String? = onMain {
        (activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)
            ?.takeIf { it.view != null }
            ?.currentLocator?.value?.href?.toString()
    }

    private fun awaitHref(activity: EpubReaderActivity, suffix: String) {
        await("href $suffix") { activity.navigatorReady && currentHref(activity)?.endsWith(suffix) == true }
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

    private fun main(action: () -> Unit) = onMain(action)

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private companion object {
        const val REFLOWABLE = "minimal-epub3.epub"
        const val FIXED = "fixed-layout.epub"
    }
}
