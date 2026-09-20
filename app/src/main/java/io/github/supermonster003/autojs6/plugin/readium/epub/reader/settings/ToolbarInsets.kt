package io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings

import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/**
 * Edge-to-edge for the toolbar screens (roadmap P4.3), the launcher's arrangement: the toolbar
 * grows under the status bar, [content] keeps its bottom padding above the navigation bar, and
 * [root] keeps the side insets (cutouts, three-button bars in landscape).
 */
internal fun AppCompatActivity.fitToolbarAndContent(root: View, toolbar: View, content: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val actionBarSize = TypedValue().let { value ->
        theme.resolveAttribute(android.R.attr.actionBarSize, value, true)
        TypedValue.complexToDimensionPixelSize(value.data, resources.displayMetrics)
    }
    val contentPaddingBottom = content.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.updatePadding(left = bars.left, right = bars.right)
        toolbar.updatePadding(top = bars.top)
        toolbar.updateLayoutParams { height = actionBarSize + bars.top }
        content.updatePadding(bottom = contentPaddingBottom + bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
}
