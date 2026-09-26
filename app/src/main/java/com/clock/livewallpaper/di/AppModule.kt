package com.clock.livewallpaper.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.clock.livewallpaper.data.local.DhikrDao
import com.clock.livewallpaper.data.local.DhikrDatabase
import com.clock.livewallpaper.data.prefs.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Long-lived scope for work that must outlive a screen (seeding, alarm bookkeeping). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SettingsRepository.DATASTORE_NAME
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DhikrDatabase =
        Room.databaseBuilder(context, DhikrDatabase::class.java, DhikrDatabase.NAME)
            // No destructive fallback: user dhikr and switches survive every future version.
            .addMigrations(*DhikrDatabase.MIGRATIONS)
            .build()

    @Provides
    fun provideDhikrDao(database: DhikrDatabase): DhikrDao = database.dhikrDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
