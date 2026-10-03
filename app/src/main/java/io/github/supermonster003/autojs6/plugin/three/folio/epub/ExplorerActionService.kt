package io.github.supermonster003.autojs6.plugin.three.folio.epub

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.explorer.api.IExplorerActionPlugin

/** `org.autojs.plugin.EXPLORER_ACTION` discovery service (roadmap D9 / D10). */
class ExplorerActionService : Service() {

    private val binder = object : IExplorerActionPlugin.Stub() {
        override fun getInfo() = threeFolioExplorerPluginInfo().apply { supportedAbis = emptyArray() }

        override fun getActionCatalog() = threeFolioEpubActionCatalog()
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
