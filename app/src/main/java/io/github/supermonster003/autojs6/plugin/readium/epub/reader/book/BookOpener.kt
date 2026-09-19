package io.github.supermonster003.autojs6.plugin.readium.epub.reader.book

import android.content.Context
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.FallbackContentProtection
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.data.CompositeContainer
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** Why a book could not be opened; `message` is safe to show to the user. */
sealed class BookOpenError(val message: String, val cause: Error? = null) {
    class NotAnEpub(val mediaType: String?) :
        BookOpenError("Not an EPUB container" + (mediaType?.let { " ($it)" } ?: ""))

    class Retrieve(cause: Error) : BookOpenError(cause.message, cause)

    class Open(cause: Error) : BookOpenError(cause.message, cause)

    /** LCP / ADEPT-marked books: the fallback protection opens them as restricted, so they are refused. */
    class Protected(cause: Error?) :
        BookOpenError(cause?.message ?: "The book is DRM-protected and cannot be opened", cause)
}

/**
 * Opens a [Resource] as a Readium [Publication] (roadmap D2 / D11). The whole Readium stack is
 * wired once per instance: the asset retriever sniffs the container, the default parser handles
 * EPUB 2 / EPUB 3, and the opener applies the fallback content protection (an LCP-marked book
 * fails with a clear error instead of rendering garbage).
 */
class BookOpener(context: Context) {

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.applicationContext.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context.applicationContext,
            httpClient,
            assetRetriever,
            pdfFactory = null,
        ),
        contentProtections = listOf(FallbackContentProtection()),
    )

    private val epubHints = FormatHints(
        mediaType = MediaType.EPUB,
        fileExtension = FileExtension("epub"),
    )

    /**
     * [extraResources] (the imported fonts, roadmap P2.2) are composed after the book's own
     * container, so the navigator can fetch them from the publication host without any copy.
     */
    suspend fun open(resource: Resource, extraResources: Container<Resource>? = null): Try<Publication, BookOpenError> {
        val asset = assetRetriever.retrieve(resource, epubHints)
            .getOrElse { return Try.failure(BookOpenError.Retrieve(it)) }
        if (!asset.format.conformsTo(Specification.Epub)) {
            val mediaType = asset.format.mediaType.toString()
            asset.close()
            return Try.failure(BookOpenError.NotAnEpub(mediaType))
        }
        val publication = publicationOpener.open(
            asset,
            allowUserInteraction = false,
            onCreatePublication = {
                if (extraResources != null) container = CompositeContainer(container, extraResources)
            },
        ).getOrElse { return Try.failure(BookOpenError.Open(it)) }
        if (publication.isRestricted) {
            val error = publication.protectionError
            publication.close()
            return Try.failure(BookOpenError.Protected(error))
        }
        return Try.success(publication)
    }
}
