package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationListing
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationRow
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.BookAnnotation
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.SheetAnnotationsBinding
import kotlinx.coroutines.launch

/**
 * The highlights and notes panel (roadmap P9.2): a bottom sheet listing the open book's
 * annotations in reading order under one header per chapter, each row with its colour, the
 * highlighted passage, the note and the time. Tapping a row jumps there and closes the sheet,
 * the pencil opens the editor, the trash icon deletes one, the header clears them all after a
 * confirmation. Everything shown comes from the [EpubReaderActivity], so the sheet has no state
 * of its own to lose.
 */
internal class AnnotationSheet : BottomSheetDialogFragment() {

    private var _binding: SheetAnnotationsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val host: EpubReaderActivity get() = requireActivity() as EpubReaderActivity

    private val adapter = AnnotationsAdapter(
        onOpen = ::open,
        onEdit = { host.editAnnotation(it) },
        onDelete = { host.deleteAnnotation(it.id) },
    )

    /** The rows currently listed (headers included); exposed for the instrumentation tests. */
    internal val rows: List<AnnotationRow> get() = adapter.rows

    /** The listed annotations in panel order. */
    internal val items: List<BookAnnotation> get() = AnnotationListing.items(adapter.rows)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SheetAnnotationsBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?): Unit = with(binding) {
        annotationClear.setOnClickListener { confirmClear() }
        annotationList.layoutManager = LinearLayoutManager(requireContext())
        annotationList.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                host.annotations.collect { render(it) }
            }
        }
        Unit
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /** Opens the [index]th listed annotation (panel order, headers not counted) and closes the sheet. */
    internal fun open(index: Int) {
        val annotation = items.getOrNull(index) ?: return
        open(annotation)
    }

    private fun open(annotation: BookAnnotation) {
        host.openAnnotation(annotation)
        dismiss()
    }

    private fun confirmClear() {
        val count = host.annotations.value.size
        if (count == 0) return
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.text_annotation_clear_all_message, count))
            .setPositiveButton(R.string.dialog_button_confirm) { _, _ -> host.clearAnnotations() }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    private fun render(annotations: List<BookAnnotation>) = with(binding) {
        annotationClear.isEnabled = annotations.isNotEmpty()
        annotationEmpty.isVisible = annotations.isEmpty()
        annotationList.isVisible = annotations.isNotEmpty()
        adapter.submit(AnnotationListing.rows(annotations, host.readingOrderHrefs))
    }

    private class AnnotationsAdapter(
        private val onOpen: (BookAnnotation) -> Unit,
        private val onEdit: (BookAnnotation) -> Unit,
        private val onDelete: (BookAnnotation) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var rows: List<AnnotationRow> = emptyList()
            private set

        class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.annotation_header)
        }

        class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
            val color: View = view.findViewById(R.id.annotation_color)
            val quote: TextView = view.findViewById(R.id.annotation_quote)
            val note: TextView = view.findViewById(R.id.annotation_note)
            val time: TextView = view.findViewById(R.id.annotation_time)
            val edit: ImageButton = view.findViewById(R.id.annotation_edit)
            val delete: ImageButton = view.findViewById(R.id.annotation_delete)
        }

        fun submit(rows: List<AnnotationRow>) {
            this.rows = rows
            @Suppress("NotifyDataSetChanged")
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = rows.size

        override fun getItemViewType(position: Int): Int = when (rows[position]) {
            is AnnotationRow.Header -> TYPE_HEADER
            is AnnotationRow.Item -> TYPE_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                HeaderHolder(inflater.inflate(R.layout.item_annotation_header, parent, false))
            } else {
                ItemHolder(inflater.inflate(R.layout.item_annotation, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val row = rows[position]) {
                is AnnotationRow.Header -> (holder as HeaderHolder).title.text = row.title
                is AnnotationRow.Item -> bind(holder as ItemHolder, row.annotation)
            }
        }

        private fun bind(holder: ItemHolder, annotation: BookAnnotation) {
            holder.color.setBackgroundColor(AnnotationColors.normalize(annotation.color))
            val quote = AnnotationPolicy.preview(annotation.quote)
            holder.quote.text = quote
            holder.quote.isVisible = !quote.isNullOrEmpty()
            holder.note.text = AnnotationPolicy.preview(annotation.note)
            holder.note.isVisible = annotation.hasNote
            holder.time.text = DateUtils.formatDateTime(holder.itemView.context, annotation.updatedAt, TIME_FLAGS)
            holder.itemView.setOnClickListener { onOpen(annotation) }
            holder.edit.setOnClickListener { onEdit(annotation) }
            holder.delete.setOnClickListener { onDelete(annotation) }
        }

        private companion object {
            const val TYPE_HEADER = 0
            const val TYPE_ITEM = 1
            const val TIME_FLAGS = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
        }
    }

    companion object {
        const val TAG = "reader-annotations"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            AnnotationSheet().show(fragmentManager, TAG)
        }
    }
}
