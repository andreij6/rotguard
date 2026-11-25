package com.rotguard.app.feature.dashboard

import com.rotguard.app.storage.AppSnoozeDao
import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.DailyScreenMinutes
import com.rotguard.app.storage.GuardedAppDao
import com.rotguard.app.storage.InspirationDefaults
import com.rotguard.app.storage.InspirationPhotoDao
import com.rotguard.app.storage.InterceptEventDao
import com.rotguard.app.storage.InterceptOutcome
import com.rotguard.app.storage.PersonalReminderDao
import com.rotguard.app.storage.ReminderDefaults
import com.rotguard.app.storage.SystemPermissionInspector
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

interface DashboardDataSource {
    suspend fun fetchDashboardSnapshotFromStorage(): DashboardSnapshot
    suspend fun updateShieldEnabledInDatabase(enabled: Boolean)
    suspend fun endAppBreak(packageName: String)
}

/**
 * Storage and permission repository powering the Home Focus Health dashboard.
 *
 * **Responsibility & Boundaries**
 * - Aggregates today's closed/opened counts, blocked apps, personal reminder, featured
 *   Inspiration photo, and live detection/overlay permissions into [DashboardSnapshot].
 * - Persists master pause-protection enable/pause toggles.
 *
 * **Side Effects**
 * - Queries `user_profile`, `guarded_apps`, `inspiration_photos`, and `intercept_events`,
 *   and inspects OS permissions.
 *
 * **Invariants**
 * - Only counts intercept events occurring since [ClockProvider.startOfTodayEpochMillis].
 *
 * **Anti-Patterns**
 * - Never trigger navigation effects or play haptics from this repository.
 */
@Singleton
class DashboardRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val guardedAppDao: GuardedAppDao,
    private val interceptEventDao: InterceptEventDao,
    private val inspirationPhotoDao: InspirationPhotoDao,
    private val personalReminderDao: PersonalReminderDao,
    private val permissionInspector: SystemPermissionInspector,
    private val clockProvider: ClockProvider,
    private val appSnoozeDao: AppSnoozeDao,
) : DashboardDataSource {

    override suspend fun fetchDashboardSnapshotFromStorage(): DashboardSnapshot {
        val nowMs = clockProvider.nowEpochMillis().value
        InspirationDefaults.ensureStarterInspirationPhotosSeeded(
            userProfileDao = userProfileDao,
            inspirationPhotoDao = inspirationPhotoDao,
            nowEpochMillis = nowMs,
        )
        val profile = ReminderDefaults.ensureStarterRemindersSeeded(
            userProfileDao = userProfileDao,
            personalReminderDao = personalReminderDao,
            nowEpochMillis = nowMs,
        )
        val guardedApps = guardedAppDao.getGuardedAppsOnce()
        val todayEvents = interceptEventDao.getEventsSinceOnce(clockProvider.startOfTodayEpochMillis().value)
        val perms = permissionInspector.inspectPermissions()
        val photos = inspirationPhotoDao.getEnabledPhotosOnce()
        val reminders = personalReminderDao.getAllRemindersOnce()
        val appNames = guardedAppDao.getAllAppsOnce().associate { it.packageName to it.displayName }
        val breaks = appSnoozeDao.getActiveSnoozesOnce(nowMs).map {
            AppBreak(it.packageName, appNames[it.packageName] ?: it.packageName, it.untilEpochMillis)
        }

        val resisted = todayEvents.count { it.outcomeName == InterceptOutcome.RESISTED.name }
        val bypassed = todayEvents.count { it.outcomeName == InterceptOutcome.BYPASSED.name }
        val reclaimedMinutes = todayEvents.sumOf { it.estimatedMinutesSaved }
        val featured = photos.firstOrNull()?.let { entity ->
            DashboardInspirationPreview(
                title = entity.title,
                caption = entity.caption,
                filePath = entity.filePath,
                bundledDrawableName = entity.bundledDrawableName,
            )
        }

        return DashboardSnapshot(
            shieldEnabled = profile.shieldEnabled,
            accessibilityGranted = perms.accessibilityEnabled,
            overlayGranted = perms.overlayEnabled,
            urgesResistedToday = resisted,
            urgesBypassedToday = bypassed,
            minutesReclaimedToday = DailyScreenMinutes(reclaimedMinutes),
            guardedAppNames = guardedApps.map { it.displayName },
            personalMantra = reminders.firstOrNull()?.text ?: profile.personalMantra,
            featuredInspirationPhoto = featured,
            inspirationPhotoCount = photos.size,
            reminderCount = reminders.size,
            appBreaks = breaks,
        )
    }

    override suspend fun endAppBreak(packageName: String) {
        appSnoozeDao.deleteSnooze(packageName)
    }

    override suspend fun updateShieldEnabledInDatabase(enabled: Boolean) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(id = 1, shieldEnabled = enabled))
    }
}
