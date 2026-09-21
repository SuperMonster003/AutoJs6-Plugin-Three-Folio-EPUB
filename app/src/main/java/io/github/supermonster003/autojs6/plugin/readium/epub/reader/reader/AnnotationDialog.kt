package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationPolicy
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.AnnotationStyle
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.annotations.BookAnnotation
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.DialogAnnotationBinding
import org.readium.r2.shared.publication.Locator

/**
 * The highlight editor (roadmap P9.2): the passage, a highlight / underline toggle, the colour
 * palette and a note field. Opened for a fresh selection ("Add note") or for an existing
 * highlight (a tap on it, or the panel's pencil), in which case a delete button joins save and
 * cancel. The choices go back to the [EpubReaderActivity], which also remembers the last colour
 * and style for the next quick highlight.
 */
internal class AnnotationDialog : DialogFragment() {

    private var _binding: DialogAnnotationBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val host: EpubReaderActivity get() = requireActivity() as EpubReaderActivity

    private val chips = ArrayList<Pair<Int, ImageView>>(AnnotationColors.PALETTE.size)

    /** The choices as they stand; exposed for the instrumentation tests. */
    internal var selectedStyle: String = AnnotationStyle.HIGHLIGHT
        private set
    internal var selectedColor: Int = AnnotationColors.DEFAULT
        private set
    internal val noteText: String get() = _binding?.annotationNote?.text?.toString().orEmpty()

    /** Non-zero while editing a stored highlight. */
    internal val annotationId: Long get() = requireArguments().getLong(ARG_ID)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val state = savedInstanceState ?: args
        val editing = annotationId != 0L
        val binding = DialogAnnotationBinding.inflate(layoutInflater).also { _binding = it }
        selectedStyle = AnnotationStyle.normalize(state.getString(ARG_STYLE))
        selectedColor = AnnotationColors.normalize(state.getInt(ARG_COLOR, AnnotationColors.DEFAULT))
        val quote = AnnotationPolicy.preview(args.getString(ARG_QUOTE))
        binding.annotationQuote.text = quote
        binding.annotationQuote.isVisible = !quote.isNullOrEmpty()
        binding.annotationStyle.check(if (selectedStyle == AnnotationStyle.UNDERLINE) R.id.annotation_style_underline else R.id.annotation_style_highlight)
        binding.annotationStyle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) selectedStyle = if (checkedId == R.id.annotation_style_underline) AnnotationStyle.UNDERLINE else AnnotationStyle.HIGHLIGHT
        }
        buildPalette(binding.annotationColors)
        if (savedInstanceState == null) binding.annotationNote.setText(args.getString(ARG_NOTE).orEmpty())
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(if (editing) R.string.text_annotation_edit else R.string.text_annotation_note)
            .setView(binding.root)
            .setPositiveButton(R.string.text_annotation_save) { _, _ -> save() }
            .setNegativeButton(R.string.dialog_button_cancel, null)
        if (editing) builder.setNeutralButton(R.string.text_annotation_delete) { _, _ -> delete() }
        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_STYLE, selectedStyle)
        outState.putInt(ARG_COLOR, selectedColor)
    }

    override fun onDestroyView() {
        _binding = null
        chips.clear()
        super.onDestroyView()
    }

    /** Applies the editor's choices: a new highlight for a fresh selection, an update for a stored one. */
    internal fun save() {
        val note = noteText
        if (annotationId != 0L) {
            host.saveAnnotation(annotationId, selectedStyle, selectedColor, note)
        } else {
            val locator = BundleCompat.getParcelable(requireArguments(), ARG_LOCATOR, Locator::class.java) ?: return
            host.addAnnotation(locator, selectedStyle, selectedColor, note)
        }
        dismissAllowingStateLoss()
    }

    internal fun delete() {
        if (annotationId != 0L) host.deleteAnnotation(annotationId)
        dismissAllowingStateLoss()
    }

    internal fun selectColor(color: Int) {
        selectedColor = AnnotationColors.normalize(color)
        chips.forEach { (chipColor, ring) -> ring.isVisible = chipColor == selectedColor }
    }

    internal fun selectStyle(style: String) {
        binding.annotationStyle.check(if (AnnotationStyle.normalize(style) == AnnotationStyle.UNDERLINE) R.id.annotation_style_underline else R.id.annotation_style_highlight)
    }

    internal fun setNote(note: String) {
        binding.annotationNote.setText(note)
    }

    private fun buildPalette(container: LinearLayout) {
        val density = resources.displayMetrics.density
        val size = (CHIP_SIZE_DP * density).toInt()
        val margin = (CHIP_MARGIN_DP * density).toInt()
        AnnotationColors.PALETTE.forEachIndexed { index, color ->
            val chip = FrameLayout(container.context)
            val dot = ImageView(container.context).apply {
                setImageResource(R.drawable.shape_annotation_dot)
                imageTintList = ColorStateList.valueOf(color)
            }
            val ring = ImageView(container.context).apply {
                setImageResource(R.drawable.shape_annotation_ring)
                isVisible = color == selectedColor
            }
            chip.addView(dot, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
            chip.addView(ring, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
            chip.contentDescription = getString(COLOR_NAMES.getOrElse(index) { R.string.text_annotation_color })
            chip.setOnClickListener { selectColor(color) }
            container.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = margin })
            chips += color to ring
        }
    }

    companion object {
        const val TAG = "reader-annotation-editor"
        private const val ARG_ID = "id"
        private const val ARG_LOCATOR = "locator"
        private const val ARG_QUOTE = "quote"
        private const val ARG_STYLE = "style"
        private const val ARG_COLOR = "color"
        private const val ARG_NOTE = "note"
        private const val CHIP_SIZE_DP = 36
        private const val CHIP_MARGIN_DP = 8
        private val COLOR_NAMES = listOf(
            R.string.text_annotation_color_yellow,
            R.string.text_annotation_color_green,
            R.string.text_annotation_color_blue,
            R.string.text_annotation_color_pink,
            R.string.text_annotation_color_purple,
        )

        /** The editor for a fresh [selection], opened with the last chosen [style] and [color]. */
        fun showNew(fragmentManager: FragmentManager, selection: Locator, style: String, color: Int) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            AnnotationDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID, 0L)
                    putParcelable(ARG_LOCATOR, selection)
                    putString(ARG_QUOTE, selection.text.highlight)
                    putString(ARG_STYLE, style)
                    putInt(ARG_COLOR, color)
                }
            }.show(fragmentManager, TAG)
        }

        /** The editor for the stored [annotation]. */
        fun showEdit(fragmentManager: FragmentManager, annotation: BookAnnotation) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            AnnotationDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID, annotation.id)
                    putString(ARG_QUOTE, annotation.quote)
                    putString(ARG_STYLE, annotation.style)
                    putInt(ARG_COLOR, annotation.color)
                    putString(ARG_NOTE, annotation.note)
                }
            }.show(fragmentManager, TAG)
        }
    }
}
