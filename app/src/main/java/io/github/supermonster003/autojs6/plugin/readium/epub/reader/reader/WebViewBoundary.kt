package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * The boundary every Readium page WebView gets on top of what Readium sets itself (roadmap P7.2 / D6):
 * JavaScript stays on and the book is served from Readium's in-memory `https://readium_package/` and
 * `https://readium_assets/` hosts, but the WebView may not read the file system or content providers,
 * and the two file-URL cross-origin switches stay off.
 *
 * The reader registers the boundary once per Activity, recursively, so the page fragments Readium creates
 * in the navigator's child fragment manager are covered as soon as their view exists. Readium starts the
 * first load in `onCreateView`; the flags are consulted per request, so they are in place before the page
 * content can ask for anything outside the two hosts. `docs/dev/security-boundaries.md` records the review.
 */
object WebViewBoundary : FragmentManager.FragmentLifecycleCallbacks() {

    fun install(fragmentManager: FragmentManager) {
        fragmentManager.registerFragmentLifecycleCallbacks(this, true)
    }

    override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
        fence(v)
    }

    /** Applies the boundary to every WebView under [view] and returns how many it found. */
    fun fence(view: View): Int {
        if (view is WebView) {
            apply(view.settings)
            return 1
        }
        if (view !is ViewGroup) return 0
        var count = 0
        for (index in 0 until view.childCount) count += fence(view.getChildAt(index))
        return count
    }

    @Suppress("DEPRECATION")
    fun apply(settings: WebSettings) {
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
    }

    /** True when [settings] carry the boundary; the instrumentation asserts it on every page WebView. */
    @Suppress("DEPRECATION")
    fun holds(settings: WebSettings): Boolean =
        !settings.allowFileAccess && !settings.allowContentAccess &&
            !settings.allowFileAccessFromFileURLs && !settings.allowUniversalAccessFromFileURLs
}
