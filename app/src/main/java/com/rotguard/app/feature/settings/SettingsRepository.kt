package com.rotguard.app.feature.settings

import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.HoldDurationMillis
import com.rotguard.app.storage.InspirationDefaults
import com.rotguard.app.storage.InspirationPhotoDao
import com.rotguard.app.storage.PermissionStatusSnapshot
import com.rotguard.app.storage.PersonalReminderDao
import com.rotguard.app.storage.PersonalReminderEntity
import com.rotguard.app.storage.ReminderCategory
import com.rotguard.app.storage.ReminderDefaults
import com.rotguard.app.storage.StarterTextCatalog
import com.rotguard.app.storage.SystemPermissionInspector
import com.rotguard.app.storage.ThemeMode
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsDataSource {
    suspend fun loadSettingsSnapshotFromDatabase(): SettingsSnapshot
    fun inspectSystemPermissions(): PermissionStatusSnapshot
    suspend fun updateHoldDurationInDatabase(durationMillis: HoldDurationMillis)
    suspend fun updateGraceWindowInDatabase(durationMillis: HoldDurationMillis)
    suspend fun updateHapticsEnabledInDatabase(enabled: Boolean)
    suspend fun updateThemeModeInDatabase(themeMode: ThemeMode)
    suspend fun updatePersonalMantraInDatabase(mantraText: String)
}

/**
 * Persistence and OS-permission diagnostics gateway for Settings.
 *
 * **Responsibility & Boundaries**
 * - Loads and updates pause length, repeat delay, vibration preference, appearance theme,
 *   Inspiration photo count, and personal reminder while inspecting live OS permissions.
 *
 * **Side Effects**
 * - Reads/writes `user_profile`, reads `inspiration_photos`, and queries OS permission states.
 *
 * **Invariants**
 * - Always creates a default `UserProfileEntity` row if local storage does not yet contain `id = 1`.
 *
 * **Anti-Patterns**
 * - Never trigger Android Settings intents or haptics inside repository methods.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val inspirationPhotoDao: InspirationPhotoDao,
    private val personalReminderDao: PersonalReminderDao,
    private val permissionInspector: SystemPermissionInspector,
    private val clockProvider: ClockProvider,
) : SettingsDataSource {

    override suspend fun loadSettingsSnapshotFromDatabase(): SettingsSnapshot {
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
        val perms = permissionInspector.inspectPermissions()
        val photoCount = inspirationPhotoDao.countEnabledPhotosOnce()
        val reminderCount = personalReminderDao.countRemindersOnce()
        return SettingsSnapshot(
            accessibilityGranted = perms.accessibilityEnabled,
            overlayGranted = perms.overlayEnabled,
            usageStatsGranted = perms.usageStatsEnabled,
            globalHoldDurationMillis = HoldDurationMillis(profile.globalHoldDurationMillis),
            sessionGraceDurationMillis = HoldDurationMillis(profile.sessionGraceDurationMillis),
            hapticsEnabled = profile.hapticsEnabled,
            personalMantra = profile.personalMantra,
            themeMode = ThemeMode.fromStorageName(profile.themeModeName),
            inspirationPhotoCount = photoCount,
            reminderCount = reminderCount,
        )
    }

    override fun inspectSystemPermissions(): PermissionStatusSnapshot {
        return permissionInspector.inspectPermissions()
    }

    override suspend fun updateHoldDurationInDatabase(durationMillis: HoldDurationMillis) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(id = 1, globalHoldDurationMillis = durationMillis.value))
    }

    override suspend fun updateGraceWindowInDatabase(durationMillis: HoldDurationMillis) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(id = 1, sessionGraceDurationMillis = durationMillis.value))
    }

    override suspend fun updateHapticsEnabledInDatabase(enabled: Boolean) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(id = 1, hapticsEnabled = enabled))
    }

    override suspend fun updateThemeModeInDatabase(themeMode: ThemeMode) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(id = 1, themeModeName = themeMode.name))
    }

    override suspend fun updatePersonalMantraInDatabase(mantraText: String) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        val normalized = mantraText.trim().ifBlank { StarterTextCatalog.MANTRA_OUTLAST_FEED }
        val existingTexts = personalReminderDao.getAllRemindersOnce().map { it.text }.toSet()
        if (normalized !in existingTexts) {
            personalReminderDao.insertReminder(
                PersonalReminderEntity(
                    text = normalized,
                    categoryName = ReminderCategory.CUSTOM.name,
                    createdAtEpochMillis = clockProvider.nowEpochMillis().value,
                ),
            )
        }
        userProfileDao.upsertProfile(
            current.copy(
                id = 1,
                personalMantra = normalized,
                defaultRemindersSeeded = true,
            ),
        )
    }
}
