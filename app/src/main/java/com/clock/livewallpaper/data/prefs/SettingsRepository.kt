package com.clock.livewallpaper.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.clock.livewallpaper.core.ArabicText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every persisted preference of the app, in one file.
 *
 * One key name, one type, one default - declared in [Keys] and read through the private read*
 * helpers so the UI, the alarm receivers and the widgets can never disagree about a default.
 * All of it is local: DataStore writes to the app's own files directory, nothing leaves the device.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SEED_VERSION = intPreferencesKey("seed_version")
        val NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val READER_FONT_SCALE = floatPreferencesKey("reader_font_scale")
        val SHOW_HIJRI = booleanPreferencesKey("show_hijri_date")

        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val OVERLAY_STYLE = stringPreferencesKey("overlay_style")
        val OVERLAY_POSITION = stringPreferencesKey("overlay_position")
        val OVERLAY_FONT_SCALE = floatPreferencesKey("overlay_font_scale")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val OVERLAY_AUTO_DISMISS = intPreferencesKey("overlay_auto_dismiss_seconds")
        val OVERLAY_HAPTIC = booleanPreferencesKey("overlay_haptic")

        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_INTERVAL = intPreferencesKey("reminder_interval_minutes")

        val QUIET_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START = intPreferencesKey("quiet_hours_start_minute")
        val QUIET_END = intPreferencesKey("quiet_hours_end_minute")

        val MORNING_ENABLED = booleanPreferencesKey("morning_reminder_enabled")
        val MORNING_MINUTE = intPreferencesKey("morning_reminder_minute")
        val EVENING_ENABLED = booleanPreferencesKey("evening_reminder_enabled")
        val EVENING_MINUTE = intPreferencesKey("evening_reminder_minute")
        val FRIDAY_ENABLED = booleanPreferencesKey("friday_reminder_enabled")
        val FRIDAY_MINUTE = intPreferencesKey("friday_reminder_minute")

        val TASBEEH_PRESET_ID = longPreferencesKey("tasbeeh_preset_dhikr_id")
        val TASBEEH_CUSTOM_TEXT = stringPreferencesKey("tasbeeh_custom_text")
        val TASBEEH_COUNT = intPreferencesKey("tasbeeh_count")
        val TASBEEH_TARGET = intPreferencesKey("tasbeeh_target")
        val TASBEEH_CUSTOM_TARGET = intPreferencesKey("tasbeeh_custom_target")
        val TASBEEH_DAILY_TOTAL = intPreferencesKey("tasbeeh_daily_total")
        val TASBEEH_DAILY_DATE = stringPreferencesKey("tasbeeh_daily_date")
        val TASBEEH_HAPTIC = booleanPreferencesKey("tasbeeh_haptic")

        val RECENT_DHIKR_IDS = stringPreferencesKey("recent_dhikr_ids")
    }

    private val preferences: Flow<Preferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    // ---------------------------------------------------------------- reads

    val onboardingCompleted: Flow<Boolean> =
        preferences.map { it[Keys.ONBOARDING_COMPLETED] ?: false }.distinctUntilChanged()

    val notificationPromptShown: Flow<Boolean> =
        preferences.map { it[Keys.NOTIFICATION_PROMPT_SHOWN] ?: false }.distinctUntilChanged()

    val appearance: Flow<AppearanceSettings> =
        preferences.map { readAppearance(it) }.distinctUntilChanged()

    val overlay: Flow<OverlaySettings> =
        preferences.map { readOverlay(it) }.distinctUntilChanged()

    val reminder: Flow<ReminderSettings> =
        preferences.map { readReminder(it) }.distinctUntilChanged()

    val quietHours: Flow<QuietHoursSettings> =
        preferences.map { readQuietHours(it) }.distinctUntilChanged()

    val morningReminder: Flow<DailyReminderSettings> =
        preferences.map { readDaily(it, Keys.MORNING_ENABLED, Keys.MORNING_MINUTE, DEFAULT_MORNING_MINUTE) }
            .distinctUntilChanged()

    val eveningReminder: Flow<DailyReminderSettings> =
        preferences.map { readDaily(it, Keys.EVENING_ENABLED, Keys.EVENING_MINUTE, DEFAULT_EVENING_MINUTE) }
            .distinctUntilChanged()

    val fridayReminder: Flow<DailyReminderSettings> =
        preferences.map { readDaily(it, Keys.FRIDAY_ENABLED, Keys.FRIDAY_MINUTE, DEFAULT_FRIDAY_MINUTE) }
            .distinctUntilChanged()

    val tasbeeh: Flow<TasbeehSettings> =
        preferences.map { readTasbeeh(it) }.distinctUntilChanged()

    suspend fun snapshot(): SettingsSnapshot {
        val current = preferences.first()
        return SettingsSnapshot(
            reminder = readReminder(current),
            quietHours = readQuietHours(current),
            overlay = readOverlay(current),
            morning = readDaily(current, Keys.MORNING_ENABLED, Keys.MORNING_MINUTE, DEFAULT_MORNING_MINUTE),
            evening = readDaily(current, Keys.EVENING_ENABLED, Keys.EVENING_MINUTE, DEFAULT_EVENING_MINUTE),
            friday = readDaily(current, Keys.FRIDAY_ENABLED, Keys.FRIDAY_MINUTE, DEFAULT_FRIDAY_MINUTE)
        )
    }

    suspend fun seedVersion(): Int = preferences.first()[Keys.SEED_VERSION] ?: 0

    suspend fun recentDhikrIds(): List<Long> = parseRecent(preferences.first()[Keys.RECENT_DHIKR_IDS])

    // --------------------------------------------------------------- writes

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setNotificationPromptShown(shown: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATION_PROMPT_SHOWN] = shown }
    }

    suspend fun setSeedVersion(version: Int) {
        dataStore.edit { it[Keys.SEED_VERSION] = version }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setReaderFontScale(scale: Float) {
        dataStore.edit {
            it[Keys.READER_FONT_SCALE] =
                scale.coerceIn(AppearanceSettings.MIN_READER_SCALE, AppearanceSettings.MAX_READER_SCALE)
        }
    }

    suspend fun setShowHijri(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_HIJRI] = show }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.OVERLAY_ENABLED] = enabled }
    }

    suspend fun setOverlayStyle(style: OverlayStyleOption) {
        dataStore.edit { it[Keys.OVERLAY_STYLE] = style.key }
    }

    suspend fun setOverlayPosition(position: OverlayPositionOption) {
        dataStore.edit { it[Keys.OVERLAY_POSITION] = position.key }
    }

    suspend fun setOverlayFontScale(scale: Float) {
        dataStore.edit {
            it[Keys.OVERLAY_FONT_SCALE] =
                scale.coerceIn(OverlaySettings.MIN_FONT_SCALE, OverlaySettings.MAX_FONT_SCALE)
        }
    }

    suspend fun setOverlayOpacity(opacity: Float) {
        dataStore.edit {
            it[Keys.OVERLAY_OPACITY] =
                opacity.coerceIn(OverlaySettings.MIN_OPACITY, OverlaySettings.MAX_OPACITY)
        }
    }

    suspend fun setOverlayAutoDismiss(seconds: Int) {
        val allowed = if (OverlaySettings.AUTO_DISMISS_OPTIONS.contains(seconds)) seconds else 0
        dataStore.edit { it[Keys.OVERLAY_AUTO_DISMISS] = allowed }
    }

    suspend fun setOverlayHaptic(enabled: Boolean) {
        dataStore.edit { it[Keys.OVERLAY_HAPTIC] = enabled }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderInterval(minutes: Int) {
        dataStore.edit { it[Keys.REMINDER_INTERVAL] = ReminderSettings.sanitizeInterval(minutes) }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.QUIET_ENABLED] = enabled }
    }

    suspend fun setQuietHours(startMinute: Int, endMinute: Int) {
        dataStore.edit {
            it[Keys.QUIET_START] = sanitizeMinute(startMinute)
            it[Keys.QUIET_END] = sanitizeMinute(endMinute)
        }
    }

    suspend fun setMorningReminder(enabled: Boolean, minuteOfDay: Int) {
        dataStore.edit {
            it[Keys.MORNING_ENABLED] = enabled
            it[Keys.MORNING_MINUTE] = sanitizeMinute(minuteOfDay)
        }
    }

    suspend fun setEveningReminder(enabled: Boolean, minuteOfDay: Int) {
        dataStore.edit {
            it[Keys.EVENING_ENABLED] = enabled
            it[Keys.EVENING_MINUTE] = sanitizeMinute(minuteOfDay)
        }
    }

    suspend fun setFridayReminder(enabled: Boolean, minuteOfDay: Int) {
        dataStore.edit {
            it[Keys.FRIDAY_ENABLED] = enabled
            it[Keys.FRIDAY_MINUTE] = sanitizeMinute(minuteOfDay)
        }
    }

    suspend fun setTasbeehSelection(presetDhikrId: Long, customText: String) {
        dataStore.edit {
            it[Keys.TASBEEH_PRESET_ID] = presetDhikrId
            it[Keys.TASBEEH_CUSTOM_TEXT] = customText
            it[Keys.TASBEEH_COUNT] = 0
        }
    }

    suspend fun setTasbeehTarget(target: Int) {
        dataStore.edit {
            val sane = TasbeehSettings.sanitizeTarget(target)
            it[Keys.TASBEEH_TARGET] = sane
            if (!TasbeehSettings.TARGETS.contains(sane)) it[Keys.TASBEEH_CUSTOM_TARGET] = sane
            val count = it[Keys.TASBEEH_COUNT] ?: 0
            if (count > sane) it[Keys.TASBEEH_COUNT] = 0
        }
    }

    suspend fun setTasbeehHaptic(enabled: Boolean) {
        dataStore.edit { it[Keys.TASBEEH_HAPTIC] = enabled }
    }

    /**
     * One tasbeeh tap: the round counter wraps at the target, the daily total keeps growing and
     * rolls over on its own when the local date changes.
     */
    suspend fun incrementTasbeeh(): TasbeehSettings {
        val updated = dataStore.edit { prefs ->
            val today = ArabicText.dayKey()
            val target = TasbeehSettings.sanitizeTarget(prefs[Keys.TASBEEH_TARGET] ?: 33)
            val current = (prefs[Keys.TASBEEH_COUNT] ?: 0).coerceAtLeast(0)
            val next = if (current >= target) 1 else current + 1
            val sameDay = prefs[Keys.TASBEEH_DAILY_DATE] == today
            val dailyBase = if (sameDay) (prefs[Keys.TASBEEH_DAILY_TOTAL] ?: 0) else 0
            prefs[Keys.TASBEEH_COUNT] = next
            prefs[Keys.TASBEEH_DAILY_DATE] = today
            prefs[Keys.TASBEEH_DAILY_TOTAL] = dailyBase + 1
        }
        return readTasbeeh(updated)
    }

    /** Only the round counter: the daily total is deliberately left untouched. */
    suspend fun resetTasbeehCounter() {
        dataStore.edit { it[Keys.TASBEEH_COUNT] = 0 }
    }

    suspend fun pushRecentDhikrId(id: Long, limit: Int) {
        if (limit <= 0) {
            dataStore.edit { it[Keys.RECENT_DHIKR_IDS] = "" }
            return
        }
        dataStore.edit { prefs ->
            val current = parseRecent(prefs[Keys.RECENT_DHIKR_IDS]).toMutableList()
            current.remove(id)
            current.add(0, id)
            while (current.size > limit) current.removeAt(current.size - 1)
            prefs[Keys.RECENT_DHIKR_IDS] = current.joinToString(",")
        }
    }

    // --------------------------------------------------------------- mapping

    private fun readAppearance(prefs: Preferences) = AppearanceSettings(
        themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
        readerFontScale = (prefs[Keys.READER_FONT_SCALE] ?: 1f)
            .coerceIn(AppearanceSettings.MIN_READER_SCALE, AppearanceSettings.MAX_READER_SCALE),
        showHijri = prefs[Keys.SHOW_HIJRI] ?: true
    )

    private fun readOverlay(prefs: Preferences) = OverlaySettings(
        enabled = prefs[Keys.OVERLAY_ENABLED] ?: false,
        style = OverlayStyleOption.fromKey(prefs[Keys.OVERLAY_STYLE]),
        position = OverlayPositionOption.fromKey(prefs[Keys.OVERLAY_POSITION]),
        fontScale = (prefs[Keys.OVERLAY_FONT_SCALE] ?: 1f)
            .coerceIn(OverlaySettings.MIN_FONT_SCALE, OverlaySettings.MAX_FONT_SCALE),
        opacity = (prefs[Keys.OVERLAY_OPACITY] ?: 1f)
            .coerceIn(OverlaySettings.MIN_OPACITY, OverlaySettings.MAX_OPACITY),
        autoDismissSeconds = (prefs[Keys.OVERLAY_AUTO_DISMISS] ?: 10)
            .let { if (OverlaySettings.AUTO_DISMISS_OPTIONS.contains(it)) it else 0 },
        haptic = prefs[Keys.OVERLAY_HAPTIC] ?: true
    )

    private fun readReminder(prefs: Preferences) = ReminderSettings(
        enabled = prefs[Keys.REMINDER_ENABLED] ?: false,
        intervalMinutes = ReminderSettings.sanitizeInterval(prefs[Keys.REMINDER_INTERVAL] ?: 30)
    )

    private fun readQuietHours(prefs: Preferences) = QuietHoursSettings(
        enabled = prefs[Keys.QUIET_ENABLED] ?: false,
        startMinute = sanitizeMinute(prefs[Keys.QUIET_START] ?: (23 * 60)),
        endMinute = sanitizeMinute(prefs[Keys.QUIET_END] ?: (6 * 60))
    )

    private fun readDaily(
        prefs: Preferences,
        enabledKey: Preferences.Key<Boolean>,
        minuteKey: Preferences.Key<Int>,
        defaultMinute: Int
    ) = DailyReminderSettings(
        enabled = prefs[enabledKey] ?: false,
        minuteOfDay = sanitizeMinute(prefs[minuteKey] ?: defaultMinute)
    )

    private fun readTasbeeh(prefs: Preferences): TasbeehSettings {
        val today = ArabicText.dayKey()
        val sameDay = prefs[Keys.TASBEEH_DAILY_DATE] == today
        val target = TasbeehSettings.sanitizeTarget(prefs[Keys.TASBEEH_TARGET] ?: 33)
        return TasbeehSettings(
            presetDhikrId = prefs[Keys.TASBEEH_PRESET_ID] ?: 0L,
            customText = prefs[Keys.TASBEEH_CUSTOM_TEXT].orEmpty(),
            count = (prefs[Keys.TASBEEH_COUNT] ?: 0).coerceIn(0, target),
            target = target,
            customTarget = TasbeehSettings.sanitizeTarget(prefs[Keys.TASBEEH_CUSTOM_TARGET] ?: 100),
            dailyTotal = if (sameDay) (prefs[Keys.TASBEEH_DAILY_TOTAL] ?: 0).coerceAtLeast(0) else 0,
            haptic = prefs[Keys.TASBEEH_HAPTIC] ?: true
        )
    }

    private fun parseRecent(raw: String?): List<Long> =
        raw?.split(',')?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()

    private fun sanitizeMinute(minute: Int): Int =
        ((minute % ArabicText.MINUTES_PER_DAY) + ArabicText.MINUTES_PER_DAY) % ArabicText.MINUTES_PER_DAY

    companion object {
        const val DATASTORE_NAME = "dhikr_settings"

        const val DEFAULT_MORNING_MINUTE = 6 * 60          // 06:00
        const val DEFAULT_EVENING_MINUTE = 17 * 60         // 05:00 م
        const val DEFAULT_FRIDAY_MINUTE = 9 * 60           // 09:00
    }
}
