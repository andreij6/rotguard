package com.rotguard.app.feature.apps

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GuardedAppsModule {
    @Binds
    @Singleton
    abstract fun bindGuardedAppsDataSource(impl: GuardedAppsRepository): GuardedAppsDataSource
}
