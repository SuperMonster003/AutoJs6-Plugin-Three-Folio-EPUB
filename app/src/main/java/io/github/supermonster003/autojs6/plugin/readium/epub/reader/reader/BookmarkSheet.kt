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
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.SheetBookmarksBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.Bookmark
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The bookmarks panel (roadmap P2.6): a bottom sheet listing the open book's bookmarks newest
 * first, each with its chapter, an excerpt of the first visible element and the time it was
 * added. Tapping a row jumps there and closes the sheet, the trash icon deletes one, the header
 * adds the current page (disabled while the page already has a bookmark, in step with the
 * toolbar icon) or clears them all after a confirmation. Everything shown comes from the
 * [EpubReaderActivity], so the sheet has no state of its own to lose.
 */
internal class BookmarkSheet : BottomSheetDialogFragment() {

    private var _binding: SheetBookmarksBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val host: EpubReaderActivity get() = requireActivity() as EpubReaderActivity

    private val adapter = BookmarksAdapter(onOpen = ::open, onDelete = { host.removeBookmark(it.id) })

    /** The rows currently listed, newest first; exposed for the instrumentation tests. */
    internal val rows: List<Bookmark> get() = adapter.rows

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SheetBookmarksBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?): Unit = with(binding) {
        bookmarkAdd.setOnClickListener { host.addBookmark() }
        bookmarkClear.setOnClickListener { confirmClear() }
        bookmarkList.layoutManager = LinearLayoutManager(requireContext())
        bookmarkList.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(host.bookmarks, host.currentBookmark) { bookmarks, current -> bookmarks to current }
                    .collect { (bookmarks, current) -> render(bookmarks, current) }
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

    /** Opens the [index]th listed bookmark (newest first) in the reader and closes the sheet. */
    internal fun open(index: Int) {
        val bookmark = adapter.rows.getOrNull(index) ?: return
        host.openBookmark(bookmark)
        dismiss()
    }

    private fun open(bookmark: Bookmark) {
        host.openBookmark(bookmark)
        dismiss()
    }

    private fun confirmClear() {
        val count = host.bookmarks.value.size
        if (count == 0) return
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.text_bookmark_clear_all_message, count))
            .setPositiveButton(R.string.dialog_button_confirm) { _, _ -> host.clearBookmarks() }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    private fun render(bookmarks: List<Bookmark>, current: Bookmark?) = with(binding) {
        bookmarkAdd.isEnabled = current == null
        bookmarkClear.isEnabled = bookmarks.isNotEmpty()
        bookmarkEmpty.isVisible = bookmarks.isEmpty()
        bookmarkList.isVisible = bookmarks.isNotEmpty()
        adapter.submit(BookmarkPolicy.listed(bookmarks))
    }

    private class BookmarksAdapter(
        private val onOpen: (Bookmark) -> Unit,
        private val onDelete: (Bookmark) -> Unit,
    ) : RecyclerView.Adapter<BookmarksAdapter.Holder>() {

        var rows: List<Bookmark> = emptyList()
            private set

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val chapter: TextView = view.findViewById(R.id.bookmark_chapter)
            val snippet: TextView = view.findViewById(R.id.bookmark_snippet)
            val time: TextView = view.findViewById(R.id.bookmark_time)
            val delete: ImageButton = view.findViewById(R.id.bookmark_delete)
        }

        fun submit(rows: List<Bookmark>) {
            this.rows = rows
            @Suppress("NotifyDataSetChanged")
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = rows.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val bookmark = rows[position]
            val chapter = bookmark.chapter ?: bookmark.href.substringAfterLast('/')
            holder.chapter.text = chapter
            holder.snippet.text = bookmark.snippet
            // The first visible element of a chapter's first page is usually its heading: no need to show it twice.
            holder.snippet.isVisible = !bookmark.snippet.isNullOrEmpty() && !bookmark.snippet.equals(chapter, ignoreCase = true)
            holder.time.text = DateUtils.formatDateTime(holder.itemView.context, bookmark.createdAtMillis, TIME_FLAGS)
            holder.itemView.setOnClickListener { onOpen(bookmark) }
            holder.delete.setOnClickListener { onDelete(bookmark) }
        }

        private companion object {
            const val TIME_FLAGS = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
        }
    }

    companion object {
        const val TAG = "reader-bookmarks"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            BookmarkSheet().show(fragmentManager, TAG)
        }
    }
}
