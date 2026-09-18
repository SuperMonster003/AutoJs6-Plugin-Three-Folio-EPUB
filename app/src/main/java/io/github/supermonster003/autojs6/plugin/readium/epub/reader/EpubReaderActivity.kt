package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.BookOpener
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.PfdResource
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ActivityEpubReaderBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.getOrElse

/**
 * Explorer Action execution entry (roadmap P0.2 baseline): validates the v2 envelope, opens the
 * book through [PfdResource] + [BookOpener], and hosts Readium's [EpubNavigatorFragment].
 */
@OptIn(ExperimentalReadiumApi::class)
class EpubReaderActivity : HostAppearanceActivity(), EpubNavigatorFragment.Listener {

    private lateinit var binding: ActivityEpubReaderBinding
    private val model: EpubReaderViewModel by viewModels()

    private val navigator: EpubNavigatorFragment?
        get() = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? EpubNavigatorFragment

    /** The open table-of-contents dialog, dismissed on destroy so recreation never leaks its window. */
    internal var tableOfContentsDialog: AlertDialog? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        val restoredFactory = model.navigatorFactory
        if (restoredFactory != null) {
            supportFragmentManager.fragmentFactory = fragmentFactory(restoredFactory)
        } else if (savedInstanceState != null) {
            // The process was killed: the publication is gone, so restore nothing and reopen below.
            supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
        }
        super.onCreate(savedInstanceState)
        binding = ActivityEpubReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (restoredFactory == null && savedInstanceState != null) {
            supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG)?.let { stale ->
                supportFragmentManager.commitNow { remove(stale) }
            }
        }

        val request = EpubReaderIntentPolicy.resolve(intent)
        if (request == null) {
            showError(getString(R.string.text_invalid_request))
            return
        }
        supportActionBar?.title = request.displayName

        when {
            restoredFactory != null -> showReader()
            model.openError != null -> showError(model.openError.orEmpty())
            else -> lifecycleScope.launch { openBook(request) }
        }
    }

    private suspend fun openBook(request: EpubReaderRequest) {
        showStatus(getString(R.string.text_opening_book))
        val descriptor = withContext(Dispatchers.IO) {
            runCatching { contentResolver.openFileDescriptor(request.documentUri, "r") }.getOrNull()
        }
        if (descriptor == null) {
            model.openError = getString(R.string.text_cannot_read_file)
            showError(model.openError.orEmpty())
            return
        }
        val resource = PfdResource(descriptor, request.displayName)
        val publication = BookOpener(this).open(resource).getOrElse { error ->
            resource.close()
            model.openError = getString(R.string.text_open_failed, error.message)
            showError(model.openError.orEmpty())
            return
        }
        model.adopt(resource, publication)
        supportFragmentManager.fragmentFactory = fragmentFactory(model.navigatorFactory ?: return)
        showReader()
    }

    private fun fragmentFactory(factory: EpubNavigatorFactory) = factory.createFragmentFactory(
        initialLocator = model.lastLocator,
        listener = this,
    )

    private fun showReader() {
        if (navigator == null) {
            supportFragmentManager.commitNow {
                replace(R.id.reader_container, EpubNavigatorFragment::class.java, Bundle(), NAVIGATOR_TAG)
            }
        }
        binding.statusPanel.isVisible = false
        binding.readerContainer.isVisible = true
        navigator?.let { fragment ->
            lifecycleScope.launch {
                fragment.currentLocator.collect { model.lastLocator = it }
            }
        }
        invalidateOptionsMenu()
    }

    private fun showStatus(message: String) {
        binding.statusPanel.isVisible = true
        binding.statusProgress.isVisible = true
        binding.statusText.text = message
    }

    private fun showError(message: String) {
        binding.statusPanel.isVisible = true
        binding.statusProgress.isVisible = false
        binding.statusText.text = message
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_epub_reader, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_table_of_contents)?.isVisible = model.publication != null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_table_of_contents -> {
            showTableOfContents()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showTableOfContents() {
        val publication = model.publication ?: return
        val entries = flattenTableOfContents(publication.tableOfContents)
        if (entries.isEmpty()) {
            Toast.makeText(this, R.string.text_no_table_of_contents, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = entries.map { (depth, link) ->
            "    ".repeat(depth) + (link.title?.takeIf { it.isNotBlank() } ?: link.href.toString())
        }.toTypedArray()
        tableOfContentsDialog?.dismiss()
        tableOfContentsDialog = AlertDialog.Builder(this)
            .setTitle(R.string.text_table_of_contents)
            .setItems(labels) { _, index -> navigator?.go(entries[index].second, animated = true) }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .setOnDismissListener { tableOfContentsDialog = null }
            .show()
    }

    override fun onDestroy() {
        tableOfContentsDialog?.dismiss()
        tableOfContentsDialog = null
        super.onDestroy()
    }

    // HyperlinkNavigator.Listener: external links are confirmed before the browser opens (roadmap D25 default).
    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        val target = url.toString()
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            Toast.makeText(this, R.string.text_cannot_open_link, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setMessage(target)
            .setPositiveButton(R.string.dialog_button_confirm) { _, _ -> openInBrowser(target) }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    private fun openInBrowser(target: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, target.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.text_cannot_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val NAVIGATOR_TAG = "readium-epub-navigator"

        internal fun flattenTableOfContents(links: List<Link>, depth: Int = 0): List<Pair<Int, Link>> =
            links.flatMap { link -> listOf(depth to link) + flattenTableOfContents(link.children, depth + 1) }
    }
}
