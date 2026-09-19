package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderViewModel
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.DialogImageViewerBinding
import kotlinx.coroutines.launch

/**
 * A tapped image, full screen on black with its caption (roadmap P2.7): look, then tap anywhere
 * to go back. No zoom and no saving; the image is read from the open book and downsampled to
 * the screen.
 */
internal class ImageViewerDialog : DialogFragment() {

    private val model: EpubReaderViewModel by activityViewModels()
    private var binding: DialogImageViewerBinding? = null

    /** Test hooks: what was asked for, and whether it is on screen. */
    internal val href: String? get() = arguments?.getString(ARG_HREF)
    internal val hasImage: Boolean get() = binding?.imageView?.drawable != null
    internal val captionText: CharSequence? get() = binding?.imageCaption?.takeIf { it.isVisible }?.text

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DialogImageViewerBinding.inflate(inflater, container, false).also { binding = it }
        val caption = arguments?.getString(ARG_CAPTION)?.takeIf { it.isNotBlank() }
        binding.imageCaption.text = caption
        binding.imageCaption.isVisible = caption != null
        binding.root.setOnClickListener { dismiss() }
        load(binding)
        return binding.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun load(binding: DialogImageViewerBinding) {
        val href = href ?: return
        val metrics = resources.displayMetrics
        val maxSide = maxOf(metrics.widthPixels, metrics.heightPixels, ImageDecoding.MIN_MAX_SIDE)
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = model.decodeImage(href, maxSide)
            if (bitmap != null) {
                binding.imageView.setImageBitmap(bitmap)
            } else {
                binding.imageCaption.setText(R.string.text_image_unavailable)
                binding.imageCaption.isVisible = true
            }
        }
    }

    companion object {
        const val TAG = "reader-image"
        private const val ARG_HREF = "href"
        private const val ARG_CAPTION = "caption"

        fun show(manager: FragmentManager, href: String, caption: String?) {
            if (manager.findFragmentByTag(TAG) != null) return
            ImageViewerDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_HREF, href)
                    putString(ARG_CAPTION, caption)
                }
            }.show(manager, TAG)
        }
    }
}
