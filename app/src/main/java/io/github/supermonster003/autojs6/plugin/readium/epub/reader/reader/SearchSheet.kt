package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.getSystemService
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
import com.google.android.material.color.MaterialColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.SheetSearchBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs.ReaderThemeColors
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchGrouping
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchRow
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchSnippet
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchState
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.search.SearchStatus
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator

/**
 * The full-text search panel (roadmap P2.5): a bottom sheet with the query field, a status line
 * (progress, hit count, hints, a cancel button while loading) and the hits grouped by chapter
 * with their context. Everything shown comes from the [EpubReaderActivity]'s search state, so the
 * sheet can be dismissed and reopened without losing the results; scrolling near the end of the
 * list asks for the next page, and tapping a hit opens it in the reader and closes the sheet.
 */
internal class SearchSheet : BottomSheetDialogFragment() {

    private var _binding: SheetSearchBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val host: EpubReaderActivity get() = requireActivity() as EpubReaderActivity

    private val adapter = ResultsAdapter(::openResult)

    /** The query the field was last filled from, so the state does not overwrite what is being typed. */
    private var renderedQuery: String? = null

    /** The rows currently listed; exposed for the instrumentation tests. */
    internal val rows: List<SearchRow<Locator>> get() = adapter.rows

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SheetSearchBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?): Unit = with(binding) {
        query.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submit()
                true
            } else {
                false
            }
        }
        searchCancel.setOnClickListener { host.cancelSearch() }
        searchResults.layoutManager = LinearLayoutManager(requireContext())
        searchResults.adapter = adapter
        searchResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val layout = recyclerView.layoutManager as LinearLayoutManager
                if (layout.findLastVisibleItemPosition() >= adapter.itemCount - LOAD_MORE_MARGIN) host.loadMoreSearchResults()
            }
        })
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                host.searchState.collect { render(it) }
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
        val window = dialog?.window ?: return
        val hasQuery = host.searchState.value.query.isNotEmpty()
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                if (hasQuery) WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN else WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE,
        )
        if (!hasQuery) binding.query.requestFocus()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /** Runs the query in the field; the instrumentation tests call it with a query of their own. */
    internal fun submit(query: String? = null) {
        if (query != null) {
            renderedQuery = query
            binding.query.setText(query)
            binding.query.setSelection(query.length)
        }
        hideKeyboard()
        host.submitSearch(binding.query.text?.toString().orEmpty())
    }

    /** Opens the [index]th hit in the reader and closes the sheet. */
    internal fun openResult(index: Int) {
        host.openSearchResult(index)
        dismiss()
    }

    private fun render(state: SearchState) = with(binding) {
        if (renderedQuery != state.query) {
            renderedQuery = state.query
            query.setText(state.query)
            query.setSelection(state.query.length)
        }
        searchProgress.isVisible = state.loading
        searchCancel.isVisible = state.loading
        searchStatus.text = statusText(state)
        val publication = host.readerModel.publication
        adapter.submit(
            SearchGrouping.rows(
                state.results,
                keyOf = { it.href.toString() },
                titleOf = { locator -> locator.title ?: publication?.let { TocSheet.chapterTitle(it, locator.href.toString()) } },
            ),
        )
    }

    private fun statusText(state: SearchState): String = when (state.status) {
        SearchStatus.IDLE -> ""
        SearchStatus.TOO_SHORT -> getString(R.string.text_search_too_short)
        SearchStatus.SEARCHING -> getString(R.string.text_search_searching)
        SearchStatus.DONE -> when {
            state.results.isEmpty() -> getString(R.string.text_search_no_results, state.query)
            state.truncated -> getString(R.string.text_search_result_count_truncated, state.results.size)
            state.exhausted -> getString(R.string.text_search_result_count, state.results.size)
            else -> getString(R.string.text_search_result_count_more, state.results.size)
        }
        SearchStatus.CANCELLED -> getString(R.string.text_search_cancelled, state.results.size)
        SearchStatus.FAILED -> getString(R.string.text_search_failed)
        SearchStatus.NOT_SEARCHABLE -> getString(R.string.text_search_not_searchable)
    }

    private fun hideKeyboard() {
        val view = _binding?.query ?: return
        view.clearFocus()
        requireContext().getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /** Chapter headers and hits; the hit text is the trimmed snippet with the match emphasized. */
    private class ResultsAdapter(private val onHit: (Int) -> Unit) : RecyclerView.Adapter<ResultsAdapter.Holder>() {

        var rows: List<SearchRow<Locator>> = emptyList()
            private set

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view as TextView
        }

        fun submit(rows: List<SearchRow<Locator>>) {
            this.rows = rows
            @Suppress("NotifyDataSetChanged")
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = rows.size

        override fun getItemViewType(position: Int): Int = when (rows[position]) {
            is SearchRow.Header -> TYPE_HEADER
            is SearchRow.Hit -> TYPE_HIT
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val layout = if (viewType == TYPE_HEADER) R.layout.item_search_header else R.layout.item_search_result
            return Holder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            when (val row = rows[position]) {
                is SearchRow.Header -> holder.text.text = row.title
                is SearchRow.Hit -> {
                    holder.text.text = snippet(holder.text, row.item)
                    holder.itemView.setOnClickListener { onHit(row.index) }
                }
            }
        }

        private fun snippet(view: TextView, locator: Locator): CharSequence {
            val text = locator.text
            val snippet = SearchSnippet.trim(text.before, text.highlight, text.after)
            val accent = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSecondary)
            return SpannableStringBuilder().apply {
                append(snippet.before)
                val start = length
                append(snippet.highlight)
                setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(BackgroundColorSpan(ReaderThemeColors.withAlpha(accent, HIGHLIGHT_ALPHA)), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                append(snippet.after)
            }
        }

        private companion object {
            const val TYPE_HEADER = 0
            const val TYPE_HIT = 1
            const val HIGHLIGHT_ALPHA = 0.3
        }
    }

    companion object {
        const val TAG = "reader-search"

        /** Rows from the end at which the next page is requested. */
        private const val LOAD_MORE_MARGIN = 8

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            SearchSheet().show(fragmentManager, TAG)
        }
    }
}
