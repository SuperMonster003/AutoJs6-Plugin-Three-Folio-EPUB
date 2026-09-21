package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.os.BundleCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationAddResult
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationMarkdown
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationStyle
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.BookAnnotation
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.FontsContainer
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ActivityEpubReaderBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontCatalog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontEntry
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontInspection
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.fonts.FontLimits
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ChromeColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ReaderTheme
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ThemeMode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.AnnotationDialog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.AnnotationExport
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.AnnotationSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.BookmarkPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.BookmarkSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ExternalLink
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ImageViewerDialog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.LinkPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PageLocation
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PageTurnAction
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PageTurnPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.PreferencesSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ProcessTextTarget
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ReaderChrome
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.ReaderProgress
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.SearchSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.SelectionActionMode
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.SelectionActions
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.TocSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader.WebViewBoundary
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.service.HostSessionPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.service.ReaderSessionController
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.FontImportResult
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchState
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.Bookmark
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.TapZones
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.SleepTimer
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.SleepTimerPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsEvent
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsLocation
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsSession
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsSheet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsStatus
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.autojs.plugin.epub.api.EpubActions
import org.autojs.plugin.epub.api.EpubContract
import org.json.JSONObject
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.HyperlinkNavigator
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
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.util.AbsoluteUrl

/**
 * Explorer Action execution entry (roadmap P1.2): validates the v2 envelope, lets the view model
 * open the book through the granted descriptor, and hosts Readium's [EpubNavigatorFragment] with
 * the reader chrome (title and chapter, progress bar, immersive mode), the table of contents,
 * scroll or paginated overflow, tap zones and volume keys, progress memory (P1.3), the reading
 * preferences panel with its themes (P2.1) and imported fonts (P2.2), the full-text search
 * with its results panel, hit decorations and previous / next bar (P2.5), and read-aloud with its
 * bar, sentence highlight and page follow (P3).
 */
@OptIn(ExperimentalReadiumApi::class)
open class EpubReaderActivity : HostAppearanceActivity(), EpubNavigatorFragment.Listener, ReaderSessionController {

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

    private var pendingJump: Locator? = null

    /** The navigator whose settings the panel follows; null between a removal and its replacement. */
    private val activeNavigator = MutableStateFlow<EpubNavigatorFragment?>(null)
    private var locatorJob: Job? = null

