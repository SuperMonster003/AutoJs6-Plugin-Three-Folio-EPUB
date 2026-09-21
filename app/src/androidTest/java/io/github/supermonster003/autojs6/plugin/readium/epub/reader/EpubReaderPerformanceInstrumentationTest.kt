package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.service.ReadiumEpubReaderPluginService
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsStatus
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.IEpubBook
import org.autojs.plugin.epub.api.IEpubPlugin
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.BufferedOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

/**
 * Roadmap P7.4 performance baseline (DEVICE evidence, kept apart from the correctness tests): four books
 * synthesized on the device (about 1 MB and 20 MB with a noise image per chapter, about 200 MB with a
 * thousand of them, and 5000 text chapters) are opened cold and warm through the reader, recording the
 * time to the first screen, to the position count and to the full fingerprint (both computed in the
 * background), the frame times of twenty animated page turns, and the process PSS peak; then through
 * the Binder service, recording open, metadata with positions, and a search that matches only the last
 * chapter. `readAloudForThirtyMinutesKeepsMemoryFlat` runs only with `-e tts 30` on a device with a
 * speech engine. Notes land in `files/p2-evidence/perf-api<N>.txt` and `perf-tts-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class EpubReaderPerformanceInstrumentationTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val notes = ArrayList<String>()

    private class Book(val label: String, val file: File, val chapters: Int)

    @Test
    fun coldAndWarmOpensPageTurnsAndTheBinderSideOnFourBooks() {
        note("API ${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL}; free cache ${StatFs(context.cacheDir.path).availableBytes shr 20} MiB")
        val books = ArrayList<Book>()
        books += Book("1 MB", generate("perf-1mb.epub", chapters = 5, images = true), 5)
        books += Book("20 MB", generate("perf-20mb.epub", chapters = 100, images = true), 100)
        if (StatFs(context.cacheDir.path).availableBytes > 1_200L shl 20) {
            books += Book("200 MB", generate("perf-200mb.epub", chapters = 1000, images = true), 1000)
        } else {
            note("200 MB: skipped, less than 1.2 GiB free in the cache directory")
        }
        books += Book("5000 chapters", generate("perf-5000ch.epub", chapters = 5000, images = false), 5000)
        for (book in books) note("${book.label}: ${book.file.name} ${book.file.length()} bytes")

        note("| book | run | first screen | positions | fingerprint | 20 page turns (frames, jank, p50 / p90 / p99) | PSS peak |")
        note("|---|---|---|---|---|---|---|")
        for (book in books) {
            measureReader(book, "cold")
            measureReader(book, "warm")
        }
        note("| book | Binder open | metadata + positions | search (last chapter) |")
        note("|---|---|---|---|")
        for (book in books) measureService(book)
        writeNotes("perf")
    }

    @Test
    fun readAloudForThirtyMinutesKeepsMemoryFlat() {
        assumeTrue("run with -e tts 30", InstrumentationRegistry.getArguments().getString("tts") == "30")
        val book = Book("5000 chapters", generate("perf-5000ch.epub", chapters = 5000, images = false), 5000)
        val activity = instrumentation.startActivitySync(request(book.file.name)) as EpubReaderActivity
        val samples = ArrayList<Long>()
        try {
            await("first screen", 120_000) { activity.navigatorReady }
            main { activity.startReadAloud() }
            val playing = awaitOrNot(60_000) { activity.readerModel.tts.status.value == TtsStatus.PLAYING }
            note("read-aloud: ${if (playing) "playing" else "no engine spoke within 60 s"} on API ${Build.VERSION.SDK_INT} ${Build.MODEL}")
            assumeTrue("no speech engine on this device", playing)
            for (minute in 1..30) {
                SystemClock.sleep(60_000)
                val pss = Debug.getPss()
                samples += pss
                val status = onMain { activity.readerModel.tts.status.value }
                val locator = locator(activity)
                note("minute $minute: PSS ${pss shr 10} MB, status $status, at $locator")
                if (status != TtsStatus.PLAYING) {
                    note("read-aloud stopped at minute $minute")
                    break
                }
            }
        } finally {
            main { activity.readerModel.tts.stop() }
            main { activity.finish() }
            instrumentation.waitForIdleSync()
        }
        writeNotes("perf-tts")
        assumeTrue("fewer than 6 samples", samples.size >= 6)
        val settled = samples.drop(4)
        val growth = (settled.last() - settled.min()) shr 10
        note("growth after minute 5: $growth MB")
        writeNotes("perf-tts")
        assertTrue("PSS grew by $growth MB after the fifth minute", growth < 128)
    }

    // ---- The reader ----

    private fun measureReader(book: Book, run: String) {
        val expectedKey = sha256(book.file)
        val sampler = PssSampler().also { it.start() }
        val started = SystemClock.elapsedRealtime()
        val activity = instrumentation.startActivitySync(request(book.file.name)) as EpubReaderActivity
        try {
            await("${book.label} $run: first screen", 180_000) { activity.navigatorReady }
            val firstScreen = SystemClock.elapsedRealtime() - started
            val positions = if (awaitOrNot(300_000) { activity.readerModel.positionCount.value > 0 }) SystemClock.elapsedRealtime() - started else -1L
            val fingerprint = if (awaitOrNot(300_000) { activity.readerModel.bookKey == expectedKey }) SystemClock.elapsedRealtime() - started else -1L
            val frames = FrameCollector(onMain { activity.window }).also { it.start() }
            repeat(TURNS) {
                main { navigator(activity).goForward(animated = true) }
                SystemClock.sleep(600)
            }
            val stats = frames.stop()
            val peak = sampler.peakKb()
            note(
                "| ${book.label} | $run | $firstScreen ms | ${positions.orDash()} | ${fingerprint.orDash()} | " +
                    "${stats.count} frames, ${stats.jankPercent}% > 16.7 ms, ${stats.p50} / ${stats.p90} / ${stats.p99} ms | ${peak shr 10} MB |",
            )
        } finally {
            main { activity.finish() }
            instrumentation.waitForIdleSync()
            awaitOrNot(10_000) { activity.isDestroyed }
            sampler.stop()
        }
    }

    private fun Long.orDash(): String = if (this < 0) "-" else "$this ms"

    // ---- The Binder service ----

    private fun measureService(book: Book) {
        val plugin = bind()
        val started = SystemClock.elapsedRealtime()
        val remote = ParcelFileDescriptor.open(book.file, ParcelFileDescriptor.MODE_READ_ONLY).use { plugin.openBook(it, Bundle()).remote() }
        val open = SystemClock.elapsedRealtime() - started
        try {
            val metadataStarted = SystemClock.elapsedRealtime()
            val metadata = remote.metadata
            val metadataMs = SystemClock.elapsedRealtime() - metadataStarted
            val positions = JSONObject(metadata.getString(EpubContract.KEY_METADATA).orEmpty().ifEmpty { "{}" }).optInt(EpubContract.FIELD_POSITIONS)
            val searchStarted = SystemClock.elapsedRealtime()
            val hits = remote.search(
                Bundle().apply {
                    putString(EpubContract.KEY_QUERY, "Chapter ${book.chapters}, paragraph 12")
                    putInt(EpubContract.KEY_LIMIT, 5)
                },
            )
            val searchMs = SystemClock.elapsedRealtime() - searchStarted
            val outcome = hits.getString(EpubContract.KEY_ERROR_CODE)
                ?: "${JSONArray(hits.getString(EpubContract.KEY_RESULTS).orEmpty().ifEmpty { "[]" }).length()} hits"
            note("| ${book.label} | $open ms | $metadataMs ms ($positions positions) | $searchMs ms ($outcome) |")
        } finally {
            runCatching { remote.close() }
        }
    }

    private fun bind(): IEpubPlugin {
        val binder = serviceRule.bindService(Intent(context, ReadiumEpubReaderPluginService::class.java))
        return IEpubPlugin.Stub.asInterface(RemoteOnlyBinder(binder))
    }

    private fun IEpubBook.remote(): IEpubBook = IEpubBook.Stub.asInterface(RemoteOnlyBinder(asBinder()))

    private class RemoteOnlyBinder(private val target: android.os.IBinder) : android.os.Binder() {
        override fun queryLocalInterface(descriptor: String): android.os.IInterface? = null

        override fun onTransact(code: Int, data: android.os.Parcel, reply: android.os.Parcel?, flags: Int): Boolean =
            target.transact(code, data, reply, flags)
    }

    // ---- Measurement helpers ----

    private class Stats(val count: Int, val jankPercent: Int, val p50: String, val p90: String, val p99: String)

    /** Collects the total duration of every frame the window renders between [start] and [stop]. */
    private class FrameCollector(private val window: Window) {
        private val thread = HandlerThread("perf-frames").apply { start() }
        private val durations = ArrayList<Long>()
        private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            synchronized(durations) { durations += metrics.getMetric(FrameMetrics.TOTAL_DURATION) }
        }

        fun start() {
            window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        }

        fun stop(): Stats {
            window.removeOnFrameMetricsAvailableListener(listener)
            thread.quitSafely()
            val sorted = synchronized(durations) { durations.sorted() }
            if (sorted.isEmpty()) return Stats(0, 0, "-", "-", "-")
            fun percentile(p: Double): String = "%.1f".format(sorted[((sorted.size - 1) * p).toInt()] / 1_000_000.0)
            val jank = sorted.count { it > 16_700_000L } * 100 / sorted.size
            return Stats(sorted.size, jank, percentile(0.5), percentile(0.9), percentile(0.99))
        }
    }

    /** Samples the process PSS every 250 ms on its own thread. */
    private class PssSampler {
        private val running = AtomicBoolean(false)
        @Volatile private var peak = 0L
        private var thread: Thread? = null

        fun start() {
            running.set(true)
            thread = Thread {
                while (running.get()) {
                    peak = maxOf(peak, Debug.getPss())
                    SystemClock.sleep(250)
                }
            }.apply { start() }
        }

        fun peakKb(): Long = maxOf(peak, Debug.getPss())

        fun stop() {
            running.set(false)
            thread?.join(2_000)
        }
    }

    // ---- Book synthesis (the same shape as .python/generate_fixtures.py --perf) ----

    private fun generate(name: String, chapters: Int, images: Boolean): File {
        val file = EpubReaderTestContentProvider.fileFor(context, name)
        if (file.isFile && file.length() > 0) return file
        val started = SystemClock.elapsedRealtime()
        ZipOutputStream(BufferedOutputStream(file.outputStream(), 1 shl 16)).use { zip ->
            zip.setLevel(Deflater.DEFAULT_COMPRESSION)
            mimetypeEntry(zip)
            entry(zip, "META-INF/container.xml", CONTAINER_XML.toByteArray())
            val opf = StringBuilder(chapters * 160)
            opf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"uid\">\n")
            opf.append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:identifier id=\"uid\">urn:uuid:autojs6-readium-perf-$name</dc:identifier>")
            opf.append("<dc:title>Perf ${name.removeSuffix(".epub")}</dc:title><dc:language>en</dc:language><meta property=\"dcterms:modified\">2026-09-21T00:00:00Z</meta></metadata>\n<manifest>\n")
            opf.append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n")
            for (n in 1..chapters) {
                opf.append("<item id=\"c$n\" href=\"c/$n.xhtml\" media-type=\"application/xhtml+xml\"/>\n")
                if (images) opf.append("<item id=\"i$n\" href=\"img/$n.png\" media-type=\"image/png\"/>\n")
            }
            opf.append("</manifest>\n<spine>\n")
            for (n in 1..chapters) opf.append("<itemref idref=\"c$n\"/>\n")
            opf.append("</spine>\n</package>\n")
            entry(zip, "OEBPS/content.opf", opf.toString().toByteArray())
            val nav = StringBuilder()
            nav.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html>\n<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\"><head><title>Contents</title></head><body><nav epub:type=\"toc\"><h1>Contents</h1><ol>")
            for (n in 1..chapters) nav.append("<li><a href=\"c/$n.xhtml\">Chapter $n</a></li>")
            nav.append("</ol></nav></body></html>\n")
            entry(zip, "OEBPS/nav.xhtml", nav.toString().toByteArray())
            for (n in 1..chapters) {
                val image = if (images) "<p><img src=\"../img/$n.png\" alt=\"noise $n\"/></p>\n" else ""
                entry(zip, "OEBPS/c/$n.xhtml", chapter(n, image).toByteArray())
                if (images) {
                    zip.setLevel(Deflater.NO_COMPRESSION)
                    entry(zip, "OEBPS/img/$n.png", noisePng(n))
                    zip.setLevel(Deflater.DEFAULT_COMPRESSION)
                }
            }
        }
        note("generated $name: ${file.length()} bytes in ${SystemClock.elapsedRealtime() - started} ms")
        return file
    }

    private fun chapter(n: Int, image: String): String {
        val random = Random(n)
        val body = StringBuilder()
        body.append("<h1>Chapter $n</h1>\n").append(image)
        for (p in 1..12) {
            val sentence = (1..18).joinToString(" ") { WORDS[random.nextInt(WORDS.size)] }.replaceFirstChar { it.uppercase() } + "."
            body.append("<p>Chapter $n, paragraph $p: $sentence $sentence</p>\n")
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html>\n<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>Chapter $n</title></head><body>\n$body</body></html>\n"
    }

    /** A 256 x 256 RGB PNG of random pixels stored without compression: about 197 KB that deflate cannot shrink. */
    private fun noisePng(seed: Int): ByteArray {
        val side = 256
        val random = Random(seed)
        val raw = ByteArray(side * (1 + side * 3))
        for (row in 0 until side) {
            val offset = row * (1 + side * 3)
            raw[offset] = 0
            random.nextBytes(raw, offset + 1, offset + 1 + side * 3)
        }
        val deflater = Deflater(Deflater.NO_COMPRESSION)
        deflater.setInput(raw)
        deflater.finish()
        val compressed = ByteArray(raw.size + raw.size / 1000 + 64)
        val length = deflater.deflate(compressed)
        deflater.end()
        val out = java.io.ByteArrayOutputStream(length + 128)
        out.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
        fun chunk(kind: String, data: ByteArray) {
            val type = kind.toByteArray(Charsets.US_ASCII)
            out.write(int(data.size))
            out.write(type)
            out.write(data)
            val crc = CRC32().apply { update(type); update(data) }
            out.write(int(crc.value.toInt()))
        }
        chunk("IHDR", int(side) + int(side) + byteArrayOf(8, 2, 0, 0, 0))
        chunk("IDAT", compressed.copyOf(length))
        chunk("IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun int(value: Int): ByteArray =
        byteArrayOf((value ushr 24).toByte(), (value ushr 16).toByte(), (value ushr 8).toByte(), value.toByte())

    private fun mimetypeEntry(zip: ZipOutputStream) {
        val data = "application/epub+zip".toByteArray()
        val entry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = data.size.toLong()
            compressedSize = data.size.toLong()
            crc = CRC32().apply { update(data) }.value
        }
        zip.putNextEntry(entry)
        zip.write(data)
        zip.closeEntry()
    }

    private fun entry(zip: ZipOutputStream, name: String, data: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(data)
        zip.closeEntry()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // ---- Reader plumbing ----

    private fun request(fixture: String): Intent {
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

    private fun locator(activity: EpubReaderActivity): String? = onMain {
        (activity.supportFragmentManager.findFragmentByTag(EpubReaderActivity.NAVIGATOR_TAG) as? EpubNavigatorFragment)
            ?.takeIf { it.view != null }?.currentLocator?.value?.let { "${it.href} @ ${it.locations.progression}" }
    }

    private fun awaitOrNot(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = runCatching(condition).getOrDefault(false) }
            if (ready) return true
            SystemClock.sleep(100)
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
        const val TURNS = 20
        const val CONTAINER_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">" +
            "<rootfiles><rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles></container>\n"
        val WORDS = listOf(
            "lighthouse", "keeper", "reef", "fog", "lamp", "brass", "salt", "tide", "stair", "log", "horizon", "boat",
            "bread", "stories", "door", "wind", "cloud", "wool", "victory", "hull", "ship", "morning", "relief", "seen",
        )
    }
}
