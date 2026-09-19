package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ActivityEpubReaderBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PageTurnAction
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PageTurnPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ReaderChrome
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ReaderProgress
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.TocSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl

/**
 * Explorer Action execution entry (roadmap P1.2): validates the v2 envelope, lets the view model
 * open the book through the granted descriptor, and hosts Readium's [EpubNavigatorFragment] with
 * the reader chrome (title and chapter, progress bar, immersive mode), the table of contents,
 * scroll or paginated overflow, tap zones and volume keys, and progress memory (P1.3).
 */
@OptIn(ExperimentalReadiumApi::class)
class EpubReaderActivity : HostAppearanceActivity(), EpubNavigatorFragment.Listener {

    private lateinit var binding: ActivityEpubReaderBinding
    private lateinit var chrome: ReaderChrome
    private val model: EpubReaderViewModel by viewModels()
    private val settings by lazy { ReaderSettings(this) }

    private val navigator: EpubNavigatorFragment?
        get() = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? EpubNavigatorFragment

    /** The navigator instance whose input and locator streams this Activity already observes. */
    private var observedNavigator: EpubNavigatorFragment? = null

    /**
     * True once the current navigator finished loading its initial resource. Readium drops every
     * locator notification until that first resource reports loaded, so a jump issued earlier
     * (table of contents, restart) would freeze the position stream for the rest of the session;
     * such jumps wait in [pendingJump] and replay from [markNavigatorReady].
     */
    internal var navigatorReady = false
        private set

    private var pendingJump: Link? = null

    private val paginationListener = object : EpubNavigatorFragment.PaginationListener {
        // Only reached once the navigator is in its ready state (reflowable layouts).
        override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) = markNavigatorReady()

