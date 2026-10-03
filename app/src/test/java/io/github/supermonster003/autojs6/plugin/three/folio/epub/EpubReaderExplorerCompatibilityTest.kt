package io.github.supermonster003.autojs6.plugin.three.folio.epub

import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubReaderExplorerCompatibilityTest {

    @Test
    fun auditedContractPinsTheBundledV1ApiAndCurrentHostCheckpoint() {
        assertEquals(1, ExplorerActionProtocol.VERSION)
        assertEquals(2, EpubReaderExplorerCompatibility.declaredProtocolVersion)
        assertEquals(5318L, EpubReaderExplorerCompatibility.minimumHostVersionCode)
        assertEquals(5318L, EpubReaderExplorerCompatibility.maximumAuditedHostVersionCode)
        assertEquals(22, EpubReaderExplorerCompatibility.maximumAuditedHostProtocolVersion)
        assertEquals(
            EpubReaderExplorerCompatibility.minimumHostVersionCode,
            ThreeFolioEpubPlugin.REQUIRED_HOST_VERSION,
        )
        assertEquals(
            EpubReaderExplorerCompatibility.declaredProtocolVersion,
            ThreeFolioEpubPlugin.PROTOCOL_VERSION,
        )
    }

    @Test
    fun hostProtocolCheckpointsAreStrictlyIncreasingAndEndAtTheAuditBoundary() {
        val checkpoints = EpubReaderExplorerCompatibility.hostProtocolCheckpoints

        assertEquals(
            listOf(5268L to 1, 5269L to 3, 5276L to 21, 5277L to 22, 5279L to 22, 5282L to 22, 5318L to 22),
            checkpoints.map { it.hostVersionCode to it.maximumProtocolVersion },
        )
        assertTrue(checkpoints.zipWithNext().all { (left, right) ->
            left.hostVersionCode < right.hostVersionCode &&
                left.maximumProtocolVersion <= right.maximumProtocolVersion
        })
        assertEquals(
            EpubReaderExplorerCompatibility.maximumAuditedHostVersionCode,
            checkpoints.last().hostVersionCode,
        )
        assertEquals(
            EpubReaderExplorerCompatibility.maximumAuditedHostProtocolVersion,
            checkpoints.last().maximumProtocolVersion,
        )
    }

    @Test
    fun hostVersionClassificationRejectsOldAndSeparatesAuditedFromFuture() {
        assertFalse(EpubReaderExplorerCompatibility.acceptsHostVersionCode(5268L))
        assertFalse(EpubReaderExplorerCompatibility.acceptsHostVersionCode(5317L))
        assertTrue(EpubReaderExplorerCompatibility.acceptsHostVersionCode(5318L))
        assertTrue(EpubReaderExplorerCompatibility.acceptsHostVersionCode(6000L))
        assertEquals(
            EpubReaderExplorerCompatibility.HostAuditStatus.UNSUPPORTED,
            EpubReaderExplorerCompatibility.auditStatus(5268L),
        )
        assertEquals(
            EpubReaderExplorerCompatibility.HostAuditStatus.AUDITED,
            EpubReaderExplorerCompatibility.auditStatus(5318L),
        )
        assertEquals(
            EpubReaderExplorerCompatibility.HostAuditStatus.FORWARD_COMPATIBLE_UNAUDITED,
            EpubReaderExplorerCompatibility.auditStatus(5319L),
        )
    }

    @Test
    fun identityConstantsMatchTheCatalogAndManifestContract() {
        assertEquals("three-folio-epub", ThreeFolioEpubPlugin.ID)
        assertEquals("three-folio-epub.primary", ThreeFolioEpubPlugin.PRIMARY_ACTION_ID)
        assertEquals("default", ThreeFolioEpubPlugin.VARIANT)
        assertEquals(listOf("application/epub+zip"), ThreeFolioEpubPlugin.MIME_TYPES.toList())
        assertEquals(listOf("epub"), ThreeFolioEpubPlugin.EXTENSIONS.toList())
        assertTrue(ThreeFolioEpubPlugin.ACTIVITY_CLASS_NAME.endsWith(".EpubReaderActivity"))
        assertEquals(2, ThreeFolioEpubPlugin.PRIMARY_PLACEMENT)
    }
}
