package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import org.autojs.plugin.epub.api.EpubActions
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Token rules of the two-step reader launch (roadmap P5.3, decision D12): `openReader` mints a
 * random 128-bit token, the host starts the reader Activity with it as the only extra, and the
 * Activity claims the session by presenting it back. A token is compared in constant time and is
 * never logged.
 */
internal object HostSessionPolicy {

    const val TOKEN_BYTES = 16

    private val TOKEN_PATTERN = Regex("[0-9a-f]{32}")
    private val random = SecureRandom()

    fun newToken(): String = ByteArray(TOKEN_BYTES).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    fun isWellFormed(token: String?): Boolean = token != null && TOKEN_PATTERN.matches(token)

    /**
     * The token of a reader-launch Intent: the reader action, an explicit component (the host
     * resolves the Activity itself) and a well-formed token; null for anything else.
     */
    fun tokenOf(action: String?, explicit: Boolean, token: String?): String? =
        token?.takeIf { action == EpubActions.READER_ACTIVITY_ACTION && explicit && isWellFormed(it) }

    fun sameToken(presented: String, expected: String): Boolean =
        MessageDigest.isEqual(presented.toByteArray(Charsets.UTF_8), expected.toByteArray(Charsets.UTF_8))
}
