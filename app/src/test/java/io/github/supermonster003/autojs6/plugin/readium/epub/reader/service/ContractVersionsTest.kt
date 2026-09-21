package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import org.autojs.plugin.epub.api.EpubContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P9.4: the version a book or session answers with, for every request a host may send. */
class ContractVersionsTest {

    @Test
    fun theBaselineIsOneAndTheNewestIsTwo() {
        assertEquals(1, ContractVersions.BASELINE)
        assertEquals(2, ContractVersions.NEWEST)
        assertEquals(EpubContract.MIN_CONTRACT_VERSION, ContractVersions.BASELINE)
        assertEquals(EpubContract.MAX_CONTRACT_VERSION, ContractVersions.NEWEST)
        assertEquals(EpubContract.CONTRACT_VERSION_ANNOTATIONS, ContractVersions.NEWEST)
    }

    @Test
    fun requestsNegotiateToTheBaselineBelowAndToTheNewestAbove() {
        // A version 1 host (6.8.0 build 5282) writes 1, and so does a host that omits the key (0).
        assertEquals(1, ContractVersions.negotiate(0))
        assertEquals(1, ContractVersions.negotiate(1))
        assertEquals(1, ContractVersions.negotiate(-3))
        // A version 2 host gets version 2; a future host is answered with the newest this plugin knows.
        assertEquals(2, ContractVersions.negotiate(2))
        assertEquals(2, ContractVersions.negotiate(3))
        assertEquals(2, ContractVersions.negotiate(Int.MAX_VALUE))
    }

    @Test
    fun onlyVersionTwoCarriesAnnotations() {
        assertFalse(ContractVersions.supportsAnnotations(0))
        assertFalse(ContractVersions.supportsAnnotations(1))
        assertTrue(ContractVersions.supportsAnnotations(2))
        assertTrue(ContractVersions.supportsAnnotations(3))
    }
}
