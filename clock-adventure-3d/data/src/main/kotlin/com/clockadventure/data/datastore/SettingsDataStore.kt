package com.clockadventure.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clockadventure.domain.model.AppLanguage
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.AppTheme
import com.clockadventure.domain.model.ClockStyle
import com.clockadventure.domain.model.DifficultyMode
import com.clockadventure.domain.model.MascotId
import com.clockadventure.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "clock_adventure_settings"
)

/**
 * Application preferences.
 *
 * DataStore is the right tool here: tiny, typed, transactional and - unlike SharedPreferences -
 * safe to read from the main thread as a flow, so a toggle on the Settings screen is reflected
 * everywhere the moment it is changed and survives a restart of the app.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val DIFFICULTY_MODE = stringPreferencesKey("difficulty_mode")
        val MUSIC = booleanPreferencesKey("music_enabled")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VOICE = booleanPreferencesKey("voice_enabled")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val THEME = stringPreferencesKey("theme")
        val CHARACTER = stringPreferencesKey("character")
        val DAILY_LIMIT_MINUTES = intPreferencesKey("daily_limit_minutes")
        val HINTS = booleanPreferencesKey("hints_enabled")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val INTRO_SEEN = booleanPreferencesKey("intro_seen")
    }

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            // A corrupt preferences file must never lock a child out of the game.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> prefs.toSettings() }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.LANGUAGE] = next.language.tag
            prefs[Keys.DIFFICULTY_MODE] = next.difficultyMode.name
            prefs[Keys.MUSIC] = next.musicEnabled
            prefs[Keys.SOUND] = next.soundEnabled
            prefs[Keys.VOICE] = next.voiceEnabled
            prefs[Keys.USE_24_HOUR] = next.use24Hour
            prefs[Keys.NOTIFICATIONS] = next.notificationsEnabled
            prefs[Keys.CLOCK_STYLE] = next.clockStyle.name
            prefs[Keys.THEME] = next.theme.name
            prefs[Keys.CHARACTER] = next.character.name
            prefs[Keys.DAILY_LIMIT_MINUTES] = next.dailyLimitMinutes
            prefs[Keys.HINTS] = next.hintsEnabled
            prefs[Keys.REDUCE_MOTION] = next.reduceMotion
            prefs[Keys.INTRO_SEEN] = next.hasSeenIntro
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        language = AppLanguage.fromTag(this[Keys.LANGUAGE]),
        difficultyMode = runCatching { DifficultyMode.valueOf(this[Keys.DIFFICULTY_MODE].orEmpty()) }
            .getOrDefault(DifficultyMode.AUTO),
        musicEnabled = this[Keys.MUSIC] ?: true,
        soundEnabled = this[Keys.SOUND] ?: true,
        voiceEnabled = this[Keys.VOICE] ?: true,
        use24Hour = this[Keys.USE_24_HOUR] ?: false,
        notificationsEnabled = this[Keys.NOTIFICATIONS] ?: true,
        clockStyle = runCatching { ClockStyle.valueOf(this[Keys.CLOCK_STYLE].orEmpty()) }
            .getOrDefault(ClockStyle.CANDY),
        theme = runCatching { AppTheme.valueOf(this[Keys.THEME].orEmpty()) }
            .getOrDefault(AppTheme.SKY),
        character = runCatching { MascotId.valueOf(this[Keys.CHARACTER].orEmpty()) }
            .getOrDefault(MascotId.TICKY),
        dailyLimitMinutes = this[Keys.DAILY_LIMIT_MINUTES] ?: 30,
        hintsEnabled = this[Keys.HINTS] ?: true,
        reduceMotion = this[Keys.REDUCE_MOTION] ?: false,
        hasSeenIntro = this[Keys.INTRO_SEEN] ?: false
    )
}
