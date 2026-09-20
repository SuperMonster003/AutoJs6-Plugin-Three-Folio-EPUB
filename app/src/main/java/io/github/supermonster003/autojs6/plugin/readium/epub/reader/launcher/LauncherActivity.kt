package io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderIntentPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubRequestPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.ReadiumEpubReaderPlugin
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ActivityLauncherBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The plugin's own front door (roadmap P4.1): a grid of the books the user opened through the
 * system document picker, newest first, with cover, title, author, progress and last read time.
 * Only documents whose read grant could be persisted are listed (roadmap D4); a book whose file
 * went away is marked unavailable and can be removed, which also releases its grant.
 */
class LauncherActivity : HostAppearanceActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private val store by lazy { RecentBooksStore.forFilesDirectory(filesDir) }
    private lateinit var adapter: RecentBooksAdapter

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onDocumentPicked(uri)
    }

    /** Test hooks: the books on screen, and the dialog offered for an unavailable or long-pressed book. */
    internal val shownBooks: List<RecentBook> get() = adapter.currentList
    internal var bookDialog: AlertDialog? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        applyInsets()
        adapter = RecentBooksAdapter(lifecycleScope, store, onOpen = ::openBook, onLongPress = ::showBookMenu)
        binding.recentBooks.layoutManager = GridLayoutManager(this, spanCount())
        binding.recentBooks.adapter = adapter
        binding.emptyOpen.setOnClickListener { pickDocument() }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroy() {
        bookDialog?.dismiss()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_launcher, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_open_document -> {
            pickDocument()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /** Opens the system document picker; the picked document lands in [onDocumentPicked]. */
    internal fun pickDocument() {
        try {
            picker.launch(PICKER_MIME_TYPES)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.text_no_file_picker, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * A picked document: its grant is persisted (that is what makes it a recent book), its name
     * and type are checked, and it opens. A grant that cannot be persisted still opens the book
     * once but keeps it off the list.
     */
    internal fun onDocumentPicked(uri: Uri) {
        val persisted = runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        val displayName = queryDisplayName(uri)
            ?: EpubRequestPolicy.sanitizeDisplayName(uri.lastPathSegment)
            ?: FALLBACK_NAME
        val mimeType = runCatching { contentResolver.getType(uri) }.getOrNull()
        if (!EpubRequestPolicy.isSupportedEpub(mimeType, displayName)) {
            if (persisted) releaseGrant(uri)
            Toast.makeText(this, R.string.text_launcher_not_epub, Toast.LENGTH_LONG).show()
            return
        }
        if (persisted) {
            val now = System.currentTimeMillis()
            lifecycleScope.launch {
                val update = withContext(Dispatchers.IO) { store.upsert(RecentBook(uri.toString(), displayName, now, now)) }
                update.evicted.forEach { releaseGrant(it.uri.toUri()) }
                reload()
            }
        } else {
            Toast.makeText(this, R.string.text_launcher_cannot_keep_access, Toast.LENGTH_LONG).show()
        }
        openBook(uri, displayName)
    }

    internal fun openBook(book: RecentBook) {
        if (!book.available) {
            showUnavailable(book)
            return
        }
        openBook(book.uri.toUri(), book.displayName)
    }

    /**
     * Starts the reader with the plugin's own explicit action; the read grant on the intent is
     * what the reader's policy checks, and a persisted grant the system no longer honours fails
     * right here (the book is then marked unavailable).
     */
    private fun openBook(uri: Uri, displayName: String): Boolean {
        val intent = Intent(this, EpubReaderActivity::class.java)
            .setAction(EpubReaderIntentPolicy.ACTION_OPEN_RECENT)
            .setDataAndType(uri, ReadiumEpubReaderPlugin.EPUB_MIME_TYPE)
            .putExtra(EpubReaderIntentPolicy.EXTRA_DISPLAY_NAME, displayName)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return try {
            startActivity(intent)
            true
        } catch (_: SecurityException) {
            markUnavailable(uri)
            Toast.makeText(this, R.string.text_launcher_unavailable, Toast.LENGTH_LONG).show()
            false
        }
    }

    internal fun removeBook(book: RecentBook) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { store.remove(book.uri) }
            releaseGrant(book.uri.toUri())
            Toast.makeText(this@LauncherActivity, R.string.text_launcher_removed, Toast.LENGTH_SHORT).show()
            reload()
        }
    }

    private fun showBookMenu(book: RecentBook) {
        bookDialog?.dismiss()
        bookDialog = AlertDialog.Builder(this)
            .setTitle(book.title ?: book.displayName)
            .setItems(arrayOf(getString(R.string.text_launcher_open), getString(R.string.text_launcher_remove))) { _, which ->
                if (which == 0) openBook(book) else removeBook(book)
            }
            .setOnDismissListener { if (bookDialog === it) bookDialog = null }
            .show()
    }

    private fun showUnavailable(book: RecentBook) {
        bookDialog?.dismiss()
        bookDialog = AlertDialog.Builder(this)
            .setTitle(book.title ?: book.displayName)
            .setMessage(R.string.text_launcher_unavailable_message)
            .setPositiveButton(R.string.text_launcher_remove) { _, _ -> removeBook(book) }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .setOnDismissListener { if (bookDialog === it) bookDialog = null }
            .show()
    }

    private fun markUnavailable(uri: Uri) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { store.update(uri.toString()) { it.copy(available = false) } }
            reload()
        }
    }

    private fun reload() {
        lifecycleScope.launch {
            val books = withContext(Dispatchers.IO) { RecentBooksPolicy.sorted(store.read()) }
            adapter.submitList(books)
            binding.recentBooks.isVisible = books.isNotEmpty()
            binding.emptyView.isVisible = books.isEmpty()
        }
    }

    private fun releaseGrant(uri: Uri) {
        runCatching { contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
        }
    }.getOrNull()?.let(EpubRequestPolicy::sanitizeDisplayName)

    private fun spanCount(): Int {
        val tileWidth = TILE_WIDTH_DP * resources.displayMetrics.density
        return (resources.displayMetrics.widthPixels / tileWidth).toInt().coerceIn(MIN_SPANS, MAX_SPANS)
    }

    /** Edge to edge like the reader: the toolbar extends under the status bar, the grid under the navigation bar. */
    private fun applyInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val actionBarSize = TypedValue().let { value ->
            theme.resolveAttribute(android.R.attr.actionBarSize, value, true)
            TypedValue.complexToDimensionPixelSize(value.data, resources.displayMetrics)
        }
        val gridPaddingBottom = binding.recentBooks.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            root.updatePadding(left = bars.left, right = bars.right)
            binding.toolbar.updatePadding(top = bars.top)
            binding.toolbar.updateLayoutParams { height = actionBarSize + bars.top }
            binding.recentBooks.updatePadding(bottom = gridPaddingBottom + bars.bottom)
            binding.emptyView.updatePadding(bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private companion object {
        const val TILE_WIDTH_DP = 140f
        const val MIN_SPANS = 2
        const val MAX_SPANS = 6
        const val FALLBACK_NAME = "book.epub"

        /** The picker filters on these; `application/octet-stream` catches providers that do not know the EPUB type (roadmap P4.1). */
        val PICKER_MIME_TYPES = arrayOf(ReadiumEpubReaderPlugin.EPUB_MIME_TYPE, "application/octet-stream")
    }
}
