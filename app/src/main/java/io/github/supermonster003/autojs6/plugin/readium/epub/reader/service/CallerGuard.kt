package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Process

/**
 * Who may call the EPUB service (roadmap P5.2 / D10). The manifest permission already limits
 * callers to apps holding `org.autojs.permission.PLUGIN`; this guard additionally requires the
 * calling uid to own the AutoJs6 host package and to be signed like this plugin. Calls from the
 * plugin's own uid (the instrumentation runner) pass only in debug builds.
 */
internal class CallerGuard(context: Context, private val allowOwnUid: Boolean) {

    private val packageManager: PackageManager = context.applicationContext.packageManager

    /** Must run on the Binder thread of the call, before any dispatch, so the calling uid is the caller's. */
    fun check() {
        val uid = Binder.getCallingUid()
        if (uid == Process.myUid()) {
            if (allowOwnUid) return
            throw SecurityException("The EPUB service does not serve its own uid")
        }
        val packages = packageManager.getPackagesForUid(uid).orEmpty()
        if (packages.none { it in HOST_PACKAGES }) {
            throw SecurityException("The EPUB service only serves the AutoJs6 host")
        }
        if (packageManager.checkSignatures(uid, Process.myUid()) != PackageManager.SIGNATURE_MATCH) {
            throw SecurityException("The caller is not signed like the EPUB plugin")
        }
    }

    companion object {
        /** Packages whose uid may use the service; the plugin center authorization lives on the host side. */
        val HOST_PACKAGES: Set<String> = setOf("org.autojs.autojs6")
    }
}
