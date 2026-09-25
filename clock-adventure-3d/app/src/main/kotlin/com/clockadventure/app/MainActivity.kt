package com.clockadventure.app

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
import com.clockadventure.presentation.navigation.AppNavHost
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen portrait game: the content draws behind the system bars, which are hidden.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

            LaunchedEffect(settings.language) {
                applyLanguage(settings.language)
            }
            LaunchedEffect(settings.musicEnabled, settings.soundEnabled, settings.voiceEnabled) {
                audio.applySettings(settings.musicEnabled, settings.soundEnabled, settings.voiceEnabled)
            }
            DisposableEffect(Unit) {
                onDispose {
                    audio.release()
                }
            }

            val rtl = settings.language == AppLanguage.ARABIC
            ClockAdventureTheme(
                theme = settings.theme,
                reduceMotion = settings.reduceMotion,
                rtl = rtl
            ) {
                CompositionLocalProvider(
                    LocalRtl provides rtl,
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
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
