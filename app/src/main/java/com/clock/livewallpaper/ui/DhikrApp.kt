package com.clock.livewallpaper.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clock.livewallpaper.ui.navigation.DhikrBottomBar
import com.clock.livewallpaper.ui.navigation.DhikrNavHost
import com.clock.livewallpaper.ui.navigation.Routes

/** Lets any screen post a short Arabic confirmation without owning its own Scaffold. */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState was not provided")
}

/**
 * The app shell: one Scaffold, one nav host, one bottom bar.
 *
 * The bottom bar only exists on the four tab destinations; reading, editor, settings and legal
 * screens are full-height and carry their own top bar with a back affordance.
 */
@Composable
fun DhikrApp(
    startWithOnboarding: Boolean,
    pendingRoute: String?,
    onRouteHandled: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Captured once: flipping the onboarding flag must not rebuild the graph under the user.
    val startDestination = remember { if (startWithOnboarding) Routes.ONBOARDING else Routes.HOME }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomBarVisible = currentRoute != null && Routes.BOTTOM_ROUTES.contains(currentRoute)

    LaunchedEffect(pendingRoute, currentRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        // A deep link never interrupts first-run onboarding.
        if (currentRoute == null || currentRoute == Routes.ONBOARDING) return@LaunchedEffect
        if (currentRoute != route) {
            navController.navigate(route) { launchSingleTop = true }
        }
        onRouteHandled()
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = bottomBarVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    DhikrBottomBar(
                        currentRoute = currentRoute,
                        onSelect = { route -> navController.navigateTab(route) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                DhikrNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}

/** Tab switching: single instance per tab, state of the previous tab saved and restored. */
fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
