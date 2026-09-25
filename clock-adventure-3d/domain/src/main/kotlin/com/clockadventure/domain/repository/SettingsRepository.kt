package com.clockadventure.domain.repository

import com.clockadventure.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** Application preferences, persisted with DataStore and exposed as a flow. */
interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun current(): AppSettings

    suspend fun update(transform: (AppSettings) -> AppSettings)
}
