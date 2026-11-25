package com.rotguard.app.feature.onboarding

import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.GuardedAppDao
import com.rotguard.app.storage.GuardedAppEntity
import com.rotguard.app.storage.InspirationDefaults
import com.rotguard.app.storage.InspirationPhotoDao
import com.rotguard.app.storage.InspirationPhotoEntity
import com.rotguard.app.storage.InspirationPhotoFileStore
import com.rotguard.app.storage.InstalledAppCatalog
import com.rotguard.app.storage.LaunchableAppRecord
import com.rotguard.app.storage.PermissionStatusSnapshot
import com.rotguard.app.storage.PersonalReminderDao
import com.rotguard.app.storage.PersonalReminderEntity
import com.rotguard.app.storage.SystemPermissionInspector
import com.rotguard.app.storage.TimestampEpochMillis
import com.rotguard.app.storage.UserGoalId
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val STOCK_PHOTO_COUNT = InspirationDefaults.bundledStarterInspirationPhotos(0L).size

class OnboardingRepositoryTest {

    @Test
    fun `loadInitialOnboardingData seeds apps and starter inspiration photos`() = runTest {
        val profileDao = FakeUserProfileDao()
        val appDao = FakeGuardedAppDao()
        val photoDao = FakeInspirationPhotoDao()
        val reminderDao = FakePersonalReminderDao()
        val fileStore = FakePhotoFileStore()
        val clock = FixedClockProvider()
        val repoWithApps = OnboardingRepository(
            userProfileDao = profileDao,
            guardedAppDao = appDao,
            installedAppCatalog = object : InstalledAppCatalog {
                override suspend fun queryLaunchableApps(): List<LaunchableAppRecord> = listOf(
                    LaunchableAppRecord(AppPackageName("com.google.android.youtube"), "YouTube", true),
                    LaunchableAppRecord(AppPackageName("com.android.calculator2"), "Calculator", false),
                )
            },
            permissionInspector = object : SystemPermissionInspector {
                override fun inspectPermissions() = PermissionStatusSnapshot(true, false, false)
            },
            inspirationPhotoDao = photoDao,
            personalReminderDao = reminderDao,
            photoFileStore = fileStore,
            clockProvider = clock,
        )

        val payload = repoWithApps.loadInitialOnboardingData()
        assertEquals(2, payload.appChoices.size)
        assertEquals(STOCK_PHOTO_COUNT, payload.inspirationPhotos.size)
        assertTrue(payload.appChoices.first { it.packageName.value == "com.google.android.youtube" }.isSelected)
        assertTrue(payload.permissions.accessibilityEnabled)

        val addedPhoto = repoWithApps.addOnboardingInspirationPhoto("content://test/1")
        assertEquals("/tmp/1.jpg", addedPhoto.filePath)
        assertEquals(STOCK_PHOTO_COUNT + 1, photoDao.countPhotosOnce())

        val fallbackRepo = OnboardingRepository(
            userProfileDao = profileDao,
            guardedAppDao = appDao,
            installedAppCatalog = object : InstalledAppCatalog {
                override suspend fun queryLaunchableApps(): List<LaunchableAppRecord> = emptyList()
            },
            permissionInspector = object : SystemPermissionInspector {
                override fun inspectPermissions() = PermissionStatusSnapshot(false, false, false)
            },
            inspirationPhotoDao = photoDao,
            personalReminderDao = reminderDao,
            photoFileStore = fileStore,
            clockProvider = clock,
        )
        val fallbackPayload = fallbackRepo.loadInitialOnboardingData()
        assertTrue(fallbackPayload.appChoices.any { it.packageName.value == "com.google.android.youtube" })
    }

    @Test
    fun `saveOnboardingCommitmentToDatabase persists profile and seeds multiple personal reminders`() = runTest {
        val profileDao = FakeUserProfileDao()
        val appDao = FakeGuardedAppDao()
        val reminderDao = FakePersonalReminderDao()
        val repo = OnboardingRepository(
            userProfileDao = profileDao,
            guardedAppDao = appDao,
            installedAppCatalog = object : InstalledAppCatalog {
                override suspend fun queryLaunchableApps(): List<LaunchableAppRecord> = emptyList()
            },
            permissionInspector = object : SystemPermissionInspector {
                override fun inspectPermissions() = PermissionStatusSnapshot(false, false, false)
            },
            inspirationPhotoDao = FakeInspirationPhotoDao(),
            personalReminderDao = reminderDao,
            photoFileStore = FakePhotoFileStore(),
            clockProvider = FixedClockProvider(),
        )

        repo.saveOnboardingCommitmentToDatabase(
            OnboardingCommitmentRequest(
                dailyDoomscrollHours = 4,
                selectedGoalIds = setOf(UserGoalId("deep_work"), UserGoalId("read_daily")),
                peakWindow = "After Work / School",
                personalMantra = "   ",
                appChoices = listOf(
                    OnboardingAppChoice(AppPackageName("com.google.android.youtube"), "YouTube", true, true),
                ),
            ),
        )

        val savedProfile = profileDao.getProfileOnce()!!
        assertTrue(savedProfile.onboardingCompleted)
        assertTrue(savedProfile.defaultRemindersSeeded)
        assertEquals("Build things that outlast the feed.", savedProfile.personalMantra)
        assertEquals(4, savedProfile.dailyDoomscrollHours)
        assertEquals(1, appDao.getGuardedAppsOnce().size)
        assertTrue(reminderDao.countRemindersOnce() >= 2)
    }

