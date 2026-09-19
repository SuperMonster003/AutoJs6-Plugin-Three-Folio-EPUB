@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.readium.epub.reader

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.content.pm.ServiceInfo
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts.TtsForegroundService
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.explorer.api.ExplorerActionCapabilityKeys
import org.autojs.plugin.explorer.api.ExplorerActionCatalogKeys
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionPluginIds
import org.autojs.plugin.explorer.api.ExplorerActionPluginPermissions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PluginContractInstrumentationTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun servicesReturnBindersForExplicitActionlessBinding() {
        val explorer = Intent().setComponent(ComponentName(context, ExplorerActionService::class.java))
        val info = Intent().setComponent(ComponentName(context, PluginInfoService::class.java))

        assertNotNull(ExplorerActionService().onBind(explorer))
        assertNotNull(PluginInfoService().onBind(info))
    }

    @Test
    fun pluginInfoDeclaresAbiIndependentExplorerEngine() {
        val info = context.readiumEpubReaderPluginInfo()

        assertEquals("Readium EPUB Reader", info.name)
        assertEquals(context.getString(R.string.plugin_description), info.description)
        assertEquals(ReadiumEpubReaderPlugin.ID, info.id)
        assertEquals(ExplorerActionPluginIds.ENGINE, info.engine)
        assertEquals(ReadiumEpubReaderPlugin.VARIANT, info.variant)
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertTrue(info.instruction?.isNotBlank() == true)
        assertTrue(info.versionCode > 0)
        assertTrue(info.versionName.isNotBlank())
        assertEquals(
            ReadiumEpubReaderPlugin.REQUIRED_HOST_VERSION,
            info.capabilities?.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION),
        )
        assertEquals(
            ReadiumEpubReaderPlugin.PROTOCOL_VERSION,
            info.capabilities?.getInt(ExplorerActionCapabilityKeys.PROTOCOL_VERSION),
        )
        assertEquals(2, ReadiumEpubReaderPlugin.PROTOCOL_VERSION)
    }

    @Test
    fun generatedIdentityResourcesMatchTheKotlinConstants() {
        assertEquals(ReadiumEpubReaderPlugin.ID, context.getString(R.string.plugin_id))
        assertEquals(ExplorerActionPluginIds.ENGINE, context.getString(R.string.plugin_engine))
        assertEquals(ReadiumEpubReaderPlugin.VARIANT, context.getString(R.string.plugin_variant))
        assertEquals(
            ReadiumEpubReaderPlugin.REQUIRED_HOST_VERSION.toString(),
            context.getString(R.string.plugin_requires_host_version),
        )
        assertEquals("SuperMonster003", context.getString(R.string.plugin_author))
    }

    @Test
    fun catalogUsesParcelableBundleAndStringArrayLists() {
        val catalog = readiumEpubReaderActionCatalog()
        val actions = catalog.getParcelableArrayList<Bundle>(ExplorerActionCatalogKeys.ACTIONS)
        val action = actions?.single { it.getString(ExplorerActionCatalogKeys.ID) == ReadiumEpubReaderPlugin.ID }
        assertEquals(2, actions?.size)
        val primary = actions?.single { it.getString(ExplorerActionCatalogKeys.ID) == ReadiumEpubReaderPlugin.PRIMARY_ACTION_ID }
        assertEquals(2, primary?.getInt(ExplorerActionCatalogKeys.PLACEMENT))
        assertEquals(ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME, primary?.getString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME))

        assertEquals(
            ReadiumEpubReaderPlugin.PROTOCOL_VERSION,
            catalog.getInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION),
        )
        assertNotNull(action)
        assertEquals("action_readium_epub_reader", action?.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME))
        assertEquals(ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME, action?.getString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME))
        assertEquals(listOf("application/epub+zip"), action?.getStringArrayList(ExplorerActionCatalogKeys.MIME_TYPES))
        assertEquals(listOf("epub"), action?.getStringArrayList(ExplorerActionCatalogKeys.EXTENSIONS))
        assertEquals(
            setOf(
                ExplorerActionCatalogKeys.ID,
                ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME,
                ExplorerActionCatalogKeys.LABEL_FALLBACK,
                ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME,
                ExplorerActionCatalogKeys.PRIORITY,
                ExplorerActionCatalogKeys.TARGET_KIND,
                ExplorerActionCatalogKeys.ACCESS_MODE,
                ExplorerActionCatalogKeys.PLACEMENT,
                ExplorerActionCatalogKeys.MIME_TYPES,
                ExplorerActionCatalogKeys.EXTENSIONS,
            ),
            action?.keySet(),
        )

        val labelResourceName = action?.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME).orEmpty()
        assertTrue(context.resources.getIdentifier(labelResourceName, "string", context.packageName) != 0)
        assertEquals(Class.forName(ReadiumEpubReaderPlugin.ACTIVITY_CLASS_NAME), EpubReaderActivity::class.java)
    }

    @Test
    fun manifestProtectsAndExportsTheDiscoveryAndExecutionComponents() {
        val packageManager = context.packageManager
        val explorerService = packageManager.getServiceInfo(ComponentName(context, ExplorerActionService::class.java), 0)
        val infoService = packageManager.getServiceInfo(ComponentName(context, PluginInfoService::class.java), 0)
        val activityInfo = packageManager.getActivityInfo(ComponentName(context, EpubReaderActivity::class.java), 0)
        val wakeInfo = packageManager.getActivityInfo(ComponentName(context, WakeActivity::class.java), 0)

        val protectedComponents = listOf(
            explorerService to explorerService.permission,
            infoService to infoService.permission,
            activityInfo to activityInfo.permission,
            wakeInfo to wakeInfo.permission,
        )
        for ((component, permission) in protectedComponents) {
            assertTrue(component.name, component.exported)
            assertEquals(component.name, ExplorerActionPluginPermissions.PLUGIN, permission)
        }

        val discovery = packageManager.queryIntentServices(
            Intent(ExplorerActionPluginActions.EXPLORER_ACTION).setPackage(context.packageName),
            0,
        )
        assertTrue(discovery.any { it.serviceInfo.name == ExplorerActionService::class.java.name })

        val info = packageManager.queryIntentServices(
            Intent("org.autojs.plugin.INFO").setPackage(context.packageName),
            0,
        )
        assertTrue(info.any { it.serviceInfo.name == PluginInfoService::class.java.name })

        val execution = packageManager.queryIntentActivities(
            Intent(ExplorerActionPluginActions.EXECUTE)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage(context.packageName),
            0,
        )
        assertTrue(execution.any { it.activityInfo.name == EpubReaderActivity::class.java.name })

        val wake = packageManager.queryIntentActivities(
            Intent("org.autojs.plugin.action.WAKE")
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage(context.packageName),
            0,
        )
        assertTrue(wake.any { it.activityInfo.name == WakeActivity::class.java.name })

        // The read-aloud service (roadmap P3 / D15) is the only other service: not exported, media playback type.
        val ttsService = packageManager.getServiceInfo(ComponentName(context, TtsForegroundService::class.java), 0)
        assertFalse(ttsService.exported)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, ttsService.foregroundServiceType)
        }
        val mediaSessionServices = packageManager.queryIntentServices(
            Intent("androidx.media3.session.MediaSessionService").setPackage(context.packageName),
            0,
        )
        assertEquals(listOf(TtsForegroundService::class.java.name), mediaSessionServices.map { it.serviceInfo.name })
    }

    /** Roadmap D15 / D19: the network permission, the plugin permission and the three read-aloud permissions, nothing else. */
    @Test
    fun manifestRequestsOnlyTheInternetPluginAndReadAloudPermissions() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val receiverProtectionPermission =
            "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

        // API 37 splits ACCESS_LOCAL_NETWORK off INTERNET for packages that target an older SDK,
        // so the installed package lists a permission the manifest never declares.
        val splitFromInternet =
            if (android.os.Build.VERSION.SDK_INT >= 37 && context.applicationInfo.targetSdkVersion < 37) {
                setOf("android.permission.ACCESS_LOCAL_NETWORK")
            } else {
                emptySet()
            }

        assertEquals(
            setOf(
                android.Manifest.permission.INTERNET,
                ExplorerActionPluginPermissions.PLUGIN,
                receiverProtectionPermission,
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
                "android.permission.POST_NOTIFICATIONS",
            ) + splitFromInternet,
            packageInfo.requestedPermissions.orEmpty().toSet(),
        )
        val permissionInfo = context.packageManager.getPermissionInfo(receiverProtectionPermission, 0)
        assertEquals(
            PermissionInfo.PROTECTION_SIGNATURE,
            permissionInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE,
        )
    }
}
