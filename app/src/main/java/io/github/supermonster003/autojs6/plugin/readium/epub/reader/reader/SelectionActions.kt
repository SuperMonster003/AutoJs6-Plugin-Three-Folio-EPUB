package io.github.supermonster003.autojs6.plugin.readium.epub.reader.reader

import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.EpubReaderActivity
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.R

/** An installed activity that handles `ACTION_PROCESS_TEXT` (translate, define, ...). */
internal data class ProcessTextTarget(val label: String, val packageName: String, val className: String)

/**
 * What the text-selection toolbar can do with the selected text (roadmap P2.7): the intents are
 * built here so the Activity only launches them.
 */
internal object SelectionActions {

    const val MIME_TEXT = "text/plain"

    /** Menu group of the dynamically added text processors; their `order` is the index in the list. */
    const val GROUP_PROCESS = 1

    /** The system's own selection toolbar shows a handful too; more would bury copy and share. */
    const val MAX_PROCESSORS = 4

    fun processTextTargets(packageManager: PackageManager): List<ProcessTextTarget> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
        val probe = Intent(Intent.ACTION_PROCESS_TEXT).setType(MIME_TEXT)
        return packageManager.queryIntentActivities(probe, 0)
            .take(MAX_PROCESSORS)
            .map { ProcessTextTarget(it.loadLabel(packageManager).toString(), it.activityInfo.packageName, it.activityInfo.name) }
    }

    fun shareIntent(text: String): Intent =
        Intent.createChooser(Intent(Intent.ACTION_SEND).setType(MIME_TEXT).putExtra(Intent.EXTRA_TEXT, text), null)

    fun webSearchIntent(text: String): Intent =
        Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, text)

    @RequiresApi(Build.VERSION_CODES.M)
    fun processTextIntent(target: ProcessTextTarget, text: String): Intent =
        Intent(Intent.ACTION_PROCESS_TEXT)
            .setType(MIME_TEXT)
            .setClassName(target.packageName, target.className)
            .putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
}

/**
 * The selection toolbar the navigator's web views show instead of the system one: copy, share,
 * web search, and one entry per text processor. The selected text is fetched from the navigator
 * when the toolbar appears (and again on each refresh), because by the time an item is clicked
 * the navigator has already started clearing the selection.
 */
internal class SelectionActionMode(private val host: EpubReaderActivity) : ActionMode.Callback {

    private var processors: List<ProcessTextTarget> = emptyList()

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_text_selection, menu)
        processors = SelectionActions.processTextTargets(host.packageManager)
        processors.forEachIndexed { index, target ->
            menu.add(SelectionActions.GROUP_PROCESS, Menu.NONE, index, target.label).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        host.rememberSelection()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        host.rememberSelection()
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val handled = if (item.groupId == SelectionActions.GROUP_PROCESS) {
            processors.getOrNull(item.order)?.let { host.processSelection(it) } ?: false
        } else {
            host.performSelectionAction(item.itemId)
        }
        if (handled) mode.finish()
        return handled
    }

    override fun onDestroyActionMode(mode: ActionMode) = Unit
}
