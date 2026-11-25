package com.rotguard.app.feature.inspiration

import com.rotguard.app.storage.ClockProvider
import com.rotguard.app.storage.InspirationDefaults
import com.rotguard.app.storage.InspirationPhotoDao
import com.rotguard.app.storage.InspirationPhotoEntity
import com.rotguard.app.storage.InspirationPhotoFileStore
import com.rotguard.app.storage.InspirationPhotoId
import com.rotguard.app.storage.PersonalReminderDao
import com.rotguard.app.storage.PersonalReminderEntity
import com.rotguard.app.storage.PersonalReminderId
import com.rotguard.app.storage.ReminderCategory
import com.rotguard.app.storage.TimestampEpochMillis
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val STOCK_PHOTO_COUNT = InspirationDefaults.bundledStarterInspirationPhotos(0L).size

class InspirationRepositoryTest {

    @Test
    fun `loadInspirationSnapshot seeds starters, supports multiple categorized reminders, and cleans delete`() =
        runTest {
            val profileDao = FakeUserProfileDao()
            val photoDao = FakeInspirationPhotoDao()
            val reminderDao = FakePersonalReminderDao()
            val fileStore = FakeFileStore()
            val clock = object : ClockProvider {
                override fun nowEpochMillis() = TimestampEpochMillis(2_000L)
                override fun startOfTodayEpochMillis() = TimestampEpochMillis(0L)
            }
            val repo = InspirationRepository(profileDao, photoDao, reminderDao, fileStore, clock)

            val snapshot = repo.loadInspirationSnapshot()
            assertEquals(STOCK_PHOTO_COUNT, snapshot.photos.size)
            assertEquals(3, snapshot.reminders.size)
            assertTrue(profileDao.getProfileOnce()!!.defaultInspirationSeeded)
            assertTrue(profileDao.getProfileOnce()!!.defaultRemindersSeeded)

            val addedPhoto = repo.addPersonalPhoto("content://gallery/9", "Keep building")
            assertEquals("/tmp/copied/1.jpg", addedPhoto.filePath)
            assertEquals(STOCK_PHOTO_COUNT + 1, photoDao.countPhotosOnce())

            val addedReminder = repo.addPersonalReminder("Ship v1 before weekend", ReminderCategory.PERSONAL_GOAL)!!
            assertEquals(ReminderCategory.PERSONAL_GOAL, addedReminder.category)
            assertEquals(4, reminderDao.countRemindersOnce())

            repo.deleteReminder(PersonalReminderId(addedReminder.id.value))
            assertEquals(3, reminderDao.countRemindersOnce())

            repo.deletePhoto(InspirationPhotoId(addedPhoto.id.value))
            assertEquals(listOf("/tmp/copied/1.jpg"), fileStore.deletedPaths)
            assertEquals(STOCK_PHOTO_COUNT, photoDao.countPhotosOnce())

            // Stock photos can't be deleted, only hidden from the pause screen.
            val stock = snapshot.photos.first { it.isStock }
            repo.deletePhoto(stock.id)
            assertEquals(STOCK_PHOTO_COUNT, photoDao.countPhotosOnce())
            repo.setPhotoEnabled(stock.id, enabled = false)
            assertEquals(STOCK_PHOTO_COUNT - 1, photoDao.countEnabledPhotosOnce())
        }

    private class FakeFileStore : InspirationPhotoFileStore {
        val deletedPaths = mutableListOf<String>()
        private var counter = 0

        override suspend fun copyUriToPrivateStorage(uriString: String): String {
            counter += 1
            return "/tmp/copied/$counter.jpg"
        }

        override suspend fun deletePrivatePhotoFile(filePath: String?) {
            if (filePath != null) deletedPaths += filePath
        }
    }

    private class FakeUserProfileDao : UserProfileDao {
        private val flow = MutableStateFlow<UserProfileEntity?>(null)
        override fun observeProfile(): Flow<UserProfileEntity?> = flow
        override suspend fun getProfileOnce(): UserProfileEntity? = flow.value
        override suspend fun upsertProfile(profile: UserProfileEntity) {
            flow.value = profile
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
