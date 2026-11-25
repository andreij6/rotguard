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
import com.rotguard.app.storage.ReminderDefaults
import com.rotguard.app.storage.StarterTextCatalog
import com.rotguard.app.storage.UserProfileDao
import com.rotguard.app.storage.UserProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

interface InspirationDataSource {
    suspend fun loadInspirationSnapshot(): InspirationSnapshot
    suspend fun addPersonalPhoto(uriString: String, caption: String): InspirationPhotoItem
    suspend fun deletePhoto(photoId: InspirationPhotoId)
    suspend fun setPhotoEnabled(photoId: InspirationPhotoId, enabled: Boolean)
    suspend fun addPersonalReminder(text: String, category: ReminderCategory): PersonalReminderItem?
    suspend fun deleteReminder(reminderId: PersonalReminderId)
    suspend fun updateShowPhotosOnBlocker(enabled: Boolean)
    suspend fun updateVaryBlockerPrompts(enabled: Boolean)
}

/**
 * Storage and private image-copy repository backing the Inspiration screen.
 *
 * **Responsibility & Boundaries**
 * - Loads inspiration photos and categorized personal reminders (seeding starter items once),
 *   copies user-picked photos into private storage, and persists added/removed reminders & toggles.
 *
 * **Side Effects**
 * - Reads/writes `inspiration_photos`, `personal_reminders`, and `user_profile` tables,
 *   and manages local files via [InspirationPhotoFileStore].
 *
 * **Invariants**
 * - Starter photos and starter reminders are seeded at most once.
 * - Deleting a user photo removes both its Room row and copied internal file.
 *
 * **Anti-Patterns**
 * - Never trigger haptics, navigation, or photo picker intents inside repository methods.
 */
@Singleton
class InspirationRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val inspirationPhotoDao: InspirationPhotoDao,
    private val personalReminderDao: PersonalReminderDao,
    private val photoFileStore: InspirationPhotoFileStore,
    private val clockProvider: ClockProvider,
) : InspirationDataSource {

    override suspend fun loadInspirationSnapshot(): InspirationSnapshot {
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
        val photos = inspirationPhotoDao.getAllPhotosOnce().map { entity ->
            InspirationPhotoItem(
                id = InspirationPhotoId(entity.id),
                title = entity.title,
                caption = entity.caption,
                filePath = entity.filePath,
                bundledDrawableName = entity.bundledDrawableName,
                isEnabled = entity.isEnabled,
            )
        }
        val reminders = personalReminderDao.getAllRemindersOnce().map { entity ->
            PersonalReminderItem(
                id = PersonalReminderId(entity.id),
                text = entity.text,
                category = ReminderCategory.fromStorageName(entity.categoryName),
            )
        }
        return InspirationSnapshot(
            photos = photos,
            reminders = reminders,
            showPhotosOnBlocker = profile.showInspirationPhotosOnBlocker,
            varyBlockerPrompts = profile.varyBlockerPrompts,
        )
    }

    override suspend fun addPersonalPhoto(uriString: String, caption: String): InspirationPhotoItem {
        val storedPath = photoFileStore.copyUriToPrivateStorage(uriString)
        val normalizedCaption = caption.trim().ifBlank { DEFAULT_PHOTO_CAPTION }
        val entity = InspirationPhotoEntity(
            title = PERSONAL_PHOTO_TITLE,
            caption = normalizedCaption,
            filePath = storedPath,
            bundledDrawableName = null,
            sortOrder = -1,
            createdAtEpochMillis = clockProvider.nowEpochMillis().value,
        )
        val rowId = inspirationPhotoDao.insertPhoto(entity)
        return InspirationPhotoItem(
            id = InspirationPhotoId(rowId),
            title = entity.title,
            caption = entity.caption,
            filePath = storedPath,
            bundledDrawableName = null,
        )
    }

    override suspend fun deletePhoto(photoId: InspirationPhotoId) {
        val existing = inspirationPhotoDao.getPhotoByIdOnce(photoId.value)
        if (existing?.bundledDrawableName != null) return // stock photos are hidden, never deleted
        inspirationPhotoDao.deletePhotoById(photoId.value)
        photoFileStore.deletePrivatePhotoFile(existing?.filePath)
    }

    override suspend fun setPhotoEnabled(photoId: InspirationPhotoId, enabled: Boolean) {
        inspirationPhotoDao.setPhotoEnabled(photoId.value, enabled)
    }

    override suspend fun addPersonalReminder(
        text: String,
        category: ReminderCategory,
    ): PersonalReminderItem? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val entity = PersonalReminderEntity(
            text = trimmed,
            categoryName = category.name,
            createdAtEpochMillis = clockProvider.nowEpochMillis().value,
        )
        val rowId = personalReminderDao.insertReminder(entity)
        val profile = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        userProfileDao.upsertProfile(
            profile.copy(id = 1, personalMantra = trimmed, defaultRemindersSeeded = true),
        )
        return PersonalReminderItem(
            id = PersonalReminderId(rowId),
            text = trimmed,
            category = category,
        )
    }

    override suspend fun deleteReminder(reminderId: PersonalReminderId) {
        personalReminderDao.deleteReminderById(reminderId.value)
    }

    override suspend fun updateShowPhotosOnBlocker(enabled: Boolean) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(id = 1, showInspirationPhotosOnBlocker = enabled))
    }

    override suspend fun updateVaryBlockerPrompts(enabled: Boolean) {
        val current = userProfileDao.getProfileOnce() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(id = 1, varyBlockerPrompts = enabled))
    }

    private companion object {
        const val PERSONAL_PHOTO_TITLE = StarterTextCatalog.PERSONAL_PHOTO_TITLE
        const val DEFAULT_PHOTO_CAPTION = StarterTextCatalog.PERSONAL_PHOTO_CAPTION
    }
}
