package com.rotguard.app.feature.insights

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InsightsModule {
    @Binds
    @Singleton
    abstract fun bindInsightsDataSource(impl: InsightsRepository): InsightsDataSource
}
