package com.rotguard.app.storage

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LaunchableAppRecord(
    val packageName: AppPackageName,
    val displayName: String,
    val isRecommendedBrainrotTarget: Boolean,
)

data class PermissionStatusSnapshot(
    val accessibilityEnabled: Boolean,
    val overlayEnabled: Boolean,
    val usageStatsEnabled: Boolean,
)

interface SystemPermissionInspector {
    fun inspectPermissions(): PermissionStatusSnapshot
}

interface InstalledAppCatalog {
    suspend fun queryLaunchableApps(): List<LaunchableAppRecord>
}

interface ClockProvider {
    fun nowEpochMillis(): TimestampEpochMillis
    fun startOfTodayEpochMillis(): TimestampEpochMillis
}

private val RECOMMENDED_BRAINROT_PACKAGES: Set<String> = setOf(
    "com.google.android.youtube",
    "com.instagram.android",
    "com.instagram.lite",
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill",
    "com.reddit.frontpage",
    "com.twitter.android",
    "com.facebook.katana",
    "com.snapchat.android",
    "com.android.chrome",
)

private const val ACCESSIBILITY_SERVICE_SIMPLE_NAME = "AppGuardAccessibilityService"

@Singleton
class AndroidSystemPermissionInspector @Inject constructor(
    @ApplicationContext private val context: Context,
) : SystemPermissionInspector {

    override fun inspectPermissions(): PermissionStatusSnapshot {
        return PermissionStatusSnapshot(
            accessibilityEnabled = isAccessibilityServiceEnabled(),
            overlayEnabled = Settings.canDrawOverlays(context),
            usageStatsEnabled = hasUsageStatsPermission(),
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        )
        if (enabled != 1) return false
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        return enabledServices
            .split(':')
            .asSequence()
            .mapNotNull { ComponentName.unflattenFromString(it.trim()) }
            .any { component ->
                component.packageName == context.packageName &&
                    component.className.endsWith(ACCESSIBILITY_SERVICE_SIMPLE_NAME)
            }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

@Singleton
class AndroidInstalledAppCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) : InstalledAppCatalog {

    override suspend fun queryLaunchableApps(): List<LaunchableAppRecord> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val seenPackages = HashSet<String>()
        resolved
            .asSequence()
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName || !seenPackages.add(pkg)) return@mapNotNull null
                val label = info.loadLabel(pm).toString().ifBlank { pkg }
                LaunchableAppRecord(
                    packageName = AppPackageName(pkg),
                    displayName = label,
                    isRecommendedBrainrotTarget = pkg in RECOMMENDED_BRAINROT_PACKAGES,
                )
            }
            .sortedWith(
                compareByDescending<LaunchableAppRecord> { it.isRecommendedBrainrotTarget }
                    .thenBy { it.displayName.lowercase() },
            )
            .toList()
    }
}

@Singleton
class SystemClockProvider @Inject constructor() : ClockProvider {
    override fun nowEpochMillis(): TimestampEpochMillis {
        return TimestampEpochMillis(System.currentTimeMillis())
    }

    override fun startOfTodayEpochMillis(): TimestampEpochMillis {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TimestampEpochMillis(calendar.timeInMillis)
    }
}
