package io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs

import kotlin.math.pow
import kotlin.math.roundToInt

/** Colours of the toolbar, the progress panel and the system bars for one [ReaderTheme]. */
internal data class ChromeColors(
    /** Toolbar and progress panel background (the page colour nudged towards the text colour). */
    val background: Int,
    /** Title, icons and progress text. */
    val foreground: Int,
    /** Subtitle and secondary text: [foreground] at reduced opacity. */
    val secondaryForeground: Int,
    /** Progress bar fill. */
    val accent: Int,
    /** True when the bars are light and the system bar icons must be dark. */
    val lightBars: Boolean,
)

/**
 * Pure colour rules shared by the toolbar and both system bars (roadmap P2.1), following the
 * previewers' contrast rule: the foreground is the theme text colour when it keeps a 4.5:1 contrast
 * against the bar, black or white otherwise.
 */
internal object ReaderThemeColors {

    private const val BAR_TINT = 0.06
    private const val SECONDARY_ALPHA = 0.72
    private const val ACCENT_ALPHA = 0.55
    private const val MINIMUM_CONTRAST = 4.5

    fun forTheme(theme: ReaderTheme): ChromeColors {
        val background = blend(theme.backgroundColor, theme.contentColor, BAR_TINT)
        val foreground = if (contrast(theme.contentColor, background) >= MINIMUM_CONTRAST) {
            theme.contentColor
        } else {
            contrastForeground(background)
        }
        return ChromeColors(
            background = background,
            foreground = foreground,
            secondaryForeground = withAlpha(foreground, SECONDARY_ALPHA),
            accent = withAlpha(foreground, ACCENT_ALPHA),
            lightBars = luminance(background) > 0.5,
        )
    }

    /** WCAG relative luminance of an opaque colour. */
    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val value = ((color ushr shift) and 255) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /** WCAG contrast ratio between two opaque colours (1.0 .. 21.0). */
    fun contrast(first: Int, second: Int): Double {
        val lighter = maxOf(luminance(first), luminance(second))
        val darker = minOf(luminance(first), luminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Black or white, whichever contrasts more with [background]. */
    fun contrastForeground(background: Int): Int {
        val luminance = luminance(background)
        val blackContrast = (luminance + 0.05) / 0.05
        val whiteContrast = 1.05 / (luminance + 0.05)
        return if (blackContrast >= whiteContrast) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }

    /** Mixes [amount] (0..1) of [towards] into [base]; both opaque. */
    fun blend(base: Int, towards: Int, amount: Double): Int {
        fun channel(shift: Int): Int {
            val from = (base ushr shift) and 255
            val to = (towards ushr shift) and 255
            return (from + (to - from) * amount).roundToInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun withAlpha(color: Int, alpha: Double): Int =
        (color and 0x00FFFFFF) or ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24)
}
