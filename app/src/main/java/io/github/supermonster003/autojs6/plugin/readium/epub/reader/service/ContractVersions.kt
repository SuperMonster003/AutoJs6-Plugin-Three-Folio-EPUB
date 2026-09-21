package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.os.Bundle
import org.autojs.plugin.epub.api.EpubContract

/**
 * Contract version negotiation on the plugin side (roadmap P9.4): the plugin advertises
 * [BASELINE] as `epubContractVersion` (so hosts that only know version 1 keep accepting it) and
 * [NEWEST] as `epubMaxContractVersion`; the host writes the version it settled on into the
 * `openBook` / `openReader` options, and the book or session answers with that version for its
 * whole life. Hosts below version 2 never receive `highlight` events and must not call
 * `getAnnotations`. The `Bundle` reader is the only Android piece; [negotiate] is pure.
 */
internal object ContractVersions {

    /** The version every host of the contract understands; what `epubContractVersion` carries. */
    const val BASELINE = EpubContract.MIN_CONTRACT_VERSION

    /** The newest version this plugin implements; what `epubMaxContractVersion` carries. */
    const val NEWEST = EpubContract.MAX_CONTRACT_VERSION

    /**
     * The version a book or session answers with for a request that named [requested]: the request's
     * version when the plugin implements it, [BASELINE] when the key is absent (0) or below it, and
     * [NEWEST] when the host asked for a version this plugin does not know yet.
     */
    fun negotiate(requested: Int): Int = when {
        requested < BASELINE -> BASELINE
        requested > NEWEST -> NEWEST
        else -> requested
    }

    /** The negotiated version of an `openBook` or `openReader` options bundle (null or without the key means the baseline). */
    fun of(options: Bundle?): Int = negotiate(options?.getInt(EpubContract.KEY_CONTRACT_VERSION, 0) ?: 0)

    /** Whether [version] includes `getAnnotations` and the `highlight` event. */
    fun supportsAnnotations(version: Int): Boolean = version >= EpubContract.CONTRACT_VERSION_ANNOTATIONS
}
