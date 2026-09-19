package io.github.supermonster003.autojs6.plugin.readium.epub.reader

/**
 * The Explorer Action contract intentionally advertised by this plugin.
 *
 * These values come from gradle/explorer-action-compatibility.properties through BuildConfig so
 * the catalog, execution policy, build gate, tests, and generated documentation share one audit
 * record. v2 supports primary and overflow actions with the same two-URI read-only envelope.
 * v4 and later require a different host-session resource model that an EPUB reader does not need.
 */
internal object EpubReaderExplorerCompatibility {

    val declaredProtocolVersion: Int = BuildConfig.EXPLORER_ACTION_PROTOCOL_VERSION
    val minimumHostVersionCode: Long = BuildConfig.EXPLORER_ACTION_MINIMUM_HOST_VERSION_CODE
    val maximumAuditedHostVersionCode: Long =
        BuildConfig.EXPLORER_ACTION_MAXIMUM_AUDITED_HOST_VERSION_CODE
    val maximumAuditedHostProtocolVersion: Int =
        BuildConfig.EXPLORER_ACTION_MAXIMUM_AUDITED_HOST_PROTOCOL_VERSION

    const val LEGACY_CLIP_ITEM_COUNT = 2

    val hostProtocolCheckpoints = listOf(
        HostProtocolCheckpoint(hostVersionCode = 5268L, maximumProtocolVersion = 1),
        HostProtocolCheckpoint(hostVersionCode = 5269L, maximumProtocolVersion = 3),
        HostProtocolCheckpoint(hostVersionCode = 5276L, maximumProtocolVersion = 21),
        HostProtocolCheckpoint(hostVersionCode = 5277L, maximumProtocolVersion = 22),
        HostProtocolCheckpoint(hostVersionCode = 5279L, maximumProtocolVersion = 22),
        HostProtocolCheckpoint(hostVersionCode = 5282L, maximumProtocolVersion = 22),
    )

    fun acceptsHostVersionCode(hostVersionCode: Long): Boolean =
        hostVersionCode >= minimumHostVersionCode

    fun auditStatus(hostVersionCode: Long): HostAuditStatus = when {
        !acceptsHostVersionCode(hostVersionCode) -> HostAuditStatus.UNSUPPORTED
        hostVersionCode <= maximumAuditedHostVersionCode -> HostAuditStatus.AUDITED
        else -> HostAuditStatus.FORWARD_COMPATIBLE_UNAUDITED
    }

    data class HostProtocolCheckpoint(
        val hostVersionCode: Long,
        val maximumProtocolVersion: Int,
    )

    enum class HostAuditStatus {
        UNSUPPORTED,
        AUDITED,
        FORWARD_COMPATIBLE_UNAUDITED,
    }
}
