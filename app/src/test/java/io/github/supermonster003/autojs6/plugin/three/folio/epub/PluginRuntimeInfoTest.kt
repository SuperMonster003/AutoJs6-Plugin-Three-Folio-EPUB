package io.github.supermonster003.autojs6.plugin.three.folio.epub

import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginRuntimeInfoTest {

    @Test
    fun theCatalogDeclaresAPrimaryAndAnOverflowActionForEpubFiles() {
        val specs = ThreeFolioEpubPlugin.actionSpecs()

        assertEquals(listOf("three-folio-epub.primary", "three-folio-epub"), specs.map { it.id })
        assertEquals(listOf(2, ExplorerActionValues.PLACEMENT_OVERFLOW), specs.map { it.placement })
        assertTrue(specs.all { it.priority == 100 })
        assertTrue(specs.all { it.targetKind == ExplorerActionValues.TARGET_FILE })
        assertTrue(specs.all { it.accessMode == ExplorerActionValues.ACCESS_READ_ONLY })
        assertTrue(specs.all { it.mimeTypes == listOf("application/epub+zip") })
        assertTrue(specs.all { it.extensions == listOf("epub") })
    }

    @Test
    fun bothActionsShareTheLabelAndTheReaderActivity() {
        val specs = ThreeFolioEpubPlugin.actionSpecs()
        val distinctPresentation = specs.map { Triple(it.labelResourceName, it.labelFallback, it.activityClassName) }.distinct()

        assertEquals(1, distinctPresentation.size)
        assertEquals("action_three_folio_epub", distinctPresentation.single().first)
        assertEquals("3-Folio EPUB", distinctPresentation.single().second)
        assertEquals(
            "io.github.supermonster003.autojs6.plugin.three.folio.epub.EpubReaderActivity",
            distinctPresentation.single().third,
        )
    }

    /** Roadmap P5.2 / P5.3 / P9.4: the EPUB service advertises extraction, the reader session and (contract version 2) annotations; TTS stays out. */
    @Test
    fun theEpubServiceAdvertisesTheExtractionFeaturesAndTheReaderSession() {
        val features = ThreeFolioEpubPlugin.EPUB_FEATURES

        assertEquals(listOf("search", "cover", "resource-export", "markdown", "reader-session", "annotations"), features)
        assertEquals(features.distinct(), features)
        assertTrue(EpubContract.FEATURES.containsAll(features))
        assertFalse(features.contains(EpubContract.FEATURE_TTS))
    }

    @Test
    fun placementsAreDistinctAndTheOverflowEntryUsesTheBundledConstant() {
        val placements = ThreeFolioEpubPlugin.actionSpecs().map { it.placement }

        assertEquals(placements.distinct(), placements)
        assertTrue(ThreeFolioEpubPlugin.PRIMARY_PLACEMENT != ExplorerActionValues.PLACEMENT_OVERFLOW)
    }
}
