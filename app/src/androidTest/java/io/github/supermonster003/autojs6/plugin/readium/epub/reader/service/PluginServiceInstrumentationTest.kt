package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationDatabase
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationStyle
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.BookAnnotation
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookFingerprint
import java.util.concurrent.TimeUnit
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.BuildConfig
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.ReadiumEpubReaderPlugin
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.readiumEpubReaderPluginInfo
import kotlinx.coroutines.runBlocking
import org.autojs.plugin.epub.api.EpubCapabilityKeys
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.autojs.plugin.epub.api.EpubIds
import org.autojs.plugin.epub.api.IEpubBook
import org.autojs.plugin.epub.api.IEpubPlugin
import org.json.JSONArray
import org.json.JSONObject
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
 * Roadmap P5.2 device evidence: the `org.autojs.plugin.EPUB` service through the generated
 * AIDL Proxy / Parcel path (a remote-only wrapper hides the local binder, so every call is
 * marshalled like the host's), on the fixtures of docs/fixtures. Notes land in
 * `files/p2-evidence/service-api<N>.txt`.
 */
@RunWith(AndroidJUnit4::class)
class PluginServiceInstrumentationTest {

    @get:Rule
    val serviceRule: ServiceTestRule = ServiceTestRule.withTimeout(60, TimeUnit.SECONDS) // GitHub-hosted emulators exceed the 5 s default

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val assets get() = InstrumentationRegistry.getInstrumentation().context.assets
    private val notes = ArrayList<String>()

    private fun fixture(name: String): File {
        val file = File(context.cacheDir, "service-$name")
        assets.open(name).use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }

    private fun descriptor(name: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(fixture(name), ParcelFileDescriptor.MODE_READ_ONLY)

    private fun bind(): IEpubPlugin {
        val binder = serviceRule.bindService(Intent(context, ReadiumEpubReaderPluginService::class.java))
        return IEpubPlugin.Stub.asInterface(RemoteOnlyBinder(binder))
    }

    private fun IEpubBook.remote(): IEpubBook = IEpubBook.Stub.asInterface(RemoteOnlyBinder(asBinder()))

    /** The options a version 2 host writes (roadmap P9.4); [openLegacy] plays a version 1 host. */
    private fun options(build: Bundle.() -> Unit = {}): Bundle = Bundle().apply {
        putInt(EpubContract.KEY_CONTRACT_VERSION, EpubContract.CONTRACT_VERSION)
        build()
    }

    private fun open(plugin: IEpubPlugin, name: String, options: Bundle = options()): IEpubBook =
        descriptor(name).use { plugin.openBook(it, options).remote() }

    /** A host of contract version 1 (6.8.0 build 5282): no version key at all. */
    private fun openLegacy(plugin: IEpubPlugin, name: String): IEpubBook = open(plugin, name, Bundle())

    private fun Bundle.errorCode(): String? = getString(EpubContract.KEY_ERROR_CODE)

    private fun Bundle.requireOk(what: String, version: Int = EpubContract.CONTRACT_VERSION): Bundle {
        assertEquals(what, version, getInt(EpubContract.KEY_CONTRACT_VERSION))
        assertNull("$what: ${getString(EpubContract.KEY_ERROR_MESSAGE)}", errorCode())
        return this
    }

    private fun Bundle.requireError(what: String, code: String, version: Int = EpubContract.CONTRACT_VERSION): Bundle {
        assertEquals(what, version, getInt(EpubContract.KEY_CONTRACT_VERSION))
        assertEquals(what, code, errorCode())
        assertTrue(what, getString(EpubContract.KEY_ERROR_MESSAGE).orEmpty().toByteArray().size <= EpubContract.MAX_ERROR_MESSAGE_BYTES)
        return this
    }

    private fun text(book: IEpubBook, index: Int? = null, href: String? = null, offset: Int = 0, maxChars: Int = 0, format: String? = null): Bundle =
        book.getText(
            Bundle().apply {
                index?.let { putInt(EpubContract.KEY_INDEX, it) }
                href?.let { putString(EpubContract.KEY_HREF, it) }
                putInt(EpubContract.KEY_OFFSET, offset)
                putInt(EpubContract.KEY_MAX_CHARS, maxChars)
                format?.let { putString(EpubContract.KEY_FORMAT, it) }
            },
        )

    private fun search(book: IEpubBook, query: String?, offset: Int = 0, limit: Int = 0): Bundle =
        book.search(
            Bundle().apply {
                putString(EpubContract.KEY_QUERY, query)
                putInt(EpubContract.KEY_OFFSET, offset)
                putInt(EpubContract.KEY_LIMIT, limit)
            },
        )

    private fun annotations(book: IEpubBook, offset: Int = 0, limit: Int = 0): Bundle =
        book.getAnnotations(
            Bundle().apply {
                putInt(EpubContract.KEY_OFFSET, offset)
                putInt(EpubContract.KEY_LIMIT, limit)
            },
        )

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

    private fun note(line: String) {
        notes += line
    }

    private fun writeNotes(name: String) {
        File(context.filesDir, "p2-evidence").apply { mkdirs() }
            .resolve("$name-api${Build.VERSION.SDK_INT}.txt")
            .writeText(notes.joinToString("\n") + "\n")
    }

    @Test
    fun identityAndCapabilitiesMatchTheExplorerServiceAndTheContract() {
        val plugin = bind()
        val explorer = context.readiumEpubReaderPluginInfo()

        val info = plugin.info
        assertEquals(EpubIds.ENGINE, info.engine)
        assertEquals(EpubIds.PLUGIN_ID, info.id)
        assertEquals(EpubIds.VARIANT_DEFAULT, info.variant)
        assertEquals(explorer.id, info.id)
        assertEquals(explorer.variant, info.variant)
        assertEquals(explorer.versionName, info.versionName)
        assertEquals(explorer.versionCode, info.versionCode)
        assertEquals(explorer.name, info.name)

        val capabilities = plugin.capabilities
        assertEquals(EpubContract.MIN_CONTRACT_VERSION, capabilities.getInt(EpubCapabilityKeys.CONTRACT_VERSION))
        assertEquals(EpubContract.MAX_CONTRACT_VERSION, capabilities.getInt(EpubCapabilityKeys.MAX_CONTRACT_VERSION))
        assertEquals(EpubIds.REQUIRED_HOST_VERSION_CODE, capabilities.getLong(EpubCapabilityKeys.REQUIRES_HOST_VERSION))
        assertEquals(ReadiumEpubReaderPlugin.EPUB_FEATURES, capabilities.getStringArray(EpubCapabilityKeys.FEATURES)?.toList())
        assertEquals(BuildConfig.READIUM_VERSION, capabilities.getString(EpubCapabilityKeys.READIUM_VERSION))
        assertEquals(capabilities.keySet(), info.capabilities?.keySet())
        assertTrue(EpubContract.supportsContractVersion(capabilities.getInt(EpubCapabilityKeys.CONTRACT_VERSION)))
    }

    @Test
    fun bookRoundTripOnTheMinimalEpub3() {
        val plugin = bind()
        val baseline = openDescriptors()
        note("device api=${Build.VERSION.SDK_INT} readium=${BuildConfig.READIUM_VERSION} descriptors=$baseline")
        val book = open(plugin, "minimal-epub3.epub", options { putString(EpubContract.KEY_DISPLAY_NAME, "minimal-epub3.epub") })

        // Metadata.
        val metadata = JSONObject(book.metadata.requireOk("metadata").getString(EpubContract.KEY_METADATA).orEmpty())
        assertEquals("Minimal EPUB 3", metadata.getString(EpubContract.FIELD_TITLE))
        assertEquals(EpubContract.LAYOUT_REFLOWABLE, metadata.getString(EpubContract.FIELD_LAYOUT))
        assertTrue(metadata.getJSONArray(EpubContract.FIELD_AUTHORS).length() >= 0)
        assertTrue(metadata.getString(EpubContract.FIELD_READING_PROGRESSION) in listOf(EpubContract.READING_PROGRESSION_LTR, EpubContract.READING_PROGRESSION_AUTO))
        note("metadata: $metadata")

        // Table of contents and reading order.
        val toc = book.toc.requireOk("toc")
        val tocRows = JSONArray(toc.getString(EpubContract.KEY_TOC).orEmpty())
        assertTrue("toc rows: ${tocRows.length()}", tocRows.length() >= 3)
        assertFalse(toc.getBoolean(EpubContract.KEY_HAS_MORE))
        val depths = (0 until tocRows.length()).map { tocRows.getJSONObject(it).getInt(EpubContract.FIELD_DEPTH) }
        assertEquals(0, depths.first())
        assertTrue("nested toc: $depths", depths.any { it > 0 })
        assertTrue((0 until tocRows.length()).all { tocRows.getJSONObject(it).getString(EpubContract.FIELD_HREF).isNotBlank() })
        note("toc: $tocRows")

        val order = book.readingOrder.requireOk("readingOrder")
        val spine = JSONArray(order.getString(EpubContract.KEY_READING_ORDER).orEmpty())
        assertEquals(3, spine.length())
        assertFalse(order.getBoolean(EpubContract.KEY_HAS_MORE))
        val hrefs = (0 until spine.length()).map { spine.getJSONObject(it).getString(EpubContract.FIELD_HREF) }
        assertEquals(hrefs.distinct(), hrefs)
        assertTrue((0 until spine.length()).all { spine.getJSONObject(it).getString(EpubContract.FIELD_TYPE).contains("xhtml") })
        note("readingOrder: $spine")

        // Chapter text: by index, by href (with and without a fragment), paged in small windows.
        val full = text(book, index = 0).requireOk("text 0")
        val fullText = full.getString(EpubContract.KEY_TEXT).orEmpty()
        assertTrue(fullText.isNotBlank())
        assertFalse(full.getBoolean(EpubContract.KEY_HAS_MORE))
        assertEquals(0, full.getInt(EpubContract.KEY_INDEX))
        assertEquals(hrefs[0], full.getString(EpubContract.KEY_HREF))
        assertEquals(EpubContract.FORMAT_TEXT, full.getString(EpubContract.KEY_FORMAT))
        assertEquals(fullText, text(book, href = hrefs[0]).requireOk("text by href").getString(EpubContract.KEY_TEXT))
        assertEquals(fullText, text(book, href = hrefs[0] + "#anything").requireOk("text by href+fragment").getString(EpubContract.KEY_TEXT))

        val pages = StringBuilder()
        var offset = 0
        var calls = 0
        while (true) {
            val page = text(book, index = 0, offset = offset, maxChars = 7).requireOk("page $offset")
            calls++
            assertEquals(offset, page.getInt(EpubContract.KEY_OFFSET))
            val chunk = page.getString(EpubContract.KEY_TEXT).orEmpty()
            assertTrue(chunk.length <= 7)
            pages.append(chunk)
            offset += chunk.length
            if (!page.getBoolean(EpubContract.KEY_HAS_MORE)) break
            assertTrue(calls < 100_000)
        }
        assertEquals(fullText, pages.toString())
        val beyond = text(book, index = 0, offset = fullText.length + 10, maxChars = 5).requireOk("beyond")
        assertEquals("", beyond.getString(EpubContract.KEY_TEXT))
        assertFalse(beyond.getBoolean(EpubContract.KEY_HAS_MORE))
        note("text[0]: ${fullText.length} chars in $calls pages of 7; first line: ${fullText.lineSequence().first()}")

        val allText = hrefs.indices.joinToString("\n") { text(book, index = it).requireOk("text $it").getString(EpubContract.KEY_TEXT).orEmpty() }
        assertTrue(allText.contains("lighthouse", ignoreCase = true))
        val allMarkdown = hrefs.indices.joinToString("\n\n") {
            text(book, index = it, format = EpubContract.FORMAT_MARKDOWN).requireOk("markdown $it").getString(EpubContract.KEY_TEXT).orEmpty()
        }
        assertTrue("markdown headings: $allMarkdown", Regex("(?m)^#{1,6} \\S").containsMatchIn(allMarkdown))
        assertTrue("markdown image: $allMarkdown", allMarkdown.contains("beacon.png)"))
        assertTrue(allMarkdown.lineSequence().any { it.startsWith("![") })
        assertFalse(allText.contains("beacon.png"))
        note("markdown image line: ${allMarkdown.lineSequence().firstOrNull { it.startsWith("![") }}")

        // Resource export through a reliable pipe.
        val resource = book.openResource(hrefs[0])
        val bytes = ParcelFileDescriptor.AutoCloseInputStream(resource).use { it.readBytes() }
        assertTrue(bytes.isNotEmpty())
        assertTrue(String(bytes, Charsets.UTF_8).contains("<html", ignoreCase = true))
        note("openResource(${hrefs[0]}): ${bytes.size} bytes")
        val imageHref = Regex("!\\[[^\\]]*]\\(([^)]+)\\)").find(allMarkdown)?.groupValues?.get(1)
        assertNotNull(imageHref)
        val imagePfd = book.openResource(imageHref!!)
        val image = ParcelFileDescriptor.AutoCloseInputStream(imagePfd).use { it.readBytes() }
        assertTrue(image.size > 8)
        assertEquals(listOf(0x89, 0x50, 0x4E, 0x47), image.take(4).map { it.toInt() and 0xFF })
        note("openResource($imageHref): ${image.size} bytes, PNG signature ok")

        // Search: hits carry href, locator and highlight; offset + limit page the same hit list.
        val hits = search(book, "lighthouse").requireOk("search")
        val results = JSONArray(hits.getString(EpubContract.KEY_RESULTS).orEmpty())
        assertTrue("hits: ${results.length()}", results.length() >= 1)
        assertFalse(hits.getBoolean(EpubContract.KEY_HAS_MORE))
        assertEquals("lighthouse", hits.getString(EpubContract.KEY_QUERY))
        for (i in 0 until results.length()) {
            val hit = results.getJSONObject(i)
            assertTrue(hit.getString(EpubContract.FIELD_HREF) in hrefs.map { it.substringBefore('#') } || hit.getString(EpubContract.FIELD_HREF).isNotBlank())
            assertEquals("lighthouse", hit.getJSONObject(EpubContract.FIELD_TEXT).getString(EpubContract.FIELD_HIGHLIGHT).lowercase())
            assertTrue(hit.getJSONObject(EpubContract.FIELD_LOCATOR).has("href"))
        }
        if (results.length() >= 2) {
            val first = JSONArray(search(book, "lighthouse", offset = 0, limit = 1).requireOk("page 1").getString(EpubContract.KEY_RESULTS).orEmpty())
            val second = JSONArray(search(book, "lighthouse", offset = 1, limit = 1).requireOk("page 2").getString(EpubContract.KEY_RESULTS).orEmpty())
            assertEquals(1, first.length())
            assertEquals(1, second.length())
            assertEquals(results.getJSONObject(0).toString(), first.getJSONObject(0).toString())
            assertEquals(results.getJSONObject(1).toString(), second.getJSONObject(0).toString())
        }
        val reef = JSONArray(search(book, "reef").requireOk("search reef").getString(EpubContract.KEY_RESULTS).orEmpty())
        assertTrue(reef.length() >= 1)
        val none = JSONArray(search(book, "zxqv-not-in-the-book").requireOk("search none").getString(EpubContract.KEY_RESULTS).orEmpty())
        assertEquals(0, none.length())
        note("search lighthouse: ${results.length()} hits; reef: ${reef.length()} hits")

        // Positions.
        val positions = book.positions.requireOk("positions").getInt(EpubContract.KEY_POSITIONS)
        assertTrue("positions: $positions", positions >= 3)
        assertEquals(positions, metadata.getInt(EpubContract.FIELD_POSITIONS))
        note("positions: $positions")

        // Close: idempotent, and every later call answers SESSION_CLOSED.
        book.close()
        book.close()
        book.metadata.requireError("closed metadata", EpubErrorCodes.SESSION_CLOSED)
        text(book, index = 0).requireError("closed text", EpubErrorCodes.SESSION_CLOSED)
        failure<IllegalStateException>(EpubErrorCodes.SESSION_CLOSED) { book.openResource(hrefs[0]) }

        val after = awaitDescriptors(baseline)
        note("descriptors after close: $after (baseline $baseline)")
        assertTrue("descriptors leaked: $baseline -> $after", after <= baseline)
        writeNotes("service")
    }

    /**
     * Roadmap P9.4 (contract version 2): a book opened by a version 1 host answers with version 1
     * and refuses `getAnnotations`; a version 2 book pages the reader's highlights of the same
     * file (stored under its full fingerprint) in reading order, echoes the offset, flags the
     * remainder and refuses an oversized page.
     */
    @Test
    fun annotationsFollowTheNegotiatedVersion() {
        val plugin = bind()
        val database = AnnotationDatabase.get(context)
        val dao = database.annotations()
        runBlocking { dao.deleteEverything() }
        val file = fixture("minimal-epub3.epub")
        val key = file.inputStream().use { BookFingerprint.fullKey(it.channel) }
        try {
            val legacy = openLegacy(plugin, "minimal-epub3.epub")
            legacy.metadata.requireOk("legacy metadata", version = 1)
            legacy.positions.requireOk("legacy positions", version = 1)
            annotations(legacy).requireError("legacy getAnnotations", EpubErrorCodes.INVALID_ARGUMENT, version = 1)
            legacy.close()
            note("version 1 host: answers stamped 1, getAnnotations refused with INVALID_ARGUMENT")

            val book = open(plugin, "minimal-epub3.epub")
            book.metadata.requireOk("v2 metadata")
            val empty = annotations(book).requireOk("empty page")
            assertEquals(0, JSONArray(empty.getString(EpubContract.KEY_ANNOTATIONS)).length())
            assertFalse(empty.getBoolean(EpubContract.KEY_HAS_MORE))

            fun locator(href: String, progression: Double, quote: String) = JSONObject()
                .put(EpubContract.FIELD_HREF, href)
                .put(EpubContract.FIELD_TYPE, "application/xhtml+xml")
                .put(EpubContract.FIELD_LOCATIONS, JSONObject().put(EpubContract.FIELD_PROGRESSION, progression))
                .put(EpubContract.FIELD_TEXT, JSONObject().put(EpubContract.FIELD_HIGHLIGHT, quote))
                .toString()
            runBlocking {
                // Inserted out of reading order on purpose; the third chapter first.
                dao.insert(BookAnnotation(bookKey = key, href = "OEBPS/chapter3.xhtml", locator = locator("OEBPS/chapter3.xhtml", 0.4, "third"), quote = "third", style = AnnotationStyle.UNDERLINE, color = AnnotationColors.BLUE, note = "a note", chapter = "Chapter 3", createdAt = 1_000L))
                dao.insert(BookAnnotation(bookKey = key, href = "OEBPS/chapter1.xhtml", locator = locator("OEBPS/chapter1.xhtml", 0.7, "later in one"), quote = "later in one", createdAt = 2_000L))
                dao.insert(BookAnnotation(bookKey = key, href = "OEBPS/chapter1.xhtml", locator = locator("OEBPS/chapter1.xhtml", 0.2, "early in one"), quote = "early in one", createdAt = 3_000L))
            }
            val all = JSONArray(annotations(book).requireOk("all").getString(EpubContract.KEY_ANNOTATIONS))
            assertEquals(3, all.length())
            assertEquals(listOf("early in one", "later in one", "third"), (0 until 3).map { all.getJSONObject(it).getString(EpubContract.FIELD_QUOTE) })
            val third = all.getJSONObject(2)
            assertEquals(EpubContract.STYLE_UNDERLINE, third.getString(EpubContract.FIELD_STYLE))
            assertEquals("#64B5F6", third.getString(EpubContract.FIELD_COLOR))
            assertEquals("a note", third.getString(EpubContract.FIELD_NOTE))
            assertEquals("Chapter 3", third.getString(EpubContract.FIELD_TITLE))
            assertEquals("OEBPS/chapter3.xhtml", third.getJSONObject(EpubContract.FIELD_LOCATOR).getString(EpubContract.FIELD_HREF))
            assertTrue(third.getLong(EpubContract.FIELD_ID) > 0L)
            assertEquals(1_000L, third.getLong(EpubContract.FIELD_CREATED_AT))
            assertFalse(all.getJSONObject(0).has(EpubContract.FIELD_NOTE))
            assertEquals(setOf("id", "style", "color", "quote", "locator", "createdAt", "updatedAt"), all.getJSONObject(0).keys().asSequence().toSet())

            val first = annotations(book, offset = 0, limit = 2).requireOk("page 1")
            assertEquals(0, first.getInt(EpubContract.KEY_OFFSET))
            assertEquals(2, JSONArray(first.getString(EpubContract.KEY_ANNOTATIONS)).length())
            assertTrue(first.getBoolean(EpubContract.KEY_HAS_MORE))
            val second = annotations(book, offset = 2, limit = 2).requireOk("page 2")
            assertEquals(2, second.getInt(EpubContract.KEY_OFFSET))
            assertEquals("third", JSONArray(second.getString(EpubContract.KEY_ANNOTATIONS)).getJSONObject(0).getString(EpubContract.FIELD_QUOTE))
            assertFalse(second.getBoolean(EpubContract.KEY_HAS_MORE))
            val beyond = annotations(book, offset = 9, limit = 2).requireOk("beyond")
            assertEquals(0, JSONArray(beyond.getString(EpubContract.KEY_ANNOTATIONS)).length())
            assertFalse(beyond.getBoolean(EpubContract.KEY_HAS_MORE))
            annotations(book, limit = EpubContract.MAX_ANNOTATIONS_PAGE + 1).requireError("oversized page", EpubErrorCodes.LIMIT_EXCEEDED)
            annotations(book, offset = -1).requireError("negative offset", EpubErrorCodes.INVALID_ARGUMENT)
            book.close()
            annotations(book).requireError("after close", EpubErrorCodes.SESSION_CLOSED)
            note("version 2 host: 3 highlights in reading order, pages 2+1, limit ${EpubContract.MAX_ANNOTATIONS_PAGE + 1} refused")
        } finally {
            runBlocking { dao.deleteEverything() }
            writeNotes("service-annotations")
        }
    }

    @Test
    fun failuresCarryTheContractCodes() {
        val plugin = bind()

        note("not a zip: " + failure<IllegalArgumentException>(EpubErrorCodes.NOT_EPUB) { open(plugin, "malformed-not-a-zip.epub") })
        note("encrypted: " + failure<IllegalStateException>(EpubErrorCodes.ENCRYPTED) { open(plugin, "malformed-encrypted-lcp.epub") })
        note("missing descriptor: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) { plugin.openBook(null, Bundle()) })
        val pipe = ParcelFileDescriptor.createPipe()
        try {
            note("pipe descriptor: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) { plugin.openBook(pipe[0], Bundle()) })
        } finally {
            pipe.forEach { runCatching { it.close() } }
        }
        val fatOptions = Bundle().apply { putString("padding", "x".repeat(EpubContract.MAX_OPTIONS_BYTES + 1024)) }
        note("fat options: " + failure<IllegalStateException>(EpubErrorCodes.LIMIT_EXCEEDED) { open(plugin, "minimal-epub3.epub", fatOptions) })
        note("reader session without callback: " + failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) {
            descriptor("minimal-epub3.epub").use { plugin.openReader(it, Bundle(), null) }
        })

        val book = open(plugin, "minimal-epub3.epub")
        try {
            text(book, href = "not/here.xhtml").requireError("unknown href", EpubErrorCodes.RESOURCE_NOT_FOUND)
            text(book).requireError("no target", EpubErrorCodes.INVALID_ARGUMENT)
            text(book, index = 99).requireError("index 99", EpubErrorCodes.RESOURCE_NOT_FOUND)
            text(book, index = -1).requireError("index -1", EpubErrorCodes.INVALID_ARGUMENT)
            text(book, index = 0, offset = -1).requireError("offset -1", EpubErrorCodes.INVALID_ARGUMENT)
            text(book, index = 0, maxChars = 2 * 1024 * 1024).requireError("maxChars 2 MiB", EpubErrorCodes.LIMIT_EXCEEDED)
            text(book, index = 0, format = "html").requireError("format html", EpubErrorCodes.INVALID_ARGUMENT)
            text(book, href = "a".repeat(EpubContract.MAX_HREF_LENGTH + 1)).requireError("long href", EpubErrorCodes.LIMIT_EXCEEDED)
            search(book, "").requireError("empty query", EpubErrorCodes.INVALID_ARGUMENT)
            search(book, null).requireError("null query", EpubErrorCodes.INVALID_ARGUMENT)
            search(book, "q".repeat(EpubContract.MAX_QUERY_LENGTH + 1)).requireError("long query", EpubErrorCodes.LIMIT_EXCEEDED)
            search(book, "reef", limit = EpubContract.MAX_SEARCH_RESULTS + 1).requireError("limit 501", EpubErrorCodes.LIMIT_EXCEEDED)
            search(book, "reef", offset = EpubContract.MAX_SEARCH_RESULTS).requireError("offset 500", EpubErrorCodes.LIMIT_EXCEEDED)
            search(book, "reef", offset = -1).requireError("offset -1", EpubErrorCodes.INVALID_ARGUMENT)
            failure<IllegalArgumentException>(EpubErrorCodes.RESOURCE_NOT_FOUND) { book.openResource("not/here.png") }
            failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) { book.openResource("") }
            failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) { book.openResource(null) }
            failure<IllegalArgumentException>(EpubErrorCodes.INVALID_ARGUMENT) { book.openResource("ab") }
            note("book-level failures answered with contract codes")
        } finally {
            book.close()
        }
        writeNotes("service-failures")
    }

    @Test
    fun theNinthBookIsRefusedAndUnbindingClosesTheRest() {
        val plugin = bind()
        val books = ArrayList<IEpubBook>()
        try {
            repeat(EpubContract.MAX_OPEN_BOOKS) { books += open(plugin, "minimal-epub3.epub") }
            note("ninth book: " + failure<IllegalStateException>(EpubErrorCodes.LIMIT_EXCEEDED) { open(plugin, "minimal-epub2.epub") })
            books.removeAt(0).close()
            books += open(plugin, "minimal-epub2.epub")
            assertEquals(EpubContract.MAX_OPEN_BOOKS, books.size)
            books.forEach { it.metadata.requireOk("open book") }

            serviceRule.unbindService()
            // The GitHub-hosted API 35 emulator needs more than 10 s to deliver the unbind (CI run 35558551373).
            val deadline = SystemClock.elapsedRealtime() + 30_000
            while (books.any { it.metadata.errorCode() != EpubErrorCodes.SESSION_CLOSED } && SystemClock.elapsedRealtime() < deadline) {
                SystemClock.sleep(100)
            }
            books.forEach { it.metadata.requireError("after unbind", EpubErrorCodes.SESSION_CLOSED) }
            note("unbind closed ${books.size} books")
        } finally {
            books.forEach { runCatching { it.close() } }
        }
        writeNotes("service-capacity")
    }

    /** Forces the generated AIDL Proxy / Parcel path even though the test shares the app process. */
    private class RemoteOnlyBinder(private val target: IBinder) : Binder() {
        override fun queryLocalInterface(descriptor: String): IInterface? = null

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            target.transact(code, data, reply, flags)
    }
}
