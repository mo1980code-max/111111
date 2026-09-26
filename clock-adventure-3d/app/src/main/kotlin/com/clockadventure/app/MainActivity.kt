package com.clockadventure.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.clockadventure.domain.model.AppLanguage
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.repository.AudioController
import com.clockadventure.domain.repository.SettingsRepository
import com.clockadventure.app.notification.ReminderScheduler
import com.clockadventure.presentation.navigation.AppNavHost
import com.clockadventure.presentation.navigation.Routes
import com.clockadventure.presentation.screens.splash.SplashScreen
import com.clockadventure.presentation.theme.ClockAdventureTheme
import com.clockadventure.presentation.theme.LocalRtl
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

/**
 * The single activity of the app. Everything else is a Compose destination.
 *
 * It wires three things together: the saved settings (which drive language, RTL, theme and sound),
 * the navigation graph and the audio controller lifetime (music stops when the app goes to the
 * background and the TextToSpeech engine is released when the activity is destroyed).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var audio: AudioController

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen portrait game: the content draws behind the system bars, which are hidden.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyOrientationLock(resources.configuration)

        setContent {
            // null until the saved preferences have been read from disk: the navigation graph must
            // not be created before we know whether this is a first run.
            val settings by settingsRepository.settings.collectAsState(initial = null)

            if (settings == null) {
                SplashScreen()
                return@setContent
            }
            val current = settings!!

            LaunchedEffect(current.language) {
                applyLanguage(current.language)
            }
            LaunchedEffect(current.musicEnabled, current.soundEnabled, current.voiceEnabled) {
                audio.applySettings(current.musicEnabled, current.soundEnabled, current.voiceEnabled)
            }
            LaunchedEffect(current.notificationsEnabled) {
                if (current.notificationsEnabled) {
                    reminderScheduler.scheduleDailyReminder()
                } else {
                    reminderScheduler.cancel()
                }
            }
            DisposableEffect(Unit) {
                onDispose {
                    audio.release()
                }
            }

            val rtl = current.language == AppLanguage.ARABIC
            ClockAdventureTheme(
                theme = current.theme,
                reduceMotion = current.reduceMotion,
                rtl = rtl
            ) {
                CompositionLocalProvider(
                    LocalRtl provides rtl,
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        startDestination = if (current.hasSeenIntro) Routes.HOME else Routes.ONBOARDING,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            HideSystemBarsEffect()
        }
    }

    override fun onPause() {
        super.onPause()
        audio.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        audio.resumeMusic()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationLock(newConfig)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        applyOrientationLock(newConfig)
    }

    /**
     * A child usually plays this one-handed on a phone, so phones stay locked to portrait - the
     * lock a previous version hard-coded into the manifest. But a fixed manifest lock also fights
     * the window manager on a tablet (which is free to rotate) and is outright rejected by Android
     * the moment the activity is put into split screen or a free-form multi-window: the platform
     * ignores - and, on some versions, logs a warning about - a fixed orientation there. So the
     * lock now lives here instead, and is re-evaluated on every configuration change: it stays on
     * for a phone in single-window, and is lifted (free rotation, follows the window/tablet) as
     * soon as the smallest width crosses the tablet threshold or multi-window kicks in.
     */
    private fun applyOrientationLock(configuration: Configuration) {
        val isLargeScreen = configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP
        requestedOrientation = if (isLargeScreen || isInMultiWindowMode()) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        val locale = if (language == AppLanguage.ARABIC) Locale("ar") else Locale.ENGLISH
        val current = resources.configuration.locales.get(0)
        if (current.language == locale.language) return
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }

    private companion object {
        /** Matches the sw600dp breakpoint Android itself uses to call a device a tablet. */
        const val TABLET_SMALLEST_WIDTH_DP = 600
    }
}

@Composable
private fun HideSystemBarsEffect() {
    val window = androidx.compose.ui.platform.LocalView.current.context as? ComponentActivity
    DisposableEffect(window) {
        window?.let { activity ->
            val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose { }
    }
}