        // Fixed layouts never report page changes; their first loaded page is the best signal available.
        override fun onPageLoaded() {
            if (model.publication?.metadata?.layout == Layout.FIXED) markNavigatorReady()
        }
    }

    /** The open table-of-contents dialog, dismissed on destroy so recreation never leaks its window. */
    internal var tableOfContentsDialog: AlertDialog? = null
        private set

    internal val isImmersive: Boolean get() = chrome.immersive

    /** Test hooks: the view model that owns the book, and the chrome toggle the center tap triggers. */
    internal val readerModel: EpubReaderViewModel get() = model

    internal fun toggleImmersive() = chrome.toggleImmersive()

    private val inputListener = object : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            val fragment = navigator ?: return false
            val rightToLeft = fragment.overflow.value.readingProgression == ReadingProgression.RTL
            perform(PageTurnPolicy.resolveTap(event.point.x, fragment.publicationView.width, rightToLeft))
            return true
        }
    }

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
        chrome = ReaderChrome(this, binding)

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
        chrome.setBookTitle(request.displayName)
        chrome.setImmersive(savedInstanceState?.getBoolean(STATE_IMMERSIVE) ?: false)

        val savedLocator = savedInstanceState?.let { BundleCompat.getParcelable(it, STATE_LOCATOR, Locator::class.java) }
        model.open(request, contentResolver, savedLocator)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    when (state) {
                        OpenState.Idle, OpenState.Opening -> showStatus(getString(R.string.text_opening_book))
                        OpenState.Ready -> showReader()
                        is OpenState.Failed -> showError(describe(state.failure))
                    }
                }
            }
        }
    }

    private fun describe(failure: OpenFailure): String = when (failure) {
        OpenFailure.CannotRead -> getString(R.string.text_cannot_read_file)
        OpenFailure.NotAnEpub -> getString(R.string.text_open_failed_not_epub)
        OpenFailure.Protected -> getString(R.string.text_open_failed_protected)
        OpenFailure.TimedOut -> getString(R.string.text_open_failed_timeout)
        is OpenFailure.Other -> getString(R.string.text_open_failed, failure.detail)
    }

    private fun fragmentFactory(factory: EpubNavigatorFactory) = factory.createFragmentFactory(
        initialLocator = model.lastLocator,
        initialPreferences = EpubPreferences(scroll = settings.scrollMode),
        listener = this,
        paginationListener = paginationListener,
    )

    private fun showReader() {
        val publication = model.publication ?: return
        if (navigator == null) {
            supportFragmentManager.fragmentFactory = fragmentFactory(model.navigatorFactory ?: return)
            supportFragmentManager.commitNow {
                replace(R.id.reader_container, EpubNavigatorFragment::class.java, Bundle(), NAVIGATOR_TAG)
            }
        }
        binding.statusPanel.isVisible = false
        binding.readerContainer.isVisible = true
        chrome.readerVisible = true
        chrome.setBookTitle(publication.metadata.title ?: binding.toolbar.title)
        val fragment = navigator ?: return
        if (observedNavigator !== fragment) {
            observedNavigator = fragment
            navigatorReady = false
            fragment.addInputListener(inputListener)
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    combine(fragment.currentLocator, model.positionCount) { locator, count -> locator to count }
                        .collect { (locator, count) -> onLocator(locator, count) }
                }
            }
        }
        invalidateOptionsMenu()
    }

    private fun onLocator(locator: Locator, positionCount: Int) {
        model.onLocatorChanged(locator)
        val publication = model.publication ?: return
        chrome.setChapterTitle(locator.title ?: TocSheet.chapterTitle(publication, locator.href.toString()))
        chrome.showProgress(
            ReaderProgress.snapshot(locator.locations.position, positionCount, locator.locations.totalProgression),
        )
    }

    private fun perform(action: PageTurnAction) {
        val fragment = navigator ?: return
        when (action) {
            PageTurnAction.PREVIOUS -> if (navigatorReady) fragment.goBackward(animated = true)
            PageTurnAction.NEXT -> if (navigatorReady) fragment.goForward(animated = true)
            PageTurnAction.TOGGLE_CHROME -> chrome.toggleImmersive()
        }
    }

    private fun markNavigatorReady() {
        if (navigatorReady) return
        navigatorReady = true
        pendingJump?.let { link ->
            pendingJump = null
            navigator?.go(link, animated = true)
        }
    }

    /** Jumps to [link] now, or as soon as the navigator has loaded its initial resource. */
    internal fun jumpTo(link: Link) {
        if (navigatorReady) navigator?.go(link, animated = true) else pendingJump = link
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
        if (::chrome.isInitialized) chrome.readerVisible = false
        invalidateOptionsMenu()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        model.lastLocator?.let { outState.putParcelable(STATE_LOCATOR, it) }
        outState.putBoolean(STATE_IMMERSIVE, ::chrome.isInitialized && chrome.immersive)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        model.flushProgress()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::chrome.isInitialized) chrome.onWindowFocusGained()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val action = volumeKeyAction(keyCode) ?: return super.onKeyDown(keyCode, event)
        if (event.repeatCount == 0) perform(action)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        volumeKeyAction(keyCode) != null || super.onKeyUp(keyCode, event)

    private fun volumeKeyAction(keyCode: Int): PageTurnAction? {
        if (!navigatorReady || model.state.value !is OpenState.Ready) return null
        return PageTurnPolicy.resolveKey(keyCode, settings.volumeKeysTurnPages)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_epub_reader, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val ready = model.publication != null
        menu.findItem(R.id.action_table_of_contents)?.isVisible = ready
        menu.findItem(R.id.action_scroll_mode)?.apply {
            isVisible = ready
            isChecked = settings.scrollMode
        }
        menu.findItem(R.id.action_volume_keys_turn_pages)?.apply {
            isVisible = ready
            isChecked = settings.volumeKeysTurnPages
        }
        menu.findItem(R.id.action_restart_book)?.isVisible = ready
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_table_of_contents -> {
            showTableOfContents()
            true
        }
        R.id.action_scroll_mode -> {
            setScrollMode(!settings.scrollMode)
            true
        }
        R.id.action_volume_keys_turn_pages -> {
            settings.volumeKeysTurnPages = !settings.volumeKeysTurnPages
            invalidateOptionsMenu()
            true
        }
        R.id.action_restart_book -> {
            confirmRestart()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    internal fun setScrollMode(enabled: Boolean) {
        settings.scrollMode = enabled
        navigator?.submitPreferences(EpubPreferences(scroll = enabled))
        invalidateOptionsMenu()
    }

    private fun showTableOfContents() {
        val publication = model.publication ?: return
        val rows = TocSheet.rows(publication)
        if (rows.isEmpty()) {
            Toast.makeText(this, R.string.text_no_table_of_contents, Toast.LENGTH_SHORT).show()
            return
        }
        tableOfContentsDialog?.dismiss()
        tableOfContentsDialog = TocSheet.show(
            context = this,
            rows = rows,
            currentHref = navigator?.currentLocator?.value?.href?.toString(),
            onSelected = ::jumpTo,
            onDismissed = { tableOfContentsDialog = null },
        )
    }

    private fun confirmRestart() {
        AlertDialog.Builder(this)
            .setTitle(R.string.text_restart_book)
            .setMessage(R.string.text_restart_book_message)
            .setPositiveButton(R.string.dialog_button_confirm) { _, _ -> restartFromBeginning() }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    internal fun restartFromBeginning() {
        val publication = model.publication ?: return
        model.clearProgress()
        publication.readingOrder.firstOrNull()?.let(::jumpTo)
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
        internal const val NAVIGATOR_TAG = "readium-epub-navigator"
        private const val STATE_LOCATOR = "locator"
        private const val STATE_IMMERSIVE = "immersive"
    }
}
