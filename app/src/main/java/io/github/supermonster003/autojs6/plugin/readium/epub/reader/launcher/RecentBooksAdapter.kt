package io.github.supermonster003.autojs6.plugin.readium.epub.reader.launcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ItemRecentBookBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The launcher grid (roadmap P4.1); covers decode off the main thread and stay in a small cache. */
internal class RecentBooksAdapter(
    private val scope: CoroutineScope,
    private val store: RecentBooksStore,
    private val onOpen: (RecentBook) -> Unit,
    private val onLongPress: (RecentBook) -> Unit,
) : ListAdapter<RecentBook, RecentBooksAdapter.Holder>(DIFF) {

    private val covers = LruCache<String, Bitmap>(COVER_CACHE_SIZE)

    class Holder(val binding: ItemRecentBookBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemRecentBookBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val book = getItem(position)
        val binding = holder.binding
        val context = binding.root.context
        val title = book.title ?: book.displayName.removeSuffix(".epub").removeSuffix(".EPUB")
        binding.title.text = title
        binding.author.text = book.author
        binding.author.isVisible = book.author != null
        val percent = RecentBooksPolicy.progressPercent(book.progression)
        binding.progress.text = when {
            !book.available -> context.getString(R.string.text_launcher_unavailable)
            percent != null -> context.getString(R.string.text_launcher_progress, percent)
            else -> context.getString(R.string.text_launcher_not_started)
        }
        binding.time.text = DateUtils.getRelativeTimeSpanString(
            maxOf(book.lastReadAt, book.addedAt),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        )
        binding.root.alpha = if (book.available) 1f else UNAVAILABLE_ALPHA
        binding.cover.contentDescription = context.getString(R.string.text_launcher_cover, title)
        binding.cover.setImageResource(R.drawable.ic_menu_book_24)
        binding.cover.tag = book.coverFile
        val coverFile = book.coverFile
        if (coverFile != null) {
            val cached = covers.get(coverFile)
            if (cached != null) {
                binding.cover.setImageBitmap(cached)
            } else {
                scope.launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        runCatching { BitmapFactory.decodeFile(store.coverFile(coverFile).path) }.getOrNull()
                    } ?: return@launch
                    covers.put(coverFile, bitmap)
                    if (binding.cover.tag == coverFile) binding.cover.setImageBitmap(bitmap)
                }
            }
        }
        binding.root.setOnClickListener { onOpen(book) }
        binding.root.setOnLongClickListener {
            onLongPress(book)
            true
        }
    }

    private companion object {
        const val COVER_CACHE_SIZE = 64
        const val UNAVAILABLE_ALPHA = 0.5f

        val DIFF = object : DiffUtil.ItemCallback<RecentBook>() {
            override fun areItemsTheSame(oldItem: RecentBook, newItem: RecentBook): Boolean = oldItem.uri == newItem.uri
            override fun areContentsTheSame(oldItem: RecentBook, newItem: RecentBook): Boolean = oldItem == newItem
        }
    }
}
