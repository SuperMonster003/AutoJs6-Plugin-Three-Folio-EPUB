package io.github.supermonster003.autojs6.plugin.three.folio.epub.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Process
import org.autojs.plugin.epub.api.EpubActions

/**
 * Who may call the EPUB service (roadmap P5.2 / D10). The manifest permission already limits
 * callers to apps holding `org.autojs.permission.PLUGIN`; this guard additionally requires the
 * calling uid to own the AutoJs6 host package and to be signed like this plugin. Calls from the
 * plugin's own uid (the instrumentation runner) pass only in debug builds.
 */
internal class CallerGuard(
    private val context: Context,
    private val allowOwnUid: Boolean,
    private val packaged: Boolean = false,
    private val ownerUid: Int? = null,
) {

    private val packageManager: PackageManager = context.applicationContext.packageManager

    /** Must run on the Binder thread of the call, before any dispatch, so the calling uid is the caller's. */
    fun check() {
        val uid = Binder.getCallingUid()
        if (ownerUid != null && uid != ownerUid) {
            throw SecurityException("The EPUB handle belongs to another uid")
        }
        if (uid == Process.myUid()) {
            if (allowOwnUid) return
            throw SecurityException("The EPUB service does not serve its own uid")
        }
        if (packaged) {
            if (context.checkPermission(EpubActions.PACKAGED_PERMISSION, Binder.getCallingPid(), uid) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("The caller lacks the EPUB runtime permission")
            }
            return
        }
        val packages = packageManager.getPackagesForUid(uid).orEmpty()
        if (packages.none { it in HOST_PACKAGES }) {
            throw SecurityException("The EPUB service only serves the AutoJs6 host")
        }
        if (packageManager.checkSignatures(uid, Process.myUid()) != PackageManager.SIGNATURE_MATCH) {
            throw SecurityException("The caller is not signed like the EPUB plugin")
        }
    }

    fun forCaller(): CallerGuard {
        check()
        return CallerGuard(context, allowOwnUid, packaged, Binder.getCallingUid())
    }

    companion object {
        /** Packages whose uid may use the service; the plugin center authorization lives on the host side. */
        val HOST_PACKAGES: Set<String> = setOf("org.autojs.autojs6")
    }
}
