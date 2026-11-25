package com.rotguard.app.feature.inspiration

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InspirationModule {

    @Binds
    @Singleton
    abstract fun bindInspirationDataSource(impl: InspirationRepository): InspirationDataSource
}
