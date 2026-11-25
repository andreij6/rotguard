package com.rotguard.app.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingCompleted: Boolean = false,
    val dailyDoomscrollHours: Int = 3,
    val selectedGoalIdsCsv: String = "deep_work,better_sleep",
    val peakRotWindow: String = StarterTextCatalog.PEAK_LATE_NIGHT,
    val personalMantra: String = StarterTextCatalog.MANTRA_OUTLAST_FEED,
    val globalHoldDurationMillis: Long = 4_000L,
    val sessionGraceDurationMillis: Long = 0L,
    val hapticsEnabled: Boolean = true,
    val shieldEnabled: Boolean = true,
    val rivalDailyTargetCount: Int = 14,
    val themeModeName: String = ThemeMode.SYSTEM.name,
    val showInspirationPhotosOnBlocker: Boolean = true,
    val varyBlockerPrompts: Boolean = true,
    val lastBlockerVariationIndex: Int = -1,
    val lastInspirationPhotoIndex: Int = -1,
    val defaultInspirationSeeded: Boolean = false,
    val defaultRemindersSeeded: Boolean = false,
    val lastReminderIdShown: Long = -1L,
)

@Entity(tableName = "guarded_apps")
data class GuardedAppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val isGuarded: Boolean,
    val customHoldDurationMillis: Long?,
    val dailyLimitCount: Int = 5,
)

@Entity(tableName = "intercept_events")
data class InterceptEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appDisplayName: String,
    val timestampEpochMillis: Long,
    val outcomeName: String,
    val estimatedMinutesSaved: Int,
)

@Entity(tableName = "inspiration_photos")
data class InspirationPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val caption: String,
    val filePath: String? = null,
    val bundledDrawableName: String? = null,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long = 0L,
    /** Stock photos (bundledDrawableName != null) can't be deleted, only hidden from the pause screen. */
    @ColumnInfo(defaultValue = "1") val isEnabled: Boolean = true,
)

/** A temporary "don't pause this app" window the user chose from a repeat pause screen. */
@Entity(tableName = "app_snoozes")
data class AppSnoozeEntity(
    @PrimaryKey val packageName: String,
    val untilEpochMillis: Long,
)

@Entity(tableName = "personal_reminders")
data class PersonalReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val categoryName: String = ReminderCategory.PERSONAL_GOAL.name,
    val createdAtEpochMillis: Long = 0L,
)
