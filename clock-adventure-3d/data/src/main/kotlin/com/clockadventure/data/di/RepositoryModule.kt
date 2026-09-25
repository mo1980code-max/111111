package com.clockadventure.data.di

import com.clockadventure.data.datastore.SettingsDataStore
import com.clockadventure.data.repository.ProgressRepositoryImpl
import com.clockadventure.domain.repository.ProgressRepository
import com.clockadventure.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the domain repository interfaces to their implementations, so the UI layer only ever
 * sees the interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository
}
