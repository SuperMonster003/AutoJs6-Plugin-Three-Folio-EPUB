package io.github.supermonster003.autojs6.plugin.readium.epub.reader.update

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings.MarkdownLite
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.settings.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.store.ReaderSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The manual update check (roadmap P4.3 / D28) behind the settings row: at most one fetch per
 * day through [UpdateSchedulePolicy] (the cached answer is shown again within the day), a
 * cancelable progress dialog while the network is busy, and a dialog for a newer release with
 * the release page, the built-in history and "ignore this version" (undone by the same button
 * later). Nothing is downloaded; the release page opens in the browser. Failures and
 * "up to date" are toasts. The stored state (last check, cached release, ignored tag) lives in
 * [ReaderSettings].
 */
internal class AppUpdateCoordinator(
    private val activity: AppCompatActivity,
    private val settings: ReaderSettings,
    private val installedVersionName: String?,
    private val source: UpdateSource? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private var job: Job? = null
    private var progress: AlertDialog? = null

    /** The release dialog on screen, if any (the instrumentation test drives its buttons). */
    internal var dialog: AlertDialog? = null
        private set

    /** True while a fetch is in flight. */
    val isChecking: Boolean get() = job?.isActive == true

    /** Called on the main thread after the stored check state changed, so the row can refresh. */
    var onChanged: (() -> Unit)? = null

    /** The settings row: reuse today's answer or fetch, then show what was found. */
    fun check() {
        if (isChecking) return
        if (!UpdateSchedulePolicy.manualFetchDue(settings.lastUpdateCheckAt, clock())) {
            toast(activity.getString(R.string.text_update_cached))
            val cached = settings.cachedRelease?.let(ReleaseInfoCodec::decode)
            if (cached == null) toast(activity.getString(R.string.text_update_no_release)) else present(cached)
            return
        }
        showProgress()
        job = activity.lifecycleScope.launch {
            val result = try {
                (sourceOverride ?: source ?: AppUpdateRepository()).fetchLatest()
            } finally {
                dismissProgress()
            }
            when (result) {
                is UpdateFetchResult.Success -> {
                    settings.lastUpdateCheckAt = clock()
                    settings.cachedRelease = result.release?.let(ReleaseInfoCodec::encode)
                    onChanged?.invoke()
                    if (result.release == null) toast(activity.getString(R.string.text_update_no_release)) else present(result.release)
                }
                is UpdateFetchResult.Failure -> toast(failureText(result))
            }
        }
    }

    /** The owning screen goes away: stop the fetch and drop the dialogs. */
    fun dismiss() {
        job?.cancel()
        dismissProgress()
        dialog?.dismiss()
        dialog = null
    }

    private fun present(release: ReleaseInfo) {
        if (!AppVersionPolicy.isNewer(release.tagName, installedVersionName)) {
            toast(activity.getString(R.string.text_update_up_to_date))
            return
        }
        val ignored = AppVersionPolicy.isIgnored(release.tagName, settings.ignoredUpdateVersion)
        val title = activity.getString(
            if (release.preRelease) R.string.text_update_available_pre_release else R.string.text_update_available,
            release.tagName,
        )
        val message = buildString {
            append(activity.getString(R.string.text_update_installed, installedVersionName ?: "?"))
            release.publishedAt?.takeIf { it.length >= DATE_LENGTH }?.let {
                append('\n').append(activity.getString(R.string.text_update_published, it.take(DATE_LENGTH)))
            }
            release.notes?.takeIf { it.isNotBlank() }?.let {
                append("\n\n").append(MarkdownLite.plainText(MarkdownLite.parse(it)))
            }
        }
        dialog?.dismiss()
        dialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.text_update_open_release) { _, _ -> openReleasePage(release.htmlUrl) }
            .setNeutralButton(R.string.release_history) { _, _ ->
                activity.startActivity(Intent(activity, ReleaseHistoryActivity::class.java))
            }
            .setNegativeButton(if (ignored) R.string.text_update_unignore else R.string.text_update_ignore) { _, _ ->
                settings.ignoredUpdateVersion = if (ignored) null else release.tagName
                if (!ignored) toast(activity.getString(R.string.text_update_ignored, release.tagName))
                onChanged?.invoke()
            }
            .setOnDismissListener { dialog = null }
            .show()
    }

    private fun openReleasePage(url: String) {
        if (!ReleaseInfoCodec.isReleasePage(url)) return
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            toast(activity.getString(R.string.text_no_app_for_action))
        }
    }

    private fun failureText(failure: UpdateFetchResult.Failure): String = when (failure.reason) {
        UpdateFailure.NETWORK -> activity.getString(R.string.text_update_failed_network)
        else -> activity.getString(R.string.text_update_failed_server, failure.detail ?: failure.reason.name)
    }

    private fun showProgress() {
        dismissProgress()
        progress = AlertDialog.Builder(activity)
            .setMessage(R.string.text_update_checking)
            .setNegativeButton(android.R.string.cancel) { _, _ -> job?.cancel() }
            .setOnCancelListener { job?.cancel() }
            .show()
    }

    private fun dismissProgress() {
        progress?.dismiss()
        progress = null
    }

    private fun toast(text: String) = Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()

    companion object {
        private const val DATE_LENGTH = 10

        /** Instrumentation tests put a fake here before the settings page starts; production never sets it. */
        @VisibleForTesting
        @Volatile
        var sourceOverride: UpdateSource? = null
    }
}
