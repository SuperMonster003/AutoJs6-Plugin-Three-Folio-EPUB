package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.TocFlattener
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.TocRow
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

/**
 * The table-of-contents list (roadmap P1.2): the flattened Readium tree with indentation per
 * depth, the current chapter in bold and scrolled into view, and the reading order as fallback
 * when the book declares no table of contents.
 */
internal object TocSheet {

    fun rows(publication: Publication): List<TocRow<Link>> {
        val tree = publication.tableOfContents.ifEmpty { publication.readingOrder }
        return TocFlattener.flatten(tree, children = { it.children })
    }

    fun chapterTitle(publication: Publication, currentHref: String?): String? =
        TocFlattener.titleForHref(rows(publication), { it.href.toString() }, { it.title }, currentHref)

    fun show(
        context: Context,
        rows: List<TocRow<Link>>,
        currentHref: String?,
        onSelected: (Link) -> Unit,
        onDismissed: () -> Unit,
    ): AlertDialog {
        val currentIndex = TocFlattener.indexOfHref(rows, { it.href.toString() }, currentHref)
        val adapter = TocAdapter(context, rows, currentIndex)
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.text_table_of_contents)
            .setAdapter(adapter) { _, index -> onSelected(rows[index].node) }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .setOnDismissListener { onDismissed() }
            .create()
        dialog.show()
        if (currentIndex >= 0) dialog.listView?.setSelection(currentIndex)
        return dialog
    }

    private class TocAdapter(
        context: Context,
        private val rows: List<TocRow<Link>>,
        private val currentIndex: Int,
    ) : ArrayAdapter<TocRow<Link>>(context, android.R.layout.simple_list_item_1, rows) {

        private val indentPx = (context.resources.displayMetrics.density * INDENT_DP).toInt()

        /** Padding of a freshly inflated row; recycled rows already carry an indentation. */
        private var basePaddingStart: Int? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            val base = basePaddingStart ?: view.paddingStart.also { basePaddingStart = it }
            val row = rows[position]
            val link = row.node
            view.text = link.title?.takeIf { it.isNotBlank() } ?: link.href.toString()
            view.setTypeface(null, if (position == currentIndex) Typeface.BOLD else Typeface.NORMAL)
            view.setPaddingRelative(base + indentPx * row.depth, view.paddingTop, view.paddingEnd, view.paddingBottom)
            return view
        }

        private companion object {
            const val INDENT_DP = 20
        }
    }
}
