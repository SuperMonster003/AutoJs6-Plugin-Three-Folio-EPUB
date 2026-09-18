package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import androidx.lifecycle.ViewModel
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.PfdResource
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Holds the opened book across configuration changes. The publication lives only in memory: after
 * process death the Activity reopens the book from its Intent instead of restoring fragments.
 */
class EpubReaderViewModel : ViewModel() {

    var resource: PfdResource? = null
    var publication: Publication? = null
    var navigatorFactory: EpubNavigatorFactory? = null
    var lastLocator: Locator? = null
    var openError: String? = null

    fun adopt(resource: PfdResource, publication: Publication) {
        release()
        this.resource = resource
        this.publication = publication
        navigatorFactory = EpubNavigatorFactory(publication)
        openError = null
    }

    private fun release() {
        navigatorFactory = null
        publication?.close()
        publication = null
        resource?.close()
        resource = null
    }

    override fun onCleared() {
        release()
    }
}
