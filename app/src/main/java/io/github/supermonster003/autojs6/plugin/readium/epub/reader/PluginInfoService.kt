package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.common.api.IPluginInfoProvider

/** `org.autojs.plugin.INFO` service used by the host plugin center and the appearance provider. */
class PluginInfoService : Service() {
    private val binder = object : IPluginInfoProvider.Stub() {
        override fun getInfo() = readiumEpubReaderPluginInfo().apply { supportedAbis = emptyArray() }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
