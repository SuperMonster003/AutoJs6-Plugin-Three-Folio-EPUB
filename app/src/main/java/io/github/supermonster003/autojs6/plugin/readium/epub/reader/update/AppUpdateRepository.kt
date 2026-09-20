package io.github.supermonster003.autojs6.plugin.readium.epub.reader.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** What a check found: the latest release (null when the repository has none yet) or why it failed. */
internal sealed class UpdateFetchResult {
    data class Success(val release: ReleaseInfo?) : UpdateFetchResult()
    data class Failure(val reason: UpdateFailure, val detail: String? = null) : UpdateFetchResult()
}

internal enum class UpdateFailure { NETWORK, HTTP, TOO_LARGE, MALFORMED, NOT_HTTPS }

/** The one call the update flow needs; tests substitute a fake. */
internal fun interface UpdateSource {
    suspend fun fetchLatest(): UpdateFetchResult
}

/**
 * The GitHub Releases API client (roadmap P4.3 / D28): one `GET .../releases/latest` over HTTPS
 * only, with connect and read timeouts, no redirects (a redirect could leave HTTPS), a 256 KB
 * cap on the answer and cancellation through `disconnect()` from a watcher coroutine, because
 * `HttpURLConnection` blocks. No APK is ever downloaded; the dialog opens the release page.
 */
internal class AppUpdateRepository(
    private val endpoint: URL = DEFAULT_ENDPOINT,
    private val connectTimeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
) : UpdateSource {

    override suspend fun fetchLatest(): UpdateFetchResult = withContext(Dispatchers.IO) {
        if (!endpoint.protocol.equals("https", ignoreCase = true)) return@withContext UpdateFetchResult.Failure(UpdateFailure.NOT_HTTPS)
        val connection = runCatching { endpoint.openConnection() as HttpURLConnection }.getOrElse {
            return@withContext UpdateFetchResult.Failure(UpdateFailure.NETWORK, it.message)
        }
        connection.connectTimeout = connectTimeoutMillis
        connection.readTimeout = readTimeoutMillis
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", USER_AGENT)
        // Cancelling the caller cancels this watcher first, whose cleanup unblocks the read below.
        val watcher = launch { try { awaitCancellation() } finally { connection.disconnect() } }
        try {
            val code = connection.responseCode
            when {
                code == HttpURLConnection.HTTP_NOT_FOUND -> UpdateFetchResult.Success(null)
                code != HttpURLConnection.HTTP_OK -> UpdateFetchResult.Failure(UpdateFailure.HTTP, code.toString())
                else -> {
                    val body = connection.inputStream.use { readCapped(it, MAX_BODY_BYTES) }
                        ?: return@withContext UpdateFetchResult.Failure(UpdateFailure.TOO_LARGE)
                    val release = ReleaseInfoCodec.fromGitHub(String(body, Charsets.UTF_8))
                        ?: return@withContext UpdateFetchResult.Failure(UpdateFailure.MALFORMED)
                    UpdateFetchResult.Success(release)
                }
            }
        } catch (e: IOException) {
            UpdateFetchResult.Failure(UpdateFailure.NETWORK, e.message)
        } finally {
            watcher.cancel()
            connection.disconnect()
        }
    }

    /** The bytes of [input], or null once they exceed [limit]. */
    private fun readCapped(input: java.io.InputStream, limit: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) return out.toByteArray()
            if (out.size() + read > limit) return null
            out.write(buffer, 0, read)
        }
    }

    companion object {
        const val REPOSITORY = "SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader"
        const val RELEASES_PAGE = "https://github.com/$REPOSITORY/releases"
        val DEFAULT_ENDPOINT: URL = URL("https://api.github.com/repos/$REPOSITORY/releases/latest")
        const val DEFAULT_TIMEOUT_MILLIS = 10_000
        const val MAX_BODY_BYTES = 256 * 1024
        const val USER_AGENT = "AutoJs6-Plugin-Readium-EPUB-Reader"
    }
}
