package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import java.util.concurrent.TimeUnit
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.autojs.plugin.epub.api.IEpubBook
import org.autojs.plugin.epub.api.IEpubPlugin
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Roadmap P7.1 hostile-input matrix over the Binder (BINDER evidence): every malformed fixture of
 * docs/fixtures plus two samples generated on the device (50 000 reading-order entries, one 1 GiB
 * zero-filled resource) either opens and answers every call, or fails with a contract code; all of
 * it within the open timeout, without a crash of the plugin process and without a single byte
 * written to the plugin's storage. External entities and traversal hrefs get dedicated checks.
 * Notes land in `files/p2-evidence/hostile-*-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class HostileInputInstrumentationTest {

    @get:Rule
    val serviceRule: ServiceTestRule = ServiceTestRule.withTimeout(60, TimeUnit.SECONDS) // GitHub-hosted emulators exceed the 5 s default

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val assets get() = InstrumentationRegistry.getInstrumentation().context.assets
    private val notes = ArrayList<String>()
    private val fixtureDirectory by lazy { File(context.cacheDir, "hostile-fixtures").apply { mkdirs() } }

    private sealed class Outcome {
        class Opened(val book: IEpubBook) : Outcome()
        class Refused(val code: String, val message: String) : Outcome()
    }

    // ---- The committed hostile fixtures and what the contract may answer ----

    private val committed = linkedMapOf(
        "malformed-not-a-zip.epub" to setOf(EpubErrorCodes.NOT_EPUB),
        "malformed-empty-zip.epub" to setOf(EpubErrorCodes.NOT_EPUB, EpubErrorCodes.PARSE_FAILED),
        "malformed-missing-mimetype.epub" to setOf(OPENS, EpubErrorCodes.NOT_EPUB, EpubErrorCodes.PARSE_FAILED),
        "malformed-missing-container.epub" to setOf(EpubErrorCodes.NOT_EPUB, EpubErrorCodes.PARSE_FAILED),
        "malformed-missing-opf.epub" to setOf(EpubErrorCodes.NOT_EPUB, EpubErrorCodes.PARSE_FAILED),
        "malformed-bad-opf.epub" to setOf(EpubErrorCodes.NOT_EPUB, EpubErrorCodes.PARSE_FAILED),
        "malformed-bad-ncx.epub" to setOf(OPENS, EpubErrorCodes.PARSE_FAILED),
        "malformed-xxe.epub" to setOf(OPENS, EpubErrorCodes.PARSE_FAILED),
        "malformed-path-traversal.epub" to setOf(OPENS, EpubErrorCodes.PARSE_FAILED),
        "malformed-traversal-encoded.epub" to setOf(OPENS, EpubErrorCodes.PARSE_FAILED),
        "malformed-long-names.epub" to setOf(OPENS, EpubErrorCodes.PARSE_FAILED),
        "malformed-duplicate-entries.epub" to setOf(OPENS, EpubErrorCodes.PARSE_FAILED),
        "malformed-many-entries.epub" to setOf(OPENS),
        "malformed-high-ratio.epub" to setOf(OPENS),
        "malformed-encrypted-lcp.epub" to setOf(EpubErrorCodes.ENCRYPTED),
        "malformed-lcp-license-only.epub" to setOf(EpubErrorCodes.ENCRYPTED),
        "malformed-encrypted-adept.epub" to setOf(EpubErrorCodes.ENCRYPTED),
    )

    @Test
    fun everyCommittedHostileFixtureFailsClosedOrOpensSafely() {
        committed.keys.forEach { fixture(it) }
        val storageBefore = storageSnapshot()
        val plugin = bind()
        for ((name, allowed) in committed) {
            val started = SystemClock.elapsedRealtime()
            val outcome = tryOpen(plugin, fixture(name))
            val openMillis = SystemClock.elapsedRealtime() - started
            assertTrue("$name: opening took $openMillis ms", openMillis <= EpubContract.OPEN_TIMEOUT_MS + 5_000)
            when (outcome) {
                is Outcome.Refused -> {
                    note("$name: refused ${outcome.code} in $openMillis ms: ${outcome.message.take(160)}")
                    assertTrue("$name answered ${outcome.code}, expected one of $allowed: ${outcome.message}", outcome.code in allowed)
                }
                is Outcome.Opened -> {
                    assertTrue("$name opened, expected one of $allowed", OPENS in allowed)
                    try {
                        note("$name: opened in $openMillis ms; " + probe(outcome.book))
                    } finally {
                        outcome.book.close()
                    }
                }
            }
        }
        note("service still answers: engine " + plugin.info.engine)
        assertEquals(storageBefore, storageSnapshot())
        note("storage unchanged: ${storageBefore.size} files")
        writeNotes("hostile-service")
    }

    @Test
    fun externalEntitiesAreNeverResolved() {
        val secret = "CANARY-" + UUID.randomUUID()
        val canary = File(context.filesDir, "xxe-canary.txt").apply { writeText("$secret\n") }
        try {
            val plugin = bind()
            when (val outcome = tryOpen(plugin, fixture("malformed-xxe.epub"))) {
                is Outcome.Refused -> {
                    note("xxe: refused ${outcome.code}: ${outcome.message.take(200)}")
                    assertTrue(outcome.code, outcome.code == EpubErrorCodes.PARSE_FAILED || outcome.code == EpubErrorCodes.NOT_EPUB)
                }
                is Outcome.Opened -> {
                    val book = outcome.book
                    try {
                        val title = metadata(book).optString(EpubContract.FIELD_TITLE)
                        note("xxe: opened, title=[$title]")
                        assertFalse("the title leaked the canary", title.contains(secret))
                        val texts = readingOrder(book).indices.map { index ->
                            val answer = text(book, index = index)
                            answer.errorCode()?.let { "<$it>" } ?: answer.getString(EpubContract.KEY_TEXT).orEmpty()
                        }
                        texts.forEachIndexed { index, body -> assertFalse("entry $index leaked the canary", body.contains(secret)) }
                        note("xxe: chapter 1 text=[" + texts.firstOrNull()?.take(240)?.replace('\n', ' ') + "]")
                        note("xxe: internal entity expanded=" + texts.any { it.contains("internal-entity-expanded") })
                        val hits = search(book, secret.substring(0, 12))
                        val count = hits.errorCode()?.let { -1 } ?: JSONArray(hits.getString(EpubContract.KEY_RESULTS).orEmpty().ifEmpty { "[]" }).length()
                        note("xxe: search hits for the canary prefix: $count")
                        assertTrue("search found the canary", count <= 0)
                    } finally {
                        book.close()
                    }
                }
            }
        } finally {
            canary.delete()
        }
        writeNotes("hostile-xxe")
    }

    @Test
    fun traversalHrefsNeverLeaveTheContainer() {
        val plugin = bind()
        val hosts = runCatching { File("/etc/hosts").readText() }.getOrDefault("")
        note("device /etc/hosts readable: ${hosts.isNotBlank()}")
        for (name in listOf("malformed-path-traversal.epub", "malformed-traversal-encoded.epub")) {
            when (val outcome = tryOpen(plugin, fixture(name))) {
                is Outcome.Refused -> note("$name: refused ${outcome.code}: ${outcome.message.take(160)}")
                is Outcome.Opened -> {
                    val book = outcome.book
                    try {
                        val order = readingOrder(book)
                        note("$name: reading order " + order.joinToString(" | ") { it.optString(EpubContract.FIELD_HREF) })
                        for ((index, entry) in order.withIndex()) {
                            val href = entry.optString(EpubContract.FIELD_HREF)
                            val answer = text(book, index = index)
                            val code = answer.errorCode()
                            if (code == null) {
                                val body = answer.getString(EpubContract.KEY_TEXT).orEmpty()
                                assertFalse("$name entry [$href] served /etc/hosts", hosts.isNotBlank() && body.contains(hosts.trim()))
                                assertFalse("$name entry [$href] served /etc/hosts", body.contains("localhost"))
                            }
                            note("$name text[$index] [$href] -> ${code ?: "ok"}")
                        }
                        for (href in HOSTILE_HREFS) {
                            val served = runCatching { book.openResource(href).use { readAll(it) } }
                            val code = served.exceptionOrNull()?.let { EpubErrorCodes.decode(it.message.orEmpty())?.first ?: "uncoded ${it.javaClass.simpleName}" }
                            served.getOrNull()?.let { bytes ->
                                val body = String(bytes)
                                assertFalse("$name openResource([$href]) served /etc/hosts", body.contains("localhost"))
                                assertTrue("$name openResource([$href]) served ${bytes.size} bytes", bytes.size < 64 * 1024)
                            }
                            assertTrue("$name openResource([$href]) -> $code", code == null || code in CONTRACT_REFUSALS)
                            val textCode = text(book, href = href).errorCode()
                            assertTrue("$name getText([$href]) -> $textCode", textCode == null || textCode in CONTRACT_REFUSALS)
                            note("$name openResource([$href]) -> ${code ?: "ok ${served.getOrNull()?.size} bytes"}; getText -> ${textCode ?: "ok"}")
                        }
                    } finally {
                        book.close()
                    }
                }
            }
        }
        writeNotes("hostile-traversal")
    }

    @Test
    fun fiftyThousandEntriesStayWithinTheOpenTimeout() {
        val file = generateManyEntries(50_000)
        note("generated ${file.name}: ${file.length()} bytes")
        val storageBefore = storageSnapshot()
        val plugin = bind()
        val started = SystemClock.elapsedRealtime()
        val outcome = tryOpen(plugin, file)
        val openMillis = SystemClock.elapsedRealtime() - started
        assertTrue("opening took $openMillis ms", openMillis <= EpubContract.OPEN_TIMEOUT_MS + 5_000)
        when (outcome) {
            is Outcome.Refused -> {
                note("50 000 entries: refused ${outcome.code} after $openMillis ms: ${outcome.message.take(160)}")
                assertTrue(outcome.code, outcome.code == EpubErrorCodes.TIMEOUT || outcome.code == EpubErrorCodes.PARSE_FAILED)
            }
            is Outcome.Opened -> {
                val book = outcome.book
                try {
                    note("50 000 entries: opened in $openMillis ms")
                    note("readingOrder: starting")
                    val orderStarted = SystemClock.elapsedRealtime()
                    val order = book.readingOrder.requireOk("readingOrder")
                    val entries = JSONArray(order.getString(EpubContract.KEY_READING_ORDER))
                    note("readingOrder: ${entries.length()} entries, hasMore=${order.getBoolean(EpubContract.KEY_HAS_MORE)} in ${SystemClock.elapsedRealtime() - orderStarted} ms")
                    assertEquals(EpubContract.MAX_READING_ORDER_ENTRIES, entries.length())
                    assertTrue(order.getBoolean(EpubContract.KEY_HAS_MORE))
                    note("metadata: starting")
                    val metadataStarted = SystemClock.elapsedRealtime()
                    val metadata = metadata(book)
                    note("metadata: positions=${metadata.optInt(EpubContract.FIELD_POSITIONS)} in ${SystemClock.elapsedRealtime() - metadataStarted} ms")
                    note("text[49999]: starting")
                    val last = text(book, index = 49_999)
                    note("text[49999] -> ${last.errorCode() ?: "ok " + last.getString(EpubContract.KEY_TEXT).orEmpty().take(40)}")
                    assertTrue(last.errorCode() == null)
                    val beyond = text(book, index = 50_000)
                    assertEquals(EpubErrorCodes.RESOURCE_NOT_FOUND, beyond.errorCode())
                    note("search: starting (a query that only the last resource matches)")
                    val searchStarted = SystemClock.elapsedRealtime()
                    val hits = search(book, "filler 49999")
                    val searchMillis = SystemClock.elapsedRealtime() - searchStarted
                    note("search: ${hits.errorCode() ?: "ok " + JSONArray(hits.getString(EpubContract.KEY_RESULTS).orEmpty().ifEmpty { "[]" }).length() + " hits"} in $searchMillis ms")
                    // The plugin answers before the host's own deadline (EpubContract.CALL_TIMEOUT_MS): the hits or TIMEOUT.
                    assertTrue("search took $searchMillis ms", searchMillis <= EpubContract.CALL_TIMEOUT_MS)
                    assertTrue(hits.errorCode().toString(), hits.errorCode() == null || hits.errorCode() == EpubErrorCodes.TIMEOUT)
                } finally {
                    book.close()
                }
            }
        }
        assertEquals(storageBefore, storageSnapshot())
        writeNotes("hostile-many")
    }

    @Test
    fun aOneGibibyteZeroFilledResourceStaysBounded() {
        val file = generateHighRatio(1L shl 30)
        note("generated ${file.name}: ${file.length()} bytes (1 GiB of zeros inside)")
        val plugin = bind()
        val heapBefore = usedHeap()
        val nativeBefore = Debug.getNativeHeapAllocatedSize()
        val started = SystemClock.elapsedRealtime()
        val outcome = tryOpen(plugin, file)
        val openMillis = SystemClock.elapsedRealtime() - started
        assertTrue("opening took $openMillis ms", openMillis <= EpubContract.OPEN_TIMEOUT_MS + 5_000)
        when (outcome) {
            is Outcome.Refused -> {
                note("1 GiB blob: refused ${outcome.code} after $openMillis ms: ${outcome.message.take(160)}")
                assertTrue(outcome.code, outcome.code == EpubErrorCodes.TIMEOUT || outcome.code == EpubErrorCodes.PARSE_FAILED)
            }
            is Outcome.Opened -> {
                val book = outcome.book
                try {
                    note("1 GiB blob: opened in $openMillis ms; " + probe(book))
                    val resource = runCatching { book.openResource("OEBPS/blob.bin").use { readAll(it).size } }
                    val code = resource.exceptionOrNull()?.let { EpubErrorCodes.decode(it.message.orEmpty())?.first ?: "uncoded ${it.javaClass.simpleName}" }
                    note("openResource(OEBPS/blob.bin) -> ${code ?: "ok ${resource.getOrNull()} bytes"}")
                    assertEquals(EpubErrorCodes.LIMIT_EXCEEDED, code)
                    val asText = text(book, href = "OEBPS/blob.bin", maxChars = 4096)
                    note("getText(OEBPS/blob.bin) -> ${asText.errorCode() ?: "ok " + asText.getString(EpubContract.KEY_TEXT).orEmpty().length + " chars"}")
                } finally {
                    book.close()
                }
            }
        }
        val heapGrowth = usedHeap() - heapBefore
        val nativeGrowth = Debug.getNativeHeapAllocatedSize() - nativeBefore
        note("heap growth ${heapGrowth / 1024} KiB, native growth ${nativeGrowth / 1024} KiB")
        assertTrue("heap grew by ${heapGrowth / 1024} KiB", heapGrowth < 128L * 1024 * 1024)
        assertTrue("native heap grew by ${nativeGrowth / 1024} KiB", nativeGrowth < 128L * 1024 * 1024)
        writeNotes("hostile-blob")
    }

    // ---- Helpers ----

    private fun fixture(name: String): File {
        val file = File(fixtureDirectory, name)
        if (!file.exists()) assets.open(name).use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }

    private fun bind(): IEpubPlugin {
        val binder = serviceRule.bindService(Intent(context, ReadiumEpubReaderPluginService::class.java))
        return IEpubPlugin.Stub.asInterface(RemoteOnlyBinder(binder))
    }

    private fun IEpubBook.remote(): IEpubBook = IEpubBook.Stub.asInterface(RemoteOnlyBinder(asBinder()))

    private fun tryOpen(plugin: IEpubPlugin, file: File): Outcome = try {
        val book = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use {
            plugin.openBook(it, Bundle().apply { putInt(EpubContract.KEY_CONTRACT_VERSION, EpubContract.CONTRACT_VERSION) }).remote()
        }
        Outcome.Opened(book)
    } catch (e: RuntimeException) {
        val decoded = EpubErrorCodes.decode(e.message.orEmpty())
            ?: throw AssertionError("${file.name}: not a contract failure: $e", e)
        assertTrue("${file.name}: ${e.javaClass.simpleName}", e is IllegalArgumentException || e is IllegalStateException)
        Outcome.Refused(decoded.first, e.message.orEmpty())
    }

    /** Exercises every read call of an opened book; each one must answer ok or with a contract code. */
    private fun probe(book: IEpubBook): String {
        val parts = ArrayList<String>()
        val metadata = book.metadata
        parts += "metadata " + (metadata.errorCode() ?: "ok title=[" + JSONObject(metadata.getString(EpubContract.KEY_METADATA).orEmpty().ifEmpty { "{}" }).optString(EpubContract.FIELD_TITLE) + "] positions=" +
            JSONObject(metadata.getString(EpubContract.KEY_METADATA).orEmpty().ifEmpty { "{}" }).optInt(EpubContract.FIELD_POSITIONS))
        val toc = book.toc
        parts += "toc " + (toc.errorCode() ?: "ok " + JSONArray(toc.getString(EpubContract.KEY_TOC).orEmpty().ifEmpty { "[]" }).length())
        val order = book.readingOrder
        val entries = if (order.errorCode() == null) JSONArray(order.getString(EpubContract.KEY_READING_ORDER).orEmpty().ifEmpty { "[]" }) else JSONArray()
        parts += "readingOrder " + (order.errorCode() ?: "ok ${entries.length()}")
        val texts = (0 until minOf(entries.length(), 6)).map { index ->
            val answer = text(book, index = index)
            answer.errorCode() ?: "ok ${answer.getString(EpubContract.KEY_TEXT).orEmpty().length}"
        }
        parts += "text " + texts.joinToString(",")
        val positions = book.positions
        parts += "positions " + (positions.errorCode() ?: "ok " + positions.getInt(EpubContract.KEY_POSITIONS))
        val hits = search(book, "lighthouse", limit = 5)
        parts += "search " + (hits.errorCode() ?: "ok " + JSONArray(hits.getString(EpubContract.KEY_RESULTS).orEmpty().ifEmpty { "[]" }).length())
        listOf(metadata, toc, order, positions, hits).forEach { answer ->
            assertEquals(EpubContract.CONTRACT_VERSION, answer.getInt(EpubContract.KEY_CONTRACT_VERSION))
            answer.errorCode()?.let { assertTrue("unknown code $it", EpubErrorCodes.isKnown(it)) }
        }
        return parts.joinToString("; ")
    }

    private fun metadata(book: IEpubBook): JSONObject =
        JSONObject(book.metadata.requireOk("metadata").getString(EpubContract.KEY_METADATA).orEmpty())

    private fun readingOrder(book: IEpubBook): List<JSONObject> {
        val array = JSONArray(book.readingOrder.requireOk("readingOrder").getString(EpubContract.KEY_READING_ORDER).orEmpty())
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private fun Bundle.errorCode(): String? = getString(EpubContract.KEY_ERROR_CODE)

    private fun Bundle.requireOk(what: String): Bundle {
        assertEquals(EpubContract.CONTRACT_VERSION, getInt(EpubContract.KEY_CONTRACT_VERSION))
        val code = errorCode()
        assertTrue("$what: $code ${getString(EpubContract.KEY_ERROR_MESSAGE)}", code == null)
        return this
    }

    private fun text(book: IEpubBook, index: Int? = null, href: String? = null, maxChars: Int = 0): Bundle =
        book.getText(
            Bundle().apply {
                index?.let { putInt(EpubContract.KEY_INDEX, it) }
                href?.let { putString(EpubContract.KEY_HREF, it) }
                putInt(EpubContract.KEY_MAX_CHARS, maxChars)
            },
        )

    private fun search(book: IEpubBook, query: String, limit: Int = 0): Bundle =
        book.search(
            Bundle().apply {
                putString(EpubContract.KEY_QUERY, query)
                putInt(EpubContract.KEY_LIMIT, limit)
            },
        )

    private fun readAll(descriptor: ParcelFileDescriptor): ByteArray =
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }

    private fun usedHeap(): Long {
        Runtime.getRuntime().gc()
        return Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
    }

    /** Every file under the plugin's private storage, minus what this test writes itself. */
    private fun storageSnapshot(): List<String> {
        val roots = listOfNotNull(context.filesDir, context.cacheDir, context.noBackupFilesDir, context.getExternalFilesDir(null))
        return roots.flatMap { root ->
            root.walkTopDown().filter { it.isFile }.map { root.name + "/" + it.relativeTo(root).path.replace('\\', '/') + ":" + it.length() }.toList()
        }.filterNot {
            // ART writes files/profileInstalled on the first launch after an install, and the system WebView keeps writing its
            // HTTP and code caches for the reader tests that ran earlier in this process; neither is the book's doing.
            it.contains("hostile-fixtures/") || it.contains("p2-evidence/") || it.endsWith("xxe-canary.txt") || it.startsWith("files/profileInstalled:") ||
                it.startsWith("cache/WebView/")
        }
            .sorted()
    }

    private fun zipEntry(zip: ZipOutputStream, name: String, data: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(data)
        zip.closeEntry()
    }

    private fun mimetypeEntry(zip: ZipOutputStream) {
        val bytes = "application/epub+zip".toByteArray()
        zip.putNextEntry(
            ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = bytes.size.toLong()
                compressedSize = bytes.size.toLong()
                crc = CRC32().apply { update(bytes) }.value
            },
        )
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun chapter(title: String, body: String): ByteArray = (
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html>\n<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
            "<head><title>$title</title></head><body><h1>$title</h1><p>$body</p></body></html>\n"
        ).toByteArray()

    private fun generateManyEntries(count: Int): File {
        val file = File(fixtureDirectory, "generated-many-$count.epub")
        if (file.exists()) return file
        ZipOutputStream(BufferedOutputStream(file.outputStream(), 1 shl 16)).use { zip ->
            mimetypeEntry(zip)
            zipEntry(zip, "META-INF/container.xml", CONTAINER_XML.toByteArray())
            val opf = StringBuilder(count * 120)
            opf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"uid\">\n")
            opf.append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:identifier id=\"uid\">urn:uuid:hostile-many-$count</dc:identifier>")
            opf.append("<dc:title>Many entries ($count)</dc:title><dc:language>en</dc:language><meta property=\"dcterms:modified\">2026-09-21T00:00:00Z</meta></metadata>\n<manifest>\n")
            opf.append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n")
            for (i in 0 until count) opf.append("<item id=\"f$i\" href=\"f/$i.xhtml\" media-type=\"application/xhtml+xml\"/>\n")
            opf.append("</manifest>\n<spine>\n")
            for (i in 0 until count) opf.append("<itemref idref=\"f$i\"/>\n")
            opf.append("</spine>\n</package>\n")
            zipEntry(zip, "OEBPS/content.opf", opf.toString().toByteArray())
            zipEntry(
                zip,
                "OEBPS/nav.xhtml",
                ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html>\n<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\">" +
                    "<head><title>Contents</title></head><body><nav epub:type=\"toc\"><ol><li><a href=\"f/0.xhtml\">Filler 0</a></li></ol></nav></body></html>\n").toByteArray(),
            )
            for (i in 0 until count) zipEntry(zip, "OEBPS/f/$i.xhtml", chapter("Filler $i", "filler $i"))
        }
        return file
    }

    private fun generateHighRatio(bytes: Long): File {
        val file = File(fixtureDirectory, "generated-high-ratio-${bytes shr 20}m.epub")
        if (file.exists()) return file
        ZipOutputStream(BufferedOutputStream(file.outputStream(), 1 shl 16)).use { zip ->
            mimetypeEntry(zip)
            zipEntry(zip, "META-INF/container.xml", CONTAINER_XML.toByteArray())
            zipEntry(
                zip,
                "OEBPS/content.opf",
                ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"uid\">\n" +
                    "<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:identifier id=\"uid\">urn:uuid:hostile-blob</dc:identifier>" +
                    "<dc:title>High ratio (1 GiB)</dc:title><dc:language>en</dc:language><meta property=\"dcterms:modified\">2026-09-21T00:00:00Z</meta></metadata>\n" +
                    "<manifest><item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>" +
                    "<item id=\"c1\" href=\"chapter1.xhtml\" media-type=\"application/xhtml+xml\"/>" +
                    "<item id=\"blob\" href=\"blob.bin\" media-type=\"application/octet-stream\"/></manifest>\n" +
                    "<spine><itemref idref=\"c1\"/></spine>\n</package>\n").toByteArray(),
            )
            zipEntry(
                zip,
                "OEBPS/nav.xhtml",
                ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html>\n<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\">" +
                    "<head><title>Contents</title></head><body><nav epub:type=\"toc\"><ol><li><a href=\"chapter1.xhtml\">Chapter 1</a></li></ol></nav></body></html>\n").toByteArray(),
            )
            zipEntry(zip, "OEBPS/chapter1.xhtml", chapter("Chapter 1", "The lighthouse keeper counted the ships."))
            zip.putNextEntry(ZipEntry("OEBPS/blob.bin"))
            val zeros = ByteArray(1 shl 20)
            var written = 0L
            while (written < bytes) {
                zip.write(zeros)
                written += zeros.size
            }
            zip.closeEntry()
        }
        return file
    }

    /** Every note also lands in the progress file at once, so a hung call still leaves its trail. */
    private fun note(line: String) {
        notes += line
        progressFile.appendText("${SystemClock.elapsedRealtime() % 100_000_000} $line\n")
    }

    private val progressFile by lazy {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }
            .resolve("hostile-progress-api${Build.VERSION.SDK_INT}.txt")
            .apply { writeText("") }
    }

    private fun writeNotes(name: String) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }
            .resolve("$name-api${Build.VERSION.SDK_INT}.txt")
            .writeText(notes.joinToString("\n") + "\n")
    }

    private class RemoteOnlyBinder(private val target: IBinder) : Binder() {
        override fun queryLocalInterface(descriptor: String): IInterface? = null

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            target.transact(code, data, reply, flags)
    }

    private companion object {
        const val OPENS = "opens"

        val CONTRACT_REFUSALS = setOf(
            EpubErrorCodes.INVALID_ARGUMENT,
            EpubErrorCodes.RESOURCE_NOT_FOUND,
            EpubErrorCodes.LIMIT_EXCEEDED,
            EpubErrorCodes.IO,
            EpubErrorCodes.PARSE_FAILED,
        )

        val HOSTILE_HREFS = listOf(
            "../escaped.txt",
            "../../../etc/passwd",
            "%2e%2e/%2e%2e/escaped.txt",
            "/etc/hosts",
            "file:///etc/hosts",
            "..\\..\\escaped.txt",
            "OEBPS/../../etc/hosts",
            "content://io.github.supermonster003.autojs6.plugin.readium.epub.reader.test.documents/documents/x",
        )

        const val CONTAINER_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">" +
            "<rootfiles><rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles></container>\n"
    }
}
