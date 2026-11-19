package io.github.supermonster003.autojs6.plugin.three.folio.epub

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.view.ViewGroup
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.folio.epub.settings.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.folio.epub.settings.ReleaseHistoryAssetPolicy
import io.github.supermonster003.autojs6.plugin.three.folio.epub.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.folio.epub.settings.loadReleaseHistory
import io.github.supermonster003.autojs6.plugin.three.folio.epub.store.ReaderSettings
import io.github.supermonster003.autojs6.plugin.three.folio.epub.update.AppUpdateCoordinator
import io.github.supermonster003.autojs6.plugin.three.folio.epub.update.ReleaseInfo
import io.github.supermonster003.autojs6.plugin.three.folio.epub.update.UpdateFetchResult
import io.github.supermonster003.autojs6.plugin.three.folio.epub.update.UpdateSource
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The release history page (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md): its title is "Release history" with the plugin
 * name as the subtitle, the newest release starts expanded, and both the settings row and the update dialog open it.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseHistoryPageInstrumentationTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Before fun resetUpdateState() {
        ReaderSettings(context).clearAll()
    }

    @After fun finishActivities() {
        AppUpdateCoordinator.sourceOverride = null
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach(Activity::finish)
        }
        instrumentation.waitForIdleSync()
        ReaderSettings(context).clearAll()
    }

    /** Unmerged: the count chips sit inside the clickable header, which merges its children. */
    private fun exists(tag: String): Boolean =
        runCatching { compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)

    private fun resumed(): Activity? {
        var activity: Activity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return activity
    }

    private fun assertThePageOpened() {
        compose.waitUntil(TIMEOUT) { resumed() is ReleaseHistoryActivity && exists("folio.releaseHistory.list") }
        assertTitleAndSubtitle()
    }

    private fun assertTitleAndSubtitle() {
        val inTopBar = hasAnyAncestor(hasTestTag("folio.topBar"))
        compose.onNode(hasText(context.getString(R.string.release_history)) and inTopBar).assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.app_name)) and inTopBar).assertIsDisplayed()
    }

    private fun startSettings(): SettingsActivity =
        instrumentation.startActivitySync(
            Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as SettingsActivity

    @Test fun theTitleIsReleaseHistoryAndTheSubtitleThePluginName() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use {
            compose.waitUntil(TIMEOUT) { exists("folio.releaseHistory.list") }
            assertTitleAndSubtitle()
            assertFalse(exists("folio.releaseHistory.empty"))
        }
    }

    private fun versions(scenario: ActivityScenario<ReleaseHistoryActivity>): List<String> {
        var versions = emptyList<String>()
        scenario.onActivity { activity ->
            val candidates = ReleaseHistoryAssetPolicy.candidatesFor(activity.resources.configuration.locales[0])
            versions = loadReleaseHistory(candidates) { path ->
                activity.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
            }.map { it.version }
        }
        return versions
    }

    /**
     * The newest release starts expanded, the others show their category counts; a header expands or collapses its
     * release, the top bar expands or collapses all of them, and the choice survives recreation.
     */
    @Test fun releasesExpandAndCollapse() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("folio.releaseHistory.list") }
            val versions = versions(scenario)
            assertTrue("releases: ${versions.size}", versions.size > 2)
            val (newest, second) = versions
            assertTrue(exists("folio.releaseHistory.body.$newest"))
            assertFalse(exists("folio.releaseHistory.counts.$newest"))
            assertFalse(exists("folio.releaseHistory.body.$second"))
            assertTrue(exists("folio.releaseHistory.counts.$second"))

            compose.onNodeWithTag("folio.releaseHistory.entry.$second").performClick()
            compose.waitUntil(TIMEOUT) { exists("folio.releaseHistory.body.$second") }
            compose.onNodeWithTag("folio.releaseHistory.entry.$newest").performClick()
            compose.waitUntil(TIMEOUT) { !exists("folio.releaseHistory.body.$newest") && exists("folio.releaseHistory.counts.$newest") }

            scenario.recreate()
            compose.waitUntil(TIMEOUT) { exists("folio.releaseHistory.list") }
            assertTrue(exists("folio.releaseHistory.body.$second"))
            assertFalse(exists("folio.releaseHistory.body.$newest"))

            compose.onNodeWithTag("folio.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_expand_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { exists("folio.releaseHistory.body.$newest") }
            compose.onNodeWithTag("folio.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_collapse_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { !exists("folio.releaseHistory.body.$newest") && !exists("folio.releaseHistory.body.$second") }
        }
    }

    @Test fun theSettingsRowOpensThePage() {
        val page = startSettings()
        instrumentation.runOnMainSync {
            val content = page.findViewById<ViewGroup>(R.id.content)
            val row = (0 until content.childCount).map(content::getChildAt).first { it.tag == R.string.release_history }
            row.performClick()
        }
        assertThePageOpened()
    }

    @Test fun theUpdateDialogOpensThePage() {
        val release = ReleaseInfo(
            tagName = "v99.0.0",
            name = "v99.0.0",
            htmlUrl = "https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB/releases/tag/v99.0.0",
            publishedAt = "2026-11-19T00:00:00Z",
            preRelease = false,
            notes = "* `Feature` Note",
        )
        AppUpdateCoordinator.sourceOverride = UpdateSource { UpdateFetchResult.Success(release) }
        val page = startSettings()
        instrumentation.runOnMainSync { page.checkForUpdates() }
        compose.waitUntil(TIMEOUT) {
            var showing = false
            instrumentation.runOnMainSync { showing = page.updates.dialog?.isShowing == true }
            showing
        }
        instrumentation.runOnMainSync {
            requireNotNull(page.updates.dialog).getButton(DialogInterface.BUTTON_NEUTRAL).performClick()
        }
        assertThePageOpened()
    }

    private companion object {
        const val TIMEOUT = 15_000L
    }
}
