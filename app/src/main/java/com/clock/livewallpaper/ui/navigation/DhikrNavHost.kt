package com.clock.livewallpaper.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.ui.screens.about.AboutScreen
import com.clock.livewallpaper.ui.screens.adhkar.AdhkarScreen
import com.clock.livewallpaper.ui.screens.editor.DhikrEditorScreen
import com.clock.livewallpaper.ui.screens.home.HomeScreen
import com.clock.livewallpaper.ui.screens.mydhikr.MyDhikrScreen
import com.clock.livewallpaper.ui.screens.onboarding.OnboardingScreen
import com.clock.livewallpaper.ui.screens.overlay.OverlaySettingsScreen
import com.clock.livewallpaper.ui.screens.privacy.PrivacyScreen
import com.clock.livewallpaper.ui.screens.reading.ReadingScreen
import com.clock.livewallpaper.ui.screens.settings.SettingsScreen
import com.clock.livewallpaper.ui.screens.tasbeeh.TasbeehScreen

private const val TRANSITION_MS = 260

/**
 * Every destination of the app.
 *
 * Transitions use the direction-aware `slideIntoContainer`, so they mirror correctly in the RTL
 * layout: a pushed screen enters from the left edge and the previous one leaves to the right.
 */
@Composable
fun DhikrNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(TRANSITION_MS)
            ) + fadeIn(tween(TRANSITION_MS))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(TRANSITION_MS)
            ) + fadeOut(tween(TRANSITION_MS))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(TRANSITION_MS)
            ) + fadeIn(tween(TRANSITION_MS))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(TRANSITION_MS)
            ) + fadeOut(tween(TRANSITION_MS))
        }
    ) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenReading = { category -> navController.navigate(Routes.reading(category)) },
                onOpenAdhkar = { navController.navigateTabRoute(Routes.ADHKAR) },
                onOpenTasbeeh = { navController.navigateTabRoute(Routes.TASBEEH) },
                onOpenMyDhikr = { navController.navigateTabRoute(Routes.MY_DHIKR) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ADHKAR) {
            AdhkarScreen(
                onOpenCategory = { category -> navController.navigate(Routes.reading(category)) },
                onAddDhikr = { navController.navigate(Routes.editor()) }
            )
        }

        composable(Routes.TASBEEH) {
            TasbeehScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.MY_DHIKR) {
            MyDhikrScreen(
                onAdd = { navController.navigate(Routes.editor()) },
                onEdit = { id -> navController.navigate(Routes.editor(id)) }
            )
        }

        composable(
            route = Routes.READING_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_CATEGORY) {
                    type = NavType.StringType
                    defaultValue = DhikrCategory.MORNING.key
                }
            )
        ) {
            ReadingScreen(
                onBack = { navController.popBackStack() },
                onAddDhikr = { navController.navigate(Routes.editor()) }
            )
        }

        composable(
            route = Routes.EDITOR_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_DHIKR_ID) {
                    type = NavType.LongType
                    defaultValue = Routes.NEW_DHIKR_ID
                }
            )
        ) {
            DhikrEditorScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenOverlaySettings = { navController.navigate(Routes.OVERLAY_SETTINGS) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.OVERLAY_SETTINGS) {
            OverlaySettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) }
            )
        }
    }
}

/** Jump to another tab from inside a tab, keeping one instance of each. */
private fun NavHostController.navigateTabRoute(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
