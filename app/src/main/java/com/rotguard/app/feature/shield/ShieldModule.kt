package com.rotguard.app.feature.shield

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ShieldTickerDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class ShieldModule {
    @Binds
    @Singleton
    abstract fun bindShieldDataSource(impl: ShieldRepository): ShieldDataSource

    companion object {
        @Provides
        @ShieldTickerDispatcher
        fun provideShieldTickerDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
    }
}