    private class FakePhotoFileStore : InspirationPhotoFileStore {
        private var count = 0
        override suspend fun copyUriToPrivateStorage(uriString: String): String {
            count += 1
            return "/tmp/$count.jpg"
        }
        override suspend fun deletePrivatePhotoFile(filePath: String?) = Unit
    }

    private class FakeInspirationPhotoDao : InspirationPhotoDao {
        private val items = mutableListOf<InspirationPhotoEntity>()
        private val flow = MutableStateFlow<List<InspirationPhotoEntity>>(emptyList())
        override fun observeAllPhotos(): Flow<List<InspirationPhotoEntity>> = flow
        override suspend fun getAllPhotosOnce(): List<InspirationPhotoEntity> = items.toList()
        override suspend fun getPhotoByIdOnce(photoId: Long): InspirationPhotoEntity? =
            items.firstOrNull { it.id == photoId }
        override suspend fun insertPhoto(photo: InspirationPhotoEntity): Long {
            val id = (items.maxOfOrNull { it.id } ?: 0L) + 1L
            items += photo.copy(id = id)
            flow.value = items.toList()
            return id
        }
        override suspend fun insertPhotos(photos: List<InspirationPhotoEntity>) {
            photos.forEach { insertPhoto(it) }
        }
        override suspend fun deletePhotoById(photoId: Long) {
            items.removeAll { it.id == photoId }
            flow.value = items.toList()
        }
        override suspend fun countPhotosOnce(): Int = items.size
        override suspend fun countEnabledPhotosOnce(): Int = items.count { it.isEnabled }
        override suspend fun getEnabledPhotosOnce(): List<InspirationPhotoEntity> = items.filter { it.isEnabled }
        override suspend fun setPhotoEnabled(photoId: Long, enabled: Boolean) {
            items.replaceAll { if (it.id == photoId) it.copy(isEnabled = enabled) else it }
            flow.value = items.toList()
        }
    }

    private class FakePersonalReminderDao : PersonalReminderDao {
        private val items = mutableListOf<PersonalReminderEntity>()
        private val flow = MutableStateFlow<List<PersonalReminderEntity>>(emptyList())
        override fun observeAllReminders(): Flow<List<PersonalReminderEntity>> = flow
        override suspend fun getAllRemindersOnce(): List<PersonalReminderEntity> = items.toList()
        override suspend fun insertReminder(reminder: PersonalReminderEntity): Long {
            val id = (items.maxOfOrNull { it.id } ?: 0L) + 1L
            items += reminder.copy(id = id)
            flow.value = items.toList()
            return id
        }
        override suspend fun insertReminders(reminders: List<PersonalReminderEntity>) {
            reminders.forEach { insertReminder(it) }
        }
        override suspend fun deleteReminderById(reminderId: Long) {
            items.removeAll { it.id == reminderId }
            flow.value = items.toList()
        }
        override suspend fun countRemindersOnce(): Int = items.size
    }

    private class FakeUserProfileDao : UserProfileDao {
        private val flow = MutableStateFlow<UserProfileEntity?>(null)
        override fun observeProfile(): Flow<UserProfileEntity?> = flow
        override suspend fun getProfileOnce(): UserProfileEntity? = flow.value
        override suspend fun upsertProfile(profile: UserProfileEntity) {
            flow.value = profile
        }
    }

    private class FakeGuardedAppDao : GuardedAppDao {
        private val apps = linkedMapOf<String, GuardedAppEntity>()
        private val flow = MutableStateFlow<List<GuardedAppEntity>>(emptyList())
        override fun observeAllApps(): Flow<List<GuardedAppEntity>> = flow
        override suspend fun getAllAppsOnce(): List<GuardedAppEntity> = apps.values.toList()
        override suspend fun getGuardedAppsOnce(): List<GuardedAppEntity> = apps.values.filter { it.isGuarded }
        override suspend fun upsertApp(app: GuardedAppEntity) {
            apps[app.packageName] = app
            flow.value = apps.values.toList()
        }
        override suspend fun upsertApps(apps: List<GuardedAppEntity>) {
            apps.forEach { this.apps[it.packageName] = it }
            flow.value = this.apps.values.toList()
        }
        override suspend fun updateGuardState(packageName: String, isGuarded: Boolean) {
            apps[packageName]?.let { apps[packageName] = it.copy(isGuarded = isGuarded) }
        }
        override suspend fun updateCustomHoldDuration(packageName: String, holdDurationMillis: Long?) {
            apps[packageName]?.let { apps[packageName] = it.copy(customHoldDurationMillis = holdDurationMillis) }
        }
    }

    private class FixedClockProvider : ClockProvider {
        override fun nowEpochMillis() = TimestampEpochMillis(1_000L)
        override fun startOfTodayEpochMillis() = TimestampEpochMillis(0L)
    }
}
