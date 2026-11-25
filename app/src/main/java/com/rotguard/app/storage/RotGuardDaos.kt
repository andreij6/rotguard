package com.rotguard.app.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileOnce(): UserProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)
}

@Dao
interface GuardedAppDao {
    @Query("SELECT * FROM guarded_apps ORDER BY isGuarded DESC, displayName ASC")
    fun observeAllApps(): Flow<List<GuardedAppEntity>>

    @Query("SELECT * FROM guarded_apps ORDER BY isGuarded DESC, displayName ASC")
    suspend fun getAllAppsOnce(): List<GuardedAppEntity>

    @Query("SELECT * FROM guarded_apps WHERE isGuarded = 1 ORDER BY displayName ASC")
    suspend fun getGuardedAppsOnce(): List<GuardedAppEntity>

    @Upsert
    suspend fun upsertApp(app: GuardedAppEntity)

    @Upsert
    suspend fun upsertApps(apps: List<GuardedAppEntity>)

    @Query("UPDATE guarded_apps SET isGuarded = :isGuarded WHERE packageName = :packageName")
    suspend fun updateGuardState(packageName: String, isGuarded: Boolean)

    @Query("UPDATE guarded_apps SET customHoldDurationMillis = :holdDurationMillis WHERE packageName = :packageName")
    suspend fun updateCustomHoldDuration(packageName: String, holdDurationMillis: Long?)
}

@Dao
interface InterceptEventDao {
    @Query(
        "SELECT * FROM intercept_events " +
            "WHERE timestampEpochMillis >= :sinceEpochMillis ORDER BY timestampEpochMillis DESC",
    )
    fun observeEventsSince(sinceEpochMillis: Long): Flow<List<InterceptEventEntity>>

    @Query(
        "SELECT * FROM intercept_events " +
            "WHERE timestampEpochMillis >= :sinceEpochMillis ORDER BY timestampEpochMillis DESC",
    )
    suspend fun getEventsSinceOnce(sinceEpochMillis: Long): List<InterceptEventEntity>

    @Query(
        "SELECT COUNT(*) FROM intercept_events " +
            "WHERE packageName = :packageName AND timestampEpochMillis >= :sinceEpochMillis",
    )
    suspend fun countEventsForPackageSince(packageName: String, sinceEpochMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: InterceptEventEntity): Long

    @Query("DELETE FROM intercept_events")
    suspend fun clearAllEvents()
}

@Dao
interface InspirationPhotoDao {
    @Query("SELECT * FROM inspiration_photos ORDER BY sortOrder ASC, id DESC")
    fun observeAllPhotos(): Flow<List<InspirationPhotoEntity>>

    @Query("SELECT * FROM inspiration_photos ORDER BY sortOrder ASC, id DESC")
    suspend fun getAllPhotosOnce(): List<InspirationPhotoEntity>

    @Query("SELECT * FROM inspiration_photos WHERE id = :photoId")
    suspend fun getPhotoByIdOnce(photoId: Long): InspirationPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: InspirationPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<InspirationPhotoEntity>)

    @Query("DELETE FROM inspiration_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)

    @Query("SELECT COUNT(*) FROM inspiration_photos")
    suspend fun countPhotosOnce(): Int

    @Query("SELECT COUNT(*) FROM inspiration_photos WHERE isEnabled = 1")
    suspend fun countEnabledPhotosOnce(): Int

    @Query("SELECT * FROM inspiration_photos WHERE isEnabled = 1 ORDER BY sortOrder ASC, id DESC")
    suspend fun getEnabledPhotosOnce(): List<InspirationPhotoEntity>

    @Query("UPDATE inspiration_photos SET isEnabled = :enabled WHERE id = :photoId")
    suspend fun setPhotoEnabled(photoId: Long, enabled: Boolean)
}

@Dao
interface AppSnoozeDao {
    @Query("SELECT * FROM app_snoozes WHERE untilEpochMillis > :nowEpochMillis ORDER BY untilEpochMillis ASC")
    suspend fun getActiveSnoozesOnce(nowEpochMillis: Long): List<AppSnoozeEntity>

    @Upsert
    suspend fun upsertSnooze(snooze: AppSnoozeEntity)

    @Query("DELETE FROM app_snoozes WHERE packageName = :packageName")
    suspend fun deleteSnooze(packageName: String)

    @Query("DELETE FROM app_snoozes WHERE untilEpochMillis <= :nowEpochMillis")
    suspend fun deleteExpired(nowEpochMillis: Long)
}

@Dao
interface PersonalReminderDao {
    @Query("SELECT * FROM personal_reminders ORDER BY id DESC")
    fun observeAllReminders(): Flow<List<PersonalReminderEntity>>

    @Query("SELECT * FROM personal_reminders ORDER BY id DESC")
    suspend fun getAllRemindersOnce(): List<PersonalReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: PersonalReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<PersonalReminderEntity>)

    @Query("DELETE FROM personal_reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)

    @Query("SELECT COUNT(*) FROM personal_reminders")
    suspend fun countRemindersOnce(): Int
}
