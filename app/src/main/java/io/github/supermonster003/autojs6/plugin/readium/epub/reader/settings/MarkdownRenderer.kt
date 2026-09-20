package io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * Turns [MarkdownLite] blocks into a [Spanned] for the release history (roadmap P4.3): headings
 * are larger and bold (the date line, level 6, small and muted), bullets get a real bullet and an
 * indent per level, inline code is monospace, and only `https` links become clickable, through
 * [onLink] so the screen can fall back to a toast when no browser exists.
 */
internal object MarkdownRenderer {

    private const val FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

    fun render(context: Context, blocks: List<MarkdownBlock>, onLink: (String) -> Unit): CharSequence {
        val out = SpannableStringBuilder()
        val density = context.resources.displayMetrics.density
        val muted = MaterialColors.getColor(context, android.R.attr.textColorSecondary, Color.GRAY)
        for (block in blocks) {
            if (block is MarkdownBlock.Rule) continue
            if (out.isNotEmpty()) out.append(if (block is MarkdownBlock.Heading && block.level <= 2) "\n\n" else "\n")
            val start = out.length
            appendInlines(out, block.inlines, onLink)
            val end = out.length
            when (block) {
                is MarkdownBlock.Heading -> {
                    val size = when (block.level) {
                        1 -> 1.4f
                        2 -> 1.25f
                        3 -> 1.1f
                        else -> 0.9f
                    }
                    out.setSpan(RelativeSizeSpan(size), start, end, FLAGS)
                    out.setSpan(StyleSpan(if (block.level <= 3) Typeface.BOLD else Typeface.ITALIC), start, end, FLAGS)
                    if (block.level > 3) out.setSpan(ForegroundColorSpan(muted), start, end, FLAGS)
                }
                is MarkdownBlock.Bullet -> {
                    out.setSpan(LeadingMarginSpan.Standard((INDENT_DP * density * block.depth).toInt()), start, end, FLAGS)
                    out.setSpan(BulletSpan((BULLET_GAP_DP * density).toInt()), start, end, FLAGS)
                }
                is MarkdownBlock.Paragraph, MarkdownBlock.Rule -> Unit
            }
        }
        return out
    }

    private fun appendInlines(out: SpannableStringBuilder, inlines: List<MarkdownInline>, onLink: (String) -> Unit) {
        for (inline in inlines) {
            val start = out.length
            out.append(inline.text)
            val end = out.length
            when (inline) {
                is MarkdownInline.Text -> Unit
                is MarkdownInline.Code -> out.setSpan(TypefaceSpan("monospace"), start, end, FLAGS)
                is MarkdownInline.Bold -> out.setSpan(StyleSpan(Typeface.BOLD), start, end, FLAGS)
                is MarkdownInline.Link -> if (inline.url.startsWith("https://")) {
                    out.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) = onLink(inline.url)
                    }, start, end, FLAGS)
                }
            }
        }
    }

    private const val INDENT_DP = 16f
    private const val BULLET_GAP_DP = 10f
}
