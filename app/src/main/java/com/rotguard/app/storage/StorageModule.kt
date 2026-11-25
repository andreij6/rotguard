package com.rotguard.app.storage

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindHapticConductor(impl: AndroidHapticConductor): HapticConductor

    @Binds
    @Singleton
    abstract fun bindPermissionInspector(impl: AndroidSystemPermissionInspector): SystemPermissionInspector

    @Binds
    @Singleton
    abstract fun bindInstalledAppCatalog(impl: AndroidInstalledAppCatalog): InstalledAppCatalog

    @Binds
    @Singleton
    abstract fun bindClockProvider(impl: SystemClockProvider): ClockProvider

    @Binds
    @Singleton
    abstract fun bindInspirationPhotoFileStore(impl: AndroidInspirationPhotoFileStore): InspirationPhotoFileStore

    companion object {
        private const val DATABASE_NAME = "rotguard_brain_health.db"

        @Provides
        @Singleton
        fun provideRotGuardDatabase(
            @ApplicationContext context: Context,
        ): RotGuardDatabase {
            return Room.databaseBuilder(
                context,
                RotGuardDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_3_4).fallbackToDestructiveMigration().build()
        }

        @Provides
        fun provideAppSnoozeDao(database: RotGuardDatabase): AppSnoozeDao = database.appSnoozeDao()

        @Provides
        fun provideUserProfileDao(database: RotGuardDatabase): UserProfileDao {
            return database.userProfileDao()
        }

        @Provides
        fun provideGuardedAppDao(database: RotGuardDatabase): GuardedAppDao {
            return database.guardedAppDao()
        }

        @Provides
        fun provideInterceptEventDao(database: RotGuardDatabase): InterceptEventDao {
            return database.interceptEventDao()
        }

        @Provides
        fun provideInspirationPhotoDao(database: RotGuardDatabase): InspirationPhotoDao {
            return database.inspirationPhotoDao()
        }

        @Provides
        fun providePersonalReminderDao(database: RotGuardDatabase): PersonalReminderDao {
            return database.personalReminderDao()
        }
    }
}
