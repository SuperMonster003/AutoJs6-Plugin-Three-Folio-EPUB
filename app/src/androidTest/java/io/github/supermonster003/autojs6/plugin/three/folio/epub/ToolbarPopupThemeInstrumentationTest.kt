package io.github.supermonster003.autojs6.plugin.three.folio.epub

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import io.github.supermonster003.autojs6.plugin.three.folio.epub.databinding.ActivityEpubReaderBinding
import io.github.supermonster003.autojs6.plugin.three.folio.epub.databinding.ActivityLauncherBinding
import io.github.supermonster003.autojs6.plugin.three.folio.epub.databinding.ActivitySettingsBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The overflow menus of the three toolbars (launcher, reader, settings) are themed by `AppTheme.PopupOverlay` on top of
 * the white-text `AppTheme.Toolbar`. Up to 2.1.3 the light menus kept that white text on the light Material 3 menu
 * container (#F3EDF7, contrast about 1.1), and the night checked boxes were #004D40 on a dark container.
 */
@RunWith(AndroidJUnit4::class)
class ToolbarPopupThemeInstrumentationTest {

    @Test
    fun overflowMenusStayReadableInLightAndDarkMode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES).forEach { nightMode ->
                val mode = if (nightMode == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
                val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
                }
                val configured = instrumentation.targetContext.createConfigurationContext(configuration)
                val inflater = LayoutInflater.from(ContextThemeWrapper(configured, R.style.AppTheme))
                listOf(
                    "launcher" to ActivityLauncherBinding.inflate(inflater).toolbar,
                    "reader" to ActivityEpubReaderBinding.inflate(inflater).toolbar,
                    "settings" to ActivitySettingsBinding.inflate(inflater).toolbar,
                ).forEach { (page, toolbar) -> checkPopup("$mode $page", toolbar) }
            }
        }
    }

    private fun checkPopup(label: String, toolbar: MaterialToolbar) {
        assertEquals("$label: popupTheme", R.style.AppTheme_PopupOverlay, toolbar.popupTheme)
        val popupContext = ContextThemeWrapper(toolbar.context, toolbar.popupTheme)
        val attributes = popupContext.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val textColor = requireNotNull(
            try {
                attributes.getColorStateList(0)?.defaultColor
            } finally {
                attributes.recycle()
            },
        ) { "$label: textColorPrimary is missing" }
        fun color(attribute: Int, name: String) = MaterialColors.getColor(popupContext, attribute, name)
        val container = color(com.google.android.material.R.attr.colorSurfaceContainer, "colorSurfaceContainer")
        // The Material 3 overflow menu paints its container with colorSurfaceContainer (m3_comp_menu_container_color);
        // colorSurface stays checked for older surfaces.
        listOf(
            com.google.android.material.R.attr.colorSurface to "colorSurface",
            com.google.android.material.R.attr.colorSurfaceContainer to "colorSurfaceContainer",
        ).forEach { (attribute, name) ->
            val surface = color(attribute, name)
            assertContrast("$label: text ${hex(textColor)} on $name ${hex(surface)}", textColor, surface, 4.5)
        }
        // The reader menu has checkable items and a single-choice submenu: their checked boxes and radio buttons use
        // colorPrimary, with colorOnPrimary for the check mark (non-text contrast, WCAG 1.4.11).
        val primary = color(androidx.appcompat.R.attr.colorPrimary, "colorPrimary")
        val onPrimary = color(com.google.android.material.R.attr.colorOnPrimary, "colorOnPrimary")
        assertContrast("$label: checked colorPrimary ${hex(primary)} on colorSurfaceContainer ${hex(container)}", primary, container, 3.0)
        assertContrast("$label: check mark colorOnPrimary ${hex(onPrimary)} on colorPrimary ${hex(primary)}", onPrimary, primary, 3.0)
    }

    private fun assertContrast(message: String, foreground: Int, background: Int, minimum: Double) {
        val contrast = ColorUtils.calculateContrast(foreground, background)
        assertTrue("$message has contrast %.2f < %.1f".format(contrast, minimum), contrast >= minimum)
    }

    private fun hex(color: Int): String = "#%08X".format(color)
}