    private val fontPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFont(uri)
    }

    /** Roadmap P9.3: the document the user picked for the Markdown export of the highlights. */
    private val annotationSaver = registerForActivityResult(ActivityResultContracts.CreateDocument(AnnotationMarkdown.MIME_TYPE)) { uri ->
        if (uri != null) writeAnnotationExport(uri)
    }

    internal var annotationExportDialog: AlertDialog? = null
        private set

    private val paginationListener = object : EpubNavigatorFragment.PaginationListener {
        // Only reached once the navigator is in its ready state (reflowable layouts).
        override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
            markNavigatorReady()
            updateCurrentPage(
                PageLocation(
                    href = locator.href.toString(),
                    progression = locator.locations.progression,
                    position = locator.locations.position,
                    pageIndex = pageIndex,
                    totalPages = totalPages,
                    scroll = currentSettings?.scroll ?: false,
                ),
            )
        }

        // Fixed layouts never report page changes; their first loaded page is the best signal available.
        override fun onPageLoaded() {
            if (model.publication?.metadata?.layout == Layout.FIXED) markNavigatorReady()
        }
    }

    /** The open table-of-contents dialog, dismissed on destroy so recreation never leaks its window. */
    internal var tableOfContentsDialog: AlertDialog? = null
        private set

    internal var noteDialog: AlertDialog? = null
        private set
    internal var externalLinkDialog: AlertDialog? = null
        private set

    /** Enabled while the link history has somewhere to go back to (roadmap P2.7). */
    private val linkBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            returnFromLink()
        }
    }

    /** Read-aloud (roadmap P3): the dialog offering the engine settings or the voice installer. */
    internal var ttsDialog: AlertDialog? = null
        private set

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(this, R.string.text_read_aloud_no_notification, Toast.LENGTH_LONG).show()
        launchReadAloud()
    }

    private var pendingTtsFollow: Locator? = null
    private var ttsFollowJob: Job? = null
    private var lastTtsFollowAt = 0L
    private var ttsHighlight: Locator? = null

    internal val isImmersive: Boolean get() = chrome.immersive

    /** Test hooks: the view model that owns the book, and the chrome toggle the center tap triggers. */
    internal val readerModel: EpubReaderViewModel get() = model

    internal fun toggleImmersive() = chrome.toggleImmersive()

    /** The theme currently painted on the chrome and handed to the navigator. */
    internal val resolvedTheme: ReaderTheme get() = model.preferences.value.resolvedTheme(hostDarkMode)

    internal val chromeColors: ChromeColors get() = chrome.colors

    internal val preferencesSheet: PreferencesSheet?
        get() = supportFragmentManager.findFragmentByTag(PreferencesSheet.TAG) as? PreferencesSheet

    internal val searchSheet: SearchSheet?
        get() = supportFragmentManager.findFragmentByTag(SearchSheet.TAG) as? SearchSheet

    internal val bookmarkSheet: BookmarkSheet?
        get() = supportFragmentManager.findFragmentByTag(BookmarkSheet.TAG) as? BookmarkSheet

    internal val annotationSheet: AnnotationSheet?
        get() = supportFragmentManager.findFragmentByTag(AnnotationSheet.TAG) as? AnnotationSheet

    internal val annotationDialog: AnnotationDialog?
        get() = supportFragmentManager.findFragmentByTag(AnnotationDialog.TAG) as? AnnotationDialog

    /** The page on screen as the bookmark rules see it; null until the navigator reports one. */
    private var currentPage: PageLocation? = null

    private val _currentBookmark = MutableStateFlow<Bookmark?>(null)

    /** The bookmark of the page on screen, or null: drives the toolbar icon and the panel's add button. */
    internal val currentBookmark: StateFlow<Bookmark?> get() = _currentBookmark

    /** Decorations are applied one batch at a time per group: Readium diffs against the last batch it got. */
    private val decorationMutex = Mutex()

    /** Roadmap P9.2: a tap on a highlight opens its editor. */
    private val annotationListener = object : DecorableNavigator.Listener {
        override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean =
            onAnnotationDecorationActivated(event.decoration.id)
    }

    private val inputListener = object : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            val fragment = navigator ?: return false
            val image = event.targetElement?.content as? Content.ImageElement
            if (image != null) {
                showImage(image)
                return true
            }
            val view = fragment.publicationView
            perform(PageTurnPolicy.resolveTap(event.point.x, event.point.y, view.width, view.height, settings.tapZones, rightToLeft))
            return true
        }
    }

    private val rightToLeft: Boolean
        get() = navigator?.overflow?.value?.readingProgression == ReadingProgression.RTL

    override fun onCreate(savedInstanceState: Bundle?) {
        // Roadmap P7.2: every page WebView Readium creates below this Activity gets the plugin's boundary.
        WebViewBoundary.install(supportFragmentManager)
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
        onBackPressedDispatcher.addCallback(this, linkBackCallback)
        linkBackCallback.isEnabled = !model.linkHistory.isEmpty()
        chrome = ReaderChrome(this, binding)
        chrome.applyTheme(resolvedTheme)
        chrome.setSearchBarListeners(
            onPrevious = { stepSearchResult(-1) },
            onNext = { stepSearchResult(+1) },
            onClose = { closeSearch() },
        )
        chrome.setReadAloudListeners(
            onPrevious = { model.tts.previous() },
            onPlayPause = { model.tts.togglePlayPause() },
            onNext = { model.tts.next() },
            onSettings = { showTtsSettings() },
            onStop = { model.tts.stop() },
        )

        if (restoredFactory == null && savedInstanceState != null) {
            supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG)?.let { stale ->
                supportFragmentManager.commitNow { remove(stale) }
            }
        }

        val resuming = intent.action == ACTION_RESUME_READ_ALOUD
        val hosted = intent.action == EpubActions.READER_ACTIVITY_ACTION
        val request = if (resuming || hosted) null else resolveRequest()
        if (resuming) {
            // Roadmap D26: the read-aloud notification brings back the book the voice is reading.
            if (!model.resumeBackground()) {
                Toast.makeText(this, R.string.text_read_aloud_no_background_session, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            chrome.setBookTitle(model.publication?.metadata?.title ?: getString(R.string.text_read_aloud))
        } else if (hosted) {
            // Roadmap P5.3 / D12: the host opened a session and starts this reader with its token; any other token is an invalid request.
            val token = HostSessionPolicy.tokenOf(
                intent.action,
                intent.component?.className == ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME,
                intent.getStringExtra(EpubActions.EXTRA_SESSION_TOKEN),
            )
            if (token == null || !model.adoptSession(token)) {
                showError(getString(R.string.text_invalid_request))
                return
            }
            chrome.setBookTitle(model.publication?.metadata?.title ?: model.hostSession?.displayName ?: "")
        } else if (request == null) {
            showError(getString(R.string.text_invalid_request))
            return
        } else {
            chrome.setBookTitle(request.displayName)
        }
        chrome.setImmersive(savedInstanceState?.getBoolean(STATE_IMMERSIVE) ?: false)

        val savedLocator = savedInstanceState?.let { BundleCompat.getParcelable(it, STATE_LOCATOR, Locator::class.java) }
        if (request != null) model.open(request, contentResolver, savedLocator)
        model.hostSession?.attachController(this)
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.search.collect { onSearchState(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.bookmarks.collect { refreshCurrentBookmark() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.annotations.collect { applyAnnotationDecorations(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.tts.status.collect { onTtsStatus(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.tts.location.collect { onTtsLocation(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.tts.events.collect { onTtsEvent(it) }
            }
        }
    }

    /** The door this component opens (roadmap P4.2): the protected reader takes the Explorer envelope and the launcher's action. */
    internal open fun resolveRequest(): EpubReaderRequest? = EpubReaderIntentPolicy.resolve(intent, RequestReceiver.READER)

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
        selectionActionModeCallback = SelectionActionMode(this@EpubReaderActivity)
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
            updateCurrentPage(null)
            fragment.addInputListener(inputListener)
            fragment.addDecorationListener(ANNOTATION_DECORATIONS, annotationListener)
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
            // A rebuilt navigator starts without decorations: hand it the current search hits, the spoken sentence and the highlights.
            applySearchDecorations(model.search.value)
            applyTtsDecoration(model.tts.location.value?.locator)
            applyAnnotationDecorations(model.annotations.value)
        }
        invalidateOptionsMenu()
    }

    private fun onLocator(locator: Locator, positionCount: Int) {
        model.onLocatorChanged(locator)
        // Fixed layouts report no page changes; there one resource is one page.
        if (fixedLayout) {
            updateCurrentPage(
                PageLocation(locator.href.toString(), locator.locations.progression, locator.locations.position, null, null, false),
            )
        }
        val publication = model.publication ?: return
        val chapterTitle = locator.title ?: TocSheet.chapterTitle(publication, locator.href.toString())
        chrome.setChapterTitle(chapterTitle)
        model.hostSession?.onLocator(locator, positionCount, chapterTitle)
        chrome.showProgress(
            ReaderProgress.snapshot(locator.locations.position, positionCount, locator.locations.totalProgression, fixedLayout),
        )
    }

    /** Every preference change: recolour the chrome, hand the navigator the effective set, refresh the menu. */
    private fun applyPreferences(state: ReaderPreferencesState) {
        chrome.applyTheme(state.resolvedTheme(hostDarkMode))
        navigator?.takeIf { it.isAdded }?.submitPreferences(effectivePreferences(state))
        // The hit decorations take the theme's accent colour.
        applySearchDecorations(model.search.value)
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
            PageTurnAction.PREVIOUS -> if (navigatorReady) {
                stopReadAloudForNavigation()
                fragment.goBackward(animated = true)
            }
            PageTurnAction.NEXT -> if (navigatorReady) {
                stopReadAloudForNavigation()
                fragment.goForward(animated = true)
            }
            PageTurnAction.TOGGLE_CHROME -> chrome.toggleImmersive()
        }
    }

    private fun markNavigatorReady() {
        if (navigatorReady) return
        navigatorReady = true
        pendingJump?.let { locator ->
            pendingJump = null
            navigator?.go(locator, animated = true)
        }
    }

    /** Jumps to [link] now, or as soon as the navigator has loaded its initial resource. */
    internal fun jumpTo(link: Link) {
        val locator = model.publication?.locatorFromLink(link) ?: return
        jumpTo(locator)
    }

    /** Jumps to [locator] (a search hit scrolls to its text) now, or once the navigator is ready. */
    internal fun jumpTo(locator: Locator) {
        stopReadAloudForNavigation()
        if (navigatorReady) navigator?.go(locator, animated = true) else pendingJump = locator
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

    /**
     * Keyboard page turns are taken here, before the focused view sees the key: a focused page
     * would otherwise swallow the arrows for its own focus navigation, and the navigator's key
     * listener only learns keys whose `KeyboardEvent.code` is set. Views that are editing text
     * keep their keys: the search field, or a form field inside the page (the web view reports
     * that through onCheckIsTextEditor).
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = PageTurnPolicy.keyboardCode(event.keyCode)
        if (code != null && currentFocus?.onCheckIsTextEditor() != true) {
            val action = keyboardAction(code, event.isShiftPressed)
            if (action != null) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) perform(action)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun keyboardAction(code: String, shift: Boolean): PageTurnAction? {
        if (!navigatorReady || model.state.value !is OpenState.Ready) return null
        return PageTurnPolicy.resolveKeyboard(code, shift, rightToLeft, scrollMode)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_epub_reader, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val ready = model.publication != null
        menu.findItem(R.id.action_search)?.isVisible = ready
        menu.findItem(R.id.action_bookmark)?.apply {
            isVisible = ready
            val bookmarked = _currentBookmark.value != null
            setIcon(if (bookmarked) R.drawable.ic_bookmark_24 else R.drawable.ic_bookmark_border_24)
            setTitle(if (bookmarked) R.string.text_bookmark_remove else R.string.text_bookmark_add)
        }
        menu.findItem(R.id.action_bookmarks)?.isVisible = ready
        menu.findItem(R.id.action_annotations)?.isVisible = ready
        menu.findItem(R.id.action_read_aloud)?.isVisible = ready && !model.tts.isActive
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
        menu.findItem(R.id.action_tap_zones)?.isVisible = ready
        menu.findItem(R.id.action_external_links_direct)?.apply {
            isVisible = ready
            isChecked = settings.externalLinksDirect
        }
        menu.findItem(
            when (settings.tapZones) {
                TapZones.OFF -> R.id.action_tap_zones_off
                TapZones.HORIZONTAL -> R.id.action_tap_zones_horizontal
                TapZones.VERTICAL -> R.id.action_tap_zones_vertical
            },
        )?.isChecked = true
        menu.findItem(R.id.action_restart_book)?.isVisible = ready
        // Roadmap P4.2: a book another app handed over can join the recent list; listed books have nothing to add.
        menu.findItem(R.id.action_add_to_recent)?.isVisible = ready && model.request?.entry == ReaderEntry.EXTERNAL && model.recentUri == null
        chrome.tintToolbarIcons(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_search -> {
            showSearch()
            true
        }
        R.id.action_bookmark -> {
            toggleBookmark()
            true
        }
        R.id.action_bookmarks -> {
            showBookmarks()
            true
        }
        R.id.action_annotations -> {
            showAnnotations()
            true
        }
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
        R.id.action_tap_zones_off -> {
            setTapZones(TapZones.OFF)
            true
        }
        R.id.action_tap_zones_horizontal -> {
            setTapZones(TapZones.HORIZONTAL)
            true
        }
        R.id.action_tap_zones_vertical -> {
            setTapZones(TapZones.VERTICAL)
            true
        }
        R.id.action_external_links_direct -> {
            setExternalLinksDirect(!settings.externalLinksDirect)
            true
        }
        R.id.action_read_aloud -> {
            startReadAloud()
            true
        }
        R.id.action_restart_book -> {
            confirmRestart()
            true
        }
        R.id.action_add_to_recent -> {
            addToRecent()
            true
        }
        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
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

    // Reading controls (roadmap P2.7)

    internal val tapZones: TapZones get() = settings.tapZones

    internal fun setTapZones(zones: TapZones) {
        settings.tapZones = zones
        invalidateOptionsMenu()
    }

    /** The selected text as last fetched from the navigator (see [SelectionActionMode]). */
    internal var rememberedSelection: String? = null
        private set

    /** The place of [rememberedSelection] (roadmap P9.2: a highlight needs the locator, not just the text). */
    private var rememberedSelectionLocator: Locator? = null

    internal fun rememberSelection() {
        val fragment = navigator ?: return
        lifecycleScope.launch {
            selectedLocator(fragment)?.let {
                rememberedSelection = it.text.highlight
                rememberedSelectionLocator = it
            }
        }
    }

    /** The current selection's locator when it has text: the page's locator plus the selected text and its context. */
    private suspend fun selectedLocator(fragment: EpubNavigatorFragment): Locator? =
        runCatching { fragment.currentSelection()?.locator }.getOrNull()?.takeIf { !it.text.highlight.isNullOrBlank() }

    /**
     * Copies, shares, web-searches, highlights or annotates the selection; false when [itemId] is
     * not one of the selection items.
     */
    internal fun performSelectionAction(itemId: Int): Boolean {
        when (itemId) {
            R.id.selection_copy -> withSelection(::copyToClipboard)
            R.id.selection_share -> withSelection { launchOrToast(SelectionActions.shareIntent(it)) }
            R.id.selection_web_search -> withSelection { launchOrToast(SelectionActions.webSearchIntent(it)) }
            // The selection handles would cover the new highlight (or the editor's page): drop them once the locator is taken.
            R.id.selection_highlight -> withSelectionLocator {
                addAnnotation(it, settings.annotationStyle, settings.annotationColor, note = null)
                navigator?.clearSelection()
            }
            R.id.selection_note -> withSelectionLocator {
                AnnotationDialog.showNew(supportFragmentManager, it, settings.annotationStyle, settings.annotationColor)
                navigator?.clearSelection()
            }
            else -> return false
        }
        return true
    }

    /** Hands the selection to one of the system's text processors (translate, define, ...). */
    internal fun processSelection(target: ProcessTextTarget): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        withSelection { launchOrToast(SelectionActions.processTextIntent(target, it)) }
        return true
    }

    private fun withSelection(action: (String) -> Unit) = withSelectionLocator { locator -> locator.text.highlight?.let(action) }

    /**
     * Runs [action] with the live selection when the page still has one, else with the one
     * remembered while the toolbar was up. `Main.immediate` issues the page query before the
     * navigator's own post-click clearing runs.
     */
    private fun withSelectionLocator(action: (Locator) -> Unit) {
        val fragment = navigator
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            val locator = (if (fragment != null) selectedLocator(fragment) else null) ?: rememberedSelectionLocator ?: return@launch
            action(locator)
        }
    }

    private fun copyToClipboard(text: String) {
        getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(SelectionActions.MIME_TEXT, text))
        // Android 13+ shows its own confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show()
    }

    private fun launchOrToast(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.text_no_app_for_action, Toast.LENGTH_SHORT).show()
        }
    }

    // Images (roadmap P2.7)

    /** Opens the tapped image full screen. Only reflowable pages report the tapped element. */
    private fun showImage(image: Content.ImageElement) {
        ImageViewerDialog.show(supportFragmentManager, image.embeddedLink.url().toString(), image.text)
    }

    internal val imageViewer: ImageViewerDialog?
        get() = supportFragmentManager.findFragmentByTag(ImageViewerDialog.TAG) as? ImageViewerDialog

    // Bookmarks (roadmap P2.6)

    internal val bookmarks: StateFlow<List<Bookmark>> get() = model.bookmarks

    private fun updateCurrentPage(page: PageLocation?) {
        currentPage = page
        refreshCurrentBookmark()
    }

    private fun refreshCurrentBookmark() {
        val bookmark = BookmarkPolicy.current(model.bookmarks.value, currentPage)
        if (bookmark?.id != _currentBookmark.value?.id) {
            _currentBookmark.value = bookmark
            invalidateOptionsMenu()
        }
    }

    /** The toolbar icon: removes the page's bookmark when it has one, adds one otherwise. */
    internal fun toggleBookmark() {
        val current = _currentBookmark.value
        if (current != null) removeBookmark(current.id) else addBookmark()
    }

    /**
     * Bookmarks the page on screen: the navigator's locator carries the page and the chapter
     * title, the first visible element (reflowable only) contributes the anchor and the excerpt.
     */
    internal fun addBookmark() {
        val fragment = navigator?.takeIf { it.isAdded && navigatorReady } ?: return
        val publication = model.publication ?: return
        lifecycleScope.launch {
            val current = fragment.currentLocator.value
            val visible = if (fixedLayout) null else runCatching { fragment.firstVisibleElementLocator() }.getOrNull()
            val href = current.href.toString()
            val chapter = current.title?.takeIf { it.isNotBlank() }
                ?: TocSheet.chapterTitle(publication, href)
                ?: current.locations.position?.takeIf { fixedLayout }?.let { getString(R.string.text_progress_page_of, it, model.positionCount.value) }
                ?: href.substringAfterLast('/')
            val highlight = visible?.text?.highlight
            val locator = BookmarkPolicy.composeLocator(current.toJSON(), visible?.locations?.cssSelector, highlight)
            when (val result = model.addBookmark(locator, chapter, BookmarkPolicy.snippet(highlight))) {
                is BookmarkAddResult.Added -> Toast.makeText(this@EpubReaderActivity, R.string.text_bookmark_added, Toast.LENGTH_SHORT).show()
                is BookmarkAddResult.Full -> Toast.makeText(this@EpubReaderActivity, getString(R.string.text_bookmark_full, result.limit), Toast.LENGTH_SHORT).show()
            }
        }
    }

    internal fun removeBookmark(id: Long) {
        if (model.removeBookmark(id)) Toast.makeText(this, R.string.text_bookmark_removed, Toast.LENGTH_SHORT).show()
    }

    internal fun clearBookmarks() = model.clearBookmarks()

    internal fun openBookmark(bookmark: Bookmark) {
        Locator.fromJSON(bookmark.locator)?.let { jumpTo(it) }
    }

    internal fun showBookmarks() {
        if (model.publication == null) return
        BookmarkSheet.show(supportFragmentManager)
    }

    // Highlights and notes (roadmap P9.2)

    internal val annotations: StateFlow<List<BookAnnotation>> get() = model.annotations

    /** The reading order's hrefs, for the panel's chapter grouping. */
    internal val readingOrderHrefs: List<String>
        get() = model.publication?.readingOrder?.map { it.href.toString() }.orEmpty()

    /**
     * Highlights [locator] (a selection) with [style] and [color], optionally with a [note];
     * highlighting the same place again restyles the existing highlight. The choice becomes the
     * default of the next quick highlight.
     */
    internal fun addAnnotation(locator: Locator, style: String, color: Int, note: String?) {
        val publication = model.publication ?: return
        val chapter = locator.title?.takeIf { it.isNotBlank() } ?: TocSheet.chapterTitle(publication, locator.href.toString())
        rememberAnnotationChoice(style, color)
        lifecycleScope.launch {
            when (val result = model.addAnnotation(locator.toJSON(), style, color, note, chapter)) {
                is AnnotationAddResult.Added -> Toast.makeText(this@EpubReaderActivity, R.string.text_annotation_added, Toast.LENGTH_SHORT).show()
                is AnnotationAddResult.Restyled -> Toast.makeText(this@EpubReaderActivity, R.string.text_annotation_restyled, Toast.LENGTH_SHORT).show()
                is AnnotationAddResult.Full -> Toast.makeText(this@EpubReaderActivity, getString(R.string.text_annotation_full, result.limit), Toast.LENGTH_SHORT).show()
                AnnotationAddResult.Invalid -> Toast.makeText(this@EpubReaderActivity, R.string.text_annotation_invalid, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Stores the editor's changes to the highlight with [id]; a row deleted meanwhile is left alone. */
    internal fun saveAnnotation(id: Long, style: String, color: Int, note: String?) {
        rememberAnnotationChoice(style, color)
        val current = model.annotations.value.firstOrNull { it.id == id } ?: return
        lifecycleScope.launch { model.updateAnnotation(current.copy(style = style, color = color, note = note)) }
    }

    private fun rememberAnnotationChoice(style: String, color: Int) {
        settings.annotationStyle = style
        settings.annotationColor = color
    }

    internal fun deleteAnnotation(id: Long) {
        lifecycleScope.launch {
            if (model.deleteAnnotation(id)) Toast.makeText(this@EpubReaderActivity, R.string.text_annotation_deleted, Toast.LENGTH_SHORT).show()
        }
    }

    internal fun clearAnnotations() {
        lifecycleScope.launch { model.clearAnnotations() }
    }

    internal fun openAnnotation(annotation: BookAnnotation) {
        locatorOf(annotation)?.let { jumpTo(it) }
    }

    internal fun editAnnotation(annotation: BookAnnotation) {
        AnnotationDialog.showEdit(supportFragmentManager, annotation)
    }

    internal fun showAnnotations() {
        if (model.publication == null) return
        AnnotationSheet.show(supportFragmentManager)
    }

    // Export (roadmap P9.3)

    /** The Markdown export of the open book's highlights, in the panel's order, with local times. */
    internal fun annotationMarkdown(): String {
        val metadata = model.publication?.metadata
        return AnnotationMarkdown.render(
            title = metadata?.title,
            authors = metadata?.authors?.map { it.name }.orEmpty(),
            annotations = model.annotations.value,
            readingOrder = readingOrderHrefs,
            formatTime = { DateUtils.formatDateTime(this, it, EXPORT_TIME_FLAGS) },
        )
    }

    /** Offers the two ways out: the share sheet with the text, or a Markdown file placed with the document picker. */
    internal fun exportAnnotations() {
        if (model.annotations.value.isEmpty()) {
            Toast.makeText(this, R.string.text_annotation_export_empty, Toast.LENGTH_SHORT).show()
            return
        }
        annotationExportDialog?.dismiss()
        annotationExportDialog = AlertDialog.Builder(this)
            .setTitle(R.string.text_annotation_export)
            .setItems(arrayOf(getString(R.string.text_annotation_export_share), getString(R.string.text_annotation_export_save))) { _, which ->
                if (which == 0) shareAnnotations() else saveAnnotations()
            }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .setOnDismissListener { if (annotationExportDialog === it) annotationExportDialog = null }
            .show()
    }

    internal fun shareAnnotations() {
        launchOrToast(AnnotationExport.shareIntent(annotationMarkdown(), model.publication?.metadata?.title))
    }

    internal fun saveAnnotations() {
        try {
            annotationSaver.launch(AnnotationMarkdown.fileName(model.publication?.metadata?.title))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.text_no_file_picker, Toast.LENGTH_SHORT).show()
        }
    }

    /** Writes the export to [uri] off the main thread and says whether it landed. */
    internal fun writeAnnotationExport(uri: Uri) {
        val markdown = annotationMarkdown()
        lifecycleScope.launch {
            val written = withContext(Dispatchers.IO) { runCatching { AnnotationExport.write(contentResolver, uri, markdown) }.isSuccess }
            Toast.makeText(
                this@EpubReaderActivity,
                if (written) R.string.text_annotation_export_saved else R.string.text_annotation_export_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    /** A tap on the decoration [decorationId] of the highlights group opens the editor of its highlight. */
    internal fun onAnnotationDecorationActivated(decorationId: String): Boolean {
        val id = decorationId.removePrefix("$ANNOTATION_DECORATIONS-").toLongOrNull() ?: return false
        val annotation = model.annotations.value.firstOrNull { it.id == id } ?: return false
        editAnnotation(annotation)
        return true
    }

    private fun locatorOf(annotation: BookAnnotation): Locator? =
        runCatching { Locator.fromJSON(JSONObject(annotation.locator)) }.getOrNull()

    /** Every highlight of the book as one decoration group; Readium diffs against the last batch. */
    private fun applyAnnotationDecorations(annotations: List<BookAnnotation>) {
        val fragment = navigator?.takeIf { it.isAdded && it.view != null } ?: return
        val decorations = annotations.mapNotNull { annotation ->
            val locator = locatorOf(annotation) ?: return@mapNotNull null
            val tint = AnnotationColors.normalize(annotation.color)
            Decoration(
                id = "$ANNOTATION_DECORATIONS-${annotation.id}",
                locator = locator,
                style = if (annotation.style == AnnotationStyle.UNDERLINE) Decoration.Style.Underline(tint) else Decoration.Style.Highlight(tint),
            )
        }
        lifecycleScope.launch {
            decorationMutex.withLock {
                if (fragment.isAdded && fragment.view != null) fragment.applyDecorations(decorations, ANNOTATION_DECORATIONS)
            }
        }
    }

    // Full-text search (roadmap P2.5)

    internal val searchState: StateFlow<SearchState> get() = model.search

    internal fun showSearch() {
        if (model.publication == null) return
        SearchSheet.show(supportFragmentManager)
    }

    internal fun submitSearch(query: String) = model.search(query)

    internal fun loadMoreSearchResults() = model.loadMoreSearchResults()

    internal fun cancelSearch() = model.cancelSearch()

    /** Shows the [index]th hit: the reader scrolls to it and decorates it. */
    internal fun openSearchResult(index: Int) {
        val locator = model.search.value.results.getOrNull(index) ?: return
        model.selectSearchResult(index)
        jumpTo(locator)
    }

    /** The bar's previous / next: moves the active hit by [delta] within the loaded results. */
    internal fun stepSearchResult(delta: Int) {
        val current = model.search.value.activeIndex ?: return
        openSearchResult(current + delta)
    }

    /** Leaves search mode: the bar and the decorations go, the results are dropped. */
    internal fun closeSearch() = model.closeSearch()

    private fun onSearchState(state: SearchState) {
        chrome.showSearchPosition(if (state.active) state.activeIndex else null, state.results.size)
        applySearchDecorations(state)
    }

    /**
     * Decorates the open hit in the same amber as the panel's snippets, or clears the group. Only
     * the active hit is decorated: Readium anchors and lays out every decoration of a page one by
     * one, and a chapter with dozens of hits took tens of seconds to settle on a 2017 phone when
     * all of them were decorated. The id carries the index so that moving to another hit removes
     * the old decoration from its page and adds the new one to its own. Fixed layouts render no
     * decorations in Readium 3.4.0; the jump still works there.
     */
    private fun applySearchDecorations(state: SearchState) {
        val fragment = navigator?.takeIf { it.isAdded && it.view != null } ?: return
        val decorations = state.activeIndex?.takeIf { state.active }?.let { index ->
            listOf(
                Decoration(
                    id = "$SEARCH_DECORATIONS-$index",
                    locator = state.results[index],
                    style = Decoration.Style.Highlight(tint = ContextCompat.getColor(this, R.color.color_secondary), isActive = true),
                ),
            )
        } ?: emptyList()
        lifecycleScope.launch {
            decorationMutex.withLock {
                if (fragment.isAdded && fragment.view != null) fragment.applyDecorations(decorations, SEARCH_DECORATIONS)
            }
        }
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

    /**
     * Roadmap P4.2 / D4: keeps a book that came through `ACTION_VIEW` only when its sender allowed
     * the grant to persist; otherwise the book opens this once and the user is told why it is
     * not listed. Returns whether the book joined the list.
     */
    internal fun addToRecent(): Boolean {
        val request = model.request ?: return false
        if (request.entry != ReaderEntry.EXTERNAL || model.publication == null) return false
        val kept = runCatching {
            contentResolver.takePersistableUriPermission(request.documentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        if (!kept) {
            Toast.makeText(this, R.string.text_launcher_cannot_keep_access, Toast.LENGTH_LONG).show()
            return false
        }
        model.addToRecent(request) { evicted ->
            runCatching { contentResolver.releasePersistableUriPermission(evicted, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        Toast.makeText(this, R.string.text_added_to_recent, Toast.LENGTH_SHORT).show()
        invalidateOptionsMenu()
        return true
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

    override fun onStart() {
        super.onStart()
        // Roadmap P4.3: the settings page may have edited the shared preference files meanwhile.
        model.reloadPreferences()
        model.tts.reloadPreferences()
        model.hostSession?.setVisible(true)
    }

    override fun onStop() {
        model.hostSession?.setVisible(false)
        super.onStop()
    }

    /**
     * Roadmap P6.2: the host launches the reader with `FLAG_ACTIVITY_SINGLE_TOP`, so when this
     * reader is already on top of its task (for example after a script left it open, D32) the
     * activity manager delivers the `EPUB_READER_OPEN` launch here instead of creating an instance.
     * The new session opens in a fresh reader and this one ends like a replaced reader; without
     * this hook the session would wait unclaimed until its 60 s timeout.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != EpubActions.READER_ACTIVITY_ACTION) return
        finish()
        startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onDestroy() {
        model.hostSession?.detachController(this)
        tableOfContentsDialog?.dismiss()
        tableOfContentsDialog = null
        dismissLinkDialogs()
        ttsDialog?.dismiss()
        ttsDialog = null
        annotationExportDialog?.dismiss()
        annotationExportDialog = null
        super.onDestroy()
    }

    // Host reader session (roadmap P5.3): the session posts these on the main thread.

    override fun goTo(locator: Locator) = jumpTo(locator)

    override fun navigate(direction: Int) {
        when (direction) {
            EpubContract.DIRECTION_NEXT_PAGE -> perform(PageTurnAction.NEXT)
            EpubContract.DIRECTION_PREVIOUS_PAGE -> perform(PageTurnAction.PREVIOUS)
            EpubContract.DIRECTION_NEXT_CHAPTER -> stepChapter(+1)
            EpubContract.DIRECTION_PREVIOUS_CHAPTER -> stepChapter(-1)
        }
    }

    override fun finishReader() = finish()

    /** The reading-order neighbour of the resource on screen; the ends stay put. */
    private fun stepChapter(delta: Int) {
        val publication = model.publication ?: return
        val current = navigator?.takeIf { it.isAdded }?.currentLocator?.value?.href?.toString()
            ?: model.lastLocator?.href?.toString()
            ?: return
        val order = publication.readingOrder
        val index = order.indexOfFirst { it.href.toString() == current }
        if (index < 0) return
        val link = order.getOrNull(index + delta) ?: return
        jumpTo(link)
    }

    // Links (roadmap P2.7)

    internal val externalLinksDirect: Boolean get() = settings.externalLinksDirect

    internal fun setExternalLinksDirect(direct: Boolean) {
        settings.externalLinksDirect = direct
        invalidateOptionsMenu()
    }

    /** The note the open dialog shows, or null when none is showing. */
    internal val noteText: CharSequence?
        get() = noteDialog?.takeIf { it.isShowing }?.findViewById<TextView>(android.R.id.message)?.text

    internal fun dismissLinkDialogs() {
        noteDialog?.dismiss()
        noteDialog = null
        externalLinkDialog?.dismiss()
        externalLinkDialog = null
    }

    /**
     * HyperlinkNavigator.Listener: a footnote (the navigator hands over its content) opens in a
     * dialog; any other in-book link is followed here so the place it was followed from goes on
     * the back stack. Readium 3.4 resolves the link to its resource before this call, so the
     * jump lands at the resource's start even when the link named an anchor.
     */
    override fun shouldFollowInternalLink(link: Link, context: HyperlinkNavigator.LinkContext?): Boolean {
        if (context is HyperlinkNavigator.FootnoteContext) {
            showNote(context.noteContent)
            return false
        }
        val target = model.publication?.locatorFromLink(link) ?: return true
        navigator?.currentLocator?.value?.let { model.linkHistory.push(it) }
        linkBackCallback.isEnabled = true
        jumpTo(target)
        return false
    }

    /** Back to where the newest in-book link was followed from; false when there is no such place. */
    internal fun returnFromLink(): Boolean {
        val origin = model.linkHistory.pop() ?: return false
        linkBackCallback.isEnabled = !model.linkHistory.isEmpty()
        jumpTo(origin)
        return true
    }

    private fun showNote(html: String) {
        noteDialog?.dismiss()
        noteDialog = AlertDialog.Builder(this)
            .setTitle(R.string.text_note)
            .setMessage(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT))
            .setPositiveButton(R.string.dialog_button_close, null)
            .setOnDismissListener { if (noteDialog === it) noteDialog = null }
            .show()
    }

    /**
     * HyperlinkNavigator.Listener: links that leave the book follow roadmap decision D25. The
     * navigator only reports hierarchical URLs here; `mailto:` and `tel:` links never arrive
     * (Readium 3.4 leaves them to the web view), so their branch is reached through
     * [openExternalLink] alone.
     */
    override fun onExternalLinkActivated(url: AbsoluteUrl) = openExternalLink(url.toString())

    internal fun openExternalLink(url: String) {
        when (val link = LinkPolicy.classifyExternal(url)) {
            is ExternalLink.Web -> if (settings.externalLinksDirect) openExternal(link.url) else confirmExternal(link.url)
            is ExternalLink.System -> openExternal(link.url)
            is ExternalLink.Rejected -> Toast.makeText(this, R.string.text_cannot_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmExternal(target: String) {
        externalLinkDialog?.dismiss()
        externalLinkDialog = AlertDialog.Builder(this)
            .setMessage(target)
            .setPositiveButton(R.string.dialog_button_confirm) { _, _ -> openExternal(target) }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .setOnDismissListener { if (externalLinkDialog === it) externalLinkDialog = null }
            .show()
    }

    private fun openExternal(target: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, target.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.text_cannot_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    // Read-aloud (roadmap P3)

    internal val ttsStatus: StateFlow<TtsStatus> get() = model.tts.status

    internal val ttsLocation: StateFlow<TtsLocation?> get() = model.tts.location

    internal val ttsPreferences: StateFlow<AndroidTtsPreferences> get() = model.tts.preferences

    internal val ttsSession: TtsSession? get() = model.tts.session.value

    /** The locator of the sentence currently highlighted, null when none is. */
    internal val ttsHighlighted: Locator? get() = ttsHighlight

    /** Test hook: whether the chrome shows the read-aloud bar (when not immersive). */
    internal val chromeShowsReadAloud: Boolean get() = chrome.readAloudVisible

    /** The last one-off read-aloud outcome, for the device tests. */
    internal var lastTtsEvent: TtsEvent? = null
        private set

    /**
     * Starts reading from the first visible element. Android 13+ asks for the notification
     * permission once beforehand (the media notification needs it); refusing keeps read-aloud
     * working without the notification controls (roadmap D15).
     */
    internal fun startReadAloud() {
        if (model.publication == null || model.tts.isActive) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !settings.readAloudNotificationAsked
        ) {
            settings.readAloudNotificationAsked = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        launchReadAloud()
    }

    private fun launchReadAloud() {
        val publication = model.publication ?: return
        lifecycleScope.launch {
            val fragment = navigator?.takeIf { it.isAdded && navigatorReady }
            val locator = fragment?.let { runCatching { it.firstVisibleElementLocator() }.getOrNull() }
                ?: fragment?.currentLocator?.value
                ?: model.lastLocator
            model.tts.start(publication, model.bookKey, publication.metadata.title, locator)
            // Roadmap P4.3: the settings page picks the timer every read-aloud session starts with.
            settings.readAloudSleepTimer.takeIf { it != SleepTimer.OFF }?.let { setSleepTimer(it) }
        }
    }

    internal fun readAloudTogglePlayPause() = model.tts.togglePlayPause()

    internal fun readAloudPrevious() = model.tts.previous()

    internal fun readAloudNext() = model.tts.next()

    internal fun stopReadAloud() = model.tts.stop()

    // Sleep timer, keep-screen-on and background read-aloud (roadmap P3 / D26)

    internal val ttsSleepTimer: StateFlow<SleepTimer> get() = model.tts.sleepTimer

    /** Arms the sleep timer of the running voice; [durationMillis] lets tests shorten a fixed span. */
    internal fun setSleepTimer(timer: SleepTimer, durationMillis: Long? = SleepTimerPolicy.durationMillis(timer)) =
        model.tts.armSleepTimer(timer, durationMillis)

    internal val readAloudKeepScreenOn: Boolean get() = settings.readAloudKeepScreenOn

    internal fun setReadAloudKeepScreenOn(keep: Boolean) {
        settings.readAloudKeepScreenOn = keep
        applyKeepScreenOn(model.tts.status.value)
    }

    internal val readAloudInBackground: Boolean get() = settings.readAloudInBackground

    internal fun setReadAloudInBackground(background: Boolean) {
        settings.readAloudInBackground = background
    }

    /** Test hook: whether the window currently holds the screen on. */
    internal val keepsScreenOn: Boolean
        get() = window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0

    private fun applyKeepScreenOn(status: TtsStatus) {
        if (status != TtsStatus.IDLE && settings.readAloudKeepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    internal fun updateTtsPreferences(transform: (AndroidTtsPreferences) -> AndroidTtsPreferences) =
        model.tts.updatePreferences(transform)

    private fun showTtsSettings() {
        if (!model.tts.isActive) return
        TtsSheet.show(supportFragmentManager)
    }

    /** A manual page turn or jump ends read-aloud: the voice would otherwise drag the page back. */
    private fun stopReadAloudForNavigation() {
        if (model.tts.isActive) model.tts.stop()
    }

    private fun onTtsStatus(status: TtsStatus) {
        chrome.showReadAloud(status)
        applyKeepScreenOn(status)
        if (status == TtsStatus.IDLE) {
            ttsFollowJob?.cancel()
            pendingTtsFollow = null
            applyTtsDecoration(null)
        }
        invalidateOptionsMenu()
    }

    private fun onTtsLocation(location: TtsLocation?) {
        applyTtsDecoration(location?.locator)
        if (location != null) followTts(location.locator)
    }

    /**
     * Turns the page to the sentence being spoken, at most once a second so a fast voice does not
     * make the navigator thrash; only while playing, so a paused voice leaves browsing alone.
     */
    private fun followTts(locator: Locator) {
        pendingTtsFollow = locator
        if (ttsFollowJob?.isActive == true) return
        val wait = (lastTtsFollowAt + TTS_FOLLOW_INTERVAL_MILLIS - SystemClock.uptimeMillis()).coerceAtLeast(0L)
        ttsFollowJob = lifecycleScope.launch {
            delay(wait)
            val target = pendingTtsFollow ?: return@launch
            pendingTtsFollow = null
            if (navigatorReady && model.tts.status.value == TtsStatus.PLAYING) {
                lastTtsFollowAt = SystemClock.uptimeMillis()
                navigator?.go(target, animated = false)
            }
        }
    }

    /** One highlight decoration for the spoken sentence (group `tts`); fixed layouts render none. */
    private fun applyTtsDecoration(locator: Locator?) {
        ttsHighlight = locator
        val fragment = navigator?.takeIf { it.isAdded && it.view != null } ?: return
        val decorations = locator?.let {
            listOf(
                Decoration(
                    id = TTS_DECORATIONS,
                    locator = it,
                    style = Decoration.Style.Highlight(tint = ContextCompat.getColor(this, R.color.color_primary)),
                ),
            )
        }.orEmpty()
        lifecycleScope.launch {
            decorationMutex.withLock {
                if (fragment.isAdded && fragment.view != null) fragment.applyDecorations(decorations, TTS_DECORATIONS)
            }
        }
    }

    private fun onTtsEvent(event: TtsEvent) {
        lastTtsEvent = event
        when (event) {
            TtsEvent.Ended -> Toast.makeText(this, R.string.text_read_aloud_ended, Toast.LENGTH_SHORT).show()
            TtsEvent.SleepTimerEnded -> Toast.makeText(this, R.string.text_read_aloud_sleep_ended, Toast.LENGTH_SHORT).show()
            TtsEvent.NoContent -> Toast.makeText(this, R.string.text_read_aloud_unavailable, Toast.LENGTH_LONG).show()
            TtsEvent.NoEngine ->
                showTtsDialog(getString(R.string.text_read_aloud_no_engine), R.string.text_read_aloud_open_settings) { openTtsSettings() }
            is TtsEvent.MissingVoiceData -> showTtsDialog(
                getString(R.string.text_read_aloud_missing_voice, displayLanguage(event.language)),
                R.string.text_read_aloud_install_voice,
            ) { installTtsVoice() }
            is TtsEvent.Failed -> Toast.makeText(
                this,
                when (event.kind) {
                    TtsEvent.Failed.Kind.NETWORK -> R.string.text_read_aloud_failed_network
                    TtsEvent.Failed.Kind.ENGINE -> R.string.text_read_aloud_failed_engine
                    TtsEvent.Failed.Kind.CONTENT -> R.string.text_read_aloud_failed_content
                },
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun displayLanguage(tag: String): String =
        Locale.forLanguageTag(tag).displayName.takeIf { it.isNotBlank() } ?: tag

    private fun showTtsDialog(message: String, actionTitle: Int, action: () -> Unit) {
        ttsDialog?.dismiss()
        ttsDialog = AlertDialog.Builder(this)
            .setTitle(R.string.text_read_aloud)
            .setMessage(message)
            .setPositiveButton(actionTitle) { _, _ -> action() }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .setOnDismissListener { if (ttsDialog === it) ttsDialog = null }
            .show()
    }

    /** The system's text-to-speech settings page (a toast when the ROM has none). */
    internal fun openTtsSettings() = launchOrToast(Intent(ACTION_TTS_SETTINGS))

    /** The engine's voice-data installer (Readium starts it when an installer activity exists). */
    internal fun installTtsVoice() {
        runCatching { AndroidTtsEngine.requestInstallVoice(this) }
            .onFailure { Toast.makeText(this, R.string.text_no_app_for_action, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        internal const val NAVIGATOR_TAG = "readium-epub-navigator"
        internal const val SEARCH_DECORATIONS = "search"
        internal const val TTS_DECORATIONS = "tts"
        internal const val ANNOTATION_DECORATIONS = "annotations"
        private const val EXPORT_TIME_FLAGS = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR

        /** Roadmap D26: the read-aloud notification reopens the reader on the book the parked voice reads. */
        internal const val ACTION_RESUME_READ_ALOUD = "io.github.supermonster003.autojs6.plugin.readium.epub.reader.RESUME_READ_ALOUD"
        private const val TTS_FOLLOW_INTERVAL_MILLIS = 1000L
        private const val ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS"
        private const val STATE_LOCATOR = "locator"
        private const val STATE_IMMERSIVE = "immersive"

        // Font MIME types differ between providers; the wildcard keeps unlabeled files pickable, the signature decides.
        private val FONT_MIME_TYPES = arrayOf(
            "font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-opentype", "application/font-sfnt", "*/*",
        )
    }
}
