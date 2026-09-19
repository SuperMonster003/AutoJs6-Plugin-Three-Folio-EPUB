package io.github.supermonster003.autojs6.plugin.readium.epub.reader

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
        assertEquals(5269L, EpubReaderExplorerCompatibility.minimumHostVersionCode)
        assertEquals(5282L, EpubReaderExplorerCompatibility.maximumAuditedHostVersionCode)
        assertEquals(22, EpubReaderExplorerCompatibility.maximumAuditedHostProtocolVersion)
        assertEquals(
            EpubReaderExplorerCompatibility.minimumHostVersionCode,
            ReadiumEpubReaderPlugin.REQUIRED_HOST_VERSION,
        )
        assertEquals(
            EpubReaderExplorerCompatibility.declaredProtocolVersion,
            ReadiumEpubReaderPlugin.PROTOCOL_VERSION,
        )
    }

    @Test
    fun hostProtocolCheckpointsAreStrictlyIncreasingAndEndAtTheAuditBoundary() {
        val checkpoints = EpubReaderExplorerCompatibility.hostProtocolCheckpoints

        assertEquals(
            listOf(5268L to 1, 5269L to 3, 5276L to 21, 5277L to 22, 5279L to 22, 5282L to 22),
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
        assertTrue(EpubReaderExplorerCompatibility.acceptsHostVersionCode(5269L))
        assertTrue(EpubReaderExplorerCompatibility.acceptsHostVersionCode(6000L))
        assertEquals(
            EpubReaderExplorerCompatibility.HostAuditStatus.UNSUPPORTED,
            EpubReaderExplorerCompatibility.auditStatus(5268L),
        )
        assertEquals(
            EpubReaderExplorerCompatibility.HostAuditStatus.AUDITED,
            EpubReaderExplorerCompatibility.auditStatus(5282L),
        )
        assertEquals(
            EpubReaderExplorerCompatibility.HostAuditStatus.FORWARD_COMPATIBLE_UNAUDITED,
            EpubReaderExplorerCompatibility.auditStatus(5283L),
        )
    }

    @Test
    fun identityConstantsMatchTheCatalogAndManifestContract() {
        assertEquals("readium-epub-reader", ReadiumEpubReaderPlugin.ID)
        assertEquals("readium-epub-reader.primary", ReadiumEpubReaderPlugin.PRIMARY_ACTION_ID)
        assertEquals("default", ReadiumEpubReaderPlugin.VARIANT)
        assertEquals(listOf("application/epub+zip"), ReadiumEpubReaderPlugin.MIME_TYPES.toList())
        assertEquals(listOf("epub"), ReadiumEpubReaderPlugin.EXTENSIONS.toList())
        assertTrue(ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME.endsWith(".EpubReaderActivity"))
        assertEquals(2, ReadiumEpubReaderPlugin.PRIMARY_PLACEMENT)
    }
}
