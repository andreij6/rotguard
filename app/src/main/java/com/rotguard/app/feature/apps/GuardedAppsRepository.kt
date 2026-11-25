package com.rotguard.app.feature.apps

import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.GuardedAppDao
import com.rotguard.app.storage.GuardedAppEntity
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.InstalledAppCatalog
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

data class GuardedAppsCatalogSnapshot(
    val globalHoldDurationMillis: HoldDurationMillis,
    val apps: List<GuardedAppRow>,
)

interface GuardedAppsDataSource {
    suspend fun fetchInstalledAndGuardedApps(): GuardedAppsCatalogSnapshot
    suspend fun persistAppGuardToggle(packageName: AppPackageName, isGuarded: Boolean)
    suspend fun persistGlobalHoldDuration(durationMillis: HoldDurationMillis)
}

/**
 * Syncs installed launchable Android apps with Room friction rules.
 *
 * **Responsibility & Boundaries**
 * - Discovers installed launcher apps via [InstalledAppCatalog], merges saved Room `guarded_apps`
 *   state, and persists user guard toggles and hold duration presets.
 *
 * **Side Effects**
 * - Queries `PackageManager` and reads/writes `guarded_apps` and `user_profile` tables in Room.
 *
 * **Invariants**
 * - Recommended or already-guarded apps appear before general installed apps.
 * - Any newly discovered launchable app is synced to Room when toggled so [AppGuardAccessibilityService]
 *   can intercept it immediately.
 *
 * **Anti-Patterns**
 * - Never trigger haptics or modify `uiState` directly inside repository methods.
 */
@Singleton
class GuardedAppsRepository @Inject constructor(
    private val guardedAppDao: GuardedAppDao,
    private val userProfileDao: UserProfileDao,
    private val installedAppCatalog: InstalledAppCatalog,
) : GuardedAppsDataSource {

    override suspend fun fetchInstalledAndGuardedApps(): GuardedAppsCatalogSnapshot {
        val profile = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        val storedByPkg = guardedAppDao.getAllAppsOnce().associateBy { it.packageName }
        val installed = installedAppCatalog.queryLaunchableApps()
        val globalHoldSec = (profile.globalHoldDurationMillis / MILLIS_PER_SECOND).toInt().coerceAtLeast(1)

        val rows = installed.map { record ->
            val stored = storedByPkg[record.packageName.value]
            val holdMs = stored?.customHoldDurationMillis ?: profile.globalHoldDurationMillis
            GuardedAppRow(
                packageName = record.packageName,
                displayName = record.displayName,
                isGuarded = stored?.isGuarded ?: false,
                isRecommended = record.isRecommendedBrainrotTarget,
                effectiveHoldSeconds = (holdMs / MILLIS_PER_SECOND).toInt().coerceAtLeast(1),
            )
        }.ifEmpty {
            storedByPkg.values.map { stored ->
                val rawHoldMs = stored.customHoldDurationMillis ?: profile.globalHoldDurationMillis
                GuardedAppRow(
                    packageName = AppPackageName(stored.packageName),
                    displayName = stored.displayName,
                    isGuarded = stored.isGuarded,
                    isRecommended = true,
                    effectiveHoldSeconds = (rawHoldMs / MILLIS_PER_SECOND).toInt().coerceAtLeast(1),
                )
            }
        }

        val sorted = rows.sortedWith(
            compareByDescending<GuardedAppRow> { it.isGuarded }
                .thenByDescending { it.isRecommended }
                .thenBy { it.displayName.lowercase() },
        )

        val missingEntities = installed
            .filter { it.packageName.value !in storedByPkg }
            .map { record ->
                GuardedAppEntity(
                    packageName = record.packageName.value,
                    displayName = record.displayName,
                    isGuarded = record.isRecommendedBrainrotTarget && storedByPkg.isEmpty(),
                    customHoldDurationMillis = null,
                )
            }
        if (missingEntities.isNotEmpty()) {
            guardedAppDao.upsertApps(missingEntities)
        }

        return GuardedAppsCatalogSnapshot(
            globalHoldDurationMillis = HoldDurationMillis(globalHoldSec * MILLIS_PER_SECOND),
            apps = sorted,
        )
    }

    override suspend fun persistAppGuardToggle(packageName: AppPackageName, isGuarded: Boolean) {
        guardedAppDao.updateGuardState(packageName.value, isGuarded)
    }

    override suspend fun persistGlobalHoldDuration(durationMillis: HoldDurationMillis) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(id = 1, globalHoldDurationMillis = durationMillis.value))
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
