package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.FontsContainer
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ActivityEpubReaderBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontCatalog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontEntry
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontInspection
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontLimits
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ChromeColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ReaderTheme
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PageTurnAction
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PageTurnPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PreferencesSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ReaderChrome
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ReaderProgress
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.TocSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.FontImportResult
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl

/**
 * Explorer Action execution entry (roadmap P1.2): validates the v2 envelope, lets the view model
 * open the book through the granted descriptor, and hosts Readium's [EpubNavigatorFragment] with
 * the reader chrome (title and chapter, progress bar, immersive mode), the table of contents,
 * scroll or paginated overflow, tap zones and volume keys, progress memory (P1.3), the reading
 * preferences panel with its themes (P2.1) and imported fonts (P2.2).
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

    /** The navigator whose settings the panel follows; null between a removal and its replacement. */
    private val activeNavigator = MutableStateFlow<EpubNavigatorFragment?>(null)
    private var locatorJob: Job? = null

    private val fontPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFont(uri)
    }

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

    /** The theme currently painted on the chrome and handed to the navigator. */
    internal val resolvedTheme: ReaderTheme get() = model.preferences.value.resolvedTheme(hostDarkMode)

    internal val chromeColors: ChromeColors get() = chrome.colors

    internal val preferencesSheet: PreferencesSheet?
        get() = supportFragmentManager.findFragmentByTag(PreferencesSheet.TAG) as? PreferencesSheet

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
        chrome.applyTheme(resolvedTheme)

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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.preferences.collect { applyPreferences(it) }
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
        initialPreferences = effectivePreferences(model.preferences.value),
        listener = this,
        paginationListener = paginationListener,
        configuration = navigatorConfiguration(model.fonts.value),
    )

    /** Every imported font becomes a `@font-face` served from the book's package host (see [FontsContainer]). */
    private fun navigatorConfiguration(catalog: FontCatalog) = EpubNavigatorFragment.Configuration().apply {
        for (entry in catalog.fonts) {
            addFontFamilyDeclaration(FontFamily(entry.family)) {
                addFontFace { addSource(FontsContainer.urlFor(entry)) }
            }
        }
    }

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
            activeNavigator.value = fragment
            navigatorReady = false
            fragment.addInputListener(inputListener)
            // A retained navigator keeps the preferences it last received; the host's night mode
            // may have changed since, so hand it the current effective set once.
            fragment.submitPreferences(effectivePreferences(model.preferences.value))
            locatorJob?.cancel()
            locatorJob = lifecycleScope.launch {
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
            ReaderProgress.snapshot(locator.locations.position, positionCount, locator.locations.totalProgression, fixedLayout),
        )
    }

    /** Every preference change: recolour the chrome, hand the navigator the effective set, refresh the menu. */
    private fun applyPreferences(state: ReaderPreferencesState) {
        chrome.applyTheme(state.resolvedTheme(hostDarkMode))
        navigator?.takeIf { it.isAdded }?.submitPreferences(effectivePreferences(state))
        invalidateOptionsMenu()
    }

    /**
     * The preferences the navigator gets: the theme resolved against the host, and for fixed
     * layouts an automatic spread resolved against the orientation (two pages in landscape), as
     * Readium 3.4.0 has no automatic spread of its own. Reflowable books keep the spread unset.
     */
    private fun effectivePreferences(state: ReaderPreferencesState): EpubPreferences =
        state.effective(hostDarkMode, autoSpread = if (fixedLayout) (if (landscape) Spread.ALWAYS else Spread.NEVER) else null)

    private val landscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** The manifest keeps the Activity across rotations, so the automatic spread is re-resolved here. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (fixedLayout) applyPreferences(model.preferences.value)
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
        model.flushPreferences()
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
        menu.findItem(R.id.action_preferences)?.isVisible = ready
        menu.findItem(R.id.action_scroll_mode)?.apply {
            isVisible = ready && !fixedLayout
            isChecked = scrollMode
        }
        menu.findItem(R.id.action_volume_keys_turn_pages)?.apply {
            isVisible = ready
            isChecked = settings.volumeKeysTurnPages
        }
        menu.findItem(R.id.action_restart_book)?.isVisible = ready
        chrome.tintToolbarIcons(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_table_of_contents -> {
            showTableOfContents()
            true
        }
        R.id.action_preferences -> {
            showPreferences()
            true
        }
        R.id.action_scroll_mode -> {
            setScrollMode(!scrollMode)
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

    /** The overflow the navigator currently uses, or the stored preference before it exists. */
    private val scrollMode: Boolean
        get() = currentSettings?.scroll ?: (model.preferences.value.epub.scroll ?: false)

    internal fun setScrollMode(enabled: Boolean) {
        model.editPreferences { it.copy(scroll = enabled) }
    }

    internal fun showPreferences() {
        if (model.publication == null) return
        PreferencesSheet.show(supportFragmentManager)
    }

    // Preferences access for the panel (roadmap P2.1)

    internal val preferencesState: StateFlow<ReaderPreferencesState> get() = model.preferences

    /** The settings of whichever navigator is current, null while none is attached (survives a rebuild). */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val navigatorSettings: Flow<EpubSettings?> =
        activeNavigator.flatMapLatest { fragment -> fragment?.settings ?: flowOf<EpubSettings?>(null) }

    internal val currentSettings: EpubSettings?
        get() = navigator?.takeIf { it.isAdded }?.settings?.value

    internal val fixedLayout: Boolean get() = model.publication?.metadata?.layout == Layout.FIXED

    internal fun editPreferences(transform: (EpubPreferences) -> EpubPreferences) = model.editPreferences(transform)

    internal fun setThemeMode(mode: ThemeMode) = model.setThemeMode(mode)

    internal fun resetPreferences() = model.resetPreferences()

    // Imported fonts (roadmap P2.2)

    internal val fontCatalog: StateFlow<FontCatalog> get() = model.fonts

    /** Opens the system document picker; the picked document lands in [importFont]. */
    internal fun pickFont() {
        try {
            fontPicker.launch(FONT_MIME_TYPES)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.text_no_file_picker, Toast.LENGTH_SHORT).show()
        }
    }

    /** Stores the font behind [uri], selects it, and rebuilds the navigator so its declaration is injected. */
    internal fun importFont(uri: Uri) {
        lifecycleScope.launch {
            val result = model.importFont(contentResolver, uri)
            when (result) {
                is FontImportResult.Imported -> {
                    model.editPreferences { it.copy(fontFamily = FontFamily(result.entry.family)) }
                    recreateNavigator()
                }
                is FontImportResult.AlreadyImported ->
                    model.editPreferences { it.copy(fontFamily = FontFamily(result.entry.family)) }
                else -> Unit
            }
            Toast.makeText(this@EpubReaderActivity, describe(result), Toast.LENGTH_SHORT).show()
        }
    }

    internal fun deleteFont(entry: FontEntry) {
        lifecycleScope.launch {
            if (!model.deleteFont(entry)) return@launch
            recreateNavigator()
            Toast.makeText(this@EpubReaderActivity, R.string.text_font_deleted, Toast.LENGTH_SHORT).show()
        }
    }

    private fun describe(result: FontImportResult): String = when (result) {
        is FontImportResult.Imported -> getString(R.string.text_font_imported, result.entry.displayName)
        is FontImportResult.AlreadyImported -> getString(R.string.text_font_already_imported, result.entry.displayName)
        is FontImportResult.Rejected -> when (result.reason) {
            FontInspection.Rejected.NotAFont -> getString(R.string.text_font_import_failed_not_a_font)
            FontInspection.Rejected.Collection -> getString(R.string.text_font_import_failed_collection)
            FontInspection.Rejected.Truncated -> getString(R.string.text_font_import_failed_truncated)
        }
        is FontImportResult.TooLarge -> getString(R.string.text_font_import_failed_too_large, FontLimits.MAX_BYTES_MEGABYTES)
        is FontImportResult.TooMany -> getString(R.string.text_font_import_failed_too_many, result.maxFonts)
        FontImportResult.Failed -> getString(R.string.text_font_import_failed_read)
    }

    /**
     * Font declarations are fixed when a navigator is created (Readium injects them into every
     * page), so a changed catalog needs a new fragment; it starts where the old one last reported.
     */
    private fun recreateNavigator() {
        val fragment = navigator ?: return
        model.flushProgress()
        activeNavigator.value = null
        locatorJob?.cancel()
        locatorJob = null
        observedNavigator = null
        navigatorReady = false
        pendingJump = null
        supportFragmentManager.commitNow { remove(fragment) }
        if (model.state.value is OpenState.Ready) showReader()
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

        // Font MIME types differ between providers; the wildcard keeps unlabeled files pickable, the signature decides.
        private val FONT_MIME_TYPES = arrayOf(
            "font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-opentype", "application/font-sfnt", "*/*",
        )
    }
}
