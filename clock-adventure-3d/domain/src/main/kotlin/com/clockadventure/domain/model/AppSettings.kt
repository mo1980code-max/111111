package com.clockadventure.domain.model

/**
 * Every preference the app has. Persisted with DataStore (Preferences) and exposed as a cold
 * Flow, so a change made on the Settings screen is visible everywhere immediately and still
 * there after the app is restarted.
 */
data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val difficultyMode: DifficultyMode = DifficultyMode.AUTO,
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val use24Hour: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val clockStyle: ClockStyle = ClockStyle.CANDY,
    val theme: AppTheme = AppTheme.SKY,
    val character: MascotId = MascotId.TICKY,
    /** Daily learning time in minutes; 0 means "no limit". */
    val dailyLimitMinutes: Int = 30,
    val hintsEnabled: Boolean = true,
    /** Optional reduced motion mode for children sensitive to animation. */
    val reduceMotion: Boolean = false,
    val hasSeenIntro: Boolean = false
)
