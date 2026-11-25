package com.rotguard.app.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        GuardedAppEntity::class,
        InterceptEventEntity::class,
        InspirationPhotoEntity::class,
        PersonalReminderEntity::class,
        AppSnoozeEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class RotGuardDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun guardedAppDao(): GuardedAppDao
    abstract fun interceptEventDao(): InterceptEventDao
    abstract fun inspirationPhotoDao(): InspirationPhotoDao
    abstract fun personalReminderDao(): PersonalReminderDao
    abstract fun appSnoozeDao(): AppSnoozeDao
}

/** v4: stock photos can be hidden (not deleted); per-app "don't pause for a while" windows. Keeps all user data. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE inspiration_photos ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS app_snoozes " +
                "(packageName TEXT NOT NULL, untilEpochMillis INTEGER NOT NULL, PRIMARY KEY(packageName))",
        )
    }
}
