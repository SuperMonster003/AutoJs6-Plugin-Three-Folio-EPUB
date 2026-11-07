package io.github.supermonster003.autojs6.plugin.three.folio.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The name of the plugin's first-level menu in the AutoJs6 file manager (host Explorer Action protocol v27): the
 * catalog declares the display name under the two catalog-level keys, without raising its protocol version (v2) or
 * its minimum host version. The instrumentation contract test asserts the exact key set of the real catalog Bundle.
 */
class ExplorerMenuGroupLabelTest {

    @Test
    fun `the catalog keys are the names the host reads`() {
        assertEquals("menuGroupLabelResourceName", ThreeFolioEpubPlugin.MENU_GROUP_LABEL_RESOURCE_NAME_KEY)
        assertEquals("menuGroupLabelFallback", ThreeFolioEpubPlugin.MENU_GROUP_LABEL_FALLBACK_KEY)
    }

    @Test
    fun `the menu is named after the display name of the plugin`() {
        assertEquals("app_name", ThreeFolioEpubPlugin.MENU_GROUP_LABEL_RESOURCE_NAME)
        assertEquals("3-Folio EPUB", ThreeFolioEpubPlugin.MENU_GROUP_LABEL_FALLBACK)
        assertTrue(Regex("[a-z][a-z0-9_]*").matches(ThreeFolioEpubPlugin.MENU_GROUP_LABEL_RESOURCE_NAME))
        assertTrue(ThreeFolioEpubPlugin.MENU_GROUP_LABEL_FALLBACK.length <= 160)
        val strings = read("app/src/main/res/values/strings_donottranslate.xml")
        assertTrue(strings.contains("<string name=\"app_name\" translatable=\"false\">3-Folio EPUB</string>"))
    }

    @Test
    fun `the catalog writes both keys and keeps its protocol version`() {
        val source = read("app/src/main/java/io/github/supermonster003/autojs6/plugin/three/folio/epub/PluginRuntimeInfo.kt")
        assertTrue(source.contains("putString(ThreeFolioEpubPlugin.MENU_GROUP_LABEL_RESOURCE_NAME_KEY, ThreeFolioEpubPlugin.MENU_GROUP_LABEL_RESOURCE_NAME)"))
        assertTrue(source.contains("putString(ThreeFolioEpubPlugin.MENU_GROUP_LABEL_FALLBACK_KEY, ThreeFolioEpubPlugin.MENU_GROUP_LABEL_FALLBACK)"))
        val compatibility = read("gradle/explorer-action-compatibility.properties")
        assertTrue(compatibility.lines().contains("declaredProtocolVersion=2"))
        assertTrue(compatibility.lines().contains("minimumHostVersionCode=5318"))
    }

    private fun read(relativePath: String): String =
        String(Files.readAllBytes(findProjectRoot().resolve(relativePath)), Charsets.UTF_8)

    private fun findProjectRoot(): Path {
        var path: Path? = Paths.get("").toAbsolutePath()
        while (path != null) {
            if (Files.isRegularFile(path.resolve("settings.gradle.kts"))) return path
            path = path.parent
        }
        error("Project root not found")
    }
}
