package com.clockadventure.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.clockadventure.presentation.screens.challenges.ChallengesRoute
import com.clockadventure.presentation.screens.games.GamesRoute
import com.clockadventure.presentation.screens.home.HomeRoute
import com.clockadventure.presentation.screens.lessons.LessonsRoute
import com.clockadventure.presentation.screens.parent.ParentGateRoute
import com.clockadventure.presentation.screens.parent.ParentRoute
import com.clockadventure.presentation.screens.progress.ProgressRoute
import com.clockadventure.presentation.screens.rewards.RewardsRoute
import com.clockadventure.presentation.screens.session.SessionRoute
import com.clockadventure.presentation.screens.settings.SettingsRoute

/**
 * The whole app navigation graph. Home is the start destination; every other destination is one
 * tap away from it and the system back button always returns to the previous screen.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeRoute(
                onStartLearning = { levelId -> navController.navigate(Routes.session(levelId = levelId)) },
                onLessons = { navController.navigate(Routes.LESSONS) },
                onChallenges = { navController.navigate(Routes.CHALLENGES) },
                onGames = { navController.navigate(Routes.GAMES) },
                onProgress = { navController.navigate(Routes.PROGRESS) },
                onRewards = { navController.navigate(Routes.REWARDS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onParent = { navController.navigate(Routes.PARENT_GATE) }
            )
        }

        composable(Routes.LESSONS) {
            LessonsRoute(
                onBack = { navController.popBackStack() },
                onOpenLesson = { levelId -> navController.navigate(Routes.session(levelId = levelId)) }
            )
        }

        composable(Routes.CHALLENGES) {
            ChallengesRoute(
                onBack = { navController.popBackStack() },
                onOpenChallenge = { challengeId ->
                    navController.navigate(Routes.session(challengeId = challengeId))
                }
            )
        }

        composable(Routes.GAMES) {
            GamesRoute(
                onBack = { navController.popBackStack() },
                onOpenGame = { gameId -> navController.navigate(Routes.session(gameId = gameId)) }
            )
        }

        composable(Routes.PROGRESS) {
            ProgressRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.REWARDS) {
            RewardsRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.PARENT_GATE) {
            ParentGateRoute(
                onBack = { navController.popBackStack() },
                onVerified = {
                    navController.navigate(Routes.PARENT) {
                        popUpTo(Routes.PARENT_GATE) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PARENT) {
            ParentRoute(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.SESSION,
            arguments = listOf(
                navArgument(Routes.ARG_LEVEL_ID) {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument(Routes.ARG_GAME_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Routes.ARG_CHALLENGE_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            SessionRoute(
                onBack = { navController.popBackStack() },
                onNextLevel = { levelId ->
                    navController.navigate(Routes.session(levelId = levelId)) {
                        popUpTo(Routes.SESSION) { inclusive = true }
                    }
                },
                onHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
