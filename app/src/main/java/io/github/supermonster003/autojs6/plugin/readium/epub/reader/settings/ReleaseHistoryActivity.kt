package io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.databinding.ActivityReleaseHistoryBinding
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.loadReleaseHistory
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.releaseHistoryCandidates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The bundled changelog in the host's language (roadmap P4.3), rendered from the CHANGELOG assets
 * through [MarkdownLite] and [MarkdownRenderer]; reached from the settings page and from the
 * update dialog. Not exported and takes no data.
 */
internal class ReleaseHistoryActivity : HostAppearanceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityReleaseHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        fitToolbarAndContent(binding.root, binding.toolbar, binding.history)
        binding.history.movementMethod = LinkMovementMethod.getInstance()
        val locale = resources.configuration.locales[0]
        lifecycleScope.launch {
            val markdown = withContext(Dispatchers.IO) {
                loadReleaseHistory(releaseHistoryCandidates(locale.language, locale.country, locale.script)) { path ->
                    assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            }
            binding.history.text = markdown
                ?.let { MarkdownRenderer.render(this@ReleaseHistoryActivity, MarkdownLite.parse(it), ::openLink) }
                ?: getString(R.string.release_history_unavailable)
        }
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.text_no_app_for_action, Toast.LENGTH_SHORT).show()
        }
    }
}
