package com.rotguard.app.feature.shield

import com.rotguard.app.storage.AppPackageName
import com.rotguard.app.storage.AppSnoozeDao
import com.rotguard.app.storage.AppSnoozeEntity
import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.GuardedAppDao
import com.rotguard.app.storage.GuardedAppEntity
import com.rotguard.app.storage.InspirationDefaults
import com.rotguard.app.storage.InspirationPhotoDao
import com.rotguard.app.storage.InspirationPhotoEntity
import com.rotguard.app.storage.InterceptEventDao
import com.rotguard.app.storage.InterceptEventEntity
import com.rotguard.app.storage.InterceptOutcome
import com.rotguard.app.storage.PersonalReminderDao
import com.rotguard.app.storage.PersonalReminderEntity
import com.rotguard.app.storage.TimestampEpochMillis
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val STOCK_PHOTO_COUNT = InspirationDefaults.bundledStarterInspirationPhotos(0L).size

class ShieldRepositoryTest {

    @Test
    fun `recordInterceptOutcomeToDatabase saves events and rotates inspiration, prompts, and reminders`() = runTest {
        val profileDao = FakeProfileDao()
        val appDao = FakeGuardedAppDao().apply {
            seed(GuardedAppEntity("com.google.android.youtube", "YouTube", true, 5_000L))
        }
        val eventDao = FakeEventDao()
        val photoDao = FakeInspirationPhotoDao()
        val reminderDao = FakePersonalReminderDao()
        val clock = object : ClockProvider {
            override fun nowEpochMillis() = TimestampEpochMillis(5_000L)
            override fun startOfTodayEpochMillis() = TimestampEpochMillis(1_000L)
        }
        val repo = ShieldRepository(profileDao, appDao, eventDao, photoDao, reminderDao, clock, FakeAppSnoozeDao())

        repo.recordInterceptOutcomeToDatabase(
            packageName = AppPackageName("com.google.android.youtube"),
            appDisplayName = "YouTube",
            outcome = InterceptOutcome.RESISTED,
        )
        repo.recordInterceptOutcomeToDatabase(
            packageName = AppPackageName("com.google.android.youtube"),
            appDisplayName = "YouTube",
            outcome = InterceptOutcome.BYPASSED,
        )

        val context = repo.loadShieldEvaluationContext()
        assertEquals(2, context.todayAttemptsByPackage["com.google.android.youtube"])
        assertEquals(1, context.todayResistedCount)
        assertEquals(76, context.brainHealth.value)
        assertNotNull(context.inspirationVisual)
        assertNotNull(context.reminderCategoryBadgeRes)
        assertEquals(STOCK_PHOTO_COUNT, photoDao.countPhotosOnce())
        assertEquals(3, reminderDao.countRemindersOnce())

        val firstDemo = repo.loadDemoBlockingDecision(AppPackageName("com.google.android.youtube"), "YouTube")
        assertEquals(5_000L, firstDemo.holdDurationMillis.value)
        assertEquals(3, firstDemo.todayAttemptsCount)
        val secondDemo = repo.loadDemoBlockingDecision(AppPackageName("com.google.android.youtube"), "YouTube")
        assertNotEquals(firstDemo.variationIndex, secondDemo.variationIndex)
        assertNotEquals(firstDemo.inspirationIndex, secondDemo.inspirationIndex)
        assertNotEquals(firstDemo.selectedReminderId, secondDemo.selectedReminderId)

        profileDao.upsertProfile(profileDao.getProfileOnce()!!.copy(showInspirationPhotosOnBlocker = false))
        val noPhotoContext = repo.loadShieldEvaluationContext()
        assertNull(noPhotoContext.inspirationVisual)
        assertTrue(eventDao.storedEvents.first().estimatedMinutesSaved == 5)
    }

    private class FakeAppSnoozeDao : AppSnoozeDao {
        private val items = mutableMapOf<String, AppSnoozeEntity>()
        override suspend fun getActiveSnoozesOnce(nowEpochMillis: Long) =
            items.values.filter { it.untilEpochMillis > nowEpochMillis }
        override suspend fun upsertSnooze(snooze: AppSnoozeEntity) {
            items[snooze.packageName] = snooze
        }
        override suspend fun deleteSnooze(packageName: String) {
            items.remove(packageName)
        }
        override suspend fun deleteExpired(nowEpochMillis: Long) {
            items.values.removeAll { it.untilEpochMillis <= nowEpochMillis }
        }
    }

    private class FakeProfileDao : UserProfileDao {
        private val flow = MutableStateFlow<UserProfileEntity?>(UserProfileEntity(onboardingCompleted = true))
        override fun observeProfile(): Flow<UserProfileEntity?> = flow
        override suspend fun getProfileOnce(): UserProfileEntity? = flow.value
        override suspend fun upsertProfile(profile: UserProfileEntity) {
            flow.value = profile
        }
    }

    private class FakeGuardedAppDao : GuardedAppDao {
        private val map = linkedMapOf<String, GuardedAppEntity>()
        private val flow = MutableStateFlow<List<GuardedAppEntity>>(emptyList())
        fun seed(entity: GuardedAppEntity) {
            map[entity.packageName] = entity
            flow.value = map.values.toList()
        }
        override fun observeAllApps(): Flow<List<GuardedAppEntity>> = flow
        override suspend fun getAllAppsOnce(): List<GuardedAppEntity> = map.values.toList()
        override suspend fun getGuardedAppsOnce(): List<GuardedAppEntity> = map.values.filter { it.isGuarded }
        override suspend fun upsertApp(app: GuardedAppEntity) = seed(app)
        override suspend fun upsertApps(apps: List<GuardedAppEntity>) = apps.forEach(::seed)
        override suspend fun updateGuardState(packageName: String, isGuarded: Boolean) = Unit
        override suspend fun updateCustomHoldDuration(packageName: String, holdDurationMillis: Long?) = Unit
    }

    private class FakeEventDao : InterceptEventDao {
        val storedEvents = mutableListOf<InterceptEventEntity>()
        private val flow = MutableStateFlow<List<InterceptEventEntity>>(emptyList())
        override fun observeEventsSince(sinceEpochMillis: Long): Flow<List<InterceptEventEntity>> = flow
        override suspend fun getEventsSinceOnce(sinceEpochMillis: Long): List<InterceptEventEntity> =
            storedEvents.filter { it.timestampEpochMillis >= sinceEpochMillis }
        override suspend fun countEventsForPackageSince(packageName: String, sinceEpochMillis: Long): Int =
            storedEvents.count { it.packageName == packageName && it.timestampEpochMillis >= sinceEpochMillis }
        override suspend fun insertEvent(event: InterceptEventEntity): Long {
            storedEvents += event
            flow.value = storedEvents.toList()
            return storedEvents.size.toLong()
        }
        override suspend fun clearAllEvents() {
            storedEvents.clear()
        }
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
}
