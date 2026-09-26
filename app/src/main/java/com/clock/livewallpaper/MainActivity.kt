package com.clock.livewallpaper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.ui.AppViewModel
import com.clock.livewallpaper.ui.DhikrApp
import com.clock.livewallpaper.ui.navigation.Routes
import com.clock.livewallpaper.ui.theme.DhikrTheme
import com.clock.livewallpaper.ui.theme.resolveDarkTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity.
 *
 * It keeps the system splash screen on screen only until the persisted theme and the onboarding
 * flag have been read - there is no artificial delay - and it routes the deep links that arrive
 * from notifications and widgets.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    /** Route requested by a notification or a widget, consumed once by the nav host. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !viewModel.state.value.loaded }
        enableEdgeToEdge()

        pendingRoute = routeFrom(intent)

        setContent {
            val appState by viewModel.state.collectAsStateWithLifecycle()
            val darkTheme = resolveDarkTheme(appState.themeMode)
            val view = LocalView.current

            LaunchedEffect(darkTheme) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }

            DhikrTheme(themeMode = appState.themeMode) {
                if (appState.loaded) {
                    DhikrApp(
                        startWithOnboarding = !appState.onboardingCompleted,
                        pendingRoute = pendingRoute,
                        onRouteHandled = { pendingRoute = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeFrom(intent)?.let { pendingRoute = it }
    }

    /** Only routes the app itself declares are accepted; anything else is ignored. */
    private fun routeFrom(intent: Intent?): String? {
        val route = intent?.getStringExtra(EXTRA_ROUTE)
        return if (Routes.isKnown(route)) route else null
    }

    companion object {

        const val EXTRA_ROUTE = "com.clock.livewallpaper.extra.ROUTE"

        /** Explicit launch intent used by notifications and widgets. */
        fun routeIntent(context: Context, route: String): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .putExtra(EXTRA_ROUTE, route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
